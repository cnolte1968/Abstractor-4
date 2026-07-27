package com.example

import com.example.data.repository.YoutubeTranscriptProviderAdapter
import com.example.domain.model.TranscriptResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TranscriptProviderTest {

    @Test
    fun testUnavailableOnInvalidUrl() = runBlocking {
        val adapter = YoutubeTranscriptProviderAdapter { null }
        val result = adapter.fetchTranscript("https://example.com/not-a-youtube-url")
        assertTrue(result is TranscriptResult.Unavailable)
    }

    @Test
    fun testAvailableOnValidTranscript() = runBlocking {
        val adapter = YoutubeTranscriptProviderAdapter { "valid transcript" }
        val result = adapter.fetchTranscript("https://www.youtube.com/watch?v=123")
        assertTrue(result is TranscriptResult.Available)
        val available = result as TranscriptResult.Available
        assertTrue(available.text == "valid transcript")
    }

    @Test
    fun testUnavailableOnEmptyTranscript() = runBlocking {
        val adapter = YoutubeTranscriptProviderAdapter { "" }
        val result = adapter.fetchTranscript("https://www.youtube.com/watch?v=123")
        assertTrue(result is TranscriptResult.Unavailable)
    }

    @Test
    fun testErrorOnException() = runBlocking {
        val adapter = YoutubeTranscriptProviderAdapter { throw RuntimeException("network error") }
        val result = adapter.fetchTranscript("https://www.youtube.com/watch?v=123")
        assertTrue(result is TranscriptResult.Error)
    }
}
