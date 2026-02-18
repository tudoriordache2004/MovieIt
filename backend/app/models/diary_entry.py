from sqlalchemy import Column, Integer, Date, TIMESTAMP, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base


class DiaryEntry(Base):
    __tablename__ = "diary_entries"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), nullable=False, index=True)
    watched_on = Column(Date, nullable=False, index=True)
    created_at = Column(TIMESTAMP, default=datetime.utcnow)

    user = relationship("User", back_populates="diary_entries")
    movie = relationship("Movie", back_populates="diary_entries")

    # un review per diary entry (recomandat)
    review = relationship(
        "Review",
        back_populates="diary_entry",
        uselist=False,
        cascade="all, delete-orphan",
        passive_deletes=True,
    )