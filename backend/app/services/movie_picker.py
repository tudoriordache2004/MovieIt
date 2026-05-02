from functools import lru_cache
from typing import Optional

import numpy as np
from sqlalchemy.orm import Session, joinedload
from transformers import pipeline

from app.models.diary_entry import DiaryEntry
from app.models.movie import Movie
from app.models.movie_embedding import MovieEmbedding
from app.models.movie_picker_session import MoviePickerSession
from app.models.review import Review
from app.schemas.movie_picker import MoviePickerSessionCreate
from app.services.recommendations import cosine_similarity, embed_text

from datetime import datetime

from fastapi import HTTPException, status

from app.schemas.movie_picker import MoviePickerPickOut


ZERO_SHOT_MODEL = "facebook/bart-large-mnli"

MOOD_LABELS = [
    "comforting",
    "thoughtful",
    "romantic",
    "funny",
    "intense",
    "mind_bending",
    "adventurous",
    "inspiring",
    "dark",
    "melancholic",
    "nostalgic",
    "relaxing",
]

AVOID_TO_MOOD_LABELS = {
    "dark": {"dark"},
    "sad": {"melancholic"},
    "horror": {"dark", "intense"},
    "violence": {"dark", "intense"},
    "war": {"dark", "intense", "melancholic"},
}

MOOD_BLOCKED_AVOID_SIGNALS = {
    "dark": {"dark"},
    "melancholic": {"sad"},
    "intense": {"violence"},
}

MOOD_QUERY_TEMPLATES = {
    "comforting": "A warm, comforting, gentle movie with emotional safety, kindness, and a reassuring tone.",
    "thoughtful": "A thoughtful, reflective movie with meaningful themes, emotional depth, and mature ideas.",
    "romantic": "A romantic, heartfelt movie focused on relationships, intimacy, longing, and emotional connection.",
    "funny": "A light, funny, entertaining movie with humor, charm, and an easygoing tone.",
    "intense": "An intense, gripping movie with suspense, danger, tension, urgency, or high stakes.",
    "mind_bending": "A mind-bending movie with complex ideas, mystery, twists, ambiguity, or reality-shifting concepts.",
    "adventurous": "An adventurous movie with discovery, travel, action, exploration, fantasy, or exciting journeys.",
    "inspiring": "An inspiring, uplifting movie about hope, resilience, ambition, courage, or personal growth.",
    "dark": "A dark, serious movie with crime, moral ambiguity, psychological tension, danger, or bleak atmosphere.",
    "melancholic": "A melancholic, emotional movie with sadness, longing, memory, heartbreak, or bittersweet themes.",
    "nostalgic": "A nostalgic movie with memory, youth, longing for the past, warmth, and emotional reflection.",
    "relaxing": "A calm, relaxing movie with a soft atmosphere, low tension, gentle pacing, and comforting emotions.",
}

