package com.example.foodexpiryapp.data.remote.lmstudio.dto

import com.google.gson.annotations.SerializedName

data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

data class OpenAiMessage(
    val role: String,
    val content: Any
)

data class OpenAiTextContent(
    val type: String = "text",
    val text: String
)

data class OpenAiImageContent(
    val type: String = "image_url",
    val imageUrl: OpenAiImageUrl
)

data class OpenAiImageUrl(
    val url: String
)

data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val usage: OpenAiUsage? = null
)

data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiResponseMessage? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class OpenAiResponseMessage(
    val role: String? = null,
    val content: String? = null
)

data class OpenAiUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerializedName("completion_tokens")
    val completionTokens: Int? = null,
    @SerializedName("total_tokens")
    val totalTokens: Int? = null
)

data class OpenAiModelsResponse(
    val data: List<OpenAiModel>? = null
)

data class OpenAiModel(
    val id: String? = null
)
