package com.example.foodexpiryapp.data.remote.dto

import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.google.gson.annotations.SerializedName

data class MealResponseDto(
    @SerializedName("meals") val meals: List<MealDto>?
)

data class MealDto(
    @SerializedName("idMeal") val idMeal: String,
    @SerializedName("strMeal") val strMeal: String,
    @SerializedName("strDrinkAlternative") val strDrinkAlternative: String?,
    @SerializedName("strCategory") val strCategory: String?,
    @SerializedName("strArea") val strArea: String?,
    @SerializedName("strInstructions") val strInstructions: String?,
    @SerializedName("strMealThumb") val strMealThumb: String?,
    @SerializedName("strTags") val strTags: String?,
    @SerializedName("strYoutube") val strYoutube: String?,
    @SerializedName("strIngredient1") val strIngredient1: String?,
    @SerializedName("strIngredient2") val strIngredient2: String?,
    @SerializedName("strIngredient3") val strIngredient3: String?,
    @SerializedName("strIngredient4") val strIngredient4: String?,
    @SerializedName("strIngredient5") val strIngredient5: String?,
    @SerializedName("strIngredient6") val strIngredient6: String?,
    @SerializedName("strIngredient7") val strIngredient7: String?,
    @SerializedName("strIngredient8") val strIngredient8: String?,
    @SerializedName("strIngredient9") val strIngredient9: String?,
    @SerializedName("strIngredient10") val strIngredient10: String?,
    @SerializedName("strIngredient11") val strIngredient11: String?,
    @SerializedName("strIngredient12") val strIngredient12: String?,
    @SerializedName("strIngredient13") val strIngredient13: String?,
    @SerializedName("strIngredient14") val strIngredient14: String?,
    @SerializedName("strIngredient15") val strIngredient15: String?,
    @SerializedName("strIngredient16") val strIngredient16: String?,
    @SerializedName("strIngredient17") val strIngredient17: String?,
    @SerializedName("strIngredient18") val strIngredient18: String?,
    @SerializedName("strIngredient19") val strIngredient19: String?,
    @SerializedName("strIngredient20") val strIngredient20: String?,
    @SerializedName("strMeasure1") val strMeasure1: String?,
    @SerializedName("strMeasure2") val strMeasure2: String?,
    @SerializedName("strMeasure3") val strMeasure3: String?,
    @SerializedName("strMeasure4") val strMeasure4: String?,
    @SerializedName("strMeasure5") val strMeasure5: String?,
    @SerializedName("strMeasure6") val strMeasure6: String?,
    @SerializedName("strMeasure7") val strMeasure7: String?,
    @SerializedName("strMeasure8") val strMeasure8: String?,
    @SerializedName("strMeasure9") val strMeasure9: String?,
    @SerializedName("strMeasure10") val strMeasure10: String?,
    @SerializedName("strMeasure11") val strMeasure11: String?,
    @SerializedName("strMeasure12") val strMeasure12: String?,
    @SerializedName("strMeasure13") val strMeasure13: String?,
    @SerializedName("strMeasure14") val strMeasure14: String?,
    @SerializedName("strMeasure15") val strMeasure15: String?,
    @SerializedName("strMeasure16") val strMeasure16: String?,
    @SerializedName("strMeasure17") val strMeasure17: String?,
    @SerializedName("strMeasure18") val strMeasure18: String?,
    @SerializedName("strMeasure19") val strMeasure19: String?,
    @SerializedName("strMeasure20") val strMeasure20: String?,
    @SerializedName("strSource") val strSource: String?,
    @SerializedName("strImageSource") val strImageSource: String?,
    @SerializedName("strCreativeCommonsConfirmed") val strCreativeCommonsConfirmed: String?,
    @SerializedName("dateModified") val dateModified: String?
) {
    fun toDomain(): Recipe {
        val ingredientsList = mutableListOf<RecipeIngredient>()
        
        // Helper to collect non-empty ingredients and measures
        val ingredients = listOf(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        )
        val measures = listOf(
            strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
            strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
            strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
            strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
        )

        for (i in ingredients.indices) {
            val name = ingredients[i]
            val measure = measures[i]
            if (!name.isNullOrBlank()) {
                ingredientsList.add(
                    RecipeIngredient(
                        name = name,
                        quantity = measure ?: "",
                        unit = ""
                    )
                )
            }
        }

        return Recipe(
            id = idMeal.toLongOrNull() ?: 0L,
            name = strMeal,
            description = strInstructions ?: "Delicious meal from TheMealDB",
            imageUrl = strMealThumb,
            ingredients = ingredientsList,
            steps = strInstructions?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
            cuisine = strArea ?: "",
            tags = emptySet(),
            prepTimeMinutes = 15,
            cookTimeMinutes = 30,
            estimatedCost = ingredientsList.size * 1.5,
            estimatedSaving = if (ingredientsList.isNotEmpty()) ingredientsList.size * 2.0 else 7.0,
            youtubeUrl = strYoutube
        )
    }
}
