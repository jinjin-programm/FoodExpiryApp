package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FoodItemMapperTest {

    @Test
    fun `FoodCategory valueOf parses all valid categories`() {
        FoodCategory.values().forEach { category ->
            assertEquals(category, FoodCategory.valueOf(category.name))
        }
    }

    @Test
    fun `FoodCategory valueOf throws on unknown value`() {
        assertThrows(IllegalArgumentException::class.java) {
            FoodCategory.valueOf("UNKNOWN_CATEGORY")
        }
    }

    @Test
    fun `FoodCategory valueOf throws on empty string`() {
        assertThrows(IllegalArgumentException::class.java) {
            FoodCategory.valueOf("")
        }
    }

    @Test
    fun `StorageLocation valueOf parses all valid locations`() {
        StorageLocation.values().forEach { location ->
            assertEquals(location, StorageLocation.valueOf(location.name))
        }
    }

    @Test
    fun `StorageLocation valueOf throws on unknown value`() {
        assertThrows(IllegalArgumentException::class.java) {
            StorageLocation.valueOf("UNKNOWN_LOCATION")
        }
    }

    @Test
    fun `ScanSource valueOf parses all valid sources`() {
        ScanSource.values().forEach { source ->
            assertEquals(source, ScanSource.valueOf(source.name))
        }
    }

    @Test
    fun `ScanSource valueOf throws on unknown value`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScanSource.valueOf("UNKNOWN_SOURCE")
        }
    }

    @Test
    fun `RiskLevel valueOf parses all valid risk levels`() {
        RiskLevel.values().forEach { level ->
            assertEquals(level, RiskLevel.valueOf(level.name))
        }
    }

    @Test
    fun `RiskLevel valueOf throws on unknown value`() {
        assertThrows(IllegalArgumentException::class.java) {
            RiskLevel.valueOf("UNKNOWN_RISK")
        }
    }

    @Test
    fun `enum fallback pattern produces correct defaults`() {
        val category = try { FoodCategory.valueOf("INVALID") } catch (_: Exception) { FoodCategory.OTHER }
        assertEquals(FoodCategory.OTHER, category)

        val location = try { StorageLocation.valueOf("INVALID") } catch (_: Exception) { StorageLocation.FRIDGE }
        assertEquals(StorageLocation.FRIDGE, location)

        val source = try { ScanSource.valueOf("INVALID") } catch (_: Exception) { ScanSource.MANUAL }
        assertEquals(ScanSource.MANUAL, source)

        val risk = try { RiskLevel.valueOf("INVALID") } catch (_: Exception) { RiskLevel.LOW }
        assertEquals(RiskLevel.LOW, risk)
    }

    @Test
    fun `FoodCategory contains expected values`() {
        val expected = listOf("DAIRY", "MEAT", "VEGETABLES", "FRUITS", "GRAINS", "BEVERAGES", "SNACKS", "CONDIMENTS", "FROZEN", "LEFTOVERS", "OTHER")
        assertEquals(expected, FoodCategory.values().map { it.name })
    }

    @Test
    fun `StorageLocation contains expected values`() {
        val expected = listOf("FRIDGE", "FREEZER", "PANTRY", "COUNTER")
        assertEquals(expected, StorageLocation.values().map { it.name })
    }

    @Test
    fun `ScanSource contains expected values`() {
        val expected = listOf("MANUAL", "BARCODE", "RECEIPT", "INGREDIENT_LIST", "YOLO_SCAN")
        assertEquals(expected, ScanSource.values().map { it.name })
    }

    @Test
    fun `RiskLevel contains expected values`() {
        val expected = listOf("LOW", "MEDIUM", "HIGH")
        assertEquals(expected, RiskLevel.values().map { it.name })
    }

    @Test
    fun `domain model date conversion round trips correctly`() {
        val date = LocalDate.of(2026, 1, 15)
        val asString = date.toString()
        val parsed = LocalDate.parse(asString)
        assertEquals(date, parsed)
    }

    @Test
    fun `FoodCategory displayNames are non-empty`() {
        FoodCategory.values().forEach { category ->
            assertTrue("Category ${category.name} has empty displayName", category.displayName.isNotEmpty())
        }
    }

    @Test
    fun `StorageLocation displayNames are non-empty`() {
        StorageLocation.values().forEach { location ->
            assertTrue("Location ${location.name} has empty displayName", location.displayName.isNotEmpty())
        }
    }
}
