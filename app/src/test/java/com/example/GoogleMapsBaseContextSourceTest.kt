package com.example

import com.example.data.contextengine.ContextEngine
import com.example.data.contextengine.ContextResult
import com.example.data.contextengine.ContextSource
import com.example.data.contextengine.ContextSourceType
import com.example.data.contextengine.GoogleMapsBaseContextSource
import com.example.data.contextengine.LocationContextInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapsBaseContextSourceTest {

    @Test
    fun testGoogleMapsBaseContextSourceSuccess() = runTest {
        val source = GoogleMapsBaseContextSource()
        val input = LocationContextInput(
            placeName = "Greenhouse Community Space",
            category = "Community Center / Coworking",
            address = "Chiang Mai, Thailand",
            description = "A community hub for local events and gatherings."
        )

        val result = source.fetchContext(input)

        assertTrue(result.isSuccessful)
        assertEquals("GOOGLE_MAPS", result.sourceName)
        assertEquals(ContextSourceType.OFFICIAL_DATA, result.sourceType)

        val snippet = result.snippet ?: ""
        assertTrue(snippet.contains("Ort: Greenhouse Community Space"))
        assertTrue(snippet.contains("Kategorie: Community Center / Coworking"))
        assertTrue(snippet.contains("Adresse: Chiang Mai, Thailand"))
        assertTrue(snippet.contains("Beschreibung: A community hub for local events and gatherings."))

        // Verify NO reviews/ratings/stars/opinions are present
        assertFalse(snippet.lowercase().contains("rating"))
        assertFalse(snippet.lowercase().contains("review"))
        assertFalse(snippet.lowercase().contains("stern"))
        assertFalse(snippet.lowercase().contains("star"))
    }

    @Test
    fun testGoogleMapsBaseContextSourceEmptyPlaceNameReturnsFail() = runTest {
        val source = GoogleMapsBaseContextSource()
        val input = LocationContextInput(placeName = "   ")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }

    @Test
    fun testContextEngineFallbackSequence() = runTest {
        // Mock Wiki that fails (no match)
        val wikiFailing = object : ContextSource {
            override val sourceName: String = "WIKIPEDIA"
            override val sourceType: ContextSourceType = ContextSourceType.ENCYCLOPEDIA
            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                return ContextResult(sourceName = sourceName, sourceType = sourceType, isSuccessful = false)
            }
        }

        // Mock Wikivoyage that fails (no match)
        val voyageFailing = object : ContextSource {
            override val sourceName: String = "WIKIVOYAGE"
            override val sourceType: ContextSourceType = ContextSourceType.TRAVEL_GUIDE
            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                return ContextResult(sourceName = sourceName, sourceType = sourceType, isSuccessful = false)
            }
        }

        val mapsBaseSource = GoogleMapsBaseContextSource()

        val engine = ContextEngine(listOf(wikiFailing, voyageFailing, mapsBaseSource))
        val input = LocationContextInput(
            placeName = "Greenhouse Community Space",
            category = "Community Center",
            address = "Chiang Mai, Thailand"
        )

        val results = engine.resolveContext(input)
        val formatted = engine.formatForGemini(results)

        assertTrue(formatted.contains("=== FAKTEN ===\nKeine enzyklopädischen Fakten verfügbar."))
        assertTrue(formatted.contains("=== REISEKONTEXT ===\nKein Reisekontext verfügbar."))
        assertTrue(formatted.contains("=== GOOGLE MAPS BASISDATEN ==="))
        assertTrue(formatted.contains("Ort: Greenhouse Community Space"))
        assertTrue(formatted.contains("Kategorie: Community Center"))
        assertTrue(formatted.contains("Adresse: Chiang Mai, Thailand"))
    }
}
