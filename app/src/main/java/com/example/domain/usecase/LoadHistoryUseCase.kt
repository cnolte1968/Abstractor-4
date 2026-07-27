package com.example.domain.usecase

import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoadHistoryUseCase(
    private val repository: AnalysisRepository
) {
    fun execute(): Flow<List<DomainSummary>> = repository.getAllAnalysesFlow()
}
