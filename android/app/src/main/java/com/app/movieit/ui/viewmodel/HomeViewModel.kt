package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.RecommendationApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.RecommendationOut
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val heroMessage: String = "",
    val mood: String = "",
    val dominantGenre: String? = null,
    val recommendations: List<RecommendationOut> = emptyList(),
    val allGenres: List<String> = emptyList(),
    val username: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recommendationApi: RecommendationApi,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        _uiState.update { it.copy(username = sessionManager.state.value.username) }
        loadRecommendations()
    }

    fun refresh() {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = recommendationApi.getHomeRecommendations()
                val genres = response.recommendations
                    .flatMap { it.matchedGenres }
                    .distinct()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        heroMessage = response.heroMessage,
                        mood = response.mood,
                        dominantGenre = response.dominantGenre,
                        recommendations = response.recommendations,
                        allGenres = genres
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
