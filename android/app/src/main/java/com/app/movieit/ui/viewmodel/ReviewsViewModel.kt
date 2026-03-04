package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.ReviewCreate
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.ReviewUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val reviews: List<ReviewOut> = emptyList(),

    val currentUserId: Int? = null,

    val myRating: Int = 1,
    val myComment: String = "",
    val posting: Boolean = false,
    val reviewPosted: Boolean = false,

    // campurile pentru edit/delete
    val editingReviewId: Int? = null,
    val editRating: Int = 1,
    val editComment: String = "",
    val savingEdit: Boolean = false,
    val deletingReviewId: Int? = null,
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewApi: ReviewApi,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int =
        savedStateHandle.get<Int>("movieId") ?: error("movieId missing")

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionManager.state.collect { s ->
                _uiState.update { it.copy(currentUserId = s.userId) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = reviewApi.getReviewsByMovie(movieId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, reviews = resp.body().orEmpty(),) }
                } else {
                    _uiState.update { it.copy(
                        loading = false,
                        error = "HTTP ${resp.code()} ${resp.message()}",
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message,) }
            }
        }
    }

    fun onRatingChange(value: Int) {
        _uiState.update { it.copy(myRating = value,) }
    }

    fun onCommentChange(value: String) {
        _uiState.update { it.copy(myComment = value,) }
    }

    fun postReview() {
        val rating = _uiState.value.myRating.coerceIn(1, 10)
        val comment = _uiState.value.myComment.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(posting = true,) }
            try {
                val resp = reviewApi.createReview(
                    ReviewCreate(movieId = movieId, rating = rating, comment = comment)
                )
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(posting = false, myComment = "", reviewPosted = true) }
                    load() // refresh list
                } else {
                    _uiState.update { it.copy(posting = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(posting = false, error = e.message) }            }
        }
    }

    fun consumeReviewPosted() {
        _uiState.update { it.copy(reviewPosted = false) }
    }

    // EDIT
    fun startEdit(review: ReviewOut) {
        _uiState.update {
            it.copy(
                editingReviewId = review.id,
                editRating = review.rating,
                editComment = review.comment.orEmpty(),
                error = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingReviewId = null) }
    }

    fun onEditRatingChange(value: Int) {
        _uiState.update { it.copy(editRating = value.coerceIn(1, 10)) }
    }

    fun onEditCommentChange(value: String) {
        _uiState.update { it.copy(editComment = value) }
    }

    fun saveEdit() {
        val reviewId = _uiState.value.editingReviewId ?: return
        val rating = _uiState.value.editRating.coerceIn(1, 10)
        val comment = _uiState.value.editComment.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(savingEdit = true, error = null) }
            try {
                val resp = reviewApi.updateReview(
                    reviewId = reviewId,
                    body = ReviewUpdate(rating = rating, comment = comment)
                )
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(savingEdit = false, editingReviewId = null, reviewPosted = true) }
                    load()
                } else {
                    _uiState.update { it.copy(savingEdit = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(savingEdit = false, error = e.message) }
            }
        }
    }

    fun deleteReview(reviewId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingReviewId = reviewId, error = null) }
            try {
                val resp = reviewApi.deleteReview(reviewId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(deletingReviewId = null, editingReviewId = null, reviewPosted = true) }
                    load()
                } else {
                    _uiState.update { it.copy(deletingReviewId = null, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(deletingReviewId = null, error = e.message) }
            }
        }
    }
}