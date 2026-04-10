package com.example.foodexpiryapp.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.PipelineState
import com.example.foodexpiryapp.domain.repository.YoloDetectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the YOLO scan tab.
 *
 * Orchestrates the detection pipeline (YOLO detect → LLM classify) and persists
 * results to Room before navigation so they survive process death (D-17/D-20).
 * Per T-06-22: cancelDetection() prevents resource exhaustion from repeated captures.
 */
@HiltViewModel
class YoloScanViewModel @Inject constructor(
    private val yoloDetectionRepository: YoloDetectionRepository,
    private val detectionResultRepository: DetectionResultRepository
) : ViewModel() {

    sealed class YoloScanUiState {
        data object Ready : YoloScanUiState()
        data object Detecting : YoloScanUiState()
        data class Classifying(val current: Int, val total: Int) : YoloScanUiState()
        data class Complete(val sessionId: String) : YoloScanUiState()
        data class Error(val message: String) : YoloScanUiState()
        data object NoDetection : YoloScanUiState() // Per D-19: 0 items detected
    }

    private val _uiState = MutableStateFlow<YoloScanUiState>(YoloScanUiState.Ready)
    val uiState: StateFlow<YoloScanUiState> = _uiState.asStateFlow()

    /**
     * Per D-06: Expose detection results for overlay rendering.
     * Updated as each item progresses through the pipeline.
     */
    private val _detections = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detections: StateFlow<List<DetectionResult>> = _detections.asStateFlow()

    private var pipelineJob: Job? = null

    /**
     * Starts the full detection pipeline on a captured bitmap.
     * Per D-05: Unified detection pipeline — YOLO always runs first.
     * Per YOLO-04: Sequential LLM classification for each crop.
     */
    fun startDetection(bitmap: Bitmap) {
        pipelineJob?.cancel()
        _detections.value = emptyList()
        pipelineJob = viewModelScope.launch {
            yoloDetectionRepository.detectFoods(bitmap).collect { state ->
                _uiState.value = when (state) {
                    is PipelineState.Detecting -> YoloScanUiState.Detecting
                    // Per D-06: Show bounding boxes immediately after YOLO completes
                    is PipelineState.Detected -> {
                        _detections.value = state.detections
                        YoloScanUiState.Detecting // Still in detection phase from UI perspective
                    }
                    is PipelineState.Classifying -> {
                        YoloScanUiState.Classifying(state.current, state.total)
                    }
                    is PipelineState.Complete -> {
                        // Save results to Room for process death survival (D-17/D-20)
                        saveResults(state.result.results, state.result.sessionId)
                        _detections.value = state.result.results
                        if (state.result.results.isEmpty()) {
                            YoloScanUiState.NoDetection // D-19
                        } else {
                            YoloScanUiState.Complete(state.result.sessionId)
                        }
                    }
                    is PipelineState.Error -> YoloScanUiState.Error(state.message)
                    is PipelineState.Cancelled -> YoloScanUiState.Ready
                    is PipelineState.Idle -> YoloScanUiState.Ready
                }
            }
        }
    }

    /**
     * Cancels an in-progress detection pipeline.
     * Per T-06-22: mitigates resource exhaustion from repeated captures.
     */
    fun cancelDetection() {
        pipelineJob?.cancel()
        viewModelScope.launch { yoloDetectionRepository.cancelDetection() }
        _uiState.value = YoloScanUiState.Ready
        _detections.value = emptyList()
    }

    /**
     * Resets state to Ready (e.g., after NoDetection was shown and user wants to retry).
     */
    fun resetToReady() {
        _uiState.value = YoloScanUiState.Ready
    }

    /**
     * Persists detection results to Room temp table.
     * Maps domain DetectionResult → DetectionResultEntity.
     */
    private suspend fun saveResults(results: List<DetectionResult>, sessionId: String) {
        val entities = results.mapIndexed { index, result ->
            DetectionResultEntity(
                sessionId = sessionId,
                indexInSession = index,
                foodName = result.foodIdentification?.name ?: result.label,
                foodNameZh = result.foodIdentification?.nameZh ?: "",
                category = result.category.name,
                confidence = result.confidence,
                llmConfidence = result.foodIdentification?.confidence ?: 0f,
                status = when (result.status) {
                    DetectionStatus.CLASSIFIED -> DetectionResultEntity.STATUS_CLASSIFIED
                    DetectionStatus.FAILED -> DetectionResultEntity.STATUS_FAILED
                    DetectionStatus.PENDING -> DetectionResultEntity.STATUS_PENDING
                },
                boundingBoxLeft = result.boundingBox.left,
                boundingBoxTop = result.boundingBox.top,
                boundingBoxRight = result.boundingBox.right,
                boundingBoxBottom = result.boundingBox.bottom
            )
        }
        detectionResultRepository.insertResults(entities)
    }

    override fun onCleared() {
        super.onCleared()
        pipelineJob?.cancel()
    }
}
