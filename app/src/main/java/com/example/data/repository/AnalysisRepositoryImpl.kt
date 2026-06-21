package com.example.data.repository

import com.example.data.AnalysisType
import com.example.data.GeminiRepository
import com.example.data.local.AbstractorDatabase
import com.example.data.local.CachedAnalysisEntity
import com.example.data.local.PendingSyncEntity
import com.example.data.local.RoomConverters
import com.example.data.remote.BackendApiService
import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnalysisRepositoryImpl(
    private val database: AbstractorDatabase,
    private val apiService: BackendApiService
) : AnalysisRepository {

    private val analysisDao = database.analysisDao()
    private val syncQueueDao = database.syncQueueDao()
    private val userCacheDao = database.userCacheDao()
    private val converters = RoomConverters()
    
    private val moshi = com.example.data.SummaryResponseParser.moshiInstance
    private val summaryAdapter = moshi.adapter(DomainSummary::class.java)

    private fun CachedAnalysisEntity.toDomain(): DomainSummary {
        return DomainSummary(
            id = id,
            title = title,
            originalUrl = originalUrl,
            shortDescription = shortDescription,
            keyTakeaways = converters.toTakeawayList(keyTakeaways),
            owner = owner,
            timestamp = timestamp
        )
    }

    private fun DomainSummary.toEntity(): CachedAnalysisEntity {
        return CachedAnalysisEntity(
            id = id,
            title = title,
            originalUrl = originalUrl,
            shortDescription = shortDescription,
            keyTakeaways = converters.fromTakeawayList(keyTakeaways),
            owner = owner,
            timestamp = timestamp
        )
    }

    override fun getCachedAnalysesFlow(): Flow<List<DomainSummary>> {
        return analysisDao.getAllAnalysesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAnalysisById(id: String): DomainSummary? {
        return analysisDao.getAnalysisById(id)?.toDomain()
    }

    override suspend fun analyzeContent(
        url: String,
        contentText: String?,
        useSearchGrounding: Boolean,
        analysisType: AnalysisType
    ): DomainSummary {
        // Fetch from LLM isolated source
        val summary = GeminiRepository.summarize(url, contentText, useSearchGrounding, analysisType, null)
        
        // Enrich context with active username, if available
        val activeUser = userCacheDao.getActiveUser()
        val finalSummary = if (activeUser != null) {
            summary.copy(owner = activeUser.username)
        } else {
            summary
        }

        saveAnalysis(finalSummary)
        return finalSummary
    }

    override suspend fun analyzeFile(
        fileBytes: ByteArray,
        mimeType: String,
        fileName: String
    ): DomainSummary {
        val summary = GeminiRepository.summarizeFile(fileBytes, mimeType, fileName)
        val activeUser = userCacheDao.getActiveUser()
        val finalSummary = if (activeUser != null) {
            summary.copy(owner = activeUser.username)
        } else {
            summary
        }

        saveAnalysis(finalSummary)
        return finalSummary
    }

    override suspend fun analyzeText(
        text: String,
        fileName: String
    ): DomainSummary {
        val summary = GeminiRepository.summarizeText(text, fileName)
        val activeUser = userCacheDao.getActiveUser()
        val finalSummary = if (activeUser != null) {
            summary.copy(owner = activeUser.username)
        } else {
            summary
        }

        saveAnalysis(finalSummary)
        return finalSummary
    }

    override suspend fun saveAnalysis(summary: DomainSummary) {
        // 1. Primary Offline Cache
        analysisDao.insertAnalysis(summary.toEntity())

        // 2. Try push to remote backend if cloud sync feature is enabled and user is logged in
        if (com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            val user = userCacheDao.getActiveUser()
            if (user != null) {
                try {
                    // Real Remote Call
                    val response = apiService.createAnalysis(summary)
                    if (!response.isSuccessful) {
                        queueForSync(summary, "SAVE")
                    }
                } catch (e: Exception) {
                    // Network failed -> enter in Offline Sync Queue
                    queueForSync(summary, "SAVE")
                }
            }
        }
    }

    override suspend fun deleteAnalysis(id: String) {
        analysisDao.deleteAnalysisById(id)

        if (com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            val user = userCacheDao.getActiveUser()
            if (user != null) {
                try {
                    val response = apiService.deleteAnalysis(id)
                    if (!response.isSuccessful) {
                        queueDeleteForSync(id)
                    }
                } catch (e: Exception) {
                    queueDeleteForSync(id)
                }
            }
        }
    }

    override suspend fun clearAll() {
        analysisDao.clearAll()
    }

    private suspend fun queueForSync(summary: DomainSummary, actionType: String) {
        val json = try {
            summaryAdapter.toJson(summary)
        } catch (e: Exception) {
            null
        }
        syncQueueDao.insertPendingItem(
            PendingSyncEntity(
                actionType = actionType,
                summaryId = summary.id,
                jsonData = json
            )
        )
    }

    private suspend fun queueDeleteForSync(id: String) {
        syncQueueDao.insertPendingItem(
            PendingSyncEntity(
                actionType = "DELETE",
                summaryId = id,
                jsonData = null
            )
        )
    }
}
