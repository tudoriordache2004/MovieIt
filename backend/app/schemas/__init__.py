# schemas/__init__.py
from .user import UserCreate, UserOut
from .movie import MovieOut, MovieCreate
from .review import ReviewCreate, ReviewOut
from .watchlist import WatchListCreate, WatchListOut
from .genre import GenreCreate, GenreOut

# Opțional: listează explicit ce e exportat
__all__ = [
    "UserCreate",
    "UserOut", 
    "MovieOut",
    "MovieCreate",
    "ReviewCreate",
    "ReviewOut",
    "WatchListCreate",
    "WatchListOut",
    "GenreCreate",
    "GenreOut",
]