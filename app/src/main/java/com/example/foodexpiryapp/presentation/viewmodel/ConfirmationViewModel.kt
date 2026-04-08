package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 */
@HiltViewModel
class ConfirmationViewModel @Inject constructor(
    private val detectionResultRepository: DetectionResultRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _results = MutableStateFlow<List<DetectionResultEntity>>(emptyList())
    val results: StateFlow<List<DetectionResultEntity>> = _results.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

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
     * Clears all results for this session from Room.
     */
    fun clearSession() {
        viewModelScope.launch {
            detectionResultRepository.clearSession(sessionId)
        }
    }

    fun getSessionId(): String = sessionId
}
