package com.example.foodexpiryapp.data.local

import com.google.gson.annotations.SerializedName

data class RecipeJsonDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("ingredients") val ingredients: List<RecipeIngredientJsonDto>,
    @SerializedName("steps") val steps: List<String>,
    @SerializedName("prepTimeMinutes") val prepTimeMinutes: Int,
    @SerializedName("cookTimeMinutes") val cookTimeMinutes: Int,
    @SerializedName("servings") val servings: Int,
    @SerializedName("cuisine") val cuisine: String,
    @SerializedName("tags") val tags: List<String>,
    @SerializedName("estimatedCost") val estimatedCost: Double,
    @SerializedName("estimatedSaving") val estimatedSaving: Double,
    @SerializedName("wasteRescueScore") val wasteRescueScore: Int
)

data class RecipeIngredientJsonDto(
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: String,
    @SerializedName("unit") val unit: String,
    @SerializedName("isOptional") val isOptional: Boolean = false
)

data class RecipesWrapperDto(
    @SerializedName("recipes") val recipes: List<RecipeJsonDto>
)