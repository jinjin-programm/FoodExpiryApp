package com.example.foodexpiryapp.domain.usecase

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.FoodIdentification
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for identifying food from an image using on-device LLM.
 * Per D-01: ViewModels call this UseCase, NOT the raw engine.
 * Per D-02: Single method — [invoke](bitmap) returns [Flow] of [FoodIdentification].
 *
 * This is a thin wrapper around [LlmInferenceRepository.inferFood] following
 * the existing UseCase pattern (see [GetAllFoodItemsUseCase]).
 */
class IdentifyFoodUseCase @Inject constructor(
    private val repository: LlmInferenceRepository
) {
    operator fun invoke(bitmap: Bitmap): Flow<FoodIdentification> {
        return repository.inferFood(bitmap)
    }
}
