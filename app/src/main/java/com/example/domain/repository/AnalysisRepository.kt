package com.example.domain.repository

import com.example.domain.model.DomainSummary

interface AnalysisRepository {
    suspend fun saveAnalysis(summary: DomainSummary)
    suspend fun getAllAnalyses(): List<DomainSummary>
    fun getAllAnalysesFlow(): kotlinx.coroutines.flow.Flow<List<DomainSummary>>
    suspend fun getAnalysisById(id: String): DomainSummary?
    suspend fun deleteAnalysis(id: String)
}
