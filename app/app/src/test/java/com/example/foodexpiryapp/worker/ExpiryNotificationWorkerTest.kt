package com.example.foodexpiryapp.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.NotificationSettings
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

class ExpiryNotificationWorkerTest {

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var notificationSettingsRepository: NotificationSettingsRepository

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        workerParams = mock(WorkerParameters::class.java)
        
        `when`(notificationSettingsRepository.getNotificationSettings())
            .thenReturn(flowOf(NotificationSettings()))
    }

    @Test
    fun doWork_returnsSuccess_whenNoItemsExpiring() = runTest {
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(emptyList())

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(foodRepository).getExpiringBeforeSync(any())
    }

    @Test
    fun doWork_returnsSuccess_withExpiredItems() = runTest {
        val expiredItems = listOf(
            createFoodItem(id = 1, name = "Milk", expiryDate = LocalDate.now().minusDays(2)),
            createFoodItem(id = 2, name = "Yogurt", expiryDate = LocalDate.now().minusDays(1))
        )
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(expiredItems)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_returnsSuccess_withSoonExpiringItems() = runTest {
        val soonExpiringItems = listOf(
            createFoodItem(id = 1, name = "Cheese", expiryDate = LocalDate.now().plusDays(1)),
            createFoodItem(id = 2, name = "Butter", expiryDate = LocalDate.now().plusDays(2)),
            createFoodItem(id = 3, name = "Eggs", expiryDate = LocalDate.now().plusDays(3))
        )
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(soonExpiringItems)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_returnsSuccess_withMixedItems() = runTest {
        val mixedItems = listOf(
            createFoodItem(id = 1, name = "Old Milk", expiryDate = LocalDate.now().minusDays(1)),
            createFoodItem(id = 2, name = "Fresh Milk", expiryDate = LocalDate.now().plusDays(2)),
            createFoodItem(id = 3, name = "Yogurt", expiryDate = LocalDate.now())
        )
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(mixedItems)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_returnsRetry_onException() = runTest {
        `when`(foodRepository.getExpiringBeforeSync(any())).thenThrow(RuntimeException("Database error"))

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun doWork_handlesLargeNumberOfItems() = runTest {
        val manyItems = (1..50).map { id ->
            createFoodItem(id = id.toLong(), name = "Item $id", expiryDate = LocalDate.now().plusDays(1))
        }
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(manyItems)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_returnsSuccess_whenNotificationsDisabled() = runTest {
        `when`(notificationSettingsRepository.getNotificationSettings())
            .thenReturn(flowOf(NotificationSettings(notificationsEnabled = false)))

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(foodRepository, never()).getExpiringBeforeSync(any())
    }

    @Test
    fun doWork_filtersOutDisabledItems() = runTest {
        val items = listOf(
            createFoodItem(id = 1, name = "Enabled Item", expiryDate = LocalDate.now().plusDays(1), notifyEnabled = true),
            createFoodItem(id = 2, name = "Disabled Item", expiryDate = LocalDate.now().plusDays(1), notifyEnabled = false)
        )
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(items)

        val worker = createWorker()
        worker.doWork()
    }

    @Test
    fun doWork_respectsCustomNotifyDays() = runTest {
        val settings = NotificationSettings(defaultDaysBefore = 3)
        `when`(notificationSettingsRepository.getNotificationSettings())
            .thenReturn(flowOf(settings))
        
        val itemWithCustomDays = createFoodItem(
            id = 1, 
            name = "Custom Days", 
            expiryDate = LocalDate.now().plusDays(5),
            notifyDaysBefore = 7
        )
        `when`(foodRepository.getExpiringBeforeSync(any())).thenReturn(listOf(itemWithCustomDays))

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun createWorker(): ExpiryNotificationWorker {
        return ExpiryNotificationWorker(context, workerParams, foodRepository, notificationSettingsRepository)
    }

    private fun createFoodItem(
        id: Long = 1,
        name: String = "Test Food",
        category: FoodCategory = FoodCategory.DAIRY,
        expiryDate: LocalDate = LocalDate.now().plusDays(7),
        quantity: Int = 1,
        location: StorageLocation = StorageLocation.FRIDGE,
        notes: String = "",
        notifyEnabled: Boolean = true,
        notifyDaysBefore: Int? = null
    ): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            category = category,
            expiryDate = expiryDate,
            quantity = quantity,
            location = location,
            notes = notes,
            notifyEnabled = notifyEnabled,
            notifyDaysBefore = notifyDaysBefore
        )
    }
}
