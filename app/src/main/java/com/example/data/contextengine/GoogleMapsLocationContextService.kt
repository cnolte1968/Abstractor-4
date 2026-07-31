package com.example.data.contextengine

class GoogleMapsLocationContextService(
    private val contextEngine: ContextEngine = ContextEngine()
) {
    suspend fun fetchLocationContext(input: LocationContextInput): String {
        val results = contextEngine.resolveContext(input)
        return contextEngine.formatForGemini(results)
    }

    suspend fun fetchLocationContextResults(input: LocationContextInput): List<ContextResult> {
        return contextEngine.resolveContext(input)
    }
}
