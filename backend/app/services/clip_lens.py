from functools import lru_cache
from io import BytesIO

import numpy as np
import torch
from PIL import Image
from transformers import CLIPModel, CLIPProcessor

from sqlalchemy.orm import Session, joinedload

from app.models.diary_entry import DiaryEntry
from app.models.movie import Movie
from app.models.movie_visual_embedding import MovieVisualEmbedding
from app.models.review import Review


CLIP_MODEL = "openai/clip-vit-base-patch32"

VISUAL_LABEL_PROMPTS = {
    # Cozy / soft everyday moods
    "cozy_home": {
        "label": "Cozy Home",
        "prompt": "a cozy warm home scene with blankets, soft light, coffee, books, candles, comfort, and quiet intimacy",
        "genres": ["Romance", "Comedy", "Drama", "Family"],
        "mood": "comfort",
        "modes": ["vibe", "cover"],
    },
    "warm_coffee": {
        "label": "Warm Coffee",
        "prompt": "a warm coffee shop or morning coffee scene with gentle light, calm atmosphere, comfort, and soft emotions",
        "genres": ["Romance", "Comedy", "Drama"],
        "mood": "comfort",
        "modes": ["vibe", "cover"],
    },
    "family_comfort": {
        "label": "Family Comfort",
        "prompt": "a wholesome family comfort movie atmosphere with warmth, kindness, home, safety, and gentle humor",
        "genres": ["Family", "Animation", "Comedy", "Drama"],
        "mood": "comfort",
        "modes": ["vibe", "poster"],
    },
    "soft_romance": {
        "label": "Soft Romance",
        "prompt": "a soft romantic movie scene with warm colors, intimacy, longing, gentle emotion, and heartfelt connection",
        "genres": ["Romance", "Drama", "Comedy"],
        "mood": "romantic",
        "modes": ["vibe", "cover", "poster"],
    },

    # Urban / night / noir
    "rainy_noir": {
        "label": "Rainy Noir",
        "prompt": "a rainy dark noir movie scene at night with wet streets, shadows, neon reflections, mystery, and crime",
        "genres": ["Thriller", "Crime", "Drama"],
        "mood": "tense",
        "modes": ["vibe", "poster"],
    },
    "neon_city": {
        "label": "Neon City",
        "prompt": "a neon city night scene with colorful lights, futuristic streets, urban isolation, technology, and cinematic atmosphere",
        "genres": ["Science Fiction", "Thriller", "Action"],
        "mood": "mind_bending",
        "modes": ["vibe", "cover", "poster"],
    },
    "lonely_city": {
        "label": "Lonely City",
        "prompt": "a lonely urban city scene with empty streets, isolation, muted colors, emotional distance, and quiet sadness",
        "genres": ["Drama", "Thriller", "Romance"],
        "mood": "melancholic",
        "modes": ["vibe", "cover"],
    },
    "street_realism": {
        "label": "Street Realism",
        "prompt": "a realistic street scene with everyday life, social tension, raw emotion, city details, and grounded drama",
        "genres": ["Drama", "Crime"],
        "mood": "reflective",
        "modes": ["vibe", "cover"],
    },

    # Nature / escape
    "sunny_nature": {
        "label": "Sunny Nature",
        "prompt": "a bright sunny nature scene with fields, trees, daylight, freedom, optimism, and peaceful escape",
        "genres": ["Adventure", "Family", "Drama"],
        "mood": "escapist",
        "modes": ["vibe", "cover"],
    },
    "forest_fantasy": {
        "label": "Forest Fantasy",
        "prompt": "a magical forest fantasy scene with glowing light, mystery, wonder, fairy tale atmosphere, and imagination",
        "genres": ["Fantasy", "Adventure", "Family"],
        "mood": "escapist",
        "modes": ["vibe", "cover", "poster"],
    },
    "ocean_adventure": {
        "label": "Ocean Adventure",
        "prompt": "an ocean adventure scene with waves, boats, open water, discovery, danger, freedom, and exploration",
        "genres": ["Adventure", "Drama", "Action"],
        "mood": "adventurous",
        "modes": ["vibe", "poster"],
    },
    "mountain_epic": {
        "label": "Mountain Epic",
        "prompt": "an epic mountain landscape with dramatic scale, survival, journey, courage, wilderness, and cinematic grandeur",
        "genres": ["Adventure", "Drama", "Action"],
        "mood": "adventurous",
        "modes": ["vibe", "poster"],
    },

    # Work / life
    "office_burnout": {
        "label": "Office Burnout",
        "prompt": "a stressful office or desk scene with papers, screens, deadlines, exhaustion, ambition, and modern work pressure",
        "genres": ["Drama", "Comedy"],
        "mood": "reflective",
        "modes": ["vibe"],
    },
    "academic_study": {
        "label": "Academic Study",
        "prompt": "an academic study scene with books, notes, libraries, learning, ambition, introspection, and coming of age energy",
        "genres": ["Drama", "Romance", "Comedy"],
        "mood": "thoughtful",
        "modes": ["vibe", "cover"],
    },
    "creative_artist": {
        "label": "Creative Artist",
        "prompt": "an artistic creative scene with music, painting, writing, performance, passion, self expression, and emotional ambition",
        "genres": ["Music", "Drama", "Romance"],
        "mood": "emotional",
        "modes": ["vibe", "cover", "poster"],
    },
    "sports_energy": {
        "label": "Sports Energy",
        "prompt": "a sports energy scene with movement, competition, training, teamwork, ambition, sweat, and personal victory",
        "genres": ["Drama", "Action", "Documentary"],
        "mood": "energetic",
        "modes": ["vibe", "poster"],
    },

    # Emotional atmosphere
    "melancholic_room": {
        "label": "Melancholic Room",
        "prompt": "a melancholic room scene with dim light, loneliness, silence, memory, sadness, and intimate emotional weight",
        "genres": ["Drama", "Romance"],
        "mood": "melancholic",
        "modes": ["vibe", "cover"],
    },
    "nostalgic_memory": {
        "label": "Nostalgic Memory",
        "prompt": "a nostalgic memory scene with old photographs, warm grain, childhood, the past, longing, and bittersweet emotion",
        "genres": ["Drama", "Family", "Romance"],
        "mood": "nostalgic",
        "modes": ["vibe", "cover"],
    },
    "hopeful_light": {
        "label": "Hopeful Light",
        "prompt": "a hopeful cinematic image with bright light, open space, renewal, healing, optimism, and emotional release",
        "genres": ["Drama", "Family", "Adventure"],
        "mood": "inspiring",
        "modes": ["vibe", "cover", "poster"],
    },
    "lonely_minimalism": {
        "label": "Lonely Minimalism",
        "prompt": "a minimal lonely image with empty space, quiet composition, isolation, emotional distance, and restrained sadness",
        "genres": ["Drama", "Romance"],
        "mood": "melancholic",
        "modes": ["vibe", "cover"],
    },

    # Strong genre signals
    "horror_shadow": {
        "label": "Horror Shadow",
        "prompt": "a horror image with deep shadows, fear, haunted atmosphere, darkness, danger, and supernatural tension",
        "genres": ["Horror", "Thriller"],
        "mood": "intense",
        "modes": ["vibe", "cover", "poster"],
    },
    "crime_gritty": {
        "label": "Gritty Crime",
        "prompt": "a gritty crime movie image with danger, violence, detectives, criminals, urban decay, and moral tension",
        "genres": ["Crime", "Thriller", "Drama"],
        "mood": "dark",
        "modes": ["vibe", "poster"],
    },
    "sci_fi_neon": {
        "label": "Sci-Fi Neon",
        "prompt": "a science fiction neon image with futuristic technology, space, robots, artificial intelligence, and cosmic mystery",
        "genres": ["Science Fiction", "Thriller", "Action"],
        "mood": "mind_bending",
        "modes": ["cover", "poster", "vibe"],
    },
    "fantasy_magic": {
        "label": "Fantasy Magic",
        "prompt": "a fantasy magic image with spells, glowing symbols, mythical worlds, castles, creatures, and wonder",
        "genres": ["Fantasy", "Adventure", "Family"],
        "mood": "escapist",
        "modes": ["cover", "poster", "vibe"],
    },
    "action_chaos": {
        "label": "Action Chaos",
        "prompt": "an action chaos image with explosions, cars, weapons, speed, danger, impact, and blockbuster intensity",
        "genres": ["Action", "Thriller", "Adventure"],
        "mood": "energetic",
        "modes": ["poster", "vibe"],
    },
    "historical_epic": {
        "label": "Historical Epic",
        "prompt": "a historical epic image with period costumes, old architecture, kingdoms, war, dramatic scale, and legacy",
        "genres": ["History", "War", "Drama"],
        "mood": "heavy",
        "modes": ["cover", "poster"],
    },
    "western_dust": {
        "label": "Western Dust",
        "prompt": "a western movie image with desert dust, cowboys, horses, frontier towns, guns, loneliness, and grit",
        "genres": ["Western", "Drama", "Action"],
        "mood": "gritty",
        "modes": ["cover", "poster", "vibe"],
    },

    # Covers / albums
    "minimalist_album": {
        "label": "Minimalist Album",
        "prompt": "a minimalist album cover with simple composition, abstract shapes, clean design, negative space, and subtle emotion",
        "genres": ["Drama", "Documentary"],
        "mood": "reflective",
        "modes": ["cover"],
    },
    "psychedelic_album": {
        "label": "Psychedelic Album",
        "prompt": "a psychedelic album cover with surreal colors, abstract patterns, dreamlike visuals, music energy, and altered reality",
        "genres": ["Music", "Science Fiction", "Fantasy"],
        "mood": "mind_bending",
        "modes": ["cover"],
    },
    "dark_album_cover": {
        "label": "Dark Album Cover",
        "prompt": "a dark album cover with moody lighting, emotional intensity, shadows, mystery, sadness, and dramatic atmosphere",
        "genres": ["Drama", "Thriller", "Music"],
        "mood": "dark",
        "modes": ["cover"],
    },
    "romantic_book_cover": {
        "label": "Romantic Book Cover",
        "prompt": "a romantic book cover with soft colors, elegant typography, intimacy, longing, emotional warmth, and relationships",
        "genres": ["Romance", "Drama", "Comedy"],
        "mood": "romantic",
        "modes": ["cover"],
    },
    "literary_drama_cover": {
        "label": "Literary Drama Cover",
        "prompt": "a literary drama book cover with serious composition, emotional restraint, human relationships, memory, and introspection",
        "genres": ["Drama", "Romance", "History"],
        "mood": "reflective",
        "modes": ["cover"],
    },
    "cyberpunk_cover": {
        "label": "Cyberpunk Cover",
        "prompt": "a cyberpunk cover with neon colors, futuristic city, technology, hackers, dystopia, and stylish science fiction",
        "genres": ["Science Fiction", "Thriller", "Action"],
        "mood": "mind_bending",
        "modes": ["cover"],
    },

    # Poster-specific
    "blockbuster_poster": {
        "label": "Blockbuster Poster",
        "prompt": "a blockbuster movie poster with dramatic faces, action composition, explosions, high stakes, and cinematic spectacle",
        "genres": ["Action", "Adventure", "Thriller"],
        "mood": "energetic",
        "modes": ["poster"],
    },
    "indie_drama_poster": {
        "label": "Indie Drama Poster",
        "prompt": "an indie drama movie poster with restrained composition, human emotion, muted colors, intimacy, and realism",
        "genres": ["Drama", "Romance"],
        "mood": "reflective",
        "modes": ["poster"],
    },
    "romantic_comedy_poster": {
        "label": "Romantic Comedy Poster",
        "prompt": "a romantic comedy movie poster with bright colors, smiling people, playful romance, charm, and lighthearted energy",
        "genres": ["Romance", "Comedy"],
        "mood": "playful",
        "modes": ["poster"],
    },
    "superhero_poster": {
        "label": "Superhero Poster",
        "prompt": "a superhero movie poster with heroic figures, costumes, action poses, city danger, powers, and epic spectacle",
        "genres": ["Action", "Science Fiction", "Adventure"],
        "mood": "energetic",
        "modes": ["poster"],
    },
    "animated_poster": {
        "label": "Animated Poster",
        "prompt": "an animated movie poster with colorful characters, expressive faces, family adventure, fantasy, humor, and warmth",
        "genres": ["Animation", "Family", "Adventure", "Comedy"],
        "mood": "comfort",
        "modes": ["poster"],
    },
}

