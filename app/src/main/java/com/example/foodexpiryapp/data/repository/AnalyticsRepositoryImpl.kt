package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.analytics.FoodAnalyticsService
import com.example.foodexpiryapp.data.local.dao.AnalyticsEventDao
import com.example.foodexpiryapp.data.mapper.AnalyticsMapper.toEntity
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.WeeklyStats
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsEventDao: AnalyticsEventDao,
    private val analyticsService: FoodAnalyticsService
) : AnalyticsRepository {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override fun trackEvent(event: AnalyticsEvent) {
        // Save to Room database
        scope.launch {
            analyticsEventDao.insertEvent(event.toEntity())
        }
        
        // Send to Firebase Analytics
        analyticsService.logEvent(event)
    }
    
    override fun getWeeklyStats(): Flow<WeeklyStats> {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        
        return analyticsEventDao.getEventsSince(weekAgo).map { events ->
            WeeklyStats(
                itemsAdded = events.count { it.eventType == EventType.ITEM_ADDED.name },
                itemsEaten = events.count { it.eventType == EventType.ITEM_EATEN.name },
                itemsExpired = events.count { it.eventType == EventType.ITEM_EXPIRED.name },
                itemsDeleted = events.count { it.eventType == EventType.ITEM_DELETED.name },
                notificationsSent = events.count { it.eventType == EventType.NOTIFICATION_SENT.name },
                notificationsOpened = events.count { it.eventType == EventType.NOTIFICATION_OPENED.name }
            )
        }
    }
    
    override suspend fun getEventCount(eventType: EventType, days: Int): Int {
        val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return analyticsEventDao.getEventCountByType(eventType.name, startTime)
    }
    
    override suspend fun cleanupOldEvents(daysToKeep: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000)
        analyticsEventDao.deleteOldEvents(cutoffTime)
    }
}
