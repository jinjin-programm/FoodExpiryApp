package com.example.foodexpiryapp.presentation.ui.chat

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val content: String,
    val isFromUser: Boolean
)