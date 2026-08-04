package com.example

import com.example.data.AnalysisType
import com.example.data.Candidate
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.GenerateContentResponse
import com.example.data.Part
import com.example.data.engine.location.LocationQuestionEngine
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.engine.location.ExecutionPlan
import com.example.domain.engine.location.LocationQuestionAggregatedContext
import com.example.domain.engine.location.LocationQuestionCoordinator
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import com.example.domain.repository.GeminiGateway
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocationQuestionEngineTest {

    private class FakeGateway(private val responseJson: String) : GeminiGateway {
        var lastRequest: GenerateContentRequest? = null
        override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
            lastRequest = request
            return GenerateContentResponse(
                candidates = listOf(
                    Candidate(
                        content = Content(parts = listOf(Part(text = responseJson))),
                        finishReason = "STOP"
                    )
                )
            )
        }
    }

    private class FakePromptLoader : PromptAssetLoader {
        override fun loadAsset(path: String): String {
            return "System instruction for $path"
        }
    }

    private val sampleValidJsonResponse = """
        {
          "title": "Kölner Dom",
          "original_url": "https://maps.app.goo.gl/example",
          "short_description": "Der Kölner Dom ist eine Kathedrale in Köln.",
          "key_takeaways": [
            {
              "title": "Baugeschichte",
              "details": "Der Bau begann 1248 und wurde 1880 vollendet."
            }
          ],
          "owner": null
        }
    """.trimIndent()

    @Test
    fun test1_EngineInstantiationAndContract() {
        val gateway = FakeGateway(sampleValidJsonResponse)
        val promptLoader = FakePromptLoader()
        val engine = LocationQuestionEngine(gateway, promptLoader)

        assertNotNull(engine)
        assertEquals("GOOGLE_MAPS_LOCATION_QUERY", engine.contract.functionId)
        assertTrue(engine.contract.capabilities.supportsSearchGrounding)
    }

    @Test
    fun test2_PlannerAndCoordinatorCalled_ReturnsValidDomainSummary() = runBlocking {
        var coordinatorCalled = false
        val customCoordinator = object : LocationQuestionCoordinator() {
            override suspend fun coordinate(
                rawLocationInput: String,
                userQuestion: String,
                existingPlan: ExecutionPlan?,
                preParsedPlacesResult: com.example.data.GooglePlacesPoCResult?
            ): LocationQuestionAggregatedContext {
                coordinatorCalled = true
                return LocationQuestionAggregatedContext(
                    userQuestion = userQuestion,
                    locationName = "Kölner Dom",
                    executionPlan = existingPlan,
                    formattedCombinedContext = "=== COMBINED CONTEXT FOR KÖLNER DOM ==="
                )
            }
        }

        val gateway = FakeGateway(sampleValidJsonResponse)
        val promptLoader = FakePromptLoader()
        val engine = LocationQuestionEngine(
            gateway = gateway,
            promptAssetLoader = promptLoader,
            coordinator = customCoordinator
        )

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Wann wurde der Dom gebaut?",
            enrichedText = "",
            analysisId = "test-123",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            freeQuery = "Wann wurde der Dom gebaut?",
            metadata = mapOf("url" to "https://maps.app.goo.gl/example")
        )

        val summary = engine.analyze(input)

        assertTrue(coordinatorCalled)
        assertNotNull(summary)
        assertEquals("Kölner Dom", summary.title)
        assertEquals(1, summary.keyTakeaways.size)
        assertEquals("Baugeschichte", summary.keyTakeaways.first().title)
    }

    @Test
    fun test3_InvalidInput_BlankFreeQuery_ThrowsException() = runBlocking {
        val gateway = FakeGateway(sampleValidJsonResponse)
        val promptLoader = FakePromptLoader()
        val engine = LocationQuestionEngine(gateway, promptLoader)

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "",
            enrichedText = "",
            analysisId = "test-invalid",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            freeQuery = "   "
        )

        var exceptionThrown = false
        try {
            engine.analyze(input)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)
    }

    @Test
    fun test4_CoordinatorError_PropagatesOrHandlesGracefully() = runBlocking {
        val failingCoordinator = object : LocationQuestionCoordinator() {
            override suspend fun coordinate(
                rawLocationInput: String,
                userQuestion: String,
                existingPlan: ExecutionPlan?,
                preParsedPlacesResult: com.example.data.GooglePlacesPoCResult?
            ): LocationQuestionAggregatedContext {
                throw RuntimeException("Coordinator Service Timeout")
            }
        }

        val gateway = FakeGateway(sampleValidJsonResponse)
        val promptLoader = FakePromptLoader()
        val engine = LocationQuestionEngine(
            gateway = gateway,
            promptAssetLoader = promptLoader,
            coordinator = failingCoordinator
        )

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Location",
            enrichedText = "",
            analysisId = "test-err",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            freeQuery = "Ist hier barrierefrei?"
        )

        var caught = false
        try {
            engine.analyze(input)
        } catch (e: Exception) {
            caught = true
            assertEquals("Coordinator Service Timeout", e.message)
        }
        assertTrue(caught)
    }

    @Test
    fun test5_DomainSummaryMappingValidation() = runBlocking {
        val gateway = FakeGateway(sampleValidJsonResponse)
        val promptLoader = FakePromptLoader()
        val engine = LocationQuestionEngine(gateway, promptLoader)

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Kölner Dom",
            enrichedText = "Location text",
            analysisId = "test-mapping",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            freeQuery = "Wann ist hier am meisten los?"
        )

        val summary = engine.analyze(input)

        assertNotNull(summary)
        assertEquals("https://maps.app.goo.gl/example", summary.originalUrl)
        assertEquals("Der Kölner Dom ist eine Kathedrale in Köln.", summary.shortDescription)
    }
}
