package com.app.movieit.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.model.ReviewCreate
import com.app.movieit.data.model.ReviewOut
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
    val myRating: Int = 8,
    val myComment: String = "",
    val posting: Boolean = false,
    val reviewPosted: Boolean = false
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewApi: ReviewApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int =
        savedStateHandle.get<Int>("movieId") ?: error("movieId missing")

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = reviewApi.getReviewsByMovie(movieId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, reviews = resp.body().orEmpty()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "HTTP ${resp.code()} ${resp.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onRatingChange(value: Int) {
        _uiState.update { it.copy(myRating = value) }
    }

    fun onCommentChange(value: String) {
        _uiState.update { it.copy(myComment = value) }
    }

    fun postReview() {
        val rating = _uiState.value.myRating.coerceIn(1, 10)
        val comment = _uiState.value.myComment.trim().ifBlank { null }

        viewModelScope.launch {
            _uiState.update { it.copy(posting = true, error = null) }
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
                _uiState.update { it.copy(posting = false, error = e.message) }
            }
        }
    }

    fun consumeReviewPosted() {
        _uiState.update { it.copy(reviewPosted = false) }
    }
}