MOOD_GENRE_AFFINITY = {
    "comforting": {
        "Family": 0.9,
        "Animation": 0.8,
        "Comedy": 0.7,
        "Romance": 0.55,
        "Drama": 0.35,
        "Horror": -1.0,
        "War": -0.8,
        "Thriller": -0.5,
        "Crime": -0.4,
    },
    "thoughtful": {
        "Drama": 0.9,
        "Documentary": 0.8,
        "History": 0.7,
        "Science Fiction": 0.45,
        "Mystery": 0.35,
    },
    "romantic": {
        "Romance": 1.0,
        "Drama": 0.7,
        "Music": 0.45,
        "Comedy": 0.35,
        "Horror": -0.8,
        "War": -0.6,
    },
    "funny": {
        "Comedy": 1.0,
        "Animation": 0.6,
        "Family": 0.5,
        "Adventure": 0.25,
        "Horror": -0.5,
        "War": -0.6,
    },
    "intense": {
        "Thriller": 0.9,
        "Action": 0.85,
        "Crime": 0.75,
        "Horror": 0.65,
        "Drama": 0.3,
        "Family": -0.5,
        "Animation": -0.3,
    },
    "mind_bending": {
        "Science Fiction": 1.0,
        "Mystery": 0.85,
        "Thriller": 0.55,
        "Drama": 0.25,
    },
    "adventurous": {
        "Adventure": 1.0,
        "Action": 0.75,
        "Fantasy": 0.7,
        "Science Fiction": 0.45,
        "Family": 0.35,
    },
    "inspiring": {
        "Drama": 0.75,
        "History": 0.55,
        "Documentary": 0.5,
        "Family": 0.45,
        "Music": 0.4,
        "War": -0.25,
    },
    "dark": {
        "Crime": 0.9,
        "Thriller": 0.8,
        "Horror": 0.75,
        "Drama": 0.6,
        "Mystery": 0.55,
        "Family": -0.8,
        "Animation": -0.4,
    },
    "melancholic": {
        "Drama": 0.9,
        "Romance": 0.6,
        "Music": 0.45,
        "History": 0.3,
        "Comedy": -0.2,
        "Action": -0.25,
    },
    "nostalgic": {
        "Drama": 0.65,
        "Family": 0.55,
        "Animation": 0.45,
        "Romance": 0.35,
        "Music": 0.35,
    },
    "relaxing": {
        "Family": 0.75,
        "Animation": 0.7,
        "Comedy": 0.55,
        "Romance": 0.4,
        "Thriller": -0.8,
        "Horror": -1.0,
        "War": -0.8,
        "Crime": -0.5,
    },
}

AVOID_KEYWORDS = {
    "horror": ["horror", "haunted", "demon", "ghost", "supernatural terror"],
    "war": ["war", "battle", "soldier", "military", "combat"],
    "violence": ["violence", "brutal", "murder", "killer", "torture", "rape"],
    "sad": ["grief", "death", "heartbreak", "loss", "tragic", "depressing", "mourning", "lonely"],
    "dark": ["bleak", "disturbing", "dark", "psychological", "trauma", "tragic", "cruel", "violent", "revenge", "abuse"],
}


@lru_cache(maxsize=1)
def get_zero_shot_classifier():
    return pipeline("zero-shot-classification", model=ZERO_SHOT_MODEL)


def get_movie_genre_names(movie: Movie) -> list[str]:
    return [genre.name for genre in getattr(movie, "genre_list", []) or []]


def get_seen_movie_ids(db: Session, user_id: int) -> set[int]:
    reviewed_movie_ids = {
        movie_id
        for (movie_id,) in db.query(Review.movie_id)
        .filter(Review.user_id == user_id)
        .all()
    }

    diary_movie_ids = {
        movie_id
        for (movie_id,) in db.query(DiaryEntry.movie_id)
        .filter(DiaryEntry.user_id == user_id)
        .all()
    }

    return reviewed_movie_ids | diary_movie_ids


def normalize_mood(mood: Optional[str]) -> Optional[str]:
    if not mood:
        return None

    normalized = mood.strip().lower().replace("-", "_").replace(" ", "_")
    if normalized not in MOOD_LABELS:
        raise ValueError(f"Unsupported mood: {mood}")

    return normalized


def normalize_avoid(avoid: list[str]) -> list[str]:
    return [
        item.strip().lower().replace("-", "_").replace(" ", "_")
        for item in avoid
        if item and item.strip()
    ]


def get_blocked_moods_from_avoid(avoid: list[str]) -> set[str]:
    blocked = set()

    for signal in normalize_avoid(avoid):
        blocked.update(AVOID_TO_MOOD_LABELS.get(signal, set()))

    return blocked


def get_conflicting_avoid_signals(mood: Optional[str], avoid: list[str]) -> set[str]:
    if not mood:
        return set()

    blocked_avoid_signals = MOOD_BLOCKED_AVOID_SIGNALS.get(mood, set())
    return set(normalize_avoid(avoid)) & blocked_avoid_signals


