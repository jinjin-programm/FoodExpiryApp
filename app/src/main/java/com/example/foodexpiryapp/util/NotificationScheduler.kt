package com.example.foodexpiryapp.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodexpiryapp.domain.model.NotificationSettings
import com.example.foodexpiryapp.worker.ExpiryNotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleDailyNotification(context: Context, settings: NotificationSettings) {
        val workManager = WorkManager.getInstance(context)

        if (!settings.notificationsEnabled) {
            workManager.cancelUniqueWork(ExpiryNotificationWorker.WORK_NAME)
            return
        }

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.notificationHour)
            set(Calendar.MINUTE, settings.notificationMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time is in the past today, schedule for tomorrow
        if (targetTime.before(currentTime)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = targetTime.timeInMillis - currentTime.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ExpiryNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace to update the schedule time
            workRequest
        )
    }
}
