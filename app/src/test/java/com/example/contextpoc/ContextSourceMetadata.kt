package com.example.contextpoc

data class ContextSourceMetadata(
    val sourceType: ContextSourceType,
    val title: String,
    val url: String,
    val externalId: String?,
    val latitude: Double?,
    val longitude: Double?
)
