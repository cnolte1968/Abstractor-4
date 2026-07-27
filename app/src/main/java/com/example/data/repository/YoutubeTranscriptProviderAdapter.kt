package com.example.data.repository

import com.example.data.YoutubeTranscriptHelper
import com.example.domain.model.TranscriptProvider
import com.example.domain.model.TranscriptResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YoutubeTranscriptProviderAdapter(
    private val fetcher: (String) -> String? = { YoutubeTranscriptHelper.fetchTranscript(it) }
) : TranscriptProvider {
    override suspend fun fetchTranscript(url: String): TranscriptResult = withContext(Dispatchers.IO) {
        try {
            // Very basic URL to Video ID extraction
            val videoId = url.substringAfter("v=").substringBefore("&")
            
            if (videoId.isEmpty() || videoId == url) {
                return@withContext TranscriptResult.Unavailable("Invalid YouTube URL format")
            }

            val transcript = fetcher(videoId)
            
            if (!transcript.isNullOrBlank()) {
                TranscriptResult.Available(transcript, null, "YouTube")
            } else {
                TranscriptResult.Unavailable("No transcript found for video: $videoId")
            }
        } catch (e: Exception) {
            TranscriptResult.Error("Failed to fetch transcript: ${e.message}", e)
        }
    }
}
