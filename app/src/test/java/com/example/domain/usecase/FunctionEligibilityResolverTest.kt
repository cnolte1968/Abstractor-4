package com.example.domain.usecase

import com.example.data.AnalysisType
import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EligibilityStatus
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionEligibilityResolverTest {

    private val resolver = FunctionEligibilityResolver()

    @Test
    fun testAvailableRequiredCapabilityResultsInEligible() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com/article",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.PAGE_ARTICLE_TEXT)
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
        assertTrue(result.missingCapabilities.isEmpty())
    }

    @Test
    fun testPotentialRequiredCapabilityResultsInPotentialAndNotEligible() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com/article",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            ),
            isPostFetchConfirmed = false
        )

        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.PAGE_ARTICLE_TEXT)
        )

        assertEquals(EligibilityStatus.POTENTIAL, result.status)
        assertNotEquals(EligibilityStatus.ELIGIBLE, result.status)
        assertTrue(profile.isPotential(SourceCapability.PAGE_ARTICLE_TEXT))
        assertNotEquals(true, profile.isAvailable(SourceCapability.PAGE_ARTICLE_TEXT))
    }

    @Test
    fun testDegradedRequiredCapabilityResultsInDegraded() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=123",
            capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.DEGRADED,
                    detailMessage = "Metadata only available"
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.VIDEO_METADATA)
        )

        assertEquals(EligibilityStatus.DEGRADED, result.status)
    }

    @Test
    fun testUnavailableRequiredCapabilityResultsInIneligibleMissingCapability() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=123",
            capabilities = mapOf(
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.TRANSCRIPT_TEXT)
        )

        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, result.status)
        assertTrue(result.missingCapabilities.contains(SourceCapability.TRANSCRIPT_TEXT))
    }

    @Test
    fun testFailedRequiredCapabilityResultsInIneligibleFailed() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.DOCUMENT,
            platform = SourcePlatform.LOCAL_FILE,
            rawInput = "/path/to/doc.pdf",
            capabilities = mapOf(
                SourceCapability.DOCUMENT_TEXT to CapabilityState(
                    capability = SourceCapability.DOCUMENT_TEXT,
                    status = CapabilityStatus.FAILED,
                    detailMessage = "PDF parsing error"
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.DOCUMENT_SUMMARY,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.DOCUMENT_TEXT)
        )

        assertEquals(EligibilityStatus.INELIGIBLE_FAILED, result.status)
        assertTrue(result.missingCapabilities.contains(SourceCapability.DOCUMENT_TEXT))
    }

    @Test
    fun testMultipleAlternativeCapabilitiesOneAvailableIsEnough() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com/article",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                ),
                SourceCapability.RAW_TEXT to CapabilityState(
                    capability = SourceCapability.RAW_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredAlternativeGroups = listOf(
                setOf(SourceCapability.PAGE_ARTICLE_TEXT),
                setOf(SourceCapability.RAW_TEXT)
            )
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
    }

    @Test
    fun testYoutubePreparationVideoMetadataAvailableAndTranscriptUnavailableResultsInEligibleWhenMetadataAllowed() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=xyz",
            capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.AVAILABLE
                ),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profile,
            requiredAlternativeGroups = listOf(
                setOf(SourceCapability.TRANSCRIPT_TEXT),
                setOf(SourceCapability.VIDEO_METADATA)
            )
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)

        val degradedProfile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=xyz",
            capabilities = mapOf(
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.DEGRADED,
                    detailMessage = "Transcript unavailable, fallback to video title and description"
                )
            ),
            isPostFetchConfirmed = true
        )

        val degradedResult = resolver.resolveEligibility(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = degradedProfile,
            requiredCapabilities = setOf(SourceCapability.TRANSCRIPT_TEXT)
        )

        assertEquals(EligibilityStatus.DEGRADED, degradedResult.status)
    }

    @Test
    fun testOrSemanticsPageArticleTextAvailableIsEnough() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            setOf(SourceCapability.RAW_TEXT),
            setOf(SourceCapability.DOCUMENT_TEXT),
            setOf(SourceCapability.TRANSCRIPT_TEXT)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
    }

    @Test
    fun testOrSemanticsRawTextAvailableIsEnough() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.RAW_TEXT,
            platform = SourcePlatform.UNKNOWN,
            rawInput = "Direct text input",
            capabilities = mapOf(
                SourceCapability.RAW_TEXT to CapabilityState(
                    capability = SourceCapability.RAW_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            setOf(SourceCapability.RAW_TEXT),
            setOf(SourceCapability.DOCUMENT_TEXT),
            setOf(SourceCapability.TRANSCRIPT_TEXT)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.KEY_TAKEAWAYS,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
    }

    @Test
    fun testOrSemanticsTranscriptTextPotentialResultsInPotential() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=abc",
            capabilities = mapOf(
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            )
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            setOf(SourceCapability.RAW_TEXT),
            setOf(SourceCapability.DOCUMENT_TEXT),
            setOf(SourceCapability.TRANSCRIPT_TEXT)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.POTENTIAL, result.status)
    }

    @Test
    fun testOrSemanticsVideoMetadataDegradedResultsInDegraded() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=abc",
            capabilities = mapOf(
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                ),
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.DEGRADED
                )
            )
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.TRANSCRIPT_TEXT),
            setOf(SourceCapability.VIDEO_METADATA)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.DEGRADED, result.status)
    }

    @Test
    fun testAndSemanticsInsideGroupBothCapabilitiesRequired() {
        val profileMissingOne = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=abc",
            capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.AVAILABLE
                ),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                )
            )
        )

        // Group requires BOTH VIDEO_METADATA AND TRANSCRIPT_TEXT together
        val strictGroup = listOf(
            setOf(SourceCapability.VIDEO_METADATA, SourceCapability.TRANSCRIPT_TEXT)
        )

        val resultMissingOne = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profileMissingOne,
            requiredAlternativeGroups = strictGroup
        )

        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, resultMissingOne.status)
        assertTrue(resultMissingOne.missingCapabilities.contains(SourceCapability.TRANSCRIPT_TEXT))

        val profileHasBoth = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=abc",
            capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.AVAILABLE
                ),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
        )

        val resultHasBoth = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            sourceProfile = profileHasBoth,
            requiredAlternativeGroups = strictGroup
        )

        assertEquals(EligibilityStatus.ELIGIBLE, resultHasBoth.status)
    }

    @Test
    fun testFailureNoAlternativeAvailableResultsInIneligibleMissing() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.UNKNOWN,
            platform = SourcePlatform.UNKNOWN,
            rawInput = "",
            capabilities = emptyMap()
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            setOf(SourceCapability.RAW_TEXT)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, result.status)
        assertTrue(result.missingCapabilities.contains(SourceCapability.PAGE_ARTICLE_TEXT))
        assertTrue(result.missingCapabilities.contains(SourceCapability.RAW_TEXT))
    }

    @Test
    fun testFailureRelevantAlternativeFailedResultsInIneligibleFailed() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.DOCUMENT,
            platform = SourcePlatform.LOCAL_FILE,
            rawInput = "/doc.pdf",
            capabilities = mapOf(
                SourceCapability.DOCUMENT_TEXT to CapabilityState(
                    capability = SourceCapability.DOCUMENT_TEXT,
                    status = CapabilityStatus.FAILED,
                    detailMessage = "PDF error"
                ),
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                )
            )
        )

        val alternativeGroups = listOf(
            setOf(SourceCapability.DOCUMENT_TEXT),
            setOf(SourceCapability.PAGE_ARTICLE_TEXT)
        )

        val result = resolver.resolveEligibilityWithAlternatives(
            analysisType = AnalysisType.DOCUMENT_SUMMARY,
            sourceProfile = profile,
            requiredAlternativeGroups = alternativeGroups
        )

        assertEquals(EligibilityStatus.INELIGIBLE_FAILED, result.status)
        assertTrue(result.missingCapabilities.contains(SourceCapability.DOCUMENT_TEXT))
    }

    @Test
    fun testAllowedSourceTypesRejectsDisallowedSource() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://youtube.com/watch?v=abc",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
        )
        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            allowedSourceTypes = setOf(SourceProfile.SourceType.WEB_PAGE, SourceProfile.SourceType.RAW_TEXT)
        )
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, result.status)
        assertTrue(result.disabledReason?.contains("not supported") == true)
    }

    @Test
    fun testAllowedSourceTypesAcceptsAllowedSource() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = "https://example.com",
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
        )
        val result = resolver.resolveEligibility(
            analysisType = AnalysisType.WEB_SUMMARY,
            sourceProfile = profile,
            requiredCapabilities = setOf(SourceCapability.PAGE_ARTICLE_TEXT),
            allowedSourceTypes = setOf(SourceProfile.SourceType.WEB_PAGE, SourceProfile.SourceType.RAW_TEXT)
        )
        assertEquals(EligibilityStatus.ELIGIBLE, result.status)
    }
}
