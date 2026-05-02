from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.user import User
from app.routers.auth import get_current_user
from app.schemas.lens import LensAnalyzeOut
from app.services.clip_lens import analyze_lens_image


router = APIRouter(prefix="/lens", tags=["lens"])


@router.post("/analyze", response_model=LensAnalyzeOut)
async def analyze_lens(
    image: UploadFile = File(...),
    mode: str = Form(...),
    limit: int = Query(5, ge=1, le=10),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if mode not in {"vibe", "cover", "poster"}:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Mode must be one of: vibe, cover, poster",
        )

    if image.content_type and image.content_type not in {"image/jpeg", "image/png", "image/webp"}:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail="Image must be JPEG, PNG, or WEBP",
        )

    image_bytes = await image.read()

    if not image_bytes:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Image file is empty",
        )

    max_size_bytes = 10 * 1024 * 1024
    if len(image_bytes) > max_size_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="Image is too large. Max size is 10MB.",
        )

    try:
        return analyze_lens_image(
            db=db,
            user_id=current_user.id,
            image_bytes=image_bytes,
            mode=mode,
            limit=limit,
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        )