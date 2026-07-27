package com.example.data.repository

import android.content.Context
import com.example.data.local.RelevantorDatabase
import com.example.data.local.AnalysisEntity
import com.example.data.local.SessionStorage
import com.example.data.remote.BackendApiService
import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepositoryImpl(
    private val db: RelevantorDatabase,
    private val api: BackendApiService,
    private val context: Context
) : AnalysisRepository {

    override suspend fun saveAnalysis(summary: DomainSummary) {
        db.analysisDao().insertAnalysis(AnalysisEntity.fromDomain(summary))
        
        // Push to remote api if user is logged in
        val activeToken = SessionStorage.getActiveToken(context)
        if (activeToken != null) {
            try {
                api.createAnalysis(summary)
            } catch (e: Exception) {
                // Ignore network issues, local save is primary
            }
        }
    }

    override suspend fun getAllAnalyses(): List<DomainSummary> {
        return db.analysisDao().getAllAnalyses().map { it.toDomain() }
    }

    override fun getAllAnalysesFlow(): Flow<List<DomainSummary>> {
        return db.analysisDao().getAllAnalysesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAnalysisById(id: String): DomainSummary? {
        return db.analysisDao().getAnalysisById(id)?.toDomain()
    }

    override suspend fun deleteAnalysis(id: String) {
        db.analysisDao().deleteAnalysisById(id)
        
        val activeToken = SessionStorage.getActiveToken(context)
        if (activeToken != null) {
            try {
                api.deleteAnalysis(id)
            } catch (e: Exception) {
                // Ignore network issues
            }
        }
    }
}
