package com.example.domain.repository

import com.example.data.GenerateContentRequest
import com.example.data.GenerateContentResponse

interface GeminiGateway {
    suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse
}
