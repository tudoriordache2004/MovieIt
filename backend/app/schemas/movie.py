from pydantic import BaseModel, model_validator
from datetime import datetime, date
from typing import Optional, List
from app.schemas.genre import GenreOut
from app.schemas.director import DirectorOut


class MovieImport(BaseModel):
    tmdb_id: int


class MovieOut(BaseModel):
    id: int
    tmdb_id: int
    title: str
    description: Optional[str]
    release_date: Optional[date]
    poster_url: Optional[str]
    avg_rating: float
    created_at: datetime
    genres: List[GenreOut] = []
    directors: List[DirectorOut] = []

    model_config = {"from_attributes": True}

    @model_validator(mode="before")
    @classmethod
    def extract_relationships(cls, obj):
        if hasattr(obj, "genre_list") or hasattr(obj, "director_list"):
            return {
                "id": obj.id,
                "tmdb_id": obj.tmdb_id,
                "title": obj.title,
                "description": obj.description,
                "release_date": obj.release_date,
                "poster_url": obj.poster_url,
                "avg_rating": obj.avg_rating,
                "created_at": obj.created_at,
                "genres": getattr(obj, "genre_list", None) or [],
                "directors": getattr(obj, "director_list", None) or [],
            }
        return obj


class GenreMoviesOut(BaseModel):
    genre_name: str
    movies: List[MovieOut] = []


class SimilarMoviesOut(BaseModel):
    by_director: List[MovieOut] = []
    by_genre: List[GenreMoviesOut] = []
