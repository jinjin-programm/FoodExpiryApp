package com.example.foodexpiryapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.local.dao.AnalyticsEventDao
import com.example.foodexpiryapp.data.local.dao.MealPlanDao
import com.example.foodexpiryapp.data.local.dao.ShoppingItemDao
import com.example.foodexpiryapp.data.local.dao.CookedRecipeDao
import com.example.foodexpiryapp.data.local.dao.LocalRecipeDao
import com.example.foodexpiryapp.data.local.dao.ShoppingTemplateDao
import com.example.foodexpiryapp.data.local.dao.DownloadStateDao
import com.example.foodexpiryapp.data.local.dao.DetectionResultDao

@Database(
    entities = [FoodItemEntity::class, AnalyticsEventEntity::class, MealPlanEntity::class, ShoppingItemEntity::class, CookedRecipeEntity::class, LocalRecipeEntity::class, ShoppingTemplateEntity::class, DownloadStateEntity::class, DetectionResultEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun cookedRecipeDao(): CookedRecipeDao
    abstract fun localRecipeDao(): LocalRecipeDao
    abstract fun shoppingTemplateDao(): ShoppingTemplateDao
    abstract fun downloadStateDao(): DownloadStateDao
    abstract fun detectionResultDao(): DetectionResultDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        isChecked INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS analytics_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventName TEXT NOT NULL,
                        eventType TEXT NOT NULL,
                        itemName TEXT,
                        itemCategory TEXT,
                        itemLocation TEXT,
                        additionalData TEXT NOT NULL DEFAULT '{}',
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        slot TEXT NOT NULL,
                        itemType TEXT NOT NULL,
                        recipeId INTEGER,
                        recipeName TEXT,
                        productName TEXT,
                        inventoryItemId INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cooked_recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        recipeName TEXT NOT NULL,
                        cookedAt INTEGER NOT NULL,
                        moneySaved REAL NOT NULL,
                        wasteRescuedPercent INTEGER NOT NULL,
                        matchedIngredients TEXT NOT NULL,
                        imageUrl TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        imageUrl TEXT,
                        ingredients TEXT NOT NULL,
                        steps TEXT NOT NULL,
                        prepTimeMinutes INTEGER NOT NULL DEFAULT 0,
                        cookTimeMinutes INTEGER NOT NULL DEFAULT 0,
                        servings INTEGER NOT NULL DEFAULT 2,
                        cuisine TEXT NOT NULL DEFAULT '',
                        tags TEXT NOT NULL DEFAULT '',
                        estimatedCost REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_items ADD COLUMN purchaseDate TEXT")
                database.execSQL("ALTER TABLE food_items ADD COLUMN scanSource TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE food_items ADD COLUMN confidence REAL NOT NULL DEFAULT 1.0")
                database.execSQL("ALTER TABLE food_items ADD COLUMN riskLevel TEXT NOT NULL DEFAULT 'LOW'")
                database.execSQL("ALTER TABLE food_items ADD COLUMN recipeRelevance REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        itemNames TEXT NOT NULL DEFAULT '[]'
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS download_state (
                        filePath TEXT NOT NULL PRIMARY KEY,
                        totalBytes INTEGER NOT NULL DEFAULT 0,
                        downloadedBytes INTEGER NOT NULL DEFAULT 0,
                        eTag TEXT,
                        lastModified TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        errorMessage TEXT,
                        expectedSha256 TEXT,
                        actualSha256 TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS detection_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        indexInSession INTEGER NOT NULL,
                        foodName TEXT NOT NULL,
                        foodNameZh TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'OTHER',
                        confidence REAL NOT NULL DEFAULT 0.0,
                        llmConfidence REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        boundingBoxLeft REAL NOT NULL DEFAULT 0.0,
                        boundingBoxTop REAL NOT NULL DEFAULT 0.0,
                        boundingBoxRight REAL NOT NULL DEFAULT 0.0,
                        boundingBoxBottom REAL NOT NULL DEFAULT 0.0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
