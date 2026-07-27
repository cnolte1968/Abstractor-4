package com.example.data.engine

import com.example.domain.engine.AnalysisEngine
import com.example.domain.engine.EngineRunner
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary

class EngineRunnerImpl : EngineRunner {
    override suspend fun runEngine(engine: AnalysisEngine, input: CanonicalAnalysisInput): DomainSummary {
        val contract = engine.contract
        
        // 1. Validate Input against contract schema
        contract.validateInput(input)
        
        // 2. Execute isolated engine call
        val summary = engine.analyze(input)
        
        // 3. Validate Output against contract schema
        contract.validateOutput(summary)
        
        return summary
    }
}
