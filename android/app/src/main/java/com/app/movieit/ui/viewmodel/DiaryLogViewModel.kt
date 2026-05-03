package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.model.DiaryCreate
import com.app.movieit.data.model.DiaryUpdate
import com.app.movieit.data.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryLogUiState(
    val watchedOn: String = LocalDate.now().toString(),
    val rating: Int? = null,
    val comment: String = "",
    val posting: Boolean = false,
    val error: String? = null,
    val logged: Boolean = false,
    val movie: Movie? = null,
    val movieLoading: Boolean = true
)

@HiltViewModel
class DiaryLogViewModel @Inject constructor(
    private val diaryApi: DiaryApi,
    private val movieApi: MovieApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])
    private val entryId: Int = savedStateHandle.get<Int>("entryId") ?: -1

    val isEditMode: Boolean get() = entryId != -1

    private val _uiState = MutableStateFlow(DiaryLogUiState())
    val uiState: StateFlow<DiaryLogUiState> = _uiState

    init {
        viewModelScope.launch {
            val movieResp = movieApi.getMovieById(movieId)
            if (movieResp.isSuccessful) {
                _uiState.update { it.copy(movie = movieResp.body(), movieLoading = false) }
            } else {
                _uiState.update { it.copy(movieLoading = false) }
            }
        }
        if (isEditMode) {
            viewModelScope.launch {
                val entryResp = diaryApi.getDiaryEntry(entryId)
                if (entryResp.isSuccessful) {
                    val entry = entryResp.body()!!
                    _uiState.update {
                        it.copy(
                            watchedOn = entry.watchedOn,
                            rating = entry.review?.rating,
                            comment = entry.review?.comment ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onWatchedOnChange(value: String) {
        _uiState.update { it.copy(watchedOn = value, error = null) }
    }

    fun onRatingChange(value: Int) {
        _uiState.update { it.copy(rating = value.coerceIn(1, 10), error = null) }
    }

    fun clearRating() {
        _uiState.update { it.copy(rating = null, error = null) }
    }

    fun onCommentChange(value: String) {
        _uiState.update { it.copy(comment = value, error = null) }
    }

    fun save() {
        val watchedOn = _uiState.value.watchedOn.trim()
        val rating = _uiState.value.rating?.coerceIn(1, 10)
        val comment = _uiState.value.comment.trim().ifBlank { null }

        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(watchedOn)) {
            _uiState.update { it.copy(error = "Date must be in YYYY-MM-DD format") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(posting = true, error = null) }
            try {
                val resp = if (isEditMode) {
                    diaryApi.updateDiaryEntry(entryId, DiaryUpdate(watchedOn = watchedOn, rating = rating, comment = comment))
                } else {
                    diaryApi.addToDiary(DiaryCreate(movieId = movieId, watchedOn = watchedOn, rating = rating, comment = comment))
                }

                if (resp.isSuccessful) {
                    _uiState.update { it.copy(posting = false, logged = true) }
                } else {
                    val errorBody = resp.errorBody()?.string() ?: "Unknown error"
                    _uiState.update { it.copy(posting = false, error = errorBody) }
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
