from datetime import datetime

from sqlalchemy import Column, Date, ForeignKey, Integer, String, Text, TIMESTAMP
from sqlalchemy.orm import relationship

from app.database import Base


class Director(Base):
    __tablename__ = "directors"

    id = Column(Integer, primary_key=True, index=True)
    tmdb_id = Column(Integer, unique=True, nullable=False, index=True)
    name = Column(String(255), nullable=False, index=True)
    biography = Column(Text)
    profile_url = Column(Text)
    birthday = Column(Date)
    deathday = Column(Date)
    place_of_birth = Column(String(255))
    created_at = Column(TIMESTAMP, default=datetime.utcnow)

    movies = relationship(
        "MovieDirector",
        back_populates="director",
        cascade="all, delete-orphan",
    )
    movie_list = relationship(
        "Movie",
        secondary="movie_directors",
        viewonly=True,
    )


class MovieDirector(Base):
    __tablename__ = "movie_directors"

    movie_id = Column(Integer, ForeignKey("movies.id"), primary_key=True)
    director_id = Column(Integer, ForeignKey("directors.id"), primary_key=True)

    movie = relationship("Movie", back_populates="directors")
    director = relationship("Director", back_populates="movies")