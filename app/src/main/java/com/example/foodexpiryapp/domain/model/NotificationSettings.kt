package com.example.foodexpiryapp.domain.model

data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val defaultDaysBefore: Int = 3,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0
)
