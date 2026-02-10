package com.example.foodexpiryapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodexpiryapp.MainActivity
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.domain.repository.FoodRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Background worker that runs daily to check for food items about to expire.
 * Uses Hilt for dependency injection via @HiltWorker.
 */
@HiltWorker
class ExpiryNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FoodRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "expiry_notifications"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "daily_expiry_check"
    }

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()

            // Items expiring within the next 3 days
            val cutoffDate = LocalDate.now().plusDays(3)
            val expiringItems = repository.getExpiringBeforeSync(cutoffDate)

            if (expiringItems.isNotEmpty()) {
                val expiredCount = expiringItems.count { it.isExpired }
                val soonCount = expiringItems.size - expiredCount

                val message = buildString {
                    if (expiredCount > 0) {
                        append("$expiredCount item${if (expiredCount > 1) "s" else ""} expired")
                    }
                    if (expiredCount > 0 && soonCount > 0) append(", ")
                    if (soonCount > 0) {
                        append("$soonCount item${if (soonCount > 1) "s" else ""} expiring soon")
                    }
                }

                val details = expiringItems.take(5).joinToString("\n") { item ->
                    val daysText = when {
                        item.daysUntilExpiry < 0 -> "EXPIRED"
                        item.daysUntilExpiry == 0L -> "Today"
                        else -> "${item.daysUntilExpiry}d left"
                    }
                    "${item.name} - $daysText"
                }

                showNotification(message, details)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Food Expiry Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about food items that are about to expire"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, details: String) {
        // On Android 13+ (API 33), check for POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(details.lines().firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
