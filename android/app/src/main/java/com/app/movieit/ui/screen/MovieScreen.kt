package com.app.movieit.ui.screen

import com.app.movieit.data.api.ApiClient
import com.app.movieit.data.model.Movie

@androidx.compose.runtime.Composable
fun MoviesScreen() {
    val movies = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<Movie>() }
    val loading = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    val error = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val response = ApiClient.movieApi.getMovies()
            if (response.isSuccessful) {
                response.body()?.let { movies.addAll(it) }
            } else {
                error.value = "HTTP ${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            error.value = e.message
        } finally {
            loading.value = false
        }
    }

    when {
        loading.value -> androidx.compose.material3.CircularProgressIndicator()
        error.value != null -> androidx.compose.material3.Text("Error: ${error.value}")
        else -> androidx.compose.foundation.lazy.LazyColumn {
            items(movies.size) { idx ->
                androidx.compose.material3.Text(movies[idx].title)
            }
        }
    }
}