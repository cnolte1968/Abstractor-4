package com.example.domain.usecase

import com.example.domain.model.CapabilityStatus
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceResolverTest {

    private val resolver = SourceResolver()

    @Test
    fun testYouTubeWatchUrl() {
        val input = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val profile = resolver.resolvePreFetchProfile(input)

        assertEquals(SourceProfile.SourceType.VIDEO, profile.sourceType)
        assertEquals(SourcePlatform.YOUTUBE, profile.platform)
        assertEquals(input, profile.normalizedUrl)
        assertFalse(profile.isPostFetchConfirmed)

        assertEquals(CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.VIDEO_METADATA))
        assertEquals(CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.TRANSCRIPT_TEXT))
        assertTrue(profile.isPotential(SourceCapability.VIDEO_METADATA))
        assertTrue(profile.isPotential(SourceCapability.TRANSCRIPT_TEXT))
    }

    @Test
    fun testYouTubeShortsAndShortLink() {
        val shortLinkInput = "youtu.be/dQw4w9WgXcQ"
        val profile1 = resolver.resolvePreFetchProfile(shortLinkInput)

        assertEquals(SourceProfile.SourceType.VIDEO, profile1.sourceType)
        assertEquals(SourcePlatform.YOUTUBE, profile1.platform)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", profile1.normalizedUrl)
        assertEquals(CapabilityStatus.POTENTIAL, profile1.getStatus(SourceCapability.TRANSCRIPT_TEXT))

        val shortsInput = "https://youtube.com/shorts/abc123xyz"
        val profile2 = resolver.resolvePreFetchProfile(shortsInput)

        assertEquals(SourceProfile.SourceType.VIDEO, profile2.sourceType)
        assertEquals(SourcePlatform.YOUTUBE, profile2.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profile2.getStatus(SourceCapability.VIDEO_METADATA))
    }

    @Test
    fun testTikTokUrl() {
        val input = "https://www.tiktok.com/@user/video/1234567890"
        val profile = resolver.resolvePreFetchProfile(input)

        assertEquals(SourceProfile.SourceType.VIDEO, profile.sourceType)
        assertEquals(SourcePlatform.TIKTOK, profile.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.VIDEO_METADATA))
    }

    @Test
    fun testInstagramAndFacebookUrl() {
        val instaInput = "https://www.instagram.com/p/C1234567/"
        val profileInsta = resolver.resolvePreFetchProfile(instaInput)

        assertEquals(SourceProfile.SourceType.WEB_PAGE, profileInsta.sourceType)
        assertEquals(SourcePlatform.INSTAGRAM, profileInsta.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profileInsta.getStatus(SourceCapability.PAGE_ARTICLE_TEXT))

        val fbInput = "https://www.facebook.com/watch/?v=987654321"
        val profileFb = resolver.resolvePreFetchProfile(fbInput)

        assertEquals(SourceProfile.SourceType.WEB_PAGE, profileFb.sourceType)
        assertEquals(SourcePlatform.FACEBOOK, profileFb.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profileFb.getStatus(SourceCapability.VIDEO_METADATA))
    }

    @Test
    fun testGoogleMapsUrl() {
        val mapsInput1 = "https://maps.google.com/?q=Berlin"
        val profile1 = resolver.resolvePreFetchProfile(mapsInput1)

        assertEquals(SourceProfile.SourceType.PLACE, profile1.sourceType)
        assertEquals(SourcePlatform.GOOGLE_MAPS, profile1.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profile1.getStatus(SourceCapability.PLACE_CONTEXT))

        val mapsInput2 = "https://maps.app.goo.gl/xyz123"
        val profile2 = resolver.resolvePreFetchProfile(mapsInput2)

        assertEquals(SourceProfile.SourceType.PLACE, profile2.sourceType)
        assertEquals(SourcePlatform.GOOGLE_MAPS, profile2.platform)
    }

    @Test
    fun testDocumentUrlAndLocalFile() {
        val pdfInput = "https://example.com/reports/document.pdf?version=1"
        val profilePdf = resolver.resolvePreFetchProfile(pdfInput)

        assertEquals(SourceProfile.SourceType.DOCUMENT, profilePdf.sourceType)
        assertEquals(SourcePlatform.WEB, profilePdf.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profilePdf.getStatus(SourceCapability.DOCUMENT_TEXT))

        val localInput = "file:///storage/emulated/0/Download/test.docx"
        val profileLocal = resolver.resolvePreFetchProfile(localInput)

        assertEquals(SourceProfile.SourceType.DOCUMENT, profileLocal.sourceType)
        assertEquals(SourcePlatform.LOCAL_FILE, profileLocal.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profileLocal.getStatus(SourceCapability.DOCUMENT_TEXT))
    }

    @Test
    fun testStandardWebPage() {
        val webInput = "https://news.ycombinator.com"
        val profile = resolver.resolvePreFetchProfile(webInput)

        assertEquals(SourceProfile.SourceType.WEB_PAGE, profile.sourceType)
        assertEquals(SourcePlatform.WEB, profile.platform)
        assertEquals(CapabilityStatus.POTENTIAL, profile.getStatus(SourceCapability.PAGE_ARTICLE_TEXT))
    }

    @Test
    fun testRawText() {
        val rawTextInput = "Das ist ein allgemeiner deutscher Text ohne URL, der direkt analysiert werden soll."
        val profile = resolver.resolvePreFetchProfile(rawTextInput)

        assertEquals(SourceProfile.SourceType.RAW_TEXT, profile.sourceType)
        assertEquals(SourcePlatform.UNKNOWN, profile.platform)
        assertNull(profile.normalizedUrl)
        assertEquals(CapabilityStatus.AVAILABLE, profile.getStatus(SourceCapability.RAW_TEXT))
        assertTrue(profile.isAvailable(SourceCapability.RAW_TEXT))
    }

    @Test
    fun testEmptyAndBlankInput() {
        val emptyInput = "   "
        val profile = resolver.resolvePreFetchProfile(emptyInput)

        assertEquals(SourceProfile.SourceType.UNKNOWN, profile.sourceType)
        assertEquals(SourcePlatform.UNKNOWN, profile.platform)
        assertNull(profile.normalizedUrl)
        assertTrue(profile.capabilities.isEmpty())
    }
}
