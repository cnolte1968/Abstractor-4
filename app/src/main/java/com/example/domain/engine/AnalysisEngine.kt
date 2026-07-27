package com.example.domain.engine

import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary

interface AnalysisEngine {
    val contract: EngineContract
    suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary
}
