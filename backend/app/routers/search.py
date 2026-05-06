import re
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import exists
from app.database import get_db
from app.models.movie import Movie
from app.models.director import Director, MovieDirector
from app.schemas.search import SuggestOut

router = APIRouter(prefix="/search", tags=["search"])


@router.get("/suggest", response_model=SuggestOut)
def suggest(
    q: str = Query(..., min_length=1),
    limit: int = Query(5, ge=1, le=20),
    db: Session = Depends(get_db),
):
    tokens = [t for t in q.lower().split() if t]

    # Movies: prefix match on title, ordered by popularity
    movies = (
        db.query(Movie)
        .options(joinedload(Movie.genre_list), joinedload(Movie.director_list))
        .filter(Movie.title.ilike(f"{q}%"))
        .order_by(Movie.popularity.desc().nullslast())
        .limit(limit)
        .all()
    )

    # Directors: each token must word-prefix-match (\m = word boundary in Postgres regex)
    # re.escape guards against metacharacters typed by the user
    director_q = db.query(Director)
    for tok in tokens:
        pattern = r"\m" + re.escape(tok)
        director_q = director_q.filter(Director.name.op("~*")(pattern))
    directors = (
        director_q
        .filter(exists().where(MovieDirector.director_id == Director.id))
        .order_by(Director.name)
        .limit(limit)
        .all()
    )

    return {"movies": movies, "directors": directors}
