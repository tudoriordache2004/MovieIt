package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class LensVisualLabel(
    val key: String,
    val label: String,
    val score: Float,
    val genres: List<String> = emptyList(),
    val mood: String?
)

data class LensRecommendation(
    val movie: Movie,
    val score: Float,
    val reason: String,
    @SerializedName("visual_similarity") val visualSimilarity: Float,
    @SerializedName("matched_genres") val matchedGenres: List<String> = emptyList()
)

data class LensAnalyzeResponse(
    val mode: String,
    val title: String,
    val description: String,
    @SerializedName("visual_labels") val visualLabels: List<LensVisualLabel> = emptyList(),
    @SerializedName("matched_genres") val matchedGenres: List<String> = emptyList(),
    val recommendations: List<LensRecommendation> = emptyList()
)
