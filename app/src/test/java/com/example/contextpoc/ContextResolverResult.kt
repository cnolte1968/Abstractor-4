package com.example.contextpoc

data class ContextResolverResult(
    val status: ContextResolutionStatus,
    val contextText: String? = null,
    val sources: List<ContextSourceMetadata> = emptyList(),
    val matchConfidence: Double = 0.0
)
