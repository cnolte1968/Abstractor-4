package com.example.data

data class AnalysisRuntimeConfig(
    val temperature: Double,
    val forceGrounding: Boolean,
    val allowUserGrounding: Boolean,
    val useJsonSchemaWhenUngrounded: Boolean
)

object AnalysisRuntimeConfigs {
    fun forType(analysisType: AnalysisType): AnalysisRuntimeConfig {
        return when (analysisType) {
            AnalysisType.STANDARD_WEBSEITE -> AnalysisRuntimeConfig(
                temperature = 0.2,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.MULTIMEDIA -> AnalysisRuntimeConfig(
                temperature = 0.2,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.DOKUMENTE -> AnalysisRuntimeConfig(
                temperature = 0.2,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.TOP_3_KERNAUSSAGEN -> AnalysisRuntimeConfig(
                temperature = 0.2,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.AKTUALITAETS_CHECK -> AnalysisRuntimeConfig(
                temperature = 0.3,
                forceGrounding = true,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.FEHLINFORMATIONS_RADAR -> AnalysisRuntimeConfig(
                temperature = 0.1,
                forceGrounding = true,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.RISIKO_ANALYSE -> AnalysisRuntimeConfig(
                temperature = 0.4,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.BUSINESS_INKUBATOR -> AnalysisRuntimeConfig(
                temperature = 0.8,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> AnalysisRuntimeConfig(
                temperature = 0.1,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
            AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS -> AnalysisRuntimeConfig(
                temperature = 0.2,
                forceGrounding = false,
                allowUserGrounding = true,
                useJsonSchemaWhenUngrounded = true
            )
        }
    }
}
