package com.example.domain.repository

import com.example.data.AnalysisType
import com.example.domain.model.DomainSummary
import kotlinx.coroutines.flow.Flow
interface AnalysisRepository {
    fun getCachedAnalysesFlow(): Flow<List<DomainSummary>>
    suspend fun getAnalysisById(id: String): DomainSummary?
    
    suspend fun analyzeContent(
        url: String,
        contentText: String?,
        useSearchGrounding: Boolean,
        analysisType: AnalysisType
    ): DomainSummary

    suspend fun analyzeFile(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): DomainSummary

    suspend fun analyzeText(
        text: String,
        fileName: String
    ): DomainSummary

    suspend fun saveAnalysis(summary: DomainSummary)
    suspend fun deleteAnalysis(id: String)
    suspend fun clearAll()
}
