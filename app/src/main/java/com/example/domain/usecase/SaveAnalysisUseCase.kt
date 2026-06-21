package com.example.domain.usecase

import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository

class SaveAnalysisUseCase(private val repository: AnalysisRepository) {
    suspend fun execute(summary: DomainSummary) {
        repository.saveAnalysis(summary)
    }
}
