package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey val id: String,
    val title: String,
    val originalUrl: String,
    val shortDescription: String,
    val keyTakeawaysJson: String,
    val owner: String?,
    val timestamp: String,
    val analysisId: String
) {
    fun toDomain(): DomainSummary {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, TakeawayItem::class.java)
        val adapter = moshi.adapter<List<TakeawayItem>>(type)
        val takeaways = try {
            adapter.fromJson(keyTakeawaysJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return DomainSummary(
            id = id,
            title = title,
            originalUrl = originalUrl,
            shortDescription = shortDescription,
            keyTakeaways = takeaways,
            owner = owner,
            timestamp = timestamp,
            analysisId = analysisId
        )
    }

    companion object {
        fun fromDomain(summary: DomainSummary): AnalysisEntity {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, TakeawayItem::class.java)
            val adapter = moshi.adapter<List<TakeawayItem>>(type)
            val json = adapter.toJson(summary.keyTakeaways)
            return AnalysisEntity(
                id = summary.id,
                title = summary.title,
                originalUrl = summary.originalUrl,
                shortDescription = summary.shortDescription,
                keyTakeawaysJson = json,
                owner = summary.owner,
                timestamp = summary.timestamp,
                analysisId = summary.analysisId
            )
        }
    }
}

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analyses")
    suspend fun getAllAnalyses(): List<AnalysisEntity>

    @Query("SELECT * FROM analyses ORDER BY timestamp DESC")
    fun getAllAnalysesFlow(): kotlinx.coroutines.flow.Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE id = :id LIMIT 1")
    suspend fun getAnalysisById(id: String): AnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: String)
}

@Database(entities = [AnalysisEntity::class], version = 5, exportSchema = false)
abstract class RelevantorDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao

    companion object {
        @JvmField
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analyses ADD COLUMN analysisId TEXT NOT NULL DEFAULT ''")
                android.util.Log.i("RelevantorDatabase", "Successfully ran migration from version 4 to 5")
            }
        }

        @Volatile
        private var INSTANCE: RelevantorDatabase? = null

        fun getInstance(context: Context): RelevantorDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                tryCopyLegacyDatabase(appContext)
                val instance = Room.databaseBuilder(
                    appContext,
                    RelevantorDatabase::class.java,
                    "relevantor_database"
                )
                .addMigrations(MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun tryCopyLegacyDatabase(context: Context) {
            try {
                val newDbFile = context.getDatabasePath("relevantor_database")
                if (newDbFile.exists()) {
                    return
                }

                val oldDbFile = context.getDatabasePath("abstractor_database")
                if (!oldDbFile.exists()) {
                    return
                }

                // Ensure parent directory exists
                newDbFile.parentFile?.mkdirs()

                // Copy the main database file
                oldDbFile.copyTo(newDbFile, overwrite = true)

                // Copy -wal and -shm files if they exist
                val oldWalFile = java.io.File(oldDbFile.absolutePath + "-wal")
                if (oldWalFile.exists()) {
                    val newWalFile = java.io.File(newDbFile.absolutePath + "-wal")
                    oldWalFile.copyTo(newWalFile, overwrite = true)
                }

                val oldShmFile = java.io.File(oldDbFile.absolutePath + "-shm")
                if (oldShmFile.exists()) {
                    val newShmFile = java.io.File(newDbFile.absolutePath + "-shm")
                    oldShmFile.copyTo(newShmFile, overwrite = true)
                }

                android.util.Log.i("RelevantorDatabase", "Successfully copied legacy abstractor_database to relevantor_database")
            } catch (e: Throwable) {
                android.util.Log.e("RelevantorDatabase", "Failed to migrate legacy abstractor_database", e)
            }
        }
    }
}
