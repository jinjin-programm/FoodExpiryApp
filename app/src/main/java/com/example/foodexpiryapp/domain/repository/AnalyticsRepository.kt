package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.WeeklyStats
import kotlinx.coroutines.flow.Flow

interface AnalyticsRepository {
    
    fun trackEvent(event: AnalyticsEvent)
    
    fun getWeeklyStats(): Flow<WeeklyStats>
    
    suspend fun getEventCount(eventType: EventType, days: Int = 7): Int
    
    suspend fun cleanupOldEvents(daysToKeep: Int = 30)
}
