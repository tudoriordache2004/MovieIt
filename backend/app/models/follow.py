from datetime import datetime

from sqlalchemy import CheckConstraint, Column, DateTime, ForeignKey, Index, Integer
from sqlalchemy.orm import relationship

from app.database import Base


class Follow(Base):
    __tablename__ = "follows"

    follower_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        primary_key=True,
    )
    following_id = Column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        primary_key=True,
    )
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    follower = relationship(
        "User",
        foreign_keys=[follower_id],
        back_populates="following_links",
    )
    following = relationship(
        "User",
        foreign_keys=[following_id],
        back_populates="follower_links",
    )

    __table_args__ = (
        CheckConstraint("follower_id <> following_id", name="ck_follows_no_self_follow"),
        Index("ix_follows_following_id", "following_id"),
    )