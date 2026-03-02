package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.model.Movie
import com.app.movieit.data.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoviesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val movies: List<Movie> = emptyList(),
    val loggedOut: Boolean = false
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieApi: MovieApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val response = movieApi.getMovies()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            movies = response.body().orEmpty()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "HTTP ${response.code()} ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun consumeLoggedOut() {
        _uiState.update { it.copy(loggedOut = false) }
    }
}