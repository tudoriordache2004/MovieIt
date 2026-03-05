package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.LoginRequest
import com.app.movieit.data.model.RegisterRequest
import com.app.movieit.data.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun register() {
        val email = uiState.value.email.trim()
        val username = uiState.value.username.trim()
        val password = uiState.value.password

        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Completează email, username și parola.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val registerResp = authApi.register(
                    RegisterRequest(email = email, username = username, password = password)
                )

                if (!registerResp.isSuccessful) {
                    _uiState.update {
                        it.copy(loading = false, error = "Register eșuat: ${registerResp.code()}")
                    }
                    return@launch
                }

                // Auto-login după register
                val loginResp = authApi.login(LoginRequest(username = username, password = password))
                if (loginResp.isSuccessful) {
                    val token = loginResp.body()
                    if (token == null) {
                        _uiState.update { it.copy(loading = false, error = "Răspuns invalid la login.") }
                        return@launch
                    }
                    tokenManager.saveTokenAndUsername(token.accessToken, username)
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
                    _uiState.update { it.copy(loading = false, registered = true) }
                } else {
                    // cont creat, dar login a eșuat (poți naviga la Login)
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "Cont creat, dar auto-login a eșuat: ${loginResp.code()}",
                            registered = false
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Eroare necunoscută.") }
            }
        }
    }

    fun consumeRegistered() {
        _uiState.update { it.copy(registered = false) }
    }
}