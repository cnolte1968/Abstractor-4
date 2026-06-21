package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val UNIQUE_PERIODIC_WORK_NAME = "com.example.data.sync.PERIODIC_SYNC"
    private const val UNIQUE_ONE_TIME_WORK_NAME = "com.example.data.sync.ONE_TIME_SYNC"

    fun schedulePeriodicSync(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        
        if (!com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            Log.d("SyncScheduler", "Periodic background sync disabled via BackendFeatureConfig. Cancelling work.")
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(3, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.d("SyncScheduler", "Periodic background sync (every 3 hours) scheduled with NetworkType.CONNECTED constraint.")
    }

    fun enqueueOneTimeSync(context: Context) {
        if (!com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            Log.d("SyncScheduler", "One-time immediate sync requested but disabled via BackendFeatureConfig. Skipping.")
            return
        }
        val workManager = WorkManager.getInstance(context.applicationContext)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
        Log.d("SyncScheduler", "One-time immediate backup sync enqueued with NetworkType.CONNECTED constraint.")
    }
}
