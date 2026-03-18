package com.example.foodexpiryapp.domain.model

data class WeeklyStats(
    val itemsAdded: Int = 0,
    val itemsEaten: Int = 0,
    val itemsExpired: Int = 0,
    val itemsDeleted: Int = 0,
    val notificationsSent: Int = 0,
    val notificationsOpened: Int = 0
)
