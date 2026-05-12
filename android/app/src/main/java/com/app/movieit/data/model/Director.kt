package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class Director(
    val id: Int,
    @SerializedName("tmdb_id") val tmdbId: Int,
    val name: String,
    val biography: String? = null,
    @SerializedName("profile_url") val profileUrl: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
)

data class DirectorMovie(
    val id: Int,
    @SerializedName("tmdb_id") val tmdbId: Int,
    val title: String,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    @SerializedName("avg_rating") val avgRating: Double = 0.0,
)

data class DirectorProfile(
    val id: Int,
    @SerializedName("tmdb_id") val tmdbId: Int,
    val name: String,
    val biography: String? = null,
    @SerializedName("profile_url") val profileUrl: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
    val movies: List<DirectorMovie> = emptyList(),
)
