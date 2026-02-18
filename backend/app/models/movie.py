from sqlalchemy import Column, Integer, String, Text, Date, Float, TIMESTAMP
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base


class Movie(Base):
    __tablename__ = "movies"
    
    id = Column(Integer, primary_key=True, index=True)
    tmdb_id = Column(Integer, unique=True, nullable=False, index=True)
    title = Column(String(255), nullable=False, index=True)
    description = Column(Text)
    release_date = Column(Date)
    poster_url = Column(Text)
    popularity = Column(Float)
    avg_rating = Column(Float, default=0.0)
    created_at = Column(TIMESTAMP, default=datetime.utcnow)
    
    reviews = relationship("Review", back_populates="movie", cascade="all, delete-orphan")
    genres = relationship("MovieGenre", back_populates="movie", cascade="all, delete-orphan")
    watchlist_items = relationship("Watchlist", back_populates="movie", cascade="all, delete-orphan")
    diary_entries = relationship("DiaryEntry", back_populates="movie", cascade="all, delete-orphan")