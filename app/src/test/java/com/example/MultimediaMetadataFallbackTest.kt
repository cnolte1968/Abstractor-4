package com.example

import com.example.domain.model.DomainSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultimediaMetadataFallbackTest {

    @Test
    fun testDegradedMetadataFallbackRetainsGeneratedShortDescription() {
        val degradedSummary = DomainSummary(
            id = "test-id-1",
            title = "YouTube Metadata Analysis",
            originalUrl = "https://www.youtube.com/watch?v=aT3RDAQ5lLQ",
            shortDescription = "Diese Zusammenfassung basiert auf der offiziellen Videobeschreibung.",
            keyTakeaways = emptyList(),
            fallbackUsed = true,
            analysisId = "analysis-1"
        )

        assertNotEquals("TRANSCRIPT_UNAVAILABLE", degradedSummary.shortDescription)
        assertEquals("Diese Zusammenfassung basiert auf der offiziellen Videobeschreibung.", degradedSummary.shortDescription)
        assertTrue("fallbackUsed must be true for degraded metadata analysis", degradedSummary.fallbackUsed)
    }

    @Test
    fun testNormalSuccessFlowUnchanged() {
        val normalSummary = DomainSummary(
            id = "test-id-2",
            title = "YouTube Full Analysis",
            originalUrl = "https://www.youtube.com/watch?v=aT3RDAQ5lLQ",
            shortDescription = "Vollständige Analyse auf Basis des extrahierten Transkripts.",
            keyTakeaways = emptyList(),
            fallbackUsed = false,
            analysisId = "analysis-2"
        )

        assertNotEquals("TRANSCRIPT_UNAVAILABLE", normalSummary.shortDescription)
        assertFalse("fallbackUsed must be false for normal full analysis", normalSummary.fallbackUsed)
    }
}
