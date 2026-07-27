package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlacesApiServiceTest {

    @Test
    fun testGetApiKeySecurityAndFallback() {
        val key = PlacesApiService.getApiKey()
        if (key != null) {
            assertFalse(key.contains("MY_PLACES_API_KEY"))
            assertFalse(key.contains("MY_GEMINI_KEY"))
            assertFalse(key.contains("MY_GEMINI_API_KEY"))
        }
    }

    @Test
    fun testPerformStufe1AnalysisApiKeyMissing() {
        val mockMapsResult = GoogleMapsPoCResult(
            originalSharedText = "https://maps.app.goo.gl/test",
            extractedUrl = "https://maps.app.goo.gl/test",
            resolvedUrl = "https://www.google.com/maps/place/SomePlace/data=!3m1!1s0x0:0x123456789abc",
            detectedUrlType = "LONG_MAPS",
            placeId = null,
            cid = "1311768467294834428",
            placeName = "SomePlace",
            searchQuery = null,
            latitude = 50.0,
            longitude = 10.0,
            zoom = 15.0,
            resolutionStatus = "SUCCESS",
            warnings = emptyList()
        )

        val placesResult = PlacesApiService.performStufe1Analysis(
            originalText = "https://maps.app.goo.gl/test",
            mapsResult = mockMapsResult
        )

        assertNotNull(placesResult)
        assertEquals("https://maps.app.goo.gl/test", placesResult.originalSharedUrl)
        assertEquals("SomePlace", placesResult.urlDerivedName)
        assertEquals(50.0, placesResult.latitude!!, 0.001)
        assertEquals(10.0, placesResult.longitude!!, 0.001)
        
        if (placesResult.apiStatus == "API_KEY_MISSING") {
            assertTrue(placesResult.warnings.any { it.contains("fehlt oder ist nicht konfiguriert") })
        }
    }
}
