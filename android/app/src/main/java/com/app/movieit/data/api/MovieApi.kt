package com.app.movieit.data.api

import com.app.movieit.data.model.Movie
import retrofit2.Response
import retrofit2.http.Path

interface MovieApi {
    @retrofit2.http.GET("movies/")
    suspend fun getMovies(): Response<List<Movie>>

    @retrofit2.http.GET("movies/{movie_id}")
    suspend fun getMovieById(@Path("movie_id") movieId: Int): Response<Movie>
}