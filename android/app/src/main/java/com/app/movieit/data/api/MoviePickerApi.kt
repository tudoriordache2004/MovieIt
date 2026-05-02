package com.app.movieit.data.api

import com.app.movieit.data.model.MoviePickerRequest
import com.app.movieit.data.model.MoviePickerResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MoviePickerApi {
    @POST("movie-picker/session")
    suspend fun createSession(@Body request: MoviePickerRequest): Response<MoviePickerResponse>

    @GET("movie-picker/session/{session_id}/next")
    suspend fun getNextPick(@Path("session_id") sessionId: String): Response<MoviePickerResponse>
}
