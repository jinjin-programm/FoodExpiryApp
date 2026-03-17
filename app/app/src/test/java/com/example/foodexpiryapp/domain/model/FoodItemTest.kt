package com.example.foodexpiryapp.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class FoodItemTest {

    @Test
    fun isExpired_returnsTrue_whenExpiryDateIsBeforeToday() {
        val yesterday = LocalDate.now().minusDays(1)
        val foodItem = createFoodItem(expiryDate = yesterday)

        assertTrue(foodItem.isExpired)
    }

    @Test
    fun isExpired_returnsFalse_whenExpiryDateIsToday() {
        val today = LocalDate.now()
        val foodItem = createFoodItem(expiryDate = today)

        assertFalse(foodItem.isExpired)
    }

    @Test
    fun isExpired_returnsFalse_whenExpiryDateIsAfterToday() {
        val tomorrow = LocalDate.now().plusDays(1)
        val foodItem = createFoodItem(expiryDate = tomorrow)

        assertFalse(foodItem.isExpired)
    }

    @Test
    fun daysUntilExpiry_returnsPositive_forFutureDate() {
        val futureDate = LocalDate.now().plusDays(5)
        val foodItem = createFoodItem(expiryDate = futureDate)

        assertTrue(foodItem.daysUntilExpiry > 0)
        assertEquals(5, foodItem.daysUntilExpiry)
    }

    @Test
    fun daysUntilExpiry_returnsNegative_forPastDate() {
        val pastDate = LocalDate.now().minusDays(3)
        val foodItem = createFoodItem(expiryDate = pastDate)

        assertTrue(foodItem.daysUntilExpiry < 0)
        assertEquals(-3, foodItem.daysUntilExpiry)
    }

    @Test
    fun daysUntilExpiry_returnsZero_forToday() {
        val today = LocalDate.now()
        val foodItem = createFoodItem(expiryDate = today)

        assertEquals(0, foodItem.daysUntilExpiry)
    }

    @Test
    fun foodItem_defaultValues_areCorrect() {
        val foodItem = FoodItem(
            id = 1,
            name = "Test Item",
            category = FoodCategory.DAIRY,
            expiryDate = LocalDate.now().plusDays(7)
        )

        assertEquals(1, foodItem.quantity)
        assertEquals(StorageLocation.FRIDGE, foodItem.location)
        assertEquals("", foodItem.notes)
        assertNull(foodItem.barcode)
        assertEquals(LocalDate.now(), foodItem.dateAdded)
    }

    @Test
    fun foodItem_customValues_areStoredCorrectly() {
        val expiryDate = LocalDate.now().plusDays(10)
        val addedDate = LocalDate.now().minusDays(2)
        val foodItem = FoodItem(
            id = 42,
            name = "Custom Food",
            category = FoodCategory.MEAT,
            expiryDate = expiryDate,
            quantity = 5,
            location = StorageLocation.FREEZER,
            notes = "Test notes",
            barcode = "123456789",
            dateAdded = addedDate
        )

        assertEquals(42, foodItem.id)
        assertEquals("Custom Food", foodItem.name)
        assertEquals(FoodCategory.MEAT, foodItem.category)
        assertEquals(expiryDate, foodItem.expiryDate)
        assertEquals(5, foodItem.quantity)
        assertEquals(StorageLocation.FREEZER, foodItem.location)
        assertEquals("Test notes", foodItem.notes)
        assertEquals("123456789", foodItem.barcode)
        assertEquals(addedDate, foodItem.dateAdded)
    }

    @Test
    fun foodCategory_displayNames_areCorrect() {
        assertEquals("Dairy", FoodCategory.DAIRY.displayName)
        assertEquals("Meat", FoodCategory.MEAT.displayName)
        assertEquals("Vegetables", FoodCategory.VEGETABLES.displayName)
        assertEquals("Fruits", FoodCategory.FRUITS.displayName)
        assertEquals("Grains", FoodCategory.GRAINS.displayName)
    }

    @Test
    fun storageLocation_displayNames_areCorrect() {
        assertEquals("Fridge", StorageLocation.FRIDGE.displayName)
        assertEquals("Freezer", StorageLocation.FREEZER.displayName)
        assertEquals("Pantry", StorageLocation.PANTRY.displayName)
        assertEquals("Counter", StorageLocation.COUNTER.displayName)
    }

    private fun createFoodItem(
        id: Long = 1,
        name: String = "Test Food",
        category: FoodCategory = FoodCategory.DAIRY,
        expiryDate: LocalDate = LocalDate.now().plusDays(7),
        quantity: Int = 1,
        location: StorageLocation = StorageLocation.FRIDGE,
        notes: String = "",
        barcode: String? = null,
        dateAdded: LocalDate = LocalDate.now()
    ): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            category = category,
            expiryDate = expiryDate,
            quantity = quantity,
            location = location,
            notes = notes,
            barcode = barcode,
            dateAdded = dateAdded
        )
    }
}
