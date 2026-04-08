package com.example.foodexpiryapp.domain.engine

import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.StorageLocation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine for inferring default food attributes (category, shelf life, storage location)
 * from food names using a local keyword-based lookup table.
 *
 * Per D-13: Maps food names (EN + ZH keywords) to category and default shelf life.
 * Uses `contains` matching on the combined food name string, with longest-match-wins
 * strategy to prefer more specific keywords.
 *
 * Fallback for unknown food names: OTHER category, 7 days, FRIDGE.
 */
@Singleton
class DefaultAttributeEngine @Inject constructor() {

    data class AttributeDefaults(
        val category: FoodCategory,
        val shelfLifeDays: Int,
        val location: StorageLocation = StorageLocation.FRIDGE
    )

    private val lookupTable: Map<String, AttributeDefaults> = mapOf(
        // Dairy
        "milk" to AttributeDefaults(FoodCategory.DAIRY, 7),
        "\u5976" to AttributeDefaults(FoodCategory.DAIRY, 7),
        "\u9bae\u5976" to AttributeDefaults(FoodCategory.DAIRY, 7),
        "\u9bae\u4e73" to AttributeDefaults(FoodCategory.DAIRY, 7),
        "cheese" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "\u829d\u58eb" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "\u8d77\u53f8" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "yogurt" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "\u9178\u5976" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "\u512a\u683c" to AttributeDefaults(FoodCategory.DAIRY, 14),
        "butter" to AttributeDefaults(FoodCategory.DAIRY, 30),
        "\u5976\u6cb9" to AttributeDefaults(FoodCategory.DAIRY, 30),
        "\u725b\u6cb9" to AttributeDefaults(FoodCategory.DAIRY, 30),
        "egg" to AttributeDefaults(FoodCategory.DAIRY, 21),
        "\u86cb" to AttributeDefaults(FoodCategory.DAIRY, 21),
        "\u96de\u86cb" to AttributeDefaults(FoodCategory.DAIRY, 21),

        // Meat
        "chicken" to AttributeDefaults(FoodCategory.MEAT, 3),
        "\u96de" to AttributeDefaults(FoodCategory.MEAT, 3),
        "\u96de\u8089" to AttributeDefaults(FoodCategory.MEAT, 3),
        "beef" to AttributeDefaults(FoodCategory.MEAT, 3),
        "\u725b\u8089" to AttributeDefaults(FoodCategory.MEAT, 3),
        "pork" to AttributeDefaults(FoodCategory.MEAT, 3),
        "\u8c6c" to AttributeDefaults(FoodCategory.MEAT, 3),
        "\u8c6c\u8089" to AttributeDefaults(FoodCategory.MEAT, 3),
        "fish" to AttributeDefaults(FoodCategory.MEAT, 2),
        "\u9b5a" to AttributeDefaults(FoodCategory.MEAT, 2),
        "\u8766" to AttributeDefaults(FoodCategory.MEAT, 2),
        "shrimp" to AttributeDefaults(FoodCategory.MEAT, 2),

        // Vegetables
        "tomato" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "\u756a\u8304" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "\u8393\u8304" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "\u897f\u7ea2\u67ff" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "lettuce" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "\u751f\u83dc" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "broccoli" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "\u897f\u5170\u82b1" to AttributeDefaults(FoodCategory.VEGETABLES, 5),
        "carrot" to AttributeDefaults(FoodCategory.VEGETABLES, 14),
        "\u80e1\u841d\u8353" to AttributeDefaults(FoodCategory.VEGETABLES, 14),
        "\u7ea2\u841d\u8353" to AttributeDefaults(FoodCategory.VEGETABLES, 14),

        // Fruits
        "apple" to AttributeDefaults(FoodCategory.FRUITS, 7),
        "\u82f9\u679c" to AttributeDefaults(FoodCategory.FRUITS, 7),
        "banana" to AttributeDefaults(FoodCategory.FRUITS, 5),
        "\u9999\u8549" to AttributeDefaults(FoodCategory.FRUITS, 5),
        "orange" to AttributeDefaults(FoodCategory.FRUITS, 10),
        "\u6a59" to AttributeDefaults(FoodCategory.FRUITS, 10),
        "\u6a58\u5b50" to AttributeDefaults(FoodCategory.FRUITS, 10),

        // Grains
        "rice" to AttributeDefaults(FoodCategory.GRAINS, 180),
        "\u7c73" to AttributeDefaults(FoodCategory.GRAINS, 180),
        "\u767d\u98ef" to AttributeDefaults(FoodCategory.GRAINS, 2),
        "bread" to AttributeDefaults(FoodCategory.GRAINS, 5),
        "\u9eb5\u5305" to AttributeDefaults(FoodCategory.GRAINS, 5),
        "noodles" to AttributeDefaults(FoodCategory.GRAINS, 5),
        "\u9eb5" to AttributeDefaults(FoodCategory.GRAINS, 5),
        "pasta" to AttributeDefaults(FoodCategory.GRAINS, 180),
        "\u610f\u5927\u5229\u9eb5" to AttributeDefaults(FoodCategory.GRAINS, 180),

        // Beverages
        "juice" to AttributeDefaults(FoodCategory.BEVERAGES, 7),
        "\u679c\u6c41" to AttributeDefaults(FoodCategory.BEVERAGES, 7),
        "tea" to AttributeDefaults(FoodCategory.BEVERAGES, 7),
        "\u8336" to AttributeDefaults(FoodCategory.BEVERAGES, 7),
        "coffee" to AttributeDefaults(FoodCategory.BEVERAGES, 14),
        "\u5496\u5561" to AttributeDefaults(FoodCategory.BEVERAGES, 14),
        "soda" to AttributeDefaults(FoodCategory.BEVERAGES, 90),
        "water" to AttributeDefaults(FoodCategory.BEVERAGES, 180),

        // Snacks
        "chips" to AttributeDefaults(FoodCategory.SNACKS, 30),
        "chocolate" to AttributeDefaults(FoodCategory.SNACKS, 90),
        "\u5de7\u514b\u529b" to AttributeDefaults(FoodCategory.SNACKS, 90),
        "cookie" to AttributeDefaults(FoodCategory.SNACKS, 14),
        "\u9905\u4e7e" to AttributeDefaults(FoodCategory.SNACKS, 14),

        // Condiments
        "soy sauce" to AttributeDefaults(FoodCategory.CONDIMENTS, 180),
        "\u91ac\u6cb9" to AttributeDefaults(FoodCategory.CONDIMENTS, 180),
        "ketchup" to AttributeDefaults(FoodCategory.CONDIMENTS, 90),
        "oil" to AttributeDefaults(FoodCategory.CONDIMENTS, 180),
        "\u6cb9" to AttributeDefaults(FoodCategory.CONDIMENTS, 180)
    )

    /**
     * Infers default attributes for a food item based on its name.
     *
     * Searches both the English and Chinese names for matching keywords,
     * returning the longest (most specific) match found.
     *
     * @param foodName The English food name from LLM classification.
     * @param foodNameZh The Chinese food name from LLM classification (optional).
     * @return AttributeDefaults with category, shelf life days, and storage location.
     */
    fun inferDefaults(foodName: String, foodNameZh: String = ""): AttributeDefaults {
        val combined = "$foodName $foodNameZh".lowercase()

        // Find the longest matching keyword (more specific matches win)
        val match = lookupTable.entries
            .filter { (keyword, _) -> combined.contains(keyword.lowercase()) }
            .maxByOrNull { (keyword, _) -> keyword.length }

        return match?.value ?: AttributeDefaults(
            category = FoodCategory.OTHER,
            shelfLifeDays = 7,
            location = StorageLocation.FRIDGE
        )
    }
}
