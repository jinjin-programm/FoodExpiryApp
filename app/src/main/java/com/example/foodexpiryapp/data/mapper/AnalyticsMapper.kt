package com.example.foodexpiryapp.data.mapper

import com.example.foodexpiryapp.data.local.database.AnalyticsEventEntity
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AnalyticsMapper {
    
    private val gson = Gson()
    
    fun AnalyticsEvent.toEntity(): AnalyticsEventEntity {
        return AnalyticsEventEntity(
            id = id,
            eventName = eventName,
            eventType = eventType.name,
            itemName = itemName,
            itemCategory = itemCategory,
            itemLocation = itemLocation,
            additionalData = gson.toJson(additionalData),
            timestamp = timestamp
        )
    }
    
    fun AnalyticsEventEntity.toDomain(): AnalyticsEvent {
        val type = try {
            EventType.valueOf(eventType)
        } catch (e: Exception) {
            EventType.SCREEN_VIEW
        }
        
        val dataMap: Map<String, String> = try {
            gson.fromJson(additionalData, object : TypeToken<Map<String, String>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }
        
        return AnalyticsEvent(
            id = id,
            eventName = eventName,
            eventType = type,
            itemName = itemName,
            itemCategory = itemCategory,
            itemLocation = itemLocation,
            additionalData = dataMap,
            timestamp = timestamp
        )
    }
}
