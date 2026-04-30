package com.app.movieit.data.api

import com.app.movieit.data.model.Genre
import retrofit2.Response
import retrofit2.http.GET

interface GenreApi {
    @GET("genres/")
    suspend fun getGenres(): Response<List<Genre>>
}
