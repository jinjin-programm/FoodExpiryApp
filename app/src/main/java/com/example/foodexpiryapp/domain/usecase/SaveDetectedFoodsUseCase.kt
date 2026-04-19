package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.ScanSource
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.util.FoodImageStorage
import java.time.LocalDate
import javax.inject.Inject

class SaveDetectedFoodsUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val detectionResultRepository: DetectionResultRepository,
    private val lookupShelfLifeUseCase: LookupShelfLifeUseCase,
    private val foodImageStorage: FoodImageStorage
) {

    data class SaveResult(
        val savedCount: Int,
        val skippedCount: Int,
        val sessionId: String,
        val error: String? = null
    )

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

            val shelfLifeResult = lookupShelfLifeUseCase(entity.foodName, entity.shelfLifeDays)

            detectionResultRepository.updateResult(
                entity.copy(shelfLifeSource = shelfLifeResult.source)
            )

            val foodItem = FoodItem(
                name = entity.foodNameZh.ifBlank { entity.foodName },
                category = shelfLifeResult.category,
                expiryDate = LocalDate.now().plusDays(shelfLifeResult.shelfLifeDays.toLong()),
                quantity = 1,
                location = shelfLifeResult.location,
                dateAdded = LocalDate.now(),
                scanSource = ScanSource.YOLO_SCAN,
                confidence = entity.llmConfidence.coerceAtLeast(entity.confidence),
                notes = buildString {
                    if (entity.foodNameZh.isNotBlank() && entity.foodNameZh != entity.foodName) {
                        append("EN: ${entity.foodName}")
                    }
                    if (shelfLifeResult.source == "auto") {
                        if (isNotEmpty()) append(" | ")
                        append("[ai-estimated]")
                    }
                }
            )

            val insertedId = foodRepository.insertFoodItem(foodItem)

            if (!entity.cropImagePath.isNullOrBlank()) {
                val savedPath = foodImageStorage.saveFromPath(entity.cropImagePath, insertedId)
                if (savedPath != null) {
                    foodRepository.updateFoodItem(foodItem.copy(id = insertedId, imagePath = savedPath))
                }
            }

            savedCount++
        }

        detectionResultRepository.clearSession(sessionId)

        return SaveResult(savedCount, skippedCount, sessionId)
    }
}
