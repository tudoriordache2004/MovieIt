package com.app.movieit.data.api

import com.app.movieit.data.model.LoginRequest
import com.app.movieit.data.model.RegisterRequest
import com.app.movieit.data.model.TokenResponse
import com.app.movieit.data.model.UserOut
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part

interface AuthApi {
    @retrofit2.http.POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<UserOut>

    @retrofit2.http.POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @retrofit2.http.GET("auth/me")
    suspend fun getMe(): Response<UserOut>

    @Multipart
    @PUT("auth/me/profile-picture")
    suspend fun uploadProfilePicture(@Part file: MultipartBody.Part): Response<UserOut>
}