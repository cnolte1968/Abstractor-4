package com.example.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.ui.MainViewModel
import com.example.ui.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DirectShareViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel()
        viewModel.initIfNeeded(context)
    }

    @Test
    fun testProcessDirectShare_ValidMapsLink_DirectStart() = runBlocking {
        // Given a valid Google Maps link and Maps Analyzer Type
        val shareText = "Hier ist ein Ort https://maps.app.goo.gl/XYZ123"
        
        // When processing direct share
        viewModel.processDirectShare(shareText, AnalysisType.GOOGLE_MAPS_ANALYZER)

        // Then it should go directly to loading state and populate sharedUrlToFill
        val state = viewModel.uiState.first()
        assertTrue("Expected Loading state, but was $state", state is UiState.Loading)
        assertEquals("https://maps.app.goo.gl/XYZ123", viewModel.sharedUrlToFill.value)
    }

    @Test
    fun testProcessDirectShare_ValidWebLink_PopulatesInputAndStartsSummary() = runBlocking {
        // Given a valid web link and Web Summary Type
        val shareText = "Artikel https://example.com/news/123"

        // When processing direct share
        viewModel.processDirectShare(shareText, AnalysisType.WEB_SUMMARY)

        // Then it populates sharedUrlToFill with clean URL and starts loading
        assertEquals("https://example.com/news/123", viewModel.sharedUrlToFill.value)
        val state = viewModel.uiState.first()
        assertTrue("Expected Loading state, but was $state", state is UiState.Loading)
    }

    @Test
    fun testProcessDirectShare_MapsLocationContext_PopulatesInputAndStarts() = runBlocking {
        // Given a valid Google Maps link and Location Context Type
        val shareText = "Ort https://maps.app.goo.gl/ABC999"

        // When processing direct share
        viewModel.processDirectShare(shareText, AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT)

        // Then it populates sharedUrlToFill with clean URL and starts loading
        assertEquals("https://maps.app.goo.gl/ABC999", viewModel.sharedUrlToFill.value)
        val state = viewModel.uiState.first()
        assertTrue("Expected Loading state, but was $state", state is UiState.Loading)
    }
    
    @Test
    fun testProcessDirectShare_InvalidMapsLink_Fallback() = runBlocking {
        // Given an invalid maps link for a Maps Analyzer Type
        val shareText = "Hier ist ein Artikel https://example.com/article"
        
        // When processing direct share
        viewModel.processDirectShare(shareText, AnalysisType.GOOGLE_MAPS_ANALYZER)

        // Then it should fall back (Idle state with shared text populated)
        val state = viewModel.uiState.first()
        assertTrue("Expected Idle state, but was $state", state is UiState.Idle)
        assertEquals("https://example.com/article", viewModel.sharedUrlToFill.value)
    }
}
