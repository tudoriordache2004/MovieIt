package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class FeedUserSummary(
    val id: Int,
    val username: String,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
)

data class FeedMovieSummary(
    val id: Int,
    val title: String,
    @SerializedName("poster_url") val posterUrl: String? = null,
)

data class ActivityFeedItem(
    val id: String,
    @SerializedName("activity_type") val activityType: String,
    val user: FeedUserSummary,
    val movie: FeedMovieSummary? = null,
    val rating: Int? = null,
    @SerializedName("created_at") val createdAt: String,
)
