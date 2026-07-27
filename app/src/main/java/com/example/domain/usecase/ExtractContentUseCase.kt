package com.example.domain.usecase

import com.example.domain.model.ContentExtractionResult
import com.example.domain.repository.ContentExtractionRepository
import com.example.data.AnalysisType

class ExtractContentUseCase(
    private val repository: ContentExtractionRepository
) {
    suspend fun execute(
        rawUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult {
        return repository.extractContent(rawUrl, directContent, analysisType, freeQuery, analysisId)
    }
}
