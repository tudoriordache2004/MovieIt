package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class MoviePickerRequest(
    val mood: String?,
    val prompt: String?,
    val intensity: String,
    val avoid: List<String>
)

data class MoviePickerResponse(
    @SerializedName("session_id") val sessionId: String,
    val movie: Movie,
    val score: Float,
    val reason: String,
    @SerializedName("interpreted_mood") val interpretedMood: String?,
    @SerializedName("secondary_moods") val secondaryMoods: List<String> = emptyList(),
    val summary: String?,
    @SerializedName("matched_genres") val matchedGenres: List<String> = emptyList(),
    @SerializedName("avoided_signals") val avoidedSignals: List<String> = emptyList(),
    val cursor: Int,
    @SerializedName("next_cursor") val nextCursor: Int,
    @SerializedName("has_more") val hasMore: Boolean
)
