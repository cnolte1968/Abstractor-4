package com.example.data.contextengine

interface ContextSource {
    val sourceName: String
    val sourceType: ContextSourceType
    suspend fun fetchContext(input: LocationContextInput): ContextResult
}
