package com.app.movieit.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.api.SearchApi
import com.app.movieit.data.api.UserApi
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.data.model.Movie
import com.app.movieit.data.model.MovieMini
import com.app.movieit.data.model.PublicProfileOut
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.UserProfileUpdateRequest
import com.app.movieit.data.model.WatchlistItemWithMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

enum class ProfileTab { DIARY, REVIEWS, WATCHLIST }

data class UserProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val profile: PublicProfileOut? = null,
    val diaryEntries: List<DiaryOut> = emptyList(),
    val reviews: List<ReviewOut> = emptyList(),
    val watchlistItems: List<WatchlistItemWithMovie> = emptyList(),
    val followBusy: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.DIARY,
    // edit dialog state
    val editing: Boolean = false,
    val editBio: String = "",
    val editTopMovies: List<MovieMini> = emptyList(),
    val editSaving: Boolean = false,
    val editError: String? = null,
    val coverUploading: Boolean = false,
    // movie search within the edit picker
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val searchLoading: Boolean = false,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userApi: UserApi,
    private val authApi: AuthApi,
    private val searchApi: SearchApi,
    private val diaryApi: DiaryApi,
    private val reviewApi: ReviewApi,
    private val watchlistApi: WatchlistApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: Int = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState

    private var searchJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = userApi.getUserProfile(userId)
                val diaryResp = diaryApi.getUserDiary(userId)
                val reviewsResp = reviewApi.getReviewsByUser(userId)
                val watchlistResp = watchlistApi.getUserWatchlist(userId)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            profile = resp.body(),
                            diaryEntries = if (diaryResp.isSuccessful) diaryResp.body().orEmpty() else it.diaryEntries,
                            reviews = if (reviewsResp.isSuccessful) reviewsResp.body().orEmpty() else it.reviews,
                            watchlistItems = if (watchlistResp.isSuccessful) watchlistResp.body().orEmpty() else it.watchlistItems,
                        )
                    }
                } else {
                    _uiState.update { it.copy(loading = false, error = "HTTP ${resp.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(followBusy = true) }
            try {
                val resp = if (profile.isFollowing) {
                    userApi.unfollowUser(userId)
                } else {
                    userApi.followUser(userId)
                }
                if (resp.isSuccessful) {
                    val status = resp.body()!!
                    _uiState.update { state ->
                        state.copy(
                            followBusy = false,
                            profile = state.profile?.copy(
                                isFollowing = status.isFollowing,
                                followersCount = status.followersCount,
                            ),
                        )
                    }
                } else {
                    _uiState.update { it.copy(followBusy = false, error = "Action failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(followBusy = false, error = e.message) }
            }
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // ── Edit profile ─────────────────────────────────────────────────────────

    fun openEdit() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                editing = true,
                editBio = profile.bio ?: "",
                editTopMovies = profile.topMovies,
                editError = null,
                searchQuery = "",
                searchResults = emptyList(),
            )
        }
    }

    fun closeEdit() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                editing = false,
                editBio = "",
                editTopMovies = emptyList(),
                editError = null,
                searchQuery = "",
                searchResults = emptyList(),
                searchLoading = false,
            )
        }
    }

    fun updateBio(bio: String) {
        if (bio.length <= 150) _uiState.update { it.copy(editBio = bio) }
    }

    fun addTopMovie(movie: Movie) {
        val current = _uiState.value.editTopMovies
        if (current.size >= 4) return
        if (current.any { it.id == movie.id }) return
        val mini = MovieMini(id = movie.id, title = movie.title, posterUrl = movie.posterUrl)
        _uiState.update {
            it.copy(
                editTopMovies = current + mini,
                searchQuery = "",
                searchResults = emptyList(),
            )
        }
    }

    fun removeTopMovieAtIndex(index: Int) {
        val current = _uiState.value.editTopMovies.toMutableList()
        if (index in current.indices) current.removeAt(index)
        _uiState.update { it.copy(editTopMovies = current) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), searchLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(searchLoading = true) }
            try {
                val resp = searchApi.suggest(query, 8)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            searchResults = resp.body()?.movies ?: emptyList(),
                            searchLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(searchLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(searchLoading = false) }
            }
        }
    }

    fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(editSaving = true, editError = null) }
            try {
                val resp = userApi.updateMyProfile(
                    UserProfileUpdateRequest(
                        bio = state.editBio.ifBlank { null },
                        topMovieIds = state.editTopMovies.map { it.id },
                    )
                )
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            profile = resp.body(),
                            editing = false,
                            editSaving = false,
                            editTopMovies = emptyList(),
                            editBio = "",
                        )
                    }
                } else {
                    _uiState.update { it.copy(editSaving = false, editError = "Failed to save (${resp.code()})") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(editSaving = false, editError = e.message) }
            }
        }
    }

    fun uploadCover(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(coverUploading = true) }
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val part = MultipartBody.Part.createFormData("file", "cover.$ext", requestBody)
                val resp = authApi.uploadCoverPhoto(part)
                if (resp.isSuccessful) {
                    load()
                }
            } catch (_: Exception) {
                // silently fail — cover stays unchanged
            } finally {
                _uiState.update { it.copy(coverUploading = false) }
            }
        }
    }
}
