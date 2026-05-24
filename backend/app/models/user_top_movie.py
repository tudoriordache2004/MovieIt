from sqlalchemy import Column, Integer, ForeignKey, CheckConstraint, UniqueConstraint
from sqlalchemy.orm import relationship
from app.database import Base


class UserTopMovie(Base):
    __tablename__ = "user_top_movies"
    __table_args__ = (
        CheckConstraint("position >= 1 AND position <= 4", name="ck_user_top_movies_position"),
        UniqueConstraint("user_id", "position", name="uq_user_top_movies_user_position"),
        UniqueConstraint("user_id", "movie_id", name="uq_user_top_movies_user_movie"),
    )

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    movie_id = Column(Integer, ForeignKey("movies.id", ondelete="CASCADE"), nullable=False, index=True)
    position = Column(Integer, nullable=False)

    user = relationship("User", back_populates="top_movies")
    movie = relationship("Movie")
