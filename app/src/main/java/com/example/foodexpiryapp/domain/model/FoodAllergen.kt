package com.example.foodexpiryapp.domain.model

enum class FoodAllergen(val displayName: String) {
    PEANUTS("Peanuts"),
    TREE_NUTS("Tree Nuts"),
    MILK("Milk"),
    EGGS("Eggs"),
    WHEAT("Wheat"),
    SOY("Soy"),
    FISH("Fish"),
    SHELLFISH("Shellfish"),
    SESAME("Sesame"),
    GLUTEN("Gluten"),
    LACTOSE("Lactose"),
    CORN("Corn"),
    CITRUS("Citrus"),
    TOMATOES("Tomatoes"),
    PORK("Pork"),
    BEEF("Beef"),
    CHICKEN("Chicken"),
    GARLIC("Garlic"),
    ONIONS("Onions"),
    SPICES("Spices")
}

data class UserAllergens(
    val presetAllergens: Set<FoodAllergen> = emptySet(),
    val customAllergens: Set<String> = emptySet()
)

sealed class AllergenWarning {
    data class Preset(val allergens: List<FoodAllergen>) : AllergenWarning()
    data class Custom(val allergens: List<String>) : AllergenWarning()
}