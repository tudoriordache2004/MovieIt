package com.app.movieit.data.api

import com.app.movieit.data.model.SuggestResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {
    @GET("search/suggest")
    suspend fun suggest(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
    ): Response<SuggestResponse>
}
