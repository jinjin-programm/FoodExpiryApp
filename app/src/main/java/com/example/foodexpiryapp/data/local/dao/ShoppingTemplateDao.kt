package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodexpiryapp.data.local.database.ShoppingTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingTemplateDao {
    @Query("SELECT * FROM shopping_templates")
    fun getAllTemplates(): Flow<List<ShoppingTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ShoppingTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ShoppingTemplateEntity>)

    @Query("SELECT COUNT(*) FROM shopping_templates")
    suspend fun getTemplateCount(): Int
}
