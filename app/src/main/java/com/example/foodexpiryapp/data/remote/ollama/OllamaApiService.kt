package com.example.foodexpiryapp.data.remote.ollama

import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaChatRequest
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaChatResponse
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaVersionResponse
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaModelsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OllamaApiService {
    
    @POST("/api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest,
        @Header("Authorization") authorization: String? = null
    ): Response<OllamaChatResponse>
    
    @GET("/api/version")
    suspend fun getVersion(
        @Header("Authorization") authorization: String? = null
    ): Response<OllamaVersionResponse>
    
    @GET("/api/tags")
    suspend fun getModels(
        @Header("Authorization") authorization: String? = null
    ): Response<OllamaModelsResponse>
}