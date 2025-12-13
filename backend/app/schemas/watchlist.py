from pydantic import BaseModel
from datetime import datetime

class WatchListCreate(BaseModel):
    movie_id: int

class WatchListOut(BaseModel):
    user_id: int
    movie_id: int
    added_at: datetime

    class Config:
        from_attributes = True

