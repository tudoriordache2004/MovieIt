package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.Genre
import com.app.movieit.data.model.Movie
import com.app.movieit.data.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val hasMore: Boolean = true,
    // search bar text + live dropdown
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val searchLoading: Boolean = false,
    val dropdownVisible: Boolean = false,
    // submitted search that filters the main grid
    val gridSearch: String? = null,
    // filters
    val genres: List<Genre> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val selectedDecades: Set<Int> = emptySet(),
    val minRating: Float? = null
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieApi: MovieApi,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState

    private val pageSize = 51
    private var searchJob: Job? = null

    init {
        loadGenres()
        loadMovies()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val response = movieApi.getGenres()
                if (response.isSuccessful) {
                    _uiState.update { it.copy(genres = response.body().orEmpty()) }
                }
            } catch (_: Exception) { }
        }
    }

    fun loadMovies(page: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val s = _uiState.value
                val response = movieApi.getMovies(
                    skip = page * pageSize,
                    limit = pageSize,
                    genreIds = s.selectedGenreIds.toList().takeIf { it.isNotEmpty() },
                    decades = s.selectedDecades.toList().takeIf { it.isNotEmpty() },
                    minRating = s.minRating,
                    search = s.gridSearch.takeIf { !it.isNullOrBlank() }
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

    fun toggleGenre(id: Int) {
        _uiState.update { s ->
            val newIds = if (id in s.selectedGenreIds) s.selectedGenreIds - id else s.selectedGenreIds + id
            s.copy(selectedGenreIds = newIds, currentPage = 0)
        }
        loadMovies(0)
    }

    fun toggleDecade(decade: Int) {
        _uiState.update { s ->
            val newDecades = if (decade in s.selectedDecades) s.selectedDecades - decade else s.selectedDecades + decade
            s.copy(selectedDecades = newDecades, currentPage = 0)
        }
        loadMovies(0)
    }

    fun setMinRating(value: Float?) {
        _uiState.update { it.copy(minRating = value, currentPage = 0) }
        loadMovies(0)
    }

    fun setDecadeAndRatingFilters(decades: Set<Int>, minRating: Float?) {
        _uiState.update { it.copy(selectedDecades = decades, minRating = minRating, currentPage = 0) }
        loadMovies(0)
    }

    fun clearAllFilters() {
        _uiState.update { it.copy(selectedGenreIds = emptySet(), selectedDecades = emptySet(), minRating = null, currentPage = 0) }
        loadMovies(0)
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchLoading = query.isNotBlank(), dropdownVisible = query.isNotBlank()) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            try {
                val response = movieApi.getMovies(search = query, limit = 10)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(searchResults = response.body().orEmpty(), searchLoading = false) }
                } else {
                    _uiState.update { it.copy(searchResults = emptyList(), searchLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            }
        }
    }

    fun submitSearchToGrid(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        _uiState.update {
            it.copy(
                gridSearch = trimmed.takeIf { it.isNotBlank() },
                searchResults = emptyList(),
                searchLoading = false,
                dropdownVisible = false,
                currentPage = 0
            )
        }
        loadMovies(0)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                searchLoading = false,
                dropdownVisible = false,
                gridSearch = null,
                currentPage = 0
            )
        }
        loadMovies(0)
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
