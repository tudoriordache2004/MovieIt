from functools import lru_cache
from typing import Optional

import numpy as np
from sentence_transformers import SentenceTransformer
from transformers import pipeline
from sqlalchemy.orm import Session, joinedload

from app.models.diary_entry import DiaryEntry
from app.models.movie import Movie
from app.models.movie_embedding import MovieEmbedding
from app.models.review import Review


EMBEDDING_MODEL = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
SENTIMENT_MODEL = "cardiffnlp/twitter-xlm-roberta-base-sentiment"

GENRE_MOOD_MAP = {
    "Action": "energetic",
    "Adventure": "escapist",
    "Animation": "comfort",
    "Comedy": "playful",
    "Crime": "dark",
    "Documentary": "curious",
    "Drama": "melancholic",
    "Family": "comfort",
    "Fantasy": "escapist",
    "History": "reflective",
    "Horror": "intense",
    "Music": "emotional",
    "Mystery": "curious",
    "Romance": "romantic",
    "Science Fiction": "mind_bending",
    "TV Movie": "comfort",
    "Thriller": "tense",
    "War": "heavy",
    "Western": "gritty",
}

MOOD_MESSAGES = {
    "energetic": "Need a rush? These picks should keep your pulse up.",
    "escapist": "Want to get away for a while? These worlds are waiting.",
    "comfort": "Need something comforting tonight? These are gentle picks.",
    "playful": "Want to keep it light? These should do the trick.",
    "dark": "Drawn to darker stories? These might be your next obsession.",
    "curious": "Feeling curious? These picks should give you something to think about.",
    "melancholic": "Feeling lonely? These stories might keep you company.",
    "reflective": "Want something thoughtful? These picks look back to say something more.",
    "intense": "Want something that gets under your skin? Try these.",
    "emotional": "Need something that hits the right notes? These are for you.",
    "romantic": "In the mood for something heartfelt? These stories lean into it.",
    "mind_bending": "Want your brain to work overtime? Start here.",
    "tense": "Craving suspense? These picks should keep you guessing.",
    "heavy": "Ready for something weighty? These stories do not look away.",
    "gritty": "Want something rough around the edges? These picks have that bite.",
}

DEFAULT_HOME_MESSAGE = "Ready to find something worth watching? Start with these picks."
DIARY_ENTRY_WEIGHT = 0.35


@lru_cache(maxsize=1)
def get_embedding_model() -> SentenceTransformer:
    return SentenceTransformer(EMBEDDING_MODEL)


@lru_cache(maxsize=1)
def get_sentiment_classifier():
    return pipeline("sentiment-analysis", model=SENTIMENT_MODEL)


def embed_text(text: str) -> list[float]:
    vector = get_embedding_model().encode(text, normalize_embeddings=True)
    return vector.astype(float).tolist()


def cosine_similarity(a: list[float], b: list[float]) -> float:
    vec_a = np.array(a, dtype=np.float32)
    vec_b = np.array(b, dtype=np.float32)

    denominator = np.linalg.norm(vec_a) * np.linalg.norm(vec_b)
    if denominator == 0:
        return 0.0

    return float(np.dot(vec_a, vec_b) / denominator)


def classify_sentiment(text: str) -> tuple[str, float]:
    if not text or not text.strip():
        return "neutral", 0.0

    result = get_sentiment_classifier()(text[:512])[0]
    label = result["label"].lower()
    score = float(result["score"])

    if "positive" in label:
        return "positive", score
    if "negative" in label:
        return "negative", score
    return "neutral", score


def build_movie_source_text(movie: Movie) -> str:
    genres = ", ".join(genre.name for genre in getattr(movie, "genre_list", []) or [])
    return "\n".join(
        part for part in [
            f"Title: {movie.title}",
            f"Genres: {genres}" if genres else "",
            f"Description: {movie.description or ''}",
        ]
        if part.strip()
    )


