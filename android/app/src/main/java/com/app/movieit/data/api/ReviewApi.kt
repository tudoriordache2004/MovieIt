package com.app.movieit.data.api

import com.app.movieit.data.model.ReviewCreate
import com.app.movieit.data.model.ReviewModerateUpdate
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.ReviewUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {

    @retrofit2.http.POST("reviews/")
    suspend fun createReview(@Body body: ReviewCreate): Response<ReviewOut>

    @retrofit2.http.GET("reviews/movie/{movie_id}")
    suspend fun getReviewsByMovie(
        @Path("movie_id") movieId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<ReviewOut>>

    @retrofit2.http.GET("reviews/me")
    suspend fun getMyReviews(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<ReviewOut>>

    @retrofit2.http.PUT("reviews/{review_id}")
    suspend fun updateReview(
        @Path("review_id") reviewId: Int,
        @Body body: ReviewUpdate
    ): Response<ReviewOut>

    @retrofit2.http.DELETE("reviews/{review_id}")
    suspend fun deleteReview(@Path("review_id") reviewId: Int): Response<Unit>

    @retrofit2.http.PUT("reviews/{review_id}/moderate")
    suspend fun moderateReview(
        @Path("review_id") reviewId: Int,
        @Body body: ReviewModerateUpdate
    ): Response<ReviewOut>

    @retrofit2.http.DELETE("reviews/{review_id}/moderate")
    suspend fun moderateDeleteReview(
        @Path("review_id") reviewId: Int
    ): Response<Unit>


}