package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.auth.TokenManager
import com.app.movieit.data.model.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        val username = uiState.value.username.trim()
        val password = uiState.value.password

        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Complete the username and the password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val response = authApi.login(LoginRequest(username = username, password = password))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        _uiState.update { it.copy(loading = false, error = "Invalid response") }
                        return@launch
                    }

                    tokenManager.saveTokenAndUsername(body.accessToken, username)
                    val meResp = authApi.getMe()
                    if (meResp.isSuccessful) {
                        val me = meResp.body()
                        if (me != null) {
                            sessionManager.setUser(
                                userId = me.id,
                                username = me.username,
                                role = me.role
                            )
                        }
                    }
                    _uiState.update { it.copy(loading = false, loggedIn = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "Login failed: ${response.code()}"
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Unknown error.") }
            }
        }
    }

    fun consumeLoggedIn() {
        _uiState.update { it.copy(loggedIn = false) }
    }
}