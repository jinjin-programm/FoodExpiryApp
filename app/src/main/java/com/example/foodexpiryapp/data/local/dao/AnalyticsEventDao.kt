package com.example.foodexpiryapp.data.local.dao

import androidx.room.*
import com.example.foodexpiryapp.data.local.database.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsEventDao {
    
    @Insert
    suspend fun insertEvent(event: AnalyticsEventEntity): Long
    
    @Query("SELECT * FROM analytics_events WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getEventsSince(startTime: Long): Flow<List<AnalyticsEventEntity>>
    
    @Query("SELECT COUNT(*) FROM analytics_events WHERE eventType = :eventType AND timestamp >= :startTime")
    suspend fun getEventCountByType(eventType: String, startTime: Long): Int
    
    @Query("DELETE FROM analytics_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)
    
    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<AnalyticsEventEntity>>
}
