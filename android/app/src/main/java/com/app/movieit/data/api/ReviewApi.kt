package com.app.movieit.data.api

import com.app.movieit.data.model.ReviewCreate
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.ReviewUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {

    @POST("reviews/")
    suspend fun createReview(@Body body: ReviewCreate): Response<ReviewOut>

    @GET("reviews/movie/{movie_id}")
    suspend fun getReviewsByMovie(
        @Path("movie_id") movieId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<ReviewOut>>

    @GET("reviews/me")
    suspend fun getMyReviews(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<ReviewOut>>

    @PUT("reviews/{review_id}")
    suspend fun updateReview(
        @Path("review_id") reviewId: Int,
        @Body body: ReviewUpdate
    ): Response<ReviewOut>

    @DELETE("reviews/{review_id}")
    suspend fun deleteReview(@Path("review_id") reviewId: Int): Response<Unit>
}