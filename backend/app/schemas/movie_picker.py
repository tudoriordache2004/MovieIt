from typing import List, Optional

from pydantic import BaseModel, Field, model_validator

from app.schemas.movie import MovieOut


class MoviePickerSessionCreate(BaseModel):
    mood: Optional[str] = None
    prompt: Optional[str] = None
    intensity: str = Field(default="medium", pattern="^(low|medium|high)$")
    avoid: List[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def require_mood_or_prompt(self):
        if not self.mood and not self.prompt:
            raise ValueError("Either mood or prompt must be provided.")
        return self


class MoviePickerPickOut(BaseModel):
    session_id: str
    movie: MovieOut
    score: float
    reason: str

    interpreted_mood: str
    secondary_moods: List[str] = []
    summary: str

    matched_genres: List[str] = []
    avoided_signals: List[str] = []

    cursor: int
    next_cursor: int
    has_more: bool


class MoviePickerSessionOut(MoviePickerPickOut):
    pass