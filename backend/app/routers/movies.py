from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func, select, or_, and_
from app.database import get_db
from app.models.movie import Movie
from app.models.genre import Genre, MovieGenre
from app.schemas.movie import MovieOut, MovieImport, SimilarMoviesOut, GenreMoviesOut
from app.models.director import Director, MovieDirector
from app.services.tmdb import tmdb_service


router = APIRouter(prefix="/movies", tags=["movies"])

@router.get("/", response_model=List[MovieOut])
def get_movies(
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(100, ge=0, le=100, description="Number of records to return"),
    genre_ids: Optional[List[int]] = Query(default=None, description="Filter by genre IDs (AND logic)"),
    decades: Optional[List[int]] = Query(default=None, description="Filter by decades e.g. 1990, 2000 (OR logic)"),
    min_rating: Optional[float] = Query(None, ge=0, le=10, description="Minimum average rating"),
    director_id: Optional[int] = Query(None, description="Filter by director ID"),
    search: Optional[str] = Query(None, description="Search by title"),
    db: Session = Depends(get_db)
):
    query = db.query(Movie)

    if limit == 0:
        limit = 51

    # AND logic: movie must belong to every selected genre
    if genre_ids:
        for gid in genre_ids:
            query = query.filter(
                Movie.id.in_(select(MovieGenre.movie_id).where(MovieGenre.genre_id == gid))
            )

    # OR logic: movie release year falls within any selected decade range
    if decades:
        decade_conditions = [
            and_(
                func.extract('year', Movie.release_date) >= d,
                func.extract('year', Movie.release_date) <= d + 9
            )
            for d in decades
        ]
        query = query.filter(or_(*decade_conditions))

    if min_rating is not None:
        query = query.filter(Movie.avg_rating >= min_rating)

    if director_id is not None:
        query = query.filter(
            Movie.id.in_(select(MovieDirector.movie_id).where(MovieDirector.director_id == director_id))
        )

    if search:
        query = query.filter(Movie.title.ilike(f"{search}%"))

    movies = query.options(
        joinedload(Movie.genre_list),
        joinedload(Movie.director_list),
    ).order_by(
        Movie.popularity.desc().nullslast(),
        Movie.avg_rating.desc()
    ).offset(skip).limit(limit).all()
    return movies

@router.get("/{movie_id}/similar", response_model=SimilarMoviesOut)
def get_similar_movies(
    movie_id: int,
    limit: int = Query(10, ge=1, le=20),
    db: Session = Depends(get_db)
):
    movie = db.query(Movie).options(
        joinedload(Movie.director_list),
        joinedload(Movie.genre_list),
    ).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=f"Movie {movie_id} not found")

    director_ids = [d.id for d in movie.director_list]
    genres = [(g.id, g.name) for g in movie.genre_list]

    by_director: list[Movie] = []
    if director_ids:
        by_director = (
            db.query(Movie)
            .options(joinedload(Movie.genre_list), joinedload(Movie.director_list))
            .filter(
                Movie.id != movie_id,
                Movie.id.in_(
                    select(MovieDirector.movie_id).where(MovieDirector.director_id.in_(director_ids))
                ),
            )
            .order_by(Movie.popularity.desc().nullslast(), Movie.avg_rating.desc())
            .limit(limit)
            .all()
        )

    by_director_ids = {m.id for m in by_director}
    exclude_ids = by_director_ids | {movie_id}

    by_genre: list[GenreMoviesOut] = []
    for genre_id, genre_name in genres:
        genre_q = (
            db.query(Movie)
            .options(joinedload(Movie.genre_list), joinedload(Movie.director_list))
            .filter(
                Movie.id.notin_(exclude_ids),
                Movie.id.in_(
                    select(MovieGenre.movie_id).where(MovieGenre.genre_id == genre_id)
                ),
            )
            .order_by(Movie.popularity.desc().nullslast(), Movie.avg_rating.desc())
            .limit(limit)
            .all()
        )
        if len(genre_q) >= 3:
            by_genre.append(GenreMoviesOut(genre_name=genre_name, movies=genre_q))

    return SimilarMoviesOut(by_director=by_director, by_genre=by_genre)


@router.get("/{movie_id}", response_model=MovieOut)
def get_movie_by_id(movie_id: int, db: Session = Depends(get_db)):
    """Obține film după ID"""
    movie = db.query(Movie).options(
        joinedload(Movie.genre_list),
        joinedload(Movie.director_list),
    ).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {movie_id} not found"
        )
    return movie

@router.get("/tmdb/{tmdb_id}", response_model=MovieOut)
def get_movie_by_tmdb_id(tmdb_id: int, db: Session = Depends(get_db)):
    """Obține film după TMDB ID"""
    movie = db.query(Movie).options(
        joinedload(Movie.genre_list),
        joinedload(Movie.director_list),
    ).filter(Movie.tmdb_id == tmdb_id).first()
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

    # 5) Directors + movie_directors
    try:
        tmdb_directors = tmdb_service.get_movie_directors(movie_data.tmdb_id)
    except Exception:
        tmdb_directors = []

    for tmdb_director in tmdb_directors:
        tmdb_director_id = tmdb_director.get("id")
        if not tmdb_director_id:
            continue

        director = db.query(Director).filter(Director.tmdb_id == tmdb_director_id).first()

        if not director:
            try:
                tmdb_person = tmdb_service.get_person_details(tmdb_director_id)
                parsed_director = tmdb_service.parse_director_data(tmdb_person)
            except Exception:
                parsed_director = {
                    "tmdb_id": tmdb_director_id,
                    "name": tmdb_director.get("name", ""),
                    "biography": "",
                    "profile_url": tmdb_service.get_profile_url(tmdb_director.get("profile_path")),
                    "birthday": None,
                    "deathday": None,
                    "place_of_birth": None,
                }

            director = Director(**parsed_director)
            db.add(director)
            db.flush()

        db.add(MovieDirector(movie_id=db_movie.id, director_id=director.id))

    db.commit()
    db.refresh(db_movie)
    return db.query(Movie).options(
        joinedload(Movie.genre_list),
        joinedload(Movie.director_list),
    ).filter(Movie.id == db_movie.id).first()

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
    
    movies = db.query(Movie).options(
        joinedload(Movie.genre_list),
        joinedload(Movie.director_list),
    ).join(MovieGenre).filter(
        MovieGenre.genre_id == genre_id
    ).order_by(Movie.popularity.desc().nullslast()).offset(skip).limit(limit).all()
    
    return movies