@lru_cache(maxsize=1)
def get_clip_model() -> CLIPModel:
    model = CLIPModel.from_pretrained(CLIP_MODEL)
    model.eval()
    return model


@lru_cache(maxsize=1)
def get_clip_processor() -> CLIPProcessor:
    return CLIPProcessor.from_pretrained(CLIP_MODEL)


def load_image_from_bytes(image_bytes: bytes) -> Image.Image:
    image = Image.open(BytesIO(image_bytes))
    return image.convert("RGB")


def normalize_vector(vector: np.ndarray) -> list[float]:
    vector = np.asarray(vector, dtype=np.float32).squeeze()

    if vector.ndim != 1:
        vector = vector.reshape(-1)

    norm = np.linalg.norm(vector)
    if norm == 0:
        return vector.astype(float).tolist()

    return (vector / norm).astype(float).tolist()


def encode_image_bytes(image_bytes: bytes) -> list[float]:
    image = load_image_from_bytes(image_bytes)

    processor = get_clip_processor()
    model = get_clip_model()

    inputs = processor(images=image, return_tensors="pt")

    with torch.no_grad():
        image_features = model.get_image_features(**inputs)

    if hasattr(image_features, "pooler_output"):
        image_features = image_features.pooler_output

    vector = image_features.detach().cpu().numpy()
    return normalize_vector(vector)


