package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.LocalRecipeDao
import com.example.foodexpiryapp.data.local.database.LocalRecipeEntity
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeTag
import com.example.foodexpiryapp.domain.repository.LocalRecipeRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRecipeRepositoryImpl @Inject constructor(
    private val localRecipeDao: LocalRecipeDao,
    private val gson: Gson
) : LocalRecipeRepository {

    override fun getAllLocalRecipes(): Flow<List<Recipe>> {
        return localRecipeDao.getAllLocalRecipes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLocalRecipeById(id: Long): Recipe? {
        return localRecipeDao.getLocalRecipeById(id)?.toDomain()
    }

    override suspend fun saveLocalRecipe(recipe: Recipe): Long {
        return localRecipeDao.insertLocalRecipe(recipe.toEntity())
    }

    override suspend fun updateLocalRecipe(recipe: Recipe) {
        localRecipeDao.updateLocalRecipe(recipe.toEntity())
    }

    override suspend fun deleteLocalRecipe(id: Long) {
        localRecipeDao.deleteLocalRecipeById(id)
    }

    private fun LocalRecipeEntity.toDomain(): Recipe {
        val ingredientsType = object : TypeToken<List<RecipeIngredient>>() {}.type
        val stepsType = object : TypeToken<List<String>>() {}.type
        val tagsType = object : TypeToken<Set<RecipeTag>>() {}.type

        return Recipe(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            ingredients = gson.fromJson(ingredients, ingredientsType) ?: emptyList(),
            steps = gson.fromJson(steps, stepsType) ?: emptyList(),
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            servings = servings,
            cuisine = cuisine,
            tags = gson.fromJson(tags, tagsType) ?: emptySet(),
            estimatedCost = estimatedCost
        )
    }

    private fun Recipe.toEntity(): LocalRecipeEntity {
        return LocalRecipeEntity(
            id = id,
            name = name,
            description = description,
            imageUrl = imageUrl,
            ingredients = gson.toJson(ingredients),
            steps = gson.toJson(steps),
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            servings = servings,
            cuisine = cuisine,
            tags = gson.toJson(tags),
            estimatedCost = estimatedCost
        )
    }
}
