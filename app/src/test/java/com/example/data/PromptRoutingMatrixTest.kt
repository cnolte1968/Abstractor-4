package com.example.data

import android.content.Context
import com.example.data.engine.AnalysisRegistryImpl
import com.example.data.engine.BaseGeminiEngine
import com.example.domain.engine.AnalysisEngine
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import com.example.domain.repository.GeminiGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PromptRoutingMatrixTest {

    private val mockGateway = object : GeminiGateway {
        override suspend fun generateContent(
            model: String,
            request: GenerateContentRequest
        ): GenerateContentResponse = GenerateContentResponse(emptyList())
    }

    private val mockContext: Context by lazy { ApplicationProvider.getApplicationContext<Context>() }

    private val mockPromptLoader = object : PromptAssetLoader {
        override fun loadAsset(path: String): String = "mock"
    }

    private val registry: AnalysisRegistryImpl by lazy { AnalysisRegistryImpl(mockGateway, mockContext, promptAssetLoader = mockPromptLoader) }

    private fun resolvePath(engine: AnalysisEngine, input: CanonicalAnalysisInput): String {
        val method = BaseGeminiEngine::class.java.getDeclaredMethod("resolvePromptPath", CanonicalAnalysisInput::class.java)
        method.isAccessible = true
        return method.invoke(engine, input) as String
    }

    private fun assertRouting(
        sourceType: SourceType,
        analysisType: AnalysisType,
        expectedPromptFile: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val functionId = registry.getFunctionIdForType(analysisType)
        val engine = registry.getEngine(functionId)
        assertNotNull("Engine for $functionId should not be null", engine)
        
        val input = CanonicalAnalysisInput(
            sourceType = sourceType,
            rawText = "mock text",
            enrichedText = "mock text",
            metadata = metadata,
            analysisId = "test-id",
            analysisType = analysisType,
            freeQuery = "mock query"
        )
        
        val resolvedPath = resolvePath(engine!!, input)
        assertEquals("prompts/$expectedPromptFile", resolvedPath)
        
        // Also verify the file exists physically
        val promptFile = File("src/main/assets/prompts/$expectedPromptFile")
        assertTrue("Prompt file $expectedPromptFile must exist in assets/prompts/", promptFile.exists())
    }

    // 1. YouTube + MULTIMEDIA_ANALYSIS -> F_MULTIMEDIA.md
    @Test
    fun testYouTubeMultimediaAnalysisRoutesToF_Multimedia() {
        assertRouting(
            sourceType = SourceType.YOUTUBE,
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            expectedPromptFile = "F_MULTIMEDIA.md",
            metadata = mapOf("sourcePlatform" to "YOUTUBE")
        )
    }

    // 2. YouTube + FREE_SOURCE_QUERY -> F_MULTIMEDIA_SOURCE_QA.md
    @Test
    fun testYouTubeFreeSourceQueryRoutesToF_MultimediaSourceQa() {
        assertRouting(
            sourceType = SourceType.YOUTUBE,
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            expectedPromptFile = "F_MULTIMEDIA_SOURCE_QA.md",
            metadata = mapOf("sourcePlatform" to "YOUTUBE")
        )
    }

    // 3. Web + FREE_SOURCE_QUERY -> F_FREIE_QUELLENANFRAGE.md
    @Test
    fun testWebFreeSourceQueryRoutesToF_FreieQuellenanfrage() {
        assertRouting(
            sourceType = SourceType.WEB,
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            expectedPromptFile = "F_FREIE_QUELLENANFRAGE.md"
        )
    }

    // 4. Web + WEB_SUMMARY -> F_STANDARD_WEBSEITE.md
    @Test
    fun testWebStandardSummaryRoutesToF_StandardWebseite() {
        assertRouting(
            sourceType = SourceType.WEB,
            analysisType = AnalysisType.WEB_SUMMARY,
            expectedPromptFile = "F_STANDARD_WEBSEITE.md"
        )
    }

    // 5. Google Maps + Location Query -> F_GOOGLE_MAPS_LOCATION_QA.md
    @Test
    fun testGoogleMapsLocationQueryRoutesToF_GoogleMapsLocationQa() {
        assertRouting(
            sourceType = SourceType.WEB, // Google Maps is often mapped as WEB source type initially
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            expectedPromptFile = "F_GOOGLE_MAPS_LOCATION_QA.md",
            metadata = mapOf("url" to "https://maps.google.com/?q=Test")
        )
    }
    
    // 6. Document + DOCUMENT_SUMMARY -> F_DOKUMENTE.md
    @Test
    fun testDocumentSummaryRoutesToF_Dokumente() {
        assertRouting(
            sourceType = SourceType.DOCUMENT,
            analysisType = AnalysisType.DOCUMENT_SUMMARY,
            expectedPromptFile = "F_DOKUMENTE.md"
        )
    }
}
