package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CachedAnalysisEntity::class,
        UserCacheEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AbstractorDatabase : RoomDatabase() {
    
    abstract fun analysisDao(): AnalysisDao
    abstract fun userCacheDao(): UserCacheDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AbstractorDatabase? = null

        fun getInstance(context: Context): AbstractorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AbstractorDatabase::class.java,
                    "abstractor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
