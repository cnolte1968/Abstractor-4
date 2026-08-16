package com.example.data.extraction

import com.example.BuildConfig
import com.example.data.AnalysisType
import com.example.data.RetrofitClient
import com.example.data.YoutubeUrlDecoder
import com.example.data.network.ExtractVideoRequestDto
import com.example.data.network.SupabaseVideoApi
import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.ExtractedContent
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import com.example.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteVideoInputExtractor(
    private val api: SupabaseVideoApi = RetrofitClient.retrofit.create(SupabaseVideoApi::class.java),
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val apiKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY
) : InputExtractor {
    
    companion object {
        const val USE_REMOTE_VIDEO_EXTRACTOR = true
    }

    override fun supports(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): Boolean {
        if (!USE_REMOTE_VIDEO_EXTRACTOR) return false
        return YoutubeUrlDecoder.isYoutubeUrl(normalizedUrl)
    }

    override suspend fun extract(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult = withContext(Dispatchers.IO) {
        try {
            val url = if (baseUrl.endsWith("/")) {
                "${baseUrl}functions/v1/extract-video"
            } else {
                "$baseUrl/functions/v1/extract-video"
            }

            val request = ExtractVideoRequestDto(
                sourceUrl = normalizedUrl,
                sourcePlatform = "YOUTUBE",
                requestedCapabilities = listOf("TRANSCRIPT", "METADATA")
            )

            val response = api.extractVideo(
                url = url,
                authHeader = "Bearer $apiKey",
                apiKey = apiKey,
                request = request
            )

            if (!response.isSuccessful) {
                return@withContext ContentExtractionResult.Failure(
                    errorType = ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                    message = "Provider API HTTP Error: ${response.code()}"
                )
            }

            val body = response.body() ?: return@withContext ContentExtractionResult.Failure(
                errorType = ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                message = "Empty response body from Provider"
            )

            when (body.providerStatus) {
                "SUCCESS" -> {
                    val transcript = body.content?.transcript ?: ""
                    val metadataMap = mutableMapOf<String, String>()
                    body.metadata?.title?.let { metadataMap["title"] = it }
                    body.metadata?.author?.let { metadataMap["channel"] = it }
                    val desc = body.metadata?.description?.ifBlank { null }
                        ?: body.metadata?.shortDescription?.ifBlank { null }
                    desc?.let { metadataMap["description"] = it }
                    metadataMap["url"] = normalizedUrl

                    val extractedContent = ExtractedContent(
                        sourceType = SourceType.YOUTUBE,
                        rawText = transcript,
                        enrichedText = transcript,
                        metadata = metadataMap,
                        useSearchGrounding = false,
                        confirmedProfile = SourceProfile(
                            sourceType = SourceProfile.SourceType.VIDEO,
                            platform = SourcePlatform.YOUTUBE,
                            rawInput = rawUrl,
                            normalizedUrl = normalizedUrl,
                            capabilities = mapOf(
                                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(SourceCapability.TRANSCRIPT_TEXT, CapabilityStatus.AVAILABLE),
                                SourceCapability.VIDEO_METADATA to CapabilityState(SourceCapability.VIDEO_METADATA, CapabilityStatus.AVAILABLE)
                            ),
                            isPostFetchConfirmed = true
                        )
                    )
                    return@withContext ContentExtractionResult.Success(extractedContent)
                }
                "DEGRADED" -> {
                    val metadataMap = mutableMapOf<String, String>()
                    val title = body.metadata?.title
                    val author = body.metadata?.author
                    val desc = body.metadata?.description?.ifBlank { null }
                        ?: body.metadata?.shortDescription?.ifBlank { null }

                    title?.let { metadataMap["title"] = it }
                    author?.let { metadataMap["channel"] = it }
                    desc?.let { metadataMap["description"] = it }
                    metadataMap["url"] = normalizedUrl

                    val textBuilder = StringBuilder()
                    textBuilder.append("ERWEITERTE YOUTUBE-INHALTSGEWINNUNG (Kein Transkript vorhanden, Inhaltsanalyse basiert auf Video-Metadaten):\n")
                    if (!title.isNullOrBlank()) {
                        textBuilder.append("- Video-Titel: ").append(title).append("\n")
                    }
                    if (!author.isNullOrBlank()) {
                        textBuilder.append("- Kanal / Ersteller: ").append(author).append("\n")
                    }
                    if (!desc.isNullOrBlank()) {
                        textBuilder.append("\nVOLLSTÄNDIGE VIDEO-BESCHREIBUNG / KAPITEL / INHALT:\n")
                        textBuilder.append(desc).append("\n")
                    }
                    val generatedText = textBuilder.toString().trim()

                    val extractedContent = ExtractedContent(
                        sourceType = SourceType.YOUTUBE,
                        rawText = generatedText,
                        enrichedText = generatedText,
                        metadata = metadataMap,
                        useSearchGrounding = false,
                        confirmedProfile = SourceProfile(
                            sourceType = SourceProfile.SourceType.VIDEO,
                            platform = SourcePlatform.YOUTUBE,
                            rawInput = rawUrl,
                            normalizedUrl = normalizedUrl,
                            capabilities = mapOf(
                                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(SourceCapability.TRANSCRIPT_TEXT, CapabilityStatus.DEGRADED),
                                SourceCapability.VIDEO_METADATA to CapabilityState(SourceCapability.VIDEO_METADATA, CapabilityStatus.AVAILABLE)
                            ),
                            isPostFetchConfirmed = true
                        )
                    )
                    return@withContext ContentExtractionResult.Degraded(extractedContent)
                }
                "FAILED" -> {
                    val errorType = if (body.accessStatus == "BLOCKED" || body.accessStatus == "LOGIN_REQUIRED") {
                        ContentExtractionResult.Failure.ErrorType.BLOCKED_SOURCE
                    } else if (body.errorInformation?.code == "TRANSCRIPT_NOT_FOUND") {
                        ContentExtractionResult.Failure.ErrorType.TRANSCRIPT_UNAVAILABLE
                    } else {
                        ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR
                    }
                    
                    return@withContext ContentExtractionResult.Failure(
                        errorType = errorType,
                        message = body.errorInformation?.message ?: "Provider extraction failed"
                    )
                }
                else -> {
                    return@withContext ContentExtractionResult.Failure(
                        errorType = ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                        message = "Unknown provider status: ${body.providerStatus}"
                    )
                }
            }

        } catch (e: Exception) {
            return@withContext ContentExtractionResult.Failure(
                errorType = ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                message = e.message ?: "Network error"
            )
        }
    }
}