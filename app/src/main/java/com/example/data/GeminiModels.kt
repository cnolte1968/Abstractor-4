package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class Blob(
    @Json(name = "mime_type") val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    @Json(name = "inline_data") val inlineData: Blob? = null
)

@JsonClass(generateAdapter = true)
data class SafetyRating(
    val category: String? = null,
    val probability: String? = null,
    val blocked: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PromptFeedback(
    @Json(name = "block_reason") val blockReasonSnake: String? = null,
    val blockReason: String? = null,
    val safetyRatings: List<SafetyRating> = emptyList()
) {
    val resolvedBlockReason: String? get() = blockReason ?: blockReasonSnake
}

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part> = emptyList(),
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val description: String? = null,
    val items: SchemaProperty? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "response_mime_type") val responseMimeType: String? = null,
    @Json(name = "response_schema") val responseSchema: ResponseSchema? = null,
    val temperature: Double? = null,
    @Json(name = "max_output_tokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    @Json(name = "googleSearch") val googleSearch: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
    @Json(name = "system_instruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    @Json(name = "finish_reason") val finishReasonSnake: String? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating> = emptyList(),
    val index: Int? = null
) {
    val resolvedFinishReason: String? get() = finishReason ?: finishReasonSnake
}

@JsonClass(generateAdapter = true)
data class UsageMetadata(
    @Json(name = "promptTokenCount") val promptTokenCount: Int? = null,
    @Json(name = "candidatesTokenCount") val candidatesTokenCount: Int? = null,
    @Json(name = "totalTokenCount") val totalTokenCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
    val usageMetadata: UsageMetadata? = null
)

@JsonClass(generateAdapter = true)
data class RelevantorSummary(
    val title: String,
    @Json(name = "original_url") val originalUrl: String,
    @Json(name = "short_description") val shortDescription: String,
    @Json(name = "key_takeaways") val keyTakeaways: List<String>,
    val owner: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonTakeawayObject(
    val title: String?,
    val details: String?,
    @Json(name = "visual_metadata") val visualMetadataSnake: Map<String, Any>? = null,
    @Json(name = "visualMetadata") val visualMetadataCamel: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class JsonSummaryWithObjects(
    val title: String?,
    @Json(name = "original_url") val originalUrl: String?,
    @Json(name = "short_description") val shortDescription: String?,
    @Json(name = "key_takeaways") val keyTakeaways: List<JsonTakeawayObject>?,
    val owner: String? = null
)

@JsonClass(generateAdapter = true)
data class JsonSummaryWithStrings(
    val title: String?,
    @Json(name = "original_url") val originalUrl: String?,
    @Json(name = "short_description") val shortDescription: String?,
    @Json(name = "key_takeaways") val keyTakeaways: List<String>?,
    val owner: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}
