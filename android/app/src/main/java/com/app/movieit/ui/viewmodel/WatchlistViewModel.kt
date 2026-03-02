package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.model.WatchlistItemWithMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<WatchlistItemWithMovie> = emptyList()
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistApi: WatchlistApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = watchlistApi.getMyWatchlist()
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(loading = false, items = resp.body().orEmpty())
                    }
                } else {
                    _uiState.update {
                        it.copy(loading = false, error = "HTTP ${resp.code()} ${resp.message()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}