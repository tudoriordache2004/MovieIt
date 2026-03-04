package com.app.movieit.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Singleton pentru user+role din sesiunea curenta pentru eventual edit+delete de catre admini/mods
data class SessionState(
    val userId: Int? = null,
    val username: String? = null,
    val role: String? = null
)

class SessionManager {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun setUser(userId: Int, username: String?, role: String?) {
        _state.value = SessionState(userId = userId, username = username, role = role)
    }

    fun clear() {
        _state.value = SessionState()
    }
}