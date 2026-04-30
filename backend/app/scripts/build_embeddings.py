import sys
from pathlib import Path

backend_path = Path(__file__).parent.parent.parent
sys.path.insert(0, str(backend_path))

from sqlalchemy.orm import joinedload

from app.database import SessionLocal
from app.models.movie import Movie
from app.models.movie_embedding import MovieEmbedding
from app.services.recommendations import build_movie_source_text, embed_text


def build_embeddings():
    db = SessionLocal()

    try:
        movies = db.query(Movie).options(joinedload(Movie.genre_list)).all()

        created = 0
        updated = 0

        for movie in movies:
            source_text = build_movie_source_text(movie)
            if not source_text.strip():
                continue

            embedding = embed_text(source_text)

            existing = (
                db.query(MovieEmbedding)
                .filter(MovieEmbedding.movie_id == movie.id)
                .first()
            )

            if existing:
                existing.source_text = source_text
                existing.embedding = embedding
                updated += 1
            else:
                db.add(
                    MovieEmbedding(
                        movie_id=movie.id,
                        source_text=source_text,
                        embedding=embedding,
                    )
                )
                created += 1

            db.commit()
            print(f"✓ {movie.title}")

        print(f"Done. Created: {created}, updated: {updated}")

    finally:
        db.close()


if __name__ == "__main__":
    build_embeddings()