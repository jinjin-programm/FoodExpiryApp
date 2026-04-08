package com.example.foodexpiryapp.data.repository

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.PipelineState
import com.example.foodexpiryapp.domain.repository.YoloDetectionRepository
import com.example.foodexpiryapp.inference.pipeline.DetectionPipeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [YoloDetectionRepository].
 *
 * Thin wrapper following the same pattern as [LlmInferenceRepositoryImpl].
 * Delegates all detection logic to [DetectionPipeline] and maintains
 * observable [PipelineState] for UI consumption.
 *
 * Per D-01: Wraps detection pipeline behind domain interface.
 * Per YOLO-01: Full YOLO+LLM batch detection pipeline.
 */
@Singleton
class YoloDetectionRepositoryImpl @Inject constructor(
    private val pipeline: DetectionPipeline
) : YoloDetectionRepository {

    private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
    override val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    override fun detectFoods(bitmap: Bitmap): Flow<PipelineState> = flow {
        pipeline.detectAndClassify(bitmap).collect { state ->
            _pipelineState.value = state
            emit(state)
        }
    }

    override suspend fun cancelDetection() {
        // Pipeline cancellation handled by collecting flow cancellation
        _pipelineState.value = PipelineState.Cancelled
    }
}
