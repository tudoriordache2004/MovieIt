from sqlalchemy import Column, Integer, String, Text, TIMESTAMP
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base


class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True, nullable=False, index=True)
    username = Column(String(255), unique=True, nullable=False, index=True)
    password_hash = Column(Text, nullable=False)
    created_at = Column(TIMESTAMP, default=datetime.utcnow)
    role = Column(String(10), nullable=False, server_default="user")  # "user" | "mod" | "admin"
    profile_picture_url = Column(String(500), nullable=True)
    bio = Column(String(150), nullable=True)
    cover_photo_url = Column(String(500), nullable=True)

    reviews = relationship("Review", back_populates="user", cascade="all, delete-orphan")
    watchlist_items = relationship("Watchlist", back_populates="user", cascade="all, delete-orphan")
    diary_entries = relationship("DiaryEntry", back_populates="user", cascade="all, delete-orphan")

    following_links = relationship(
        "Follow",
        foreign_keys="Follow.follower_id",
        back_populates="follower",
        cascade="all, delete-orphan",
    )
    follower_links = relationship(
        "Follow",
        foreign_keys="Follow.following_id",
        back_populates="following",
        cascade="all, delete-orphan",
    )

    top_movies = relationship(
        "UserTopMovie",
        back_populates="user",
        cascade="all, delete-orphan",
        order_by="UserTopMovie.position",
    )