package com.example.domain.model

data class TakeawayItem(
    val title: String,
    val details: String
)

data class DomainSummary(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val originalUrl: String,
    val shortDescription: String,
    val keyTakeaways: List<TakeawayItem>,
    val owner: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
