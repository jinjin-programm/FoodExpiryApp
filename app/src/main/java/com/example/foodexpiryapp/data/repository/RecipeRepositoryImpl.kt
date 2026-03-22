package com.example.foodexpiryapp.data.repository

import android.content.Context
import com.example.foodexpiryapp.data.local.RecipeIngredientJsonDto
import com.example.foodexpiryapp.data.local.RecipeJsonDto
import com.example.foodexpiryapp.data.local.RecipesWrapperDto
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeTag
import com.example.foodexpiryapp.domain.repository.RecipeRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : RecipeRepository {

    private var cachedRecipes: List<Recipe>? = null

    private fun loadRecipesFromAssets(): List<Recipe> {
        cachedRecipes?.let { return it }
        return try {
            context.assets.open("recipes.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val wrapper = gson.fromJson(reader, RecipesWrapperDto::class.java)
                    wrapper.recipes.map { it.toDomain() }.also { cachedRecipes = it }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getAllRecipes(): Flow<List<Recipe>> = flow {
        emit(loadRecipesFromAssets())
    }.flowOn(Dispatchers.IO)

    override suspend fun getRecipeById(id: Long): Recipe? {
        return loadRecipesFromAssets().find { it.id == id }
    }

    override fun searchRecipes(query: String): Flow<List<Recipe>> = flow {
        val lower = query.lowercase()
        emit(loadRecipesFromAssets().filter { recipe ->
            recipe.name.lowercase().contains(lower) ||
            recipe.description.lowercase().contains(lower) ||
            recipe.ingredients.any { it.name.lowercase().contains(lower) } ||
            recipe.tags.any { it.name.lowercase().contains(lower) }
        })
    }.flowOn(Dispatchers.IO)

    override fun getRecipesByTag(tag: String): Flow<List<Recipe>> = flow {
        emit(loadRecipesFromAssets().filter { recipe ->
            recipe.tags.any { it.name.equals(tag, ignoreCase = true) }
        })
    }.flowOn(Dispatchers.IO)

    override fun getRecipesMatchingInventory(inventoryItemNames: List<String>): Flow<List<Recipe>> = flow {
        val names = inventoryItemNames.map { it.lowercase() }
        emit(loadRecipesFromAssets().filter { recipe ->
            recipe.ingredients.any { ing ->
                names.any { name -> ing.name.lowercase().contains(name) || name.contains(ing.name.lowercase()) }
            }
        }.sortedByDescending { recipe ->
            recipe.ingredients.count { ing ->
                names.any { name -> ing.name.lowercase().contains(name) || name.contains(ing.name.lowercase()) }
            }
        })
    }.flowOn(Dispatchers.IO)

    private fun RecipeJsonDto.toDomain(): Recipe {
        return Recipe(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            ingredients = ingredients.map { it.toDomain() },
            steps = steps,
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            servings = servings,
            cuisine = cuisine,
            tags = tags.mapNotNull { tagName ->
                try {
                    RecipeTag.valueOf(tagName.uppercase().replace(" ", "_"))
                } catch (e: Exception) {
                    null
                }
            }.toSet(),
            estimatedCost = estimatedCost,
            estimatedSaving = estimatedSaving,
            wasteRescueScore = wasteRescueScore
        )
    }

    private fun RecipeIngredientJsonDto.toDomain(): RecipeIngredient {
        return RecipeIngredient(
            name = name,
            quantity = quantity,
            unit = unit,
            isOptional = isOptional
        )
    }
}