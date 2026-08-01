package com.example.data.engine.document

import com.example.data.AnalysisType
import com.example.data.FileProcessingHelper
import com.example.data.engine.BaseGeminiEngine
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.repository.GeminiGateway
import java.io.IOException

class DocumentAnalysisEngine(
    gateway: GeminiGateway,
    promptAssetLoader: PromptAssetLoader,
    override val contract: EngineContract = EngineContract(
        functionId = "DOCUMENT_SUMMARY",
        version = "1.0.0",
        inputSchema = "CanonicalAnalysisInput(rawBytes!=null || enrichedText!=null)",
        outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
        capabilities = EngineCapabilities(
            name = "Document Analysis",
            supportsSearchGrounding = false,
            supportsDirectPdf = true
        ),
        promptPath = "prompts/F_DOKUMENTE.md"
    )
) : BaseGeminiEngine(gateway, promptAssetLoader) {

    override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
        val targetAnalysisType = if (input.analysisType != AnalysisType.DOKUMENTE) {
            AnalysisType.DOKUMENTE
        } else {
            input.analysisType
        }

        val rawBytes = input.rawBytes
        val mimeType = input.mimeType ?: ""
        val fileName = input.metadata["fileName"] ?: ""

        val processedInput = if (rawBytes != null && rawBytes.isNotEmpty()) {
            val lower = fileName.lowercase()
            if (lower.endsWith(".pdf") || mimeType == "application/pdf") {
                if (rawBytes.size > 20 * 1024 * 1024) {
                    throw IOException("FILE_TOO_LARGE")
                }
                if (USE_DIRECT_PDF_PROCESSING) {
                    input.copy(
                        enrichedText = "[PDF Direct Processing Mode]",
                        analysisType = targetAnalysisType
                    )
                } else {
                    val extracted = FileProcessingHelper.extractTextFromPdf(rawBytes)
                    if (extracted.isNullOrBlank()) {
                        throw IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                    }
                    input.copy(
                        enrichedText = extracted,
                        rawText = extracted,
                        analysisType = targetAnalysisType
                    )
                }
            } else if (lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".pptx")) {
                val extracted = FileProcessingHelper.extractOfficeTextFromBytes(rawBytes)
                if (extracted.isNullOrBlank()) {
                    throw IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                }
                input.copy(
                    enrichedText = extracted,
                    rawText = extracted,
                    analysisType = targetAnalysisType
                )
            } else if (mimeType.startsWith("text/") || lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".html")) {
                val text = String(rawBytes, Charsets.UTF_8)
                if (text.isBlank()) {
                    throw IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                }
                input.copy(
                    enrichedText = text,
                    rawText = text,
                    analysisType = targetAnalysisType
                )
            } else {
                throw IOException("INSUFFICIENT_DOCUMENT_CONTENT")
            }
        } else {
            val enriched = input.enrichedText
            if (enriched.isNullOrBlank()) {
                throw IOException("INSUFFICIENT_DOCUMENT_CONTENT")
            }
            input.copy(analysisType = targetAnalysisType)
        }

        return super.analyze(processedInput)
    }

    companion object {
        var USE_DIRECT_PDF_PROCESSING = true
    }
}
