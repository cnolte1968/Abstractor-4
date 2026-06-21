package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM cached_analyses ORDER BY timestamp DESC")
    fun getAllAnalysesFlow(): Flow<List<CachedAnalysisEntity>>

    @Query("SELECT * FROM cached_analyses ORDER BY timestamp DESC")
    suspend fun getAllAnalyses(): List<CachedAnalysisEntity>

    @Query("SELECT * FROM cached_analyses WHERE id = :id")
    suspend fun getAnalysisById(id: String): CachedAnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(entity: CachedAnalysisEntity)

    @Query("DELETE FROM cached_analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: String)

    @Query("DELETE FROM cached_analyses")
    suspend fun clearAll()
}

@Dao
interface UserCacheDao {
    @Query("SELECT * FROM user_cache WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveUser(): UserCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserCacheEntity)

    @Query("DELETE FROM user_cache")
    suspend fun clearUser()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM pending_sync_queue ORDER BY timestamp ASC")
    suspend fun getPendingItems(): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingItem(item: PendingSyncEntity)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deletePendingItemById(id: Long)
}
