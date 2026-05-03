# Script is used for updating current movies from the db with their specific movie directors
import argparse
import sys
import time
from pathlib import Path

backend_path = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_path))

from sqlalchemy.orm import joinedload

from app.database import SessionLocal
from app.models.director import Director, MovieDirector
from app.models.movie import Movie
from app.services.tmdb import tmdb_service


def get_or_create_director(db, tmdb_director: dict, refresh_existing: bool = False) -> Director:
    tmdb_director_id = tmdb_director.get("id")
    if not tmdb_director_id:
        raise ValueError("TMDB director has no id")

    director = db.query(Director).filter(Director.tmdb_id == tmdb_director_id).first()

    if director and not refresh_existing:
        return director

    try:
        tmdb_person = tmdb_service.get_person_details(tmdb_director_id)
        parsed = tmdb_service.parse_director_data(tmdb_person)
    except Exception:
        parsed = {
            "tmdb_id": tmdb_director_id,
            "name": tmdb_director.get("name", ""),
            "biography": "",
            "profile_url": tmdb_service.get_profile_url(tmdb_director.get("profile_path")),
            "birthday": None,
            "deathday": None,
            "place_of_birth": None,
        }

    if director:
        director.name = parsed["name"]
        director.biography = parsed["biography"]
        director.profile_url = parsed["profile_url"]
        director.birthday = parsed["birthday"]
        director.deathday = parsed["deathday"]
        director.place_of_birth = parsed["place_of_birth"]
        return director

    director = Director(**parsed)
    db.add(director)
    db.flush()
    return director


def attach_directors_for_movie(db, movie: Movie, refresh_existing: bool = False) -> int:
    tmdb_directors = tmdb_service.get_movie_directors(movie.tmdb_id)
    attached = 0

    for tmdb_director in tmdb_directors:
        director = get_or_create_director(
            db,
            tmdb_director,
            refresh_existing=refresh_existing,
        )

        existing_link = (
            db.query(MovieDirector)
            .filter(
                MovieDirector.movie_id == movie.id,
                MovieDirector.director_id == director.id,
            )
            .first()
        )

        if existing_link:
            continue

        db.add(MovieDirector(movie_id=movie.id, director_id=director.id))
        attached += 1

    return attached


def backfill_directors(limit: int | None = None, refresh_existing: bool = False, sleep_seconds: float = 0.25):
    db = SessionLocal()

    try:
        query = (
            db.query(Movie)
            .options(joinedload(Movie.director_list))
            .order_by(Movie.id.asc())
        )

        if limit:
            query = query.limit(limit)

        movies = query.all()

        processed = 0
        skipped = 0
        linked = 0
        errors = 0

        for index, movie in enumerate(movies, start=1):
            try:
                if movie.director_list and not refresh_existing:
                    skipped += 1
                    print(f"[{index}/{len(movies)}] Skip: {movie.title}")
                    continue

                attached = attach_directors_for_movie(
                    db,
                    movie,
                    refresh_existing=refresh_existing,
                )

                db.commit()

                processed += 1
                linked += attached
                print(f"[{index}/{len(movies)}] {movie.title}: {attached} director link(s)")

                time.sleep(sleep_seconds)

            except Exception as exc:
                db.rollback()
                errors += 1
                print(f"[{index}/{len(movies)}] Error for {movie.title}: {exc}")

        print("\nDone.")
        print(f"Processed: {processed}")
        print(f"Skipped: {skipped}")
        print(f"Director links created: {linked}")
        print(f"Errors: {errors}")

    finally:
        db.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Backfill movie directors from TMDB.")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of movies to process.")
    parser.add_argument("--refresh-existing", action="store_true", help="Refresh existing director metadata too.")
    parser.add_argument("--sleep", type=float, default=0.25, help="Delay between TMDB calls.")
    args = parser.parse_args()

    backfill_directors(
        limit=args.limit,
        refresh_existing=args.refresh_existing,
        sleep_seconds=args.sleep,
    )