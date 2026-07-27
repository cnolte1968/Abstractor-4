package com.example.data.engine.web

import com.example.data.AnalysisType
import com.example.data.engine.BaseGeminiEngine
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.repository.GeminiGateway

class WebpageAnalysisEngine(
    gateway: GeminiGateway,
    promptAssetLoader: PromptAssetLoader,
    override val contract: EngineContract = EngineContract(
        functionId = "WEB_SUMMARY",
        version = "1.0.0",
        inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
        outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
        capabilities = EngineCapabilities(
            name = "Web and Prompt Analysis",
            supportsSearchGrounding = true,
            supportsDirectPdf = false
        ),
        promptPath = "prompts/F_STANDARD_WEBSEITE.md"
    )
) : BaseGeminiEngine(gateway, promptAssetLoader) {

    override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
        val webInput = if (input.analysisType == AnalysisType.DOKUMENTE || input.analysisType == AnalysisType.TOP_3_KERNAUSSAGEN || input.analysisType == AnalysisType.KEY_TAKEAWAYS || input.analysisType == AnalysisType.DOCUMENT_SUMMARY) {
            input.copy(analysisType = AnalysisType.WEB_SUMMARY)
        } else {
            input
        }
        return super.analyze(webInput)
    }
}
