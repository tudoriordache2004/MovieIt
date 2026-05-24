from pydantic import BaseModel, EmailStr, field_validator
from datetime import datetime
from typing import List, Optional
import re

_PASSWORD_REGEX = re.compile(r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$')

class UserCreate(BaseModel):
    email: EmailStr
    username: str
    password: str

    @field_validator('password')
    @classmethod
    def password_strength(cls, v: str) -> str:
        if not _PASSWORD_REGEX.match(v):
            raise ValueError(
                'Password must be at least 8 characters and include '
                'an uppercase letter, a lowercase letter, and a number.'
            )
        return v

class UserOut(BaseModel):
    id: int
    email: EmailStr
    username: str
    created_at: datetime
    role: str
    profile_picture_url: str | None = None

    class Config:
        from_attributes = True

class PublicUserOut(BaseModel):
    id: int
    username: str
    profile_picture_url: str | None = None

    class Config:
        from_attributes = True


class PublicUserWithFollow(PublicUserOut):
    is_following: bool


class MovieMini(BaseModel):
    id: int
    title: str
    poster_url: Optional[str] = None

    class Config:
        from_attributes = True


class UserProfileUpdate(BaseModel):
    bio: Optional[str] = None
    cover_photo_url: Optional[str] = None
    top_movie_ids: List[int] = []

    @field_validator("bio")
    @classmethod
    def bio_length(cls, v):
        if v is not None and len(v) > 150:
            raise ValueError("Bio must be 150 characters or fewer.")
        return v

    @field_validator("top_movie_ids")
    @classmethod
    def top_movies_constraints(cls, v):
        if len(v) > 4:
            raise ValueError("At most 4 top movies allowed.")
        if len(set(v)) != len(v):
            raise ValueError("Top movies must be unique.")
        return v


class PublicProfileOut(PublicUserOut):
    created_at: datetime
    followers_count: int
    following_count: int
    reviews_count: int
    diary_count: int
    movies_watched_count: int
    average_rating: Optional[float] = None
    bio: Optional[str] = None
    cover_photo_url: Optional[str] = None
    top_movies: List[MovieMini] = []
    is_following: bool
    is_me: bool


class FollowStatusOut(BaseModel):
    user_id: int
    is_following: bool
    followers_count: int