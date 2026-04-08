package com.example.foodexpiryapp.presentation.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.remote.ModelDownloadManager
import com.example.foodexpiryapp.domain.model.DownloadUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloadManager: ModelDownloadManager
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

    private val _statusText = MutableStateFlow("Checking model status...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    init {
        checkModelStatus()
    }

    fun checkModelStatus() {
        viewModelScope.launch {
            try {
                val isReady = modelDownloadManager.isModelReady()
                _isModelLoaded.value = isReady
                _statusText.value = if (isReady) "AI model ready" else "AI model not downloaded"

                if (!isReady) {
                    _messages.value = listOf(
                        ChatMessage(
                            content = "AI chat requires the on-device model (~1.2GB). Please use the photo scan feature to trigger the model download first.",
                            isFromUser = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking model status", e)
                _isModelLoaded.value = false
                _statusText.value = "Error checking model: ${e.message}"
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(content = text.trim(), isFromUser = true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true

            if (!_isModelLoaded.value) {
                val aiMessage = ChatMessage(
                    content = "AI model is not available. Please download the model first by using the photo scan feature.",
                    isFromUser = false
                )
                _messages.value = _messages.value + aiMessage
                _isLoading.value = false
                return@launch
            }

            // Note: Full chat LLM integration (free-form conversation) will be enhanced
            // in a future phase. For now, the model is available for food identification.
            val aiMessage = ChatMessage(
                content = "AI chat is now powered by MNN! The model is loaded and ready. Food identification is available through the photo scan feature. Free-form chat will be enhanced in a future update.",
                isFromUser = false
            )
            _messages.value = _messages.value + aiMessage
            _isLoading.value = false
        }
    }

    fun startModelDownload() {
        viewModelScope.launch {
            modelDownloadManager.downloadModel().collect { state ->
                when (state) {
                    is DownloadUiState.Complete -> {
                        _isModelLoaded.value = true
                        _statusText.value = "AI model ready"
                    }
                    is DownloadUiState.Error -> {
                        _statusText.value = "Download failed: ${state.message}"
                    }
                    else -> {
                        _statusText.value = state::class.simpleName ?: "Downloading..."
                    }
                }
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}