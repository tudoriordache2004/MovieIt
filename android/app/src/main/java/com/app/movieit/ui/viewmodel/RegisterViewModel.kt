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
) {
    val hasMinLength: Boolean get() = password.length >= 8
    val hasUppercase: Boolean get() = password.any { it.isUpperCase() }
    val hasLowercase: Boolean get() = password.any { it.isLowerCase() }
    val hasDigit: Boolean get() = password.any { it.isDigit() }
    val passwordValid: Boolean get() = hasMinLength && hasUppercase && hasLowercase && hasDigit
}

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
        val state = uiState.value
        val email = state.email.trim()
        val username = state.username.trim()
        val password = state.password

        if (email.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in email, username and password.") }
            return
        }

        if (!state.passwordValid) {
            _uiState.update { it.copy(error = "Your password doesn't meet the requirements below.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val registerResp = authApi.register(
                    RegisterRequest(email = email, username = username, password = password)
                )

                if (!registerResp.isSuccessful) {
                    val errorMsg = when (registerResp.code()) {
                        400 -> {
                            val body = registerResp.errorBody()?.string().orEmpty()
                            when {
                                body.contains("Email already registered", ignoreCase = true) ->
                                    "That email is already in use."
                                body.contains("Username already taken", ignoreCase = true) ->
                                    "That username is already taken."
                                else -> "Registration failed. Please try again."
                            }
                        }
                        422 -> "Password doesn't meet the requirements."
                        else -> "Registration failed. Please try again."
                    }
                    _uiState.update { it.copy(loading = false, error = errorMsg) }
                    return@launch
                }

                val loginResp = authApi.login(LoginRequest(username = username, password = password))
                if (loginResp.isSuccessful) {
                    val token = loginResp.body()
                    if (token == null) {
                        _uiState.update { it.copy(loading = false, error = "Account created. Please log in.") }
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
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "Account created! Please log in to continue.",
                            registered = false
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Connection error. Check your internet and try again.") }
            }
        }
    }

    fun consumeRegistered() {
        _uiState.update { it.copy(registered = false) }
    }
}