package com.example.foodexpiryapp.data.remote

import com.example.foodexpiryapp.data.remote.dto.OpenFoodFactsResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenFoodFactsService @Inject constructor(
    private val api: OpenFoodFactsApi
) {
    
    suspend fun getProductInfo(barcode: String): Result<OpenFoodFactsResponse> {
        return try {
            val response = api.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body()?.status == 1) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}