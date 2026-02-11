from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from datetime import date

class MovieImport(BaseModel):
    tmdb_id: int

class MovieOut(BaseModel):
    id: int
    tmdb_id: int
    title: str
    description: Optional[str]
    release_date: Optional[date]
    poster_url: Optional[str]
    avg_rating: float  # from reviews
    created_at: datetime
    
    class Config:
        from_attributes = True