def encode_text_labels(labels: list[str]) -> list[list[float]]:
    processor = get_clip_processor()
    model = get_clip_model()

    inputs = processor(text=labels, return_tensors="pt", padding=True)

    with torch.no_grad():
        text_features = model.get_text_features(**inputs)

    if hasattr(text_features, "pooler_output"):
        text_features = text_features.pooler_output

    vectors = text_features.detach().cpu().numpy()
    return [normalize_vector(vector) for vector in vectors]

def cosine_similarity_vector(a: list[float], b: list[float]) -> float:
    vec_a = np.array(a, dtype=np.float32)
    vec_b = np.array(b, dtype=np.float32)

    denominator = np.linalg.norm(vec_a) * np.linalg.norm(vec_b)
    if denominator == 0:
        return 0.0

    return float(np.dot(vec_a, vec_b) / denominator)


@lru_cache(maxsize=None)
def get_visual_label_embeddings(mode: str) -> dict[str, list[float]]:
    keys = [
        key
        for key, metadata in VISUAL_LABEL_PROMPTS.items()
        if mode in metadata.get("modes", [])
    ]

    prompts = [VISUAL_LABEL_PROMPTS[key]["prompt"] for key in keys]
    embeddings = encode_text_labels(prompts)

    return {
        key: embedding
        for key, embedding in zip(keys, embeddings)
    }


