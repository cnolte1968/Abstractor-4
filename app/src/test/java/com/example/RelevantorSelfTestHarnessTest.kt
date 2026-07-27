package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.Candidate
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.GenerateContentResponse
import com.example.data.Part
import com.example.data.SummaryResponseParser
import com.example.data.engine.AnalysisRegistryImpl
import com.example.data.engine.web.WebpageAnalysisEngine
import com.example.data.engine.top3.Top3KeyPointsEngine
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.model.SourceType
import com.example.domain.repository.AnalysisRepository
import com.example.domain.repository.GeminiGateway
import com.example.ui.metadata.FeatureCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RelevantorSelfTestHarnessTest {

    private lateinit var context: Context
    private lateinit var fakeRepository: FakeAnalysisRepository
    private lateinit var fakeGateway: FakeGeminiGateway
    private lateinit var filePromptLoader: FileSystemPromptAssetLoader
    private lateinit var registry: AnalysisRegistryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeRepository = FakeAnalysisRepository()
        fakeGateway = FakeGeminiGateway()
        filePromptLoader = FileSystemPromptAssetLoader()
        registry = AnalysisRegistryImpl(fakeGateway, context, filePromptLoader)
    }

    // --- Utility Methods for Asset Loading ---

    private fun loadTestAsset(path: String): String {
        val paths = listOf(
            "src/test/assets/$path",
            "app/src/test/assets/$path",
            "../app/src/test/assets/$path",
            "applet/app/src/test/assets/$path"
        )
        for (p in paths) {
            val file = File(p)
            if (file.exists()) {
                return file.readText(Charsets.UTF_8)
            }
        }
        throw FileNotFoundException("Golden test asset not found in any standard path: $path")
    }

    private fun loadPromptAsset(path: String): String {
        val cleanPath = if (path.startsWith("prompts/")) path else "prompts/$path"
        val paths = listOf(
            "src/main/assets/$cleanPath",
            "app/src/main/assets/$cleanPath",
            "../app/src/main/assets/$cleanPath",
            "applet/app/src/main/assets/$cleanPath"
        )
        for (p in paths) {
            val file = File(p)
            if (file.exists()) {
                return file.readText(Charsets.UTF_8)
            }
        }
        throw FileNotFoundException("Prompt asset not found in any standard path: $cleanPath")
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // --- Fake Components ---

    class FakeAnalysisRepository : AnalysisRepository {
        private val savedSummaries = mutableListOf<DomainSummary>()

        override suspend fun saveAnalysis(summary: DomainSummary) {
            savedSummaries.add(summary)
        }

        override suspend fun getAllAnalyses(): List<DomainSummary> {
            return savedSummaries
        }

        override fun getAllAnalysesFlow(): Flow<List<DomainSummary>> {
            return flowOf(savedSummaries)
        }

        override suspend fun getAnalysisById(id: String): DomainSummary? {
            return savedSummaries.find { it.id == id }
        }

        override suspend fun deleteAnalysis(id: String) {
            savedSummaries.removeAll { it.id == id }
        }

        fun clear() {
            savedSummaries.clear()
        }
    }

    class FakeGeminiGateway : GeminiGateway {
        var rawResponseText: String = ""

        override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
            return GenerateContentResponse(
                candidates = listOf(
                    Candidate(
                        content = Content(
                            parts = listOf(
                                Part(text = rawResponseText)
                            )
                        )
                    )
                )
            )
        }
    }

    inner class FileSystemPromptAssetLoader : PromptAssetLoader {
        override fun loadAsset(path: String): String {
            return loadPromptAsset(path)
        }
    }

    // --- Core Self-Test Acceptance Test Cases ---

    /**
     * Test 1: selfTest_WEB_SUMMARY_standardWebseite_passesFullPipeline
     * Validates that standard webpage summaries pass through extraction, prompt loading, mock LLM generation, contract enforcement, and repository saving.
     */
    @Test
    fun selfTest_WEB_SUMMARY_standardWebseite_passesFullPipeline() = runBlocking {
        // Load A.1 Golden Artifacts
        val inputHtml = loadTestAsset("golden/WEB_SUMMARY/input.html")
        val inputUrl = loadTestAsset("golden/WEB_SUMMARY/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/WEB_SUMMARY/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/WEB_SUMMARY/expected_domain_summary.json")

        // 1. Simulate HTML / Text Extraction
        val textLength = inputHtml.length
        assertTrue("WEB_SUMMARY input.html must be loaded and non-empty", textLength > 0)

        // 2. Setup mock response
        fakeGateway.rawResponseText = geminiResponse

        // 3. Resolve from Registry
        val engine = registry.getEngine("WEB_SUMMARY")
        assertNotNull("Engine WEB_SUMMARY must be registered in AnalysisRegistry", engine)

        // 4. Validate Prompt Integrity
        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("WEB_SUMMARY system prompt must not be blank", promptText.isBlank())

        // 5. Construct input payload
        val analysisId = UUID.randomUUID().toString()
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = analysisId,
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )

        // 6. Execute pipeline
        val summary = engine.analyze(input)

        // 7. Verify contract
        engine.contract.validateOutput(summary)

        // 8. Save and verify storage
        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved in the history repository", saved)

        // 9. Output validations
        assertEquals("WEB_SUMMARY", engine.contract.functionId)
        assertEquals("Apple Vision Pro im Langzeittest", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        // Output detailed factual report (Teil E)
        printFactualReport(
            functionId = "WEB_SUMMARY",
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            inputFixture = "golden/WEB_SUMMARY/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/WEB_SUMMARY/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/WEB_SUMMARY/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test 2: selfTest_KEY_TAKEAWAYS_top3Kernaussagen_passesFullPipeline
     * Validates that TOP_3_KERNAUSSAGEN summaries pass successfully with at most 3 takeaways.
     */
    @Test
    fun selfTest_KEY_TAKEAWAYS_top3Kernaussagen_passesFullPipeline() = runBlocking {
        // Load A.2 Golden Artifacts
        val inputHtml = loadTestAsset("golden/KEY_TAKEAWAYS/input.html")
        val inputUrl = loadTestAsset("golden/KEY_TAKEAWAYS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/KEY_TAKEAWAYS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/KEY_TAKEAWAYS/expected_domain_summary.json")

        // 1. Simulate HTML / Text Extraction
        val textLength = inputHtml.length
        assertTrue("KEY_TAKEAWAYS input.html must be loaded and non-empty", textLength > 0)

        // 2. Setup mock response
        fakeGateway.rawResponseText = geminiResponse

        // 3. Resolve from Registry
        val engine = registry.getEngine("KEY_TAKEAWAYS")
        assertNotNull("Engine KEY_TAKEAWAYS must be registered in AnalysisRegistry", engine)

        // 4. Validate Prompt Integrity
        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("KEY_TAKEAWAYS system prompt must not be blank", promptText.isBlank())

        // 5. Construct input payload
        val analysisId = UUID.randomUUID().toString()
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = analysisId,
            analysisType = AnalysisType.TOP_3_KERNAUSSAGEN
        )

        // 6. Execute pipeline
        val summary = engine.analyze(input)

        // 7. Verify contract
        engine.contract.validateOutput(summary)

        // 8. Save and verify storage
        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved in the history repository", saved)

        // 9. Output validations
        assertEquals("KEY_TAKEAWAYS", engine.contract.functionId)
        assertEquals("Studie zu Homeoffice und Produktivität", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Takeaways count must be at most 3 for KEY_TAKEAWAYS", summary.keyTakeaways.size <= 3)

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        // Output detailed factual report (Teil E)
        printFactualReport(
            functionId = "KEY_TAKEAWAYS",
            analysisType = AnalysisType.TOP_3_KERNAUSSAGEN,
            inputFixture = "golden/KEY_TAKEAWAYS/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/KEY_TAKEAWAYS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/KEY_TAKEAWAYS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_FREE_SOURCE_QUERY_freieQuellenanfrage_passesFullPipeline
     */
    @Test
    fun selfTest_FREE_SOURCE_QUERY_freieQuellenanfrage_passesFullPipeline() = runBlocking {
        val inputText = loadTestAsset("golden/FREE_SOURCE_QUERY/input_text.txt")
        val inputUrl = loadTestAsset("golden/FREE_SOURCE_QUERY/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/FREE_SOURCE_QUERY/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/FREE_SOURCE_QUERY/expected_domain_summary.json")

        val textLength = inputText.length
        assertTrue("FREE_SOURCE_QUERY input_text.txt must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("FREE_SOURCE_QUERY")
        assertNotNull("Engine FREE_SOURCE_QUERY must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("FREE_SOURCE_QUERY system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputText,
            enrichedText = inputText,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.FREIE_QUELLENANFRAGE
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("FREE_SOURCE_QUERY", engine.contract.functionId)
        assertEquals("KI im Journalismus", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "FREE_SOURCE_QUERY",
            analysisType = AnalysisType.FREIE_QUELLENANFRAGE,
            inputFixture = "golden/FREE_SOURCE_QUERY/input_text.txt",
            inputHash = inputText.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/FREE_SOURCE_QUERY/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/FREE_SOURCE_QUERY/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_MULTIMEDIA_ANALYSIS_multimedia_passesFullPipeline
     */
    @Test
    fun selfTest_MULTIMEDIA_ANALYSIS_multimedia_passesFullPipeline() = runBlocking {
        val transcriptText = loadTestAsset("golden/MULTIMEDIA_ANALYSIS/transcript.txt")
        val inputUrl = loadTestAsset("golden/MULTIMEDIA_ANALYSIS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/MULTIMEDIA_ANALYSIS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/MULTIMEDIA_ANALYSIS/expected_domain_summary.json")

        val textLength = transcriptText.length
        assertTrue("A.4 transcript.txt must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("MULTIMEDIA_ANALYSIS")
        assertNotNull("Engine MULTIMEDIA_ANALYSIS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("MULTIMEDIA_ANALYSIS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = transcriptText,
            enrichedText = transcriptText,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.MULTIMEDIA
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        // Rule check: No hallucinated direct video seeing claims
        assertFalse("Must not claim video direct sight without context", 
            summary.shortDescription.contains("Ich habe dieses Video gesehen", ignoreCase = true) ||
            summary.shortDescription.contains("Direkt angesehen", ignoreCase = true)
        )

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("MULTIMEDIA_ANALYSIS", engine.contract.functionId)
        assertEquals("Podcast: Erneuerbare Energien & Solar", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "MULTIMEDIA_ANALYSIS",
            analysisType = AnalysisType.MULTIMEDIA,
            inputFixture = "golden/MULTIMEDIA_ANALYSIS/transcript.txt",
            inputHash = transcriptText.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/MULTIMEDIA_ANALYSIS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/MULTIMEDIA_ANALYSIS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_FRESHNESS_CHECK_aktualitaetsCheck_passesFullPipeline
     */
    @Test
    fun selfTest_FRESHNESS_CHECK_aktualitaetsCheck_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/FRESHNESS_CHECK/input.html")
        val inputUrl = loadTestAsset("golden/FRESHNESS_CHECK/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/FRESHNESS_CHECK/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/FRESHNESS_CHECK/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("FRESHNESS_CHECK input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("FRESHNESS_CHECK")
        assertNotNull("Engine FRESHNESS_CHECK must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("FRESHNESS_CHECK system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.AKTUALITAETS_CHECK
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("FRESHNESS_CHECK", engine.contract.functionId)
        assertEquals("Aktualität: Technologietrends 2026", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "FRESHNESS_CHECK",
            analysisType = AnalysisType.AKTUALITAETS_CHECK,
            inputFixture = "golden/FRESHNESS_CHECK/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/FRESHNESS_CHECK/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/FRESHNESS_CHECK/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_MISINFORMATION_RADAR_fehlinformationsRadar_passesFullPipeline
     */
    @Test
    fun selfTest_MISINFORMATION_RADAR_fehlinformationsRadar_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/MISINFORMATION_RADAR/input.html")
        val inputUrl = loadTestAsset("golden/MISINFORMATION_RADAR/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/MISINFORMATION_RADAR/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/MISINFORMATION_RADAR/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("MISINFORMATION_RADAR input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("MISINFORMATION_RADAR")
        assertNotNull("Engine MISINFORMATION_RADAR must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("MISINFORMATION_RADAR system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.FEHLINFORMATIONS_RADAR
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("MISINFORMATION_RADAR", engine.contract.functionId)
        assertEquals("Radar: Klimawandel-Behauptungen", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "MISINFORMATION_RADAR",
            analysisType = AnalysisType.FEHLINFORMATIONS_RADAR,
            inputFixture = "golden/MISINFORMATION_RADAR/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/MISINFORMATION_RADAR/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/MISINFORMATION_RADAR/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_FACTS_VS_OPINIONS_factsVsOpinions_passesFullPipeline
     */
    @Test
    fun selfTest_FACTS_VS_OPINIONS_factsVsOpinions_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/FACTS_VS_OPINIONS/input.html")
        val inputUrl = loadTestAsset("golden/FACTS_VS_OPINIONS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/FACTS_VS_OPINIONS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/FACTS_VS_OPINIONS/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("FACTS_VS_OPINIONS input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("FACTS_VS_OPINIONS")
        assertNotNull("Engine FACTS_VS_OPINIONS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("FACTS_VS_OPINIONS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.FACTS_VS_OPINIONS_ANALYZER
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("FACTS_VS_OPINIONS", engine.contract.functionId)
        assertEquals("Fakt/Meinung: Immobilienmarkt", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "FACTS_VS_OPINIONS",
            analysisType = AnalysisType.FACTS_VS_OPINIONS_ANALYZER,
            inputFixture = "golden/FACTS_VS_OPINIONS/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/FACTS_VS_OPINIONS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/FACTS_VS_OPINIONS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_RISK_ANALYSIS_risikoanalyse_passesFullPipeline
     */
    @Test
    fun selfTest_RISK_ANALYSIS_risikoanalyse_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/RISK_ANALYSIS/input.html")
        val inputUrl = loadTestAsset("golden/RISK_ANALYSIS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/RISK_ANALYSIS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/RISK_ANALYSIS/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("RISK_ANALYSIS input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("RISK_ANALYSIS")
        assertNotNull("Engine RISK_ANALYSIS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("RISK_ANALYSIS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.RISIKO_ANALYSE
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("RISK_ANALYSIS", engine.contract.functionId)
        assertEquals("Risikoanalyse: Krypto-Altersvorsorge", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())
        assertTrue("Must contain at least one high-risk takeaway", summary.keyTakeaways.any { it.visualMetadata["risk_level"] == "high" })

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "RISK_ANALYSIS",
            analysisType = AnalysisType.RISIKO_ANALYSE,
            inputFixture = "golden/RISK_ANALYSIS/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/RISK_ANALYSIS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/RISK_ANALYSIS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_PERSPECTIVES_COUNTERPOSITIONS_perspektiven_passesFullPipeline
     */
    @Test
    fun selfTest_PERSPECTIVES_COUNTERPOSITIONS_perspektiven_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/PERSPECTIVES_COUNTERPOSITIONS/input.html")
        val inputUrl = loadTestAsset("golden/PERSPECTIVES_COUNTERPOSITIONS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/PERSPECTIVES_COUNTERPOSITIONS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/PERSPECTIVES_COUNTERPOSITIONS/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("PERSPECTIVES_COUNTERPOSITIONS input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("PERSPECTIVES_COUNTERPOSITIONS")
        assertNotNull("Engine PERSPECTIVES_COUNTERPOSITIONS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("PERSPECTIVES_COUNTERPOSITIONS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("PERSPECTIVES_COUNTERPOSITIONS", engine.contract.functionId)
        assertEquals("Perspektiven: Tempolimit-Debatte", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "PERSPECTIVES_COUNTERPOSITIONS",
            analysisType = AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS,
            inputFixture = "golden/PERSPECTIVES_COUNTERPOSITIONS/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/PERSPECTIVES_COUNTERPOSITIONS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/PERSPECTIVES_COUNTERPOSITIONS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_RELEVANT_ASPECTS_weitereRelevanteAspekte_passesFullPipeline
     */
    @Test
    fun selfTest_RELEVANT_ASPECTS_weitereRelevanteAspekte_passesFullPipeline() = runBlocking {
        val inputHtml = loadTestAsset("golden/RELEVANT_ASPECTS/input.html")
        val inputUrl = loadTestAsset("golden/RELEVANT_ASPECTS/input_url.txt").trim()
        val geminiResponse = loadTestAsset("golden/RELEVANT_ASPECTS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/RELEVANT_ASPECTS/expected_domain_summary.json")

        val textLength = inputHtml.length
        assertTrue("RELEVANT_ASPECTS input.html must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("RELEVANT_ASPECTS")
        assertNotNull("Engine RELEVANT_ASPECTS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("RELEVANT_ASPECTS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = inputHtml,
            enrichedText = inputHtml,
            metadata = mapOf("url" to inputUrl),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.WEITERE_RELEVANTE_ASPEKTE
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("RELEVANT_ASPECTS", engine.contract.functionId)
        assertEquals("Aspekte: Homeoffice-Regelungen", summary.title)
        assertEquals(inputUrl, summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "RELEVANT_ASPECTS",
            analysisType = AnalysisType.WEITERE_RELEVANTE_ASPEKTE,
            inputFixture = "golden/RELEVANT_ASPECTS/input.html",
            inputHash = inputHtml.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/RELEVANT_ASPECTS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/RELEVANT_ASPECTS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test: selfTest_DOCUMENT_SUMMARY_passesFullPipeline
     */
    @Test
    fun selfTest_DOCUMENT_SUMMARY_passesFullPipeline() = runBlocking {
        val inputDocText = loadTestAsset("golden/DOCUMENT_SUMMARY/input_document.txt")
        val fileName = loadTestAsset("golden/DOCUMENT_SUMMARY/file_name.txt").trim()
        val geminiResponse = loadTestAsset("golden/DOCUMENT_SUMMARY/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/DOCUMENT_SUMMARY/expected_domain_summary.json")

        val textLength = inputDocText.length
        assertTrue("DOCUMENT_SUMMARY input_document.txt must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("DOCUMENT_SUMMARY")
        assertNotNull("Engine DOCUMENT_SUMMARY must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("DOCUMENT_SUMMARY system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = inputDocText.toByteArray(Charsets.UTF_8),
            mimeType = "text/plain",
            metadata = mapOf("fileName" to fileName),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("DOCUMENT_SUMMARY", engine.contract.functionId)
        assertEquals("Dokument: Projekt-Charta AI-Entwicklung 2026", summary.title)
        assertEquals("projekt_charta.pdf", summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "DOCUMENT_SUMMARY",
            analysisType = AnalysisType.DOKUMENTE,
            inputFixture = "golden/DOCUMENT_SUMMARY/input_document.txt",
            inputHash = inputDocText.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/DOCUMENT_SUMMARY/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/DOCUMENT_SUMMARY/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    @Test
    fun selfTest_PHOTO_SCREENSHOT_ANALYSIS_passesFullPipeline() = runBlocking {
        val inputDocText = loadTestAsset("golden/PHOTO_SCREENSHOT_ANALYSIS/input_image_meta.txt")
        val geminiResponse = loadTestAsset("golden/PHOTO_SCREENSHOT_ANALYSIS/gemini_response.json")
        val expectedOutputJson = loadTestAsset("golden/PHOTO_SCREENSHOT_ANALYSIS/expected_domain_summary.json")

        val textLength = inputDocText.length
        assertTrue("PHOTO_SCREENSHOT_ANALYSIS input_image_meta.txt must be loaded", textLength > 0)

        fakeGateway.rawResponseText = geminiResponse

        val engine = registry.getEngine("PHOTO_SCREENSHOT_ANALYSIS")
        assertNotNull("Engine PHOTO_SCREENSHOT_ANALYSIS must be registered", engine)

        val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
        val promptHash = promptText.sha256()
        assertFalse("PHOTO_SCREENSHOT_ANALYSIS system prompt must not be blank", promptText.isBlank())

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = inputDocText.toByteArray(Charsets.UTF_8),
            mimeType = "image/png",
            metadata = mapOf("fileName" to "test_screenshot.png"),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )

        val summary = engine.analyze(input)
        engine.contract.validateOutput(summary)

        fakeRepository.saveAnalysis(summary)
        val saved = fakeRepository.getAnalysisById(summary.id)
        assertNotNull("Analysis must be successfully saved", saved)

        assertEquals("PHOTO_SCREENSHOT_ANALYSIS", engine.contract.functionId)
        assertEquals("Bildanalyse: Beispiel-Screenshot", summary.title)
        assertEquals("content://media/external/images/media/1", summary.originalUrl)
        assertFalse("Short description must not be blank", summary.shortDescription.isBlank())
        assertTrue("Must contain key takeaways", summary.keyTakeaways.isNotEmpty())

        val allNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() && it.details.isNotBlank() }
        assertTrue("Every key takeaway must have a non-blank title and non-blank details", allNonBlank)

        printFactualReport(
            functionId = "PHOTO_SCREENSHOT_ANALYSIS",
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS,
            inputFixture = "golden/PHOTO_SCREENSHOT_ANALYSIS/input_image_meta.txt",
            inputHash = inputDocText.sha256(),
            extractedTextLength = textLength,
            promptFile = engine.contract.promptPath,
            promptHash = promptHash,
            fakeGeminiResponse = "golden/PHOTO_SCREENSHOT_ANALYSIS/gemini_response.json",
            responseHash = geminiResponse.sha256(),
            expectedOutput = "golden/PHOTO_SCREENSHOT_ANALYSIS/expected_domain_summary.json",
            expectedOutputHash = expectedOutputJson.sha256(),
            parserSuccess = true,
            contractSuccess = true,
            titleNonBlank = summary.title.isNotBlank(),
            shortDescriptionNonBlank = summary.shortDescription.isNotBlank(),
            takeawayCount = summary.keyTakeaways.size,
            allTakeawayTitlesNonBlank = summary.keyTakeaways.all { it.title.isNotBlank() },
            allTakeawayDetailsNonBlank = allNonBlank,
            historySaved = (saved != null),
            finalStatus = "GRÜN"
        )
    }

    /**
     * Test for B.6 Menu/UI Visibility
     * Assures B.6 Weitere relevante Aspekte is enabled, has correct metadata, and label is correct in FeatureCatalog.
     */
    @Test
    fun selfTest_RELEVANT_ASPECTS_menuUiVisibility_isCorrect() {
        val b6Feature = FeatureCatalog.features.find { it.functionId == "RELEVANT_ASPECTS" }
        assertNotNull("B.6 must exist in the FeatureCatalog", b6Feature)
        assertTrue("B.6 must be enabled", b6Feature!!.enabled)
        assertFalse("B.6 must not be a placeholder", b6Feature.isPlaceholder)
        assertEquals("B", b6Feature.category)
        assertEquals("Weitere relevante Aspekte", b6Feature.name)

        // Ensure category "B" exists and has correct label/name
        val categoryB = FeatureCatalog.categories.find { it.id == "B" }
        assertNotNull("Category B must exist", categoryB)
        assertEquals("Qualität, Kritik & Einordnung", categoryB!!.name)
    }

    /**
     * Test 3: selfTest_WEB_SUMMARY_rejectsOrSanitizesEmptyTakeawayDetails
     * Verifies that blank takeaway details are strictly caught and rejected by either the parser or contract.
     */
    @Test
    fun selfTest_WEB_SUMMARY_rejectsOrSanitizesEmptyTakeawayDetails() = runBlocking {
        // Case A: Response where details is blank, but title is present.
        // The parser's sanitizeTakeawayItem copies the non-blank title to details.
        val sanitizableResponse = """
            {
              "title": "Invalid Response",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "Title with empty details",
                  "details": ""
                }
              ]
            }
        """.trimIndent()

        val parsedSanitized = SummaryResponseParser.parse(
            rawText = sanitizableResponse,
            originalFallbackUrl = "https://example.com",
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            analysisId = UUID.randomUUID().toString()
        )
        // Verify that the empty details got sanitized and are now non-blank (with controlled supplement instead of title copy)
        assertTrue("Sanitizer must populate empty details", parsedSanitized.keyTakeaways.isNotEmpty())
        assertEquals("Ergänzende Detailausführungen sind dem Quelltext direkt zu entnehmen.", parsedSanitized.keyTakeaways[0].details)

        // Case B: Response where BOTH title and details are empty.
        // This cannot be sanitized and must result in a structured extraction failure.
        val fullyBlankResponse = """
            {
              "title": "Fully Blank",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "",
                  "details": ""
                }
              ]
            }
        """.trimIndent()

        try {
            SummaryResponseParser.parse(
                rawText = fullyBlankResponse,
                originalFallbackUrl = "https://example.com",
                analysisType = AnalysisType.STANDARD_WEBSEITE,
                analysisId = UUID.randomUUID().toString()
            )
            fail("Expected parse to fail with IOException when all takeaways are blank")
        } catch (e: Exception) {
            assertTrue("Should throw STRUCTURED_EXTRACTION_FAILED", 
                e.message?.contains("STRUCTURED_EXTRACTION_FAILED") == true || e is java.io.IOException)
        }

        // Case C: Verify that EngineContract strictly fails if a DomainSummary with blank details escapes the parser
        val engine = registry.getEngine("WEB_SUMMARY")
        assertNotNull(engine)

        val invalidSummary = DomainSummary(
            id = UUID.randomUUID().toString(),
            title = "Invalid Webpage",
            originalUrl = "https://example.com",
            shortDescription = "Invalid description",
            keyTakeaways = listOf(
                com.example.domain.model.TakeawayItem("Takeaway Title", "") // empty details
            ),
            analysisId = UUID.randomUUID().toString()
        )

        try {
            engine!!.contract.validateOutput(invalidSummary)
            fail("EngineContract validation must fail if key takeaways contain blank details.")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should describe contract details violation",
                e.message?.contains("requires non-blank takeaway 'details'") == true)
        }
    }

    @Test
    fun selfTest_parser_visualMetadataSupport() = runBlocking {
        // Case A: JSON with visual_metadata (snake_case)
        val jsonSnake = """
            {
              "title": "Snake Case Test",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "Risk Item",
                  "details": "Details here",
                  "visual_metadata": {
                    "risk_level": "high",
                    "severity": "medium",
                    "not_string_val": 42
                  }
                }
              ]
            }
        """.trimIndent()

        val parsedSnake = SummaryResponseParser.parse(
            rawText = jsonSnake,
            originalFallbackUrl = "https://example.com",
            analysisType = AnalysisType.RISIKO_ANALYSE,
            analysisId = UUID.randomUUID().toString()
        )
        assertEquals(1, parsedSnake.keyTakeaways.size)
        val itemSnake = parsedSnake.keyTakeaways[0]
        assertEquals("high", itemSnake.visualMetadata["risk_level"])
        assertEquals("medium", itemSnake.visualMetadata["severity"])
        assertEquals("42", itemSnake.visualMetadata["not_string_val"]) // safely converted to string

        // Case B: JSON with visualMetadata (camelCase)
        val jsonCamel = """
            {
              "title": "Camel Case Test",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "Risk Item",
                  "details": "Details here",
                  "visualMetadata": {
                    "risk_level": "low",
                    "severity": "high"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsedCamel = SummaryResponseParser.parse(
            rawText = jsonCamel,
            originalFallbackUrl = "https://example.com",
            analysisType = AnalysisType.RISIKO_ANALYSE,
            analysisId = UUID.randomUUID().toString()
        )
        assertEquals(1, parsedCamel.keyTakeaways.size)
        val itemCamel = parsedCamel.keyTakeaways[0]
        assertEquals("low", itemCamel.visualMetadata["risk_level"])
        assertEquals("high", itemCamel.visualMetadata["severity"])

        // Case C: String-based key takeaways remain compatible
        val jsonStrings = """
            {
              "title": "Strings Test",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                "Just a string takeaway"
              ]
            }
        """.trimIndent()

        val parsedStrings = SummaryResponseParser.parse(
            rawText = jsonStrings,
            originalFallbackUrl = "https://example.com",
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            analysisId = UUID.randomUUID().toString()
        )
        assertEquals(1, parsedStrings.keyTakeaways.size)
        assertTrue(parsedStrings.keyTakeaways[0].visualMetadata.isEmpty())
    }

    @Test
    fun selfTest_parser_robustness_rawJson_and_fences() = runBlocking {
        // Case A: JSON with markdown code fences
        val jsonWithFences = """
            ```json
            {
              "title": "Fenced Test",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "Clear Point",
                  "details": "Details of the point"
                }
              ]
            }
            ```
        """.trimIndent()

        val parsedFences = SummaryResponseParser.parse(
            rawText = jsonWithFences,
            originalFallbackUrl = "https://example.com",
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            analysisId = UUID.randomUUID().toString()
        )
        assertEquals("Fenced Test", parsedFences.title)
        assertEquals(1, parsedFences.keyTakeaways.size)
        assertEquals("Clear Point", parsedFences.keyTakeaways[0].title)

        // Case B: Raw JSON inside details must be filtered/rejected
        val invalidNestedJson = """
            {
              "title": "Nested Invalid",
              "original_url": "https://example.com",
              "short_description": "Description",
              "key_takeaways": [
                {
                  "title": "Bad Point",
                  "details": { "nested_title": "should not be here" }
                }
              ]
            }
        """.trimIndent()

        try {
            SummaryResponseParser.parse(
                rawText = invalidNestedJson,
                originalFallbackUrl = "https://example.com",
                analysisType = AnalysisType.STANDARD_WEBSEITE,
                analysisId = UUID.randomUUID().toString()
            )
            fail("Expected parse to fail with IOException when takeaways contain raw JSON in details")
        } catch (e: Exception) {
            assertTrue("Should throw STRUCTURED_EXTRACTION_FAILED or ParserFailure", 
                e.message?.contains("STRUCTURED_EXTRACTION_FAILED") == true || 
                e.message?.contains("ParserFailure") == true || 
                e is java.io.IOException
            )
        }
    }

    /**
     * Test 4: selfTest_KEY_TAKEAWAYS_enforcesMaxThreeTakeaways
     * Verifies that TOP_3_KERNAUSSAGEN strictly enforces a limit of at most 3 key takeaways.
     */
    @Test
    fun selfTest_KEY_TAKEAWAYS_enforcesMaxThreeTakeaways() = runBlocking {
        // Gemini response returning 4 takeaways instead of 3
        val overloadedResponse = """
            {
              "title": "Overloaded Response Test",
              "original_url": "https://example.com",
              "short_description": "Four takeaways",
              "key_takeaways": [
                {"title": "One", "details": "Detail one"},
                {"title": "Two", "details": "Detail two"},
                {"title": "Three", "details": "Detail three"},
                {"title": "Four", "details": "Detail four"}
              ]
            }
        """.trimIndent()

        val engine = registry.getEngine("KEY_TAKEAWAYS")
        assertNotNull("Engine KEY_TAKEAWAYS must be registered", engine)

        fakeGateway.rawResponseText = overloadedResponse

        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Dummy content",
            enrichedText = "Dummy content",
            metadata = mapOf("url" to "https://example.com"),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.TOP_3_KERNAUSSAGEN
        )

        val result = engine!!.analyze(input)

        // The Top3KeyPointsEngine implementation should either fail contract validation or truncate to 3.
        // Let's assert that the final domain summary has at most 3 takeaways.
        assertTrue("A.2 Engine must limit output key takeaways size to at most 3", result.keyTakeaways.size <= 3)
    }

    /**
     * Test 5: selfTest_sequential_A1_A2_noStateLeak
     * Verifies that sequential runs of A.1 and A.2 do not mix up or leak states.
     */
    @Test
    fun selfTest_sequential_A1_A2_noStateLeak() = runBlocking {
        // Run A.1
        selfTest_WEB_SUMMARY_standardWebseite_passesFullPipeline()

        // Clear fake history
        fakeRepository.clear()

        // Run A.2
        selfTest_KEY_TAKEAWAYS_top3Kernaussagen_passesFullPipeline()

        // Ensure history only contains the final A.2 summary and not any leftover A.1
        val history = fakeRepository.getAllAnalyses()
        assertEquals("History must contain exactly one saved item after clearing and running A.2", 1, history.size)
    }

    /**
     * Test 6: selfTest_history_savesSuccessfulResults
     * Validates that historical logs match exactly what was analyzed.
     */
    @Test
    fun selfTest_history_savesSuccessfulResults() = runBlocking {
        fakeRepository.clear()

        val item = DomainSummary(
            id = "test-history-item-uuid",
            title = "History Saved Item",
            originalUrl = "https://example.com/saved",
            shortDescription = "Description of history item",
            keyTakeaways = listOf(
                com.example.domain.model.TakeawayItem("Historical Fact", "Detailed history verification point")
            ),
            analysisId = UUID.randomUUID().toString()
        )

        fakeRepository.saveAnalysis(item)

        val list = fakeRepository.getAllAnalyses()
        assertEquals(1, list.size)
        assertEquals("test-history-item-uuid", list[0].id)
        assertEquals("History Saved Item", list[0].title)
        assertEquals("https://example.com/saved", list[0].originalUrl)
    }

    /**
     * Test 7: selfTest_error_contractViolation_notContentLoading
     * Proves that ContractViolation is correctly classified as a process processing error, not a content-loading error.
     */
    @Test
    fun selfTest_error_contractViolation_notContentLoading() {
        val contractErrorMsg = "Contract violation: output schema requires non-blank takeaway 'details'"
        val contentErrorMsg = "Unable to resolve host \"example.com\": No address associated with hostname"

        // Rule: ContractViolation is not content loading error
        val isContractA = contractErrorMsg.contains("Contract violation", ignoreCase = true)
        val isContractB = contentErrorMsg.contains("Contract violation", ignoreCase = true)

        assertTrue("contractErrorMsg is classified as a contract violation", isContractA)
        assertFalse("contentErrorMsg is not classified as a contract violation", isContractB)

        // Classify UI Message
        val uiMessageForContract = if (isContractA) "Analyseergebnis konnte nicht verarbeitet werden." else "Inhalt konnte nicht geladen werden"
        val uiMessageForContent = if (isContractB) "Analyseergebnis konnte nicht verarbeitet werden." else "Inhalt konnte nicht geladen werden"

        assertEquals("Analyseergebnis konnte nicht verarbeitet werden.", uiMessageForContract)
        assertEquals("Inhalt konnte nicht geladen werden", uiMessageForContent)
    }

    /**
     * Test 8: selfTest_featureCatalog_registry_prompt_menu_consistency
     * Asserts that ALL enabled features in FeatureCatalog are fully backed, registered, and their prompts exist.
     */
    @Test
    fun selfTest_featureCatalog_registry_prompt_menu_consistency() {
        val enabledFeatures = FeatureCatalog.features.filter { it.enabled && !it.isPlaceholder && it.functionId != "GOOGLE_MAPS_ANALYZER" }

        assertTrue("There must be enabled production features", enabledFeatures.isNotEmpty())

        for (feat in enabledFeatures) {
            val aType = feat.analysisType
            assertNotNull("Active production feature ${feat.functionId} must have an associated AnalysisType", aType)

            // Resolve functionId from Registry for this analysisType
            val registryFuncId = registry.getFunctionIdForType(aType!!)
            assertEquals("AnalysisType mapping must match functionId in FeatureCatalog", feat.functionId, registryFuncId)

            // Resolve engine
            val engine = registry.getEngine(feat.functionId)
            assertNotNull("Active feature ${feat.functionId} must have an engine registered in AnalysisRegistry", engine)

            // Validate that the system prompt asset can be loaded and is non-empty
            val promptText = filePromptLoader.loadAsset(engine!!.contract.promptPath)
            assertFalse("Prompt asset '${engine.contract.promptPath}' for active feature ${feat.functionId} must exist and not be blank", promptText.isBlank())
        }
    }

    // --- Optional Manual Live Smoke Test (Teil G) ---

    @Test
    @Ignore("Manual live smoke test - requires real Gemini API Key. Skipped in CI/standard unit tests.")
    fun testLive_SmokeTest_A1_WebpageAnalysis() = runBlocking {
        // This test would make a live call if enabled and an API key was provided
        val apiKey = System.getenv("RELEVANTOR_LIVE_API_KEY") ?: System.getProperty("RELEVANTOR_LIVE_API_KEY")
        assertNotNull("To run the live smoke test, RELEVANTOR_LIVE_API_KEY must be supplied", apiKey)

        // Simulating the run condition
        println("Executing manual live smoke test with API Key...")
    }

    @Test
    fun selfTest_outputQuality_verifications() = runBlocking {
        // 1. B.6 engine must not produce markdown in title/details
        val b6Engine = registry.getEngine("RELEVANT_ASPECTS")
        assertNotNull("B.6 engine must be registered", b6Engine)
        
        val testResponse = """
            {
              "title": "Homeoffice-Aspekte",
              "original_url": "https://example.com/b6",
              "short_description": "Beschreibung der Aspekte",
              "key_takeaways": [
                {
                  "title": "**Flexibilität**: Erlaubt freies Arbeiten",
                  "details": "Details der **Flexibilität** ohne __Markdown__."
                }
              ]
            }
        """.trimIndent()
        
        fakeGateway.rawResponseText = testResponse
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Inhalt",
            enrichedText = "Inhalt",
            metadata = mapOf("url" to "https://example.com/b6"),
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.WEITERE_RELEVANTE_ASPEKTE
        )
        
        val summary = b6Engine!!.analyze(input)
        assertNotNull(summary)
        
        // Assert no "**" or "__" in title/details
        for (item in summary.keyTakeaways) {
            assertFalse("Title must not contain markdown", item.title.contains("**") || item.title.contains("__"))
            assertFalse("Details must not contain markdown", item.details.contains("**") || item.details.contains("__"))
            assertNotEquals("Title and details must not be identical", item.title, item.details)
        }
        
        // 2. OutputPresentationPolicy: A.2 (TOP_3_KERNAUSSAGEN) must be NUMBERED, others must be BULLET
        val top3Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.TOP_3_KERNAUSSAGEN)
        val webseitePolicy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.STANDARD_WEBSEITE)
        val b6Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.WEITERE_RELEVANTE_ASPEKTE)
        val riskPolicy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.RISIKO_ANALYSE)
        val perspectivesPolicy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS)

        assertEquals(com.example.ui.metadata.ListStyle.NUMBERED, top3Policy.listStyle)
        assertEquals(com.example.ui.metadata.LayoutType.TOP3_LIST, top3Policy.layoutType)

        assertEquals(com.example.ui.metadata.ListStyle.BULLET, riskPolicy.listStyle)
        assertEquals(com.example.ui.metadata.LayoutType.RISK_LIST, riskPolicy.layoutType)

        assertEquals(com.example.ui.metadata.ListStyle.BULLET, perspectivesPolicy.listStyle)
        assertEquals(com.example.ui.metadata.LayoutType.PRO_CONTRA_LIST, perspectivesPolicy.layoutType)

        assertEquals(com.example.ui.metadata.ListStyle.BULLET, webseitePolicy.listStyle)
        assertEquals(com.example.ui.metadata.LayoutType.DEFAULT_LIST, webseitePolicy.layoutType)

        assertEquals(com.example.ui.metadata.ListStyle.BULLET, b6Policy.listStyle)
        assertEquals(com.example.ui.metadata.LayoutType.DEFAULT_LIST, b6Policy.layoutType)
    }

    @Test
    fun selfTest_exportFormatter_formattingAndEscaping() {
        println("=== RELEVANTOR EXPORTFORMATTER TESTS ===")
        val summary = com.example.domain.model.DomainSummary(
            id = "test-id",
            title = "Testing <Escaping> & formatting",
            shortDescription = "Desc with \"quotes\" & newline\nSecond line",
            owner = "Author's Name",
            originalUrl = "https://example.com/?param=1&param=2",
            keyTakeaways = listOf(
                com.example.domain.model.TakeawayItem(
                    title = "Takeaway <One>",
                    details = "Details & text\nNewline details"
                ),
                com.example.domain.model.TakeawayItem(
                    title = "Takeaway Two",
                    details = "Normal details"
                )
            ),
            analysisId = "test-analysis-id"
        )

        val numberedPolicy = com.example.ui.metadata.PresentationPolicy(listStyle = com.example.ui.metadata.ListStyle.NUMBERED)
        val bulletPolicy = com.example.ui.metadata.PresentationPolicy(listStyle = com.example.ui.metadata.ListStyle.BULLET)

        // 1. Plaintext formatting check (Numbered)
        val plainNumbered = com.example.ui.metadata.ExportFormatter.formatPlainText(summary, numberedPolicy)
        assertTrue("Plaintext should contain title", plainNumbered.contains("**Testing <Escaping> & formatting**"))
        assertTrue("Plaintext numbered should contain 01. ", plainNumbered.contains("01. Takeaway <One>"))
        assertTrue("Plaintext numbered should contain 02. ", plainNumbered.contains("02. Takeaway Two"))

        // 2. Plaintext formatting check (Bullet)
        val plainBullet = com.example.ui.metadata.ExportFormatter.formatPlainText(summary, bulletPolicy)
        assertTrue("Plaintext bullet should contain • ", plainBullet.contains("• Takeaway <One>"))

        // 3. HTML formatting check
        val htmlNumbered = com.example.ui.metadata.ExportFormatter.formatHtml(summary, numberedPolicy)
        val htmlBullet = com.example.ui.metadata.ExportFormatter.formatHtml(summary, bulletPolicy)

        // Check HTML escaping
        assertFalse("HTML must escape < inside title", htmlNumbered.contains("<h1>Testing <Escaping>"))
        assertTrue("HTML must escape < inside title", htmlNumbered.contains("&lt;Escaping&gt;"))
        assertTrue("HTML must escape & inside title", htmlNumbered.contains("Testing &lt;Escaping&gt; &amp; formatting"))
        assertTrue("HTML must escape quotes inside description", htmlNumbered.contains("&quot;quotes&quot;"))
        assertTrue("HTML must escape ' in author name", htmlNumbered.contains("Author&#x27;s Name"))
        assertTrue("HTML must escape & inside originalUrl", htmlNumbered.contains("param=1&amp;param=2"))
        
        // Check newline replacement in HTML
        assertTrue("HTML must convert description newlines to <br/>", htmlNumbered.contains("Desc with &quot;quotes&quot; &amp; newline<br/>Second line"))
        assertTrue("HTML must convert details newlines to <br/>", htmlNumbered.contains("Details &amp; text<br/>Newline details"))

        // Check prefixes
        assertTrue("HTML numbered must contain 01. ", htmlNumbered.contains("01. "))
        assertTrue("HTML bullet must contain &bull; ", htmlBullet.contains("&bull; "))
        
        println("All ExportFormatter tests passed successfully!")
    }

    @Test
    fun selfTest_featureOnboardingVerification() = runBlocking {
        println("=== RELEVANTOR FEATURE ONBOARDING VERIFICATION ===")
        val activeFeatures = FeatureCatalog.features.filter { it.enabled && !it.isPlaceholder && it.functionId != "GOOGLE_MAPS_ANALYZER" }
        val placeholderFeatures = FeatureCatalog.features.filter { it.isPlaceholder }

        // Assert there are active and placeholder features
        assertTrue("Must have active production features", activeFeatures.isNotEmpty())
        assertTrue("Must have placeholder features", placeholderFeatures.isNotEmpty())

        // Load prompt_manifest.json
        val manifestContent = context.assets.open("prompts/prompt_manifest.json").bufferedReader().use { it.readText() }
        val manifestJson = org.json.JSONObject(manifestContent)

        // Load function_registry.json
        val registryContent = context.assets.open("prompts/function_registry.json").bufferedReader().use { it.readText() }
        val registryJson = org.json.JSONObject(registryContent)
        val functionsArray = registryJson.getJSONArray("functions")

        println(String.format("%-10s | %-32s | %-8s | %-30s | %-8s | %-8s | %-8s | %-8s | %s",
            "Func ID", "Analysis Type", "Category", "Prompt File", "Engine", "Policy", "Golden", "Expected", "Status"))
        println("-".repeat(140))

        var allFeaturesGreen = true

        for (feature in activeFeatures) {
            var featureGreen = true
            val errors = mutableListOf<String>()

            // 1. Basic properties
            if (feature.functionId.isBlank()) {
                featureGreen = false
                errors.add("Blank functionId")
            }
            if (feature.name.isBlank()) {
                featureGreen = false
                errors.add("Blank name")
            }
            if (feature.category.isBlank()) {
                featureGreen = false
                errors.add("Blank category")
            } else {
                val categoryExists = FeatureCatalog.categories.any { it.id == feature.category }
                if (!categoryExists) {
                    featureGreen = false
                    errors.add("Category ${feature.category} not found in FeatureCatalog")
                }
            }

            // 2. Analysis type and Registry
            val aType = feature.analysisType
            if (aType == null) {
                featureGreen = false
                errors.add("Missing analysisType")
            } else {
                val registryFuncId = registry.getFunctionIdForType(aType)
                if (registryFuncId != feature.functionId) {
                    featureGreen = false
                    errors.add("Registry mapping functionId mismatch: expected ${feature.functionId}, got $registryFuncId")
                }
            }

            // 3. Engine and Contract
            val engine = registry.getEngine(feature.functionId)
            if (engine == null) {
                featureGreen = false
                errors.add("Engine not registered")
            } else {
                val contract = engine.contract
                if (contract == null) {
                    featureGreen = false
                    errors.add("EngineContract missing")
                } else {
                    if (contract.functionId != feature.functionId) {
                        featureGreen = false
                        errors.add("Contract functionId mismatch: expected ${feature.functionId}, got ${contract.functionId}")
                    }

                    // 4. Prompt Asset
                    try {
                        val promptStream = context.assets.open(contract.promptPath)
                        val promptText = promptStream.bufferedReader().use { it.readText() }
                        if (promptText.isBlank()) {
                            featureGreen = false
                            errors.add("Prompt file is empty: ${contract.promptPath}")
                        }
                    } catch (e: Exception) {
                        featureGreen = false
                        errors.add("Failed to load prompt file: ${contract.promptPath}")
                    }

                    // 5. prompt_manifest.json mapping
                    if (aType != null) {
                        if (!manifestJson.has(aType.name)) {
                            featureGreen = false
                            errors.add("prompt_manifest.json lacks entry for ${aType.name}")
                        } else {
                            val expectedPromptFile = contract.promptPath.substringAfterLast("/")
                            val manifestPromptFile = manifestJson.getString(aType.name)
                            if (expectedPromptFile != manifestPromptFile) {
                                featureGreen = false
                                errors.add("prompt_manifest.json mapping mismatch: expected $expectedPromptFile, got $manifestPromptFile")
                            }
                        }
                    }
                }
            }

            // 6. function_registry.json checks
            if (aType != null) {
                val isMappedInRegistryJson = (0 until functionsArray.length()).any { i ->
                    functionsArray.getJSONObject(i).getString("function_id") == aType.name
                }
                if (isMappedInRegistryJson) {
                    val jsonFunc = (0 until functionsArray.length())
                        .map { functionsArray.getJSONObject(it) }
                        .first { it.getString("function_id") == aType.name }
                    if (!jsonFunc.getBoolean("enabled")) {
                        featureGreen = false
                        errors.add("function_registry.json has function marked as disabled")
                    }
                }
            }

            // 7. OutputPresentationPolicy
            val policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(aType)
            if (policy == null) {
                featureGreen = false
                errors.add("PresentationPolicy missing")
            }

            // 8. Golden Fixtures & expected json
            var goldenFound = "NEIN"
            var expectedFound = "NEIN"
            try {
                val searchDirs = listOf(
                    "src/test/assets/golden/${feature.functionId}",
                    "app/src/test/assets/golden/${feature.functionId}",
                    "../app/src/test/assets/golden/${feature.functionId}",
                    "applet/app/src/test/assets/golden/${feature.functionId}"
                )
                var goldenDir: java.io.File? = null
                for (dirPath in searchDirs) {
                    val dir = java.io.File(dirPath)
                    if (dir.exists() && dir.isDirectory) {
                        goldenDir = dir
                        break
                    }
                }

                if (goldenDir != null) {
                    val files = goldenDir.listFiles()?.map { it.name } ?: emptyList()
                    if (files.isNotEmpty()) {
                        goldenFound = "JA"
                        if (files.contains("gemini_response.json") && files.contains("expected_domain_summary.json")) {
                            expectedFound = "JA"
                        } else {
                            featureGreen = false
                            errors.add("Golden directory lacks gemini_response.json or expected_domain_summary.json")
                        }
                    } else {
                        featureGreen = false
                        errors.add("Golden directory is empty")
                    }
                } else {
                    featureGreen = false
                    errors.add("Golden directory is missing")
                }
            } catch (e: Exception) {
                featureGreen = false
                errors.add("Golden directory check failed: ${e.message}")
            }

            // 9. Self-Test Abdeckung check via Reflection
            val testMethods = RelevantorSelfTestHarnessTest::class.java.methods.map { it.name }
            val cleanFuncId = feature.functionId.replace(".", "")
            val hasPipelineTest = testMethods.any { 
                it.contains("selfTest_${cleanFuncId}", ignoreCase = true) || 
                it.contains("selfTest_${feature.functionId}", ignoreCase = true) 
            }
            if (!hasPipelineTest) {
                featureGreen = false
                errors.add("Self-test cover missing (expected test name selfTest_${cleanFuncId}_passesFullPipeline)")
            }

            val status = if (featureGreen) "GRÜN" else "ROT"
            if (!featureGreen) {
                allFeaturesGreen = false
                println(String.format("%-10s | %-32s | %-8s | %-30s | %-8s | %-8s | %-8s | %-8s | %s (Fehler: %s)",
                    feature.functionId,
                    aType?.name ?: "null",
                    feature.category,
                    engine?.contract?.promptPath?.substringAfterLast("/") ?: "n/a",
                    if (engine != null) "JA" else "NEIN",
                    if (policy != null) "JA" else "NEIN",
                    goldenFound,
                    expectedFound,
                    status,
                    errors.joinToString(", ")
                ))
            } else {
                println(String.format("%-10s | %-32s | %-8s | %-30s | %-8s | %-8s | %-8s | %-8s | %s",
                    feature.functionId,
                    aType?.name ?: "null",
                    feature.category,
                    engine?.contract?.promptPath?.substringAfterLast("/") ?: "n/a",
                    "JA", "JA", "JA", "JA", status
                ))
            }
        }

        println("=".repeat(140))
        println("Active functions checked: ${activeFeatures.size}")
        println("Placeholder functions found: ${placeholderFeatures.size}")

        // Check placeholders don't trigger engine/prompt requirements but are marked isPlaceholder
        for (placeholder in placeholderFeatures) {
            assertTrue("Placeholder must have isPlaceholder=true", placeholder.isPlaceholder)
            assertFalse("Placeholder must have enabled=false", placeholder.enabled)
            assertNull("Placeholder should have null analysisType", placeholder.analysisType)
        }

        assertTrue("All active features must be fully onboarded and GRÜN", allFeaturesGreen)
    }

    @Test
    fun selfTest_architectureRegression_noDirectAnalysisTypeWeiches() {
        println("=== ARCHITECTURE REGRESSION TEST: NO DIRECT ANALYSISTYPE WEICHES IN MAINACTIVITY ===")
        // Find MainActivity.kt
        val mainActivityFile = java.io.File("app/src/main/java/com/example/MainActivity.kt").takeIf { it.exists() }
            ?: java.io.File("src/main/java/com/example/MainActivity.kt").takeIf { it.exists() }

        if (mainActivityFile != null) {
            val lines = mainActivityFile.readLines()
            var directWeichesFound = 0
            lines.forEachIndexed { index, line ->
                // Search for AnalysisType.TOP_3_KERNAUSSAGEN or TOP_3_KERNAUSSAGEN
                if (line.contains("TOP_3_KERNAUSSAGEN") && !line.contains("OutputPresentationPolicy") && !line.contains("import")) {
                    println("VIOLATION: Direct AnalysisType.TOP_3_KERNAUSSAGEN check on line ${index + 1}: $line")
                    directWeichesFound++
                }
            }
            assertEquals("No direct AnalysisType.TOP_3_KERNAUSSAGEN switches allowed in MainActivity.kt! Use OutputPresentationPolicy instead.", 0, directWeichesFound)
            println("Verification complete. 0 direct AnalysisType.TOP_3_KERNAUSSAGEN checks found in MainActivity.kt.")
        } else {
            println("WARNING: MainActivity.kt not accessible in local directory structure. Skipping source scanning.")
        }
    }

    // --- Factual Report Printer Helper ---

    private fun printFactualReport(
        functionId: String,
        analysisType: AnalysisType,
        inputFixture: String,
        inputHash: String,
        extractedTextLength: Int,
        promptFile: String,
        promptHash: String,
        fakeGeminiResponse: String,
        responseHash: String,
        expectedOutput: String,
        expectedOutputHash: String,
        parserSuccess: Boolean,
        contractSuccess: Boolean,
        titleNonBlank: Boolean,
        shortDescriptionNonBlank: Boolean,
        takeawayCount: Int,
        allTakeawayTitlesNonBlank: Boolean,
        allTakeawayDetailsNonBlank: Boolean,
        historySaved: Boolean,
        finalStatus: String
    ) {
        println("=== RELEVANTOR SELF-TEST FAKTENBERICHT ===")
        println("functionId: $functionId")
        println("AnalysisType: $analysisType")
        println("inputFixture: $inputFixture")
        println("inputHash: $inputHash")
        println("extractedTextLength: $extractedTextLength")
        println("promptFile: $promptFile")
        println("promptHash: $promptHash")
        println("fakeGeminiResponse: $fakeGeminiResponse")
        println("responseHash: $responseHash")
        println("expectedOutput: $expectedOutput")
        println("expectedOutputHash: $expectedOutputHash")
        println("parserSuccess: ${if (parserSuccess) "ja" else "nein"}")
        println("contractSuccess: ${if (contractSuccess) "ja" else "nein"}")
        println("titleNonBlank: ${if (titleNonBlank) "ja" else "nein"}")
        println("shortDescriptionNonBlank: ${if (shortDescriptionNonBlank) "ja" else "nein"}")
        println("takeawayCount: $takeawayCount")
        println("allTakeawayTitlesNonBlank: ${if (allTakeawayTitlesNonBlank) "ja" else "nein"}")
        println("allTakeawayDetailsNonBlank: ${if (allTakeawayDetailsNonBlank) "ja" else "nein"}")
        println("historySaved: ${if (historySaved) "ja" else "nein"}")
        println("finalStatus: $finalStatus")
        println("==========================================\n")
    }
}
