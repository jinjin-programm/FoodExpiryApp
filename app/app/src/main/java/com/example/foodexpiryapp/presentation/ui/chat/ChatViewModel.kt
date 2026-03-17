package com.example.foodexpiryapp.presentation.ui.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.presentation.ui.llm.LlamaBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
        private const val MMPROJ_FILE = "mmproj.gguf"
    }

    private val llamaBridge = LlamaBridge.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _statusText = MutableStateFlow("Initializing...")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    init {
        initializeModel()
    }

    private fun initializeModel() {
        viewModelScope.launch {
            _statusText.value = "Loading model..."
            
            try {
                val modelLoaded = withContext(Dispatchers.IO) {
                    val modelPath = File(context.filesDir, MODEL_DIR)
                    val modelFile = File(modelPath, MODEL_FILE)
                    val mmprojFile = File(modelPath, MMPROJ_FILE)
                    
                    Log.d(TAG, "Looking for model at: ${modelFile.absolutePath}")
                    
                    // Copy model from assets if needed
                    if (!modelFile.exists()) {
                        try {
                            context.assets.open("$MODEL_DIR/$MODEL_FILE").use { input ->
                                modelPath.mkdirs()
                                modelFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.i(TAG, "Model copied from assets")
                        } catch (e: Exception) {
                            Log.e(TAG, "Model not found in assets: ${e.message}")
                            return@withContext false
                        }
                    }
                    
                    // Copy mmproj from assets if needed
                    if (!mmprojFile.exists()) {
                        try {
                            context.assets.open("$MODEL_DIR/$MMPROJ_FILE").use { input ->
                                modelPath.mkdirs()
                                mmprojFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.i(TAG, "Mmproj copied from assets")
                        } catch (e: Exception) {
                            Log.w(TAG, "Mmproj not found in assets: ${e.message}")
                            // Continue without mmproj - text-only mode
                        }
                    }
                    
                    if (modelFile.exists()) {
                        Log.i(TAG, "Loading model from: ${modelFile.absolutePath}")
                        val loaded = llamaBridge.loadModel(modelFile.absolutePath, 4096, 4)
                        Log.i(TAG, "Model load result: $loaded")
                        
                        if (loaded && mmprojFile.exists()) {
                            Log.i(TAG, "Loading mmproj from: ${mmprojFile.absolutePath}")
                            val mmprojLoaded = llamaBridge.loadMmproj(mmprojFile.absolutePath)
                            Log.i(TAG, "Mmproj load result: $mmprojLoaded")
                        }
                        
                        loaded
                    } else {
                        Log.e(TAG, "Model file does not exist")
                        false
                    }
                }
                
                _isModelLoaded.value = modelLoaded
                
                val hasVision = llamaBridge.hasVisionSupport()
                _statusText.value = when {
                    !modelLoaded -> "Model not loaded"
                    hasVision -> "Qwen3-VL ready"
                    else -> "Qwen3.5 ready (text only)"
                }
                
                if (modelLoaded) {
                    val welcomeMsg = if (hasVision) {
                        "Hello! I'm Qwen3-VL, your AI assistant. I can see images and help identify food items. How can I help you today?"
                    } else {
                        "Hello! I'm Qwen3.5, your AI assistant. How can I help you today?"
                    }
                    _messages.value = listOf(
                        ChatMessage(
                            content = welcomeMsg,
                            isFromUser = false
                        )
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing model: ${e.message}", e)
                _isModelLoaded.value = false
                _statusText.value = "Error: ${e.message}"
            }
        }
    }

    fun checkModelStatus() {
        viewModelScope.launch {
            _isModelLoaded.value = llamaBridge.isLoaded()
            _statusText.value = if (_isModelLoaded.value) "Model ready" else "Model not loaded"
            Log.d(TAG, "Model loaded: ${_isModelLoaded.value}")
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            content = text.trim(),
            isFromUser = true
        )
        _messages.value = _messages.value + userMessage

        generateResponse(text.trim())
    }

    private fun generateResponse(prompt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusText.value = "Generating..."

            try {
                val response = withContext(Dispatchers.IO) {
                    llamaBridge.generate(prompt)
                }

                Log.d(TAG, "LLM response: $response")

                val aiMessage = ChatMessage(
                    content = response,
                    isFromUser = false
                )
                _messages.value = _messages.value + aiMessage
                _statusText.value = "Ready"

            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                val errorMessage = ChatMessage(
                    content = "Error: ${e.message}",
                    isFromUser = false
                )
                _messages.value = _messages.value + errorMessage
                _statusText.value = "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}