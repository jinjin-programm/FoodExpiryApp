package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.ShoppingItemDao
import com.example.foodexpiryapp.data.local.database.ShoppingItemEntity
import com.example.foodexpiryapp.domain.model.ShoppingItem
import com.example.foodexpiryapp.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val shoppingItemDao: ShoppingItemDao
) : ShoppingRepository {

    override fun getAllShoppingItems(): Flow<List<ShoppingItem>> {
        return shoppingItemDao.getAllShoppingItems().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addShoppingItem(name: String) {
        shoppingItemDao.insertShoppingItem(ShoppingItemEntity(name = name))
    }

    override suspend fun updateShoppingItem(item: ShoppingItem) {
        shoppingItemDao.updateShoppingItem(ShoppingItemEntity.fromDomain(item))
    }

    override suspend fun deleteShoppingItem(item: ShoppingItem) {
        shoppingItemDao.deleteShoppingItem(ShoppingItemEntity.fromDomain(item))
    }

    override suspend fun clearCompletedItems() {
        shoppingItemDao.clearCompletedItems()
    }
}
