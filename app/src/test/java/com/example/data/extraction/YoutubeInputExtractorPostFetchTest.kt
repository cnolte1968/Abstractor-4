package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.data.YoutubeTranscriptHelper
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeInputExtractorPostFetchTest {

    private val extractor = YoutubeInputExtractor()

    @Test
    fun testInvalidUrlReturnsFailure() = runBlocking {
        val result = extractor.extract(
            rawUrl = "https://example.com/not-youtube",
            normalizedUrl = "https://example.com/not-youtube",
            directContent = null,
            analysisType = AnalysisType.WEB_SUMMARY,
            freeQuery = null,
            analysisId = "test-yt-invalid"
        )

        assertTrue(result is ContentExtractionResult.Failure)
    }

    @Test
    fun testYoutubeWithExtendedMetadataOnlyReturnsDegradedProfile() = runBlocking {
        val helperClass = YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient

        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()

                val responseBodyStr = when {
                    urlStr.contains("youtube.com/watch") -> {
                        "<html><head><title>Mock Video Title</title><meta name=\"description\" content=\"This is a mock description of the video which acts as extended content.\"></head><body>No caption tracks here!</body></html>"
                    }
                    urlStr.contains("youtubei/v1/player") -> {
                        """{"videoDetails":{"title":"Mock Video Title","author":"Mock Author","shortDescription":"This is a mock description of the video which acts as extended content."}}"""
                    }
                    urlStr.contains("oembed") -> {
                        """{"title": "Mock Video Title", "author_name": "Mock Author"}"""
                    }
                    else -> ""
                }

                Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBodyStr.toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()

        clientField.set(helperClass, mockClient)

        try {
            val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val result = extractor.extract(
                rawUrl = url,
                normalizedUrl = url,
                directContent = null,
                analysisType = AnalysisType.WEB_SUMMARY,
                freeQuery = null,
                analysisId = "test-yt-degraded"
            )

            assertTrue("Result should be Degraded", result is ContentExtractionResult.Degraded)
            val degraded = result as ContentExtractionResult.Degraded
            val profile = degraded.content.confirmedProfile

            assertNotNull("confirmedProfile should not be null", profile)
            assertEquals(SourceProfile.SourceType.VIDEO, profile?.sourceType)
            assertEquals(SourcePlatform.YOUTUBE, profile?.platform)
            assertTrue(profile?.isPostFetchConfirmed == true)
            assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.VIDEO_METADATA))
            assertEquals(CapabilityStatus.FAILED, profile?.getStatus(SourceCapability.TRANSCRIPT_TEXT))
        } finally {
            clientField.set(helperClass, originalYtClient)
        }
    }

    @Test
    fun testYoutubeWithTranscriptReturnsSuccessProfile() = runBlocking {
        val helperClass = YoutubeTranscriptHelper
        val clientField = helperClass.javaClass.getDeclaredField("client")
        clientField.isAccessible = true
        val originalYtClient = clientField.get(helperClass) as OkHttpClient

        val mockClient = originalYtClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val urlStr = request.url.toString()

                val responseBodyStr = when {
                    urlStr.contains("youtube.com/watch") -> {
                        """<html><head><title>Mock Video Title</title></head><body>"INNERTUBE_API_KEY": "test_api_key_123"</body></html>"""
                    }
                    urlStr.contains("youtubei/v1/player") -> {
                        """{"captions":{"playerCaptionsTracklistRenderer":{"captionTracks":[{"baseUrl":"https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ&lang=de","vssId":"de","languageCode":"de"}]}}}"""
                    }
                    urlStr.contains("timedtext") -> {
                        """<transcript><text start="0.0" dur="2.0">Hello wonderful world of YouTube transcripts. This is a very interesting video transcript with plenty of characters to pass the length check!</text></transcript>"""
                    }
                    urlStr.contains("oembed") -> {
                        """{"title": "Mock Video Title", "author_name": "Mock Author"}"""
                    }
                    else -> ""
                }

                Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBodyStr.toResponseBody("text/html".toMediaType()))
                    .build()
            }
            .build()

        clientField.set(helperClass, mockClient)

        try {
            val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val result = extractor.extract(
                rawUrl = url,
                normalizedUrl = url,
                directContent = null,
                analysisType = AnalysisType.WEB_SUMMARY,
                freeQuery = null,
                analysisId = "test-yt-success"
            )

            assertTrue("Result should be Success", result is ContentExtractionResult.Success)
            val success = result as ContentExtractionResult.Success
            val profile = success.content.confirmedProfile

            assertNotNull("confirmedProfile should not be null", profile)
            assertEquals(SourceProfile.SourceType.VIDEO, profile?.sourceType)
            assertEquals(SourcePlatform.YOUTUBE, profile?.platform)
            assertTrue(profile?.isPostFetchConfirmed == true)
            assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.VIDEO_METADATA))
            assertEquals(CapabilityStatus.AVAILABLE, profile?.getStatus(SourceCapability.TRANSCRIPT_TEXT))
        } finally {
            clientField.set(helperClass, originalYtClient)
        }
    }
}
