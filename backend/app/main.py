from fastapi import FastAPI
from app.routers import auth
from app.routers import movies
from app.routers import reviews

app = FastAPI(
    title="Movie Review API",
    description="API pentru aplicația de review-uri filme",
    version="1.0.0"
)

# Include routers
app.include_router(auth.router)
app.include_router(movies.router)
app.include_router(reviews.router)

@app.get("/")
def root():
    return {"message": "Movie Review API", "status": "running"}
