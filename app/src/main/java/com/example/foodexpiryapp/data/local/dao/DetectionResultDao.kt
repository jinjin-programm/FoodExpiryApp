package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for DetectionResultEntity.
 * Provides CRUD operations for the temporary detection results table.
 */
@Dao
interface DetectionResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<DetectionResultEntity>)

    @Query("SELECT * FROM detection_results WHERE sessionId = :sessionId ORDER BY indexInSession ASC")
    fun getBySessionId(sessionId: String): Flow<List<DetectionResultEntity>>

    @Query("SELECT * FROM detection_results WHERE sessionId = :sessionId ORDER BY indexInSession ASC")
    suspend fun getBySessionIdSync(sessionId: String): List<DetectionResultEntity>

    @Query("DELETE FROM detection_results WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM detection_results WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Update
    suspend fun update(result: DetectionResultEntity)
}