package com.example.contextpoc

object ContextSuitabilityEvaluator {
    fun isSuitable(placeTypes: List<String>): Boolean {
        val suitableTypes = setOf(
            "tourist_attraction", "museum", "monument", "church", "hindu_temple",
            "park", "neighborhood", "locality", "country", "historical_site"
        )
        return placeTypes.any { it in suitableTypes }
    }
}
