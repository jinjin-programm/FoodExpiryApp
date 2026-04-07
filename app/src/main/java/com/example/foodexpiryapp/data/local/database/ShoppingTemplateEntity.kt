package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodexpiryapp.domain.model.ShoppingTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "shopping_templates")
data class ShoppingTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val itemNames: String = "[]"
) {
    fun toDomain(): ShoppingTemplate {
        val listType = object : TypeToken<List<String>>() {}.type
        val items: List<String> = try {
            Gson().fromJson(itemNames, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return ShoppingTemplate(id, name, description, items)
    }

    companion object {
        fun fromDomain(template: ShoppingTemplate): ShoppingTemplateEntity {
            val json = Gson().toJson(template.itemNames)
            return ShoppingTemplateEntity(template.id, template.name, template.description, json)
        }
    }
}
