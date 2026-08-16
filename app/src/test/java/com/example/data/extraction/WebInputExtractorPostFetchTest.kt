package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebInputExtractorPostFetchTest {

    private val extractor = WebInputExtractor()

    @Test
    fun testGoogleMapsUrlSetsPlaceConfirmedProfile() = runBlocking {
        val url = "https://google.com/maps/place/Berlin"
        val result = extractor.extract(
            rawUrl = url,
            normalizedUrl = url,
            directContent = null,
            analysisType = AnalysisType.WEB_SUMMARY,
            freeQuery = null,
            analysisId = "test-123"
        )

        assertTrue(result is ContentExtractionResult.Success)
        val success = result as ContentExtractionResult.Success
        val profile = success.content.confirmedProfile

        assertNotNull("confirmedProfile should not be null", profile)
        assertEquals(SourceProfile.SourceType.PLACE, profile?.sourceType)
        assertEquals(SourcePlatform.GOOGLE_MAPS, profile?.platform)
        assertTrue(profile?.isPostFetchConfirmed == true)
        assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.PLACE_CONTEXT))
    }

    @Test
    fun testGoogleMapsShortUrlSetsPlaceConfirmedProfile() = runBlocking {
        val url = "https://maps.app.goo.gl/xyz123"
        val result = extractor.extract(
            rawUrl = url,
            normalizedUrl = url,
            directContent = null,
            analysisType = AnalysisType.WEB_SUMMARY,
            freeQuery = null,
            analysisId = "test-456"
        )

        assertTrue(result is ContentExtractionResult.Success)
        val success = result as ContentExtractionResult.Success
        val profile = success.content.confirmedProfile

        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.PLACE, profile?.sourceType)
        assertEquals(SourcePlatform.GOOGLE_MAPS, profile?.platform)
        assertTrue(profile?.isPostFetchConfirmed == true)
        assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.PLACE_CONTEXT))
    }
}

