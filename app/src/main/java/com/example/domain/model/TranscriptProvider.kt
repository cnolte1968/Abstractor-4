package com.example.domain.model

sealed interface TranscriptResult {
    data class Available(val text: String, val language: String?, val provider: String) : TranscriptResult
    data class Unavailable(val reason: String) : TranscriptResult
    data class Error(val message: String, val throwable: Throwable?) : TranscriptResult
}

interface TranscriptProvider {
    suspend fun fetchTranscript(url: String): TranscriptResult
}
