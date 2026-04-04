package com.example.foodexpiryapp.data.remote

import com.example.foodexpiryapp.data.remote.dto.MealResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    companion object {
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
    }

    @GET("search.php")
    suspend fun searchMeals(
        @Query("s") query: String
    ): MealResponseDto

    @GET("filter.php")
    suspend fun filterByIngredient(
        @Query("i") ingredient: String
    ): MealResponseDto

    @GET("filter.php")
    suspend fun filterByCategory(
        @Query("c") category: String
    ): MealResponseDto

    @GET("filter.php")
    suspend fun filterByArea(
        @Query("a") area: String
    ): MealResponseDto

    @GET("lookup.php")
    suspend fun getMealDetails(
        @Query("i") id: String
    ): MealResponseDto

    @GET("random.php")
    suspend fun getRandomMeal(): MealResponseDto
}
