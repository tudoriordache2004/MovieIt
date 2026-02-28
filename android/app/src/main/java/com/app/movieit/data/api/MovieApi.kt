package com.app.movieit.data.api

import com.app.movieit.data.model.Movie

interface MovieApi {
    @retrofit2.http.GET("movies/")
    suspend fun getMovies(): retrofit2.Response<List<Movie>>
}