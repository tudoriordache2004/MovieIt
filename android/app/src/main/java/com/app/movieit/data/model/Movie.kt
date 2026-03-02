package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    @SerializedName("tmdb_id") val tmdbId: Int,
    val title: String,
    val description: String?,
    @SerializedName("release_date") val releaseDate: String?,   // îl ținem String pentru început
    @SerializedName("poster_url") val posterUrl: String?,
    @SerializedName("avg_rating") val avgRating: Float,
    @SerializedName("created_at") val createdAt: String
)