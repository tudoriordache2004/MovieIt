package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class WatchlistCreate(
    @SerializedName("movie_id") val movieId: Int
)

data class WatchlistItem(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("added_at") val addedAt: String
)

// pentru GET /watchlist/me si /watchlist/user/{id} (WatchListWithMovieOut)
data class WatchlistItemWithMovie(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("added_at") val addedAt: String,
    val movie: Movie
)