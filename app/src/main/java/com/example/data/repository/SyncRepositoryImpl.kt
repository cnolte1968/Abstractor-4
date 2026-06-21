package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AbstractorDatabase
import com.example.data.local.RoomConverters
import com.example.data.local.CachedAnalysisEntity
import com.example.data.remote.BackendApiService
import com.example.data.remote.SyncPushRequest
import com.example.domain.model.DomainSummary
import com.example.domain.repository.SyncRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

class SyncRepositoryImpl(
    private val database: AbstractorDatabase,
    private val apiService: BackendApiService
) : SyncRepository {

    private val syncQueueDao = database.syncQueueDao()
    private val analysisDao = database.analysisDao()
    private val userCacheDao = database.userCacheDao()
    private val converters = RoomConverters()

    private val moshi = com.example.data.SummaryResponseParser.moshiInstance
    private val summaryAdapter = moshi.adapter(DomainSummary::class.java)

    override suspend fun pushOfflineChanges() {
        // Query pending queue items in chronological order
        val pendingItems = syncQueueDao.getPendingItems()
        for (item in pendingItems) {
            try {
                when (item.actionType) {
                    "SAVE" -> {
                        val summaryJson = item.jsonData
                        if (!summaryJson.isNullOrEmpty()) {
                            val summary = summaryAdapter.fromJson(summaryJson)
                            if (summary != null) {
                                val response = apiService.createAnalysis(summary)
                                if (response.isSuccessful || response.code() == 409) {
                                    // Succeeded or target already exists 
                                    syncQueueDao.deletePendingItemById(item.id)
                                }
                            } else {
                                // JSON corrupted, delete from queue to avoid blockages
                                syncQueueDao.deletePendingItemById(item.id)
                            }
                        } else {
                            syncQueueDao.deletePendingItemById(item.id)
                        }
                    }
                    "DELETE" -> {
                        val response = apiService.deleteAnalysis(item.summaryId)
                        if (response.isSuccessful || response.code() == 404) {
                            // Succeeded or already deleted on server
                            syncQueueDao.deletePendingItemById(item.id)
                        }
                    }
                }
            } catch (e: Exception) {
                // Network still failing or other exception, halt processing of queue to retain chronological sequence
                break
            }
        }
    }

    override suspend fun pullRemoteChanges() {
        val user = userCacheDao.getActiveUser() ?: return
        try {
            // Retrieve latest summaries from remote endpoint
            val response = apiService.getUserAnalyses(user.id)
            if (response.isSuccessful && response.body() != null) {
                val remoteSummaries = response.body()!!
                // Integrate remote summaries into local Room DB cache in a fast single transaction
                database.withTransaction {
                    for (summary in remoteSummaries) {
                        val localEntity = CachedAnalysisEntity(
                            id = summary.id,
                            title = summary.title,
                            originalUrl = summary.originalUrl,
                            shortDescription = summary.shortDescription,
                            keyTakeaways = converters.fromTakeawayList(summary.keyTakeaways),
                            owner = summary.owner,
                            timestamp = summary.timestamp
                        )
                        analysisDao.insertAnalysis(localEntity)
                    }
                }
            }
        } catch (e: IOException) {
            // Silently complete if offline
        }
    }

    override suspend fun syncAll() {
        if (!com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            throw IllegalStateException("Synchronisation deaktiviert – Local-First Modus ist aktiv. Registrierung oder Login erforderlich – Synchronisation nicht möglich im lokalen Gastmodus.")
        }
        val user = userCacheDao.getActiveUser() ?: throw IllegalStateException("Registrierung oder Login erforderlich – Synchronisation nicht möglich im lokalen Gastmodus.")
        // Token session restoration for authorization headers
        BackendApiService.setToken(user.token)
        
        pushOfflineChanges()
        pullRemoteChanges()
    }

    override suspend fun getPendingQueueSize(): Int {
        return syncQueueDao.getPendingItems().size
    }
}
