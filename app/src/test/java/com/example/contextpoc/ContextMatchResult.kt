package com.example.contextpoc

enum class MatchStatus { PASS, PARTIAL, NO_CONTEXT_FOUND, AMBIGUOUS_MATCH }

data class ContextMatchResult(
    val status: MatchStatus,
    val bestCandidate: ContextCandidate? = null
)
