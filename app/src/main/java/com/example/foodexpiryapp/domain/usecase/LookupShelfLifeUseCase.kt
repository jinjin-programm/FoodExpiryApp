package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.repository.ShelfLifeRepository
import javax.inject.Inject

class LookupShelfLifeUseCase @Inject constructor(
    private val shelfLifeRepository: ShelfLifeRepository
) {
    data class ShelfLifeResult(
        val shelfLifeDays: Int,
        val category: FoodCategory,
        val location: StorageLocation,
        val source: String,
        val isNewlyLearned: Boolean = false
    )

    suspend operator fun invoke(foodName: String, llmShelfLifeDays: Int? = null): ShelfLifeResult {
        shelfLifeRepository.ensureSeeded()

        val existing = shelfLifeRepository.lookup(foodName)
        if (existing != null) {
            return ShelfLifeResult(
                shelfLifeDays = existing.shelfLifeDays,
                category = parseCategory(existing.category),
                location = parseLocation(existing.location),
                source = existing.source
            )
        }

        if (llmShelfLifeDays != null && llmShelfLifeDays > 0) {
            val fallback = inferFromName(foodName)
            shelfLifeRepository.saveLearnedEntry(
                foodName = foodName,
                shelfLifeDays = llmShelfLifeDays,
                category = fallback.category.name,
                location = fallback.location.name
            )
            return ShelfLifeResult(
                shelfLifeDays = llmShelfLifeDays,
                category = fallback.category,
                location = fallback.location,
                source = "auto",
                isNewlyLearned = true
            )
        }

        return ShelfLifeResult(
            shelfLifeDays = 7,
            category = FoodCategory.OTHER,
            location = StorageLocation.FRIDGE,
            source = "fallback"
        )
    }

    private fun inferFromName(foodName: String): ShelfLifeResult {
        val lower = foodName.lowercase()
        val category = when {
            lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") ||
            lower.contains("butter") || lower.contains("egg") || lower.contains("\u5976") ||
            lower.contains("\u829d\u58eb") || lower.contains("\u9178\u5976") || lower.contains("\u86cb") -> FoodCategory.DAIRY
            lower.contains("chicken") || lower.contains("beef") || lower.contains("pork") ||
            lower.contains("fish") || lower.contains("shrimp") || lower.contains("meat") ||
            lower.contains("\u96de") || lower.contains("\u725b\u8089") || lower.contains("\u8c6c") ||
            lower.contains("\u9b5a") || lower.contains("\u8766") -> FoodCategory.MEAT
            lower.contains("apple") || lower.contains("banana") || lower.contains("orange") ||
            lower.contains("grape") || lower.contains("berry") || lower.contains("fruit") ||
            lower.contains("\u82f9\u679c") || lower.contains("\u9999\u8549") || lower.contains("\u6a59") -> FoodCategory.FRUITS
            lower.contains("tomato") || lower.contains("lettuce") || lower.contains("carrot") ||
            lower.contains("broccoli") || lower.contains("vegetable") || lower.contains("onion") ||
            lower.contains("\u756a\u8304") || lower.contains("\u751f\u83dc") || lower.contains("\u80e1\u841d\u8353") -> FoodCategory.VEGETABLES
            lower.contains("rice") || lower.contains("bread") || lower.contains("pasta") ||
            lower.contains("noodle") || lower.contains("grain") || lower.contains("cereal") ||
            lower.contains("\u7c73") || lower.contains("\u9eb5") -> FoodCategory.GRAINS
            lower.contains("juice") || lower.contains("soda") || lower.contains("water") ||
            lower.contains("coffee") || lower.contains("tea") || lower.contains("beer") ||
            lower.contains("\u679c\u6c41") || lower.contains("\u8336") || lower.contains("\u5496\u5561") -> FoodCategory.BEVERAGES
            lower.contains("chip") || lower.contains("chocolate") || lower.contains("cookie") ||
            lower.contains("candy") || lower.contains("snack") || lower.contains("\u5de7\u514b\u529b") -> FoodCategory.SNACKS
            lower.contains("sauce") || lower.contains("oil") || lower.contains("vinegar") ||
            lower.contains("ketchup") || lower.contains("condiment") || lower.contains("\u91ac\u6cb9") ||
            lower.contains("\u6cb9") -> FoodCategory.CONDIMENTS
            lower.contains("frozen") || lower.contains("ice cream") -> FoodCategory.FROZEN
            lower.contains("leftover") || lower.contains("takeout") -> FoodCategory.LEFTOVERS
            else -> FoodCategory.OTHER
        }
        val location = when (category) {
            FoodCategory.FROZEN -> StorageLocation.FREEZER
            FoodCategory.GRAINS, FoodCategory.SNACKS, FoodCategory.BEVERAGES -> StorageLocation.PANTRY
            FoodCategory.FRUITS -> StorageLocation.COUNTER
            else -> StorageLocation.FRIDGE
        }
        return ShelfLifeResult(
            shelfLifeDays = 7,
            category = category,
            location = location,
            source = "fallback"
        )
    }

    private fun parseCategory(name: String): FoodCategory {
        return try { FoodCategory.valueOf(name) } catch (_: Exception) { FoodCategory.OTHER }
    }

    private fun parseLocation(name: String): StorageLocation {
        return try { StorageLocation.valueOf(name) } catch (_: Exception) { StorageLocation.FRIDGE }
    }
}
