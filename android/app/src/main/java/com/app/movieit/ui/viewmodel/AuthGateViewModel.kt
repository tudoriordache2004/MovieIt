package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.auth.SessionManager
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
    private val tokenManager: TokenManager,
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthGateState>(AuthGateState.Loading)
    val state: StateFlow<AuthGateState> = _state

    init {
        viewModelScope.launch {
            tokenManager.tokenFlow().collect { token ->
                if (token.isNullOrBlank()) {
                    sessionManager.clear()
                    _state.value = AuthGateState.Unauthed
                } else {
                    // Re-hydrate SessionManager on every app start that bypasses login
                    try {
                        val me = authApi.getMe()
                        if (me.isSuccessful) {
                            me.body()?.let {
                                sessionManager.setUser(it.id, it.username, it.role)
                            }
                        }
                    } catch (_: Exception) { }
                    _state.value = AuthGateState.Authed
                }
            }
        }
    }
}