from .user import User
from .movie import Movie
from .genre import Genre
from .review import Review
from .watchlist import Watchlist
from .diary_entry import DiaryEntry

# Import Base pentru a putea crea tabelele
from app.database import Base

__all__ = ["User", "Movie", "Genre", "Review", "Watchlist", "Base", "DiaryEntry"]