package com.app.movieit.data.api

import com.app.movieit.data.model.WatchlistCreate
import com.app.movieit.data.model.WatchlistItem
import com.app.movieit.data.model.WatchlistItemWithMovie
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path

interface WatchlistApi {

    @retrofit2.http.POST("watchlist/")
    suspend fun addToWatchlist(
        @Body body: WatchlistCreate
    ): Response<WatchlistItem>

    @retrofit2.http.DELETE("watchlist/{movie_id}")
    suspend fun removeFromWatchlist(
        @Path("movie_id") movieId: Int
    ): Response<Unit> // va avea code 204

    @retrofit2.http.GET("watchlist/me")
    suspend fun getMyWatchlist(): Response<List<WatchlistItemWithMovie>>

    @retrofit2.http.GET("watchlist/user/{user_id}")
    suspend fun getUserWatchlist(
        @Path("user_id") userId: Int
    ): Response<List<WatchlistItemWithMovie>>

    @retrofit2.http.GET("watchlist/{movie_id}/check")
    suspend fun isInMyWatchlist(
        @Path("movie_id") movieId: Int
    ): Response<Boolean>
}