package com.example.domain.model

import com.example.data.AnalysisType

enum class EligibilityStatus {
    ELIGIBLE,
    POTENTIAL,
    DEGRADED,
    INELIGIBLE_MISSING_CAPABILITY,
    INELIGIBLE_FAILED
}

data class FunctionEligibility(
    val analysisType: AnalysisType,
    val status: EligibilityStatus,
    val disabledReason: String? = null,
    val missingCapabilities: List<SourceCapability> = emptyList()
)
