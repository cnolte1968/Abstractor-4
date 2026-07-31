package com.example

import com.example.data.GoogleMapsUrlParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleMapsUrlParserTest {

    @Test
    fun testIsGoogleMapsUrlBasic() {
        // maps.app.goo.gl-Kurzlink
        assertTrue(GoogleMapsUrlParser.isGoogleMapsUrl("https://maps.app.goo.gl/abcdefg"))
        // lange google.com/maps/place-URL
        assertTrue(GoogleMapsUrlParser.isGoogleMapsUrl("https://www.google.com/maps/place/Brandenburger+Tor/"))
        // maps.google.com/?q=-URL
        assertTrue(GoogleMapsUrlParser.isGoogleMapsUrl("https://maps.google.com/?q=Berlin"))
        assertTrue(GoogleMapsUrlParser.isGoogleMapsUrl("https://maps.google.de/?q=Koeln"))
    }

    @Test
    fun testIsGoogleMapsUrlSecurity() {
        // http statt https (Requirement: HTTPS only)
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("http://maps.app.goo.gl/abcdefg"))
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("http://www.google.com/maps"))

        // google.com.evil.example (manipulierte Domain)
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("https://google.com.evil.example/maps"))
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("https://maps.google.com.evil.example/place"))

        // evil.example/google.com/maps (fremde Domain mit google im Pfad)
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("https://evil.example/google.com/maps"))

        // normaler Nicht-Maps-Link
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("https://www.google.com/search?q=maps"))
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("https://example.com/"))
        assertFalse(GoogleMapsUrlParser.isGoogleMapsUrl("not_a_url"))
    }

    @Test
    fun testIsSafeHost() {
        assertTrue(GoogleMapsUrlParser.isSafeHost("maps.google.com"))
        assertTrue(GoogleMapsUrlParser.isSafeHost("google.de"))

        // localhost & loopbacks
        assertFalse(GoogleMapsUrlParser.isSafeHost("localhost"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("127.0.0.1"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("127.0.0.2"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("::1"))

        // Private IPv4 networks
        assertFalse(GoogleMapsUrlParser.isSafeHost("10.0.0.1"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("172.16.2.3"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("192.168.1.1"))

        // Link-local IPv4
        assertFalse(GoogleMapsUrlParser.isSafeHost("169.254.1.2"))

        // Private / Link-local IPv6
        assertFalse(GoogleMapsUrlParser.isSafeHost("fc00::1"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("fd12:3456:789a:1::1"))
        assertFalse(GoogleMapsUrlParser.isSafeHost("fe80::1"))
    }

    @Test
    fun testDetectUrlType() {
        assertEquals("SHORT_LINK", GoogleMapsUrlParser.detectUrlType("https://maps.app.goo.gl/abcdefg"))
        assertEquals("LONG_MAPS", GoogleMapsUrlParser.detectUrlType("https://www.google.com/maps/place/Brandenburger+Tor/"))
        assertEquals("SEARCH_MAPS", GoogleMapsUrlParser.detectUrlType("https://maps.google.com/?q=Berlin"))
        assertEquals("UNKNOWN", GoogleMapsUrlParser.detectUrlType("https://maps.google.com/"))
    }

    @Test
    fun testParseLongPlaceUrl() {
        val url = "https://www.google.com/maps/place/Brandenburger+Tor/@52.5162746,13.377704,17z/data=!3m1!4b1!4m6!3m5!1s0x47a851c64013741d:0x25a07c088ef39144!8m2!3d52.5162746!4d13.377704!16s%2Fm%2F01p30"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl(
            originalText = "Schau hier: $url",
            url = url,
            resolvedUrl = url,
            resolutionStatus = "SUCCESS"
        )
        
        assertEquals("Brandenburger Tor", result.placeName)
        assertEquals(52.5162746, result.latitude!!, 1e-5)
        assertEquals(13.377704, result.longitude!!, 1e-5)
        assertEquals(17.0, result.zoom!!, 0.1)
        
        // Hex CID extracted from 0x25a07c088ef39144 is 2711303351876948292
        assertEquals("2711303351876948292", result.cid)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun testParseQueryPlaceIdAndCid() {
        val url = "https://maps.google.com/?query_place_id=ChIJ7X8T4bT4uEcR_ABCDEF&cid=987654321012345"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl(
            originalText = url,
            url = url,
            resolvedUrl = url,
            resolutionStatus = "SUCCESS"
        )
        
        assertEquals("ChIJ7X8T4bT4uEcR_ABCDEF", result.placeId)
        assertEquals("987654321012345", result.cid)
        assertNull(result.latitude)
        assertNull(result.longitude)
    }

    @Test
    fun testParsePureCoordinatesUrl() {
        val url = "https://maps.google.com/?q=52.5162,13.3777&z=15"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl(
            originalText = url,
            url = url,
            resolvedUrl = url,
            resolutionStatus = "SUCCESS"
        )
        
        assertEquals(52.5162, result.latitude!!, 1e-4)
        assertEquals(13.3777, result.longitude!!, 1e-4)
        assertEquals(15.0, result.zoom!!, 0.1)
        assertNull(result.searchQuery) // Ignored coords inside query parameter q as searchQuery
    }

    @Test
    fun testParseSearchQueryAndUrlEncoding() {
        // URL-Encoding test
        val url = "https://maps.google.com/?q=K%C3%B6lner+Dom"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl(
            originalText = url,
            url = url,
            resolvedUrl = url,
            resolutionStatus = "SUCCESS"
        )
        
        assertEquals("Kölner Dom", result.searchQuery)
        assertNull(result.latitude)
    }

    @Test
    fun testParseInvalidGoogleMapsUrlAndMissingIdentifications() {
        // ungültige URL / fehlende Identifikationsmerkmale
        val url = "https://www.google.com/maps/invalid/format"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl(
            originalText = url,
            url = url,
            resolvedUrl = url,
            resolutionStatus = "SUCCESS"
        )
        
        assertNull(result.placeId)
        assertNull(result.cid)
        assertNull(result.latitude)
        assertEquals("EXTRACTION_FAILED", result.resolutionStatus)
        assertTrue(result.warnings.contains("Keine Identifikationsmerkmale extrahierbar"))
    }

    @Test
    fun testParseHappyFrogUrl() {
        val url = "https://www.google.com/maps/place/%22The+Happy+Frog%22*One+Nimman,+One+Nimman,+Nimmanahaeminda+Road+Nimmanhaemin+Tambon+Su+Thep,+Mueang+Chiang+Mai+District,+Chiang+Mai+50200/data=!4m2!3m1!1s0x30da3b0729e8e51f:0x196dfc6ac7f60407"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl("text", url, url, "SUCCESS")
        
        assertEquals("The Happy Frog", result.placeName)
        assertEquals("One Nimman, One Nimman, Nimmanahaeminda Road Nimmanhaemin Tambon Su Thep, Mueang Chiang Mai District, Chiang Mai 50200", result.address)
        assertEquals("30da3b0729e8e51f", result.featureId)
        assertEquals("1832398158961181703", result.cid) // hex 196dfc6ac7f60407
    }

    @Test
    fun testParsePorjaiMassageUrl() {
        val url = "https://www.google.com/maps/place/Porjai+Massage*Soi+8+Tambon+Si+Phum,+Mueang+Chiang+Mai+District,+Chiang+Mai+50200/data=!4m2!3m1!1s0x30da3b10c0e5a873:0x1234567890abcdef"
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl("text", url, url, "SUCCESS")
        
        assertEquals("Porjai Massage", result.placeName)
        assertEquals("Soi 8 Tambon Si Phum, Mueang Chiang Mai District, Chiang Mai 50200", result.address)
        assertEquals("30da3b10c0e5a873", result.featureId)
    }

    @Test
    fun testResolveShortUrlValidation() {
        // http URL
        val httpRes = GoogleMapsUrlParser.resolveShortUrl("http://maps.app.goo.gl/abcdef")
        assertEquals("NOT_A_MAPS_URL", httpRes.second)

        // Localhost SSRF
        val localhostRes = GoogleMapsUrlParser.resolveShortUrl("https://localhost/maps")
        assertEquals("NOT_A_MAPS_URL", localhostRes.second) // fails isGoogleMapsUrl

        // Non-maps URL
        val nonMapsRes = GoogleMapsUrlParser.resolveShortUrl("https://example.com")
        assertEquals("NOT_A_MAPS_URL", nonMapsRes.second)
    }

    @Test
    fun testShareIntentGoogleMapsUpdatesSharedUrlWithoutAutoRun() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val viewModel = com.example.ui.MainViewModel()
        viewModel.initIfNeeded(context)

        val testMapsUrl = "https://maps.app.goo.gl/Dmv1wmRazyu1hyacA"
        
        // Ensure sharedUrlToFill is initially empty and uiState is Idle
        assertTrue(viewModel.sharedUrlToFill.value.isEmpty())
        assertTrue(viewModel.uiState.value is com.example.ui.UiState.Idle)

        // Process shared text mimicking the Share Intent
        viewModel.setSharedText(testMapsUrl)

        // Verify the URL was successfully put into sharedUrlToFill
        assertEquals(testMapsUrl, viewModel.sharedUrlToFill.value)

        // Verify it DID NOT auto-run the analysis (it should remain Idle, not Loading or Success)
        assertTrue(viewModel.uiState.value is com.example.ui.UiState.Idle)
    }
}
