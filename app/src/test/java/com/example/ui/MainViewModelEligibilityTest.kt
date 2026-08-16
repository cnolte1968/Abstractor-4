package com.example.ui

import com.example.domain.model.EligibilityStatus
import com.example.domain.model.SourceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MainViewModelEligibilityTest {

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel()
    }

    @Test
    fun testWebUrlResultsInGeneralTextFunctionsPotentialAndMapsIneligible() {
        viewModel.updateRawInput("https://example.com/article")

        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.WEB_PAGE, profile?.sourceType)

        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.POTENTIAL, map["WEB_SUMMARY"]?.status)
        assertEquals(EligibilityStatus.POTENTIAL, map["KEY_TAKEAWAYS"]?.status)
        assertEquals(EligibilityStatus.POTENTIAL, map["FREE_SOURCE_QUERY"]?.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["GOOGLE_MAPS_ANALYZER"]?.status)
    }

    @Test
    fun testYoutubeUrlResultsInTextAndMultimediaPotential() {
        viewModel.updateRawInput("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.VIDEO, profile?.sourceType)

        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["WEB_SUMMARY"]?.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["KEY_TAKEAWAYS"]?.status)
        assertEquals(EligibilityStatus.POTENTIAL, map["MULTIMEDIA_ANALYSIS"]?.status)
    }

    @Test
    fun testGoogleMapsUrlResultsInMapsPotentialAndGeneralTextIneligible() {
        viewModel.updateRawInput("https://maps.google.com/?q=Berlin")

        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.PLACE, profile?.sourceType)

        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.POTENTIAL, map["GOOGLE_MAPS_ANALYZER"]?.status)
        assertEquals(EligibilityStatus.POTENTIAL, map["GOOGLE_MAPS_LOCATION_CONTEXT"]?.status)
        assertEquals(EligibilityStatus.POTENTIAL, map["GOOGLE_MAPS_LOCATION_QUERY"]?.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["WEB_SUMMARY"]?.status)
    }

    @Test
    fun testRawTextResultsInGeneralTextEligibleAndMapsIneligible() {
        viewModel.updateRawInput("Dies ist ein direkter Text ohne URL.")

        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.RAW_TEXT, profile?.sourceType)

        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.ELIGIBLE, map["WEB_SUMMARY"]?.status)
        assertEquals(EligibilityStatus.ELIGIBLE, map["KEY_TAKEAWAYS"]?.status)
        assertEquals(EligibilityStatus.ELIGIBLE, map["FREE_SOURCE_QUERY"]?.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["GOOGLE_MAPS_ANALYZER"]?.status)
    }

    @Test
    fun testEmptyInputResultsInIneligible() {
        viewModel.updateRawInput("")

        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.UNKNOWN, profile?.sourceType)

        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["WEB_SUMMARY"]?.status)
        assertEquals(EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY, map["GOOGLE_MAPS_ANALYZER"]?.status)
    }

    @Test
    fun testResetToIdleRetainsInputState() {
        viewModel.updateRawInput("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        viewModel.resetToIdle()
        
        val profile = viewModel.sourceProfile.value
        assertNotNull(profile)
        assertEquals(SourceProfile.SourceType.VIDEO, profile?.sourceType)
        
        val map = viewModel.featureEligibilityMap.value
        assertEquals(EligibilityStatus.POTENTIAL, map["MULTIMEDIA_ANALYSIS"]?.status)
    }
}
