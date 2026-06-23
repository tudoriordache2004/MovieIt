# backend/scripts/populate_movies.py
"""
Script pentru popularea bazei de date cu date din TMDB API

Usage:
  python -m app.scripts.populate_movies [--pages 15] [--source all_time|popular|top_rated]
  python -m app.scripts.populate_movies --reset [--pages 15] [--skip-embeddings]

--reset wipes all movie-related data first (preserves users/follows), then
runs the full pipeline: genres → movies → directors → semantic embeddings →
visual embeddings.

--wipe-only clears data without repopulating.
"""
import sys
import os
from pathlib import Path

backend_path = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_path))

from sqlalchemy import text
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models.movie import Movie
from app.models.genre import Genre, MovieGenre
from app.services.tmdb import tmdb_service
import argparse
import time


# ---------------------------------------------------------------------------
# Wipe
# ---------------------------------------------------------------------------

def wipe_all(db: Session) -> None:
    print("🗑  Wiping all movie-related data...")

    # Sessions store ranked_movie_ids as JSON — not FK-linked to movies.
    db.execute(text("DELETE FROM movie_picker_sessions"))
    print("   ✓ movie_picker_sessions")

    # movie_directors has no ondelete=CASCADE on movie_id, must go before movies.
    db.execute(text("DELETE FROM movie_directors"))
    print("   ✓ movie_directors")

    # Deleting movies cascades to: reviews, diary_entries, watchlist,
    # user_top_movies, movie_genres, movie_embeddings, movie_visual_embeddings.
    db.execute(text("DELETE FROM movies"))
    print("   ✓ movies (cascade: reviews, diary, watchlist, top_movies, genres, embeddings)")

    db.execute(text("DELETE FROM directors"))
    print("   ✓ directors")

    db.execute(text("DELETE FROM genres"))
    print("   ✓ genres")

    db.commit()
    print("   Wipe complete.\n")


# ---------------------------------------------------------------------------
# Genres
# ---------------------------------------------------------------------------

def get_or_create_genre(db: Session, genre_name: str) -> Genre:
    genre = db.query(Genre).filter(Genre.name == genre_name).first()
    if not genre:
        genre = Genre(name=genre_name)
        db.add(genre)
        db.commit()
        db.refresh(genre)
    return genre


def sync_genres(db: Session):
    print("📚 Syncronizare genuri din TMDB...")
    try:
        tmdb_genres = tmdb_service.get_genres()
        created_count = 0

        for tmdb_genre in tmdb_genres:
            genre_name = tmdb_genre.get("name")
            if genre_name:
                existing = db.query(Genre).filter(Genre.name == genre_name).first()
                if not existing:
                    genre = Genre(name=genre_name)
                    db.add(genre)
                    created_count += 1

        db.commit()
        print(f"  ✓ Genuri sincronizate: {created_count} noi, {len(tmdb_genres)} total")
        return True
    except Exception as e:
        db.rollback()
        print(f"  ✗ Eroare la sincronizare genuri: {e}")
        return False


# ---------------------------------------------------------------------------
# Movies
# ---------------------------------------------------------------------------

