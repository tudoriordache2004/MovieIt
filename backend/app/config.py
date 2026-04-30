import os
from dotenv import load_dotenv

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL", "")

# JWT Settings
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60*24*7

# TMDB API
TMDB_API_KEY = os.getenv("TMDB_API_KEY", "")
TMDB_BASE_URL = "https://api.themoviedb.org/3"
TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

# Groq LLM (used as tiebreaker in hybrid classifiers)
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.1-8b-instant")

# Spoiler detection thresholds (scores outside [LOW, HIGH] are decided by HF alone)
SPOILER_LOW = float(os.getenv("SPOILER_LOW", "0.30"))
SPOILER_HIGH = float(os.getenv("SPOILER_HIGH", "0.60"))

# Vulgarity detection thresholds
TOXICITY_LOW = float(os.getenv("TOXICITY_LOW", "0.35"))
TOXICITY_HIGH = float(os.getenv("TOXICITY_HIGH", "0.75"))