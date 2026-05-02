package com.app.movieit.data.api

import com.app.movieit.data.model.LensAnalyzeResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface LensApi {
    @Multipart
    @POST("lens/analyze")
    suspend fun analyzeLens(
        @Part image: MultipartBody.Part,
        @Part("mode") mode: RequestBody,
        @Query("limit") limit: Int = 5
    ): Response<LensAnalyzeResponse>
}
