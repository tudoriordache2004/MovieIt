# schemas/review.py
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class ReviewCreate(BaseModel):
    movie_id: int
    rating: int = Field(ge=1, le=10)
    comment: Optional[str] = None

class ReviewUpdate(BaseModel):
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None

class ReviewOut(BaseModel):
    id: int
    user_id: int  # ar trebui inclus aici dacă vrei să-l vezi în response
    movie_id: int
    rating: int
    comment: Optional[str]
    created_at: datetime
    
    class Config:
        from_attributes = True