from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.movie_picker import MoviePickerPickOut, MoviePickerSessionCreate
from app.services.movie_picker import (
    create_movie_picker_session,
    get_initial_pick_for_session,
    get_next_pick_for_session,
)


router = APIRouter(prefix="/movie-picker", tags=["movie-picker"])


@router.post(
    "/session",
    response_model=MoviePickerPickOut,
    status_code=status.HTTP_201_CREATED,
)
def create_picker_session(
    payload: MoviePickerSessionCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    session = create_movie_picker_session(
        db=db,
        user_id=current_user.id,
        payload=payload,
    )

    return get_initial_pick_for_session(db=db, session=session)


@router.get(
    "/session/{session_id}/next",
    response_model=MoviePickerPickOut,
)
def get_next_picker_movie(
    session_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return get_next_pick_for_session(
        db=db,
        user_id=current_user.id,
        session_id=session_id,
    )