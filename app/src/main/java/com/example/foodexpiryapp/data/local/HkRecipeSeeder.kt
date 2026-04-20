package com.example.foodexpiryapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.foodexpiryapp.data.local.dao.LocalRecipeDao
import com.example.foodexpiryapp.data.local.database.LocalRecipeEntity
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeTag
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HkRecipeSeeder @Inject constructor(
    private val localRecipeDao: LocalRecipeDao,
    private val gson: Gson
) {
    private val prefsName = "hk_recipe_seeder"
    private val keySeeded = "has_seeded_v1"

    suspend fun seedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (prefs.getBoolean(keySeeded, false)) return

        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open("hk_recipes.json")
                    .bufferedReader()
                    .use { it.readText() }

                val type = object : TypeToken<List<HkRecipeJson>>() {}.type
                val recipes: List<HkRecipeJson> = gson.fromJson(json, type)

                for (recipe in recipes) {
                    val entity = LocalRecipeEntity(
                        id = recipe.id + HK_RECIPE_ID_OFFSET,
                        name = recipe.name,
                        description = recipe.description,
                        imageUrl = null,
                        ingredients = gson.toJson(recipe.ingredients.map {
                            RecipeIngredient(
                                name = it.name,
                                quantity = it.quantity,
                                unit = it.unit,
                                isOptional = it.isOptional
                            )
                        }),
                        steps = gson.toJson(recipe.steps),
                        prepTimeMinutes = recipe.prepTimeMinutes,
                        cookTimeMinutes = recipe.cookTimeMinutes,
                        servings = recipe.servings,
                        cuisine = recipe.cuisine,
                        tags = gson.toJson(recipe.tags.mapNotNull { tagStr ->
                            try { RecipeTag.valueOf(tagStr) } catch (_: Exception) { null }
                        }.toSet()),
                        estimatedCost = recipe.estimatedCost,
                        category = recipe.category
                    )
                    localRecipeDao.insertLocalRecipe(entity)
                }

                prefs.edit().putBoolean(keySeeded, true).apply()
            } catch (e: Exception) {
                android.util.Log.e("HkRecipeSeeder", "Failed to seed recipes", e)
            }
        }
    }

    companion object {
        const val HK_RECIPE_ID_OFFSET = 100_000L
    }
}

private data class HkRecipeJson(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val cuisine: String,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val estimatedCost: Double,
    val ingredients: List<HkIngredientJson>,
    val steps: List<String>,
    val tags: List<String>
)

private data class HkIngredientJson(
    val name: String,
    val quantity: String,
    val unit: String,
    val isOptional: Boolean
)
