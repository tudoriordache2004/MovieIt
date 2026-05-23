package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.FeedApi
import com.app.movieit.data.auth.NotificationsSeen
import com.app.movieit.data.model.ActivityFeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityFeedUiState(
    val isLoading: Boolean = true,
    val items: List<ActivityFeedItem> = emptyList(),
    val error: String? = null,
    val unreadCount: Int = 0,
)

@HiltViewModel
class ActivityFeedViewModel @Inject constructor(
    private val feedApi: FeedApi,
    private val seen: NotificationsSeen,
) : ViewModel() {

    private val _items = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ActivityFeedUiState> = combine(
        _items,
        _isLoading,
        _error,
        seen.lastSeenFlow,
    ) { items, loading, error, lastSeenIso ->
        val unread = if (lastSeenIso == null) items.size
        else items.count { it.createdAt > lastSeenIso }
        ActivityFeedUiState(
            isLoading = loading,
            items = items,
            error = error,
            unreadCount = unread,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActivityFeedUiState(),
    )

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.update { true }
            _error.update { null }
            try {
                val resp = feedApi.getFeed()
                if (resp.isSuccessful) {
                    _items.update { resp.body().orEmpty() }
                    _isLoading.update { false }
                } else {
                    _error.update { "HTTP ${resp.code()} ${resp.message()}" }
                    _isLoading.update { false }
                }
            } catch (e: Exception) {
                _error.update { e.message }
                _isLoading.update { false }
            }
        }
    }

    fun markAllSeen() {
        val newest = _items.value.firstOrNull()?.createdAt ?: return
        viewModelScope.launch {
            seen.markSeen(newest)
        }
    }
}
