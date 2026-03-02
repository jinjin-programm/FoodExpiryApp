package com.example.foodexpiryapp.domain.model

/**
 * Domain model representing user settings and preferences.
 */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val householdSize: Int = 1,
    val dietaryPreferences: Set<DietaryPreference> = emptySet()
)

/**
 * Supported dietary preferences.
 */
enum class DietaryPreference(val displayName: String) {
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    GLUTEN_FREE("Gluten-Free"),
    DAIRY_FREE("Dairy-Free"),
    NUT_FREE("Nut-Free"),
    LOW_CARB("Low-Carb"),
    KETO("Keto"),
    PALEO("Paleo")
}
