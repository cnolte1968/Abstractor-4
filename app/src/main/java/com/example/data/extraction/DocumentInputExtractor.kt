package com.example.data.extraction

import android.content.Context
import com.example.data.AnalysisType
import com.example.data.FileProcessingHelper
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

class DocumentInputExtractor(private val context: Context) : InputExtractor {

    override fun supports(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): Boolean {
        if (!directContent.isNullOrBlank()) return true
        if (analysisType.canonical() == AnalysisType.DOCUMENT_SUMMARY) return true
        
        val lowerUrl = normalizedUrl.lowercase()
        return lowerUrl.startsWith("content://") ||
                lowerUrl.startsWith("file://") ||
                lowerUrl.endsWith(".pdf") ||
                lowerUrl.endsWith(".docx") ||
                lowerUrl.endsWith(".xlsx")
    }

    override suspend fun extract(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult {
        val canonicalType = analysisType.canonical()
        // If we have direct text content (from paste/text input)
        if (!directContent.isNullOrBlank()) {
            if (canonicalType == AnalysisType.KEY_TAKEAWAYS && !hasEnoughRealContent(directContent)) {
                return ContentExtractionResult.Predefined(
                    DomainSummary(
                        id = UUID.randomUUID().toString(),
                        title = "Inhalt nicht auslesbar",
                        originalUrl = normalizedUrl,
                        shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                        keyTakeaways = listOf(
                            TakeawayItem(title = "Seiteninhalt benötigt", details = "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript."),
                            TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden."),
                            TakeawayItem(title = "Alternative", details = "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App.")
                        ),
                        analysisId = analysisId
                    )
                )
            } else if (canonicalType == AnalysisType.FACTS_VS_OPINIONS && !hasEnoughRealContent(directContent)) {
                return ContentExtractionResult.Predefined(
                    DomainSummary(
                        id = UUID.randomUUID().toString(),
                        title = "Inhalt nicht auswertbar",
                        originalUrl = normalizedUrl,
                        shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                        keyTakeaways = listOf(
                            TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                            TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                            TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                        ),
                        analysisId = analysisId
                    )
                )
            } else if (canonicalType == AnalysisType.PERSPECTIVES_COUNTERPOSITIONS && !hasEnoughRealContent(directContent)) {
                return ContentExtractionResult.Predefined(
                    DomainSummary(
                        id = UUID.randomUUID().toString(),
                        title = "Inhalt nicht auswertbar",
                        originalUrl = normalizedUrl,
                        shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                        keyTakeaways = listOf(
                            TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                            TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                            TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                        ),
                        analysisId = analysisId
                    )
                )
            } else if (canonicalType == AnalysisType.FREE_SOURCE_QUERY && !hasEnoughRealContent(directContent)) {
                return ContentExtractionResult.Predefined(
                    DomainSummary(
                        id = UUID.randomUUID().toString(),
                        title = "Quelle nicht auslesbar",
                        originalUrl = normalizedUrl,
                        shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden.",
                        keyTakeaways = listOf(
                            TakeawayItem(title = "Inhalt benötigt", details = "Um deine Frage zur Quelle beantworten zu können, muss der Text ausgelesen werden können."),
                            TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus der URL alleine können keine präzisen Antworten ermittelt werden."),
                            TakeawayItem(title = "Alternative", details = "Bitte kopiere den Text manuell in das Textfeld, um die Analyse durchzuführen.")
                        ),
                        analysisId = analysisId
                    )
                )
            }

            return ContentExtractionResult.Success(
                ExtractedContent(
                    sourceType = SourceType.DOCUMENT,
                    rawText = directContent,
                    enrichedText = directContent,
                    metadata = mapOf("url" to normalizedUrl),
                    useSearchGrounding = false,
                    confirmedProfile = SourceProfile(
                        sourceType = SourceProfile.SourceType.RAW_TEXT,
                        platform = SourcePlatform.LOCAL_FILE,
                        rawInput = directContent,
                        capabilities = mapOf(
                            SourceCapability.RAW_TEXT to CapabilityState(
                                capability = SourceCapability.RAW_TEXT,
                                status = CapabilityStatus.AVAILABLE
                            )
                        ),
                        isPostFetchConfirmed = true
                    )
                )
            )
        }

        // If it is a file URI (e.g. content:// or file://)
        val isUri = normalizedUrl.startsWith("content://") || normalizedUrl.startsWith("file://")
        if (isUri) {
            try {
                val uri = android.net.Uri.parse(normalizedUrl)
                val contentResolver = context.contentResolver
                val mimeType = FileProcessingHelper.getMimeType(contentResolver, uri) ?: ""
                val fileName = uri.lastPathSegment ?: "document"
                val extractedText = FileProcessingHelper.extractTextFromUri(contentResolver, uri, mimeType, fileName)
                if (!extractedText.isNullOrBlank()) {
                    return ContentExtractionResult.Success(
                        ExtractedContent(
                            sourceType = SourceType.DOCUMENT,
                            rawText = extractedText,
                            enrichedText = extractedText,
                            metadata = mapOf("url" to normalizedUrl, "fileName" to fileName),
                            useSearchGrounding = false,
                            confirmedProfile = SourceProfile(
                                sourceType = SourceProfile.SourceType.DOCUMENT,
                                platform = SourcePlatform.LOCAL_FILE,
                                rawInput = normalizedUrl,
                                capabilities = mapOf(
                                    SourceCapability.DOCUMENT_TEXT to CapabilityState(
                                        capability = SourceCapability.DOCUMENT_TEXT,
                                        status = CapabilityStatus.AVAILABLE
                                    )
                                ),
                                isPostFetchConfirmed = true
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                return ContentExtractionResult.Failure(
                    ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR,
                    "Datei konnte nicht ausgelesen werden",
                    e.localizedMessage
                )
            }
        }

        return ContentExtractionResult.Failure(
            ContentExtractionResult.Failure.ErrorType.INSUFFICIENT_CONTENT,
            "Kein Inhalt vorhanden",
            "Das Dokument enthält keinen lesbaren Text."
        )
    }

    private fun hasEnoughRealContent(content: String?): Boolean {
        return !content.isNullOrBlank() && content.trim().length >= 500
    }
}