def classify_prompt_mood(prompt: str) -> tuple[str, list[str]]:
    result = get_zero_shot_classifier()(
        prompt,
        candidate_labels=MOOD_LABELS,
        multi_label=True,
    )

    labels = result["labels"]
    scores = result["scores"]

    ranked = [
        label
        for label, score in zip(labels, scores)
        if score >= 0.25
    ]

    if not ranked:
        return "thoughtful", []

    primary = ranked[0]
    secondary = [label for label in ranked[1:3] if label != primary]

    return primary, secondary


def interpret_picker_request(payload: MoviePickerSessionCreate) -> tuple[str, list[str], str]:
    mood = normalize_mood(payload.mood)
    blocked_moods = get_blocked_moods_from_avoid(payload.avoid)

    conflicting_avoid_signals = get_conflicting_avoid_signals(mood, payload.avoid)
    if conflicting_avoid_signals:
        readable_conflicts = ", ".join(sorted(conflicting_avoid_signals))
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=(
                f"You selected '{mood}' but also asked to avoid "
                f"{readable_conflicts}."
            ),
        )

    if payload.prompt and payload.prompt.strip():
        prompt_primary, prompt_secondary = classify_prompt_mood(payload.prompt.strip())

        primary_mood = mood or prompt_primary
        if primary_mood in blocked_moods and mood is None:
            primary_mood = "comforting"

        secondary_moods = [
            label
            for label in [prompt_primary, *prompt_secondary]
            if label != primary_mood and label not in blocked_moods
        ][:3]
    else:
        primary_mood = mood or "thoughtful"
        secondary_moods = []

    secondary_moods = [
        label for label in secondary_moods if label not in blocked_moods
    ]

    summary = build_picker_summary(
        primary_mood=primary_mood,
        secondary_moods=secondary_moods,
        prompt=payload.prompt,
        avoid=payload.avoid,
    )

    return primary_mood, secondary_moods, summary


def build_picker_summary(
    primary_mood: str,
    secondary_moods: list[str],
    prompt: Optional[str],
    avoid: list[str],
) -> str:
    parts = [
        f"The user wants a {primary_mood.replace('_', ' ')} movie."
    ]

    if secondary_moods:
        readable = ", ".join(mood.replace("_", " ") for mood in secondary_moods)
        parts.append(f"Secondary emotional signals: {readable}.")

    if prompt and prompt.strip():
        parts.append(f'User prompt: "{prompt.strip()}".')

    if avoid:
        parts.append(f"Avoid: {', '.join(avoid)}.")

    return " ".join(parts)


def build_query_text(
    primary_mood: str,
    secondary_moods: list[str],
    prompt: Optional[str],
    intensity: str,
    avoid: list[str],
) -> str:
    mood_text = MOOD_QUERY_TEMPLATES[primary_mood]

    secondary_text = " ".join(
        MOOD_QUERY_TEMPLATES[mood]
        for mood in secondary_moods
        if mood in MOOD_QUERY_TEMPLATES
    )

    prompt_text = f'User request: "{prompt.strip()}".' if prompt and prompt.strip() else ""

    avoid_text = ""
    if avoid:
        avoid_text = (
            " Strongly avoid movies with these qualities: "
            f"{', '.join(avoid)}."
        )

    return (
        f"{mood_text} {secondary_text} "
        f"The requested intensity is {intensity}. "
        f"{prompt_text}"
        f"{avoid_text}"
    ).strip()


def score_genre_affinity(movie_genres: list[str], moods: list[str]) -> float:
    if not movie_genres or not moods:
        return 0.0

    total = 0.0
    count = 0

    for mood in moods:
        affinities = MOOD_GENRE_AFFINITY.get(mood, {})
        for genre in movie_genres:
            if genre in affinities:
                total += affinities[genre]
                count += 1

    if count == 0:
        return 0.0

    score = total / count
    return max(min(score, 1.0), -1.0)


