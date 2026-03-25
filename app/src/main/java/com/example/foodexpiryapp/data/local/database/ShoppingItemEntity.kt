package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodexpiryapp.domain.model.ShoppingItem

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isChecked: Boolean = false
) {
    fun toDomain(): ShoppingItem = ShoppingItem(id, name, isChecked)
    
    companion object {
        fun fromDomain(item: ShoppingItem): ShoppingItemEntity = 
            ShoppingItemEntity(item.id, item.name, item.isChecked)
    }
}
