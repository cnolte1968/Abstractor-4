package com.example.data.engine.location

import com.example.data.AnalysisType
import com.example.data.engine.BaseGeminiEngine
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.engine.location.LocationQuestionCoordinator
import com.example.domain.engine.location.LocationQuestionPlanner
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.repository.GeminiGateway

class LocationQuestionEngine(
    gateway: GeminiGateway,
    promptAssetLoader: PromptAssetLoader,
    val coordinator: LocationQuestionCoordinator = LocationQuestionCoordinator(),
    val planner: LocationQuestionPlanner = LocationQuestionPlanner,
    override val contract: EngineContract = EngineContract(
        functionId = "GOOGLE_MAPS_LOCATION_QUERY",
        version = "1.0.0",
        inputSchema = "CanonicalAnalysisInput(enrichedText!=null or freeQuery!=null)",
        outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
        capabilities = EngineCapabilities(
            name = "Google Maps Location Query",
            supportsSearchGrounding = true,
            supportsDirectPdf = false
        ),
        promptPath = "prompts/F_GOOGLE_MAPS_LOCATION_QA.md"
    )
) : BaseGeminiEngine(gateway, promptAssetLoader) {

    override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
        val userQuestion = input.freeQuery
            ?: input.metadata["freeQuery"]
            ?: input.rawText

        if (userQuestion.isBlank()) {
            throw IllegalArgumentException("User question (freeQuery) is required for GOOGLE_MAPS_LOCATION_QUERY")
        }

        val rawLocationInput = input.metadata["url"]
            ?: input.metadata["uri"]
            ?: input.metadata["locationName"]
            ?: input.rawText

        val plan = planner.planExecution(freeQuery = userQuestion, locationName = rawLocationInput)

        val aggregatedContext = coordinator.coordinate(
            rawLocationInput = rawLocationInput,
            userQuestion = userQuestion,
            existingPlan = plan
        )

        val updatedInput = input.copy(
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            enrichedText = aggregatedContext.formattedCombinedContext,
            useSearchGrounding = input.useSearchGrounding || aggregatedContext.requiresGrounding
        )

        return super.analyze(updatedInput)
    }
}
