package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceProfileContractTest {

    @Test
    fun testExtractedContentWithoutConfirmedProfileDefaultsToNull() {
        val extracted = ExtractedContent(
            sourceType = SourceType.WEB,
            rawText = "https://example.com",
            enrichedText = "Article text"
        )

        assertNull("confirmedProfile should default to null for backwards compatibility", extracted.confirmedProfile)
    }

    @Test
    fun testExtractedContentWithConfirmedProfile() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val extracted = ExtractedContent(
            sourceType = SourceType.WEB,
            rawText = "https://example.com",
            enrichedText = "Article text",
            confirmedProfile = profile
        )

        assertEquals(profile, extracted.confirmedProfile)
        assertTrue(extracted.confirmedProfile?.isPostFetchConfirmed == true)
        assertTrue(extracted.confirmedProfile?.isAvailable(SourceCapability.PAGE_ARTICLE_TEXT) == true)
    }

    @Test
    fun testSourceProfileHelpers() {
        val initialProfile = SourceProfile(
            sourceType = SourceProfile.SourceType.DOCUMENT,
            platform = SourcePlatform.LOCAL_FILE,
            rawInput = "/path/to/doc.pdf",
            capabilities = mapOf(
                SourceCapability.DOCUMENT_TEXT to CapabilityState(
                    capability = SourceCapability.DOCUMENT_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            ),
            isPostFetchConfirmed = false
        )

        assertFalse(initialProfile.isPostFetchConfirmed)
        assertFalse(initialProfile.isAvailable(SourceCapability.DOCUMENT_TEXT))
        assertTrue(initialProfile.isPotential(SourceCapability.DOCUMENT_TEXT))

        val confirmedProfile = initialProfile
            .withCapabilityStatus(SourceCapability.DOCUMENT_TEXT, CapabilityStatus.AVAILABLE)
            .withPostFetchConfirmed()

        assertTrue(confirmedProfile.isPostFetchConfirmed)
        assertTrue(confirmedProfile.isAvailable(SourceCapability.DOCUMENT_TEXT))
        assertFalse(confirmedProfile.isPotential(SourceCapability.DOCUMENT_TEXT))
    }

    @Test
    fun testYouTubeDegradedModelability() {
        val preFetchProfile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=123",
            capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(SourceCapability.VIDEO_METADATA, CapabilityStatus.POTENTIAL),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(SourceCapability.TRANSCRIPT_TEXT, CapabilityStatus.POTENTIAL)
            ),
            isPostFetchConfirmed = false
        )

        // Simulate post-fetch YouTube fetch where metadata succeeded but transcript failed
        val postFetchProfile = preFetchProfile
            .withCapabilityStatus(SourceCapability.VIDEO_METADATA, CapabilityStatus.AVAILABLE)
            .withCapabilityStatus(SourceCapability.TRANSCRIPT_TEXT, CapabilityStatus.FAILED, "No transcript available")
            .withPostFetchConfirmed()

        assertTrue(postFetchProfile.isPostFetchConfirmed)
        assertTrue(postFetchProfile.isAvailable(SourceCapability.VIDEO_METADATA))
        assertFalse(postFetchProfile.isAvailable(SourceCapability.TRANSCRIPT_TEXT))
        assertTrue(postFetchProfile.isUnavailable(SourceCapability.TRANSCRIPT_TEXT))
        assertEquals(CapabilityStatus.FAILED, postFetchProfile.getStatus(SourceCapability.TRANSCRIPT_TEXT))
    }
}
