package com.example.contextpoc

import kotlin.math.*

object ContextCandidateMatcher {
    fun match(input: ContextPlaceInput, candidates: List<ContextCandidate>): ContextMatchResult {
        if (!ContextSuitabilityEvaluator.isSuitable(input.placeTypes)) {
            return ContextMatchResult(MatchStatus.NO_CONTEXT_FOUND)
        }

        val matches = candidates.map { candidate ->
            val distance = calculateDistance(input.latitude, input.longitude, candidate.latitude, candidate.longitude)
            val nameSimilarity = calculateNameSimilarity(input.name, candidate.wikipediaTitle)
            
            // Define tolerance based on object type
            val tolerance = when (candidate.objectType) {
                "monument", "church", "museum" -> 0.2 // 200m
                "park", "neighborhood" -> 2.0 // 2km
                "city", "country" -> 50.0 // 50km
                else -> 0.5
            }

            val isSpatialMatch = distance <= tolerance
            val isNameMatch = nameSimilarity > 0.7
            
            Triple(candidate, isSpatialMatch, isNameMatch)
        }.filter { it.second && it.third }

        return when {
            matches.isEmpty() -> ContextMatchResult(MatchStatus.NO_CONTEXT_FOUND)
            matches.size == 1 -> ContextMatchResult(MatchStatus.PASS, matches.first().first)
            else -> ContextMatchResult(MatchStatus.AMBIGUOUS_MATCH)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateNameSimilarity(name1: String, name2: String): Double {
        // Very basic similarity (just check if one contains the other for PoC)
        return if (name1.contains(name2, ignoreCase = true) || name2.contains(name1, ignoreCase = true)) 1.0 else 0.0
    }
}