def classify_image_labels(
    image_embedding: list[float],
    mode: str,
    top_k: int = 3,
) -> list[dict]:
    label_embeddings = get_visual_label_embeddings(mode)

    scored = []
    for key, label_embedding in label_embeddings.items():
        metadata = VISUAL_LABEL_PROMPTS[key]
        score = cosine_similarity_vector(image_embedding, label_embedding)

        scored.append(
            {
                "key": key,
                "label": metadata["label"],
                "score": round(score, 4),
                "genres": metadata["genres"],
                "mood": metadata["mood"],
            }
        )

    scored.sort(key=lambda item: item["score"], reverse=True)
    return scored[:top_k]

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


def collect_genres_from_labels(labels: list[dict]) -> list[str]:
    genre_scores: dict[str, float] = {}

    for label in labels:
        for genre in label["genres"]:
            genre_scores[genre] = genre_scores.get(genre, 0.0) + float(label["score"])

    return [
        genre
        for genre, _ in sorted(
            genre_scores.items(),
            key=lambda item: item[1],
            reverse=True,
        )
    ]


def get_movie_genre_names(movie: Movie) -> list[str]:
    return [genre.name for genre in getattr(movie, "genre_list", []) or []]


def build_lens_title(labels: list[dict]) -> str:
    if not labels:
        return "A cinematic match"

    return labels[0]["label"]


