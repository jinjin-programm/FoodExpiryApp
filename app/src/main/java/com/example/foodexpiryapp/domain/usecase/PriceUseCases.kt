package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.data.remote.OpenFoodFactsApi
import javax.inject.Inject

class GetIngredientPriceUseCase @Inject constructor(
    private val openFoodFactsApi: OpenFoodFactsApi
) {
    suspend operator fun invoke(barcode: String): Double? {
        return try {
            val response = openFoodFactsApi.getProductByBarcode(barcode)
            if (response.isSuccessful) {
                response.body()?.product?.price?.toDoubleOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
