package com.app.movieit.data.api

import com.app.movieit.data.model.DirectorProfile
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface DirectorApi {
    @GET("directors/{director_id}")
    suspend fun getDirectorProfile(@Path("director_id") directorId: Int): Response<DirectorProfile>
}
