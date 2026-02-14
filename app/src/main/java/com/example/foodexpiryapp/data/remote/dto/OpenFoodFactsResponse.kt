package com.example.foodexpiryapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("code")
    val code: String?,
    @SerializedName("product")
    val product: OpenFoodFactsProduct?,
    @SerializedName("status")
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String?
)

data class OpenFoodFactsProduct(
    @SerializedName("product_name")
    val productName: String?,
    @SerializedName("brands")
    val brands: String?,
    @SerializedName("categories")
    val categories: String?,
    @SerializedName("categories_tags")
    val categoriesTags: List<String>?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("quantity")
    val quantity: String?,
    @SerializedName("serving_quantity")
    val servingQuantity: String?,
    @SerializedName("nutriments")
    val nutriments: Nutriments?
)

data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal: Double?,
    @SerializedName("proteins_100g")
    val proteins: Double?,
    @SerializedName("carbohydrates_100g")
    val carbohydrates: Double?,
    @SerializedName("fat_100g")
    val fat: Double?
)