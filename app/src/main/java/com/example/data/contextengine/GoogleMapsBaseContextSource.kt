package com.example.data.contextengine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleMapsBaseContextSource : ContextSource {

    override val sourceName: String = "GOOGLE_MAPS"
    override val sourceType: ContextSourceType = ContextSourceType.OFFICIAL_DATA

    override suspend fun fetchContext(input: LocationContextInput): ContextResult {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val rawPlaceName = input.placeName.trim()

            if (rawPlaceName.isBlank()) {
                return@withContext ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = false,
                    sourceUrl = input.rawUrl,
                    trustScore = 0.80,
                    confidenceScore = 0.0,
                    fetchedAtTimestamp = now,
                    metadata = mapOf("status" to "NO_CONTEXT_FOUND", "reason" to "Empty place name")
                )
            }

            // Extract ONLY allowed fields:
            // - Ortsname
            // - Kategorie
            // - Adresse / Lage
            // - offizielle Beschreibung bzw. allgemeine Ortsinformationen
            // ABSOLUTELY NO ratings, reviews, stars, or user opinions.

            val details = mutableListOf<String>()
            details.add("Ort: $rawPlaceName")

            if (!input.category.isNullOrBlank()) {
                details.add("Kategorie: ${input.category.trim()}")
            }

            if (!input.address.isNullOrBlank()) {
                details.add("Adresse: ${input.address.trim()}")
            } else if (input.latitude != null && input.longitude != null) {
                details.add("Lage: ${input.latitude}, ${input.longitude}")
            }

            if (!input.description.isNullOrBlank()) {
                details.add("Beschreibung: ${input.description.trim()}")
            }

            val snippet = details.joinToString("\n")

            ContextResult(
                sourceName = sourceName,
                sourceType = sourceType,
                isSuccessful = true,
                snippet = snippet,
                sourceUrl = input.rawUrl,
                trustScore = 0.80,
                confidenceScore = 0.70,
                fetchedAtTimestamp = now,
                metadata = mapOf(
                    "status" to "MATCHED",
                    "placeName" to rawPlaceName,
                    "hasCategory" to (!input.category.isNullOrBlank()).toString(),
                    "hasAddress" to (!input.address.isNullOrBlank()).toString(),
                    "hasDescription" to (!input.description.isNullOrBlank()).toString()
                )
            )
        }
    }
}
