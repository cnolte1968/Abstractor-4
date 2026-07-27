package com.example.data.repository

import android.content.Context
import com.example.data.AnalysisType
import com.example.data.WebpageExtractor
import com.example.data.YoutubeUrlDecoder
import com.example.data.extraction.InputExtractorRegistry
import com.example.domain.model.ContentExtractionResult
import com.example.domain.repository.ContentExtractionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentExtractionRepositoryImpl(
    private val context: Context,
    private val registry: InputExtractorRegistry = InputExtractorRegistry(context)
) : ContentExtractionRepository {

    override suspend fun extractContent(
        rawUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult = withContext(Dispatchers.IO) {
        try {
            com.example.data.PipelineReportStore.startStep("url_normalization", "URL Normalization", "Raw URL: $rawUrl")
            val isDirectContent = !directContent.isNullOrBlank()
            val url = if (isDirectContent && rawUrl.isBlank()) {
                com.example.data.PipelineReportStore.endStepPass("url_normalization", "Direct content, URL normalization skipped", decision = "Proceed with direct content")
                ""
            } else {
                // Pre-process & normalize URL input
                var extracted = (YoutubeUrlDecoder.extractUrl(rawUrl) ?: rawUrl).trim()
                if (rawUrl.trim().endsWith("/") && !extracted.endsWith("/")) {
                    extracted += "/"
                }
                val inputUrl = if (!extracted.startsWith("http://", ignoreCase = true) && !extracted.startsWith("https://", ignoreCase = true)) {
                    "https://$extracted"
                } else {
                    extracted
                }

                // Check for basic URL validity before redirect resolution
                if (!inputUrl.contains(".") || inputUrl.length < 5) {
                    if (isDirectContent) {
                        com.example.data.PipelineReportStore.endStepPass("url_normalization", "Invalid URL, but has direct content", decision = "Proceed with direct content")
                        ""
                    } else {
                        val failEx = java.io.IOException("INVALID_URL: $inputUrl")
                        com.example.data.PipelineReportStore.endStepFail("url_normalization", failEx)
                        return@withContext ContentExtractionResult.Failure(
                            ContentExtractionResult.Failure.ErrorType.INVALID_URL,
                            "Ungültige Webadresse eingegeben.",
                            "Bitte stelle sicher, dass du eine vollständige Adresse eingegeben hast, z. B. „spiegel.de“ oder einen Link aus deinem Browser."
                        )
                    }
                } else {
                    // Resolve redirects (like lnkd.in, fb.me, t.co) to find final canonical destination
                    val resolved = try {
                        WebpageExtractor.resolveUrl(inputUrl)
                    } catch (e: Exception) {
                        inputUrl
                    }
                    
                    com.example.data.PipelineReportStore.updateSection("url_normalization") { map ->
                        map["rawUrl"] = rawUrl
                        map["trimmedUrl"] = rawUrl.trim()
                        map["decodedUrl"] = extracted
                        map["normalizedSourceUrl"] = resolved
                        map["urlPassedToWebpageExtractor"] = resolved
                        try {
                            val uri = java.net.URI(resolved)
                            map["host"] = uri.host ?: ""
                            map["path"] = uri.path ?: ""
                            map["query"] = uri.query ?: ""
                            map["fragment"] = uri.fragment ?: ""
                            map["schemeAfter"] = uri.scheme ?: ""
                        } catch(e: Exception) {}
                    }
                    com.example.data.PipelineReportStore.endStepPass("url_normalization", "Normalized URL: $resolved", decision = "Proceed to extractor selection")
                    resolved
                }
            }

            // Retrieve extractor from registry
            com.example.data.PipelineReportStore.startStep("extractor_selection", "Extractor Selection", "Normalized URL: $url")
            val extractor = registry.getExtractor(rawUrl, url, directContent, analysisType)
            if (extractor != null) {
                com.example.data.PipelineReportStore.updateSection("extractor_selection") { map ->
                    map["inputType"] = if (isDirectContent) "DIRECT_TEXT" else "WEB_URL"
                    map["analysisType"] = analysisType.name
                    map["canonicalAnalysisType"] = analysisType.canonical().name
                    map["selectedExtractorName"] = extractor.javaClass.simpleName
                    map["selectedExtractorClass"] = extractor.javaClass.name
                    map["extractorMatchesExpected"] = true
                }
                com.example.data.PipelineReportStore.endStepPass("extractor_selection", "Selected extractor: ${extractor.javaClass.simpleName}", decision = "Execute extraction")
                android.util.Log.i("RUNTIME_SMOKE", "EXTRACTOR_SELECTED - Extractor: ${extractor.javaClass.simpleName}")
                return@withContext extractor.extract(
                    rawUrl = rawUrl,
                    normalizedUrl = url,
                    directContent = directContent,
                    analysisType = analysisType,
                    freeQuery = freeQuery,
                    analysisId = analysisId
                )
            }

            val unknownExtractorEx = java.lang.IllegalStateException("No extractor found for inputs")
            com.example.data.PipelineReportStore.endStepFail("extractor_selection", unknownExtractorEx)
            return@withContext ContentExtractionResult.Failure(
                ContentExtractionResult.Failure.ErrorType.INVALID_URL,
                "Unbekannter Eingabetyp",
                "Für die eingegebenen Daten konnte kein passender Extraktor gefunden werden."
            )

        } catch (e: Exception) {
            com.example.data.PipelineReportStore.endStepFail("extractor_selection", e)
            return@withContext ContentExtractionResult.Failure(
                ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                "Inhalt konnte nicht geladen werden",
                e.localizedMessage
            )
        }
    }
}
