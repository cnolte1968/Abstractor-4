package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCurrentFormattedTimestamp(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN)
    return sdf.format(Date())
}

data class TakeawayItem(
    val title: String,
    val details: String,
    val visualMetadata: Map<String, String> = emptyMap()
)

data class DomainSummary(
    val id: String,
    val title: String,
    val originalUrl: String,
    val shortDescription: String,
    val keyTakeaways: List<TakeawayItem>,
    val owner: String? = null,
    val timestamp: String = getCurrentFormattedTimestamp(),
    val fallbackUsed: Boolean = false,
    val analysisId: String
)
