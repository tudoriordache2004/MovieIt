from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.movie import Movie
from app.models.genre import Genre, MovieGenre
from app.schemas.movie import MovieOut, MovieImport
from app.services.tmdb import tmdb_service


router = APIRouter(prefix="/movies", tags=["movies"])

@router.get("/", response_model=List[MovieOut])
def get_movies(
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(100, ge=1, le=100, description="Number of records to return"),
    genre_id: Optional[int] = Query(None, description="Filter by genre ID"),
    year: Optional[int] = Query(None, description="Filter by release year"),
    min_rating: Optional[float] = Query(None, ge=0, le=10, description="Minimum average rating"),
    search: Optional[str] = Query(None, description="Search by title"),
    db: Session = Depends(get_db)
):
    """Listă filme cu paginare și filtre"""
    query = db.query(Movie)
    
    # many-to-many MovieGenre
    if genre_id:
        query = query.join(MovieGenre).filter(MovieGenre.genre_id == genre_id)
    
    if year:
        query = query.filter(
            func.extract('year', Movie.release_date) == year
        )
    
    if min_rating is not None:
        query = query.filter(Movie.avg_rating >= min_rating)
    
    if search:
        query = query.filter(Movie.title.ilike(f"%{search}%"))
    
    movies = query.order_by(Movie.popularity.desc().nullslast(), Movie.avg_rating.desc()).offset(skip).limit(limit).all()
    return movies

@router.get("/{movie_id}", response_model=MovieOut)
def get_movie_by_id(movie_id: int, db: Session = Depends(get_db)):
    """Obține film după ID"""
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {movie_id} not found"
        )
    return movie

@router.get("/tmdb/{tmdb_id}", response_model=MovieOut)
def get_movie_by_tmdb_id(tmdb_id: int, db: Session = Depends(get_db)):
    """Obține film după TMDB ID"""
    movie = db.query(Movie).filter(Movie.tmdb_id == tmdb_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with TMDB id {tmdb_id} not found"
        )
    return movie

@router.post("/", response_model=MovieOut)
def create_movie(movie_data: MovieImport, db: Session = Depends(get_db)):
    """Importă film din TMDB după tmdb_id (fără creare manuală)"""

    # 1) Idempotent: dacă există deja, returnează-l
    existing = db.query(Movie).filter(Movie.tmdb_id == movie_data.tmdb_id).first()
    if existing:
        return existing

    # 2) Fetch din TMDB
    try:
        tmdb_movie = tmdb_service.get_movie_details(movie_data.tmdb_id)
    except Exception:
        # poți diferenția 404 vs rate limit etc, dar pentru început e ok:
        raise HTTPException(status_code=502, detail="TMDB request failed")

    # 3) Map TMDB -> modelul Movie
    parsed = tmdb_service.parse_movie_data(tmdb_movie)  # tmdb_id/title/description/release_date/poster_url/popularity
    db_movie = Movie(**parsed)

    db.add(db_movie)
    db.flush()  # ca să obții db_movie.id înainte de commit

    # 4) Genuri + movie_genres (TMDB details are "genres": [{id, name}, ...])
    for g in tmdb_movie.get("genres", []):
        name = (g.get("name") or "").strip()
        if not name:
            continue

        genre = db.query(Genre).filter(Genre.name == name).first()
        if not genre:
            genre = Genre(name=name)
            db.add(genre)
            db.flush()

        db.add(MovieGenre(movie_id=db_movie.id, genre_id=genre.id))

    db.commit()
    db.refresh(db_movie)
    return db_movie

@router.get("/genre/{genre_id}", response_model=List[MovieOut])
def get_movies_by_genre(
    genre_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """Listă filme după gen"""
    genre = db.query(Genre).filter(Genre.id == genre_id).first()
    if not genre:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Genre with id {genre_id} not found"
        )
    
    movies = db.query(Movie).join(MovieGenre).filter(
        MovieGenre.genre_id == genre_id
    ).order_by(Movie.popularity.desc().nullslast()).offset(skip).limit(limit).all()
    
    return movies