def build_user_profile_vector(db: Session, user_id: int, min_rating: int = 8) -> tuple[Optional[list[float]], str]:
    reviews = (
        db.query(Review)
        .options(joinedload(Review.movie).joinedload(Movie.genre_list))
        .filter(
            Review.user_id == user_id,
            Review.rating >= min_rating,
        )
        .all()
    )
    diary_entries = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie).joinedload(Movie.genre_list))
        .filter(DiaryEntry.user_id == user_id)
        .all()
    )
    reviewed_movie_ids = {
        movie_id for (movie_id,) in db.query(Review.movie_id).filter(Review.user_id == user_id).all()
    }

    weighted_vectors = []
    weights = []
    strongest_sentiment = "neutral"

    for review in reviews:
        if review.comment and review.comment.strip():
            sentiment, sentiment_score = classify_sentiment(review.comment)

            if sentiment == "negative":
                continue

            movie_embedding = (
                db.query(MovieEmbedding)
                .filter(MovieEmbedding.movie_id == review.movie_id)
                .first()
            )

            if not movie_embedding:
                continue

            vector = movie_embedding.embedding

            sentiment_weight = 1.0 + sentiment_score if sentiment == "positive" else 1.0
            rating_weight = review.rating / 10.0
            weight = rating_weight * sentiment_weight

            weighted_vectors.append(np.array(vector, dtype=np.float32) * weight)
            weights.append(weight)

            if sentiment == "positive":
                strongest_sentiment = "positive"
        else:
            movie_embedding = (
                db.query(MovieEmbedding)
                .filter(MovieEmbedding.movie_id == review.movie_id)
                .first()
            )
            if movie_embedding:
                weight = review.rating / 10.0
                weighted_vectors.append(np.array(movie_embedding.embedding, dtype=np.float32) * weight)
                weights.append(weight)

    for entry in diary_entries:
        if entry.movie_id in reviewed_movie_ids:
            continue

        movie_embedding = (
            db.query(MovieEmbedding)
            .filter(MovieEmbedding.movie_id == entry.movie_id)
            .first()
        )

        if not movie_embedding:
            continue

        weighted_vectors.append(np.array(movie_embedding.embedding, dtype=np.float32) * DIARY_ENTRY_WEIGHT)
        weights.append(DIARY_ENTRY_WEIGHT)

    if not weighted_vectors or not weights:
        return None, strongest_sentiment

    profile = np.sum(weighted_vectors, axis=0) / sum(weights)
    norm = np.linalg.norm(profile)
    if norm > 0:
        profile = profile / norm

    return profile.astype(float).tolist(), strongest_sentiment


def format_genre_list(genres: list[str]) -> str:
    if len(genres) == 1:
        return genres[0]
    if len(genres) == 2:
        return f"{genres[0]} and {genres[1]}"
    return f"{', '.join(genres[:-1])}, and {genres[-1]}"


def build_recommendation_reason(sentiment: str, matched_genres: list[str]) -> str:
    if matched_genres and sentiment == "positive":
        return (
            "Because you loved similar movies, this one also matches your taste "
            f"for {format_genre_list(matched_genres)}."
        )

    if matched_genres:
        return f"Recommended because it shares your taste for {format_genre_list(matched_genres)}."

    if sentiment == "positive":
        return "A close match based on movies you rated highly and reviewed positively."

    return "A semantically close pick based on the movies you rated highly."


def get_user_taste_genres(db: Session, user_id: int, min_rating: int = 8) -> list[str]:
    reviews = (
        db.query(Review)
        .options(joinedload(Review.movie).joinedload(Movie.genre_list))
        .filter(
            Review.user_id == user_id,
            Review.rating >= min_rating,
        )
        .all()
    )
    diary_entries = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie).joinedload(Movie.genre_list))
        .filter(DiaryEntry.user_id == user_id)
        .all()
    )
    reviewed_movie_ids = {
        movie_id for (movie_id,) in db.query(Review.movie_id).filter(Review.user_id == user_id).all()
    }

    genre_scores: dict[str, float] = {}

    for review in reviews:
        rating_weight = (review.rating or min_rating) / 10.0

        for genre_name in get_movie_genre_names(review.movie):
            genre_scores[genre_name] = genre_scores.get(genre_name, 0.0) + rating_weight

    for entry in diary_entries:
        if entry.movie_id in reviewed_movie_ids:
            continue

        for genre_name in get_movie_genre_names(entry.movie):
            genre_scores[genre_name] = genre_scores.get(genre_name, 0.0) + DIARY_ENTRY_WEIGHT

    return [
        genre_name
        for genre_name, _ in sorted(
            genre_scores.items(),
            key=lambda item: item[1],
            reverse=True,
        )
    ]


