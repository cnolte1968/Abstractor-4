package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SupabaseVideoApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: SupabaseVideoApi

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(SupabaseVideoApi::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test1_successResponse_correctlyParsed() = runBlocking {
        val successJson = """
            {
              "sourcePlatform": "YOUTUBE",
              "providerStatus": "SUCCESS",
              "accessStatus": "PUBLIC",
              "capabilityStatus": {
                "TRANSCRIPT": "AVAILABLE",
                "METADATA": "AVAILABLE"
              },
              "metadata": {
                "title": "Test Title",
                "author": "Test Author",
                "durationSeconds": 120
              },
              "content": {
                "transcript": "Hello World",
                "language": "en",
                "timestamps": [{"time": 0.0, "text": "Hello World"}]
              },
              "errorInformation": null
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(successJson))

        val req = ExtractVideoRequestDto(
            sourceUrl = "http://test",
            sourcePlatform = "YOUTUBE",
            requestedCapabilities = listOf("TRANSCRIPT", "METADATA")
        )

        val response = api.extractVideo(mockWebServer.url("/functions/v1/extract-video").toString(), null, null, req)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("SUCCESS", body?.providerStatus)
        assertEquals("PUBLIC", body?.accessStatus)
        assertEquals("Test Title", body?.metadata?.title)
        assertEquals("Hello World", body?.content?.transcript)
    }

    @Test
    fun test2_degradedResponse_metadataWithoutTranscriptCorrectlyParsed() = runBlocking {
        val degradedJson = """
            {
              "sourcePlatform": "YOUTUBE",
              "providerStatus": "DEGRADED",
              "accessStatus": "PUBLIC",
              "capabilityStatus": {
                "TRANSCRIPT": "UNAVAILABLE",
                "METADATA": "AVAILABLE"
              },
              "metadata": {
                "title": "Only Metadata"
              },
              "content": null,
              "errorInformation": {
                "code": "NO_TRANSCRIPT",
                "message": "Transcript missing"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(degradedJson))

        val req = ExtractVideoRequestDto(
            sourceUrl = "http://test",
            sourcePlatform = "YOUTUBE",
            requestedCapabilities = listOf("TRANSCRIPT")
        )

        val response = api.extractVideo(mockWebServer.url("/functions/v1/extract-video").toString(), null, null, req)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("DEGRADED", body?.providerStatus)
        assertEquals("UNAVAILABLE", body?.capabilityStatus?.get("TRANSCRIPT"))
        assertEquals("Only Metadata", body?.metadata?.title)
        assertNull(body?.content)
        assertEquals("NO_TRANSCRIPT", body?.errorInformation?.code)
    }

    @Test
    fun test3_failedResponse_errorObjectCorrectlyParsed() = runBlocking {
        val failedJson = """
            {
              "sourcePlatform": "YOUTUBE",
              "providerStatus": "FAILED",
              "accessStatus": "BLOCKED",
              "capabilityStatus": {
                "TRANSCRIPT": "UNAVAILABLE"
              },
              "metadata": null,
              "content": null,
              "errorInformation": {
                "code": "UNAUTHORIZED",
                "message": "Blocked access"
              }
            }
        """.trimIndent()

        // 200 OK because the Supabase Edge function caught the error and returns a valid contract payload
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(failedJson))

        val req = ExtractVideoRequestDto(
            sourceUrl = "http://test",
            sourcePlatform = "YOUTUBE",
            requestedCapabilities = emptyList()
        )

        val response = api.extractVideo(mockWebServer.url("/functions/v1/extract-video").toString(), null, null, req)
        
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("FAILED", body?.providerStatus)
        assertEquals("BLOCKED", body?.accessStatus)
        assertEquals("UNAUTHORIZED", body?.errorInformation?.code)
    }

    @Test
    fun test4_timeoutOrHttpError_cleanErrorState() = runBlocking {
        // Enqueue a response that just hangs / timeouts or returns HTTP 500
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val req = ExtractVideoRequestDto(
            sourceUrl = "http://test",
            sourcePlatform = "YOUTUBE",
            requestedCapabilities = emptyList()
        )

        val response = api.extractVideo(mockWebServer.url("/functions/v1/extract-video").toString(), null, null, req)
        
        assertTrue(!response.isSuccessful)
        assertEquals(500, response.code())
        assertNull(response.body())
    }
}
