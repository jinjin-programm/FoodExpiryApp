package com.example.foodexpiryapp.domain.model

import org.junit.Test
import org.junit.Assert.*

class NotificationSettingsTest {

    @Test
    fun defaultSettings_areCorrect() {
        val settings = NotificationSettings()

        assertTrue(settings.notificationsEnabled)
        assertEquals(3, settings.defaultDaysBefore)
        assertEquals(9, settings.notificationHour)
        assertEquals(0, settings.notificationMinute)
    }

    @Test
    fun customSettings_areStoredCorrectly() {
        val settings = NotificationSettings(
            notificationsEnabled = false,
            defaultDaysBefore = 7,
            notificationHour = 18,
            notificationMinute = 30
        )

        assertFalse(settings.notificationsEnabled)
        assertEquals(7, settings.defaultDaysBefore)
        assertEquals(18, settings.notificationHour)
        assertEquals(30, settings.notificationMinute)
    }

    @Test
    fun settings_withDifferentDaysBefore() {
        val settings1 = NotificationSettings(defaultDaysBefore = 1)
        val settings7 = NotificationSettings(defaultDaysBefore = 7)
        val settings14 = NotificationSettings(defaultDaysBefore = 14)

        assertEquals(1, settings1.defaultDaysBefore)
        assertEquals(7, settings7.defaultDaysBefore)
        assertEquals(14, settings14.defaultDaysBefore)
    }

    @Test
    fun settings_withDifferentTimes() {
        val morningSettings = NotificationSettings(notificationHour = 8, notificationMinute = 0)
        val eveningSettings = NotificationSettings(notificationHour = 20, notificationMinute = 30)

        assertEquals(8, morningSettings.notificationHour)
        assertEquals(0, morningSettings.notificationMinute)
        
        assertEquals(20, eveningSettings.notificationHour)
        assertEquals(30, eveningSettings.notificationMinute)
    }
}
