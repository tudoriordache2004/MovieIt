package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.auth.SessionManager
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
    val loggedOut: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieApi: MovieApi,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState

    init {
        loadMovies()
    }

    private val pageSize = 51

    fun loadMovies(page: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val response = movieApi.getMovies(
                    skip = page * pageSize,
                    limit = 51
                )
                if (response.isSuccessful) {
                    val result = response.body().orEmpty()
                    _uiState.update {
                        it.copy(
                            loading = false,
                            movies = result,
                            currentPage = page,
                            hasMore = result.size == pageSize
                        )
                    }
                } else {
                    _uiState.update { it.copy(loading = false, error = "HTTP ${response.code()} ${response.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun nextPage() {
        val s = _uiState.value
        if (!s.hasMore || s.loading) return
        loadMovies(s.currentPage + 1)
    }

    fun prevPage() {
        val s = _uiState.value
        if (s.currentPage <= 0 || s.loading) return
        loadMovies(s.currentPage - 1)
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            sessionManager.clear()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun consumeLoggedOut() {
        _uiState.update { it.copy(loggedOut = false) }
    }
}