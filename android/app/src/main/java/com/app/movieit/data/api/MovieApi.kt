package com.app.movieit.data.api

import com.app.movieit.data.model.Movie
import retrofit2.Response
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {
    @retrofit2.http.GET("movies/")
    suspend fun getMovies(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<Movie>>

    @retrofit2.http.GET("movies/{movie_id}")
    suspend fun getMovieById(@Path("movie_id") movieId: Int): Response<Movie>
}