package com.example.foodexpiryapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodexpiryapp.presentation.ui.llm.LlamaBridge
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

        CoroutineScope(Dispatchers.Default).launch {
            try {
                delay(1200)
                val bridge = LlamaBridge.getInstance()
                bridge.logDeviceProfile(this@FoodExpiryApp)
                val report = bridge.ensureModelLoaded(
                    context = this@FoodExpiryApp,
                    config = LlamaBridge.recommendedVisionConfig(),
                )
                android.util.Log.i(
                    "FoodExpiryApp",
                    "LLM warmup: success=${report.success}, load_ms=${report.loadTimeMs}, context=${report.contextSize}, threads=${report.threads}, model_mb=${report.modelSizeBytes / (1024 * 1024)}, mmproj_mb=${report.mmprojSizeBytes / (1024 * 1024)}"
                )
            } catch (e: Exception) {
                android.util.Log.w("FoodExpiryApp", "LLM warmup skipped: ${e.message}")
            }
        }

        android.util.Log.d("FoodExpiryApp", "Application onCreate finished")
    }
}
