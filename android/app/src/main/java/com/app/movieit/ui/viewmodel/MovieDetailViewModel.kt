package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.model.Movie
import com.app.movieit.data.model.WatchlistCreate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val movie: Movie? = null,
    val inWatchlist: Boolean? = null,
    val watchlistBusy: Boolean = false
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieApi: MovieApi,
    private val watchlistApi: WatchlistApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = movieApi.getMovieById(movieId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, movie = resp.body()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
                val checkResp = watchlistApi.isInMyWatchlist(movieId)
                if (checkResp.isSuccessful) {
                    _uiState.update { it.copy(inWatchlist = checkResp.body() ?: false) }
                } else {
                    _uiState.update { it.copy(inWatchlist = false) } // fallback
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value.inWatchlist
        if (current == null) return // încă nu știm

        viewModelScope.launch {
            _uiState.update { it.copy(watchlistBusy = true, error = null) }
            try {
                val resp = if (current) {
                    watchlistApi.removeFromWatchlist(movieId)
                } else {
                    watchlistApi.addToWatchlist(WatchlistCreate(movieId))
                }

                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            watchlistBusy = false,
                            inWatchlist = !current
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            watchlistBusy = false,
                            error = "Watchlist error: HTTP ${resp.code()} ${resp.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(watchlistBusy = false, error = e.message) }
            }
        }
    }
}