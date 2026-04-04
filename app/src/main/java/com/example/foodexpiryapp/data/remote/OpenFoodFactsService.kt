package com.example.foodexpiryapp.data.remote

import com.example.foodexpiryapp.data.remote.dto.OpenFoodFactsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodFactsService {

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/api/v2/"
    }

    @GET("product/")
    suspend fun getProduct(
        @Query("code") barcode: String,
        @Query("fields") fields: String = "product_name,brands,categories,price,price_debug,serving_size,nutriscore_grade,image_url,image_front_url,quantity,unit,ingredients_text"
    ): OpenFoodFactsResponse
}
