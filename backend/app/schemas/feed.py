from datetime import datetime
from enum import Enum
from typing import Optional

from pydantic import BaseModel


class ActivityType(str, Enum):
    DIARY_LOG = "diary_log"
    NEW_FOLLOWER = "new_follower"


class FeedUserSummary(BaseModel):
    id: int
    username: str
    profile_picture_url: Optional[str] = None

    class Config:
        from_attributes = True


class FeedMovieSummary(BaseModel):
    id: int
    title: str
    poster_url: Optional[str] = None

    class Config:
        from_attributes = True


class ActivityFeedResponse(BaseModel):
    id: str
    activity_type: ActivityType
    user: FeedUserSummary
    movie: Optional[FeedMovieSummary] = None
    rating: Optional[int] = None
    created_at: datetime
