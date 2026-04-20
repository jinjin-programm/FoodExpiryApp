package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.data.local.database.FoodItemEntity
import com.example.foodexpiryapp.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FoodItemMapperTest {

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
        imagePath = "/data/food_images/1.jpg"
    )

    private val testDomain = FoodItem(
        id = 1,
        name = "Milk",
        category = FoodCategory.DAIRY,
        expiryDate = LocalDate.of(2026, 1, 15),
        quantity = 2,
        location = StorageLocation.FRIDGE,
        notes = "Organic",
        barcode = "1234567890",
        dateAdded = LocalDate.of(2026, 1, 10),
        notifyEnabled = true,
        notifyDaysBefore = 3,
        purchaseDate = LocalDate.of(2026, 1, 9),
        scanSource = ScanSource.BARCODE,
        confidence = 0.95f,
        riskLevel = RiskLevel.MEDIUM,
        recipeRelevance = 0.5f,
        imagePath = "/data/food_images/1.jpg"
    )

    @Test
    fun `entityToDomain maps all fields correctly`() {
        val result = FoodItemMapper.entityToDomain(testEntity)
        assertEquals(testDomain, result)
    }

    @Test
    fun `domainToEntity maps all fields correctly`() {
        val result = FoodItemMapper.domainToEntity(testDomain)
        assertEquals(testEntity, result)
    }

    @Test
    fun `round trip entity-domain-entity preserves data`() {
        val entityResult = FoodItemMapper.domainToEntity(FoodItemMapper.entityToDomain(testEntity))
        assertEquals(testEntity, entityResult)
    }

    @Test
    fun `round trip domain-entity-domain preserves data`() {
        val domainResult = FoodItemMapper.entityToDomain(FoodItemMapper.domainToEntity(testDomain))
        assertEquals(testDomain, domainResult)
    }

    @Test
    fun `entityToDomain handles unknown category gracefully`() {
        val entity = testEntity.copy(category = "UNKNOWN_CATEGORY")
        val result = FoodItemMapper.entityToDomain(entity)
        assertEquals(FoodCategory.OTHER, result.category)
    }

    @Test
    fun `entityToDomain handles unknown location gracefully`() {
        val entity = testEntity.copy(location = "UNKNOWN_LOCATION")
        val result = FoodItemMapper.entityToDomain(entity)
        assertEquals(StorageLocation.FRIDGE, result.location)
    }

    @Test
    fun `entityToDomain handles unknown scan source gracefully`() {
        val entity = testEntity.copy(scanSource = "UNKNOWN_SOURCE")
        val result = FoodItemMapper.entityToDomain(entity)
        assertEquals(ScanSource.MANUAL, result.scanSource)
    }

    @Test
    fun `entityToDomain handles unknown risk level gracefully`() {
        val entity = testEntity.copy(riskLevel = "UNKNOWN_RISK")
        val result = FoodItemMapper.entityToDomain(entity)
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun `entityToDomain handles null optional fields`() {
        val entity = testEntity.copy(
            barcode = null,
            purchaseDate = null,
            imagePath = null,
            notifyDaysBefore = null
        )
        val result = FoodItemMapper.entityToDomain(entity)
        assertNull(result.barcode)
        assertNull(result.purchaseDate)
        assertNull(result.imagePath)
        assertNull(result.notifyDaysBefore)
    }

    @Test
    fun `domainToEntity converts null optional fields`() {
        val domain = testDomain.copy(
            barcode = null,
            purchaseDate = null,
            imagePath = null,
            notifyDaysBefore = null
        )
        val result = FoodItemMapper.domainToEntity(domain)
        assertNull(result.barcode)
        assertNull(result.purchaseDate)
        assertNull(result.imagePath)
        assertNull(result.notifyDaysBefore)
    }

    @Test
    fun `entityToDomain handles empty category string`() {
        val entity = testEntity.copy(category = "")
        val result = FoodItemMapper.entityToDomain(entity)
        assertEquals(FoodCategory.OTHER, result.category)
    }

    @Test
    fun `entityToDomain handles all valid categories`() {
        FoodCategory.values().forEach { category ->
            val entity = testEntity.copy(category = category.name)
            val result = FoodItemMapper.entityToDomain(entity)
            assertEquals(category, result.category)
        }
    }

    @Test
    fun `entityToDomain handles all valid locations`() {
        StorageLocation.values().forEach { location ->
            val entity = testEntity.copy(location = location.name)
            val result = FoodItemMapper.entityToDomain(entity)
            assertEquals(location, result.location)
        }
    }

    @Test
    fun `entityToDomain handles all valid risk levels`() {
        RiskLevel.values().forEach { riskLevel ->
            val entity = testEntity.copy(riskLevel = riskLevel.name)
            val result = FoodItemMapper.entityToDomain(entity)
            assertEquals(riskLevel, result.riskLevel)
        }
    }

    @Test
    fun `entityToDomain handles all valid scan sources`() {
        ScanSource.values().forEach { scanSource ->
            val entity = testEntity.copy(scanSource = scanSource.name)
            val result = FoodItemMapper.entityToDomain(entity)
            assertEquals(scanSource, result.scanSource)
        }
    }
}
