from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel


class DirectorOut(BaseModel):
    id: int
    tmdb_id: int
    name: str
    biography: Optional[str] = None
    profile_url: Optional[str] = None
    birthday: Optional[date] = None
    deathday: Optional[date] = None
    place_of_birth: Optional[str] = None
    created_at: datetime

    model_config = {"from_attributes": True}


class DirectorMovieOut(BaseModel):
    id: int
    tmdb_id: int
    title: str
    description: Optional[str] = None
    release_date: Optional[date] = None
    poster_url: Optional[str] = None
    avg_rating: float

    model_config = {"from_attributes": True}


class DirectorProfileOut(DirectorOut):
    movies: List[DirectorMovieOut] = []