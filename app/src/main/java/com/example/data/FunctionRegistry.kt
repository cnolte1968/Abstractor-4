package com.example.data

data class FunctionRegistry(
    val functions: List<FunctionConfig> = emptyList()
)

data class FunctionConfig(
    val functionId: String,
    val label: String,
    val subtitle: String = "",
    val category: String = "GENERAL",
    val categoryLabel: String = "Allgemein",
    val promptFile: String,
    val enabled: Boolean = true,
    val favoriteDefault: Boolean = false,
    val inputConfig: InputConfig = InputConfig(),
    val outputSchema: String = "relevantorSummarySchema",
    val renderingConfig: RenderingConfig = RenderingConfig(),
    val cardinalityConfig: CardinalityConfig = CardinalityConfig(),
    val groundingConfig: GroundingConfig = GroundingConfig(),
    val errorBehavior: String = "ANALYSIS_ERROR",
    val fallbackBehavior: String = "NONE",
    val ownerRequired: Boolean = false,
    val originalUrlRequired: Boolean = false,
    val showOwner: Boolean = true,
    val showSource: Boolean = true,
    val showTimestamp: Boolean = true,
    val pdfExportEnabled: Boolean = true
)

data class InputConfig(
    val inputType: String = "URL_OR_TEXT",
    val acceptedMimeTypes: List<String> = emptyList(),
    val preferredSourceOrder: List<String> = emptyList(),
    val supportsUrl: Boolean = true,
    val supportsText: Boolean = true,
    val supportsFile: Boolean = false,
    val supportsBinaryPayload: Boolean = false,
    val supportsTextExtraction: Boolean = true,
    val maxInputChars: Int = 15000,
    val maxFileSizeMb: Int = 10
)

data class RenderingConfig(
    val renderingProfile: String = "summary_bullets",
    val listStyle: String = "BULLET"
)

data class CardinalityConfig(
    val minItems: Int = 1,
    val targetItems: Int = 3,
    val maxItems: Int = 10,
    val allowFewerItems: Boolean = true,
    val forbidPadding: Boolean = true
)

data class GroundingConfig(
    val groundingMode: String = "OPTIONAL"
)
