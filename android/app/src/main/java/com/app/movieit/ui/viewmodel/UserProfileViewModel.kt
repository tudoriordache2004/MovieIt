package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.UserApi
import com.app.movieit.data.model.PublicProfileOut
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: PublicProfileOut? = null,
    val followBusy: Boolean = false
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: Int = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = userApi.getUserProfile(userId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, profile = resp.body()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "Failed to load profile") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(followBusy = true) }
            try {
                val resp = if (profile.isFollowing) {
                    userApi.unfollowUser(userId)
                } else {
                    userApi.followUser(userId)
                }
                if (resp.isSuccessful) {
                    val status = resp.body()!!
                    _uiState.update { state ->
                        state.copy(
                            followBusy = false,
                            profile = state.profile?.copy(
                                isFollowing = status.isFollowing,
                                followersCount = status.followersCount
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(followBusy = false, error = "Action failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(followBusy = false, error = e.message) }
            }
        }
    }
}
