package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class ReviewCreate(
    @SerializedName("movie_id") val movieId: Int,
    val rating: Int? = null,
    val comment: String? = null,
    @SerializedName("is_spoiler") val isSpoiler: Boolean = false
)

data class ReviewUpdate(
    val rating: Int? = null,
    val comment: String? = null,
    @SerializedName("is_spoiler") val isSpoiler: Boolean? = null
)

data class ReviewOut(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("movie_id") val movieId: Int,
    val rating: Int?,
    val comment: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_spoiler") val isSpoiler: Boolean
)

data class ReviewModerateUpdate(
    val comment: String? = null,
    @SerializedName("is_spoiler") val isSpoiler: Boolean? = null
)