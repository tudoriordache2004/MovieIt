from sqlalchemy import Column, Integer, TIMESTAMP, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base


class Watchlist(Base):
    __tablename__ = "watchlist"
    
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), primary_key=True)
    added_at = Column(TIMESTAMP, default=datetime.utcnow)
    
    user = relationship("User", back_populates="watchlist_items")
    movie = relationship("Movie", back_populates="watchlist_items")