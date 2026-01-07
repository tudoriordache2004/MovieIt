# backend/scripts/populate_db.py
"""
Script pentru popularea bazei de date cu date din TMDB API
Usage: python -m scripts.populate_db [--pages 5] [--source popular|top_rated]
"""
import sys
import os
from pathlib import Path

# Adaugă backend/app la path pentru imports
backend_path = Path(__file__).parent.parent
sys.path.insert(0, str(backend_path))

from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models.movie import Movie
from app.models.genre import Genre, MovieGenre
from app.services.tmdb import tmdb_service
import argparse
import time
from typing import Dict

def get_or_create_genre(db: Session, genre_name: str) -> Genre:
    """Găsește sau creează un gen"""
    genre = db.query(Genre).filter(Genre.name == genre_name).first()
    if not genre:
        genre = Genre(name=genre_name)
        db.add(genre)
        db.commit()
        db.refresh(genre)
        print(f"  ✓ Creat gen: {genre_name}")
    return genre

def sync_genres(db: Session):
    """Sincronizează genurile din TMDB"""
    print("📚 Sincronizare genuri din TMDB...")
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

def populate_movies(db: Session, source: str = "popular", num_pages: int = 5):
    """
    Populează baza de date cu filme din TMDB
    
    Args:
        db: Database session
        source: "popular" sau "top_rated"
        num_pages: Număr de pagini de preluat (20 filme/pagină)
    """
    print(f"🎬 Populare filme din TMDB ({source}, {num_pages} pagini)...")
    
    movies_created = 0
    movies_skipped = 0
    errors = 0
    
    for page in range(1, num_pages + 1):
        print(f"\n📄 Pagina {page}/{num_pages}...")
        
        try:
            # Preia filme populare sau top-rated
            if source == "top_rated":
                response = tmdb_service.get_top_rated_movies(page=page)
            else:
                response = tmdb_service.get_popular_movies(page=page)
            
            movies_data = response.get("results", [])
            
            for tmdb_movie_data in movies_data:
                try:
                    tmdb_id = tmdb_movie_data.get("id")
                    
                    # Verifică dacă filmul există deja
                    existing = db.query(Movie).filter(Movie.tmdb_id == tmdb_id).first()
                    if existing:
                        movies_skipped += 1
                        continue
                    
                    # Preia detaliile complete ale filmului (pentru genuri)
                    movie_details = tmdb_service.get_movie_details(tmdb_id)
                    
                    # Parsează datele
                    movie_data = tmdb_service.parse_movie_data(movie_details)
                    
                    # Creează filmul
                    movie = Movie(
                        tmdb_id=movie_data["tmdb_id"],
                        title=movie_data["title"],
                        description=movie_data["description"],
                        release_date=movie_data["release_date"],
                        poster_url=movie_data["poster_url"],
                        popularity=movie_data["popularity"]
                    )
                    db.add(movie)
                    db.flush()  # Pentru a obține ID-ul
                    
                    # Adaugă genurile (many-to-many)
                    tmdb_genres = movie_details.get("genres", [])
                    for tmdb_genre in tmdb_genres:
                        genre_name = tmdb_genre.get("name")
                        if genre_name:
                            genre = get_or_create_genre(db, genre_name)
                            # Creează legătura many-to-many
                            movie_genre = MovieGenre(
                                movie_id=movie.id,
                                genre_id=genre.id
                            )
                            db.add(movie_genre)
                    
                    db.commit()
                    movies_created += 1
                    print(f"  ✓ {movie.title} ({tmdb_id})")
                    
                    # Rate limiting - nu întreba TMDB prea des
                    time.sleep(0.25)  # 250ms între request-uri
                    
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
        choices=["popular", "top_rated"],
        default="popular",
        help="Sursa filmelor: popular sau top_rated (default: popular)"
    )
    parser.add_argument(
        "--genres-only",
        action="store_true",
        help="Sincronizează doar genurile, fără filme"
    )
    
    args = parser.parse_args()
    
    db: Session = SessionLocal()
    try:
        # Sincronizează genurile întotdeauna
        sync_genres(db)
        
        if not args.genres_only:
            populate_movies(db, source=args.source, num_pages=args.pages)
        
    finally:
        db.close()

if __name__ == "__main__":
    main()