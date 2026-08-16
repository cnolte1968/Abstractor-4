package com.example.domain.model

data class ExtractedContent(
    val sourceType: SourceType,
    val rawText: String,
    val enrichedText: String,
    val metadata: Map<String, String> = emptyMap(),
    val useSearchGrounding: Boolean = false,
    val confirmedProfile: SourceProfile? = null
)
