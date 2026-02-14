package com.example.foodexpiryapp.util

import java.time.LocalDate

object ShelfLifeEstimator {
    
    data class CategoryShelfLife(
        val days: Int,
        val category: String
    )
    
    // Category-based shelf life in days (at room temperature or as noted)
    private val categoryShelfLifeMap = mapOf(
        // Dairy
        "dairy" to 7,
        "milk" to 7,
        "cheese" to 14,
        "yogurt" to 14,
        "butter" to 30,
        "cream" to 7,
        
        // Meat & Seafood
        "meat" to 3,
        "beef" to 3,
        "chicken" to 2,
        "pork" to 3,
        "fish" to 2,
        "seafood" to 2,
        "sausage" to 7,
        
        // Fruits
        "fruit" to 7,
        "apple" to 14,
        "banana" to 5,
        "orange" to 14,
        "grape" to 7,
        "berry" to 3,
        "strawberry" to 3,
        "blueberry" to 5,
        "watermelon" to 7,
        
        // Vegetables
        "vegetable" to 7,
        "tomato" to 7,
        "lettuce" to 5,
        "carrot" to 21,
        "potato" to 30,
        "onion" to 30,
        "cucumber" to 7,
        "pepper" to 14,
        "broccoli" to 5,
        "spinach" to 5,
        
        // Bakery
        "bread" to 5,
        "baguette" to 2,
        "croissant" to 3,
        "cake" to 5,
        "cookie" to 30,
        
        // Beverages
        "juice" to 7,
        "smoothie" to 3,
        
        // Canned & Packaged
        "canned" to 365,
        "dry" to 180,
        "pasta" to 730,
        "rice" to 730,
        "cereal" to 180,
        "snack" to 90,
        "chocolate" to 180,
        "candy" to 365,
        
        // Frozen
        "frozen" to 90,
        "ice-cream" to 180,
        
        // Condiments
        "condiment" to 180,
        "sauce" to 180,
        "oil" to 365,
        "vinegar" to 730,
        "honey" to 3650, // Honey lasts almost forever
        "jam" to 365,
        
        // Default
        "other" to 14
    )
    
    fun estimateShelfLife(categories: List<String>?): CategoryShelfLife {
        if (categories.isNullOrEmpty()) {
            return CategoryShelfLife(14, "Other")
        }
        
        // Check each category tag against our map
        for (category in categories) {
            val lowerCategory = category.lowercase()
            
            // Try exact match first
            categoryShelfLifeMap[lowerCategory]?.let {
                return CategoryShelfLife(it, capitalizeCategory(lowerCategory))
            }
            
            // Try partial match
            for ((key, days) in categoryShelfLifeMap) {
                if (lowerCategory.contains(key) || key.contains(lowerCategory)) {
                    return CategoryShelfLife(days, capitalizeCategory(key))
                }
            }
        }
        
        return CategoryShelfLife(14, "Other")
    }
    
    fun calculateExpiryDate(daysToAdd: Int): LocalDate {
        return LocalDate.now().plusDays(daysToAdd.toLong())
    }
    
    private fun capitalizeCategory(category: String): String {
        return category.replaceFirstChar { it.uppercase() }
    }
    
    fun parseCategories(categoriesString: String?): List<String> {
        if (categoriesString.isNullOrBlank()) return emptyList()
        return categoriesString.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }
}