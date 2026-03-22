package com.example.foodexpiryapp.domain.model

data class Recipe(
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val servings: Int = 2,
    val cuisine: String = "",
    val tags: Set<RecipeTag> = emptySet(),
    val estimatedCost: Double = 0.0,
    val estimatedSaving: Double = 0.0,
    val wasteRescueScore: Int = 0
) {
    val totalTimeMinutes: Int get() = prepTimeMinutes + cookTimeMinutes

    fun matchScoreWithInventory(inventoryItems: List<FoodItem>): Int {
        val inventoryNames = inventoryItems.map { it.name.lowercase() }.toSet()
        return ingredients.count { ing ->
            inventoryNames.any { invName -> ing.name.lowercase().contains(invName) || invName.contains(ing.name.lowercase()) }
        }
    }

    fun matchedInventoryItems(inventoryItems: List<FoodItem>): List<FoodItem> {
        val inventoryNames = inventoryItems.map { it.name.lowercase() }.toSet()
        return inventoryItems.filter { item ->
            ingredients.any { ing -> ing.name.lowercase().contains(item.name.lowercase()) || item.name.lowercase().contains(ing.name.lowercase()) }
        }
    }

    fun urgencyScore(): Int {
        return when {
            tags.contains(RecipeTag.URGENT) -> 3
            tags.contains(RecipeTag.USE_SOON) -> 2
            else -> 1
        }
    }
}

data class RecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String = "",
    val isOptional: Boolean = false
)

enum class RecipeTag(val displayName: String) {
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    GLUTEN_FREE("Gluten-Free"),
    DAIRY_FREE("Dairy-Free"),
    NUT_FREE("Nut-Free"),
    LOW_CARB("Low-Carb"),
    KETO("Keto"),
    QUICK("Quick (Under 30 min)"),
    ONE_POT("One Pot"),
    NO_COOK("No Cook"),
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    DESSERT("Dessert"),
    SOUP("Soup"),
    SALAD("Salad"),
    STIRFRY("Stir Fry"),
    CURRY("Curry"),
    PASTA("Pasta"),
    RICE("Rice"),
    BREAD("Bread"),
    SMOOTHIE("Smoothie"),
    JUICE("Juice"),
    USE_SOON("Use Soon"),
    URGENT("Urgent - Expiring Today!"),
    WASTE_BUSTER("Waste Buster"),
    MONEY_SAVER("Money Saver"),
    FAMILY_FAV("Family Favorite")
}

data class RecipeMatch(
    val recipe: Recipe,
    val matchCount: Int,
    val matchedIngredients: List<RecipeIngredient>,
    val matchedInventoryItems: List<FoodItem>,
    val estimatedMoneySaved: Double,
    val wasteRescuePercent: Int,
    val urgencyLevel: Int,
    val dietaryFlags: List<RecipeTag>
)