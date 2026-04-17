package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import kotlinx.coroutines.flow.Flow

interface ShelfLifeRepository {
    fun getAll(): Flow<List<ShelfLifeEntity>>
    fun getAllBySource(source: String): Flow<List<ShelfLifeEntity>>
    fun searchByName(query: String): Flow<List<ShelfLifeEntity>>
    suspend fun findByName(foodName: String): ShelfLifeEntity?
    suspend fun lookup(foodName: String): ShelfLifeEntity?
    suspend fun saveLearnedEntry(foodName: String, shelfLifeDays: Int, category: String, location: String)
    suspend fun confirmEntry(id: Long)
    suspend fun updateEntry(entity: ShelfLifeEntity)
    suspend fun deleteEntry(id: Long)
    suspend fun count(): Int
    suspend fun countBySource(source: String): Int
    suspend fun ensureSeeded()
}
