package com.example.domain.usecase

import com.example.domain.model.CapabilityStatus
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceResolverMapsTest {

    private val resolver = SourceResolver()

    @Test
    fun testGoogleMapsUrlPatterns() {
        val testCases = listOf(
            "https://google.com/maps/place/Berlin",
            "https://google.de/maps/place/München",
            "https://maps.google.com/maps?q=Berlin",
            "https://maps.google.de/maps?q=München",
            "https://maps.app.goo.gl/xyz123",
            "https://goo.gl/maps/abc123xyz"
        )

        for (url in testCases) {
            val profile = resolver.resolvePreFetchProfile(url)
            assertEquals("URL $url should be PLACE", SourceProfile.SourceType.PLACE, profile.sourceType)
            assertEquals("URL $url should be GOOGLE_MAPS", SourcePlatform.GOOGLE_MAPS, profile.platform)
            assertEquals("URL $url should have PLACE_CONTEXT", CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.PLACE_CONTEXT))
        }
    }

    @Test
    fun testNormalWebUrlIsNotMaps() {
        val input = "https://www.google.com/search?q=berlin+wetter"
        val profile = resolver.resolvePreFetchProfile(input)
        
        assertNotEquals("Should not be PLACE", SourceProfile.SourceType.PLACE, profile.sourceType)
        assertEquals(SourceProfile.SourceType.WEB_PAGE, profile.sourceType)
        assertEquals(SourcePlatform.WEB, profile.platform)
        // Ensure no PLACE_CONTEXT
        assertEquals(CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.PAGE_ARTICLE_TEXT))
    }
}
