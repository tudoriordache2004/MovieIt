package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.model.DiaryCreate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryLogUiState(
    val watchedOn: String = LocalDate.now().toString(),
    val rating: Int = 1,
    val comment: String = "",
    val posting: Boolean = false,
    val error: String? = null,
    val logged: Boolean = false
)

@HiltViewModel
class DiaryLogViewModel @Inject constructor(
    private val diaryApi: DiaryApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int =
        savedStateHandle.get<Int>("movieId") ?: error("movieId missing")

    private val _uiState = MutableStateFlow(DiaryLogUiState())
    val uiState: StateFlow<DiaryLogUiState> = _uiState

    fun onWatchedOnChange(value: String) {
        _uiState.update { it.copy(watchedOn = value, error = null) }
    }

    fun onRatingChange(value: Int) {
        _uiState.update { it.copy(rating = value.coerceIn(1, 10), error = null) }
    }

    fun onCommentChange(value: String) {
        _uiState.update { it.copy(comment = value, error = null) }
    }

    fun logToDiary() {
        val watchedOn = _uiState.value.watchedOn.trim()
        val rating = _uiState.value.rating.coerceIn(1, 10)
        val comment = _uiState.value.comment.trim().ifBlank { null }

        // Minim: verificare format basic YYYY-MM-DD
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(watchedOn)) {
            _uiState.update { it.copy(error = "watched_on trebuie să fie în format YYYY-MM-DD") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(posting = true, error = null) }
            try {
                val resp = diaryApi.addToDiary(
                    DiaryCreate(
                        movieId = movieId,
                        watchedOn = watchedOn,
                        rating = rating,
                        comment = comment
                    )
                )

                if (resp.isSuccessful) {
                    _uiState.update { it.copy(posting = false, logged = true) }
                } else {
                    _uiState.update { it.copy(posting = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(posting = false, error = e.message) }
            }
        }
    }

    fun consumeLogged() {
        _uiState.update { it.copy(logged = false) }
    }
}