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
            featureId = "0x0",
            placeName = "SomePlace",
            address = null,
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

    @Test
    fun testBuildQueryTextHappyFrog() {
        val mockResult = GoogleMapsPoCResult(
            originalSharedText = "url", extractedUrl = "url", resolvedUrl = "url",
            detectedUrlType = "LONG_MAPS", placeId = null, cid = null, featureId = null,
            placeName = "The Happy Frog",
            address = "One Nimman, One Nimman, Nimmanahaeminda Road Nimmanhaemin Tambon Su Thep, Mueang Chiang Mai District, Chiang Mai 50200",
            searchQuery = null, latitude = null, longitude = null, zoom = null,
            resolutionStatus = "SUCCESS", warnings = emptyList()
        )
        val query = PlacesApiService.buildQueryText(mockResult)
        assertEquals("The Happy Frog One Nimman, One Nimman, Nimmanahaeminda Road Nimmanhaemin Tambon Su Thep, Mueang Chiang Mai District, Chiang Mai 50200", query)
    }

    @Test
    fun testBuildQueryTextPorjaiMassage() {
        val mockResult = GoogleMapsPoCResult(
            originalSharedText = "url", extractedUrl = "url", resolvedUrl = "url",
            detectedUrlType = "LONG_MAPS", placeId = null, cid = null, featureId = null,
            placeName = "Porjai Massage",
            address = "Soi 8 Tambon Si Phum, Mueang Chiang Mai District, Chiang Mai 50200",
            searchQuery = null, latitude = null, longitude = null, zoom = null,
            resolutionStatus = "SUCCESS", warnings = emptyList()
        )
        val query = PlacesApiService.buildQueryText(mockResult)
        assertEquals("Porjai Massage Soi 8 Tambon Si Phum, Mueang Chiang Mai District, Chiang Mai 50200", query)
    }

    @Test
    fun testBuildQueryTextFallbackToSearchQuery() {
        // Fallback to searchQuery when placeName is missing
        val mockResult = GoogleMapsPoCResult(
            originalSharedText = "url", extractedUrl = "url", resolvedUrl = "url",
            detectedUrlType = "SEARCH_MAPS", placeId = null, cid = null, featureId = null,
            placeName = null,
            address = null,
            searchQuery = "Kölner Dom", latitude = null, longitude = null, zoom = null,
            resolutionStatus = "SUCCESS", warnings = emptyList()
        )
        val query = PlacesApiService.buildQueryText(mockResult)
        assertEquals("Kölner Dom", query)
    }

    @Test
    fun testBuildQueryTextOnlyPlaceName() {
        val mockResult = GoogleMapsPoCResult(
            originalSharedText = "url", extractedUrl = "url", resolvedUrl = "url",
            detectedUrlType = "LONG_MAPS", placeId = null, cid = null, featureId = null,
            placeName = "SomePlace",
            address = null,
            searchQuery = "Ignored Query", latitude = null, longitude = null, zoom = null,
            resolutionStatus = "SUCCESS", warnings = emptyList()
        )
        val query = PlacesApiService.buildQueryText(mockResult)
        assertEquals("SomePlace", query)
    }
}
