package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.data.local.database.FoodItemEntity
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.RiskLevel
import com.example.foodexpiryapp.domain.model.ScanSource
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.util.AppLog
import java.time.LocalDate

/**
 * Maps between FoodItemEntity (data layer) and FoodItem (domain layer).
 */
object FoodItemMapper {

    private const val TAG = "FoodItemMapper"

    fun entityToDomain(entity: FoodItemEntity): FoodItem {
        return FoodItem(
            id = entity.id,
            name = entity.name,
            category = try {
                FoodCategory.valueOf(entity.category)
            } catch (e: IllegalArgumentException) {
                AppLog.w(TAG, "Unknown FoodCategory '${entity.category}' for item '${entity.name}', defaulting to OTHER")
                FoodCategory.OTHER
            },
            expiryDate = LocalDate.parse(entity.expiryDate),
            quantity = entity.quantity,
            location = try {
                StorageLocation.valueOf(entity.location)
            } catch (e: IllegalArgumentException) {
                AppLog.w(TAG, "Unknown StorageLocation '${entity.location}' for item '${entity.name}', defaulting to FRIDGE")
                StorageLocation.FRIDGE
            },
            notes = entity.notes,
            barcode = entity.barcode,
            dateAdded = LocalDate.parse(entity.dateAdded),
            notifyEnabled = entity.notifyEnabled,
            notifyDaysBefore = entity.notifyDaysBefore,
            purchaseDate = entity.purchaseDate?.let { LocalDate.parse(it) },
            scanSource = try {
                ScanSource.valueOf(entity.scanSource)
            } catch (e: IllegalArgumentException) {
                AppLog.w(TAG, "Unknown ScanSource '${entity.scanSource}' for item '${entity.name}', defaulting to MANUAL")
                ScanSource.MANUAL
            },
            confidence = entity.confidence,
            riskLevel = try {
                RiskLevel.valueOf(entity.riskLevel)
            } catch (e: IllegalArgumentException) {
                AppLog.w(TAG, "Unknown RiskLevel '${entity.riskLevel}' for item '${entity.name}', defaulting to LOW")
                RiskLevel.LOW
            },
            recipeRelevance = entity.recipeRelevance,
            imagePath = entity.imagePath
        )
    }

    fun domainToEntity(domain: FoodItem): FoodItemEntity {
        return FoodItemEntity(
            id = domain.id,
            name = domain.name,
            category = domain.category.name,
            expiryDate = domain.expiryDate.toString(),
            quantity = domain.quantity,
            location = domain.location.name,
            notes = domain.notes,
            barcode = domain.barcode,
            dateAdded = domain.dateAdded.toString(),
            notifyEnabled = domain.notifyEnabled,
            notifyDaysBefore = domain.notifyDaysBefore,
            purchaseDate = domain.purchaseDate?.toString(),
            scanSource = domain.scanSource.name,
            confidence = domain.confidence,
            riskLevel = domain.riskLevel.name,
            recipeRelevance = domain.recipeRelevance,
            imagePath = domain.imagePath
        )
    }
}
