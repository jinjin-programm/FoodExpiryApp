package com.example.foodexpiryapp.domain.model

data class AnalyticsEvent(
    val id: Long = 0,
    val eventName: String,
    val eventType: EventType,
    val itemName: String? = null,
    val itemCategory: String? = null,
    val itemLocation: String? = null,
    val additionalData: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
