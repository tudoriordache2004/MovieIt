from pydantic import BaseModel, model_validator
from datetime import datetime, date
from typing import Optional, List
from app.schemas.genre import GenreOut


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

    model_config = {"from_attributes": True}

    @model_validator(mode="before")
    @classmethod
    def extract_genres(cls, obj):
        if hasattr(obj, "genre_list"):
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
            }
        return obj
