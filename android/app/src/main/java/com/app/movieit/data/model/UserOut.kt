package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class UserOut(
    val id: Int,
    val email: String,
    val username: String,
    @SerializedName("created_at") val createdAt: String,
    val role: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null
)
