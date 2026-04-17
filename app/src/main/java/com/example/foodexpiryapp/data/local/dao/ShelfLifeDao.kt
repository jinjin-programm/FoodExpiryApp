package com.example.foodexpiryapp.data.local.dao

import androidx.room.*
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfLifeDao {

    @Query("SELECT * FROM shelf_life_entries WHERE foodName = :foodName LIMIT 1")
    suspend fun findByName(foodName: String): ShelfLifeEntity?

    @Query("SELECT * FROM shelf_life_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ShelfLifeEntity?

    @Query("SELECT * FROM shelf_life_entries")
    suspend fun getAllSync(): List<ShelfLifeEntity>

    @Query("SELECT * FROM shelf_life_entries WHERE foodName LIKE '%' || :query || '%' ORDER BY hitCount DESC")
    fun searchByName(query: String): Flow<List<ShelfLifeEntity>>

    @Query("SELECT * FROM shelf_life_entries ORDER BY foodName ASC")
    fun getAll(): Flow<List<ShelfLifeEntity>>

    @Query("SELECT * FROM shelf_life_entries WHERE source = :source ORDER BY foodName ASC")
    fun getAllBySource(source: String): Flow<List<ShelfLifeEntity>>

    @Query("SELECT COUNT(*) FROM shelf_life_entries WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("SELECT COUNT(*) FROM shelf_life_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShelfLifeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ShelfLifeEntity>)

    @Update
    suspend fun update(entity: ShelfLifeEntity)

    @Query("UPDATE shelf_life_entries SET hitCount = hitCount + 1, updatedAt = :timestamp WHERE foodName = :foodName")
    suspend fun incrementHitCount(foodName: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM shelf_life_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
