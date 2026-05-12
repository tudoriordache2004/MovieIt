package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DirectorApi
import com.app.movieit.data.model.DirectorProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DirectorProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: DirectorProfile? = null,
)

@HiltViewModel
class DirectorProfileViewModel @Inject constructor(
    private val directorApi: DirectorApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val directorId: Int = checkNotNull(savedStateHandle["directorId"])

    private val _uiState = MutableStateFlow(DirectorProfileUiState())
    val uiState: StateFlow<DirectorProfileUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = directorApi.getDirectorProfile(directorId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, profile = resp.body()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "Failed to load director") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}
