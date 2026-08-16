package com.example

import com.example.data.AnalysisType
import com.example.data.RuntimeVerificationLayer
import com.example.data.SummaryResponseParser
import com.example.domain.engine.EngineCapabilities
import com.example.domain.engine.EngineContract
import com.example.domain.model.DomainSummary
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class FreeSourceQueryEmptyTakeawaysTest {

    @Test
    fun testFreeSourceQueryWithEmptyTakeawaysAndPresentShortDescriptionSucceeds() {
        val json = """
            {
              "title": "Lofi Girl Live",
              "original_url": "https://www.youtube.com/watch?v=5qap5aO4i9A",
              "short_description": "Dazu sind im Transkript keine Angaben erkennbar.",
              "key_takeaways": []
            }
        """.trimIndent()

        val summary = SummaryResponseParser.parse(
            rawText = json,
            originalFallbackUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            analysisId = "test-free-query-empty-takeaways"
        )

        assertNotNull(summary)
        assertEquals("Lofi Girl Live", summary.title)
        assertEquals("Dazu sind im Transkript keine Angaben erkennbar.", summary.shortDescription)
        assertTrue(summary.keyTakeaways.isEmpty())
    }

    @Test
    fun testFreeSourceQueryWithEmptyTakeawaysAndEmptyShortDescriptionFails() {
        val json = """
            {
              "title": "Lofi Girl Live",
              "original_url": "https://www.youtube.com/watch?v=5qap5aO4i9A",
              "short_description": "",
              "key_takeaways": []
            }
        """.trimIndent()

        try {
            SummaryResponseParser.parse(
                rawText = json,
                originalFallbackUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
                analysisType = AnalysisType.FREE_SOURCE_QUERY,
                analysisId = "test-free-query-empty-desc"
            )
            fail("Expected IOException with STRUCTURED_EXTRACTION_FAILED")
        } catch (e: IOException) {
            assertEquals("STRUCTURED_EXTRACTION_FAILED", e.message)
        }
    }

    @Test
    fun testWebSummaryWithEmptyTakeawaysFailsStrictly() {
        val json = """
            {
              "title": "Standard Web Page",
              "original_url": "https://example.com",
              "short_description": "A standard webpage summary.",
              "key_takeaways": []
            }
        """.trimIndent()

        try {
            SummaryResponseParser.parse(
                rawText = json,
                originalFallbackUrl = "https://example.com",
                analysisType = AnalysisType.WEB_SUMMARY,
                analysisId = "test-web-summary-empty-takeaways"
            )
            fail("Expected IOException with STRUCTURED_EXTRACTION_FAILED")
        } catch (e: IOException) {
            assertEquals("STRUCTURED_EXTRACTION_FAILED", e.message)
        }
    }

    @Test
    fun testFreeSourceQueryWithNormalTakeawaysSucceedsUnchanged() {
        val json = """
            {
              "title": "Documentary Video",
              "original_url": "https://www.youtube.com/watch?v=12345",
              "short_description": "A documentary overview.",
              "key_takeaways": [
                {
                  "title": "Main Theme",
                  "details": "Explanation of the central argument."
                }
              ]
            }
        """.trimIndent()

        val summary = SummaryResponseParser.parse(
            rawText = json,
            originalFallbackUrl = "https://www.youtube.com/watch?v=12345",
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            analysisId = "test-free-query-normal"
        )

        assertNotNull(summary)
        assertEquals("Documentary Video", summary.title)
        assertEquals("A documentary overview.", summary.shortDescription)
        assertEquals(1, summary.keyTakeaways.size)
        assertEquals("Main Theme", summary.keyTakeaways[0].title)
    }

    @Test
    fun testRuntimeVerificationLayerAllowsEmptyTakeawaysForFreeSourceQuery() {
        val summary = DomainSummary(
            id = "test-id",
            title = "Lofi Girl Live",
            originalUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            shortDescription = "Dazu sind im Transkript keine Angaben erkennbar.",
            keyTakeaways = emptyList(),
            analysisId = "test-id"
        )
        val context = RuntimeVerificationLayer.VerificationContext(
            functionId = "FREE_SOURCE_QUERY",
            promptHash = "dummy_hash",
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            sourceUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A"
        )

        val result = RuntimeVerificationLayer.validate(summary, context)
        assertTrue("Verification should pass for FREE_SOURCE_QUERY with empty takeaways and valid shortDescription", result.isValid)
    }

    @Test
    fun testRuntimeVerificationLayerRejectsEmptyTakeawaysForWebSummary() {
        val summary = DomainSummary(
            id = "test-id-web",
            title = "Web Summary",
            originalUrl = "https://example.com",
            shortDescription = "Short description",
            keyTakeaways = emptyList(),
            analysisId = "test-id-web"
        )
        val context = RuntimeVerificationLayer.VerificationContext(
            functionId = "WEB_SUMMARY",
            promptHash = "dummy_hash",
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceUrl = "https://example.com"
        )

        val result = RuntimeVerificationLayer.validate(summary, context)
        assertFalse("Verification should fail for WEB_SUMMARY with empty takeaways", result.isValid)
    }

    @Test
    fun testAnalysisRegistryContractValidationAllowsFreeSourceQueryEmptyTakeaways() {
        val contract = EngineContract(
            functionId = "FREE_SOURCE_QUERY",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = EngineCapabilities(
                name = "Free Source Request",
                supportsSearchGrounding = true,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/F_FREIE_QUELLENANFRAGE.md"
        )

        val summary = DomainSummary(
            id = "test-contract",
            title = "Lofi Girl Live",
            originalUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            shortDescription = "Dazu sind im Transkript keine Angaben erkennbar.",
            keyTakeaways = emptyList(),
            analysisId = "test-contract"
        )

        // Should not throw
        contract.validateOutput(summary)
    }

    @Test
    fun testAnalysisRegistryContractValidationRejectsWebSummaryEmptyTakeaways() {
        val contract = EngineContract(
            functionId = "WEB_SUMMARY",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = EngineCapabilities(
                name = "Webpage Analysis",
                supportsSearchGrounding = true,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/F_STANDARD_WEBSEITE.md"
        )

        val summary = DomainSummary(
            id = "test-contract-web",
            title = "Webpage Title",
            originalUrl = "https://example.com",
            shortDescription = "A description",
            keyTakeaways = emptyList(),
            analysisId = "test-contract-web"
        )

        try {
            contract.validateOutput(summary)
            fail("Expected IllegalStateException for WEB_SUMMARY with empty keyTakeaways")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Contract violation: output schema requires non-empty 'keyTakeaways'") == true)
        }
    }

    @Test
    fun testMetadataFallbackForDegradedResultWhenGeminiTitleAndOwnerMissing() {
        val geminiSummary = DomainSummary(
            id = "test-fallback",
            title = "",
            originalUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            shortDescription = "Reine Metadatenanalyse",
            keyTakeaways = emptyList(),
            owner = null,
            analysisId = "test-fallback"
        )
        val metadata = mapOf(
            "title" to "Lofi Girl - beats to relax/study to",
            "channel" to "Lofi Girl",
            "description" to "Peaceful lofi hip hop radio"
        )

        val fallbackTitle = if (geminiSummary.title.isBlank() || geminiSummary.title == "Unbekannter Titel" || geminiSummary.title == "Video nicht auslesbar") {
            metadata["title"]?.ifBlank { null } ?: geminiSummary.title
        } else {
            geminiSummary.title
        }
        val fallbackOwner = if (geminiSummary.owner.isNullOrBlank()) {
            metadata["channel"]?.ifBlank { null }
        } else {
            geminiSummary.owner
        }

        val finalSummary = geminiSummary.copy(
            title = fallbackTitle,
            owner = fallbackOwner,
            fallbackUsed = true
        )

        assertEquals("Lofi Girl - beats to relax/study to", finalSummary.title)
        assertEquals("Lofi Girl", finalSummary.owner)
        assertTrue(finalSummary.fallbackUsed)
    }

    @Test
    fun testMetadataFallbackDoesNotOverwriteNonEmptyGeminiValues() {
        val geminiSummary = DomainSummary(
            id = "test-fallback-gemini",
            title = "Gemini Specific Title",
            originalUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            shortDescription = "Reine Metadatenanalyse",
            keyTakeaways = emptyList(),
            owner = "Gemini Author",
            analysisId = "test-fallback-gemini"
        )
        val metadata = mapOf(
            "title" to "Extracted Metadata Title",
            "channel" to "Extracted Metadata Channel"
        )

        val fallbackTitle = if (geminiSummary.title.isBlank() || geminiSummary.title == "Unbekannter Titel" || geminiSummary.title == "Video nicht auslesbar") {
            metadata["title"]?.ifBlank { null } ?: geminiSummary.title
        } else {
            geminiSummary.title
        }
        val fallbackOwner = if (geminiSummary.owner.isNullOrBlank()) {
            metadata["channel"]?.ifBlank { null }
        } else {
            geminiSummary.owner
        }

        val finalSummary = geminiSummary.copy(
            title = fallbackTitle,
            owner = fallbackOwner,
            fallbackUsed = true
        )

        assertEquals("Gemini Specific Title", finalSummary.title)
        assertEquals("Gemini Author", finalSummary.owner)
        assertTrue(finalSummary.fallbackUsed)
    }
}
