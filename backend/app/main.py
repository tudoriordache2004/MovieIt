from fastapi import FastAPI
from app.routers import auth

app = FastAPI(
    title="Movie Review API",
    description="API pentru aplicația de review-uri filme",
    version="1.0.0"
)

# Include router-ul de autentificare
app.include_router(auth.router)

@app.get("/")
def root():
    return {"message": "Movie Review API", "status": "running"}
