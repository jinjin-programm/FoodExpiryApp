package com.example.foodexpiryapp.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shelf_life_entries",
    indices = [Index(value = ["foodName"], unique = true)]
)
data class ShelfLifeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "foodName") val foodName: String,
    @ColumnInfo(name = "shelfLifeDays") val shelfLifeDays: Int,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "hitCount") val hitCount: Int = 0,
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)
