package com.example.ui.metadata

import com.example.data.AnalysisType
import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EligibilityStatus
import com.example.domain.model.FunctionEligibility
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import com.example.domain.usecase.FunctionEligibilityResolver
import com.example.domain.usecase.SourceResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeatureCatalogEligibilityTest {

    private lateinit var sourceResolver: SourceResolver
    private lateinit var eligibilityResolver: FunctionEligibilityResolver

    @Before
    fun setUp() {
        sourceResolver = SourceResolver()
        eligibilityResolver = FunctionEligibilityResolver()
    }

    private fun getFeature(functionId: String): FeatureMetadata {
        return FeatureCatalog.features.first { it.functionId == functionId }
    }

    private fun evaluateEligibility(feature: FeatureMetadata, profile: SourceProfile): FunctionEligibility {
        val analysisType = requireNotNull(feature.analysisType) { "Feature ${feature.functionId} has no AnalysisType" }
        return if (feature.requiredAlternativeGroups.isNotEmpty()) {
            eligibilityResolver.resolveEligibilityWithAlternatives(
                analysisType = analysisType,
                sourceProfile = profile,
                requiredAlternativeGroups = feature.requiredAlternativeGroups,
                optionalCapabilities = feature.optionalCapabilities,
                allowedSourceTypes = feature.allowedSourceTypes
            )
        } else {
            eligibilityResolver.resolveEligibility(
                analysisType = analysisType,
                sourceProfile = profile,
                requiredCapabilities = feature.requiredCapabilities,
                optionalCapabilities = feature.optionalCapabilities,
                allowedSourceTypes = feature.allowedSourceTypes
            )
        }
    }

    @Test
    fun testWebUrlResultsInGeneralTextFunctionsPotentialAndMapsIneligible() {
        val profile = sourceResolver.resolvePreFetchProfile("https://example.com/article")
        
        val webSummary = getFeature("WEB_SUMMARY")
        val keyTakeaways = getFeature("KEY_TAKEAWAYS")
        val freeQuery = getFeature("FREE_SOURCE_QUERY")
        val mapsAnalyzer = getFeature("GOOGLE_MAPS_ANALYZER")

        val webSummaryEligibility = evaluateEligibility(webSummary, profile)
        val keyTakeawaysEligibility = evaluateEligibility(keyTakeaways, profile)
        val freeQueryEligibility = evaluateEligibility(freeQuery, profile)
        val mapsEligibility = evaluateEligibility(mapsAnalyzer, profile)

        assertEquals(EligibilityStatus.POTENTIAL, webSummaryEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, keyTakeawaysEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, freeQueryEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, mapsEligibility.status)
    }

    @Test
    fun testYoutubeUrlResultsInTextAndMultimediaPotential() {
        val profile = sourceResolver.resolvePreFetchProfile("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        val webSummary = getFeature("WEB_SUMMARY")
        val keyTakeaways = getFeature("KEY_TAKEAWAYS")
        val freeQuery = getFeature("FREE_SOURCE_QUERY")
        val multimedia = getFeature("MULTIMEDIA_ANALYSIS")

        val webSummaryEligibility = evaluateEligibility(webSummary, profile)
        val keyTakeawaysEligibility = evaluateEligibility(keyTakeaways, profile)
        val freeQueryEligibility = evaluateEligibility(freeQuery, profile)
        val multimediaEligibility = evaluateEligibility(multimedia, profile)

        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, webSummaryEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, keyTakeawaysEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, freeQueryEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, multimediaEligibility.status)
    }

    @Test
    fun testYoutubePostFetchWithoutTranscriptButVideoMetadataAvailableResultsInMultimediaEligibleAndTextIneligible() {
        val profile = SourceProfile(
            sourceType = SourceProfile.SourceType.VIDEO,
            platform = SourcePlatform.YOUTUBE,
            rawInput = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            capabilities = mapOf(
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.UNAVAILABLE
                ),
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.AVAILABLE
                )
            ),
            isPostFetchConfirmed = true
        )

        val multimedia = getFeature("MULTIMEDIA_ANALYSIS")
        val webSummary = getFeature("WEB_SUMMARY")

        val multimediaEligibility = evaluateEligibility(multimedia, profile)
        val webSummaryEligibility = evaluateEligibility(webSummary, profile)

        assertEquals(EligibilityStatus.ELIGIBLE, multimediaEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, webSummaryEligibility.status)
    }

    @Test
    fun testDocumentUrlResultsInDocumentAndGeneralTextPotential() {
        val profile = sourceResolver.resolvePreFetchProfile("https://example.com/document.pdf")

        val docSummary = getFeature("DOCUMENT_SUMMARY")
        val webSummary = getFeature("WEB_SUMMARY")

        val docEligibility = evaluateEligibility(docSummary, profile)
        val webEligibility = evaluateEligibility(webSummary, profile)

        assertEquals(EligibilityStatus.POTENTIAL, docEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, webEligibility.status)
    }

    @Test
    fun testRawTextResultsInGeneralTextEligibleAndDocumentAndMapsIneligible() {
        val profile = sourceResolver.resolvePreFetchProfile("Dies ist ein direkter Text ohne URL.")

        val webSummary = getFeature("WEB_SUMMARY")
        val keyTakeaways = getFeature("KEY_TAKEAWAYS")
        val freeQuery = getFeature("FREE_SOURCE_QUERY")
        val docSummary = getFeature("DOCUMENT_SUMMARY")
        val mapsAnalyzer = getFeature("GOOGLE_MAPS_ANALYZER")

        val webEligibility = evaluateEligibility(webSummary, profile)
        val keyEligibility = evaluateEligibility(keyTakeaways, profile)
        val freeEligibility = evaluateEligibility(freeQuery, profile)
        val docEligibility = evaluateEligibility(docSummary, profile)
        val mapsEligibility = evaluateEligibility(mapsAnalyzer, profile)

        assertEquals(EligibilityStatus.ELIGIBLE, webEligibility.status)
        assertEquals(EligibilityStatus.ELIGIBLE, keyEligibility.status)
        assertEquals(EligibilityStatus.ELIGIBLE, freeEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, docEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, mapsEligibility.status)
    }

    @Test
    fun testGoogleMapsUrlResultsInMapsPotentialAndGeneralTextIneligible() {
        val profile = sourceResolver.resolvePreFetchProfile("https://maps.google.com/?q=Berlin")

        val mapsAnalyzer = getFeature("GOOGLE_MAPS_ANALYZER")
        val mapsContext = getFeature("GOOGLE_MAPS_LOCATION_CONTEXT")
        val mapsQuery = getFeature("GOOGLE_MAPS_LOCATION_QUERY")
        val webSummary = getFeature("WEB_SUMMARY")

        val analyzerEligibility = evaluateEligibility(mapsAnalyzer, profile)
        val contextEligibility = evaluateEligibility(mapsContext, profile)
        val queryEligibility = evaluateEligibility(mapsQuery, profile)
        val webEligibility = evaluateEligibility(webSummary, profile)

        assertEquals(EligibilityStatus.POTENTIAL, analyzerEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, contextEligibility.status)
        assertEquals(EligibilityStatus.POTENTIAL, queryEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, webEligibility.status)
    }

    @Test
    fun testUnknownOrEmptySourceResultsInIneligible() {
        val profile = sourceResolver.resolvePreFetchProfile("")

        val webSummary = getFeature("WEB_SUMMARY")
        val mapsAnalyzer = getFeature("GOOGLE_MAPS_ANALYZER")
        val docSummary = getFeature("DOCUMENT_SUMMARY")

        val webEligibility = evaluateEligibility(webSummary, profile)
        val mapsEligibility = evaluateEligibility(mapsAnalyzer, profile)
        val docEligibility = evaluateEligibility(docSummary, profile)

        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, webEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, mapsEligibility.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, docEligibility.status)
    }
}
