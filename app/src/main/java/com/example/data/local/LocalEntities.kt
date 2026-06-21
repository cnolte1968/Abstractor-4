package com.example.data.local

import androidx.room.*
import com.example.domain.model.TakeawayItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "cached_analyses")
data class CachedAnalysisEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "original_url") val originalUrl: String,
    @ColumnInfo(name = "short_description") val shortDescription: String,
    @ColumnInfo(name = "key_takeaways") val keyTakeaways: String, // Stored as JSON string
    val owner: String?,
    val timestamp: Long
)

@Entity(tableName = "user_cache")
data class UserCacheEntity(
    @PrimaryKey val id: String = "current_user",
    val username: String,
    val token: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(tableName = "pending_sync_queue")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "action_type") val actionType: String, // "SAVE" or "DELETE"
    @ColumnInfo(name = "summary_id") val summaryId: String,
    @ColumnInfo(name = "json_data") val jsonData: String?, // Full JSON string if saving
    val timestamp: Long = System.currentTimeMillis()
)

// Converter class for Room to convert List<TakeawayItem> to and from String using Moshi
class RoomConverters {
    private val moshi = com.example.data.SummaryResponseParser.moshiInstance
    
    private val takeawayListType = Types.newParameterizedType(List::class.java, TakeawayItem::class.java)
    private val adapter = moshi.adapter<List<TakeawayItem>>(takeawayListType)

    @TypeConverter
    fun fromTakeawayList(value: List<TakeawayItem>?): String {
        return if (value == null) "[]" else adapter.toJson(value)
    }

    @TypeConverter
    fun toTakeawayList(value: String?): List<TakeawayItem> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
