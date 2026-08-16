package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.data.network.ExtractVideoRequestDto
import com.example.data.network.ExtractVideoResponseDto
import com.example.data.network.ProviderContentDto
import com.example.data.network.ProviderErrorDto
import com.example.data.network.ProviderMetadataDto
import com.example.data.network.SupabaseVideoApi
import com.example.domain.model.ContentExtractionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteVideoInputExtractorTest {

    class FakeSupabaseVideoApi(
        private val responseToReturn: Response<ExtractVideoResponseDto>? = null,
        private val exceptionToThrow: Exception? = null
    ) : SupabaseVideoApi {
        override suspend fun extractVideo(
            url: String,
            authHeader: String?,
            apiKey: String?,
            request: ExtractVideoRequestDto
        ): Response<ExtractVideoResponseDto> {
            if (exceptionToThrow != null) {
                throw exceptionToThrow
            }
            return responseToReturn!!
        }
    }

    @Test
    fun testSuccessResponse_returnsSuccessResult() = runBlocking {
        val fakeResponse = ExtractVideoResponseDto(
            sourcePlatform = "YOUTUBE",
            providerStatus = "SUCCESS",
            accessStatus = "PUBLIC",
            capabilityStatus = mapOf("TRANSCRIPT" to "AVAILABLE", "METADATA" to "AVAILABLE"),
            metadata = ProviderMetadataDto(title = "Test Title", author = "Test Author"),
            content = ProviderContentDto(transcript = "Hello World"),
            errorInformation = null
        )
        
        val api = FakeSupabaseVideoApi(Response.success(fakeResponse))
        val extractor = RemoteVideoInputExtractor(api, "http://test", "key")
        
        val result = extractor.extract("url", "url", null, AnalysisType.KEY_TAKEAWAYS, null, "id")
        
        assertTrue(result is ContentExtractionResult.Success)
        val success = result as ContentExtractionResult.Success
        assertEquals("Hello World", success.content.rawText)
        assertEquals("Test Title", success.content.metadata["title"])
    }

    @Test
    fun testDegradedResponse_returnsDegradedResult() = runBlocking {
        val fakeResponse = ExtractVideoResponseDto(
            sourcePlatform = "YOUTUBE",
            providerStatus = "DEGRADED",
            accessStatus = "PUBLIC",
            capabilityStatus = mapOf("TRANSCRIPT" to "UNAVAILABLE", "METADATA" to "AVAILABLE"),
            metadata = ProviderMetadataDto(title = "Test Title", author = "Test Author", description = "Test Description"),
            content = null,
            errorInformation = ProviderErrorDto("NO_TRANSCRIPT", "Missing")
        )
        
        val api = FakeSupabaseVideoApi(Response.success(fakeResponse))
        val extractor = RemoteVideoInputExtractor(api, "http://test", "key")
        
        val result = extractor.extract("url", "url", null, AnalysisType.KEY_TAKEAWAYS, null, "id")
        
        assertTrue(result is ContentExtractionResult.Degraded)
        val degraded = result as ContentExtractionResult.Degraded
        assertTrue(degraded.content.rawText.contains("Test Title"))
        assertTrue(degraded.content.rawText.contains("Test Author"))
        assertTrue(degraded.content.rawText.contains("Test Description"))
        assertEquals("Test Title", degraded.content.metadata["title"])
        assertEquals("Test Author", degraded.content.metadata["channel"])
        assertEquals("Test Description", degraded.content.metadata["description"])
    }

    @Test
    fun testFailedResponse_returnsFailureResult() = runBlocking {
        val fakeResponse = ExtractVideoResponseDto(
            sourcePlatform = "YOUTUBE",
            providerStatus = "FAILED",
            accessStatus = "BLOCKED",
            capabilityStatus = mapOf("TRANSCRIPT" to "UNAVAILABLE", "METADATA" to "UNAVAILABLE"),
            metadata = null,
            content = null,
            errorInformation = ProviderErrorDto("UNAUTHORIZED", "Blocked")
        )
        
        val api = FakeSupabaseVideoApi(Response.success(fakeResponse))
        val extractor = RemoteVideoInputExtractor(api, "http://test", "key")
        
        val result = extractor.extract("url", "url", null, AnalysisType.KEY_TAKEAWAYS, null, "id")
        
        assertTrue(result is ContentExtractionResult.Failure)
        val failure = result as ContentExtractionResult.Failure
        assertEquals(ContentExtractionResult.Failure.ErrorType.BLOCKED_SOURCE, failure.errorType)
    }

    @Test
    fun testTimeoutOrError_returnsGeneralFailure() = runBlocking {
        val api = FakeSupabaseVideoApi(exceptionToThrow = RuntimeException("Timeout"))
        val extractor = RemoteVideoInputExtractor(api, "http://test", "key")
        
        val result = extractor.extract("url", "url", null, AnalysisType.KEY_TAKEAWAYS, null, "id")
        
        assertTrue(result is ContentExtractionResult.Failure)
        val failure = result as ContentExtractionResult.Failure
        assertEquals(ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR, failure.errorType)
    }
}
