package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AbstractorDatabase
import com.example.data.remote.BackendApiService
import com.example.data.repository.SyncRepositoryImpl
import java.io.IOException

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting WorkManager background synchronization...")
        
        if (!com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            Log.d("SyncWorker", "Background synchronization is disabled via BackendFeatureConfig.")
            return Result.success()
        }
        
        val db = AbstractorDatabase.getInstance(applicationContext)
        val api = BackendApiService.create()
        val syncRepo = SyncRepositoryImpl(db, api)

        return try {
            syncRepo.syncAll()
            Log.i("SyncWorker", "Background synchronization completed successfully.")
            Result.success()
        } catch (e: IllegalStateException) {
            // Guest mode / not logged in is not a failure, return success() but do not retry
            Log.i("SyncWorker", "Background sync skipped: ${e.message}")
            Result.success()
        } catch (e: IOException) {
            // Transient network issues or api server offline
            Log.w("SyncWorker", "Transient network error during background sync. Retrying...", e)
            Result.retry()
        } catch (e: Exception) {
            // Any other unpredictable errors
            Log.e("SyncWorker", "Unexpected error during background sync.", e)
            Result.retry()
        }
    }
}
