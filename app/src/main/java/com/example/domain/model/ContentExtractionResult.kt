package com.example.domain.model

sealed interface ContentExtractionResult {
    data class Success(val content: ExtractedContent) : ContentExtractionResult
    data class Degraded(val content: ExtractedContent) : ContentExtractionResult
    data class Predefined(val summary: DomainSummary) : ContentExtractionResult
    data class Failure(
        val errorType: ErrorType,
        val message: String,
        val detail: String? = null
    ) : ContentExtractionResult {
        enum class ErrorType {
            INSUFFICIENT_CONTENT,
            BLOCKED_SOURCE,
            TRANSCRIPT_UNAVAILABLE,
            INVALID_URL,
            GENERAL_ERROR
        }
    }
}
