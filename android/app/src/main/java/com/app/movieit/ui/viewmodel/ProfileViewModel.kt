package com.app.movieit.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val uploading: Boolean = false,
    val diaryCount: Int = 0,
    val watchlistCount: Int = 0,
    val reviewsCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi,
    private val diaryApi: DiaryApi,
    private val watchlistApi: WatchlistApi,
    private val reviewApi: ReviewApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        viewModelScope.launch {
            sessionManager.state.collect { s ->
                _uiState.update { it.copy(username = s.username) }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val meResp = authApi.getMe()
                val diaryCountResp = diaryApi.getDiaryCount()
                val watchResp = watchlistApi.getMyWatchlist()
                val reviewsCountResp = reviewApi.getMyReviewsCount()

                _uiState.update {
                    it.copy(
                        loading = false,
                        profilePictureUrl = if (meResp.isSuccessful) meResp.body()?.profilePictureUrl else it.profilePictureUrl,
                        diaryCount = if (diaryCountResp.isSuccessful) (diaryCountResp.body()?.count ?: 0) else it.diaryCount,
                        watchlistCount = if (watchResp.isSuccessful) (watchResp.body()?.size ?: 0) else it.watchlistCount,
                        reviewsCount = if (reviewsCountResp.isSuccessful) (reviewsCountResp.body()?.count ?: 0) else it.reviewsCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun uploadProfilePicture(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploading = true) }
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val part = MultipartBody.Part.createFormData("file", "profile.$ext", requestBody)

                val resp = authApi.uploadProfilePicture(part)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(uploading = false, profilePictureUrl = resp.body()?.profilePictureUrl)
                    }
                } else {
                    _uiState.update { it.copy(uploading = false, error = "Upload failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(uploading = false, error = e.message) }
            }
        }
    }
}
