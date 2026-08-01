package com.example

import com.example.data.AnalysisType
import com.example.data.contextengine.ContextEngine
import com.example.data.contextengine.GoogleMapsLocationContextService
import com.example.data.contextengine.LocationContextInput
import com.example.data.contextengine.WikipediaContextSource
import com.example.data.contextengine.WikivoyageContextSource
import com.example.domain.engine.AnalysisEngine
import com.example.domain.engine.AnalysisRegistry
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.EngineRunner
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.model.SourceType
import com.example.domain.repository.AnalysisRepository
import com.example.domain.usecase.AnalyzeContentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleMapsLocationContextServiceTest {

    @Test
    fun testFetchLocationContextFormattings() = runTest {
        val wikiFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "Berlin" }],
                    "pages": {
                      "1": { "title": "Berlin", "extract": "Berlin ist die Bundeshauptstadt Deutschlands." }
                    }
                  }
                }
            """.trimIndent()
        }

        val voyageFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "Berlin" }],
                    "pages": {
                      "2": { "title": "Berlin", "extract": "Reiseführer für Berlin: Brandenburger Tor, Alexanderplatz." }
                    }
                  }
                }
            """.trimIndent()
        }

        val wikiSource = WikipediaContextSource(networkFetcher = wikiFetcher)
        val voyageSource = WikivoyageContextSource(networkFetcher = voyageFetcher)
        val engine = ContextEngine(listOf(wikiSource, voyageSource))
        val service = GoogleMapsLocationContextService(engine)

        val input = LocationContextInput(placeName = "Berlin")
        val formatted = service.fetchLocationContext(input)

        assertTrue(formatted.contains("=== FAKTEN ==="))
        assertTrue(formatted.contains("Berlin ist die Bundeshauptstadt Deutschlands."))
        assertTrue(formatted.contains("=== REISEKONTEXT ==="))
        assertTrue(formatted.contains("Reiseführer für Berlin: Brandenburger Tor, Alexanderplatz."))
    }

    @Test
    fun testAnalyzeContentUseCaseIntegrationForLocationContext() = runTest {
        val mockRepo = object : AnalysisRepository {
            override suspend fun saveAnalysis(summary: DomainSummary) {}
            override suspend fun getAllAnalyses(): List<DomainSummary> = emptyList()
            override fun getAllAnalysesFlow(): Flow<List<DomainSummary>> = emptyFlow()
            override suspend fun getAnalysisById(id: String): DomainSummary? = null
            override suspend fun deleteAnalysis(id: String) {}
        }

        var capturedInput: CanonicalAnalysisInput? = null

        val mockEngine = object : AnalysisEngine {
            override val contract = EngineContract(
                functionId = "GOOGLE_MAPS_LOCATION_CONTEXT",
                version = "1.0.0",
                inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
                outputSchema = "DomainSummary",
                capabilities = EngineCapabilities(name = "Google Maps Location Context", supportsSearchGrounding = false, supportsDirectPdf = false),
                promptPath = "prompts/F_GOOGLE_MAPS_LOCATION_CONTEXT.md"
            )
            override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
                capturedInput = input
                return DomainSummary(
                    id = "test_1",
                    title = "Test Location",
                    originalUrl = input.rawText,
                    shortDescription = "Test",
                    keyTakeaways = emptyList(),
                    analysisId = input.analysisId
                )
            }
        }

        val mockRegistry = object : AnalysisRegistry {
            override fun getEngine(functionId: String): AnalysisEngine? = if (functionId == "GOOGLE_MAPS_LOCATION_CONTEXT") mockEngine else null
            override fun getFunctionIdForType(analysisType: AnalysisType): String = "GOOGLE_MAPS_LOCATION_CONTEXT"
        }

        val mockRunner = object : EngineRunner {
            override suspend fun runEngine(engine: AnalysisEngine, input: CanonicalAnalysisInput): DomainSummary {
                return engine.analyze(input)
            }
        }

        val wikiFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "Heidelberg" }],
                    "pages": { "10": { "title": "Heidelberg", "extract": "Heidelberg ist eine Stadt am Neckar." } }
                  }
                }
            """.trimIndent()
        }
        val voyageFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "Heidelberg" }],
                    "pages": { "20": { "title": "Heidelberg", "extract": "Sehenswürdigkeiten: Heidelberger Schloss." } }
                  }
                }
            """.trimIndent()
        }

        val customEngine = ContextEngine(listOf(WikipediaContextSource(networkFetcher = wikiFetcher), WikivoyageContextSource(networkFetcher = voyageFetcher)))
        val service = GoogleMapsLocationContextService(customEngine)

        val useCase = AnalyzeContentUseCase(
            repository = mockRepo,
            registry = mockRegistry,
            runner = mockRunner,
            locationContextService = service
        )

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "https://maps.google.com/?q=Heidelberg",
            enrichedText = "Places details info",
            metadata = mapOf("placeName" to "Heidelberg"),
            analysisId = "analysis_123",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT
        )

        val result = useCase.execute(input)

        assertEquals("analysis_123", result.analysisId)
        assertTrue(capturedInput != null)
        val enrichedText = capturedInput!!.enrichedText
        assertTrue(enrichedText.contains("Places details info"))
        assertTrue(enrichedText.contains("=== FAKTEN ==="))
        assertTrue(enrichedText.contains("Heidelberg ist eine Stadt am Neckar."))
        assertTrue(enrichedText.contains("=== REISEKONTEXT ==="))
        assertTrue(enrichedText.contains("Sehenswürdigkeiten: Heidelberger Schloss."))
    }

    @Test
    fun testAnalyzeContentUseCaseDoesNotAffectOtherAnalysisTypes() = runTest {
        val mockRepo = object : AnalysisRepository {
            override suspend fun saveAnalysis(summary: DomainSummary) {}
            override suspend fun getAllAnalyses(): List<DomainSummary> = emptyList()
            override fun getAllAnalysesFlow(): Flow<List<DomainSummary>> = emptyFlow()
            override suspend fun getAnalysisById(id: String): DomainSummary? = null
            override suspend fun deleteAnalysis(id: String) {}
        }

        var capturedInput: CanonicalAnalysisInput? = null

        val mockEngine = object : AnalysisEngine {
            override val contract = EngineContract(
                functionId = "GOOGLE_MAPS_ANALYZER",
                version = "1.0.0",
                inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
                outputSchema = "DomainSummary",
                capabilities = EngineCapabilities(name = "Google Maps Analyzer", supportsSearchGrounding = false, supportsDirectPdf = false),
                promptPath = "prompts/F_GOOGLE_MAPS_ANALYZER.md"
            )
            override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
                capturedInput = input
                return DomainSummary(
                    id = "test_2",
                    title = "Analyzer Location",
                    originalUrl = input.rawText,
                    shortDescription = "Test",
                    keyTakeaways = emptyList(),
                    analysisId = input.analysisId
                )
            }
        }

        val mockRegistry = object : AnalysisRegistry {
            override fun getEngine(functionId: String): AnalysisEngine? = mockEngine
            override fun getFunctionIdForType(analysisType: AnalysisType): String = "GOOGLE_MAPS_ANALYZER"
        }

        val mockRunner = object : EngineRunner {
            override suspend fun runEngine(engine: AnalysisEngine, input: CanonicalAnalysisInput): DomainSummary {
                return engine.analyze(input)
            }
        }

        val useCase = AnalyzeContentUseCase(
            repository = mockRepo,
            registry = mockRegistry,
            runner = mockRunner
        )

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "https://maps.google.com/?q=Hamburg",
            enrichedText = "Original Places Info Only",
            metadata = mapOf("placeName" to "Hamburg"),
            analysisId = "analysis_456",
            analysisType = AnalysisType.GOOGLE_MAPS_ANALYZER
        )

        useCase.execute(input)

        assertTrue(capturedInput != null)
        assertEquals("Original Places Info Only", capturedInput!!.enrichedText)
        assertFalse(capturedInput!!.enrichedText.contains("=== FAKTEN ==="))
    }

    @Test
    fun testHappyFrogShortLinkResolvesPlaceNameAndRejectsJavaScriptNotice() = runTest {
        var capturedPlaceName: String? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedPlaceName = input.placeName
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Location found for ${input.placeName}"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val jsNotice = "When you have eliminated the JavaScript, whatever remains must be an empty page. Enable JavaScript to see Google Maps."
        val input = LocationContextInput(
            placeName = jsNotice,
            rawUrl = "https://maps.app.goo.gl/Kwj5C5RbgNdf41zD6"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedPlaceName != null)
        assertTrue(capturedPlaceName != jsNotice)
        assertEquals("The Happy Frog", capturedPlaceName)
    }

    @Test
    fun testValidExistingPlaceNameNotOverwritten() = runTest {
        var capturedPlaceName: String? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedPlaceName = input.placeName
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Valid place"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "Wat Phra That Doi Suthep",
            rawUrl = "https://maps.app.goo.gl/Kwj5C5RbgNdf41zD6"
        )

        service.fetchLocationContextResults(input)

        assertEquals("Wat Phra That Doi Suthep", capturedPlaceName)
    }

    @Test
    fun testInvalidJavaScriptTextTriggersUrlResolution() = runTest {
        var capturedPlaceName: String? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedPlaceName = input.placeName
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Resolved place"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "Bitte aktivieren Sie JavaScript um Google Maps zu nutzen",
            rawUrl = "https://www.google.com/maps/place/Brandenburger+Tor/"
        )

        service.fetchLocationContextResults(input)

        assertEquals("Brandenburger Tor", capturedPlaceName)
    }

    @Test
    fun testUnresolvableUrlHandledGracefullyWithoutCrash() = runTest {
        var capturedPlaceName: String? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedPlaceName = input.placeName
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = input.placeName.isNotBlank(),
                    snippet = null
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "Google Maps",
            rawUrl = "https://invalid-non-maps-url.example.com/foo"
        )

        val results = service.fetchLocationContextResults(input)

        assertEquals("", capturedPlaceName)
        assertTrue(results.isNotEmpty())
        assertFalse(results[0].isSuccessful)
    }

    @Test
    fun testHappyFrogShortLinkWithGoogleMapsSuffixCleansTitleAndExecutesParser() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Found place"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "The Happy Frog - Google Maps",
            rawUrl = "https://maps.app.goo.gl/Kwj5C5RbgNdf41zD6"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedInput != null)
        assertEquals("The Happy Frog", capturedInput!!.placeName)
    }

    @Test
    fun testHappyFrogExpandedUrlExtractsCoordinatesAndPlaceName() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Found place"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "The Happy Frog - Google Maps",
            rawUrl = "https://www.google.com/maps/place/The+Happy+Frog/@-27.4682,153.0234,17z/"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedInput != null)
        assertEquals("The Happy Frog", capturedInput!!.placeName)
        assertEquals(-27.4682, capturedInput!!.latitude!!, 0.0001)
        assertEquals(153.0234, capturedInput!!.longitude!!, 0.0001)
    }

    @Test
    fun testValidExistingPlaceNameComplementedWithParserMetadata() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Complemented metadata"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "Wat Phra That Doi Suthep",
            rawUrl = "https://www.google.com/maps/place/Wat+Phra+That+Doi+Suthep/@18.80498,98.92158,17z/"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedInput != null)
        assertEquals("Wat Phra That Doi Suthep", capturedInput!!.placeName)
        assertEquals(18.80498, capturedInput!!.latitude!!, 0.0001)
        assertEquals(98.92158, capturedInput!!.longitude!!, 0.0001)
    }

    @Test
    fun testNonGoogleMapsUrlDoesNotInvokeParser() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Non-maps URL"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "Berlin",
            rawUrl = "https://example.com/berlin"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedInput != null)
        assertEquals("Berlin", capturedInput!!.placeName)
        assertEquals(null, capturedInput!!.latitude)
        assertEquals(null, capturedInput!!.longitude)
    }

    @Test
    fun testUnresolvableGoogleMapsUrlHandledGracefullyFallback() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Fallback"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val input = LocationContextInput(
            placeName = "The Happy Frog - Google Maps",
            rawUrl = "https://maps.google.com/invalid_path_unresolvable_xyz"
        )

        service.fetchLocationContextResults(input)

        assertTrue(capturedInput != null)
        assertEquals("The Happy Frog", capturedInput!!.placeName)
    }

    @Test
    fun testPlaceNameSuffixAndQuoteCleaning() = runTest {
        val testCases = listOf(
            "The Happy Frog - Google Maps" to "The Happy Frog",
            "The Happy Frog – Google Maps" to "The Happy Frog",
            "\"The Happy Frog\"" to "The Happy Frog",
            "“The Happy Frog”" to "The Happy Frog",
            "The Happy Frog | Google Maps" to "The Happy Frog"
        )

        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = input.placeName
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))

        for ((raw, expected) in testCases) {
            var capturedName: String? = null
            val customSource = object : com.example.data.contextengine.ContextSource {
                override val sourceName: String = "TEST"
                override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
                override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                    capturedName = input.placeName
                    return com.example.data.contextengine.ContextResult(
                        sourceName = sourceName,
                        sourceType = sourceType,
                        isSuccessful = true,
                        snippet = input.placeName
                    )
                }
            }
            val customService = GoogleMapsLocationContextService(ContextEngine(listOf(customSource)))
            customService.fetchLocationContextResults(LocationContextInput(placeName = raw, rawUrl = "https://example.com"))
            assertEquals(expected, capturedName)
        }
    }

    @Test
    fun testRealisticWebInputExtractorIntegrationFlow() = runTest {
        var capturedInput: LocationContextInput? = null
        val trackingSource = object : com.example.data.contextengine.ContextSource {
            override val sourceName: String = "TEST"
            override val sourceType = com.example.data.contextengine.ContextSourceType.OFFICIAL_DATA
            override suspend fun fetchContext(input: LocationContextInput): com.example.data.contextengine.ContextResult {
                capturedInput = input
                return com.example.data.contextengine.ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Integration test"
                )
            }
        }

        val service = GoogleMapsLocationContextService(ContextEngine(listOf(trackingSource)))
        val extractorSimulatedInput = LocationContextInput(
            placeName = "The Happy Frog - Google Maps",
            description = "Find local businesses, view maps and get driving directions in Google Maps.",
            rawUrl = "https://www.google.com/maps/place/The+Happy+Frog/@-27.4682,153.0234,17z/"
        )

        service.fetchLocationContextResults(extractorSimulatedInput)

        assertTrue(capturedInput != null)
        assertEquals("The Happy Frog", capturedInput!!.placeName)
        assertEquals(-27.4682, capturedInput!!.latitude!!, 0.0001)
        assertEquals(153.0234, capturedInput!!.longitude!!, 0.0001)
    }

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }
}
