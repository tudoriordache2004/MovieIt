from typing import List

from pydantic import BaseModel

from app.schemas.movie import MovieOut


class LensVisualLabelOut(BaseModel):
    key: str
    label: str
    score: float
    genres: List[str]
    mood: str


class LensRecommendationOut(BaseModel):
    movie: MovieOut
    score: float
    reason: str
    visual_similarity: float
    matched_genres: List[str]


class LensAnalyzeOut(BaseModel):
    mode: str
    title: str
    description: str
    visual_labels: List[LensVisualLabelOut]
    matched_genres: List[str]
    recommendations: List[LensRecommendationOut]