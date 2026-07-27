package com.example.data.engine.top3

import com.example.data.AnalysisType
import com.example.data.engine.BaseGeminiEngine
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.repository.GeminiGateway

class Top3KeyPointsEngine(
    gateway: GeminiGateway,
    promptAssetLoader: PromptAssetLoader,
    override val contract: EngineContract = EngineContract(
        functionId = "KEY_TAKEAWAYS",
        version = "1.0.0",
        inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
        outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
        capabilities = EngineCapabilities(
            name = "Top 3 Key Points",
            supportsSearchGrounding = false,
            supportsDirectPdf = false
        ),
        promptPath = "prompts/F_TOP_3_KERNAUSSAGEN.md"
    )
) : BaseGeminiEngine(gateway, promptAssetLoader) {

    override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
        val top3Input = if (input.analysisType != AnalysisType.TOP_3_KERNAUSSAGEN && input.analysisType != AnalysisType.KEY_TAKEAWAYS) {
            input.copy(analysisType = AnalysisType.KEY_TAKEAWAYS)
        } else {
            input
        }
        return super.analyze(top3Input)
    }
}
