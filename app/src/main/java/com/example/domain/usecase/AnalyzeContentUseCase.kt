package com.example.domain.usecase

import com.example.data.AnalysisType
import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository

class AnalyzeContentUseCase(private val repository: AnalysisRepository) {
    suspend fun execute(
        url: String,
        contentText: String?,
        useSearchGrounding: Boolean,
        analysisType: AnalysisType
    ): DomainSummary {
        return repository.analyzeContent(url, contentText, useSearchGrounding, analysisType)
    }

    suspend fun executeFromFile(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): DomainSummary {
        return repository.analyzeFile(fileBytes, mimeType, fileName)
    }

    suspend fun executeFromText(
        text: String,
        fileName: String
    ): DomainSummary {
        return repository.analyzeText(text, fileName)
    }
}
