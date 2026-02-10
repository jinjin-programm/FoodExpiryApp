package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.data.local.database.FoodItemEntity
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import java.time.LocalDate

/**
 * Maps between FoodItemEntity (data layer) and FoodItem (domain layer).
 */
object FoodItemMapper {

    fun entityToDomain(entity: FoodItemEntity): FoodItem {
        return FoodItem(
            id = entity.id,
            name = entity.name,
            category = try {
                FoodCategory.valueOf(entity.category)
            } catch (e: IllegalArgumentException) {
                FoodCategory.OTHER
            },
            expiryDate = LocalDate.parse(entity.expiryDate),
            quantity = entity.quantity,
            location = try {
                StorageLocation.valueOf(entity.location)
            } catch (e: IllegalArgumentException) {
                StorageLocation.FRIDGE
            },
            notes = entity.notes,
            barcode = entity.barcode,
            dateAdded = LocalDate.parse(entity.dateAdded)
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
            dateAdded = domain.dateAdded.toString()
        )
    }
}
