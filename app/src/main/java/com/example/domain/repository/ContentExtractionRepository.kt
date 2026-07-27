package com.example.domain.repository

import com.example.domain.model.ContentExtractionResult
import com.example.data.AnalysisType

interface ContentExtractionRepository {
    suspend fun extractContent(
        rawUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult
}
