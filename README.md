# MovieIt

A full-stack Android app for logging, reviewing and discovering movies, built as my Bachelor's thesis project (Computer Science, University of Bucharest).

On the backend, a FastAPI service with PostgreSQL handles a catalog of around 300 movies loaded from TMDb. On top of the catalog sit several ML features: a semantic recommendation engine, a mood-based movie picker, image-to-movie visual search with CLIP and pgvector, and an automated review moderation pipeline.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_+_pgvector-4169E1?logo=postgresql&logoColor=white)
![Hugging Face](https://img.shields.io/badge/Hugging_Face-FFD21E?logo=huggingface&logoColor=black)

---

## Features

**Social movie diary**
- Reviews with ratings, a viewing diary, a watchlist and a "top movies" profile section
- Follow other users and see their activity in a feed
- Director pages
- Search with suggestions
- Roles: user, moderator, admin

**ML features**

| Feature | How it works |
|---|---|
| **Personal recommendations** | Builds a taste vector for each user from the embeddings of movies they rated 8/10 or higher and from their diary entries. Each review is weighted by its rating and by the sentiment of its text. The review text itself is also embedded and blended into the movie vector (35%), so what the user wrote shifts the profile. Candidates are ranked by **0.8 × cosine similarity + 0.2 × genre overlap**, and each recommendation comes with an explanation. |
| **Mood-based movie picker** | The user describes what they want in free text. A zero-shot classifier (`facebook/bart-large-mnli`) maps it to moods. Movies are then ranked by genre-mood affinity, rating and popularity, with penalties for things the user wants to avoid. Picks are served one at a time from a saved session. |
| **MovieIt Lens (visual search)** | Upload an image and get movies with a matching look and atmosphere. The image is embedded with CLIP (`openai/clip-vit-base-patch32`, 512 dimensions), compared against movie embeddings with an **HNSW index in pgvector** (cosine distance), then re-ranked using visual labels inferred by CLIP (genres and moods). |
| **Review moderation** | Spoiler detection (`roberta-base-finetuned-imdb-spoilers` plus plot-point patterns) and vulgarity detection (`unitary/toxic-bert`). Scores in a configurable "uncertain" band are sent to an LLM via the Groq API as a tiebreaker. |

## Architecture

```
┌──────────────────────────┐        REST / JWT        ┌──────────────────────────────┐
│ Android app              │ ───────────────────────▶ │ FastAPI backend              │
│ Kotlin · Jetpack Compose │                          │ 13 routers · SQLAlchemy      │
│ MVVM · Hilt · Retrofit   │ ◀─────────────────────── │ ML models preloaded at start │
└──────────────────────────┘                          └───────┬───────────────┬──────┘
                                                              │               │
                                                              ▼               ▼
                                              ┌────────────────────┐   ┌──────────────┐
                                              │ PostgreSQL         │   │ Groq LLM API │
                                              │ + pgvector (HNSW)  │   │ (tiebreaker) │
                                              └─────────▲──────────┘   └──────────────┘
                                                        │
                       ┌────────────────────────────────┴───────────┐
                       │ Data loading pipeline (scripts/)           │
                       │ TMDb API → genres → movies → directors →   │
                       │ text embeddings → CLIP visual embeddings   │
                       └────────────────────────────────────────────┘
```

All ML models (sentence embeddings, sentiment, zero-shot, spoiler, toxicity, CLIP) are loaded once when the server starts, so requests don't pay the model-loading cost.

## Data layer

- **PostgreSQL schema** managed with **Alembic**, with 15 migrations so far. It covers users, follows, movies, genres (many-to-many), directors, reviews, diary entries, watchlists, top movies, movie-picker sessions, and text and visual embeddings.
- **Referential integrity**: deleting a movie cascades to its reviews, diary entries, watchlist rows, genre links and embeddings.
- **Vector storage**:
  - Visual embeddings are stored as a `vector(512)` column in pgvector, with an `hnsw (vector_cosine_ops)` index.
  - Text embeddings (`paraphrase-multilingual-MiniLM-L12-v2`, multilingual) are stored with the text they were computed from, so they can be rebuilt when that text changes.
- **Data loading pipeline** (`app/scripts/populate_movies.py`):

```bash
python -m app.scripts.populate_movies --reset                 # wipe + full pipeline (~300 movies)
python -m app.scripts.populate_movies --pages 5 --source top_rated
python -m app.scripts.populate_movies --reset --skip-embeddings
python -m app.scripts.populate_movies --wipe-only
```

The `--reset` run goes through every step in order: genres → movies (TMDb) → directors → semantic embeddings → visual embeddings. It deletes in dependency order and keeps users and follows. The embedding scripts update existing rows instead of duplicating them, so they can be re-run safely.

## Running locally

**Requirements:** Python 3.11+, PostgreSQL with the `pgvector` extension, Android Studio (for the app), a [TMDb API key](https://www.themoviedb.org/settings/api) and, optionally, a [Groq API key](https://console.groq.com).

```bash
# Backend
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

cat > .env <<'EOF'
DATABASE_URL=postgresql://user:password@localhost:5432/movieit
SECRET_KEY=change-me
TMDB_API_KEY=...
GROQ_API_KEY=...          # optional: without it, borderline reviews are decided by the classifier alone
EOF

alembic upgrade head                                  # create the schema
python -m app.scripts.populate_movies --reset         # load the catalog and build embeddings
python -m app.scripts.populate_users                  # optional demo users
python run.py                                         # API at http://0.0.0.0:8000, docs at /docs
```

```properties
# Android: android/local.properties
base.url=http://<your-machine-ip>:8000/
```

Then open `android/` in Android Studio and run it on an emulator or device (minSdk 26).

> The first start of the backend downloads the Hugging Face models, which takes a few GB and several minutes.

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `SPOILER_LOW` / `SPOILER_HIGH` | 0.30 / 0.60 | Below LOW → not a spoiler, above HIGH → spoiler, in between → ask the LLM |
| `TOXICITY_LOW` / `TOXICITY_HIGH` | 0.35 / 0.75 | Same rule for vulgarity |
| `GROQ_MODEL` | `llama-3.1-8b-instant` | LLM used as tiebreaker |

## Project structure

```
android/                  Kotlin + Jetpack Compose app (MVVM, Hilt, Retrofit)
backend/
  alembic/versions/       schema migrations
  app/models/             SQLAlchemy models
  app/routers/            REST endpoints (auth, movies, reviews, diary, watchlist, feed, lens, …)
  app/services/           recommendations, movie picker, CLIP lens, moderation, TMDb client
  app/scripts/            data loading and embedding pipelines
  test_spoiler_detection.py, test_vulgarity_detection.py
```
