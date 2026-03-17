package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.mapper.FoodItemMapper
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val dao: FoodItemDao
) : FoodRepository {

    override fun getAllFoodItems(): Flow<List<FoodItem>> {
        return dao.getAllFoodItems().map { entities ->
            entities.map { FoodItemMapper.entityToDomain(it) }
        }
    }

    override suspend fun getFoodItemById(id: Long): FoodItem? {
        return dao.getFoodItemById(id)?.let { FoodItemMapper.entityToDomain(it) }
    }

    override fun getExpiringBefore(date: LocalDate): Flow<List<FoodItem>> {
        return dao.getExpiringBefore(date.toString()).map { entities ->
            entities.map { FoodItemMapper.entityToDomain(it) }
        }
    }

    override fun getFoodItemsByCategory(category: FoodCategory): Flow<List<FoodItem>> {
        return dao.getFoodItemsByCategory(category.name).map { entities ->
            entities.map { FoodItemMapper.entityToDomain(it) }
        }
    }

    override fun getFoodItemsByLocation(location: StorageLocation): Flow<List<FoodItem>> {
        return dao.getFoodItemsByLocation(location.name).map { entities ->
            entities.map { FoodItemMapper.entityToDomain(it) }
        }
    }

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> {
        return dao.searchFoodItems(query).map { entities ->
            entities.map { FoodItemMapper.entityToDomain(it) }
        }
    }

    override suspend fun insertFoodItem(foodItem: FoodItem): Long {
        return dao.insertFoodItem(FoodItemMapper.domainToEntity(foodItem))
    }

    override suspend fun updateFoodItem(foodItem: FoodItem) {
        dao.updateFoodItem(FoodItemMapper.domainToEntity(foodItem))
    }

    override suspend fun deleteFoodItem(foodItem: FoodItem) {
        dao.deleteFoodItem(FoodItemMapper.domainToEntity(foodItem))
    }

    override suspend fun deleteFoodItemById(id: Long) {
        dao.deleteFoodItemById(id)
    }

    override fun getFoodItemCount(): Flow<Int> {
        return dao.getFoodItemCount()
    }

    override suspend fun getExpiringBeforeSync(date: LocalDate): List<FoodItem> {
        return dao.getExpiringBeforeSync(date.toString())
            .map { FoodItemMapper.entityToDomain(it) }
    }
}