def build_lens_description(labels: list[dict]) -> str:
    if not labels:
        return "We found movies with a similar visual atmosphere."

    top = labels[0]
    genres = ", ".join(top["genres"][:3])
    return f"This image has a {top['label'].lower()} atmosphere, close to {genres} movies."


def score_genre_overlap(movie_genres: list[str], target_genres: list[str]) -> float:
    if not movie_genres or not target_genres:
        return 0.0

    overlap = set(movie_genres) & set(target_genres)
    return len(overlap) / max(len(set(target_genres)), 1)


def rank_lens_recommendations(
    db: Session,
    user_id: int,
    image_embedding: list[float],
    visual_labels: list[dict],
    mode: str,
    limit: int = 5,
) -> list[dict]:
    seen_movie_ids = get_seen_movie_ids(db, user_id)
    target_genres = collect_genres_from_labels(visual_labels)

    candidate_limit = max(limit * 20, 100)

    query = (
        db.query(MovieVisualEmbedding)
        .join(Movie)
        .options(joinedload(MovieVisualEmbedding.movie).joinedload(Movie.genre_list))
    )

    if seen_movie_ids:
        query = query.filter(~MovieVisualEmbedding.movie_id.in_(seen_movie_ids))

    candidates = (
        query
        .order_by(MovieVisualEmbedding.embedding.cosine_distance(image_embedding))
        .limit(candidate_limit)
        .all()
    )

    if not candidates:
        raise ValueError("No visual movie embeddings are available yet.")

    ranked = []

    for candidate in candidates:
        movie = candidate.movie
        movie_genres = get_movie_genre_names(movie)

        visual_similarity = cosine_similarity_vector(image_embedding, candidate.embedding)
        genre_overlap = score_genre_overlap(movie_genres, target_genres)

        if mode == "vibe":
            final_score = (0.55 * genre_overlap) + (0.35 * visual_similarity)
        elif mode == "cover":
            final_score = (0.65 * visual_similarity) + (0.25 * genre_overlap)
        else:  # poster
            final_score = (0.60 * visual_similarity) + (0.30 * genre_overlap)

        if movie.avg_rating:
            final_score += 0.05 * min(movie.avg_rating / 10.0, 1.0)

        if movie.popularity:
            final_score += 0.05 * min(movie.popularity / 1000.0, 1.0)

        matched_genres = [
            genre for genre in movie_genres if genre in target_genres
        ]

        reason = (
            f"This movie matches the {visual_labels[0]['label'].lower()} visual vibe"
            if visual_labels
            else "This movie matches the visual atmosphere of your image"
        )

        if matched_genres:
            reason += f" and shares {', '.join(matched_genres)} elements."

        ranked.append(
            {
                "movie": movie,
                "score": round(float(final_score), 4),
                "reason": reason,
                "visual_similarity": round(float(visual_similarity), 4),
                "matched_genres": matched_genres,
            }
        )

    ranked.sort(key=lambda item: item["score"], reverse=True)
    results = ranked[:limit]

    if not results:
        raise ValueError("No matching lens recommendations were found.")

    return results


def analyze_lens_image(
    db: Session,
    user_id: int,
    image_bytes: bytes,
    mode: str,
    limit: int = 5,
) -> dict:
    if mode not in {"vibe", "cover", "poster"}:
        raise ValueError("Mode must be one of: vibe, cover, poster")

    image_embedding = encode_image_bytes(image_bytes)
    visual_labels = classify_image_labels(image_embedding, mode=mode, top_k=3)
    matched_genres = collect_genres_from_labels(visual_labels)

    recommendations = rank_lens_recommendations(
        db=db,
        user_id=user_id,
        image_embedding=image_embedding,
        visual_labels=visual_labels,
        mode=mode,
        limit=limit,
    )

    return {
        "mode": mode,
        "title": build_lens_title(visual_labels),
        "description": build_lens_description(visual_labels),
        "visual_labels": visual_labels,
        "matched_genres": matched_genres,
        "recommendations": recommendations,
    }