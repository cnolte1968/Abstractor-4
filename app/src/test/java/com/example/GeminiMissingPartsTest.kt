package com.example

import com.example.data.*
import com.example.data.engine.BaseGeminiEngine
import com.example.data.engine.NonRetryableGeminiException
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import com.example.domain.repository.GeminiGateway
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiMissingPartsTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private class TestGeminiEngine(
        gateway: GeminiGateway,
        promptLoader: PromptAssetLoader,
        override val contract: EngineContract = EngineContract(
            functionId = "TEST_FUNCTION",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary",
            capabilities = EngineCapabilities(
                name = "Test Capabilities",
                supportsSearchGrounding = false,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/test.md"
        )
    ) : BaseGeminiEngine(gateway, promptLoader)

    private class DummyPromptLoader : PromptAssetLoader {
        override fun loadAsset(path: String): String = """
            {
              "title": "Test Title",
              "original_url": "https://example.com",
              "short_description": "Test Desc",
              "key_takeaways": [
                { "title": "A", "details": "B" }
              ]
            }
        """.trimIndent()
    }

    private fun createDummyInput(): CanonicalAnalysisInput {
        return CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "https://example.com",
            enrichedText = "Some website content",
            analysisId = "test-analysis-id"
        )
    }

    @Test
    fun testMoshiDeserializationMissingPartsField() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model"
                  },
                  "finishReason": "MAX_TOKENS"
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(GenerateContentResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertNotNull(response?.candidates)
        assertEquals(1, response?.candidates?.size)

        val candidate = response!!.candidates.first()
        assertNotNull(candidate.content)
        assertNotNull(candidate.content?.parts)
        assertTrue(candidate.content!!.parts.isEmpty())
        assertEquals("MAX_TOKENS", candidate.resolvedFinishReason)
    }

    @Test
    fun testMoshiDeserializationWithSafetyFinishReasonAndPromptFeedback() {
        val json = """
            {
              "candidates": [
                {
                  "finish_reason": "SAFETY",
                  "safetyRatings": [
                    { "category": "HATE_SPEECH", "blocked": true }
                  ]
                }
              ],
              "promptFeedback": {
                "block_reason": "SAFETY"
              }
            }
        """.trimIndent()

        val adapter = moshi.adapter(GenerateContentResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals("SAFETY", response?.promptFeedback?.resolvedBlockReason)
        assertEquals("SAFETY", response?.candidates?.firstOrNull()?.resolvedFinishReason)
        assertEquals(true, response?.candidates?.firstOrNull()?.safetyRatings?.firstOrNull()?.blocked)
    }

    @Test
    fun testBaseGeminiEngineSafetyBlockHandling() {
        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                return GenerateContentResponse(
                    candidates = listOf(
                        Candidate(
                            content = Content(parts = emptyList()),
                            finishReason = "SAFETY"
                        )
                    )
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())

        try {
            runBlocking { engine.analyze(createDummyInput()) }
            fail("Expected NonRetryableGeminiException due to SAFETY block")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("SAFETY_BLOCK", e.category)
            assertTrue(e.message.contains("Sicherheitsregel"))
        }
    }

    @Test
    fun testBaseGeminiEngineRecitationBlockHandling() {
        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                return GenerateContentResponse(
                    candidates = listOf(
                        Candidate(
                            content = Content(parts = emptyList()),
                            finishReason = "RECITATION"
                        )
                    )
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())

        try {
            runBlocking { engine.analyze(createDummyInput()) }
            fail("Expected NonRetryableGeminiException due to RECITATION block")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("RECITATION_BLOCK", e.category)
            assertTrue(e.message.contains("urheberrechtlich"))
        }
    }

    @Test
    fun testBaseGeminiEngineMultiCandidateFallbackToCandidateWithText() {
        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                return GenerateContentResponse(
                    candidates = listOf(
                        Candidate(content = Content(parts = emptyList())), // Candidate 0 empty
                        Candidate(content = Content(parts = listOf(Part(text = """
                            {
                              "title": "Test Title",
                              "original_url": "https://example.com",
                              "short_description": "Test Desc",
                              "key_takeaways": [
                                { "title": "A", "details": "B" }
                              ]
                            }
                        """.trimIndent())))) // Candidate 1 has text
                    )
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())

        val result = runBlocking { engine.analyze(createDummyInput()) }
        assertNotNull(result)
        assertEquals("Test Title", result.title)
    }

    @Test
    fun testBaseGeminiEngineEmptyCandidateThrowsEmptyCandidateContent() {
        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                return GenerateContentResponse(
                    candidates = listOf(
                        Candidate(content = Content(parts = emptyList()), finishReason = "STOP")
                    )
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())

        try {
            runBlocking { engine.analyze(createDummyInput()) }
            fail("Expected NonRetryableGeminiException due to EMPTY_CANDIDATE_CONTENT")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("EMPTY_CANDIDATE_CONTENT", e.category)
        }
    }

    @Test
    fun testBuildBalancedExcerptSegmentDistribution() {
        val sb = StringBuilder()
        for (i in 1..25000) {
            sb.append("A")
            if (i % 80 == 0) sb.append("\n")
        }
        val longText = sb.toString()
        val excerpt = BaseGeminiEngine.buildBalancedExcerpt(longText, 12000)

        assertTrue(excerpt.contains("[AUSZUG ANFANG]"))
        assertTrue(excerpt.contains("[AUSZUG MITTE]"))
        assertTrue(excerpt.contains("[AUSZUG ENDE]"))
        assertTrue(excerpt.length <= 12500)

        val idxStart = excerpt.indexOf("[AUSZUG ANFANG]")
        val idxMid = excerpt.indexOf("[AUSZUG MITTE]")
        val idxEnd = excerpt.indexOf("[AUSZUG ENDE]")

        assertTrue(idxStart < idxMid)
        assertTrue(idxMid < idxEnd)
    }

    @Test
    fun testAdaptiveFallbackTriggeredOnLongInputWithGroundingAndEmptyCandidate() {
        val longText = "Sample text ".repeat(1200) // ~14,400 chars (> 12,000)
        var callCount = 0
        var secondAttemptPayloadLength = 0

        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                callCount++
                if (callCount == 1) {
                    return GenerateContentResponse(
                        candidates = listOf(Candidate(content = Content(parts = emptyList()), finishReason = "STOP"))
                    )
                } else {
                    secondAttemptPayloadLength = request.contents.firstOrNull()?.parts?.lastOrNull()?.text?.length ?: 0
                    return GenerateContentResponse(
                        candidates = listOf(
                            Candidate(content = Content(parts = listOf(Part(text = """
                                {
                                  "title": "Fallback Success",
                                  "original_url": "https://example.com",
                                  "short_description": "Worked on attempt 2",
                                  "key_takeaways": [{ "title": "Key", "details": "Val" }]
                                }
                            """.trimIndent()))))
                        )
                    )
                }
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())
        val input = createDummyInput().copy(enrichedText = longText, useSearchGrounding = true)

        val result = runBlocking { engine.analyze(input) }

        assertEquals(2, callCount)
        assertEquals("Fallback Success", result.title)
        assertTrue(secondAttemptPayloadLength > 0)
    }

    @Test
    fun testAdaptiveFallbackFailsAfterSecondAttempt() {
        val longText = "Sample text ".repeat(1200)
        var callCount = 0

        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                callCount++
                return GenerateContentResponse(
                    candidates = listOf(Candidate(content = Content(parts = emptyList()), finishReason = "STOP"))
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())
        val input = createDummyInput().copy(enrichedText = longText, useSearchGrounding = true)

        try {
            runBlocking { engine.analyze(input) }
            fail("Expected NonRetryableGeminiException after 2 empty candidate attempts")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("EMPTY_CANDIDATE_CONTENT", e.category)
            assertEquals(2, callCount)
        }
    }

    @Test
    fun testNoFallbackWhenShortInputEmptyCandidate() {
        val shortText = "Short text" // < 12000 chars
        var callCount = 0

        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                callCount++
                return GenerateContentResponse(
                    candidates = listOf(Candidate(content = Content(parts = emptyList()), finishReason = "STOP"))
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())
        val input = createDummyInput().copy(enrichedText = shortText, useSearchGrounding = true)

        try {
            runBlocking { engine.analyze(input) }
            fail("Expected NonRetryableGeminiException without fallback")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("EMPTY_CANDIDATE_CONTENT", e.category)
            assertEquals(1, callCount)
        }
    }

    @Test
    fun testNoFallbackOnSafetyBlock() {
        val longText = "Sample text ".repeat(1200)
        var callCount = 0

        val gateway = object : GeminiGateway {
            override suspend fun generateContent(model: String, request: GenerateContentRequest): GenerateContentResponse {
                callCount++
                return GenerateContentResponse(
                    candidates = listOf(Candidate(content = Content(parts = emptyList()), finishReason = "SAFETY"))
                )
            }
        }

        val engine = TestGeminiEngine(gateway, DummyPromptLoader())
        val input = createDummyInput().copy(enrichedText = longText, useSearchGrounding = true)

        try {
            runBlocking { engine.analyze(input) }
            fail("Expected NonRetryableGeminiException for safety block")
        } catch (e: NonRetryableGeminiException) {
            assertEquals("SAFETY_BLOCK", e.category)
            assertEquals(1, callCount)
        }
    }
}
