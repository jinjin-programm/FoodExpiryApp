package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.remote.OpenFoodFactsApi
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.util.ShelfLifeEstimator
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class BarcodeScanResult(
    val name: String,
    val brand: String?,
    val category: FoodCategory,
    val estimatedExpiryDate: LocalDate,
    val shelfLifeDays: Int,
    val imageUrl: String?,
    val barcode: String
)

@Singleton
class BarcodeRepository @Inject constructor(
    private val openFoodFactsApi: OpenFoodFactsApi
) {
    
    suspend fun scanBarcode(barcode: String): Result<BarcodeScanResult> {
        return try {
            val response = openFoodFactsApi.getProductByBarcode(barcode)
            if (!response.isSuccessful) {
                return Result.failure(Exception("API error: ${response.code()}"))
            }
            val body = response.body()
            val product = body?.product
            
            val name = product?.productName ?: "Unknown Product"
            
            val categoriesList = product?.categories?.let {
                ShelfLifeEstimator.parseCategories(it)
            } ?: emptyList()
            
            val shelfLife = ShelfLifeEstimator.estimateShelfLife(categoriesList)
            val foodCategory = mapToFoodCategory(shelfLife.category)
            val expiryDate = ShelfLifeEstimator.calculateExpiryDate(shelfLife.days)
            
            Result.success(BarcodeScanResult(
                name = name,
                brand = product?.brands,
                category = foodCategory,
                estimatedExpiryDate = expiryDate,
                shelfLifeDays = shelfLife.days,
                imageUrl = product?.imageUrl,
                barcode = barcode
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun mapToFoodCategory(categoryName: String): FoodCategory {
        return when (categoryName.lowercase()) {
            "dairy", "milk", "cheese", "yogurt", "butter", "cream" -> FoodCategory.DAIRY
            "meat", "beef", "chicken", "pork", "sausage", "fish", "seafood" -> FoodCategory.MEAT
            "fruit", "apple", "banana", "orange", "grape", "berry" -> FoodCategory.FRUITS
            "vegetable", "tomato", "lettuce", "carrot", "potato", "onion", "cucumber", "pepper", "broccoli", "spinach" -> FoodCategory.VEGETABLES
            "bread", "baguette", "croissant", "cake", "cookie", "pasta", "rice", "cereal", "grains" -> FoodCategory.GRAINS
            "frozen", "ice-cream" -> FoodCategory.FROZEN
            "snack", "chocolate", "candy" -> FoodCategory.SNACKS
            "beverage", "juice", "smoothie" -> FoodCategory.BEVERAGES
            "condiment", "sauce", "oil", "vinegar", "honey", "jam" -> FoodCategory.CONDIMENTS
            else -> FoodCategory.OTHER
        }
    }
}
