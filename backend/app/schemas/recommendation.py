from typing import List, Optional

from pydantic import BaseModel, Field

from app.schemas.movie import MovieOut


class RecommendationOut(BaseModel):
    movie: MovieOut
    score: float
    reason: str
    movie_genres: List[str] = Field(default_factory=list)
    matched_genres: List[str] = Field(default_factory=list)

    model_config = {"from_attributes": True}


class HomeRecommendationsOut(BaseModel):
    hero_message: str
    dominant_genre: Optional[str] = None
    mood: str
    recommendations: List[RecommendationOut]