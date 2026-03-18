package com.example.foodexpiryapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.local.dao.AnalyticsEventDao

@Database(
    entities = [FoodItemEntity::class, AnalyticsEventEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun analyticsEventDao(): AnalyticsEventDao

    companion object {
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
    }
}
