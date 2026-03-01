package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthGateState {
    data object Loading : AuthGateState()
    data object Authed : AuthGateState()
    data object Unauthed : AuthGateState()
}

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
    val state: StateFlow<AuthGateState> = _state

    init {
        viewModelScope.launch {
            tokenManager.tokenFlow().collect { token ->
                _state.value = if (token.isNullOrBlank()) {
                    AuthGateState.Unauthed
                } else {
                    AuthGateState.Authed
                }
            }
        }
    }
}