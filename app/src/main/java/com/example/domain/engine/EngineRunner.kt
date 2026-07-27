package com.example.domain.engine

import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary

interface EngineRunner {
    suspend fun runEngine(engine: AnalysisEngine, input: CanonicalAnalysisInput): DomainSummary
}
