package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.data.local.database.MealPlanEntity
import com.example.foodexpiryapp.domain.model.MealItemType
import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import java.time.LocalDate

object MealPlanMapper {

    fun entityToDomain(entity: MealPlanEntity): MealPlan {
        return MealPlan(
            id = entity.id,
            date = LocalDate.parse(entity.date),
            slot = try {
                MealSlot.valueOf(entity.slot)
            } catch (e: IllegalArgumentException) {
                MealSlot.BREAKFAST
            },
            itemType = try {
                MealItemType.valueOf(entity.itemType)
            } catch (e: IllegalArgumentException) {
                MealItemType.PRODUCT
            },
            recipeId = entity.recipeId,
            recipeName = entity.recipeName,
            productName = entity.productName,
            inventoryItemId = entity.inventoryItemId
        )
    }

    fun domainToEntity(domain: MealPlan): MealPlanEntity {
        return MealPlanEntity(
            id = domain.id,
            date = domain.date.toString(),
            slot = domain.slot.name,
            itemType = domain.itemType.name,
            recipeId = domain.recipeId,
            recipeName = domain.recipeName,
            productName = domain.productName,
            inventoryItemId = domain.inventoryItemId
        )
    }
}
