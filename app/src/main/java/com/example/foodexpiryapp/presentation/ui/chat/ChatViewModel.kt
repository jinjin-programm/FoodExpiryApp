package com.example.foodexpiryapp.presentation.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _statusText = MutableStateFlow("LLM not available")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    init {
        // TODO: MNN LLM integration will be added in Phase 5
        _isModelLoaded.value = false
        _statusText.value = "LLM engine removed — MNN integration pending (Phase 5)"
        _messages.value = listOf(
            ChatMessage(
                content = "AI chat is temporarily unavailable. The LLM engine is being upgraded to MNN. Please use the photo scan feature for food detection.",
                isFromUser = false
            )
        )
    }

    fun checkModelStatus() {
        _isModelLoaded.value = false
        _statusText.value = "LLM engine removed — MNN integration pending (Phase 5)"
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            content = text.trim(),
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true
            val aiMessage = ChatMessage(
                content = "AI chat is temporarily unavailable. The LLM engine is being upgraded to MNN for better performance. This feature will return in a future update.",
                isFromUser = false
            )
            _messages.value = _messages.value + aiMessage
            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}