def populate_movies(db: Session, source: str = "all_time", num_pages: int = 5):
    print(f"🎬 Populare filme din TMDB ({source}, {num_pages} pagini)...")

    movies_created = 0
    movies_skipped = 0
    errors = 0

    for page in range(1, num_pages + 1):
        print(f"\n📄 Pagina {page}/{num_pages}...")

        try:
            if source == "top_rated":
                response = tmdb_service.get_top_rated_movies(page=page)
            elif source == "all_time":
                response = tmdb_service.get_all_time_popular_movies(page=page)
            else:
                response = tmdb_service.get_popular_movies(page=page)

            movies_data = response.get("results", [])

            for tmdb_movie_data in movies_data:
                try:
                    tmdb_id = tmdb_movie_data.get("id")

                    existing = db.query(Movie).filter(Movie.tmdb_id == tmdb_id).first()
                    if existing:
                        movies_skipped += 1
                        continue

                    movie_details = tmdb_service.get_movie_details(tmdb_id)
                    movie_data = tmdb_service.parse_movie_data(movie_details)

                    movie = Movie(
                        tmdb_id=movie_data["tmdb_id"],
                        title=movie_data["title"],
                        description=movie_data["description"],
                        release_date=movie_data["release_date"],
                        poster_url=movie_data["poster_url"],
                        popularity=movie_data["popularity"]
                    )
                    db.add(movie)
                    db.flush()

                    tmdb_genres = movie_details.get("genres", [])
                    for tmdb_genre in tmdb_genres:
                        genre_name = tmdb_genre.get("name")
                        if genre_name:
                            genre = get_or_create_genre(db, genre_name)
                            db.add(MovieGenre(movie_id=movie.id, genre_id=genre.id))

                    db.commit()
                    movies_created += 1
                    print(f"  ✓ {movie.title} ({tmdb_id})")
                    time.sleep(0.25)

                except Exception as e:
                    db.rollback()
                    errors += 1
                    print(f"  ✗ Eroare la film {tmdb_id}: {e}")
                    continue

        except Exception as e:
            print(f"  ✗ Eroare la pagina {page}: {e}")
            errors += 1
            continue

    print(f"\n✅ Finalizat!")
    print(f"   • Filme create: {movies_created}")
    print(f"   • Filme skip (existente): {movies_skipped}")
    print(f"   • Erori: {errors}")


# ---------------------------------------------------------------------------
# Directors / Embeddings (called only in --reset mode)
# ---------------------------------------------------------------------------

def _run_backfill_directors() -> None:
    print("\n🎬 Backfill directors...")
    from app.scripts.backfill_directors import backfill_directors
    backfill_directors(sleep_seconds=0.25)


def _run_semantic_embeddings() -> None:
    print("\n🧠 Building semantic embeddings (sentence-transformer)...")
    from app.scripts.build_embeddings import build_embeddings
    build_embeddings()


def _run_visual_embeddings() -> None:
    print("\n🖼  Building visual embeddings (CLIP)...")
    from app.scripts.build_visual_embeddings import build_visual_embeddings
    build_visual_embeddings()


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Populează baza de date cu date din TMDB")
    parser.add_argument(
        "--pages",
        type=int,
        default=5,
        help="Număr de pagini de preluat (default: 5, ~100 filme)"
    )
    parser.add_argument(
        "--source",
        choices=["all_time", "popular", "top_rated"],
        default="all_time",
        help="Sursa filmelor: all_time, popular sau top_rated (default: all_time)"
    )
    parser.add_argument(
        "--genres-only",
        action="store_true",
        help="Sincronizează doar genurile, fără filme"
    )
    parser.add_argument(
        "--reset",
        action="store_true",
        help=(
            "Wipe all movie-related data first, then run the full pipeline: "
            "genres → movies → directors → embeddings. "
            "Defaults to 15 pages (~300 movies) and all_time source."
        )
    )
    parser.add_argument(
        "--wipe-only",
        action="store_true",
        help="Wipe all movie-related data and exit without repopulating."
    )
    parser.add_argument(
        "--skip-embeddings",
        action="store_true",
        help="(--reset only) Skip semantic and visual embedding steps."
    )

    args = parser.parse_args()

    db: Session = SessionLocal()
    try:
        if args.wipe_only:
            wipe_all(db)
            return

        if args.reset:
            wipe_all(db)
            sync_genres(db)
            pages = args.pages if args.pages != 5 else 15  # sensible default for reset
            populate_movies(db, source="all_time", num_pages=pages)
        else:
            sync_genres(db)
            if not args.genres_only:
                populate_movies(db, source=args.source, num_pages=args.pages)

    finally:
        db.close()

    if args.reset:
        _run_backfill_directors()
        if not args.skip_embeddings:
            _run_semantic_embeddings()
            _run_visual_embeddings()
        else:
            print("\n--skip-embeddings set — skipping embedding steps.")

    print("\n✅ All done!")


if __name__ == "__main__":
    main()
