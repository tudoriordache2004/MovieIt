# backend/app/routers/reviews.py
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

router = APIRouter(prefix="/reviews", tags=["reviews"])

def update_movie_avg_rating(db: Session, movie_id: int):
    """Recalculează avg_rating pentru un film"""
    avg_rating = db.query(func.avg(Review.rating)).filter(
        Review.movie_id == movie_id
    ).scalar() or 0.0
    
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if movie:
        movie.avg_rating = round(avg_rating, 2)
        db.commit()

@router.post("/", response_model=ReviewOut, status_code=status.HTTP_201_CREATED)
def create_review(
    review_data: ReviewCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Creează review nou (user_id vine din token)"""
    # Verifică dacă filmul există
    movie = db.query(Movie).filter(Movie.id == review_data.movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {review_data.movie_id} not found"
        )
    
    # Creează review
    db_review = Review(
        user_id=current_user.id,  # din token, nu din request
        movie_id=review_data.movie_id,
        rating=review_data.rating,
        comment=review_data.comment
    )
    db.add(db_review)
    db.commit()
    db.refresh(db_review)
    
    # Recalculează avg_rating pentru film
    update_movie_avg_rating(db, review_data.movie_id)
    
    return db_review

@router.get("/", response_model=List[ReviewOut])
def get_reviews(
    movie_id: Optional[int] = Query(None, description="Filter by movie ID"),
    user_id: Optional[int] = Query(None, description="Filter by user ID"),
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """Listă reviews cu filtrare"""
    query = db.query(Review)
    
    if movie_id:
        query = query.filter(Review.movie_id == movie_id)
    
    if user_id:
        query = query.filter(Review.user_id == user_id)
    
    reviews = query.order_by(Review.created_at.desc()).offset(skip).limit(limit).all()
    return reviews

@router.get("/movie/{movie_id}", response_model=List[ReviewOut])
def get_reviews_by_movie(
    movie_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """Listă reviews pentru un anumit film"""
    # Verifică dacă filmul există
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {movie_id} not found"
        )
    
    reviews = db.query(Review).filter(
        Review.movie_id == movie_id
    ).order_by(Review.created_at.desc()).offset(skip).limit(limit).all()
    
    return reviews

@router.get("/user/{user_id}", response_model=List[ReviewOut])
def get_reviews_by_user(
    user_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """Listă reviews ale unui anumit user"""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with id {user_id} not found"
        )
    
    reviews = db.query(Review).filter(
        Review.user_id == user_id
    ).order_by(Review.created_at.desc()).offset(skip).limit(limit).all()
    
    return reviews

@router.get("/me", response_model=List[ReviewOut])
def get_my_reviews(
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Listă reviews ale user-ului curent"""
    reviews = db.query(Review).filter(
        Review.user_id == current_user.id
    ).order_by(Review.created_at.desc()).offset(skip).limit(limit).all()
    
    return reviews

@router.get("/{review_id}", response_model=ReviewOut)
def get_review_by_id(review_id: int, db: Session = Depends(get_db)):
    """Obține review după ID"""
    review = db.query(Review).filter(Review.id == review_id).first()
    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found"
        )
    return review

@router.put("/{review_id}", response_model=ReviewOut)
def update_review(
    review_id: int,
    review_update: ReviewUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Actualizează propriul review"""
    review = db.query(Review).filter(Review.id == review_id).first()
    
    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found"
        )
    
    # Verifică dacă review-ul aparține user-ului curent
    if review.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only update your own reviews"
        )
    
    # Actualizează doar câmpurile furnizate
    if review_update.rating is not None:
        review.rating = review_update.rating
    if review_update.comment is not None:
        review.comment = review_update.comment
    
    db.commit()
    db.refresh(review)
    
    # Recalculează avg_rating pentru film
    update_movie_avg_rating(db, review.movie_id)
    
    return review

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
    if not target_user or target_user.role != "user":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    if payload.comment is not None:
        review.comment = payload.comment

    db.commit()
    db.refresh(review)
    return review

@router.delete("/{review_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_review(
    review_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Șterge propriul review"""
    review = db.query(Review).filter(Review.id == review_id).first()
    
    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Review with id {review_id} not found"
        )
    
    # Verifică dacă review-ul aparține user-ului curent
    if review.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only delete your own reviews"
        )
    
    movie_id = review.movie_id  # Salvează pentru recalculare
    db.delete(review)
    db.commit()
    
    # Recalculează avg_rating pentru film
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
    if not target_user or target_user.role != "user":
        raise HTTPException(status_code=403, detail="Cannot moderate this user's review")

    movie_id = review.movie_id
    db.delete(review)
    db.commit()

    update_movie_avg_rating(db, movie_id)