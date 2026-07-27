package com.example.data.engine

import android.content.Context
import com.example.data.AnalysisType
import com.example.data.engine.document.DocumentAnalysisEngine
import com.example.data.engine.top3.Top3KeyPointsEngine
import com.example.data.engine.web.WebpageAnalysisEngine
import com.example.domain.engine.*
import com.example.domain.engine.validators.A1ContractValidator
import com.example.domain.engine.validators.A2ContractValidator
import com.example.domain.repository.GeminiGateway

class AnalysisRegistryImpl(
    private val gateway: GeminiGateway,
    private val context: Context,
    private val promptAssetLoader: PromptAssetLoader = AndroidAssetPromptLoader(context)
) : AnalysisRegistry {

    private val enginesMap = mutableMapOf<String, AnalysisEngine>()

    init {
        // 1. Document Engine (Dokumente)
        registerEngine(DocumentAnalysisEngine(gateway, promptAssetLoader))

        // 2. Top3 Engine (Kernaussagen)
        val a2Contract = EngineContract(
            functionId = "KEY_TAKEAWAYS",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = EngineCapabilities(
                name = "Top 3 Key Points",
                supportsSearchGrounding = false,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/F_TOP_3_KERNAUSSAGEN.md",
            customValidator = A2ContractValidator()
        )
        registerEngine(Top3KeyPointsEngine(gateway, promptAssetLoader, a2Contract))

        // 3. Web and custom prompt engines for other functionIds
        val webFunctions = listOf(
            Triple("WEB_SUMMARY", "Webpage Analysis", "prompts/F_STANDARD_WEBSEITE.md"),
            Triple("FREE_SOURCE_QUERY", "Free Source Request", "prompts/F_FREIE_QUELLENANFRAGE.md"),
            Triple("MULTIMEDIA_ANALYSIS", "Multimedia Analysis", "prompts/F_MULTIMEDIA.md"),
            Triple("FRESHNESS_CHECK", "Recency Check", "prompts/F_AKTUALITAETS_CHECK.md"),
            Triple("MISINFORMATION_RADAR", "Disinformation Radar", "prompts/F_FEHLINFORMATIONS_RADAR.md"),
            Triple("FACTS_VS_OPINIONS", "Facts vs Opinions", "prompts/F_FACTS_VS_OPINIONS_ANALYZER.md"),
            Triple("RISK_ANALYSIS", "Risk Analysis", "prompts/F_RISIKO_ANALYSE.md"),
            Triple("PERSPECTIVES_COUNTERPOSITIONS", "Perspectives and Counterpositions", "prompts/F_PERSPECTIVES_AND_COUNTERPOSITIONS.md"),
            Triple("RELEVANT_ASPECTS", "Weitere relevante Aspekte", "prompts/F_WEITERE_RELEVANTE_ASPEKTE.md"),
            Triple("BUSINESS_INKUBATOR", "Business Incubator", "prompts/F_BUSINESS_INKUBATOR.md"),
            Triple("GOOGLE_MAPS_ANALYZER", "Google Maps Analyser", "prompts/F_GOOGLE_MAPS_ANALYZER.md"),
            Triple("PHOTO_SCREENSHOT_ANALYSIS", "Photo & Screenshot Analysis", "prompts/F_PHOTO_SCREENSHOT_ANALYSIS.md")
        )

        for ((fid, name, path) in webFunctions) {
            val contract = EngineContract(
                functionId = fid,
                version = "1.0.0",
                inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
                outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
                capabilities = EngineCapabilities(
                    name = name,
                    supportsSearchGrounding = (fid != "MULTIMEDIA_ANALYSIS"),
                    supportsDirectPdf = false
                ),
                promptPath = path,
                customValidator = if (fid == "WEB_SUMMARY") A1ContractValidator() else null
            )
            registerEngine(WebpageAnalysisEngine(gateway, promptAssetLoader, contract))
        }
    }

    private fun registerEngine(engine: AnalysisEngine) {
        enginesMap[engine.contract.functionId] = engine
    }

    override fun getEngine(functionId: String): AnalysisEngine? {
        return enginesMap[functionId]
    }

    override fun getFunctionIdForType(analysisType: AnalysisType): String {
        return when (analysisType) {
            AnalysisType.STANDARD_WEBSEITE, AnalysisType.WEB_SUMMARY -> "WEB_SUMMARY"
            AnalysisType.TOP_3_KERNAUSSAGEN, AnalysisType.KEY_TAKEAWAYS -> "KEY_TAKEAWAYS"
            AnalysisType.FREIE_QUELLENANFRAGE, AnalysisType.FREE_SOURCE_QUERY -> "FREE_SOURCE_QUERY"
            AnalysisType.MULTIMEDIA, AnalysisType.MULTIMEDIA_ANALYSIS -> "MULTIMEDIA_ANALYSIS"
            AnalysisType.AKTUALITAETS_CHECK, AnalysisType.FRESHNESS_CHECK -> "FRESHNESS_CHECK"
            AnalysisType.FEHLINFORMATIONS_RADAR, AnalysisType.MISINFORMATION_RADAR -> "MISINFORMATION_RADAR"
            AnalysisType.FACTS_VS_OPINIONS_ANALYZER, AnalysisType.FACTS_VS_OPINIONS -> "FACTS_VS_OPINIONS"
            AnalysisType.RISIKO_ANALYSE, AnalysisType.RISK_ANALYSIS -> "RISK_ANALYSIS"
            AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS, AnalysisType.PERSPECTIVES_COUNTERPOSITIONS -> "PERSPECTIVES_COUNTERPOSITIONS"
            AnalysisType.BUSINESS_INKUBATOR -> "BUSINESS_INKUBATOR"
            AnalysisType.DOKUMENTE, AnalysisType.DOCUMENT_SUMMARY -> "DOCUMENT_SUMMARY"
            AnalysisType.WEITERE_RELEVANTE_ASPEKTE, AnalysisType.RELEVANT_ASPECTS -> "RELEVANT_ASPECTS"
            AnalysisType.GOOGLE_MAPS_ANALYZER -> "GOOGLE_MAPS_ANALYZER"
            AnalysisType.PHOTO_SCREENSHOT_ANALYSIS -> "PHOTO_SCREENSHOT_ANALYSIS"
        }
    }
}
