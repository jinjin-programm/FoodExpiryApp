package com.example.foodexpiryapp.data.remote

import com.example.foodexpiryapp.data.remote.dto.OpenFoodFactsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,brands,categories,categories_tags,image_url,quantity,nutriments"
    ): Response<OpenFoodFactsResponse>
    
    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}