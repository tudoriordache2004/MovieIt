# backend/app/routers/genres.py
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.genre import Genre
from app.schemas.genre import GenreOut

router = APIRouter(prefix="/genres", tags=["genres"])

@router.get("/", response_model=List[GenreOut])
def get_genres(db: Session = Depends(get_db)):
    """Listă toate genurile (pentru filtre în UI)"""
    genres = db.query(Genre).order_by(Genre.name).all()
    return genres

@router.get("/{genre_id}", response_model=GenreOut)
def get_genre(genre_id: int, db: Session = Depends(get_db)):
    """Obține gen după ID"""
    genre = db.query(Genre).filter(Genre.id == genre_id).first()
    if not genre:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Genre with id {genre_id} not found"
        )
    return genre