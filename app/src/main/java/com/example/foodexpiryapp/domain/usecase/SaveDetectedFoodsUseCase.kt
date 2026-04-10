package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import com.example.foodexpiryapp.domain.engine.DefaultAttributeEngine
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.ScanSource
import com.example.foodexpiryapp.domain.repository.FoodRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * UseCase for batch-saving detected food items to the permanent inventory.
 *
 * Per D-14: Save flow — iterate items, apply DefaultAttributeEngine, batch insert to Room,
 * popBackStack to YoloScanFragment, Snackbar with "View" action.
 *
 * Per D-13: Each item's category and shelf life are inferred by DefaultAttributeEngine
 * using keyword matching on the food name (EN + ZH).
 */
class SaveDetectedFoodsUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val detectionResultRepository: DetectionResultRepository,
    private val attributeEngine: DefaultAttributeEngine
) {

    data class SaveResult(
        val savedCount: Int,
        val skippedCount: Int,
        val sessionId: String,
        val error: String? = null  // Per D-12: Error message for UI display
    )

    /**
     * Saves all active (non-removed, non-failed) detection results as FoodItems
     * in the permanent inventory, then cleans up the temp session data.
     *
     * @param sessionId The detection session whose results to save.
     * @return SaveResult with counts of saved and skipped items.
     */
    suspend operator fun invoke(sessionId: String): SaveResult {
        val entities = detectionResultRepository.getResultsSync(sessionId)
        val activeEntities = entities.filter { it.status != "REMOVED" && it.status != "FAILED" }

        var savedCount = 0
        var skippedCount = 0

        for (entity in activeEntities) {
            if (entity.foodName.equals("Unknown", ignoreCase = true) || entity.foodName.isBlank()) {
                skippedCount++
                continue
            }

            val defaults = attributeEngine.inferDefaults(entity.foodName, entity.foodNameZh)

            val foodItem = FoodItem(
                name = entity.foodNameZh.ifBlank { entity.foodName },
                category = defaults.category,
                expiryDate = LocalDate.now().plusDays(defaults.shelfLifeDays.toLong()),
                quantity = 1,
                location = defaults.location,
                dateAdded = LocalDate.now(),
                scanSource = ScanSource.YOLO_SCAN,
                confidence = entity.llmConfidence.coerceAtLeast(entity.confidence),
                notes = if (entity.foodNameZh.isNotBlank() && entity.foodNameZh != entity.foodName) "EN: ${entity.foodName}" else ""
            )

            foodRepository.insertFoodItem(foodItem)
            savedCount++
        }

        // Cleanup temp data after save
        detectionResultRepository.clearSession(sessionId)

        return SaveResult(savedCount, skippedCount, sessionId)
    }
}
