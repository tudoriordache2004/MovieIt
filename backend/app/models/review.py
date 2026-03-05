from sqlalchemy import Column, Integer, Text, Boolean, TIMESTAMP, ForeignKey, CheckConstraint
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class Review(Base):
    __tablename__ = "reviews"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), nullable=False, index=True)
    # Pentru Mark as Spoiler
    is_spoiler = Column(Boolean, nullable=False, server_default="false")

    # NEW: FK către diary_entries
    diary_entry_id = Column(
        Integer,
        ForeignKey("diary_entries.id", ondelete="CASCADE"),
        nullable=True,
        unique=True,   # 1 review per diary entry
        index=True,
    )

    rating = Column(Integer, CheckConstraint("rating >= 1 AND rating <= 10"))
    comment = Column(Text)
    created_at = Column(TIMESTAMP, default=datetime.utcnow)

    user = relationship("User", back_populates="reviews")
    movie = relationship("Movie", back_populates="reviews")
    diary_entry = relationship("DiaryEntry", back_populates="review")