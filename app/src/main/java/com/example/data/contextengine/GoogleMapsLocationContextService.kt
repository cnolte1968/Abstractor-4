package com.example.data.contextengine

import com.example.data.GoogleMapsUrlParser

class GoogleMapsLocationContextService(
    private val contextEngine: ContextEngine = ContextEngine()
) {
    suspend fun fetchLocationContext(input: LocationContextInput): String {
        val effectiveInput = resolveEffectiveInput(input)
        val results = contextEngine.resolveContext(effectiveInput)
        return contextEngine.formatForGemini(results)
    }

    suspend fun fetchLocationContextResults(input: LocationContextInput): List<ContextResult> {
        val effectiveInput = resolveEffectiveInput(input)
        return contextEngine.resolveContext(effectiveInput)
    }

    private fun resolveEffectiveInput(input: LocationContextInput): LocationContextInput {
        val cleanedInputName = cleanPlaceName(input.placeName)
        val isInputNameValid = isValidPlaceName(cleanedInputName)

        val targetUrl = input.rawUrl ?: ""
        val isMapsUrl = targetUrl.isNotBlank() && GoogleMapsUrlParser.isGoogleMapsUrl(targetUrl)

        if (!isMapsUrl) {
            return input.copy(placeName = if (isInputNameValid) cleanedInputName else "")
        }

        var parsedResult: com.example.data.GoogleMapsPoCResult? = null
        try {
            val (resolvedUrl, resStatus) = GoogleMapsUrlParser.resolveShortUrl(targetUrl)
            parsedResult = GoogleMapsUrlParser.parseGoogleMapsUrl(targetUrl, targetUrl, resolvedUrl, resStatus)
        } catch (e: Exception) {
            // Fallback gracefully without crash
        }

        val parsedName = cleanPlaceName(parsedResult?.placeName ?: parsedResult?.searchQuery)
        val isParsedNameValid = isValidPlaceName(parsedName)

        val effectivePlaceName = when {
            isInputNameValid -> cleanedInputName
            isParsedNameValid -> parsedName
            else -> ""
        }

        val effectiveLat = parsedResult?.latitude ?: input.latitude
        val effectiveLng = parsedResult?.longitude ?: input.longitude
        val effectiveAddress = parsedResult?.address?.takeIf { it.isNotBlank() } ?: input.address

        return input.copy(
            placeName = effectivePlaceName,
            latitude = effectiveLat,
            longitude = effectiveLng,
            address = effectiveAddress
        )
    }

    private fun cleanPlaceName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return ""
        var cleaned = rawName.trim()

        val suffixes = listOf(
            " - Google Maps",
            " – Google Maps",
            " — Google Maps",
            " | Google Maps",
            " - Google Map",
            " – Google Map",
            " | Google Map",
            "- Google Maps",
            "– Google Maps",
            "— Google Maps",
            "| Google Maps"
        )
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
            }
        }

        val quoteChars = charArrayOf('"', '\'', '“', '”', '‘', '’', '«', '»')
        cleaned = cleaned.trimStart(*quoteChars).trimEnd(*quoteChars).trim()

        return cleaned
    }

    private fun isValidPlaceName(name: String): Boolean {
        if (name.isBlank()) return false
        val trimmed = name.trim()
        val lower = trimmed.lowercase()

        if (lower == "google maps" || lower == "google map" || lower == "maps") return false
        if (lower.contains("javascript") || lower.contains("enable javascript") || lower.contains("muss javascript aktiviert sein")) return false
        if (lower.contains("eliminated the javascript") || lower.contains("whatever remains")) return false

        return true
    }
}
