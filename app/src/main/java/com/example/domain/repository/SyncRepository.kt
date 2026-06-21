package com.example.domain.repository

interface SyncRepository {
    suspend fun pushOfflineChanges()
    suspend fun pullRemoteChanges()
    suspend fun syncAll()
    suspend fun getPendingQueueSize(): Int
}
