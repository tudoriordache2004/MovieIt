from datetime import datetime, timedelta
from uuid import uuid4

from sqlalchemy import Column, DateTime, ForeignKey, Integer, JSON, String, Text
from sqlalchemy.orm import relationship

from app.database import Base


class MoviePickerSession(Base):
    __tablename__ = "movie_picker_sessions"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)

    mood = Column(String(64), nullable=True)
    prompt = Column(Text, nullable=True)

    interpreted_mood = Column(String(64), nullable=False)
    secondary_moods = Column(JSON, nullable=False, default=list)

    summary = Column(Text, nullable=False)
    ranked_movie_ids = Column(JSON, nullable=False, default=list)
    ranked_results = Column(JSON, nullable=False, default=list)

    current_cursor = Column(Integer, nullable=False, default=0)

    created_at = Column(DateTime, default=datetime.utcnow)
    expires_at = Column(
        DateTime,
        default=lambda: datetime.utcnow() + timedelta(hours=6),
        nullable=False,
    )

    user = relationship("User")