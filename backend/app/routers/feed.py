from typing import List

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.models.diary_entry import DiaryEntry
from app.models.follow import Follow
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.feed import (
    ActivityFeedResponse,
    ActivityType,
    FeedMovieSummary,
    FeedUserSummary,
)

router = APIRouter(prefix="/users", tags=["feed"])

FEED_LIMIT = 20


@router.get("/me/feed", response_model=List[ActivityFeedResponse])
def get_my_feed(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # Step A — collect diary entries from followed users + self
    following_ids = [
        row[0]
        for row in db.query(Follow.following_id)
        .filter(Follow.follower_id == current_user.id)
        .all()
    ]
    relevant_user_ids = following_ids + [current_user.id]

    diary_entries = (
        db.query(DiaryEntry)
        .options(
            joinedload(DiaryEntry.user),
            joinedload(DiaryEntry.movie),
            joinedload(DiaryEntry.review),
        )
        .filter(DiaryEntry.user_id.in_(relevant_user_ids))
        .order_by(DiaryEntry.created_at.desc())
        .limit(FEED_LIMIT)
        .all()
    )

    # Step B — collect users who recently followed me
    new_followers = (
        db.query(Follow)
        .options(joinedload(Follow.follower))
        .filter(Follow.following_id == current_user.id)
        .order_by(Follow.created_at.desc())
        .limit(FEED_LIMIT)
        .all()
    )

    # Step C — map, merge, sort, slice
    items: List[ActivityFeedResponse] = []

    for entry in diary_entries:
        items.append(
            ActivityFeedResponse(
                id=f"diary_{entry.id}",
                activity_type=ActivityType.DIARY_LOG,
                user=FeedUserSummary.model_validate(entry.user),
                movie=FeedMovieSummary.model_validate(entry.movie),
                rating=entry.review.rating if entry.review else None,
                created_at=entry.created_at,
            )
        )

    for follow in new_followers:
        items.append(
            ActivityFeedResponse(
                id=f"follow_{follow.follower_id}_{follow.following_id}",
                activity_type=ActivityType.NEW_FOLLOWER,
                user=FeedUserSummary.model_validate(follow.follower),
                movie=None,
                rating=None,
                created_at=follow.created_at,
            )
        )

    items.sort(key=lambda x: x.created_at, reverse=True)
    return items[:FEED_LIMIT]
