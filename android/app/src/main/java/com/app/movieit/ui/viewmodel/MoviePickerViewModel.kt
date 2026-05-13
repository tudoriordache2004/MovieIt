package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.movieit.data.api.MoviePickerApi
import com.app.movieit.data.model.MoviePickerRequest
import com.app.movieit.data.model.MoviePickerResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoviePickerUiState(
    val selectedMood: String? = null,
    val prompt: String = "",
    val intensity: String = "medium",
    val avoid: Set<String> = emptySet(),
    val loading: Boolean = false,
    val error: String? = null,
    val validationError: String? = null,
    val result: MoviePickerResponse? = null,
    val currentStep: Int = 0,
    val maxVisitedStep: Int = 0,
    val pendingResultNavigation: Boolean = false
) {
    val canSubmit: Boolean
        get() = selectedMood != null || prompt.isNotBlank()
}

@HiltViewModel
class MoviePickerViewModel @Inject constructor(
    private val moviePickerApi: MoviePickerApi
) : ViewModel() {

    companion object {
        const val TOTAL_STEPS = 4

        private val moodBlockedAvoidSignals = mapOf(
            "dark" to setOf("dark"),
            "melancholic" to setOf("sad"),
            "intense" to setOf("violence")
        )

        fun blockedAvoidSignalsForMood(mood: String?): Set<String> =
            moodBlockedAvoidSignals[mood].orEmpty()
    }

    private val _uiState = MutableStateFlow(MoviePickerUiState())
    val uiState: StateFlow<MoviePickerUiState> = _uiState

    fun selectMood(mood: String) {
        _uiState.update { state ->
            val selectedMood = if (state.selectedMood == mood) null else mood
            val blockedAvoids = blockedAvoidSignalsForMood(selectedMood)
            state.copy(
                selectedMood = selectedMood,
                avoid = state.avoid - blockedAvoids,
                validationError = null
            )
        }
    }

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(prompt = text, validationError = null) }
    }

    fun selectIntensity(level: String) {
        _uiState.update { it.copy(intensity = level) }
    }

    fun toggleAvoid(signal: String) {
        _uiState.update { state ->
            if (signal in blockedAvoidSignalsForMood(state.selectedMood)) {
                return@update state
            }
            val updated = if (signal in state.avoid) state.avoid - signal else state.avoid + signal
            state.copy(avoid = updated)
        }
    }

    fun findMovie() {
        val state = _uiState.value
        if (state.selectedMood == null && state.prompt.isBlank()) {
            _uiState.update { it.copy(validationError = "Choose a mood or describe what you need tonight.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, validationError = null) }
            try {
                val request = MoviePickerRequest(
                    mood = state.selectedMood,
                    prompt = state.prompt.ifBlank { null },
                    intensity = state.intensity,
                    avoid = state.avoid.toList()
                )
                val resp = moviePickerApi.createSession(request)
                if (resp.isSuccessful) {
                    _uiState.update {
                        it.copy(loading = false, result = resp.body(), pendingResultNavigation = true)
                    }
                } else {
                    _uiState.update { it.copy(loading = false, error = "Couldn't find a match. Try a different mood.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Something went wrong. Check your connection and try again.") }
            }
        }
    }

    fun anotherPick() {
        val sessionId = _uiState.value.result?.sessionId ?: return
        val hasMore = _uiState.value.result?.hasMore ?: false
        if (!hasMore) {
            _uiState.update { it.copy(error = "You've seen the strongest matches. Try changing your mood.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val resp = moviePickerApi.getNextPick(sessionId)
                if (resp.isSuccessful) {
                    _uiState.update { it.copy(loading = false, result = resp.body()) }
                } else {
                    _uiState.update { it.copy(loading = false, error = "Couldn't get another pick. Try again.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = "Something went wrong. Check your connection and try again.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun nextStep() {
        _uiState.update {
            val next = (it.currentStep + 1).coerceAtMost(TOTAL_STEPS - 1)
            it.copy(currentStep = next, maxVisitedStep = maxOf(it.maxVisitedStep, next))
        }
    }

    fun prevStep() {
        _uiState.update {
            val prev = (it.currentStep - 1).coerceAtLeast(0)
            it.copy(currentStep = prev)
        }
    }

    fun goToStep(step: Int) {
        _uiState.update {
            if (step < 0 || step > it.maxVisitedStep) return@update it
            it.copy(currentStep = step)
        }
    }

    fun consumeResultNavigation() {
        _uiState.update { it.copy(pendingResultNavigation = false) }
    }

}
