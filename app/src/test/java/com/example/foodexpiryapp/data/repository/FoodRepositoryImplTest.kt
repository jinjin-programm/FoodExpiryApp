package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FoodRepositoryImplTest {

    @Mock
    private lateinit var dao: FoodItemDao

    private lateinit var repository: FoodRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = FoodRepositoryImpl(dao)
    }

    @Test
    fun `getAllFoodItems delegates to dao`() = runTest {
        whenever(dao.getAllFoodItems()).thenReturn(flowOf(emptyList()))
        val result = repository.getAllFoodItems().first()
        verify(dao).getAllFoodItems()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAllFoodItems returns empty list when no data`() = runTest {
        whenever(dao.getAllFoodItems()).thenReturn(flowOf(emptyList()))
        val result = repository.getAllFoodItems().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFoodItemById returns null when dao returns null`() = runTest {
        whenever(dao.getFoodItemById(99L)).thenReturn(null)
        val result = repository.getFoodItemById(99L)
        assertNull(result)
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
    fun `searchFoodItems delegates to dao with query`() = runTest {
        whenever(dao.searchFoodItems("milk")).thenReturn(flowOf(emptyList()))
        repository.searchFoodItems("milk").first()
        verify(dao).searchFoodItems("milk")
    }

    @Test
    fun `getExpiringBefore delegates to dao with date string`() = runTest {
        whenever(dao.getExpiringBefore("2026-01-15")).thenReturn(flowOf(emptyList()))
        repository.getExpiringBefore(java.time.LocalDate.of(2026, 1, 15)).first()
        verify(dao).getExpiringBefore("2026-01-15")
    }

    @Test
    fun `getFoodItemCount delegates to dao`() = runTest {
        whenever(dao.getFoodItemCount()).thenReturn(flowOf(5))
        val result = repository.getFoodItemCount().first()
        assertEquals(5, result)
    }

    @Test
    fun `getExpiringBeforeSync delegates to dao with date string`() = runTest {
        whenever(dao.getExpiringBeforeSync("2026-01-15")).thenReturn(emptyList())
        repository.getExpiringBeforeSync(java.time.LocalDate.of(2026, 1, 15))
        verify(dao).getExpiringBeforeSync("2026-01-15")
    }

    @Test
    fun `getFoodItemsByCategory delegates to dao with category name`() = runTest {
        whenever(dao.getFoodItemsByCategory("DAIRY")).thenReturn(flowOf(emptyList()))
        repository.getFoodItemsByCategory(com.example.foodexpiryapp.domain.model.FoodCategory.DAIRY).first()
        verify(dao).getFoodItemsByCategory("DAIRY")
    }

    @Test
    fun `getFoodItemsByLocation delegates to dao with location name`() = runTest {
        whenever(dao.getFoodItemsByLocation("FRIDGE")).thenReturn(flowOf(emptyList()))
        repository.getFoodItemsByLocation(com.example.foodexpiryapp.domain.model.StorageLocation.FRIDGE).first()
        verify(dao).getFoodItemsByLocation("FRIDGE")
    }
}
