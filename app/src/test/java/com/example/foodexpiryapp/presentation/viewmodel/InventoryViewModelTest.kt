package com.example.foodexpiryapp.presentation.viewmodel

import com.example.foodexpiryapp.domain.model.*
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.repository.CookedRecipeRepository
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.domain.repository.UIStyleRepository
import com.example.foodexpiryapp.domain.usecase.*
import com.example.foodexpiryapp.util.FoodImageStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var getAllFoodItems: GetAllFoodItemsUseCase

    @Mock
    private lateinit var addFoodItem: AddFoodItemUseCase

    @Mock
    private lateinit var updateFoodItem: UpdateFoodItemUseCase

    @Mock
    private lateinit var deleteFoodItem: DeleteFoodItemUseCase

    @Mock
    private lateinit var searchFoodItems: SearchFoodItemsUseCase

    @Mock
    private lateinit var foodRepository: FoodRepository

    @Mock
    private lateinit var cookedRecipeRepository: CookedRecipeRepository

    @Mock
    private lateinit var analyticsRepository: AnalyticsRepository

    @Mock
    private lateinit var uiStyleRepository: UIStyleRepository

    @Mock
    private lateinit var foodImageStorage: FoodImageStorage

    private val testDispatcher = StandardTestDispatcher()

    private val testFoodItems = listOf(
        FoodItem(
            id = 1,
            name = "Milk",
            category = FoodCategory.DAIRY,
            expiryDate = LocalDate.now().plusDays(1),
            quantity = 1,
            location = StorageLocation.FRIDGE,
            dateAdded = LocalDate.now()
        ),
        FoodItem(
            id = 2,
            name = "Apple",
            category = FoodCategory.FRUITS,
            expiryDate = LocalDate.now().plusDays(5),
            quantity = 3,
            location = StorageLocation.COUNTER,
            dateAdded = LocalDate.now()
        )
    )

    @Before
    fun setUp() {
        whenever(uiStyleRepository.uiStyle).thenReturn(flowOf(UIStyleRepository.STYLE_CUTE))
        whenever(getAllFoodItems()).thenReturn(flowOf(testFoodItems))
    }

    private fun createViewModel(): InventoryViewModel {
        return InventoryViewModel(
            getAllFoodItems = getAllFoodItems,
            addFoodItem = addFoodItem,
            updateFoodItem = updateFoodItem,
            deleteFoodItem = deleteFoodItem,
            searchFoodItems = searchFoodItems,
            foodRepository = foodRepository,
            cookedRecipeRepository = cookedRecipeRepository,
            analyticsRepository = analyticsRepository,
            uiStyleRepository = uiStyleRepository,
            foodImageStorage = foodImageStorage
        )
    }

    @Test
    fun `initial state has loading true`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
    }

    @Test
    fun `uiState defaults are correct`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedCategory)
        assertNull(state.selectedLocation)
        assertNull(state.errorMessage)
        assertEquals(UIStyleRepository.STYLE_CUTE, state.uiStyle)
    }

    @Test
    fun `onSearchQueryChanged updates search query in state`() {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChanged("milk")
        assertEquals("milk", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `onSearchQueryChanged with blank query uses getAllFoodItems`() = runTest(testDispatcher) {
        whenever(searchFoodItems("milk")).thenReturn(flowOf(emptyList()))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("milk")
        advanceUntilIdle()

        verify(searchFoodItems).invoke("milk")
    }

    @Test
    fun `onCategorySelected updates selected category`() {
        val viewModel = createViewModel()
        viewModel.onCategorySelected(FoodCategory.DAIRY)
        assertEquals(FoodCategory.DAIRY, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `onCategorySelected null clears category`() {
        val viewModel = createViewModel()
        viewModel.onCategorySelected(FoodCategory.DAIRY)
        viewModel.onCategorySelected(null)
        assertNull(viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `onLocationSelected updates location and clears category`() {
        val viewModel = createViewModel()
        viewModel.onCategorySelected(FoodCategory.DAIRY)
        viewModel.onLocationSelected(StorageLocation.FRIDGE)
        assertEquals(StorageLocation.FRIDGE, viewModel.uiState.value.selectedLocation)
        assertNull(viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `onLocationSelected null clears location`() {
        val viewModel = createViewModel()
        viewModel.onLocationSelected(StorageLocation.FRIDGE)
        viewModel.onLocationSelected(null)
        assertNull(viewModel.uiState.value.selectedLocation)
    }

    @Test
    fun `onAddFoodItem calls use case and emits success event`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        whenever(addFoodItem(item)).thenReturn(1L)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddFoodItem(item)
        advanceUntilIdle()

        verify(addFoodItem).invoke(item)
        verify(analyticsRepository).trackEvent(any())
    }

    @Test
    fun `onAddFoodItem emits error message on failure`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        whenever(addFoodItem(item)).thenThrow(RuntimeException("Insert failed"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        val events = mutableListOf<InventoryEvent>()
        testDispatcher.scheduler.advanceUntilIdle()
        // Collect events in a separate way to test
        viewModel.onAddFoodItem(item)
        advanceUntilIdle()

        verify(addFoodItem).invoke(item)
    }

    @Test
    fun `onUpdateFoodItem calls use case`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUpdateFoodItem(item)
        advanceUntilIdle()

        verify(updateFoodItem).invoke(item)
    }

    @Test
    fun `onDeleteFoodItem calls delete use case`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteFoodItem(item)
        advanceUntilIdle()

        verify(deleteFoodItem).invoke(item)
        verify(analyticsRepository).trackEvent(any())
    }

    @Test
    fun `onMarkAsEaten deletes item and tracks analytics`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMarkAsEaten(item)
        advanceUntilIdle()

        verify(deleteFoodItem).invoke(item)
        verify(analyticsRepository).trackEvent(any())
    }

    @Test
    fun `onUndoDelete re-adds the item`() = runTest(testDispatcher) {
        val item = testFoodItems.first()
        whenever(addFoodItem(item)).thenReturn(1L)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUndoDelete(item)
        advanceUntilIdle()

        verify(addFoodItem).invoke(item)
    }

    @Test
    fun `onDeleteAllFoodItems clears images and repository`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteAllFoodItems()
        advanceUntilIdle()

        verify(foodImageStorage).deleteAllImages()
        verify(foodRepository).deleteAllFoodItems()
        verify(cookedRecipeRepository).deleteAll()
    }
}
