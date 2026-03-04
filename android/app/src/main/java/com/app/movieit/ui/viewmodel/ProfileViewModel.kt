package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val username: String? = null,

    val diaryCount: Int = 0,
    val watchlistCount: Int = 0,
    val reviewsCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val diaryApi: DiaryApi,
    private val watchlistApi: WatchlistApi,
    private val reviewApi: ReviewApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionManager.state.collect { s ->
                _uiState.update { it.copy(username = s.username) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val diaryCountResp = diaryApi.getDiaryCount()
                val watchResp = watchlistApi.getMyWatchlist()
                val reviewsResp = reviewApi.getMyReviews(skip = 0, limit = 1)

                _uiState.update {
                    it.copy(
                        loading = false,
                        diaryCount = if (diaryCountResp.isSuccessful) (diaryCountResp.body()?.count ?: 0) else it.diaryCount,
                        watchlistCount = if (watchResp.isSuccessful) (watchResp.body()?.size ?: 0) else it.watchlistCount,
                        reviewsCount = if (reviewsResp.isSuccessful) (reviewsResp.body()?.size ?: 0) else it.reviewsCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}