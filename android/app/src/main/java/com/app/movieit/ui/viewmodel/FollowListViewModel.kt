package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.UserApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.PublicUserWithFollow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

data class FollowListUiState(
    val items: List<PublicUserWithFollow> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val followBusy: Set<Int> = emptySet(),
    val currentUserId: Int? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val userApi: UserApi,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Int = checkNotNull(savedStateHandle["userId"])
    val listType: String = checkNotNull(savedStateHandle["listType"])

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState

    init {
        _uiState.update { it.copy(currentUserId = sessionManager.state.value.userId) }
        loadMore()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return

        val isFirstPage = state.items.isEmpty()
        viewModelScope.launch {
            _uiState.update {
                if (isFirstPage) it.copy(loading = true, error = null)
                else it.copy(loadingMore = true, error = null)
            }
            try {
                val skip = _uiState.value.items.size
                val resp = if (listType == "followers") {
                    userApi.getFollowers(userId, skip, PAGE_SIZE)
                } else {
                    userApi.getFollowing(userId, skip, PAGE_SIZE)
                }

                if (resp.isSuccessful) {
                    val page = resp.body() ?: emptyList()
                    _uiState.update { s ->
                        s.copy(
                            loading = false,
                            loadingMore = false,
                            items = s.items + page,
                            hasMore = page.size == PAGE_SIZE
                        )
                    }
                } else {
                    _uiState.update { it.copy(loading = false, loadingMore = false, error = "Failed to load list") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, loadingMore = false, error = e.message) }
            }
        }
    }

    fun toggleFollow(targetUserId: Int) {
        val item = _uiState.value.items.find { it.id == targetUserId } ?: return
        if (targetUserId in _uiState.value.followBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(followBusy = it.followBusy + targetUserId) }
            try {
                val resp = if (item.isFollowing) {
                    userApi.unfollowUser(targetUserId)
                } else {
                    userApi.followUser(targetUserId)
                }
                if (resp.isSuccessful) {
                    val newFollowing = resp.body()!!.isFollowing
                    _uiState.update { s ->
                        s.copy(
                            followBusy = s.followBusy - targetUserId,
                            items = s.items.map {
                                if (it.id == targetUserId) it.copy(isFollowing = newFollowing) else it
                            }
                        )
                    }
                } else {
                    _uiState.update { it.copy(followBusy = it.followBusy - targetUserId, error = "Action failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(followBusy = it.followBusy - targetUserId, error = e.message) }
            }
        }
    }
}
