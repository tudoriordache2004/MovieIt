package com.app.movieit.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.LensApi
import com.app.movieit.data.model.LensAnalyzeResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class MovieItLensUiState(
    val selectedMode: String = "vibe",
    val permissionGranted: Boolean = false,
    val permissionRequested: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val result: LensAnalyzeResponse? = null,
    val showSheet: Boolean = false,
    val pickedImageUri: Uri? = null
)

@HiltViewModel
class MovieItLensViewModel @Inject constructor(
    private val lensApi: LensApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieItLensUiState())
    val uiState: StateFlow<MovieItLensUiState> = _uiState

    fun selectMode(mode: String) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted, permissionRequested = true) }
    }

    fun onCaptureError() {
        _uiState.update { it.copy(loading = false, error = "Capture failed. Tap the shutter again.") }
    }

    fun onCameraError() {
        _uiState.update { it.copy(error = "Camera unavailable.") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setPickedImage(uri: Uri) {
        _uiState.update { it.copy(pickedImageUri = uri, result = null, showSheet = false, error = null) }
    }

    fun clearPickedImage() {
        _uiState.update { it.copy(pickedImageUri = null, error = null) }
    }

    fun dismissResult() {
        _uiState.update { it.copy(showSheet = false, pickedImageUri = null) }
    }

    fun analyze(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val imagePart = MultipartBody.Part.createFormData(
                    "image",
                    "lens_capture.jpg",
                    file.asRequestBody("image/jpeg".toMediaType())
                )
                val modePart = _uiState.value.selectedMode.toRequestBody("text/plain".toMediaType())

                val resp = lensApi.analyzeLens(imagePart, modePart, 5)
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    if (body.recommendations.isEmpty()) {
                        _uiState.update {
                            it.copy(loading = false, error = "No matching films found. Try a different photo or mode.")
                        }
                    } else {
                        _uiState.update { it.copy(loading = false, result = body, showSheet = true) }
                    }
                } else {
                    _uiState.update { it.copy(loading = false, error = "Analysis failed. Try again.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Network error. Check your connection and try again.") }
            } finally {
                try { file.delete() } catch (_: Exception) {}
            }
        }
    }
}
