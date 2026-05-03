from typing import List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.diary_entry import DiaryEntry
from app.models.follow import Follow
from app.models.review import Review
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.user import FollowStatusOut, PublicProfileOut, PublicUserOut, PublicUserWithFollow

router = APIRouter(prefix="/users", tags=["users"])


def get_user_or_404(db: Session, user_id: int) -> User:
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with id {user_id} not found",
        )
    return user


def count_followers(db: Session, user_id: int) -> int:
    return (
        db.query(func.count(Follow.follower_id))
        .filter(Follow.following_id == user_id)
        .scalar()
        or 0
    )


def count_following(db: Session, user_id: int) -> int:
    return (
        db.query(func.count(Follow.following_id))
        .filter(Follow.follower_id == user_id)
        .scalar()
        or 0
    )


def is_following_user(db: Session, follower_id: int, following_id: int) -> bool:
    return (
        db.query(Follow)
        .filter(
            Follow.follower_id == follower_id,
            Follow.following_id == following_id,
        )
        .first()
        is not None
    )


@router.get("/search", response_model=List[PublicUserOut])
def search_users(
    query: str = Query(..., min_length=1),
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=50),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    users = (
        db.query(User)
        .filter(User.username.ilike(f"%{query}%"))
        .order_by(User.username.asc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return users


@router.get("/{user_id}/profile", response_model=PublicProfileOut)
def get_public_profile(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user = get_user_or_404(db, user_id)

    followers_count = count_followers(db, user_id)
    following_count = count_following(db, user_id)
    reviews_count = (
        db.query(func.count(Review.id))
        .filter(Review.user_id == user_id)
        .scalar()
        or 0
    )
    diary_count = (
        db.query(func.count(DiaryEntry.id))
        .filter(DiaryEntry.user_id == user_id)
        .scalar()
        or 0
    )

    is_me = current_user.id == user_id
    is_following = False if is_me else is_following_user(
        db=db,
        follower_id=current_user.id,
        following_id=user_id,
    )

    return PublicProfileOut(
        id=user.id,
        username=user.username,
        profile_picture_url=user.profile_picture_url,
        created_at=user.created_at,
        followers_count=followers_count,
        following_count=following_count,
        reviews_count=reviews_count,
        diary_count=diary_count,
        is_following=is_following,
        is_me=is_me,
    )


@router.post("/{user_id}/follow", response_model=FollowStatusOut)
def follow_user(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    target_user = get_user_or_404(db, user_id)

    if target_user.id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You cannot follow yourself",
        )

    existing_follow = (
        db.query(Follow)
        .filter(
            Follow.follower_id == current_user.id,
            Follow.following_id == target_user.id,
        )
        .first()
    )

    if not existing_follow:
        follow = Follow(
            follower_id=current_user.id,
            following_id=target_user.id,
        )
        db.add(follow)
        db.commit()

    return FollowStatusOut(
        user_id=target_user.id,
        is_following=True,
        followers_count=count_followers(db, target_user.id),
    )


@router.delete("/{user_id}/follow", response_model=FollowStatusOut)
def unfollow_user(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    target_user = get_user_or_404(db, user_id)

    if target_user.id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You cannot unfollow yourself",
        )

    existing_follow = (
        db.query(Follow)
        .filter(
            Follow.follower_id == current_user.id,
            Follow.following_id == target_user.id,
        )
        .first()
    )

    if existing_follow:
        db.delete(existing_follow)
        db.commit()

    return FollowStatusOut(
        user_id=target_user.id,
        is_following=False,
        followers_count=count_followers(db, target_user.id),
    )


@router.get("/{user_id}/followers", response_model=List[PublicUserWithFollow])
def get_followers(
    user_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    get_user_or_404(db, user_id)

    followers = (
        db.query(User)
        .join(Follow, Follow.follower_id == User.id)
        .filter(Follow.following_id == user_id)
        .order_by(Follow.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

    return [
        PublicUserWithFollow(
            id=u.id,
            username=u.username,
            profile_picture_url=u.profile_picture_url,
            is_following=is_following_user(db, current_user.id, u.id),
        )
        for u in followers
    ]


@router.get("/{user_id}/following", response_model=List[PublicUserWithFollow])
def get_following(
    user_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    get_user_or_404(db, user_id)

    following = (
        db.query(User)
        .join(Follow, Follow.following_id == User.id)
        .filter(Follow.follower_id == user_id)
        .order_by(Follow.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

    return [
        PublicUserWithFollow(
            id=u.id,
            username=u.username,
            profile_picture_url=u.profile_picture_url,
            is_following=is_following_user(db, current_user.id, u.id),
        )
        for u in following
    ]