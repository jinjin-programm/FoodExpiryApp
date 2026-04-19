package com.example.foodexpiryapp.data.remote.ollama.dto

import com.google.gson.annotations.SerializedName

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val format: OllamaJsonSchema? = null,
    val options: OllamaOptions? = null,
    @SerializedName("keep_alive")
    val keepAlive: String? = null,
    val think: Boolean = false
)

data class OllamaMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

data class OllamaOptions(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val numPredict: Int? = null,
    val seed: Int? = null
)

data class OllamaJsonSchema(
    val type: String = "object",
    val properties: Map<String, OllamaSchemaProperty>,
    val required: List<String>? = null
)

data class OllamaSchemaProperty(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null
)

data class OllamaChatResponse(
    val model: String,
    @SerializedName("created_at")
    val createdAt: String,
    val message: OllamaResponseMessage?,
    val done: Boolean,
    @SerializedName("done_reason")
    val doneReason: String? = null,
    @SerializedName("total_duration")
    val totalDuration: Long? = null,
    @SerializedName("eval_count")
    val evalCount: Int? = null,
    @SerializedName("eval_duration")
    val evalDuration: Long? = null
)

data class OllamaResponseMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null,
    @SerializedName("tool_calls")
    val toolCalls: List<Any>? = null
)

data class OllamaVersionResponse(
    val version: String? = null
)

data class OllamaModelInfo(
    val name: String,
    val modifiedAt: String? = null,
    val size: Long? = null,
    val digest: String? = null
)

data class OllamaModelsResponse(
    val models: List<OllamaModelInfo>? = null
)