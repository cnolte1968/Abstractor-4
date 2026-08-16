package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.WebpageExtractor
import com.example.data.repository.ContentExtractionRepositoryImpl
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.SourceType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContentExtractionRegressionTest {

    private lateinit var context: Context
    private lateinit var originalClient: OkHttpClient
    private lateinit var repository: ContentExtractionRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = ContentExtractionRepositoryImpl(context)

        // Save original OkHttpClient
        originalClient = WebpageExtractor.client
    }

    @After
    fun tearDown() {
        // Restore original OkHttpClient
        WebpageExtractor.client = originalClient
    }

    private fun setMockClient(interceptor: (okhttp3.Interceptor.Chain) -> Response) {
        val mockClient = originalClient.newBuilder()
            .addInterceptor { chain -> interceptor(chain) }
            .build()
        WebpageExtractor.client = mockClient
    }

    class TrackingResponseBody(private val delegate: okhttp3.ResponseBody) : okhttp3.ResponseBody() {
        var isClosed = false
        override fun contentType() = delegate.contentType()
        override fun contentLength() = delegate.contentLength()
        override fun source() = delegate.source()
        override fun close() {
            isClosed = true
            super.close()
        }
    }

    @Test
    fun testWebpageExtractorClosesResponseOnSuccessHttp200() {
        var trackingBody: TrackingResponseBody? = null
        setMockClient { chain ->
            val rawBody = "<html><head><title>Test Title</title><meta name=\"description\" content=\"Test Desc\"></head><body>${"A".repeat(600)}</body></html>"
                .toResponseBody("text/html".toMediaTypeOrNull())
            trackingBody = TrackingResponseBody(rawBody)
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(trackingBody)
                .build()
        }

        val result = WebpageExtractor.extractWebpageContent("https://example.com")
        assertNotNull(result)
        assertNotNull(trackingBody)
        assertTrue("Response body must be closed on HTTP 200", trackingBody!!.isClosed)
    }

    @Test
    fun testWebpageExtractorClosesResponseOnHttp403Or404() {
        var trackingBody: TrackingResponseBody? = null
        setMockClient { chain ->
            val rawBody = "Access Denied".toResponseBody("text/plain".toMediaTypeOrNull())
            trackingBody = TrackingResponseBody(rawBody)
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body(trackingBody)
                .build()
        }

        try {
            WebpageExtractor.extractWebpageContent("https://example.com")
            fail("Expected extractWebpageContent to throw IOException on 403")
        } catch (e: Exception) {
            assertTrue(e is IOException)
            assertTrue(e.message?.contains("HTTP_ERROR_403") == true)
        }

        assertNotNull(trackingBody)
        assertTrue("Response body must be closed on HTTP 403", trackingBody!!.isClosed)
    }

    @Test
    fun testRepositoryMapsHttp404ToControlledError() = kotlinx.coroutines.runBlocking {
        setMockClient { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .body("Not Found".toResponseBody("text/plain".toMediaTypeOrNull()))
                .build()
        }

        val result = repository.extractContent(
            rawUrl = "https://example.com",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )

        println("DEBUG_TEST: HTTP 404 Result is: $result")
        if (result is ContentExtractionResult.Failure) {
            println("DEBUG_TEST: HTTP 404 Failure message: ${result.message}, detail: ${result.detail}")
        }

        assertTrue("Result must be ContentExtractionResult.Failure", result is ContentExtractionResult.Failure)
        val failure = result as ContentExtractionResult.Failure
        assertTrue("Message should contain 404 error text", failure.message?.contains("404") == true)
        assertTrue("Detail should mention Tippfehler", failure.detail?.contains("Tippfehler") == true)
    }

    @Test
    fun testRepositoryMapsUnknownHostExceptionToControlledError() = kotlinx.coroutines.runBlocking {
        setMockClient {
            throw UnknownHostException("dns lookup failed")
        }

        val result = repository.extractContent(
            rawUrl = "https://example.com",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )

        println("DEBUG_TEST: UnknownHost Result is: $result")
        if (result is ContentExtractionResult.Failure) {
            println("DEBUG_TEST: UnknownHost Failure message: ${result.message}, detail: ${result.detail}")
        }

        assertTrue("Result must be ContentExtractionResult.Failure", result is ContentExtractionResult.Failure)
        val failure = result as ContentExtractionResult.Failure
        assertTrue("Message should refer to Internetconnection or URL", failure.message?.contains("Internetverbindung") == true || failure.message?.contains("URL") == true || failure.message?.contains("ungültige") == true)
        assertTrue("Detail should explain host could not be resolved", failure.detail?.contains("Host") == true || failure.detail?.contains("Netzwerk") == true)
    }

    @Test
    fun testRepositorySuccessOnValidHtml() = kotlinx.coroutines.runBlocking {
        setMockClient { chain ->
            val contentHtml = "<html><head><title>Success Title</title></head><body>${"Good content about something important. ".repeat(40)}</body></html>"
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(contentHtml.toResponseBody("text/html".toMediaTypeOrNull()))
                .build()
        }

        val result = repository.extractContent(
            rawUrl = "https://example.com",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )

        assertTrue("Result must be ContentExtractionResult.Success", result is ContentExtractionResult.Success)
        val success = result as ContentExtractionResult.Success
        assertEquals("Success Title", success.content.metadata["title"])
        assertTrue("Enriched text should contain extracted content", success.content.enrichedText.contains("Success Title"))
    }

    @Test
    fun testBothStandardWebpageAndTop3KernaussagenReachSuccess() = kotlinx.coroutines.runBlocking {
        setMockClient { chain ->
            val contentHtml = "<html><head><title>My Article</title></head><body>${"Valid content for summary. ".repeat(50)}</body></html>"
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(contentHtml.toResponseBody("text/html".toMediaTypeOrNull()))
                .build()
        }

        // Test for STANDARD_WEBSEITE
        val resultStandard = repository.extractContent(
            rawUrl = "https://example.com",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )
        assertTrue("STANDARD_WEBSEITE must result in Success", resultStandard is ContentExtractionResult.Success)

        // Test for TOP_3_KERNAUSSAGEN
        val resultTop3 = repository.extractContent(
            rawUrl = "https://example.com",
            directContent = null,
            analysisType = AnalysisType.TOP_3_KERNAUSSAGEN,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )
        assertTrue("TOP_3_KERNAUSSAGEN must result in Success", resultTop3 is ContentExtractionResult.Success)
    }

    @Test
    fun testSummaryResponseParserHandlesStringTakeawaysWithoutDetails() {
        val rawResponse = """
            {
              "title": "Test Title",
              "original_url": "https://example.com",
              "short_description": "A short test description.",
              "key_takeaways": [
                "This is a takeaway without a colon",
                "**Highlighted Title**: Detailed explanation here",
                "Another Title: With details here"
              ]
            }
        """.trimIndent()

        val parsed = com.example.data.SummaryResponseParser.parse(
            rawText = rawResponse,
            originalFallbackUrl = "https://example.com",
            analysisType = com.example.data.AnalysisType.STANDARD_WEBSEITE,
            analysisId = UUID.randomUUID().toString()
        )

        assertNotNull(parsed)
        assertEquals("Test Title", parsed.title)
        assertTrue(parsed.keyTakeaways.isNotEmpty())
        for (item in parsed.keyTakeaways) {
            assertTrue("Title should not be blank", item.title.isNotBlank())
            assertTrue("Details should not be blank", item.details.isNotBlank())
        }
        
        // Verify that the EngineContract validates this output successfully
        val contract = com.example.domain.engine.EngineContract(
            functionId = "WEB_SUMMARY",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = com.example.domain.engine.EngineCapabilities(
                name = "Web Analysis Test",
                supportsSearchGrounding = false,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/F_STANDARD_WEBSEITE.md"
        )
        try {
            contract.validateOutput(parsed)
        } catch (e: Exception) {
            fail("Contract validation should pass: ${e.message}")
        }
    }

    @Test
    fun testSummaryResponseParserSanitizesBlankDetailsToTitle() {
        val rawResponse = """
            {
              "title": "Test Title",
              "original_url": "https://example.com",
              "short_description": "A short test description.",
              "key_takeaways": [
                {
                  "title": "Takeaway 1 with blank details",
                  "details": ""
                },
                {
                  "title": "",
                  "details": "This details contains a sentence. Therefore the first sentence will become the title."
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.data.SummaryResponseParser.parse(
            rawText = rawResponse,
            originalFallbackUrl = "https://example.com",
            analysisType = com.example.data.AnalysisType.STANDARD_WEBSEITE,
            analysisId = UUID.randomUUID().toString()
        )

        assertNotNull(parsed)
        assertEquals(2, parsed.keyTakeaways.size)
        
        val first = parsed.keyTakeaways[0]
        assertEquals("Takeaway 1 with blank details", first.title)
        assertEquals("Ergänzende Detailausführungen sind dem Quelltext direkt zu entnehmen.", first.details)

        val second = parsed.keyTakeaways[1]
        assertEquals("This details contains a sentence", second.title)
        assertEquals("This details contains a sentence. Therefore the first sentence will become the title.", second.details)

        // Verify EngineContract validation passes
        val contract = com.example.domain.engine.EngineContract(
            functionId = "WEB_SUMMARY",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = com.example.domain.engine.EngineCapabilities(
                name = "Web Analysis Test",
                supportsSearchGrounding = false,
                supportsDirectPdf = false
            ),
            promptPath = "prompts/F_STANDARD_WEBSEITE.md"
        )
        try {
            contract.validateOutput(parsed)
        } catch (e: Exception) {
            fail("Contract validation should pass: ${e.message}")
        }
    }

    @Test
    fun testPdfTextExtractionWithValidContent() {
        val pdfContent = "stream\n(This is some valid text about interesting topics that has a decent length and vowel ratio to avoid being marked as garbage or residue)\nendstream".toByteArray(Charsets.US_ASCII)
        val extracted = com.example.data.FileProcessingHelper.extractTextFromPdf(pdfContent)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("This is some valid text"))
    }

    @Test
    fun testPdfTextExtractionWithGarbageContent() {
        val garbageBytes = "This is not a PDF stream at all, just some garbage data without any stream definitions".toByteArray(Charsets.US_ASCII)
        val extracted = com.example.data.FileProcessingHelper.extractTextFromPdf(garbageBytes)
        assertNull("Garbage PDF without streams should return null", extracted)
    }

    @Test
    fun testPdfTextExtractionWithEmptyStream() {
        val pdfWithEmptyStream = "stream\n()\nendstream".toByteArray(Charsets.US_ASCII)
        val extracted = com.example.data.FileProcessingHelper.extractTextFromPdf(pdfWithEmptyStream)
        assertNull("PDF stream with no extractable text should return null", extracted)
    }

    @Test
    fun testPdfTextExtractionWithFontMetadataIsSkipped() {
        val fontMetadata = "stream\n/FontDescriptor /ToUnicode /FontName begincmap\nendstream".toByteArray(Charsets.US_ASCII)
        val extracted = com.example.data.FileProcessingHelper.extractTextFromPdf(fontMetadata)
        assertNull("Font metadata and CMaps should be skipped and return null", extracted)
    }

    @Test
    fun testOfficeDocxExtractionWithValidContent() {
        val bos = java.io.ByteArrayOutputStream()
        val zos = java.util.zip.ZipOutputStream(bos)
        val entry = java.util.zip.ZipEntry("word/document.xml")
        zos.putNextEntry(entry)
        val xmlContent = "<w:p><w:r><w:t>This is some valid document text in docx format with a complete sentence</w:t></w:r></w:p>"
        zos.write(xmlContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()
        
        val bytes = bos.toByteArray()
        val extracted = com.example.data.FileProcessingHelper.extractOfficeTextFromBytes(bytes)
        assertNotNull(extracted)
        assertEquals("This is some valid document text in docx format with a complete sentence", extracted)
    }

    @Test
    fun testOfficeDocxExtractionWithCorruptedZip() {
        val corruptedBytes = "Not a zip file at all".toByteArray(Charsets.UTF_8)
        val extracted = com.example.data.FileProcessingHelper.extractOfficeTextFromBytes(corruptedBytes)
        assertNull("Corrupted zip should return null gracefully without throwing", extracted)
    }

    @Test
    fun testOfficeXlsxExtractionWithValidContent() {
        val bos = java.io.ByteArrayOutputStream()
        val zos = java.util.zip.ZipOutputStream(bos)
        val entry = java.util.zip.ZipEntry("xl/sharedStrings.xml")
        zos.putNextEntry(entry)
        val xmlContent = "<sst><si><t>Shared string content for excel sheet data cells</t></si></sst>"
        zos.write(xmlContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
        zos.close()
        
        val bytes = bos.toByteArray()
        val extracted = com.example.data.FileProcessingHelper.extractOfficeTextFromBytes(bytes)
        assertNotNull(extracted)
        assertEquals("Shared string content for excel sheet data cells", extracted)
    }

    @Test
    fun testFileProcessingHelperMimeTypeBehavior() {
        assertTrue(com.example.data.FileProcessingHelper.isExtractableTextType("application/pdf", "test.pdf"))
        assertTrue(com.example.data.FileProcessingHelper.isExtractableTextType("application/octet-stream", "test.pdf"))
        assertTrue(com.example.data.FileProcessingHelper.isExtractableTextType("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "test.docx"))
        assertTrue(com.example.data.FileProcessingHelper.isExtractableTextType("application/octet-stream", "test.xlsx"))
        assertFalse(com.example.data.FileProcessingHelper.isExtractableTextType("application/octet-stream", "test.bin"))
    }

    @Test
    fun testInputExtractorRegistryRouting() {
        val registry = com.example.data.extraction.InputExtractorRegistry(context)

        // 1. Web URL should resolve to WebInputExtractor
        val webExtractor = registry.getExtractor(
            rawUrl = "https://spiegel.de",
            normalizedUrl = "https://spiegel.de",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )
        assertNotNull(webExtractor)
        assertTrue(webExtractor is com.example.data.extraction.WebInputExtractor)

        // 2. YouTube URL should resolve to YoutubeInputExtractor
        val ytExtractor = registry.getExtractor(
            rawUrl = "https://youtube.com/watch?v=12345678901",
            normalizedUrl = "https://youtube.com/watch?v=12345678901",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )
        assertNotNull(ytExtractor)
        assertTrue(ytExtractor is com.example.data.extraction.RemoteVideoInputExtractor || ytExtractor is com.example.data.extraction.YoutubeInputExtractor)

        // 3. Document inputs (direct content, document type, or document file extension) should resolve to DocumentInputExtractor
        val docExtractor1 = registry.getExtractor(
            rawUrl = "",
            normalizedUrl = "",
            directContent = "This is some pasted document text",
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )
        assertNotNull(docExtractor1)
        assertTrue(docExtractor1 is com.example.data.extraction.DocumentInputExtractor)

        val docExtractor2 = registry.getExtractor(
            rawUrl = "content://path/to/file.pdf",
            normalizedUrl = "content://path/to/file.pdf",
            directContent = null,
            analysisType = AnalysisType.DOKUMENTE
        )
        assertNotNull(docExtractor2)
        assertTrue(docExtractor2 is com.example.data.extraction.DocumentInputExtractor)

        val docExtractor3 = registry.getExtractor(
            rawUrl = "file://path/to/file.xlsx",
            normalizedUrl = "file://path/to/file.xlsx",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )
        assertNotNull(docExtractor3)
        assertTrue(docExtractor3 is com.example.data.extraction.DocumentInputExtractor)
    }

    @Test
    fun testInputExtractorRegistryUnknownInput() {
        val registry = com.example.data.extraction.InputExtractorRegistry(context)
        val unknownExtractor = registry.getExtractor(
            rawUrl = "invalid_input",
            normalizedUrl = "invalid_input",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE
        )
        assertNull("Unknown input should return null from registry", unknownExtractor)
    }

    @Test
    fun testContentExtractionRepositoryUnknownInput() {
        val result = kotlinx.coroutines.runBlocking {
            repository.extractContent(
                rawUrl = "abc",
                directContent = null,
                analysisType = AnalysisType.STANDARD_WEBSEITE,
                freeQuery = null,
                analysisId = UUID.randomUUID().toString()
            )
        }
        assertTrue(result is ContentExtractionResult.Failure)
        val failure = result as ContentExtractionResult.Failure
        assertEquals(ContentExtractionResult.Failure.ErrorType.INVALID_URL, failure.errorType)
    }

    @Test
    fun testUrlNormalizationAndSourceHostDiagnostics() = kotlinx.coroutines.runBlocking {
        com.example.data.GatewayDiagnostics.reset()
        setMockClient { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("<html><head><title>Test Title</title></head><body>${"Valid content. ".repeat(40)}</body></html>".toResponseBody("text/html".toMediaTypeOrNull()))
                .build()
        }

        val result = repository.extractContent(
            rawUrl = "  https://example.com/  ",
            directContent = null,
            analysisType = AnalysisType.STANDARD_WEBSEITE,
            freeQuery = null,
            analysisId = UUID.randomUUID().toString()
        )

        assertTrue("Result must be ContentExtractionResult.Success", result is ContentExtractionResult.Success)
        assertEquals("https://example.com/", com.example.data.GatewayDiagnostics.normalizedSourceUrl)
        assertEquals("  https://example.com/  ", com.example.data.GatewayDiagnostics.sourceUrl)
        assertEquals(200, com.example.data.GatewayDiagnostics.sourceHttpStatus)

        // Since the application interceptor mocks and short-circuits the call,
        // we directly invoke our diagnostics helper to test and verify its logging behavior.
        try {
            com.example.data.WebpageExtractor.populateDiagnosticsBeforeRequest("https://example.com/")
        } catch (e: Exception) {
            // Ignored - expected to throw UnknownHostException in sandbox
        }
        assertEquals("example.com", com.example.data.GatewayDiagnostics.sourceHost)
        assertTrue(
            "DNS outcome must be SUCCESS or FAIL",
            com.example.data.GatewayDiagnostics.sourceDnsOutcome == "SUCCESS" || com.example.data.GatewayDiagnostics.sourceDnsOutcome == "FAIL"
        )
    }

    @Test
    fun testPhotoScreenshotAnalysisRegistrationAndRegistry() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        
        // 1. Verify getFunctionIdForType maps PHOTO_SCREENSHOT_ANALYSIS correctly
        val functionId = registry.getFunctionIdForType(AnalysisType.PHOTO_SCREENSHOT_ANALYSIS)
        assertEquals("PHOTO_SCREENSHOT_ANALYSIS", functionId)
        
        // 2. Verify getEngine retrieves the registered engine
        val engine = registry.getEngine(functionId)
        assertNotNull("Engine must be registered for PHOTO_SCREENSHOT_ANALYSIS", engine)
        
        // 3. Verify the engine contract maps correctly
        assertEquals("PHOTO_SCREENSHOT_ANALYSIS", engine!!.contract.functionId)
        assertEquals("prompts/F_PHOTO_SCREENSHOT_ANALYSIS.md", engine.contract.promptPath)
        assertEquals("CanonicalAnalysisInput(imageBytes!=null)", engine.contract.inputSchema)
    }

    @Test
    fun testPhotoScreenshotAnalysisContractValidation_PNG_JPEG_Pass() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val engine = registry.getEngine("PHOTO_SCREENSHOT_ANALYSIS")!!

        // PNG test
        val pngInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = byteArrayOf(1, 2, 3, 4),
            mimeType = "image/png",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )
        engine.contract.validateInput(pngInput) // Should PASS without exception

        // JPEG test
        val jpegInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = byteArrayOf(5, 6, 7, 8),
            mimeType = "image/jpeg",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )
        engine.contract.validateInput(jpegInput) // Should PASS without exception
    }

    @Test
    fun testPhotoScreenshotAnalysisContractValidation_FailCases() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val engine = registry.getEngine("PHOTO_SCREENSHOT_ANALYSIS")!!

        // Empty bytes
        val emptyBytesInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = byteArrayOf(),
            mimeType = "image/png",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )
        try {
            engine.contract.validateInput(emptyBytesInput)
            fail("Should fail on empty bytes")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("non-null/non-empty rawBytes") == true)
        }

        // Invalid MIME type
        val invalidMimeInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = byteArrayOf(1, 2, 3),
            mimeType = "application/pdf",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )
        try {
            engine.contract.validateInput(invalidMimeInput)
            fail("Should fail on invalid mime type")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("valid image/ MIME type") == true)
        }

        // Null rawBytes
        val nullBytesInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = null,
            mimeType = "image/png",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
        )
        try {
            engine.contract.validateInput(nullBytesInput)
            fail("Should fail on null rawBytes")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("non-null/non-empty rawBytes") == true)
        }
    }

    @Test
    fun testTextFunctionsContractValidation_Regression() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val webEngine = registry.getEngine("WEB_SUMMARY")!!

        // Text function without enrichedText should FAIL
        val noEnrichedInput = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "",
            enrichedText = "",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.WEB_SUMMARY
        )
        try {
            webEngine.contract.validateInput(noEnrichedInput)
            fail("Should fail on empty enrichedText for text function")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("non-null/non-empty enrichedText") == true)
        }

        // Text function with enrichedText should PASS
        val validTextInput = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "Hello",
            enrichedText = "Hello world enriched",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.WEB_SUMMARY
        )
        webEngine.contract.validateInput(validTextInput) // Should PASS
    }

    @Test
    fun testYoutubeUrlParsingAndIDExtraction() {
        // Test watch?v= URLs
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://www.youtube.com/watch?v=nnqnfbGbuoA"))
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://youtube.com/watch?v=nnqnfbGbuoA&feature=share"))
        
        // Test youtu.be/ URLs
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://youtu.be/nnqnfbGbuoA"))
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://youtu.be/nnqnfbGbuoA?si=abc"))
        
        // Test /live/ URLs
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ"))
        assertEquals("nnqnfbGbuoA", com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId("https://www.youtube.com/live/nnqnfbGbuoA"))
    }

    @Test
    fun testYoutubeTranscriptHelperFindCaptionUrl() {
        val helperClass = com.example.data.YoutubeTranscriptHelper
        val findCaptionUrlMethod = helperClass.javaClass.getDeclaredMethod("findCaptionUrl", String::class.java)
        findCaptionUrlMethod.isAccessible = true

        // 1. Case with only automatic transcript
        val htmlWithAutomaticOnly = """
            "captionTracks":[{"baseUrl":"https://www.youtube.com/api/timedtext?v=123&asr=1","vssId":"a.de","languageCode":"de","kind":"asr"}]
        """.trimIndent()
        val resultAuto = findCaptionUrlMethod.invoke(helperClass, htmlWithAutomaticOnly) as? String
        assertNotNull(resultAuto)
        assertEquals("https://www.youtube.com/api/timedtext?v=123&asr=1", resultAuto)

        // 2. Case with both manual and automatic (should prefer manual)
        val htmlWithManualAndAuto = """
            "captionTracks":[
                {"baseUrl":"https://www.youtube.com/api/timedtext?v=123&asr=1","vssId":"a.de","languageCode":"de","kind":"asr"},
                {"baseUrl":"https://www.youtube.com/api/timedtext?v=123&manual=1","vssId":"de","languageCode":"de"}
            ]
        """.trimIndent()
        val resultManual = findCaptionUrlMethod.invoke(helperClass, htmlWithManualAndAuto) as? String
        assertNotNull(resultManual)
        assertEquals("https://www.youtube.com/api/timedtext?v=123&manual=1", resultManual)

        // 3. Case with no captionTracks but timedtext fallback
        val htmlWithTimedTextFallback = """
            some other content before https://www.youtube.com/api/timedtext?v=999&fallback=true some content after
        """.trimIndent()
        val resultFallback = findCaptionUrlMethod.invoke(helperClass, htmlWithTimedTextFallback) as? String
        assertNotNull(resultFallback)
        assertEquals("https://www.youtube.com/api/timedtext?v=999&fallback=true", resultFallback)

        // 4. Case with no transcript at all
        val htmlWithNoTranscript = """
            <html><body>No captions here at all!</body></html>
        """.trimIndent()
        val resultNone = findCaptionUrlMethod.invoke(helperClass, htmlWithNoTranscript) as? String
        assertNull(resultNone)
    }

    @Test
    fun testYoutubeInputExtractorReturnsTranscriptUnavailable() = kotlinx.coroutines.runBlocking {
        val extractor = com.example.data.extraction.YoutubeInputExtractor()
        
        // Mock YoutubeTranscriptHelper's client using reflection to return 404
        val helperClass = com.example.data.YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient
        
        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("Video Unavailable".toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()
        clientField.set(helperClass, mockClient)
        
        try {
            val result = extractor.extract(
                rawUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                normalizedUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                directContent = null,
                analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
                freeQuery = null,
                analysisId = UUID.randomUUID().toString()
            )

            println("DEBUG_TEST: result class is: " + result.javaClass.name)
            if (result is ContentExtractionResult.Failure) {
                println("DEBUG_TEST: Failure type is: " + result.errorType + ", message: " + result.message + ", detail: " + result.detail)
            } else if (result is ContentExtractionResult.Success) {
                println("DEBUG_TEST: Success rawText length: " + result.content.rawText.length)
            } else if (result is ContentExtractionResult.Predefined) {
                println("DEBUG_TEST: Predefined title: " + result.summary.title + ", shortDescription: " + result.summary.shortDescription)
            }

            assertTrue(result is ContentExtractionResult.Predefined)
            val predefined = result as ContentExtractionResult.Predefined
            assertEquals("Video nicht auslesbar", predefined.summary.title)
            assertEquals("TRANSCRIPT_UNAVAILABLE", predefined.summary.shortDescription)
        } finally {
            // Restore original client
            clientField.set(helperClass, originalYtClient)
        }
    }

    @Test
    fun testYoutubePlayerApiFallbackSuccess() = kotlinx.coroutines.runBlocking {
        val extractor = com.example.data.extraction.YoutubeInputExtractor()
        
        val helperClass = com.example.data.YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient
        
        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()
                
                val responseBody = when {
                    urlStr.contains("youtube.com/watch") -> {
                        // Watch HTML with no caption tracks but an INNERTUBE_API_KEY
                        """
                        <html>
                          <body>
                            "INNERTUBE_API_KEY": "test_api_key_123"
                            No caption tracks here!
                          </body>
                        </html>
                        """.trimIndent()
                    }
                    urlStr.contains("youtubei/v1/player") -> {
                        // Player API response with caption tracks minified to match extractField parser
                        """{"captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[{"baseUrl":"https://www.youtube.com/api/timedtext?v=nnqnfbGbuoA&lang=de","vssId":"de","languageCode":"de"}]}}}"""
                    }
                    urlStr.contains("timedtext") -> {
                        // Subtitle XML content
                        """
                        <transcript>
                          <text start="0.0" dur="2.0">Hello wonderful world of YouTube transcripts. This is a very interesting video transcript with plenty of characters to pass the length check!</text>
                        </transcript>
                        """.trimIndent()
                    }
                    urlStr.contains("oembed") -> {
                        """
                        {
                          "title": "Mock YouTube Video",
                          "author_name": "Mock Channel"
                        }
                        """.trimIndent()
                    }
                    else -> ""
                }
                
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()
            
        clientField.set(helperClass, mockClient)
        
        try {
            com.example.data.PipelineReportStore.startNewReport("TEST")
            val result = extractor.extract(
                rawUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                normalizedUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                directContent = null,
                analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
                freeQuery = null,
                analysisId = UUID.randomUUID().toString()
            )
            
            if (result !is ContentExtractionResult.Success) {
                println("DEBUG_TEST: result is NOT Success! It is: " + result.javaClass.name)
                if (result is ContentExtractionResult.Failure) {
                    println("DEBUG_TEST: errorType=" + result.errorType + ", message=" + result.message + ", detail=" + result.detail)
                } else if (result is ContentExtractionResult.Predefined) {
                    println("DEBUG_TEST: predefined title=" + result.summary.title + ", desc=" + result.summary.shortDescription)
                }
                println("DEBUG_TEST: GatewayDiagnostics.ytFallbackFailureReason=" + com.example.data.GatewayDiagnostics.ytFallbackFailureReason)
                println("DEBUG_TEST: GatewayDiagnostics.exceptionMessage=" + com.example.data.GatewayDiagnostics.exceptionMessage)
            }
            assertTrue(result is ContentExtractionResult.Success)
            val successResult = result as ContentExtractionResult.Success
            assertEquals("Hello wonderful world of YouTube transcripts. This is a very interesting video transcript with plenty of characters to pass the length check!", successResult.content.rawText)
            
            // Check diagnostic fields
            assertEquals("PLAYER_API_FALLBACK", com.example.data.GatewayDiagnostics.ytTranscriptDiscoveryPath)
            assertEquals(1, com.example.data.GatewayDiagnostics.ytTracksFoundCount)
            assertEquals("manual", com.example.data.GatewayDiagnostics.ytSelectedTrackType)
            assertEquals("de", com.example.data.GatewayDiagnostics.ytLanguage)
            assertEquals(200, com.example.data.GatewayDiagnostics.ytCaptionHttpStatus)
            assertEquals(1, com.example.data.GatewayDiagnostics.ytExtractedSegmentCount)
            assertEquals(141, com.example.data.GatewayDiagnostics.ytFinalTranscriptLength)
            
            // Generate pipeline report to verify mappings
            com.example.data.PipelineReportStore.populateFromDiagnostics(context)
            val reportJson = com.example.data.PipelineReportStore.getLastReportJson()
            println("DEBUG_TEST_REPORT_JSON: " + reportJson)
            assertTrue(reportJson.contains("\"ytTranscriptDiscoveryPath\": \"PLAYER_API_FALLBACK\""))
            assertTrue(reportJson.contains("\"ytSelectedTrackType\": \"manual\""))
            assertTrue(reportJson.contains("\"ytLanguage\": \"de\""))
            
        } finally {
            clientField.set(helperClass, originalYtClient)
        }
    }

    @Test
    fun testYoutubeInputExtractorReturnsDegradedWhenTranscriptUnavailableButMetadataExists() = kotlinx.coroutines.runBlocking {
        val extractor = com.example.data.extraction.YoutubeInputExtractor()
        
        val helperClass = com.example.data.YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient
        
        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()
                
                val responseBody = when {
                    urlStr.contains("youtube.com/watch") -> {
                        "<html><head><title>Mock Video Title</title><meta name=\"description\" content=\"This is a mock description of the video which acts as extended content.\"></head><body>No caption tracks here!</body></html>"
                    }
                    urlStr.contains("youtubei/v1/player") -> {
                        """{"videoDetails":{"title":"Mock Video Title","author":"Mock Author","shortDescription":"This is a mock description of the video which acts as extended content."}}"""
                    }
                    urlStr.contains("oembed") -> {
                        """
                        {
                          "title": "Mock Video Title",
                          "author_name": "Mock Author"
                        }
                        """.trimIndent()
                    }
                    else -> ""
                }
                
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()
            
        clientField.set(helperClass, mockClient)
        
        try {
            com.example.data.PipelineReportStore.startNewReport("TEST")
            val result = extractor.extract(
                rawUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                normalizedUrl = "https://youtube.com/live/nnqnfbGbuoA?si=x4Z1uN_zpotWqRNZ",
                directContent = null,
                analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
                freeQuery = null,
                analysisId = UUID.randomUUID().toString()
            )
            
            assertTrue(result is ContentExtractionResult.Degraded)
            val degradedResult = result as ContentExtractionResult.Degraded
            assertTrue(degradedResult.content.rawText.contains("ERWEITERTE YOUTUBE-INHALTSGEWINNUNG"))
            assertTrue(degradedResult.content.rawText.contains("Mock Video Title"))
            assertTrue(degradedResult.content.rawText.contains("This is a mock description"))
        } finally {
            clientField.set(helperClass, originalYtClient)
        }
    }

    @Test
    fun testYoutubeBoilerplateFiltering() {
        val boilerplate = "Enjoy the videos and music that you love, upload original content, and share it all with friends, family, and the world on YouTube."
        val customText = "Das ist ein wichtiges Video über KI."
        val mixedText = "$boilerplate $customText"
        
        val cleaned = com.example.data.YoutubeTranscriptHelper.cleanYoutubeText(mixedText)
        assertEquals("Das ist ein wichtiges Video über KI.", cleaned)
        
        val pureBoilerplateCleaned = com.example.data.YoutubeTranscriptHelper.cleanYoutubeText(boilerplate)
        assertEquals("", pureBoilerplateCleaned)
    }

    @Test
    fun testFailedClientSchedulesExceptionAndStatus() = kotlinx.coroutines.runBlocking {
        val extractor = com.example.data.extraction.YoutubeInputExtractor()
        val helperClass = com.example.data.YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient
        
        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()
                val responseBody = when {
                    urlStr.contains("youtube.com/watch") -> {
                        "<html><body>\"INNERTUBE_API_KEY\": \"test_api_key_123\"</body></html>"
                    }
                    urlStr.contains("youtubei/v1/player") -> {
                        """{"playabilityStatus":{"status":"LOGIN_REQUIRED","reason":"This video is private."}}"""
                    }
                    else -> ""
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody.toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()
            
        clientField.set(helperClass, mockClient)
        try {
            com.example.data.GatewayDiagnostics.reset()
            // Try fetching transcript
            val transcript = com.example.data.YoutubeTranscriptHelper.fetchTranscript("nnqnfbGbuoA")
            assertNull(transcript)
            assertEquals("LOGIN_REQUIRED", com.example.data.GatewayDiagnostics.ytPlayabilityStatus)
            assertEquals("MWEB", com.example.data.GatewayDiagnostics.ytPlayerClientName)
            assertEquals("2.20240718.01.00", com.example.data.GatewayDiagnostics.ytPlayerClientVersion)
        } finally {
            clientField.set(helperClass, originalYtClient)
        }
    }

    @Test
    fun testDegradedStatusHandlingInPipelineReport() {
        com.example.data.GatewayDiagnostics.reset()
        com.example.data.GatewayDiagnostics.sourceUrl = "https://www.youtube.com/watch?v=nnqnfbGbuoA"
        com.example.data.GatewayDiagnostics.ytMetadataOnly = true
        com.example.data.GatewayDiagnostics.ytFinalTranscriptLength = 0
        
        com.example.data.PipelineReportStore.startNewReport("TEST")
        com.example.data.PipelineReportStore.populateFromDiagnostics(context)
        
        val reportJson = com.example.data.PipelineReportStore.getLastReportJson()
        assertTrue("Report must indicate DEGRADED final status", reportJson.contains("\"finalStatus\": \"DEGRADED\""))
        assertTrue("Report must have technical error category", reportJson.contains("\"technicalErrorCategory\": \"TRANSCRIPT_UNAVAILABLE\""))
    }

    @Test
    fun testDocumentSummaryContractValidation_OfficeFiles_Pass() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val engine = registry.getEngine("DOCUMENT_SUMMARY")!!

        assertEquals("CanonicalAnalysisInput(rawBytes!=null || enrichedText!=null)", engine.contract.inputSchema)

        // A. DOCX
        val docxInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "DOCX Content",
            enrichedText = "Docx text paragraph summary content",
            rawBytes = null,
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        engine.contract.validateInput(docxInput) // Should PASS

        // B. PPTX
        val pptxInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "PPTX Content",
            enrichedText = "Pptx slide title and bullet points content",
            rawBytes = null,
            mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        engine.contract.validateInput(pptxInput) // Should PASS

        // C. XLSX
        val xlsxInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "XLSX Content",
            enrichedText = "Sheet1: Cell A1 Data, Cell B1 Data",
            rawBytes = null,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        engine.contract.validateInput(xlsxInput) // Should PASS
    }

    @Test
    fun testDocumentSummaryContractValidation_PdfAndTxt_Pass() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val engine = registry.getEngine("DOCUMENT_SUMMARY")!!

        // D. PDF with rawBytes
        val pdfInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46), // %PDF
            mimeType = "application/pdf",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        engine.contract.validateInput(pdfInput) // Should PASS

        // E. TXT with enrichedText
        val txtInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "Plain text content",
            enrichedText = "Plain text content enriched",
            rawBytes = null,
            mimeType = "text/plain",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        engine.contract.validateInput(txtInput) // Should PASS
    }

    @Test
    fun testDocumentSummaryContractValidation_InvalidInput_Fail() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)
        val engine = registry.getEngine("DOCUMENT_SUMMARY")!!

        // F. Invalid input: null rawBytes and empty enrichedText
        val emptyInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            rawBytes = null,
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOKUMENTE
        )
        try {
            engine.contract.validateInput(emptyInput)
            fail("Should fail when both rawBytes and enrichedText are empty/null")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("requires rawBytes!=null or enrichedText!=null") == true)
        }
    }

    @Test
    fun testRealisticOfficeDocumentFlowSimulation() {
        val gateway = com.example.data.GeminiRepository
        val registry = com.example.data.engine.AnalysisRegistryImpl(gateway, context)

        // Simulate extraction result for an Office file
        val extractedOfficeText = "Title: Q3 Report\nSummary: Sales increased by 15%."
        val simulatedInput = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = extractedOfficeText,
            enrichedText = extractedOfficeText,
            rawBytes = null, // Extracted locally, so rawBytes is null
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            analysisId = UUID.randomUUID().toString(),
            analysisType = AnalysisType.DOCUMENT_SUMMARY,
            metadata = mapOf(
                "fileName" to "q3_report.docx",
                "detectedType" to "DOCX"
            )
        )

        // 1. Engine resolution
        val fid = registry.getFunctionIdForType(simulatedInput.analysisType)
        assertEquals("DOCUMENT_SUMMARY", fid)

        val engine = registry.getEngine(fid)
        assertNotNull("Engine must be found for DOCUMENT_SUMMARY", engine)

        // 2. Input contract validation
        engine!!.contract.validateInput(simulatedInput) // Must PASS without exception
        assertEquals("CanonicalAnalysisInput(rawBytes!=null || enrichedText!=null)", engine.contract.inputSchema)
    }
}
