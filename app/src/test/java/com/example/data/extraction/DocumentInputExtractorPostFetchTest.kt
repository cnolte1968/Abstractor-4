package com.example.data.extraction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentInputExtractorPostFetchTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val extractor by lazy { DocumentInputExtractor(context) }

    @Test
    fun testDirectContentSetsRawTextConfirmedProfile() = runBlocking {
        val directText = "This is a direct text input provided by the user for analysis."
        val result = extractor.extract(
            rawUrl = "",
            normalizedUrl = "",
            directContent = directText,
            analysisType = AnalysisType.WEB_SUMMARY,
            freeQuery = null,
            analysisId = "test-doc-123"
        )

        assertTrue(result is ContentExtractionResult.Success)
        val success = result as ContentExtractionResult.Success
        val profile = success.content.confirmedProfile

        assertNotNull("confirmedProfile should not be null", profile)
        assertEquals(SourceProfile.SourceType.RAW_TEXT, profile?.sourceType)
        assertEquals(SourcePlatform.LOCAL_FILE, profile?.platform)
        assertTrue(profile?.isPostFetchConfirmed == true)
        assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.RAW_TEXT))
    }
}

