import os

DATABASE_URL="postgresql+psycopg2://postgres:tudipass@localhost:5432/movie_app" 

# JWT Settings
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30