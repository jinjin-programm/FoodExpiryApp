package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.ShoppingItem
import com.example.foodexpiryapp.domain.model.ShoppingTemplate
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>
    suspend fun addShoppingItem(name: String)
    suspend fun updateShoppingItem(item: ShoppingItem)
    suspend fun deleteShoppingItem(item: ShoppingItem)
    suspend fun clearCompletedItems()
    fun getAllTemplates(): Flow<List<ShoppingTemplate>>
    suspend fun applyTemplate(template: ShoppingTemplate)
    fun getInventoryItemNames(): Flow<List<String>>
}
