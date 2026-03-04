package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.model.DiaryOut
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val entries: List<DiaryOut> = emptyList()
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryApi: DiaryApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = diaryApi.getMyDiary()
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, entries = resp.body().orEmpty()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
}