# backend/app/services/tmdb.py
import requests
from typing import List, Optional, Dict
from datetime import datetime, date
from app.config import TMDB_API_KEY, TMDB_BASE_URL, TMDB_IMAGE_BASE_URL

class TMDBService:
    """Serviciu pentru interacțiune cu TMDB API"""
    
    def __init__(self, api_key: str = TMDB_API_KEY):
        self.api_key = api_key
        self.base_url = TMDB_BASE_URL
        self.image_base_url = TMDB_IMAGE_BASE_URL
    
    def _make_request(self, endpoint: str, params: Optional[Dict] = None) -> Dict:
        """Face request către TMDB API"""
        url = f"{self.base_url}/{endpoint}"
        if params is None:
            params = {}
        params["api_key"] = self.api_key
        
        response = requests.get(url, params=params)
        response.raise_for_status()
        return response.json()
    
    def get_popular_movies(self, page: int = 1) -> Dict:
        return self._make_request("movie/popular", params={"page": page})
    
    def get_top_rated_movies(self, page: int = 1) -> Dict:
        return self._make_request("movie/top_rated", params={"page": page})
    
    def get_movie_details(self, tmdb_id: int) -> Dict:
        return self._make_request(f"movie/{tmdb_id}")

    def get_movie_credits(self, tmdb_id: int) -> Dict:
        return self._make_request(f"movie/{tmdb_id}/credits")

    def get_person_details(self, person_id: int) -> Dict:
        return self._make_request(f"person/{person_id}")

    def get_profile_url(self, profile_path: Optional[str]) -> Optional[str]:
        if not profile_path:
            return None
        return f"{self.image_base_url}{profile_path}"

    def get_movie_directors(self, tmdb_id: int) -> List[Dict]:
        credits = self.get_movie_credits(tmdb_id)
        crew = credits.get("crew", [])
        return [
            person
            for person in crew
            if person.get("job") == "Director"
        ]

    def parse_person_date(self, value: Optional[str]) -> Optional[date]:
        if not value:
            return None
        try:
            return datetime.strptime(value, "%Y-%m-%d").date()
        except (ValueError, TypeError):
            return None

    def parse_director_data(self, tmdb_person: Dict) -> Dict:
        return {
            "tmdb_id": tmdb_person["id"],
            "name": tmdb_person.get("name", ""),
            "biography": tmdb_person.get("biography") or "",
            "profile_url": self.get_profile_url(tmdb_person.get("profile_path")),
            "birthday": self.parse_person_date(tmdb_person.get("birthday")),
            "deathday": self.parse_person_date(tmdb_person.get("deathday")),
            "place_of_birth": tmdb_person.get("place_of_birth"),
        }
    
    def get_genres(self) -> List[Dict]:
        response = self._make_request("genre/movie/list")
        return response.get("genres", [])
    
    def search_movies(self, query: str, page: int = 1) -> Dict:
        return self._make_request("search/movie", params={"query": query, "page": page})
    
    def get_poster_url(self, poster_path: Optional[str]) -> Optional[str]:
        if not poster_path:
            return None
        return f"{self.image_base_url}{poster_path}"
    
    def parse_movie_data(self, tmdb_movie: Dict) -> Dict:
        # Parsează release_date (poate fi None sau format "YYYY-MM-DD")
        release_date = None
        if tmdb_movie.get("release_date"):
            try:
                release_date = datetime.strptime(tmdb_movie["release_date"], "%Y-%m-%d").date()
            except (ValueError, TypeError):
                release_date = None
        
        return {
            "tmdb_id": tmdb_movie["id"],
            "title": tmdb_movie.get("title", ""),
            "description": tmdb_movie.get("overview") or tmdb_movie.get("description", ""),
            "release_date": release_date,
            "poster_url": self.get_poster_url(tmdb_movie.get("poster_path")),
            "popularity": tmdb_movie.get("popularity", 0.0),
        }
    
    def parse_genre_data(self, tmdb_genre: Dict) -> Dict:
        """Parsează datele genului TMDB"""
        return {
            "tmdb_id": tmdb_genre.get("id"),  
            "name": tmdb_genre.get("name", "")
        }

    def get_all_time_popular_movies(self, page: int = 1):
        """Preia cele mai populare filme din toate timpurile (bazat pe numărul de voturi)"""
        return self._make_request(
            "discover/movie",
            params={
                "page": page,
                "sort_by": "vote_count.desc",
                "include_adult": False,
                "include_video": False
            }
        )

tmdb_service = TMDBService()