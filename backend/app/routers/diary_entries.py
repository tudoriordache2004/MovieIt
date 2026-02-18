from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import func

from app.database import get_db
from app.models.diary_entry import DiaryEntry
from app.models.review import Review
from app.models.user import User
from app.models.movie import Movie
from app.schemas.diary_entry import DiaryCreate, DiaryUpdate, DiaryOut
from app.routers.auth import get_current_user

router = APIRouter(prefix="/diary", tags=["diary"])


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

    # multiple entries allowed (same movie + same date)
    entry = DiaryEntry(
        user_id=current_user.id,
        movie_id=payload.movie_id,
        watched_on=payload.watched_on,
    )
    db.add(entry)
    db.commit()
    db.refresh(entry)

    review = None
    # creează review dacă user a trimis rating sau comment
    if payload.rating is not None or payload.comment is not None:
        review = Review(
            user_id=current_user.id,
            movie_id=payload.movie_id,
            diary_entry_id=entry.id,
            rating=payload.rating,
            comment=payload.comment,
        )
        db.add(review)
        db.commit()
        db.refresh(review)
        update_movie_avg_rating(db, payload.movie_id)

    return DiaryOut(
        id=entry.id,
        user_id=entry.user_id,
        movie_id=entry.movie_id,
        watched_on=entry.watched_on,
        created_at=entry.created_at,
        review_id=review.id if review else None,
        rating=review.rating if review else None,
        comment=review.comment if review else None,
    )


@router.get("/me", response_model=List[DiaryOut])
def get_my_diary(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    entries = (
        db.query(DiaryEntry)
        .filter(DiaryEntry.user_id == current_user.id)
        .order_by(DiaryEntry.watched_on.desc(), DiaryEntry.created_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )

    out: List[DiaryOut] = []
    for e in entries:
        r = e.review  # relationship uselist=False
        out.append(
            DiaryOut(
                id=e.id,
                user_id=e.user_id,
                movie_id=e.movie_id,
                watched_on=e.watched_on,
                created_at=e.created_at,
                review_id=r.id if r else None,
                rating=r.rating if r else None,
                comment=r.comment if r else None,
            )
        )
    return out


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

    # update/create review bound to this diary entry
    if payload.rating is not None or payload.comment is not None:
        review = db.query(Review).filter(Review.diary_entry_id == entry.id).first()

        if not review:
            review = Review(
                user_id=current_user.id,
                movie_id=entry.movie_id,
                diary_entry_id=entry.id,
                rating=payload.rating,
                comment=payload.comment,
            )
            db.add(review)
        else:
            if payload.rating is not None:
                review.rating = payload.rating
            if payload.comment is not None:
                review.comment = payload.comment

        db.commit()
        update_movie_avg_rating(db, entry.movie_id)

    db.commit()
    db.refresh(entry)

    r = entry.review
    return DiaryOut(
        id=entry.id,
        user_id=entry.user_id,
        movie_id=entry.movie_id,
        watched_on=entry.watched_on,
        created_at=entry.created_at,
        review_id=r.id if r else None,
        rating=r.rating if r else None,
        comment=r.comment if r else None,
    )


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