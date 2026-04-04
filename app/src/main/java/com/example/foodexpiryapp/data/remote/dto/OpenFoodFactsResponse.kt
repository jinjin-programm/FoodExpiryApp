package com.example.foodexpiryapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("status_verbose") val statusVerbose: String?,
    @SerializedName("product") val product: ProductDto?
)

data class ProductDto(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("price") val price: String?,
    @SerializedName("price_debug") val priceDebug: String?,
    @SerializedName("serving_size") val servingSize: String?,
    @SerializedName("nutriscore_grade") val nutriscoreGrade: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_front_url") val imageFrontUrl: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("unit") val unit: String?,
    @SerializedName("ingredients_text") val ingredientsText: String?
)
