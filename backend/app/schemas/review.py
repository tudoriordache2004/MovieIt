# schemas/review.py
from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class ReviewCreate(BaseModel):
    movie_id: int
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None
    is_spoiler: bool = False

class ReviewUpdate(BaseModel):
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None
    is_spoiler: Optional[bool] = None

class ReviewOut(BaseModel):
    id: int
    user_id: int  
    movie_id: int
    rating: Optional[int] = None
    comment: Optional[str]
    is_spoiler: bool
    created_at: datetime
    
    class Config:
        from_attributes = True

class ReviewModerateUpdate(BaseModel):
    comment: Optional[str] = None
    is_spoiler: Optional[bool] = None