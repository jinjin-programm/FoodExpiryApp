package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.util.AppLog
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import com.example.foodexpiryapp.domain.usecase.SaveDetectedFoodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the ConfirmationFragment.
 *
 * Per D-16: Reads sessionId from SavedStateHandle (nav args).
 * Per D-20: Observes results from Room DB — survives process death.
 * Per D-09: Full-screen confirmation page for batch results.
 * Per D-12: Quick Mode auto-confirm countdown for single items.
 * Per D-14: Save flow via SaveDetectedFoodsUseCase.
 */
@HiltViewModel
class ConfirmationViewModel @Inject constructor(
    private val detectionResultRepository: DetectionResultRepository,
    private val saveDetectedFoodsUseCase: SaveDetectedFoodsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _results = MutableStateFlow<List<DetectionResultEntity>>(emptyList())
    val results: StateFlow<List<DetectionResultEntity>> = _results.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveDetectedFoodsUseCase.SaveResult?>(null)
    val saveResult: StateFlow<SaveDetectedFoodsUseCase.SaveResult?> = _saveResult.asStateFlow()

    // Quick Mode support (D-12)
    private val _quickModeCountdown = MutableStateFlow(0)
    val quickModeCountdown: StateFlow<Int> = _quickModeCountdown.asStateFlow()

    var quickModeEnabled: Boolean = false

    private var quickModeJob: Job? = null

    init {
        if (sessionId.isNotEmpty()) {
            viewModelScope.launch {
                detectionResultRepository.getResults(sessionId).collect { entities ->
                    _results.value = entities
                }
            }
        }
    }

    /**
     * Updates the name of a detected item after user edits.
     */
    fun updateItemName(entityId: Long, newName: String, newNameZh: String) {
        viewModelScope.launch {
            val current = _results.value.find { it.id == entityId } ?: return@launch
            detectionResultRepository.updateResult(
                current.copy(foodName = newName, foodNameZh = newNameZh, status = DetectionResultEntity.STATUS_CLASSIFIED)
            )
        }
    }

    /**
     * Marks an item as removed (soft delete).
     */
    fun removeItem(entityId: Long) {
        viewModelScope.launch {
            val current = _results.value.find { it.id == entityId } ?: return@launch
            detectionResultRepository.updateResult(
                current.copy(status = "REMOVED")
            )
        }
    }

    /**
     * Returns only active (non-removed) results for the save flow.
     */
    fun getActiveResults(): List<DetectionResultEntity> =
        _results.value.filter { it.status != "REMOVED" }

    /**
     * Per D-14: Save all detected items via SaveDetectedFoodsUseCase.
     * Applies DefaultAttributeEngine, batch inserts to Room, cleans up temp data.
     */
    fun saveAll() {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val result = saveDetectedFoodsUseCase(sessionId)
                _saveResult.value = result
            } catch (e: Exception) {
                AppLog.e("ConfirmationVM", "Error saving detection results", e)
                // Per D-12: Report errors via StateFlow instead of silent swallowing
                _saveResult.value = SaveDetectedFoodsUseCase.SaveResult(
                    savedCount = 0,
                    skippedCount = getActiveResults().size,
                    sessionId = sessionId,
                    error = e.message ?: "Save failed"
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Per D-12: Quick Mode auto-confirm countdown.
     * Only triggers when Quick Mode is ON and exactly 1 active item exists.
     * Countdown from 3 → 1, then auto-saves.
     */
    fun startQuickModeCountdownIfNeeded() {
        if (!quickModeEnabled) return
        val activeResults = getActiveResults()
        if (activeResults.size != 1) return

        quickModeJob?.cancel()
        quickModeJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _quickModeCountdown.value = i
                delay(1000)
            }
            saveAll()
        }
    }

    /**
     * Per D-12: Cancel Quick Mode countdown.
     * Called on any user interaction (touch, scroll, click).
     */
    fun cancelQuickModeCountdown() {
        quickModeJob?.cancel()
        quickModeJob = null
        _quickModeCountdown.value = 0
    }

    /**
     * Clears all results for this session from Room.
     */
    fun clearSession() {
        viewModelScope.launch {
            detectionResultRepository.clearSession(sessionId)
        }
    }

    fun getSessionId(): String = sessionId

    override fun onCleared() {
        super.onCleared()
        quickModeJob?.cancel()
    }
}
