package com.example.foodexpiryapp.domain.model

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val isChecked: Boolean = false
)
