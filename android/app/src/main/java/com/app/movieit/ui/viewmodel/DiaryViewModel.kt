package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.data.model.DiaryUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val entries: List<DiaryOut> = emptyList(),

    // edit
    val editingEntryId: Int? = null,
    val editWatchedOn: String = "",
    val editRating: Int? = null,
    val editComment: String = "",
    val saving: Boolean = false,

    // delete
    val deletingEntryId: Int? = null
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

    fun startEdit(entry: DiaryOut) {
        _uiState.update {
            it.copy(
                editingEntryId = entry.id,
                editWatchedOn = entry.watchedOn,
                editRating = entry.review?.rating,
                editComment = entry.review?.comment.orEmpty(),
                error = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingEntryId = null) }
    }

    fun onEditWatchedOnChange(v: String) {
        _uiState.update { it.copy(editWatchedOn = v, error = null) }
    }

    fun onEditRatingChange(v: Int) {
        _uiState.update { it.copy(editRating = v.coerceIn(1, 10), error = null) }
    }

    fun clearEditRating() {
        _uiState.update { it.copy(editRating = null, error = null) }
    }

    fun onEditCommentChange(v: String) {
        _uiState.update { it.copy(editComment = v, error = null) }
    }

    fun saveEdit() {
        val entryId = _uiState.value.editingEntryId ?: return
        val watchedOn = _uiState.value.editWatchedOn.trim()
        val rating = _uiState.value.editRating?.coerceIn(1, 10)
        val comment = _uiState.value.editComment.trim().ifBlank { null }

        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(watchedOn)) {
            _uiState.update { it.copy(error = "watched_on trebuie să fie YYYY-MM-DD") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                val resp = diaryApi.updateDiaryEntry(
                    entryId = entryId,
                    body = DiaryUpdate(
                        watchedOn = watchedOn,
                        rating = rating,
                        comment = comment
                    )
                )

                if (resp.isSuccessful) {
                    _uiState.update { it.copy(saving = false, editingEntryId = null) }
                    load()
                } else {
                    _uiState.update { it.copy(saving = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, error = e.message) }
            }
        }
    }

    fun deleteEntry(entryId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingEntryId = entryId, error = null) }
            try {
                val resp = diaryApi.deleteDiaryEntry(entryId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(deletingEntryId = null) }
                    load()
                } else {
                    _uiState.update { it.copy(deletingEntryId = null, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(deletingEntryId = null, error = e.message) }
            }
        }
    }
}