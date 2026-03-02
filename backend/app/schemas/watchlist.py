from pydantic import BaseModel
from datetime import datetime
from app.schemas.movie import MovieOut

class WatchListCreate(BaseModel):
    movie_id: int

class WatchListOut(BaseModel):
    user_id: int
    movie_id: int
    added_at: datetime

    class Config:
        from_attributes = True


# Schema necesara pentru a face join-ul cu Movie, astfel incat in frontend sa am acces la Movie Title
class WatchListWithMovieOut(BaseModel):
    user_id: int
    added_at: datetime
    movie: MovieOut 
    
    class Config:
        orm_mode = True