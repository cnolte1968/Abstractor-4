package com.example.domain.usecase

import com.example.data.AnalysisType
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.EligibilityStatus
import com.example.domain.model.FunctionEligibility
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourceProfile

class FunctionEligibilityResolver {

    /**
     * Resolves function eligibility for a given analysis type and source profile.
     * All capabilities in [requiredCapabilities] must be satisfied by the profile.
     */
    fun resolveEligibility(
        analysisType: AnalysisType,
        sourceProfile: SourceProfile,
        requiredCapabilities: Set<SourceCapability>,
        optionalCapabilities: Set<SourceCapability> = emptySet(),
        allowedSourceTypes: Set<SourceProfile.SourceType>? = null
    ): FunctionEligibility {
        if (allowedSourceTypes != null && !allowedSourceTypes.contains(sourceProfile.sourceType)) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY,
                disabledReason = "Function not supported for this source type"
            )
        }

        if (requiredCapabilities.isEmpty()) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.ELIGIBLE
            )
        }

        val missingCaps = mutableListOf<SourceCapability>()
        val failedCaps = mutableListOf<SourceCapability>()
        var hasPotential = false
        var hasDegraded = false

        for (cap in requiredCapabilities) {
            when (sourceProfile.getStatus(cap)) {
                CapabilityStatus.AVAILABLE -> {
                    // Fully satisfied
                }
                CapabilityStatus.DEGRADED -> {
                    hasDegraded = true
                }
                CapabilityStatus.POTENTIAL -> {
                    hasPotential = true
                }
                CapabilityStatus.FAILED -> {
                    failedCaps.add(cap)
                }
                CapabilityStatus.UNAVAILABLE, CapabilityStatus.UNKNOWN -> {
                    missingCaps.add(cap)
                }
            }
        }

        if (failedCaps.isNotEmpty()) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.INELIGIBLE_FAILED,
                disabledReason = "Capability extraction failed: ${failedCaps.joinToString()}",
                missingCapabilities = failedCaps
            )
        }

        if (missingCaps.isNotEmpty()) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY,
                disabledReason = "Missing required capabilities: ${missingCaps.joinToString()}",
                missingCapabilities = missingCaps
            )
        }

        if (hasPotential) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.POTENTIAL,
                disabledReason = "Capabilities pending post-fetch confirmation"
            )
        }

        if (hasDegraded) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.DEGRADED,
                disabledReason = "Operating in degraded mode with limited content"
            )
        }

        return FunctionEligibility(
            analysisType = analysisType,
            status = EligibilityStatus.ELIGIBLE
        )
    }

    /**
     * Resolves eligibility when required capabilities can be satisfied by alternative capability groups.
     * Outer List has OR-semantics (at least one capability group must be satisfied).
     * Inner Set has AND-semantics (all capabilities in the group must be satisfied).
     */
    fun resolveEligibilityWithAlternatives(
        analysisType: AnalysisType,
        sourceProfile: SourceProfile,
        requiredAlternativeGroups: List<Set<SourceCapability>>,
        optionalCapabilities: Set<SourceCapability> = emptySet(),
        allowedSourceTypes: Set<SourceProfile.SourceType>? = null
    ): FunctionEligibility {
        if (allowedSourceTypes != null && !allowedSourceTypes.contains(sourceProfile.sourceType)) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY,
                disabledReason = "Function not supported for this source type"
            )
        }

        if (requiredAlternativeGroups.isEmpty()) {
            return FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.ELIGIBLE
            )
        }

        var bestStatus: CapabilityGroupRank? = null
        val failedCaps = mutableListOf<SourceCapability>()
        val missingCaps = mutableListOf<SourceCapability>()

        for (group in requiredAlternativeGroups) {
            if (group.isEmpty()) {
                return FunctionEligibility(
                    analysisType = analysisType,
                    status = EligibilityStatus.ELIGIBLE
                )
            }

            var groupHasFailed = false
            var groupHasMissing = false
            var groupHasPotential = false
            var groupHasDegraded = false

            val groupFailedCaps = mutableListOf<SourceCapability>()
            val groupMissingCaps = mutableListOf<SourceCapability>()

            for (cap in group) {
                when (sourceProfile.getStatus(cap)) {
                    CapabilityStatus.AVAILABLE -> {
                        // Fully satisfied
                    }
                    CapabilityStatus.POTENTIAL -> {
                        groupHasPotential = true
                    }
                    CapabilityStatus.DEGRADED -> {
                        groupHasDegraded = true
                    }
                    CapabilityStatus.FAILED -> {
                        groupHasFailed = true
                        groupFailedCaps.add(cap)
                    }
                    CapabilityStatus.UNAVAILABLE, CapabilityStatus.UNKNOWN -> {
                        groupHasMissing = true
                        groupMissingCaps.add(cap)
                    }
                }
            }

            if (groupHasFailed) {
                failedCaps.addAll(groupFailedCaps)
            } else if (groupHasMissing) {
                missingCaps.addAll(groupMissingCaps)
            } else if (groupHasPotential) {
                if (bestStatus == null || bestStatus < CapabilityGroupRank.POTENTIAL) {
                    bestStatus = CapabilityGroupRank.POTENTIAL
                }
            } else if (groupHasDegraded) {
                if (bestStatus == null || bestStatus < CapabilityGroupRank.DEGRADED) {
                    bestStatus = CapabilityGroupRank.DEGRADED
                }
            } else {
                // All caps in group are AVAILABLE
                return FunctionEligibility(
                    analysisType = analysisType,
                    status = EligibilityStatus.ELIGIBLE
                )
            }
        }

        return when (bestStatus) {
            CapabilityGroupRank.POTENTIAL -> FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.POTENTIAL,
                disabledReason = "Capabilities pending post-fetch confirmation"
            )
            CapabilityGroupRank.DEGRADED -> FunctionEligibility(
                analysisType = analysisType,
                status = EligibilityStatus.DEGRADED,
                disabledReason = "Operating in degraded mode with limited content"
            )
            null -> {
                if (failedCaps.isNotEmpty()) {
                    FunctionEligibility(
                        analysisType = analysisType,
                        status = EligibilityStatus.INELIGIBLE_FAILED,
                        disabledReason = "Capability extraction failed for required capability group(s)",
                        missingCapabilities = failedCaps.distinct()
                    )
                } else {
                    FunctionEligibility(
                        analysisType = analysisType,
                        status = EligibilityStatus.INELIGIBLE_MISSING_CAPABILITY,
                        disabledReason = "Missing required capabilities for all alternative groups",
                        missingCapabilities = missingCaps.distinct()
                    )
                }
            }
        }
    }

    private enum class CapabilityGroupRank {
        DEGRADED,
        POTENTIAL
    }
}
