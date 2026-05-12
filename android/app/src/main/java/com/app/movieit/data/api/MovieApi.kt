package com.app.movieit.data.api

import com.app.movieit.data.model.Genre
import com.app.movieit.data.model.Movie
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {
    @GET("movies/")
    suspend fun getMovies(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("genre_ids") genreIds: List<Int>? = null,
        @Query("decades") decades: List<Int>? = null,
        @Query("min_rating") minRating: Float? = null,
        @Query("director_id") directorId: Int? = null,
        @Query("search") search: String? = null
    ): Response<List<Movie>>

    @GET("movies/{movie_id}")
    suspend fun getMovieById(@Path("movie_id") movieId: Int): Response<Movie>

    @GET("genres/")
    suspend fun getGenres(): Response<List<Genre>>
}
