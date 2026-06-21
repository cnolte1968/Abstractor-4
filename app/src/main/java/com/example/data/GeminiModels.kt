package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

object GeminiModelConfig {
    // Phase 6: Central model constants for Gemini text generation
    // gemini-1.5-flash is now unsupported (throws 404 NOT_FOUND).
    // gemini-3.5-flash is brand new and currently overloaded (throws 503 SERVICE_UNAVAILABLE / high demand).
    // gemini-2.5-flash is the modern, highly performant, and stable production-ready model.
    const val TEXT_MODEL = "gemini-2.5-flash"
    
    // Fallback model used if the primary model fails or experiences transient issues
    const val FALLBACK_MODEL = "gemini-3.5-flash"
    
    // Phase 3 Build diagnostic constant to prove current build execution
    const val ABSTRACTOR_BUILD_DIAGNOSTIC = "model-check-2026-06-11"
}

enum class AnalysisType {
    STANDARD_WEBSEITE,
    MULTIMEDIA,
    DOKUMENTE,
    TOP_3_KERNAUSSAGEN,
    AKTUALITAETS_CHECK,
    FEHLINFORMATIONS_RADAR,
    RISIKO_ANALYSE,
    BUSINESS_INKUBATOR,
    FACTS_VS_OPINIONS_ANALYZER,
    PERSPECTIVES_AND_COUNTERPOSITIONS
}

@JsonClass(generateAdapter = false)
data class GenerateContentRequest(
    @param:Json(name = "contents") val contents: List<Content>,
    @param:Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @param:Json(name = "tools") val tools: List<Tool>? = null,
    @param:Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = false)
data class Content(
    @param:Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = false)
data class Part(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = false)
data class InlineData(
    @param:Json(name = "mimeType") val mimeType: String,
    @param:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = false)
data class Tool(
    @param:Json(name = "googleSearch") val googleSearch: Map<String, String>? = null
)

@JsonClass(generateAdapter = false)
data class GenerationConfig(
    @param:Json(name = "responseMimeType") val responseMimeType: String? = null,
    @param:Json(name = "responseSchema") val responseSchema: ResponseSchema? = null,
    @param:Json(name = "temperature") val temperature: Double? = null,
    @param:Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = false)
data class ResponseSchema(
    @param:Json(name = "type") val type: String = "OBJECT",
    @param:Json(name = "properties") val properties: Map<String, SchemaProperty>? = null,
    @param:Json(name = "required") val required: List<String>? = null
)

@JsonClass(generateAdapter = false)
data class SchemaProperty(
    @param:Json(name = "type") val type: String, // "STRING", "ARRAY"
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "items") val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = false)
data class GenerateContentResponse(
    @param:Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = false)
data class Candidate(
    @param:Json(name = "content") val content: Content? = null
)
