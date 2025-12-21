# backend/app/routers/watchlist.py
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.watchlist import Watchlist
from app.models.user import User
from app.models.movie import Movie
from app.schemas.watchlist import WatchListCreate, WatchListOut
from app.routers.auth import get_current_user

router = APIRouter(prefix="/watchlist", tags=["watchlist"])

@router.post("/", response_model=WatchListOut, status_code=status.HTTP_201_CREATED)
def add_to_watchlist(
    watchlist_data: WatchListCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Adaugă film în watchlist"""
    # Verifică dacă filmul există
    movie = db.query(Movie).filter(Movie.id == watchlist_data.movie_id).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Movie with id {watchlist_data.movie_id} not found"
        )
    
    # Verifică dacă filmul e deja în watchlist
    existing = db.query(Watchlist).filter(
        Watchlist.user_id == current_user.id,
        Watchlist.movie_id == watchlist_data.movie_id
    ).first()
    
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Movie is already in your watchlist"
        )
    
    # Adaugă în watchlist
    db_watchlist = Watchlist(
        user_id=current_user.id,
        movie_id=watchlist_data.movie_id
    )
    db.add(db_watchlist)
    db.commit()
    db.refresh(db_watchlist)
    
    return db_watchlist

@router.delete("/{movie_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_from_watchlist(
    movie_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Șterge film din watchlist"""
    watchlist_item = db.query(Watchlist).filter(
        Watchlist.user_id == current_user.id,
        Watchlist.movie_id == movie_id
    ).first()
    
    if not watchlist_item:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Movie not found in your watchlist"
        )
    
    db.delete(watchlist_item)
    db.commit()
    
    return None

@router.get("/me", response_model=List[WatchListOut])
def get_my_watchlist(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Listă watchlist-ul user-ului curent"""
    watchlist_items = db.query(Watchlist).filter(
        Watchlist.user_id == current_user.id
    ).order_by(Watchlist.added_at.desc()).all()
    
    return watchlist_items

@router.get("/user/{user_id}", response_model=List[WatchListOut])
def get_user_watchlist(
    user_id: int,
    db: Session = Depends(get_db)
):
    """Listă watchlist-ul unui user (pentru profil public)"""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"User with id {user_id} not found"
        )
    
    watchlist_items = db.query(Watchlist).filter(
        Watchlist.user_id == user_id
    ).order_by(Watchlist.added_at.desc()).all()
    
    return watchlist_items

@router.get("/{movie_id}/check", response_model=bool)
def check_in_watchlist(
    movie_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Verifică dacă un film este în watchlist-ul user-ului curent"""
    exists = db.query(Watchlist).filter(
        Watchlist.user_id == current_user.id,
        Watchlist.movie_id == movie_id
    ).first() is not None
    
    return exists