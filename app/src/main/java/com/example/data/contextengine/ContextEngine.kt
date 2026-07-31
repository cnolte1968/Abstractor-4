package com.example.data.contextengine

class ContextEngine(
    private val sources: List<ContextSource> = listOf(
        WikipediaContextSource(),
        WikivoyageContextSource(),
        GoogleMapsBaseContextSource()
    )
) {
    suspend fun resolveContext(input: LocationContextInput): List<ContextResult> {
        if (sources.isEmpty()) {
            return listOf(
                ContextResult(
                    sourceName = "MOCK_SOURCE",
                    sourceType = ContextSourceType.UNKNOWN,
                    isSuccessful = true,
                    snippet = "Mock location context for ${input.placeName}",
                    trustScore = 0.5,
                    confidenceScore = 0.5,
                    metadata = mapOf("status" to "SKELETON_MOCK")
                )
            )
        }
        return sources.map { source ->
            try {
                source.fetchContext(input)
            } catch (e: Exception) {
                ContextResult(
                    sourceName = source.sourceName,
                    sourceType = source.sourceType,
                    isSuccessful = false,
                    trustScore = 0.0,
                    confidenceScore = 0.0,
                    metadata = mapOf("status" to "NO_CONTEXT_FOUND", "error" to (e.localizedMessage ?: "Unknown error")),
                    errorMessage = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun formatForGemini(results: List<ContextResult>): String {
        val facts = results.filter { it.isSuccessful && (it.sourceType == ContextSourceType.ENCYCLOPEDIA || it.sourceName == "WIKIPEDIA") }
            .mapNotNull { it.snippet }
            .joinToString("\n\n")

        val travelContext = results.filter { it.isSuccessful && (it.sourceType == ContextSourceType.TRAVEL_GUIDE || it.sourceName == "WIKIVOYAGE") }
            .mapNotNull { it.snippet }
            .joinToString("\n\n")

        val googleMapsContext = results.filter { it.isSuccessful && (it.sourceType == ContextSourceType.OFFICIAL_DATA || it.sourceName == "GOOGLE_MAPS" || it.sourceName == "GOOGLE_MAPS_BASE") }
            .mapNotNull { it.snippet }
            .joinToString("\n\n")

        val builder = StringBuilder()
        builder.append("=== FAKTEN ===\n")
        builder.append(if (facts.isNotBlank()) facts else "Keine enzyklopädischen Fakten verfügbar.")
        builder.append("\n\n=== REISEKONTEXT ===\n")
        builder.append(if (travelContext.isNotBlank()) travelContext else "Kein Reisekontext verfügbar.")

        if (googleMapsContext.isNotBlank()) {
            builder.append("\n\n=== GOOGLE MAPS BASISDATEN ===\n")
            builder.append(googleMapsContext)
        }

        return builder.toString()
    }
}

