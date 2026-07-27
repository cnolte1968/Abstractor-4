package com.example.data

data class AnalysisRuntimeConfig(
    val forceGrounding: Boolean,
    val temperature: Double,
    val maxOutputTokens: Int = 4096
)

object AnalysisRuntimeConfigs {
    fun forType(type: AnalysisType): AnalysisRuntimeConfig {
        val canonical = type.canonical()
        val forceGrounding = canonical == AnalysisType.FRESHNESS_CHECK || canonical == AnalysisType.MISINFORMATION_RADAR || type == AnalysisType.AKTUALITAETS_CHECK || type == AnalysisType.FEHLINFORMATIONS_RADAR
        val temperature = when (canonical) {
            AnalysisType.WEB_SUMMARY -> 0.4
            AnalysisType.KEY_TAKEAWAYS -> 0.4
            AnalysisType.MISINFORMATION_RADAR -> 0.1
            AnalysisType.FACTS_VS_OPINIONS -> 0.1
            AnalysisType.FRESHNESS_CHECK -> 0.3
            AnalysisType.RISK_ANALYSIS -> 0.4
            AnalysisType.BUSINESS_INKUBATOR -> 0.8
            else -> 0.2
        }
        return AnalysisRuntimeConfig(forceGrounding, temperature)
    }
}
