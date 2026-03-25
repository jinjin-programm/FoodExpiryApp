package com.example.foodexpiryapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.local.dao.AnalyticsEventDao
import com.example.foodexpiryapp.data.local.dao.MealPlanDao
import com.example.foodexpiryapp.data.local.dao.ShoppingItemDao

@Database(
    entities = [FoodItemEntity::class, AnalyticsEventEntity::class, MealPlanEntity::class, ShoppingItemEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingItemDao(): ShoppingItemDao

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
    }
}
