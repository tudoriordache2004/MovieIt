from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import func

from app.database import get_db
from app.models.review import Review
from app.models.user import User
from app.models.movie import Movie
from app.schemas.review import ReviewCreate, ReviewOut, ReviewUpdate, ReviewModerateUpdate
from app.routers.auth import get_current_user

from app.services.spoiler_detector import predict_spoiler
from app.services.vulgarity_filter import is_vulgar


router = APIRouter(prefix="/reviews", tags=["reviews"])

VULGARITY_REJECTION_MESSAGE = "Your review contains inappropriate language and cannot be posted."


def _enrich(review: Review) -> Review:
    """Attach username and profile_picture_url from the user relationship."""
    if review.user:
        review.username = review.user.username
        review.profile_picture_url = review.user.profile_picture_url
    return review


def update_movie_avg_rating(db: Session, movie_id: int):
    """Recalculează avg_rating pentru un film"""
    avg_rating = (
        db.query(func.avg(Review.rating))
        .filter(Review.movie_id == movie_id)
        .scalar()
        or 0.0
    )

    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if movie:
        movie.avg_rating = round(avg_rating, 2)
        db.commit()
    return None


@router.post("/", response_model=ReviewOut, status_code=status.HTTP_201_CREATED)
def create_review(
    review_data: ReviewCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Creează review nou (user_id vine din token)"""
    movie = db.query(Movie).filter(Movie.id == review_data.movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {review_data.movie_id} not found",
        )

    if review_data.rating is None and (
        review_data.comment is None or review_data.comment.strip() == ""
    ):
        raise HTTPException(
            status_code=422,
            detail="Review cannot be null",
        )

    comment = review_data.comment.strip() if review_data.comment else ""

    # Reject vulgar reviews
    if comment and is_vulgar(comment):
        raise HTTPException(status_code=400, detail=VULGARITY_REJECTION_MESSAGE)

    # NLP spoiler suggestion overrides user flag (only if comment provided)
    suggested_is_spoiler = predict_spoiler(comment, movie_title=movie.title) if comment else False
    is_spoiler_value = suggested_is_spoiler if comment else review_data.is_spoiler

    db_review = Review(
        user_id=current_user.id,
        movie_id=review_data.movie_id,
        rating=review_data.rating,
        comment=review_data.comment,
        is_spoiler=is_spoiler_value,
    )
    db.add(db_review)
    db.commit()
    db.refresh(db_review)

    update_movie_avg_rating(db, review_data.movie_id)

    # Non-persistent fields used by response schema defaults
    db_review.suggested_is_spoiler = suggested_is_spoiler
    db_review.suggested_is_toxic = False

    return _enrich(db_review)


@router.get("/", response_model=List[ReviewOut])
def get_reviews(
    movie_id: Optional[int] = Query(None, description="Filter by movie ID"),
    user_id: Optional[int] = Query(None, description="Filter by user ID"),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """Listă reviews cu filtrare"""
    query = db.query(Review)

    if movie_id:
        query = query.filter(Review.movie_id == movie_id)

    if user_id:
        query = query.filter(Review.user_id == user_id)

    reviews = (
        query.order_by(Review.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [_enrich(r) for r in reviews]


@router.get("/movie/{movie_id}", response_model=List[ReviewOut])
def get_reviews_by_movie(
    movie_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """Listă reviews pentru un anumit film"""
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {movie_id} not found",
        )

    reviews = (
        db.query(Review)
        .filter(Review.movie_id == movie_id)
        .order_by(Review.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [_enrich(r) for r in reviews]


@router.get("/user/{user_id}", response_model=List[ReviewOut])
def get_reviews_by_user(
    user_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """Listă reviews ale unui anumit user"""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with id {user_id} not found",
        )

    reviews = (
        db.query(Review)
        .filter(Review.user_id == user_id)
        .order_by(Review.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [_enrich(r) for r in reviews]


@router.get("/me/count")
def get_my_reviews_count(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    count = (
        db.query(func.count(Review.id))
        .filter(Review.user_id == current_user.id)
        .scalar()
        or 0
    )
    return {"count": int(count)}


@router.get("/me", response_model=List[ReviewOut])
def get_my_reviews(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Listă reviews ale user-ului curent"""
    reviews = (
        db.query(Review)
        .filter(Review.user_id == current_user.id)
        .order_by(Review.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [_enrich(r) for r in reviews]


@router.get("/{review_id}", response_model=ReviewOut)
def get_review_by_id(review_id: int, db: Session = Depends(get_db)):
    """Obține review după ID"""
    review = db.query(Review).filter(Review.id == review_id).first()
    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found",
        )
    return _enrich(review)


@router.put("/{review_id}", response_model=ReviewOut)
def update_review(
    review_id: int,
    review_update: ReviewUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Actualizează propriul review"""
    review = db.query(Review).filter(Review.id == review_id).first()

    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found",
        )

    if review.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only update your own reviews",
        )

    if review_update.rating is not None:
        review.rating = review_update.rating

    if review_update.comment is not None:
        new_comment = review_update.comment.strip()

        if new_comment and is_vulgar(new_comment):
            raise HTTPException(status_code=400, detail=VULGARITY_REJECTION_MESSAGE)

        review.comment = review_update.comment

        # If comment changes, recompute spoiler from NLP (overrides payload is_spoiler)
        movie = db.query(Movie).filter(Movie.id == review.movie_id).first()
        suggested_is_spoiler = (
            predict_spoiler(new_comment, movie_title=movie.title if movie else None)
            if new_comment
            else False
        )
        review.is_spoiler = suggested_is_spoiler if new_comment else False

        # Non-persistent response fields
        review.suggested_is_spoiler = suggested_is_spoiler
        review.suggested_is_toxic = False

    elif review_update.is_spoiler is not None:
        # Allow manual override only when comment isn't updated
        review.is_spoiler = review_update.is_spoiler

    db.commit()
    db.refresh(review)

    update_movie_avg_rating(db, review.movie_id)

    return _enrich(review)


@router.put("/{review_id}/moderate", response_model=ReviewOut)
def moderate_review_comment(
    review_id: int,
    payload: ReviewModerateUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if current_user.role not in ("mod", "admin"):
        raise HTTPException(status_code=403, detail="Forbidden")

    review = db.query(Review).filter(Review.id == review_id).first()
    if not review:
        raise HTTPException(status_code=404, detail="Review not found")

    target_user = db.query(User).filter(User.id == review.user_id).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="User not found")

    if current_user.role == "mod" and target_user.role != "user":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    if current_user.role == "admin" and target_user.role == "admin":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    if payload.comment is not None:
        # Optional: keep same vulgarity rule for moderation edits
        new_comment = payload.comment.strip()
        if new_comment and is_vulgar(new_comment):
            raise HTTPException(status_code=400, detail=VULGARITY_REJECTION_MESSAGE)
        review.comment = payload.comment

    if payload.is_spoiler is not None:
        review.is_spoiler = payload.is_spoiler

    db.commit()
    db.refresh(review)
    return _enrich(review)


@router.delete("/{review_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_review(
    review_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Șterge propriul review"""
    review = db.query(Review).filter(Review.id == review_id).first()

    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found",
        )

    if review.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only delete your own reviews",
        )

    movie_id = review.movie_id
    db.delete(review)
    db.commit()

    update_movie_avg_rating(db, movie_id)

    return None


@router.delete("/{review_id}/moderate", status_code=204)
def moderate_delete_review(
    review_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if current_user.role not in ("mod", "admin"):
        raise HTTPException(status_code=403, detail="Forbidden")

    review = db.query(Review).filter(Review.id == review_id).first()
    if not review:
        raise HTTPException(status_code=404, detail="Review not found")

    target_user = db.query(User).filter(User.id == review.user_id).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="User not found")

    if current_user.role == "mod" and target_user.role != "user":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    if current_user.role == "admin" and target_user.role == "admin":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    movie_id = review.movie_id
    db.delete(review)
    db.commit()

    update_movie_avg_rating(db, movie_id)