package com.app.movieit.data.model

import com.google.gson.annotations.SerializedName

data class GenreMoviesSection(
    @SerializedName("genre_name") val genreName: String,
    val movies: List<Movie> = emptyList()
)

data class SimilarMoviesResponse(
    @SerializedName("by_director") val byDirector: List<Movie> = emptyList(),
    @SerializedName("by_genre") val byGenre: List<GenreMoviesSection> = emptyList()
)
