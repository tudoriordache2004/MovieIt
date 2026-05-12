package com.app.movieit.data.model

data class SuggestResponse(
    val movies: List<Movie> = emptyList(),
    val directors: List<Director> = emptyList(),
)
