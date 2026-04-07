package com.example.foodexpiryapp.domain.model

data class ShoppingTemplate(
    val id: Long = 0,
    val name: String,
    val description: String,
    val itemNames: List<String>
)
