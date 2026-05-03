from pydantic import BaseModel, EmailStr
from datetime import datetime

class UserCreate(BaseModel):
    email: EmailStr
    username: str
    password: str

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


class PublicProfileOut(PublicUserOut):
    created_at: datetime
    followers_count: int
    following_count: int
    reviews_count: int
    diary_count: int
    is_following: bool
    is_me: bool


class FollowStatusOut(BaseModel):
    user_id: int
    is_following: bool
    followers_count: int