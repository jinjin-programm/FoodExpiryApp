package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventName: String,
    val eventType: String,
    val itemName: String? = null,
    val itemCategory: String? = null,
    val itemLocation: String? = null,
    val additionalData: String = "{}",  // JSON string
    val timestamp: Long = System.currentTimeMillis()
)
