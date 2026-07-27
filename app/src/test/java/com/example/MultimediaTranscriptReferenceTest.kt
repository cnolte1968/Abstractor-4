package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.GeminiRepository
import com.example.data.PromptLoader
import com.example.data.ResponseNormalizer
import com.example.data.SummaryResponseParser
import com.example.data.GenerateContentRequest
import com.example.data.Content
import com.example.data.Part
import com.example.data.GenerationConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultimediaTranscriptReferenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GeminiRepository.staticContext = context
    }

    @Test
    fun testMultimediaTranscriptFullPipeline() = runBlocking {
        val transcriptFile = File("src/test/Transkript_Youtube.txt")
        val transcriptText = transcriptFile.readText()
        
        println("TEST_LOG: Transcript file: ${transcriptFile.absolutePath}")
        println("TEST_LOG: Bytes: ${transcriptFile.length()}")
        println("TEST_LOG: Chars: ${transcriptText.length}")
        println("TEST_LOG: Lines: ${transcriptText.lines().size}")
        
        val prompt = PromptLoader.loadPromptForAnalysisType(context, AnalysisType.MULTIMEDIA_ANALYSIS)
        
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
        if (apiKey.isEmpty()) {
            println("TEST_LOG: Skipping test: GEMINI_API_KEY missing")
            return@runBlocking
        }
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "$prompt\n\n$transcriptText")))),
            generationConfig = GenerationConfig(temperature = 0.2)
        )
        
        val response = GeminiRepository.generateContent("gemini-3.5-flash", request)
        val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        
        val normalized = ResponseNormalizer.normalize(rawJson)
        val summary = SummaryResponseParser.parse(
            rawText = normalized,
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS
        )
        
        println("TEST_LOG: DomainSummary: $summary")
        assertNotNull(summary)
    }
}
