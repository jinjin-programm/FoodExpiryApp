package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.local.database.FoodItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class FoodRepositoryImplTest {

    @Mock
    private lateinit var dao: FoodItemDao

    private lateinit var repository: FoodRepositoryImpl

    private val testEntity = FoodItemEntity(
        id = 1,
        name = "Milk",
        category = "DAIRY",
        expiryDate = "2026-01-15",
        quantity = 2,
        location = "FRIDGE",
        notes = "Organic",
        barcode = "1234567890",
        dateAdded = "2026-01-10",
        notifyEnabled = true,
        notifyDaysBefore = 3,
        purchaseDate = "2026-01-09",
        scanSource = "BARCODE",
        confidence = 0.95f,
        riskLevel = "MEDIUM",
        recipeRelevance = 0.5f,
        imagePath = null
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = FoodRepositoryImpl(dao)
    }

    @Test
    fun `getAllFoodItems maps entities to domain`() = runTest {
        whenever(dao.getAllFoodItems()).thenReturn(flowOf(listOf(testEntity)))
        val result = repository.getAllFoodItems().first()
        assertEquals(1, result.size)
        assertEquals("Milk", result[0].name)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `getAllFoodItems returns empty list when no data`() = runTest {
        whenever(dao.getAllFoodItems()).thenReturn(flowOf(emptyList()))
        val result = repository.getAllFoodItems().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFoodItemById returns mapped domain item`() = runTest {
        whenever(dao.getFoodItemById(1L)).thenReturn(testEntity)
        val result = repository.getFoodItemById(1L)
        assertNotNull(result)
        assertEquals("Milk", result!!.name)
    }

    @Test
    fun `getFoodItemById returns null when not found`() = runTest {
        whenever(dao.getFoodItemById(99L)).thenReturn(null)
        val result = repository.getFoodItemById(99L)
        assertNull(result)
    }

    @Test
    fun `insertFoodItem delegates to dao and returns id`() = runTest {
        whenever(dao.insertFoodItem(any())).thenReturn(42L)
        val domainItem = com.example.foodexpiryapp.domain.model.FoodItem(
            id = 0,
            name = "Test",
            category = com.example.foodexpiryapp.domain.model.FoodCategory.OTHER,
            expiryDate = LocalDate.now().plusDays(7),
            dateAdded = LocalDate.now()
        )
        val result = repository.insertFoodItem(domainItem)
        assertEquals(42L, result)
        verify(dao).insertFoodItem(any())
    }

    @Test
    fun `updateFoodItem delegates to dao`() = runTest {
        val domainItem = com.example.foodexpiryapp.domain.model.FoodItem(
            id = 1,
            name = "Updated Milk",
            category = com.example.foodexpiryapp.domain.model.FoodCategory.DAIRY,
            expiryDate = LocalDate.now().plusDays(5),
            dateAdded = LocalDate.now()
        )
        repository.updateFoodItem(domainItem)
        verify(dao).updateFoodItem(any())
    }

    @Test
    fun `deleteFoodItem delegates to dao`() = runTest {
        val domainItem = com.example.foodexpiryapp.domain.model.FoodItem(
            id = 1,
            name = "Milk",
            category = com.example.foodexpiryapp.domain.model.FoodCategory.DAIRY,
            expiryDate = LocalDate.now().plusDays(5),
            dateAdded = LocalDate.now()
        )
        repository.deleteFoodItem(domainItem)
        verify(dao).deleteFoodItem(any())
    }

    @Test
    fun `deleteFoodItemById delegates to dao`() = runTest {
        repository.deleteFoodItemById(1L)
        verify(dao).deleteFoodItemById(1L)
    }

    @Test
    fun `deleteAllFoodItems delegates to dao`() = runTest {
        repository.deleteAllFoodItems()
        verify(dao).deleteAllFoodItems()
    }

    @Test
    fun `searchFoodItems maps results`() = runTest {
        whenever(dao.searchFoodItems("mil")).thenReturn(flowOf(listOf(testEntity)))
        val result = repository.searchFoodItems("mil").first()
        assertEquals(1, result.size)
        assertEquals("Milk", result[0].name)
    }

    @Test
    fun `getExpiringBefore maps results`() = runTest {
        val date = "2026-01-15"
        whenever(dao.getExpiringBefore(date)).thenReturn(flowOf(listOf(testEntity)))
        val result = repository.getExpiringBefore(LocalDate.of(2026, 1, 15)).first()
        assertEquals(1, result.size)
    }

    @Test
    fun `getFoodItemCount delegates to dao`() = runTest {
        whenever(dao.getFoodItemCount()).thenReturn(flowOf(5))
        val result = repository.getFoodItemCount().first()
        assertEquals(5, result)
    }

    @Test
    fun `getExpiringBeforeSync maps results`() = runTest {
        val date = "2026-01-15"
        whenever(dao.getExpiringBeforeSync(date)).thenReturn(listOf(testEntity))
        val result = repository.getExpiringBeforeSync(LocalDate.of(2026, 1, 15))
        assertEquals(1, result.size)
        assertEquals("Milk", result[0].name)
    }
}
