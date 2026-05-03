from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.models.director import Director
from app.schemas.director import DirectorProfileOut


router = APIRouter(prefix="/directors", tags=["directors"])


@router.get("/{director_id}", response_model=DirectorProfileOut)
def get_director_profile(director_id: int, db: Session = Depends(get_db)):
    director = db.query(Director).options(
        joinedload(Director.movie_list)
    ).filter(Director.id == director_id).first()

    if not director:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Director with id {director_id} not found",
        )

    return {
        "id": director.id,
        "tmdb_id": director.tmdb_id,
        "name": director.name,
        "biography": director.biography,
        "profile_url": director.profile_url,
        "birthday": director.birthday,
        "deathday": director.deathday,
        "place_of_birth": director.place_of_birth,
        "created_at": director.created_at,
        "movies": director.movie_list,
    }