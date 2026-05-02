from datetime import datetime

from pgvector.sqlalchemy import Vector
from sqlalchemy import Column, DateTime, ForeignKey, Integer, Text
from sqlalchemy.orm import relationship

from app.database import Base


class MovieVisualEmbedding(Base):
    __tablename__ = "movie_visual_embeddings"

    id = Column(Integer, primary_key=True, index=True)
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), unique=True, nullable=False, index=True)
    poster_url = Column(Text, nullable=False)
    embedding = Column(Vector(512), nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    movie = relationship("Movie", back_populates="visual_embedding")
