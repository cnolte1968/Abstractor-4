package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.domain.model.ContentExtractionResult

interface InputExtractor {
    fun supports(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): Boolean

    suspend fun extract(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult
}
