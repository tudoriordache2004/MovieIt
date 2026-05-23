from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func

from app.database import get_db
from app.models.diary_entry import DiaryEntry
from app.models.review import Review
from app.models.user import User
from app.models.movie import Movie
from app.schemas.diary_entry import DiaryCreate, DiaryUpdate, DiaryOut, DiaryCountOut
from app.routers.auth import get_current_user
from app.services.spoiler_detector import predict_spoiler
from app.services.vulgarity_filter import is_vulgar

router = APIRouter(prefix="/diary", tags=["diary"])

VULGARITY_REJECTION_MESSAGE = "Your review contains inappropriate language and cannot be posted."


def update_movie_avg_rating(db: Session, movie_id: int):
    avg_rating = db.query(func.avg(Review.rating)).filter(Review.movie_id == movie_id).scalar() or 0.0
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if movie:
        movie.avg_rating = round(avg_rating, 2)
        db.commit()


@router.post("/", response_model=DiaryOut, status_code=status.HTTP_201_CREATED)
def add_to_diary(
    payload: DiaryCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    movie = db.query(Movie).filter(Movie.id == payload.movie_id).first()
    if not movie:
        raise HTTPException(status_code=404, detail="Movie not found")

    comment = payload.comment.strip() if payload.comment else ""

    # Validate review text before creating anything in the DB
    if comment and is_vulgar(comment):
        raise HTTPException(status_code=400, detail=VULGARITY_REJECTION_MESSAGE)

    entry = DiaryEntry(
        user_id=current_user.id,
        movie_id=payload.movie_id,
        watched_on=payload.watched_on,
    )
    db.add(entry)
    db.commit()
    db.refresh(entry)

    # Create review bound to this diary entry if user sent rating/comment
    if payload.rating is not None or comment:
        suggested_is_spoiler = predict_spoiler(comment, movie_title=movie.title) if comment else False
        review = Review(
            user_id=current_user.id,
            movie_id=payload.movie_id,
            diary_entry_id=entry.id,
            rating=payload.rating,
            comment=payload.comment,
            is_spoiler=suggested_is_spoiler,
        )
        db.add(review)
        db.commit()
        update_movie_avg_rating(db, payload.movie_id)

    # Reload with relationships for embedded response
    entry = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie), joinedload(DiaryEntry.review))
        .filter(DiaryEntry.id == entry.id)
        .first()
    )
    return entry


@router.get("/me", response_model=List[DiaryOut])
def get_my_diary(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entries = (
        db.query(DiaryEntry)
        .options(
            joinedload(DiaryEntry.movie),
            joinedload(DiaryEntry.review),
        )
        .filter(DiaryEntry.user_id == current_user.id)
        .order_by(DiaryEntry.watched_on.desc(), DiaryEntry.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return entries



@router.put("/{entry_id}", response_model=DiaryOut)
def update_diary_entry(
    entry_id: int,
    payload: DiaryUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entry = db.query(DiaryEntry).filter(DiaryEntry.id == entry_id).first()
    if not entry:
        raise HTTPException(status_code=404, detail="Diary entry not found")
    if entry.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Forbidden")

    if payload.watched_on is not None:
        entry.watched_on = payload.watched_on

    fields = payload.model_fields_set  # pydantic v2

    if "rating" in fields or "comment" in fields:
        new_comment = payload.comment.strip() if payload.comment else ""

        # Reject vulgar comment before touching the DB
        if new_comment and is_vulgar(new_comment):
            raise HTTPException(status_code=400, detail=VULGARITY_REJECTION_MESSAGE)

        review = db.query(Review).filter(Review.diary_entry_id == entry.id).first()

        if not review:
            if payload.rating is not None or new_comment:
                movie = db.query(Movie).filter(Movie.id == entry.movie_id).first()
                suggested_is_spoiler = (
                    predict_spoiler(new_comment, movie_title=movie.title if movie else None)
                    if new_comment
                    else False
                )
                review = Review(
                    user_id=current_user.id,
                    movie_id=entry.movie_id,
                    diary_entry_id=entry.id,
                    rating=payload.rating,
                    comment=payload.comment,
                    is_spoiler=suggested_is_spoiler,
                )
                db.add(review)
        else:
            if "rating" in fields:
                review.rating = payload.rating
            if "comment" in fields:
                review.comment = payload.comment
                movie = db.query(Movie).filter(Movie.id == entry.movie_id).first()
                suggested_is_spoiler = (
                    predict_spoiler(new_comment, movie_title=movie.title if movie else None)
                    if new_comment
                    else False
                )
                review.is_spoiler = suggested_is_spoiler

            # If both fields are now empty, remove the review
            if review.rating is None and (review.comment is None or review.comment.strip() == ""):
                db.delete(review)

        db.commit()
        update_movie_avg_rating(db, entry.movie_id)

    db.commit()

    entry = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie), joinedload(DiaryEntry.review))
        .filter(DiaryEntry.id == entry_id)
        .first()
    )
    return entry


@router.delete("/{entry_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_diary_entry(
    entry_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entry = db.query(DiaryEntry).filter(DiaryEntry.id == entry_id).first()
    if not entry:
        raise HTTPException(status_code=404, detail="Diary entry not found")
    if entry.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Forbidden")

    movie_id = entry.movie_id
    db.delete(entry)
    db.commit()

    # dacă ai ondelete=CASCADE pe review.diary_entry_id, review se șterge automat,
    # dar avg_rating trebuie recalculat
    update_movie_avg_rating(db, movie_id)
    return None


@router.get("/me/count", response_model=DiaryCountOut)
def get_my_diary_count(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    count = (
        db.query(func.count(DiaryEntry.id))
        .filter(DiaryEntry.user_id == current_user.id)
        .scalar()
        or 0
    )
    return {"count": int(count)}


@router.get("/{entry_id}", response_model=DiaryOut)
def get_diary_entry(
    entry_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entry = (
        db.query(DiaryEntry)
        .options(joinedload(DiaryEntry.movie), joinedload(DiaryEntry.review))
        .filter(DiaryEntry.id == entry_id, DiaryEntry.user_id == current_user.id)
        .first()
    )
    if not entry:
        raise HTTPException(status_code=404, detail="Entry not found")
    return entry