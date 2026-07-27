package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiRepository : com.example.domain.repository.GeminiGateway {
    private const val TAG = "GeminiRepository"
    
    var staticContext: Context? = null

    fun getApiKey(): String {
        val envKey1 = System.getenv("GEMINI_API_KEY") ?: ""
        val envKey2 = System.getenv("Gemini_Relevantor") ?: ""
        val buildConfigKey1 = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        val buildConfigKey2 = try { com.example.BuildConfig.Gemini_Relevantor } catch (e: Throwable) { "" }
        
        val allKeys = listOf(envKey1, envKey2, buildConfigKey1, buildConfigKey2)
        val placeholders = setOf("", "MY_GEMINI_KEY", "MY_GEMINI_API_KEY", "YOUR_API_KEY_HERE")
        
        return allKeys.firstOrNull { it.isNotEmpty() && !placeholders.contains(it) } ?: ""
    }

    override suspend fun generateContent(
        model: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "API-Schlüssel (GEMINI_API_KEY) ist nicht konfiguriert oder enthält noch einen Platzhalter.\n\n" +
                "Bitte trage deinen echten Gemini API-Key im Secrets-Panel von Google AI Studio ein, damit die Analysen ausgeführt werden können."
            )
        }
        return withContext(Dispatchers.IO) {
            RetrofitClient.service.generateContent(model, apiKey, request)
        }
    }
}
