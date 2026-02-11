from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware #Android app
from app.routers import auth, movies, reviews, genres, watchlists

app = FastAPI(
    title="Movie Review API",
    description="API pentru aplicația de review-uri filme",
    version="1.0.0"
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

@app.get("/")
def root():
    return {"message": "Movie Review API", "status": "running"}
