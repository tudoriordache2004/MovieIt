package com.app.movieit.data.api

import com.app.movieit.data.model.HomeRecommendationsOut
import retrofit2.http.GET
import retrofit2.http.Query

interface RecommendationApi {
    @GET("recommendations/home")
    suspend fun getHomeRecommendations(
        @Query("limit") limit: Int = 12,
        @Query("min_rating") minRating: Float = 7f
    ): HomeRecommendationsOut
}
