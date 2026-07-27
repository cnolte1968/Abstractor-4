package com.example.data.repository

import android.content.Context
import com.example.data.local.RelevantorDatabase
import com.example.data.local.AnalysisEntity
import com.example.data.local.SessionStorage
import com.example.data.remote.BackendApiService
import com.example.data.remote.SyncPushRequest
import com.example.domain.repository.SyncRepository

class SyncRepositoryImpl(
    private val db: RelevantorDatabase,
    private val api: BackendApiService,
    private val context: Context
) : SyncRepository {

    override suspend fun syncAll() {
        val activeUsername = SessionStorage.getActiveUsername(context)
            ?: throw IllegalStateException("Synchronisierung ist im lokalen Gastmodus nicht möglich. Bitte registriere dich oder melde dich an.")

        // For registered users, sync push/pull
        val localAnalyses = db.analysisDao().getAllAnalyses().map { it.toDomain() }
        
        // Push local content
        api.syncPush(SyncPushRequest(localAnalyses))
        
        // Pull remote content
        val remoteResponse = api.syncPull()
        if (remoteResponse.isSuccessful && remoteResponse.body() != null) {
            remoteResponse.body()!!.forEach { summary ->
                db.analysisDao().insertAnalysis(AnalysisEntity.fromDomain(summary))
            }
        }
    }
}
