from sqlalchemy import Column, Integer, String, ForeignKey
from sqlalchemy.orm import relationship
from app.database import Base


class Genre(Base):
    __tablename__ = "genres"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), unique=True, nullable=False)
    
    movies = relationship("MovieGenre", back_populates="genre", cascade="all, delete-orphan")


class MovieGenre(Base):
    """
    Tabela de asociere many-to-many între Movies și Genres.
    """
    __tablename__ = "movie_genres"
    
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), primary_key=True)
    genre_id = Column(Integer, ForeignKey("genres.id", ondelete="CASCADE"), primary_key=True)
    
    movie = relationship("Movie", back_populates="genres")
    genre = relationship("Genre", back_populates="movies")