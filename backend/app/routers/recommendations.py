from typing import List

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.recommendation import RecommendationOut
from app.schemas.recommendation import RecommendationOut, HomeRecommendationsOut
from app.services.recommendations import (
    get_recommendations_for_user,
    get_home_recommendations_for_user,
)


router = APIRouter(prefix="/recommendations", tags=["recommendations"])


@router.get("/me", response_model=List[RecommendationOut])
def get_my_recommendations(
    limit: int = Query(6, ge=1, le=20),
    min_rating: int = Query(8, ge=1, le=10),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return get_recommendations_for_user(
        db=db,
        user_id=current_user.id,
        limit=limit,
        min_rating=min_rating,
    )

@router.get("/home", response_model=HomeRecommendationsOut)
def get_my_home_recommendations(
    limit: int = Query(6, ge=1, le=20),
    min_rating: int = Query(8, ge=1, le=10),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return get_home_recommendations_for_user(
        db=db,
        user_id=current_user.id,
        limit=limit,
        min_rating=min_rating,
    )