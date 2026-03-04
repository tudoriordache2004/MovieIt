from pydantic import BaseModel, Field
from datetime import date, datetime
from typing import Optional
from app.schemas.movie import MovieOut
from app.schemas.review import ReviewOut

class DiaryCreate(BaseModel):
    movie_id: int
    watched_on: date
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None

class DiaryUpdate(BaseModel):
    watched_on: Optional[date] = None
    rating: Optional[int] = Field(None, ge=1, le=10)
    comment: Optional[str] = None

# Renunt la DiaryWithMovieOut si adaug direct filmul si review-ul in DiaryOut pentru a fi vizibile la /diary
class DiaryOut(BaseModel):
    id: int
    user_id: int
    watched_on: date
    created_at: datetime

    movie: MovieOut
    review: Optional[ReviewOut] = None

    class Config:
        from_attributes = True

# Monitorizez constant count de filme pentru a face fetch-ul la fiecare edit/delete in Diary
class DiaryCountOut(BaseModel):
    count: int


# Schema necesara pentru a face join-ul cu Movie, sa accesez titlul in diary
# class DiaryWithMovieOut(BaseModel):
#     id: int
#     user_id: int
#     watched_on: date
#     created_at: datetime

#     movie: MovieOut

#     review_id: Optional[int] = None
#     rating: Optional[int] = None
#     comment: Optional[str] = None

#     class Config:
#         from_attributes = True