def get_recommendations_for_user(db: Session, user_id: int, limit: int = 6, min_rating: int = 8):
    user_vector, sentiment = build_user_profile_vector(db, user_id, min_rating=min_rating)

    if user_vector is None:
        fallback_movies = (
            db.query(Movie)
            .options(joinedload(Movie.genre_list))
            .order_by(Movie.avg_rating.desc(), Movie.popularity.desc().nullslast())
            .limit(limit)
            .all()
        )

        return [
            {
                "movie": movie,
                "score": 0.0,
                "reason": "Need a fresh start? Check it out!",
                "movie_genres": get_movie_genre_names(movie),
                "matched_genres": [],
            }
            for movie in fallback_movies
        ]

    user_taste_genres = get_user_taste_genres(db, user_id, min_rating=min_rating)

    reviewed_movie_ids = {
        movie_id for (movie_id,) in db.query(Review.movie_id).filter(Review.user_id == user_id).all()
    }
    diary_movie_ids = {
        movie_id for (movie_id,) in db.query(DiaryEntry.movie_id).filter(DiaryEntry.user_id == user_id).all()
    }
    seen_movie_ids = reviewed_movie_ids | diary_movie_ids

    candidates = (
        db.query(MovieEmbedding)
        .join(Movie)
        .options(joinedload(MovieEmbedding.movie).joinedload(Movie.genre_list))
        .filter(~MovieEmbedding.movie_id.in_(seen_movie_ids))
        .all()
    )

    scored = []
    for candidate in candidates:
        semantic_score = cosine_similarity(user_vector, candidate.embedding)

        movie_genres = get_movie_genre_names(candidate.movie)
        movie_genre_set = set(movie_genres)

        matched_genres = [
            genre_name
            for genre_name in user_taste_genres
            if genre_name in movie_genre_set
        ]

        genre_overlap_score = (
            len(matched_genres) / len(user_taste_genres)
            if user_taste_genres
            else 0.0
        )

        final_score = (0.80 * semantic_score) + (0.20 * genre_overlap_score)

        scored.append(
            {
                "movie": candidate.movie,
                "score": round(final_score, 4),
                "reason": build_recommendation_reason(sentiment, matched_genres),
                "movie_genres": movie_genres,
                "matched_genres": matched_genres,
            }
        )

    scored.sort(key=lambda item: item["score"], reverse=True)
    return scored[:limit]

def get_movie_genre_names(movie: Movie) -> list[str]:
    return [genre.name for genre in getattr(movie, "genre_list", []) or []]


def infer_home_context(recommendations: list[dict]) -> dict:
    genre_scores: dict[str, float] = {}

    for item in recommendations:
        movie = item["movie"]
        score = max(float(item.get("score", 0.0)), 0.25)

        for genre_name in get_movie_genre_names(movie):
            genre_scores[genre_name] = genre_scores.get(genre_name, 0.0) + score

    if not genre_scores:
        return {
            "hero_message": DEFAULT_HOME_MESSAGE,
            "dominant_genre": None,
            "mood": "neutral",
        }

    dominant_genre = max(genre_scores.items(), key=lambda item: item[1])[0]
    mood = GENRE_MOOD_MAP.get(dominant_genre, "neutral")

    return {
        "hero_message": MOOD_MESSAGES.get(mood, DEFAULT_HOME_MESSAGE),
        "dominant_genre": dominant_genre,
        "mood": mood,
    }


def infer_user_taste_context(db: Session, user_id: int, min_rating: int = 8) -> Optional[dict]:
    reviews = (
        db.query(Review)
        .options(joinedload(Review.movie).joinedload(Movie.genre_list))
        .filter(
            Review.user_id == user_id,
            Review.rating >= min_rating,
        )
        .all()
    )
    diary_entries = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie).joinedload(Movie.genre_list))
        .filter(DiaryEntry.user_id == user_id)
        .all()
    )
    reviewed_movie_ids = {
        movie_id for (movie_id,) in db.query(Review.movie_id).filter(Review.user_id == user_id).all()
    }

    genre_scores: dict[str, float] = {}

    for review in reviews:
        rating_weight = (review.rating or min_rating) / 10.0

        for genre_name in get_movie_genre_names(review.movie):
            genre_scores[genre_name] = genre_scores.get(genre_name, 0.0) + rating_weight

    for entry in diary_entries:
        if entry.movie_id in reviewed_movie_ids:
            continue

        for genre_name in get_movie_genre_names(entry.movie):
            genre_scores[genre_name] = genre_scores.get(genre_name, 0.0) + DIARY_ENTRY_WEIGHT

    if not genre_scores:
        return None

    dominant_genre = max(genre_scores.items(), key=lambda item: item[1])[0]
    mood = GENRE_MOOD_MAP.get(dominant_genre, "neutral")

    return {
        "hero_message": MOOD_MESSAGES.get(mood, DEFAULT_HOME_MESSAGE),
        "dominant_genre": dominant_genre,
        "mood": mood,
    }


def get_home_recommendations_for_user(
    db: Session,
    user_id: int,
    limit: int = 6,
    min_rating: int = 8,
) -> dict:
    recommendations = get_recommendations_for_user(
        db=db,
        user_id=user_id,
        limit=limit,
        min_rating=min_rating,
    )

    context = infer_user_taste_context(db, user_id, min_rating=min_rating)
    if context is None:
        context = {
            "hero_message": DEFAULT_HOME_MESSAGE,
            "dominant_genre": None,
            "mood": "neutral",
        }

    return {
        **context,
        "recommendations": recommendations,
    }