def score_rating(movie: Movie) -> float:
    if movie.avg_rating is None:
        return 0.0
    return max(min(movie.avg_rating / 10.0, 1.0), 0.0)


def score_popularity(movie: Movie) -> float:
    if movie.popularity is None:
        return 0.0

    # TMDB popularity is unbounded; this keeps it from dominating the score.
    return max(min(movie.popularity / 1000.0, 1.0), 0.0)


def detect_avoided_signals(movie: Movie, avoid: list[str]) -> list[str]:
    if not avoid:
        return []

    description = (movie.description or "").lower()
    genres = [genre.lower() for genre in get_movie_genre_names(movie)]

    found = []

    for raw_signal in avoid:
        signal = raw_signal.strip().lower()
        if not signal:
            continue

        if signal in genres:
            found.append(signal)
            continue

        keywords = AVOID_KEYWORDS.get(signal, [signal])
        if any(keyword.lower() in description for keyword in keywords):
            found.append(signal)

    return sorted(set(found))


def score_avoid_penalty(avoided_signals: list[str]) -> float:
    return min(len(avoided_signals) * 0.25, 0.75)


def build_pick_reason(
    primary_mood: str,
    secondary_moods: list[str],
    matched_genres: list[str],
    avoided_signals: list[str],
) -> str:
    readable_mood = primary_mood.replace("_", " ")

    reason = f"This pick matches your request for something {readable_mood}."

    if secondary_moods:
        readable_secondary = ", ".join(mood.replace("_", " ") for mood in secondary_moods)
        reason += f" It also fits the secondary mood signals: {readable_secondary}."

    if matched_genres:
        reason += f" Its genres line up with the mood through {', '.join(matched_genres)}."

    if avoided_signals:
        reason += (
            " It may still contain some elements you wanted to avoid, "
            f"so it was ranked lower for: {', '.join(avoided_signals)}."
        )

    return reason


def rank_movies_for_picker(
    db: Session,
    user_id: int,
    query_text: str,
    primary_mood: str,
    secondary_moods: list[str],
    avoid: list[str],
) -> list[dict]:
    query_embedding = embed_text(query_text)
    seen_movie_ids = get_seen_movie_ids(db, user_id)
    moods = [primary_mood, *secondary_moods]

    candidates = (
        db.query(MovieEmbedding)
        .join(Movie)
        .options(joinedload(MovieEmbedding.movie).joinedload(Movie.genre_list))
        .filter(~MovieEmbedding.movie_id.in_(seen_movie_ids))
        .all()
    )

    ranked = []

    for candidate in candidates:
        movie = candidate.movie
        movie_genres = get_movie_genre_names(movie)

        semantic_score = cosine_similarity(query_embedding, candidate.embedding)
        genre_score = score_genre_affinity(movie_genres, moods)
        rating_score = score_rating(movie)
        popularity_score = score_popularity(movie)

        avoided_signals = detect_avoided_signals(movie, avoid)
        avoid_penalty = score_avoid_penalty(avoided_signals)

        final_score = (
            0.60 * semantic_score
            + 0.22 * genre_score
            + 0.10 * rating_score
            + 0.08 * popularity_score
            - avoid_penalty
        )

        matched_genres = [
            genre
            for genre in movie_genres
            if any(MOOD_GENRE_AFFINITY.get(mood, {}).get(genre, 0.0) > 0 for mood in moods)
        ]

        ranked.append(
            {
                "movie": movie,
                "score": round(float(final_score), 4),
                "reason": build_pick_reason(
                    primary_mood=primary_mood,
                    secondary_moods=secondary_moods,
                    matched_genres=matched_genres,
                    avoided_signals=avoided_signals,
                ),
                "matched_genres": matched_genres,
                "avoided_signals": avoided_signals,
            }
        )

    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked


