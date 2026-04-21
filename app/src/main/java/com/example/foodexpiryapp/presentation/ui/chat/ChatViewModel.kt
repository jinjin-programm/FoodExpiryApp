package com.example.foodexpiryapp.presentation.ui.chat

import com.example.foodexpiryapp.util.AppLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.remote.ProviderConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioServerConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioVisionClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaServerConfig
import com.example.foodexpiryapp.data.remote.ollama.OllamaVisionClient
import com.example.foodexpiryapp.domain.client.FoodVisionClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val ollamaClient: OllamaVisionClient,
    private val lmStudioClient: LmStudioVisionClient,
    private val ollamaServerConfig: OllamaServerConfig,
    private val lmStudioServerConfig: LmStudioServerConfig,
    private val providerConfig: ProviderConfig
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _statusText = MutableStateFlow("Checking server connection...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private suspend fun getActiveClient(): FoodVisionClient {
        val provider = providerConfig.getProvider()
        return if (provider == ProviderConfig.PROVIDER_LMSTUDIO) lmStudioClient else ollamaClient
    }

    init {
        checkServerConnection()
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            try {
                val client = getActiveClient()
                val provider = providerConfig.getProvider()
                val providerName = if (provider == ProviderConfig.PROVIDER_LMSTUDIO) "LM Studio" else "Ollama"
                val modelName = if (provider == ProviderConfig.PROVIDER_LMSTUDIO) {
                    lmStudioServerConfig.getModelName()
                } else {
                    ollamaServerConfig.getModelName()
                }

                val isConnected = client.testConnection()
                _isServerConnected.value = isConnected
                _statusText.value = if (isConnected) {
                    "$providerName connected ($modelName)"
                } else {
                    "$providerName not connected"
                }

                if (!isConnected) {
                    _messages.value = listOf(
                        ChatMessage(
                            content = "AI chat requires a connection to the $providerName server. Please configure the server settings first.",
                            isFromUser = false
                        )
                    )
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error checking server connection", e)
                _isServerConnected.value = false
                _statusText.value = "Error checking server: ${e.message}"
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(content = text.trim(), isFromUser = true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true

            if (!_isServerConnected.value) {
                val aiMessage = ChatMessage(
                    content = "AI server is not connected. Please check the server settings.",
                    isFromUser = false
                )
                _messages.value = _messages.value + aiMessage
                _isLoading.value = false
                return@launch
            }

            val provider = providerConfig.getProvider()
            val providerName = if (provider == ProviderConfig.PROVIDER_LMSTUDIO) "LM Studio" else "Ollama"
            val aiMessage = ChatMessage(
                content = "AI chat is powered by $providerName! The server is connected and ready. Food identification is available through the photo scan feature.",
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
