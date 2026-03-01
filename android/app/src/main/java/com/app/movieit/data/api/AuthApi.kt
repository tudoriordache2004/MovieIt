package com.app.movieit.data.api

import com.app.movieit.data.model.LoginRequest
import com.app.movieit.data.model.RegisterRequest
import com.app.movieit.data.model.TokenResponse
import com.app.movieit.data.model.UserOut
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserOut>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserOut>
}