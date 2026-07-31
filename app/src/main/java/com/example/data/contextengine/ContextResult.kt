package com.example.data.contextengine

data class ContextResult(
    val sourceName: String,
    val sourceType: ContextSourceType = ContextSourceType.UNKNOWN,
    val isSuccessful: Boolean,
    val snippet: String? = null,
    val sourceUrl: String? = null,
    val trustScore: Double = 1.0,
    val confidenceScore: Double = 1.0,
    val fetchedAtTimestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)
