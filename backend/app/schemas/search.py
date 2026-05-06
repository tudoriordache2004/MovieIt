from typing import List
from pydantic import BaseModel
from app.schemas.movie import MovieOut
from app.schemas.director import DirectorOut


class SuggestOut(BaseModel):
    movies: List[MovieOut] = []
    directors: List[DirectorOut] = []
