package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class RecommendationOut(
    val movie: Movie,
    val score: Float,
    val reason: String,
    @SerializedName("movie_genres") val movieGenres: List<String> = emptyList(),
    @SerializedName("matched_genres") val matchedGenres: List<String> = emptyList()
)

data class HomeRecommendationsOut(
    @SerializedName("hero_message") val heroMessage: String,
    @SerializedName("dominant_genre") val dominantGenre: String?,
    val mood: String,
    val recommendations: List<RecommendationOut>
)
