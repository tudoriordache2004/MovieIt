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

SAFE_GENRES = {"Animation", "Comedy", "Family", "Romance", "Adventure"}
DARK_GENRES = {"Crime", "Horror", "Thriller", "War"}
INTENSE_GENRES = {"Action", "Crime", "Horror", "Thriller", "War", "Western"}

GENRE_MOOD_MAP = {
    "Action": "energetic",
    "Adventure": "escapist",
    "Animation": "comfort",
    "Comedy": "playful",
    "Crime": "dark",
    "Documentary": "curious",
    "Drama": "reflective",
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

MOOD_COMPATIBILITY = {
    "bright": {"comfort", "playful", "escapist", "romantic", "inspiring"},
    "comfort": {"comfort", "playful", "romantic", "inspiring"},
    "escapist": {"escapist", "comfort", "playful", "inspiring"},
    "hopeful": {"inspiring", "comfort", "playful", "escapist"},
    "inspiring": {"inspiring", "escapist", "comfort", "reflective"},
    "playful": {"playful", "comfort", "escapist"},
    "romantic": {"romantic", "comfort", "reflective"},
    "reflective": {"reflective", "emotional", "melancholic"},
    "melancholic": {"melancholic", "reflective", "romantic"},
    "dark": {"dark", "tense", "intense", "gritty"},
    "intense": {"intense", "tense", "dark", "energetic"},
    "mind_bending": {"mind_bending", "tense", "escapist"},
}

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
        "moods": ["bright", "hopeful", "comfort", "escapist"],
        "positive_genres": ["Adventure", "Family", "Animation", "Comedy"],
        "soft_genres": ["Drama", "Romance", "Fantasy"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "bright",
        "intensity": "low",
    },
    "sunny_flower": {
        "label": "Sunny Flower",
        "prompt": "a close bright sunny flower image with yellow petals, green leaves, daylight, gentle warmth, optimism, and peaceful nature",
        "genres": ["Family", "Animation", "Adventure", "Comedy"],
        "mood": "comfort",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["bright", "comfort", "hopeful", "playful"],
        "positive_genres": ["Family", "Animation", "Comedy", "Adventure"],
        "soft_genres": ["Romance", "Drama", "Fantasy"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "bright",
        "intensity": "low",
    },
    "golden_field": {
        "label": "Golden Field",
        "prompt": "a golden sunny field or meadow with warm yellow light, open space, calm optimism, summer, and gentle adventure",
        "genres": ["Adventure", "Family", "Drama", "Romance"],
        "mood": "inspiring",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["bright", "hopeful", "escapist", "romantic"],
        "positive_genres": ["Adventure", "Family", "Romance", "Animation"],
        "soft_genres": ["Drama", "Fantasy"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "bright",
        "intensity": "low",
    },
    "green_meadow": {
        "label": "Green Meadow",
        "prompt": "a fresh green meadow or garden in daylight with plants, peaceful nature, safety, softness, and quiet joy",
        "genres": ["Family", "Adventure", "Animation", "Drama"],
        "mood": "comfort",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["comfort", "bright", "escapist", "hopeful"],
        "positive_genres": ["Family", "Animation", "Adventure", "Comedy"],
        "soft_genres": ["Drama", "Romance"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "bright",
        "intensity": "low",
    },
    "warm_sunset": {
        "label": "Warm Sunset",
        "prompt": "a warm sunset image with orange golden light, nostalgia, gentle emotion, calm beauty, and reflective optimism",
        "genres": ["Romance", "Drama", "Adventure", "Family"],
        "mood": "romantic",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["romantic", "reflective", "hopeful", "comfort"],
        "positive_genres": ["Romance", "Drama", "Family", "Adventure"],
        "soft_genres": ["Animation", "Comedy"],
        "negative_genres": ["Horror", "Crime", "War"],
        "brightness": "warm",
        "intensity": "low",
    },
    "bright_playful_colors": {
        "label": "Bright Playful Colors",
        "prompt": "a colorful bright cheerful image with playful colors, friendly energy, joy, humor, animation, and light adventure",
        "genres": ["Animation", "Comedy", "Family", "Adventure"],
        "mood": "playful",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["playful", "comfort", "bright", "escapist"],
        "positive_genres": ["Animation", "Comedy", "Family", "Adventure"],
        "soft_genres": ["Fantasy", "Romance"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "bright",
        "intensity": "low",
    },
    "pastel_daydream": {
        "label": "Pastel Daydream",
        "prompt": "a pastel soft dreamy image with gentle colors, innocence, imagination, calm wonder, and comforting fantasy",
        "genres": ["Animation", "Family", "Fantasy", "Romance"],
        "mood": "comfort",
        "modes": ["vibe", "cover", "poster"],
        "moods": ["comfort", "romantic", "escapist", "hopeful"],
        "positive_genres": ["Animation", "Family", "Fantasy", "Romance"],
        "soft_genres": ["Adventure", "Comedy", "Drama"],
        "negative_genres": ["Horror", "Crime", "Thriller", "War"],
        "brightness": "soft",
        "intensity": "low",
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
                "moods": metadata.get("moods", [metadata["mood"]]),
                "positive_genres": metadata.get("positive_genres", metadata["genres"]),
                "soft_genres": metadata.get("soft_genres", []),
                "negative_genres": metadata.get("negative_genres", []),
                "brightness": metadata.get("brightness"),
                "intensity": metadata.get("intensity"),
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
        label_genres = (
            label.get("positive_genres")
            or label.get("genres")
            or []
        )
        soft_genres = label.get("soft_genres") or []

        for genre in label_genres:
            genre_scores[genre] = genre_scores.get(genre, 0.0) + float(label["score"])

        for genre in soft_genres:
            genre_scores[genre] = genre_scores.get(genre, 0.0) + (0.55 * float(label["score"]))

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


def collect_weighted_label_values(labels: list[dict], field_name: str, fallback_field: str | None = None) -> dict[str, float]:
    values: dict[str, float] = {}

    for label in labels:
        label_score = max(float(label.get("score", 0.0)), 0.01)
        raw_values = label.get(field_name)

        if not raw_values and fallback_field:
            raw_values = label.get(fallback_field)

        if isinstance(raw_values, str):
            raw_values = [raw_values]

        for value in raw_values or []:
            values[value] = values.get(value, 0.0) + label_score

    return values


def score_weighted_overlap(movie_genres: list[str], weighted_targets: dict[str, float]) -> float:
    if not movie_genres or not weighted_targets:
        return 0.0

    movie_genre_set = set(movie_genres)
    total_weight = sum(weighted_targets.values())
    if total_weight <= 0:
        return 0.0

    matched_weight = sum(
        weight for genre, weight in weighted_targets.items()
        if genre in movie_genre_set
    )
    return min(matched_weight / total_weight, 1.0)


def infer_movie_moods(movie_genres: list[str]) -> set[str]:
    return {
        mood
        for genre in movie_genres
        if (mood := GENRE_MOOD_MAP.get(genre))
    }


def score_mood_compatibility(movie_genres: list[str], labels: list[dict]) -> float:
    movie_moods = infer_movie_moods(movie_genres)
    if not movie_moods:
        return 0.0

    target_moods = collect_weighted_label_values(labels, "moods", fallback_field="mood")
    if not target_moods:
        return 0.0

    total_weight = sum(target_moods.values())
    if total_weight <= 0:
        return 0.0

    matched_weight = 0.0
    for target_mood, weight in target_moods.items():
        compatible_moods = MOOD_COMPATIBILITY.get(target_mood, {target_mood})
        if movie_moods & compatible_moods:
            matched_weight += weight

    return min(matched_weight / total_weight, 1.0)


def score_safety_fit(movie_genres: list[str], labels: list[dict]) -> float:
    movie_genre_set = set(movie_genres)
    has_safe_genre = bool(movie_genre_set & SAFE_GENRES)
    has_dark_genre = bool(movie_genre_set & DARK_GENRES)

    wants_safe = any(
        label.get("brightness") in {"bright", "soft", "warm"}
        or label.get("intensity") == "low"
        or label.get("mood") in {"comfort", "inspiring", "playful"}
        for label in labels
    )

    if not wants_safe:
        return 0.0

    if has_safe_genre and not has_dark_genre:
        return 1.0
    if has_safe_genre:
        return 0.45
    if has_dark_genre:
        return 0.0
    return 0.25


def score_negative_genre_penalty(movie_genres: list[str], labels: list[dict]) -> float:
    movie_genre_set = set(movie_genres)
    weighted_negative_genres = collect_weighted_label_values(labels, "negative_genres")

    if not movie_genre_set or not weighted_negative_genres:
        return 0.0

    total_weight = sum(weighted_negative_genres.values())
    if total_weight <= 0:
        return 0.0

    matched_weight = sum(
        weight for genre, weight in weighted_negative_genres.items()
        if genre in movie_genre_set
    )
    return min(matched_weight / total_weight, 1.0)


def score_dark_mismatch_penalty(movie_genres: list[str], labels: list[dict]) -> float:
    movie_genre_set = set(movie_genres)
    if not movie_genre_set & INTENSE_GENRES:
        return 0.0

    bright_low_intensity = any(
        label.get("brightness") in {"bright", "soft", "warm"}
        and label.get("intensity") == "low"
        for label in labels
    )

    if not bright_low_intensity:
        return 0.0

    intense_count = len(movie_genre_set & INTENSE_GENRES)
    return min(0.35 + (0.15 * intense_count), 1.0)


def score_dark_text_penalty(movie: Movie, labels: list[dict]) -> float:
    bright_low_intensity = any(
        label.get("brightness") in {"bright", "soft", "warm"}
        and label.get("intensity") == "low"
        for label in labels
    )

    if not bright_low_intensity:
        return 0.0

    source_text = f"{movie.title or ''} {movie.description or ''}".lower()
    dark_terms = {
        "assassin", "blood", "curse", "dark", "death", "demon", "evil",
        "gothic", "haunted", "killer", "maleficent", "murder", "nightmare",
        "revenge", "shadow", "terror", "violent", "war", "witch",
    }
    matched_terms = sum(1 for term in dark_terms if term in source_text)

    if matched_terms == 0:
        return 0.0

    return min(0.25 + (0.12 * matched_terms), 1.0)


def get_visual_similarity_threshold(mode: str) -> float:
    if mode == "poster":
        return 0.50
    if mode == "cover":
        return 0.47
    return 0.48


def score_low_similarity_penalty(visual_similarity: float, mode: str) -> float:
    threshold = get_visual_similarity_threshold(mode)
    if visual_similarity >= threshold:
        return 0.0

    return min((threshold - visual_similarity) / 0.18, 1.0)


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
    weighted_positive_genres = collect_weighted_label_values(
        visual_labels,
        "positive_genres",
        fallback_field="genres",
    )
    weighted_soft_genres = collect_weighted_label_values(visual_labels, "soft_genres")

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
        positive_genre_fit = score_weighted_overlap(movie_genres, weighted_positive_genres)
        soft_genre_fit = score_weighted_overlap(movie_genres, weighted_soft_genres)
        genre_fit = min(positive_genre_fit + (0.45 * soft_genre_fit), 1.0)
        mood_fit = score_mood_compatibility(movie_genres, visual_labels)
        safety_fit = score_safety_fit(movie_genres, visual_labels)

        negative_penalty = score_negative_genre_penalty(movie_genres, visual_labels)
        dark_mismatch_penalty = score_dark_mismatch_penalty(movie_genres, visual_labels)
        dark_text_penalty = score_dark_text_penalty(movie, visual_labels)
        low_similarity_penalty = score_low_similarity_penalty(visual_similarity, mode)

        if mode == "vibe":
            final_score = (
                (0.43 * visual_similarity)
                + (0.22 * genre_fit)
                + (0.22 * mood_fit)
                + (0.08 * safety_fit)
            )
            final_score -= (
                (0.18 * negative_penalty)
                + (0.12 * dark_mismatch_penalty)
                + (0.12 * dark_text_penalty)
                + (0.10 * low_similarity_penalty)
            )
        elif mode == "cover":
            final_score = (
                (0.52 * visual_similarity)
                + (0.18 * genre_fit)
                + (0.18 * mood_fit)
                + (0.06 * safety_fit)
            )
            final_score -= (
                (0.14 * negative_penalty)
                + (0.10 * dark_mismatch_penalty)
                + (0.09 * dark_text_penalty)
                + (0.08 * low_similarity_penalty)
            )
        else:  # poster
            final_score = (
                (0.50 * visual_similarity)
                + (0.20 * genre_fit)
                + (0.18 * mood_fit)
                + (0.07 * safety_fit)
            )
            final_score -= (
                (0.14 * negative_penalty)
                + (0.10 * dark_mismatch_penalty)
                + (0.08 * dark_text_penalty)
                + (0.08 * low_similarity_penalty)
            )

        if movie.avg_rating:
            final_score += 0.03 * min(movie.avg_rating / 10.0, 1.0)

        if movie.popularity:
            final_score += 0.02 * min(movie.popularity / 1000.0, 1.0)

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
                "score": round(float(max(final_score, 0.0)), 4),
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