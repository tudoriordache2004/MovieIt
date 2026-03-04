package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class DiaryCreate(
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("watched_on") val watchedOn: String, // "YYYY-MM-DD"
    val rating: Int? = null, // 1..10
    val comment: String? = null
)

data class DiaryUpdate(
    @SerializedName("watched_on") val watchedOn: String? = null, // "YYYY-MM-DD"
    val rating: Int? = null,
    val comment: String? = null
)

data class DiaryOut(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("movie_id") val movieId: Int,
    @SerializedName("watched_on") val watchedOn: String,   // "YYYY-MM-DD"
    @SerializedName("created_at") val createdAt: String,   // ISO datetime
    @SerializedName("review_id") val reviewId: Int? = null,
    val rating: Int? = null,
    val comment: String? = null
)