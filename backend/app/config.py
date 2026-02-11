import os

DATABASE_URL="postgresql+psycopg2://postgres:postgres@localhost:5432/movie_app" 

# JWT Settings
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

#TMDB API
TMDB_API_KEY = os.getenv("TMDB_API_KEY", "c49fed62d5c0d6b9f5a6ea85623d828b")
TMDB_BASE_URL = "https://api.themoviedb.org/3"
TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"