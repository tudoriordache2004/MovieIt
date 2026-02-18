from pydantic import BaseModel, Field
from datetime import date, datetime
from typing import Optional


class DiaryCreate(BaseModel):
    """Schema pentru adăugare film în diary – poate include și review/rating."""
    movie_id: int
    watched_on: date
    rating: Optional[int] = Field(None, ge=1, le=10, description="Rating opțional 1-10")
    comment: Optional[str] = None


class DiaryUpdate(BaseModel):
    """Schema pentru actualizare intrare diary."""
    watched_on: Optional[date] = None
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None


class DiaryOut(BaseModel):
    """Schema pentru răspuns diary – include eventualele date din review."""
    id: int
    user_id: int
    movie_id: int
    watched_on: date
    created_at: datetime

    review_id: Optional[int] = None
    rating: Optional[int] = None
    comment: Optional[str] = None

    class Config:
        from_attributes = True