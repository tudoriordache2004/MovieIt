package com.app.movieit.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.api.SearchApi
import com.app.movieit.data.api.UserApi
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.auth.SessionManager
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.data.model.Movie
import com.app.movieit.data.model.MovieMini
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.UserProfileUpdateRequest
import com.app.movieit.data.model.WatchlistItemWithMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    val userId: Int? = null,
    val username: String? = null,
    val profilePictureUrl: String? = null,
    val bio: String? = null,
    val coverPhotoUrl: String? = null,
    val topMovies: List<com.app.movieit.data.model.MovieMini> = emptyList(),
    val averageRating: Double? = null,
    val uploading: Boolean = false,
    val coverUploading: Boolean = false,
    val diaryCount: Int = 0,
    val watchlistCount: Int = 0,
    val reviewsCount: Int = 0,
    val diaryEntries: List<DiaryOut> = emptyList(),
    val reviews: List<ReviewOut> = emptyList(),
    val watchlistItems: List<WatchlistItemWithMovie> = emptyList(),
    val favoriteGenre: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    // edit state (mirrors UserProfileViewModel)
    val editing: Boolean = false,
    val editBio: String = "",
    val editTopMovies: List<com.app.movieit.data.model.MovieMini> = emptyList(),
    val editSaving: Boolean = false,
    val editError: String? = null,
    val searchQuery: String = "",
    val searchResults: List<com.app.movieit.data.model.Movie> = emptyList(),
    val searchLoading: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi,
    private val diaryApi: DiaryApi,
    private val watchlistApi: WatchlistApi,
    private val reviewApi: ReviewApi,
    private val userApi: UserApi,
    private val searchApi: SearchApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private var searchJob: Job? = null

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
                coroutineScope {
                    val meDeferred = async { authApi.getMe() }
                    val diaryDeferred = async { diaryApi.getMyDiary() }
                    val watchDeferred = async { watchlistApi.getMyWatchlist() }
                    val reviewDeferred = async { reviewApi.getMyReviews() }
                    val statsDeferred = async { authApi.getProfileStats() }

                    val meResp = meDeferred.await()
                    val myId = if (meResp.isSuccessful) meResp.body()?.id else null
                    val profileDeferred = myId?.let { async { userApi.getUserProfile(it) } }

                    val diaryResp = diaryDeferred.await()
                    val watchResp = watchDeferred.await()
                    val reviewResp = reviewDeferred.await()
                    val statsResp = statsDeferred.await()
                    val profileResp = profileDeferred?.await()

                    _uiState.update {
                        it.copy(
                            loading = false,
                            userId = myId ?: it.userId,
                            profilePictureUrl = if (meResp.isSuccessful) meResp.body()?.profilePictureUrl else it.profilePictureUrl,
                            watchlistCount = if (watchResp.isSuccessful) (watchResp.body()?.size ?: 0) else it.watchlistCount,
                            diaryEntries = if (diaryResp.isSuccessful) diaryResp.body().orEmpty() else it.diaryEntries,
                            reviews = if (reviewResp.isSuccessful) reviewResp.body().orEmpty() else it.reviews,
                            watchlistItems = if (watchResp.isSuccessful) watchResp.body().orEmpty() else it.watchlistItems,
                            favoriteGenre = if (statsResp.isSuccessful) statsResp.body()?.favoriteGenre else it.favoriteGenre,
                            diaryCount = if (profileResp?.isSuccessful == true) profileResp.body()?.moviesWatchedCount ?: it.diaryCount else it.diaryCount,
                            reviewsCount = if (profileResp?.isSuccessful == true) profileResp.body()?.reviewsCount ?: it.reviewsCount else it.reviewsCount,
                            followersCount = if (profileResp?.isSuccessful == true) profileResp.body()?.followersCount ?: it.followersCount else it.followersCount,
                            followingCount = if (profileResp?.isSuccessful == true) profileResp.body()?.followingCount ?: it.followingCount else it.followingCount,
                            bio = if (profileResp?.isSuccessful == true) profileResp.body()?.bio else it.bio,
                            coverPhotoUrl = if (profileResp?.isSuccessful == true) profileResp.body()?.coverPhotoUrl else it.coverPhotoUrl,
                            topMovies = if (profileResp?.isSuccessful == true) profileResp.body()?.topMovies ?: it.topMovies else it.topMovies,
                            averageRating = if (profileResp?.isSuccessful == true) profileResp.body()?.averageRating else it.averageRating,
                        )
                    }
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
                val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
                val part = MultipartBody.Part.createFormData("file", "profile.$ext", requestBody)
                val resp = authApi.uploadProfilePicture(part)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(uploading = false, profilePictureUrl = resp.body()?.profilePictureUrl) }
                } else {
                    _uiState.update { it.copy(uploading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(uploading = false) }
            }
        }
    }

    fun uploadCover(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(coverUploading = true) }
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = stream.readBytes()
                stream.close()
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaType())
                val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
                val part = MultipartBody.Part.createFormData("file", "cover.$ext", requestBody)
                val resp = authApi.uploadCoverPhoto(part)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(coverUploading = false, coverPhotoUrl = resp.body()?.coverPhotoUrl) }
                    load()
                } else {
                    _uiState.update { it.copy(coverUploading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(coverUploading = false) }
            }
        }
    }

    // ── Edit profile ─────────────────────────────────────────────────────────

    fun openEdit() {
        val s = _uiState.value
        _uiState.update { it.copy(editing = true, editBio = s.bio ?: "", editTopMovies = s.topMovies, editError = null, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeEdit() {
        searchJob?.cancel()
        _uiState.update { it.copy(editing = false, editBio = "", editTopMovies = emptyList(), editError = null, searchQuery = "", searchResults = emptyList(), searchLoading = false) }
    }

    fun updateBio(bio: String) {
        if (bio.length <= 150) _uiState.update { it.copy(editBio = bio) }
    }

    fun addTopMovie(movie: Movie) {
        val current = _uiState.value.editTopMovies
        if (current.size >= 4 || current.any { it.id == movie.id }) return
        _uiState.update { it.copy(editTopMovies = current + MovieMini(movie.id, movie.title, movie.posterUrl), searchQuery = "", searchResults = emptyList()) }
    }

    fun removeTopMovieAtIndex(index: Int) {
        val current = _uiState.value.editTopMovies.toMutableList()
        if (index in current.indices) current.removeAt(index)
        _uiState.update { it.copy(editTopMovies = current) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) { _uiState.update { it.copy(searchResults = emptyList(), searchLoading = false) }; return }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(searchLoading = true) }
            try {
                val resp = searchApi.suggest(query, 8)
                if (resp.isSuccessful) _uiState.update { it.copy(searchResults = resp.body()?.movies ?: emptyList(), searchLoading = false) }
                else _uiState.update { it.copy(searchLoading = false) }
            } catch (_: Exception) { _uiState.update { it.copy(searchLoading = false) } }
        }
    }

    fun saveProfile() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(editSaving = true, editError = null) }
            try {
                val resp = userApi.updateMyProfile(UserProfileUpdateRequest(bio = s.editBio.ifBlank { null }, topMovieIds = s.editTopMovies.map { it.id }))
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    _uiState.update { it.copy(editing = false, editSaving = false, bio = body.bio, topMovies = body.topMovies, coverPhotoUrl = body.coverPhotoUrl) }
                } else {
                    _uiState.update { it.copy(editSaving = false, editError = "Failed to save (${resp.code()})") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(editSaving = false, editError = e.message) }
            }
        }
    }
}
