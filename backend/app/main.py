from contextlib import asynccontextmanager
from pathlib import Path
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware #Android app
from fastapi.staticfiles import StaticFiles
from app.routers import auth, movies, reviews, genres, watchlists, diary_entries, recommendations, movie_picker
from app.services.spoiler_detector import get_classifier as get_spoiler_classifier
from app.services.vulgarity_filter import get_classifier as get_vulgarity_classifier
from app.routers import auth, movies, reviews, genres, watchlists, diary_entries, recommendations, movie_picker, lens

@asynccontextmanager
async def lifespan(app: FastAPI):
    get_spoiler_classifier()
    get_vulgarity_classifier()
    yield

app = FastAPI(
    title="Movie Review API",
    description="API pentru aplicația de review-uri filme",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router)
app.include_router(movies.router)
app.include_router(reviews.router)
app.include_router(genres.router)
app.include_router(watchlists.router)
app.include_router(diary_entries.router)
app.include_router(recommendations.router)
app.include_router(movie_picker.router)
app.include_router(lens.router)

uploads_path = Path("uploads")
uploads_path.mkdir(exist_ok=True)
app.mount("/uploads", StaticFiles(directory=str(uploads_path)), name="uploads")

@app.get("/")
def root():
    return {"message": "Movie Review API", "status": "running"}