def create_movie_picker_session(
    db: Session,
    user_id: int,
    payload: MoviePickerSessionCreate,
) -> MoviePickerSession:
    primary_mood, secondary_moods, summary = interpret_picker_request(payload)

    query_text = build_query_text(
        primary_mood=primary_mood,
        secondary_moods=secondary_moods,
        prompt=payload.prompt,
        intensity=payload.intensity,
        avoid=payload.avoid,
    )

    ranked = rank_movies_for_picker(
        db=db,
        user_id=user_id,
        query_text=query_text,
        primary_mood=primary_mood,
        secondary_moods=secondary_moods,
        avoid=payload.avoid,
    )

    session = MoviePickerSession(
        user_id=user_id,
        mood=payload.mood,
        prompt=payload.prompt,
        interpreted_mood=primary_mood,
        secondary_moods=secondary_moods,
        summary=summary,
        ranked_movie_ids=[item["movie"].id for item in ranked],
        ranked_results=[
            {
                "movie_id": item["movie"].id,
                "score": item["score"],
                "reason": item["reason"],
                "matched_genres": item["matched_genres"],
                "avoided_signals": item["avoided_signals"],
            }
            for item in ranked
        ],
        current_cursor=0,
    )

    db.add(session)
    db.commit()
    db.refresh(session)

    return session


def build_pick_response(
    session: MoviePickerSession,
    movie: Movie,
    result: dict,
    cursor: int,
) -> MoviePickerPickOut:
    total = len(session.ranked_results)
    next_cursor = cursor + 1

    return MoviePickerPickOut(
        session_id=session.id,
        movie=movie,
        score=float(result.get("score", 0.0)),
        reason=result.get("reason", ""),
        interpreted_mood=session.interpreted_mood,
        secondary_moods=session.secondary_moods or [],
        summary=session.summary,
        matched_genres=result.get("matched_genres", []),
        avoided_signals=result.get("avoided_signals", []),
        cursor=cursor,
        next_cursor=next_cursor,
        has_more=next_cursor < total,
    )


def get_session_or_404(db: Session, user_id: int, session_id: str) -> MoviePickerSession:
    session = (
        db.query(MoviePickerSession)
        .filter(
            MoviePickerSession.id == session_id,
            MoviePickerSession.user_id == user_id,
        )
        .first()
    )

    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Movie picker session not found",
        )

    if session.expires_at < datetime.utcnow():
        raise HTTPException(
            status_code=status.HTTP_410_GONE,
            detail="Movie picker session expired",
        )

    return session


def get_pick_from_session(
    db: Session,
    session: MoviePickerSession,
    cursor: int,
) -> MoviePickerPickOut:
    if not session.ranked_results:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No matching movies found",
        )

    if cursor >= len(session.ranked_results):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No more movie picker results available for this session",
        )

    safe_cursor = cursor
    result = session.ranked_results[safe_cursor]
    movie_id = result["movie_id"]

    movie = (
        db.query(Movie)
        .options(joinedload(Movie.genre_list))
        .filter(Movie.id == movie_id)
        .first()
    )

    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Movie from picker session no longer exists",
        )

    return build_pick_response(
        session=session,
        movie=movie,
        result=result,
        cursor=safe_cursor,
    )


def get_initial_pick_for_session(
    db: Session,
    session: MoviePickerSession,
) -> MoviePickerPickOut:
    return get_pick_from_session(db=db, session=session, cursor=0)


def get_next_pick_for_session(
    db: Session,
    user_id: int,
    session_id: str,
) -> MoviePickerPickOut:
    session = get_session_or_404(db=db, user_id=user_id, session_id=session_id)

    next_cursor = session.current_cursor + 1

    if next_cursor >= len(session.ranked_results):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No more movie picker results available for this session",
        )

    session.current_cursor = next_cursor
    db.commit()
    db.refresh(session)

    return get_pick_from_session(db=db, session=session, cursor=next_cursor)