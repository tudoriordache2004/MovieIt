package com.app.movieit.data.api

import com.app.movieit.data.model.FollowStatusOut
import com.app.movieit.data.model.PublicProfileOut
import com.app.movieit.data.model.PublicUserWithFollow
import com.app.movieit.data.model.UserProfileUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {
    @GET("users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: Int): Response<PublicProfileOut>

    @PUT("users/me/profile")
    suspend fun updateMyProfile(@Body body: UserProfileUpdateRequest): Response<PublicProfileOut>

    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: Int): Response<FollowStatusOut>

    @DELETE("users/{userId}/follow")
    suspend fun unfollowUser(@Path("userId") userId: Int): Response<FollowStatusOut>

    @GET("users/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: Int,
        @Query("skip") skip: Int,
        @Query("limit") limit: Int
    ): Response<List<PublicUserWithFollow>>

    @GET("users/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: Int,
        @Query("skip") skip: Int,
        @Query("limit") limit: Int
    ): Response<List<PublicUserWithFollow>>
}
