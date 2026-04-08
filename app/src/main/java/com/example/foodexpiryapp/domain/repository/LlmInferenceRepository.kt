package com.example.foodexpiryapp.domain.repository

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.FoodIdentification
import com.example.foodexpiryapp.domain.model.ModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for LLM food identification.
 * Per D-01: Domain interface — implementation in data layer.
 * Per D-02: Single method API surface — inferFood(Bitmap): Flow<FoodIdentification>.
 *
 * Model status is observed through `modelState: StateFlow<ModelState>`, not
 * through separate query methods. The implementation keeps this flow updated
 * as model download progresses, loads, and becomes ready for inference.
 *
 * The implementation handles model loading, prompt building, inference,
 * and JSON parsing internally. Callers just collect the Flow.
 */
interface LlmInferenceRepository {

    /**
     * Identifies food from a bitmap image using on-device LLM.
     *
     * @param bitmap The cropped food image to identify
     * @return Flow emitting FoodIdentification result (single emission)
     */
    fun inferFood(bitmap: Bitmap): Flow<FoodIdentification>

    /**
     * Observable model state for UI to display status.
     * Emits ModelState values: NotDownloaded, Downloading, Verifying,
     * Loading, Ready, Error. UI collects this reactively — no polling.
     *
     * This replaces the earlier isModelReady()/getModelStatus() methods
     * to stay true to D-02's "single method API surface" for inference.
     */
    val modelState: StateFlow<ModelState>
}
