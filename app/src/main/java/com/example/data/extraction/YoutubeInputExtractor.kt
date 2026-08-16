package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.data.YoutubeTranscriptHelper
import com.example.data.YoutubeUrlDecoder
import com.example.data.PipelineReportStore
import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.ExtractedContent
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import com.example.domain.model.SourceType
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import java.util.UUID

class YoutubeInputExtractor : InputExtractor {

    override fun supports(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): Boolean {
        return YoutubeUrlDecoder.isYoutubeUrl(normalizedUrl)
    }

    override suspend fun extract(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult {
        val videoId = YoutubeUrlDecoder.extractYoutubeVideoId(normalizedUrl)
        if (videoId == null) {
            return ContentExtractionResult.Failure(
                ContentExtractionResult.Failure.ErrorType.INVALID_URL,
                "Ungültige YouTube Video-ID",
                "Details: Eine gültige 11-stellige YouTube Video-ID konnte nicht aus dem Link extrahiert werden. Bitte prüfe das Format der URL."
            )
        }

        com.example.data.GatewayDiagnostics.reset()
        com.example.data.GatewayDiagnostics.sourceUrl = normalizedUrl
        try {
            com.example.data.WebpageExtractor.populateDiagnosticsBeforeRequest("https://www.youtube.com/watch?v=$videoId")
        } catch (e: Exception) {}

        // Start steps in PipelineReportStore to ensure consistent diagnostics
        PipelineReportStore.startStep("source_http_fetch", "Source HTTP Fetch", "YouTube Video ID: $videoId")
        PipelineReportStore.startStep("html_extraction", "HTML Extraction", "YouTube Watch Page / Player API")
        PipelineReportStore.startStep("content_cleaning", "Content Cleaning", "Extracting segments from subtitle XML")

        val youtubeInput = try {
            val res = YoutubeTranscriptHelper.extractYoutubeContent(videoId, normalizedUrl)
            com.example.data.GatewayDiagnostics.sourceHttpStatus = 200
            com.example.data.GatewayDiagnostics.sourceConnectOutcome = "SUCCESS"
            com.example.data.GatewayDiagnostics.textBeforeCleaningLength = res.rawText.length
            com.example.data.GatewayDiagnostics.textAfterCleaningLength = res.rawText.length
            
            // End steps as PASS
            PipelineReportStore.endStepPass("source_http_fetch", "Fetched YouTube content successfully")
            PipelineReportStore.endStepPass("html_extraction", "Extracted caption tracks successfully")
            PipelineReportStore.endStepPass("content_cleaning", "Parsed XML subtitles: ${res.rawText.length} characters")
            
            res
        } catch (e: Exception) {
            com.example.data.GatewayDiagnostics.exceptionClass = e.javaClass.name
            com.example.data.GatewayDiagnostics.exceptionMessage = e.message ?: e.toString()
            com.example.data.GatewayDiagnostics.sourceConnectOutcome = "FAILED"
            
            // End steps as FAIL
            PipelineReportStore.endStepFail("source_http_fetch", e, "Failed fetching YouTube transcript")
            PipelineReportStore.endStepFail("html_extraction", e, "TRANSCRIPT_UNAVAILABLE: No transcript tracks found or fetched")
            PipelineReportStore.endStepFail("content_cleaning", e, "Failed cleaning subtitles")
            
            null
        }

        if (youtubeInput == null) {
            com.example.data.GatewayDiagnostics.exceptionMessage = "TRANSCRIPT_UNAVAILABLE: Keine Inhaltsgewinnung moeglich."
            return ContentExtractionResult.Predefined(
                DomainSummary(
                    id = UUID.randomUUID().toString(),
                    title = "Video nicht auslesbar",
                    originalUrl = normalizedUrl,
                    shortDescription = "TRANSCRIPT_UNAVAILABLE",
                    keyTakeaways = listOf(
                        TakeawayItem(title = "Kein Transkript vorhanden", details = "Für das Video stehen keine automatischen oder manuellen Untertitel zur Verfügung."),
                        TakeawayItem(title = "Keine erweiterte Inhaltsbeschreibung", details = "Auch die erweiterte Inhaltsgewinnung (Titel und ausführliche Beschreibung) lieferte keine Daten."),
                        TakeawayItem(title = "Alternative", details = "Bitte versuche eine andere Video-URL oder kopiere den relevanten Text manuell in die App.")
                    ),
                    analysisId = analysisId
                )
            )
        }

        val isExtended = youtubeInput.structuredExtras["extended"] == "true" || com.example.data.GatewayDiagnostics.ytFinalTranscriptLength == 0
        if (isExtended) {
            com.example.data.GatewayDiagnostics.ytMetadataOnly = true
        }

        val confirmedProfile = if (isExtended) {
            SourceProfile(
                sourceType = SourceProfile.SourceType.VIDEO,
                platform = SourcePlatform.YOUTUBE,
                rawInput = rawUrl,
                normalizedUrl = normalizedUrl,
                capabilities = mapOf(
                    SourceCapability.VIDEO_METADATA to CapabilityState(
                        capability = SourceCapability.VIDEO_METADATA,
                        status = CapabilityStatus.AVAILABLE
                    ),
                    SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                        capability = SourceCapability.TRANSCRIPT_TEXT,
                        status = CapabilityStatus.FAILED,
                        detailMessage = "Transkript für dieses Video nicht verfügbar"
                    )
                ),
                isPostFetchConfirmed = true
            )
        } else {
            SourceProfile(
                sourceType = SourceProfile.SourceType.VIDEO,
                platform = SourcePlatform.YOUTUBE,
                rawInput = rawUrl,
                normalizedUrl = normalizedUrl,
                capabilities = mapOf(
                    SourceCapability.VIDEO_METADATA to CapabilityState(
                        capability = SourceCapability.VIDEO_METADATA,
                        status = CapabilityStatus.AVAILABLE
                    ),
                    SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                        capability = SourceCapability.TRANSCRIPT_TEXT,
                        status = CapabilityStatus.AVAILABLE
                    )
                ),
                isPostFetchConfirmed = true
            )
        }

        val extractedContent = ExtractedContent(
            sourceType = SourceType.YOUTUBE,
            rawText = youtubeInput.rawText,
            enrichedText = youtubeInput.enrichedText,
            metadata = youtubeInput.metadata,
            useSearchGrounding = false,
            confirmedProfile = confirmedProfile
        )

        return if (isExtended) {
            ContentExtractionResult.Degraded(extractedContent)
        } else {
            ContentExtractionResult.Success(extractedContent)
        }
    }
}
