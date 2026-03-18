package com.example.foodexpiryapp.data.analytics

import android.os.Bundle
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodAnalyticsService @Inject constructor() {
    
    private var firebaseAnalytics: FirebaseAnalytics? = try {
        Firebase.analytics
    } catch (e: Exception) {
        null
    }
    
    fun logEvent(event: AnalyticsEvent) {
        try {
            val bundle = Bundle().apply {
                putString("event_name", event.eventName)
                putString("event_type", event.eventType.name)
                event.itemName?.let { putString("item_name", it) }
                event.itemCategory?.let { putString("item_category", it) }
                event.itemLocation?.let { putString("item_location", it) }
                putLong("timestamp", event.timestamp)
                
                // Add additional data
                event.additionalData.forEach { (key, value) ->
                    putString(key, value)
                }
            }
            
            firebaseAnalytics?.logEvent(event.eventName, bundle)
        } catch (e: Exception) {
            // Firebase not available, log locally only
            android.util.Log.d("Analytics", "Firebase unavailable: ${e.message}")
        }
    }
    
    fun logItemAdded(name: String, category: String, location: String) {
        logEvent(AnalyticsEvent(
            eventName = "item_added",
            eventType = EventType.ITEM_ADDED,
            itemName = name,
            itemCategory = category,
            itemLocation = location
        ))
    }
    
    fun logItemEaten(name: String, daysBeforeExpiry: Int) {
        logEvent(AnalyticsEvent(
            eventName = "item_eaten",
            eventType = EventType.ITEM_EATEN,
            itemName = name,
            additionalData = mapOf("days_before_expiry" to daysBeforeExpiry.toString())
        ))
    }
    
    fun logItemDeleted(name: String, reason: String = "user_deleted") {
        logEvent(AnalyticsEvent(
            eventName = "item_deleted",
            eventType = EventType.ITEM_DELETED,
            itemName = name,
            additionalData = mapOf("reason" to reason)
        ))
    }
    
    fun logItemExpired(name: String, daysExpired: Int) {
        logEvent(AnalyticsEvent(
            eventName = "item_expired",
            eventType = EventType.ITEM_EXPIRED,
            itemName = name,
            additionalData = mapOf("days_expired" to daysExpired.toString())
        ))
    }
    
    fun logNotificationSent(expiringCount: Int, expiredCount: Int) {
        logEvent(AnalyticsEvent(
            eventName = "notification_sent",
            eventType = EventType.NOTIFICATION_SENT,
            additionalData = mapOf(
                "expiring_count" to expiringCount.toString(),
                "expired_count" to expiredCount.toString()
            )
        ))
    }
    
    fun logNotificationOpened(notificationType: String) {
        logEvent(AnalyticsEvent(
            eventName = "notification_opened",
            eventType = EventType.NOTIFICATION_OPENED,
            additionalData = mapOf("notification_type" to notificationType)
        ))
    }
    
    fun logScanSuccess(scanType: String, itemName: String) {
        logEvent(AnalyticsEvent(
            eventName = "scan_success",
            eventType = EventType.SCAN_SUCCESS,
            itemName = itemName,
            additionalData = mapOf("scan_type" to scanType)
        ))
    }
    
    fun logScanFailure(scanType: String, error: String) {
        logEvent(AnalyticsEvent(
            eventName = "scan_failure",
            eventType = EventType.SCAN_FAILURE,
            additionalData = mapOf("scan_type" to scanType, "error" to error)
        ))
    }
    
    fun logScreenView(screenName: String) {
        logEvent(AnalyticsEvent(
            eventName = "screen_view",
            eventType = EventType.SCREEN_VIEW,
            additionalData = mapOf("screen_name" to screenName)
        ))
    }
    
    fun logAppOpened() {
        logEvent(AnalyticsEvent(
            eventName = "app_opened",
            eventType = EventType.APP_OPENED
        ))
    }
}
