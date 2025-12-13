from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from datetime import date

class MovieCreate(BaseModel):
    title: str
    description: str
    release_date: datetime
    poster_url: Optional[str]

class MovieOut(BaseModel):
    id: int
    tmdb_id: int
    title: str
    description: Optional[str]
    release_date: Optional[date]
    poster_url: Optional[str]
    avg_rating: float  # calculat din reviews
    
    class Config:
        from_attributes = True