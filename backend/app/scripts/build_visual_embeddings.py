import sys
import time
from pathlib import Path
from io import BytesIO

import requests
from PIL import Image
from sqlalchemy.orm import joinedload

backend_path = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_path))

from app.database import SessionLocal
from app.models.movie import Movie
from app.models.movie_visual_embedding import MovieVisualEmbedding
from app.services.clip_lens import encode_image_bytes


def download_image(url: str) -> bytes:
    response = requests.get(url, timeout=15)
    response.raise_for_status()
    return response.content


def validate_image(image_bytes: bytes) -> bytes:
    image = Image.open(BytesIO(image_bytes)).convert("RGB")

    output = BytesIO()
    image.save(output, format="JPEG", quality=90)
    return output.getvalue()


def build_visual_embeddings(limit: int | None = None):
    db = SessionLocal()

    try:
        query = (
            db.query(Movie)
            .options(joinedload(Movie.visual_embedding))
            .filter(Movie.poster_url.isnot(None))
            .order_by(Movie.id.asc())
        )

        if limit:
            query = query.limit(limit)

        movies = query.all()

        created = 0
        updated = 0
        skipped = 0
        errors = 0

        for index, movie in enumerate(movies, start=1):
            if not movie.poster_url:
                skipped += 1
                continue

            try:
                print(f"[{index}/{len(movies)}] {movie.title}")

                image_bytes = download_image(movie.poster_url)
                image_bytes = validate_image(image_bytes)
                embedding = encode_image_bytes(image_bytes)

                existing = movie.visual_embedding

                if existing:
                    existing.poster_url = movie.poster_url
                    existing.embedding = embedding
                    updated += 1
                else:
                    db.add(
                        MovieVisualEmbedding(
                            movie_id=movie.id,
                            poster_url=movie.poster_url,
                            embedding=embedding,
                        )
                    )
                    created += 1

                db.commit()
                time.sleep(0.05)

            except Exception as exc:
                db.rollback()
                errors += 1
                print(f"  Error: {exc}")

        print("\nDone.")
        print(f"Created: {created}")
        print(f"Updated: {updated}")
        print(f"Skipped: {skipped}")
        print(f"Errors: {errors}")

    finally:
        db.close()


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Build CLIP visual embeddings for movie posters.")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of movies to process.")
    args = parser.parse_args()

    build_visual_embeddings(limit=args.limit)