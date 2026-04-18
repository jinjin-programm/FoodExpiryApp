package com.example.foodexpiryapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface UIStyleRepository {
    val uiStyle: Flow<String>
    suspend fun setUIStyle(style: String)

    companion object {
        const val STYLE_CUTE = "cute"
        const val STYLE_ORIGINAL = "original"
    }
}
