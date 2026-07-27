package com.example.domain.model

enum class SourceType {
    WEB, YOUTUBE, DOCUMENT
}

data class CanonicalAnalysisInput(
    val sourceType: SourceType,
    val rawText: String,
    val enrichedText: String,
    val metadata: Map<String, String> = emptyMap(),
    val structuredExtras: Map<String, String> = emptyMap(),
    val rawBytes: ByteArray? = null,
    val mimeType: String? = null,
    val analysisId: String,
    val useSearchGrounding: Boolean = false,
    val analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.WEB_SUMMARY,
    val freeQuery: String? = null
)

