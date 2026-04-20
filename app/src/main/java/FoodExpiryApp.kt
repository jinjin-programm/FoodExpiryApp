package com.example.foodexpiryapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import com.example.foodexpiryapp.util.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FoodExpiryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationSettingsRepository: NotificationSettingsRepository

    @Inject
    lateinit var modelStorageManager: com.example.foodexpiryapp.data.local.ModelStorageManager

    @Inject
    lateinit var hkRecipeSeeder: com.example.foodexpiryapp.data.local.HkRecipeSeeder

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FoodExpiryApp", "Application onCreate started")

        CoroutineScope(Dispatchers.IO).launch {
            val settings = notificationSettingsRepository.getNotificationSettings().first()
            NotificationScheduler.scheduleDailyNotification(this@FoodExpiryApp, settings)
        }

        CoroutineScope(Dispatchers.IO).launch {
            hkRecipeSeeder.seedIfNeeded(this@FoodExpiryApp)
        }

        // Phase 5: Cleanup incomplete model downloads from previous sessions
        try {
            modelStorageManager.cleanupIncompleteDownloads()
            android.util.Log.d("FoodExpiryApp", "Incomplete model downloads cleaned up")
        } catch (e: Exception) {
            android.util.Log.w("FoodExpiryApp", "Failed to cleanup downloads", e)
        }

        android.util.Log.d("FoodExpiryApp", "Application onCreate finished")
    }
}
