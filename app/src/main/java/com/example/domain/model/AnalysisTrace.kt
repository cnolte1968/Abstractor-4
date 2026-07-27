package com.example.domain.model

import android.util.Log

data class AnalysisTrace(
    val analysisId: String,
    val functionId: String,
    val sourceType: String,
    val displayName: String,
    val mimeType: String,
    val byteSize: Long,
    val promptFile: String,
    val promptHash: String,
    val model: String,
    val requestMode: String,
    val httpStatus: Int,
    val rawResponseLength: Int,
    val parserSuccess: Boolean,
    val takeawayCount: Int,
    val fallbackUsed: Boolean,
    val exceptionClass: String,
    val exceptionMessage: String,
    val durationMs: Long
) {
    fun validateOrThrow() {
        if (analysisId.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: analysisId is blank")
        if (functionId.isBlank() || functionId == "unknown") throw IllegalStateException("AnalysisTrace validation failed: functionId is invalid ($functionId)")
        if (sourceType.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: sourceType is blank")
        if (displayName.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: displayName is blank")
        if (promptFile.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: promptFile is blank")
        if (promptHash.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: promptHash is blank")
        if (model.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: model is blank")
        if (requestMode.isBlank()) throw IllegalStateException("AnalysisTrace validation failed: requestMode is blank")
    }

    fun log() {
        validateOrThrow()
        val msg = """
            ANALYSIS_TRACE
            analysisId: $analysisId
            functionId: $functionId
            sourceType: $sourceType
            displayName: $displayName
            mimeType: $mimeType
            byteSize: $byteSize
            promptFile: $promptFile
            promptHash: $promptHash
            model: $model
            requestMode: $requestMode
            httpStatus: $httpStatus
            rawResponseLength: $rawResponseLength
            parserSuccess: $parserSuccess
            takeawayCount: $takeawayCount
            fallbackUsed: $fallbackUsed
            exception: ${if (exceptionClass.isNotEmpty() && exceptionClass != "None") "$exceptionClass: $exceptionMessage" else "null"}
            durationMs: $durationMs
        """.trimIndent()
        Log.i("ANALYSIS_TRACE", msg)
        println("ANALYSIS_TRACE: $msg")
    }
}
