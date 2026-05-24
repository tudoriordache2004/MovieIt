package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class PublicUserOut(
    val id: Int,
    val username: String,
    @SerializedName("profile_picture_url") val profilePictureUrl: String?
)

data class MovieMini(
    val id: Int,
    val title: String,
    @SerializedName("poster_url") val posterUrl: String? = null,
)

data class PublicProfileOut(
    val id: Int,
    val username: String,
    @SerializedName("profile_picture_url") val profilePictureUrl: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("reviews_count") val reviewsCount: Int,
    @SerializedName("diary_count") val diaryCount: Int,
    @SerializedName("movies_watched_count") val moviesWatchedCount: Int = 0,
    @SerializedName("average_rating") val averageRating: Double? = null,
    val bio: String? = null,
    @SerializedName("cover_photo_url") val coverPhotoUrl: String? = null,
    @SerializedName("top_movies") val topMovies: List<MovieMini> = emptyList(),
    @SerializedName("is_following") val isFollowing: Boolean,
    @SerializedName("is_me") val isMe: Boolean,
)

data class UserProfileUpdateRequest(
    val bio: String? = null,
    @SerializedName("cover_photo_url") val coverPhotoUrl: String? = null,
    @SerializedName("top_movie_ids") val topMovieIds: List<Int> = emptyList(),
)

data class FollowStatusOut(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("is_following") val isFollowing: Boolean,
    @SerializedName("followers_count") val followersCount: Int
)

data class PublicUserWithFollow(
    val id: Int,
    val username: String,
    @SerializedName("profile_picture_url") val profilePictureUrl: String?,
    @SerializedName("is_following") val isFollowing: Boolean
)
