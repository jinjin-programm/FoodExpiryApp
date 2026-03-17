package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.NotificationSettings
import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    fun getNotificationSettings(): Flow<NotificationSettings>
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateDefaultDaysBefore(days: Int)
    suspend fun updateNotificationTime(hour: Int, minute: Int)
}
