package com.example.data.contextengine

import android.content.Context
import com.example.data.AnalysisType
import com.example.data.GoogleMapsUrlParser
import java.security.MessageDigest

data class LocationContextDiagnosticReport(
    val inputUrl: String,
    val placeName: String,
    val placeData: String,
    val analysisType: String,
    val loadedPromptPath: String,
    val loadedPromptSha256: String,
    val serviceCallStatus: String,
    val contextEngineStatus: String,
    val wikipediaResultTitle: String,
    val wikipediaResultStatus: String,
    val wikipediaResultSnippet: String,
    val wikipediaResultCharCount: Int,
    val wikivoyageResultTitle: String,
    val wikivoyageResultStatus: String,
    val wikivoyageResultSnippet: String,
    val wikivoyageResultCharCount: Int,
    val googleMapsResultStatus: String = "NOT_RUN",
    val googleMapsResultSnippet: String = "",
    val googleMapsResultCharCount: Int = 0,
    val geminiContextInjectionLength: Int,
    val geminiContextInjectionHasFacts: Boolean,
    val geminiContextInjectionHasTravelContext: Boolean,
    val geminiContextInjectionHasGoogleMapsBase: Boolean = false,
    val geminiContextInjectionPreview: String,
    val finalContractStatus: String,
    val timestamp: String = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
) {
    fun toFormattedString(): String {
        return buildString {
            appendLine("=== LOCATION CONTEXT DIAGNOSE REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("1. Input URL: $inputUrl")
            appendLine("2. Erkannter Ort / Place Daten: Ort: '$placeName' | Details: $placeData")
            appendLine("3. Verwendeter AnalysisType: $analysisType")
            appendLine("4. Geladener Prompt: Pfad: $loadedPromptPath | SHA256: $loadedPromptSha256")
            appendLine("5. Aufruf GoogleMapsLocationContextService: $serviceCallStatus")
            appendLine("6. ContextEngine Status: $contextEngineStatus")
            appendLine("7. WikipediaContextSource Ergebnis: Status: $wikipediaResultStatus | Titel: '$wikipediaResultTitle' | Zeichen: $wikipediaResultCharCount")
            if (wikipediaResultSnippet.isNotBlank()) {
                appendLine("   Auszug Wikipedia: ${wikipediaResultSnippet.take(200).replace("\n", " ")}...")
            }
            appendLine("8. WikivoyageContextSource Ergebnis: Status: $wikivoyageResultStatus | Titel: '$wikivoyageResultTitle' | Zeichen: $wikivoyageResultCharCount")
            if (wikivoyageResultSnippet.isNotBlank()) {
                appendLine("   Auszug Wikivoyage: ${wikivoyageResultSnippet.take(200).replace("\n", " ")}...")
            }
            appendLine("8b. GoogleMapsBaseContextSource Ergebnis: Status: $googleMapsResultStatus | Zeichen: $googleMapsResultCharCount")
            if (googleMapsResultSnippet.isNotBlank()) {
                appendLine("   Auszug Google Maps: ${googleMapsResultSnippet.take(200).replace("\n", " ")}...")
            }
            appendLine("9. Gemini Context Injection:")
            appendLine("   Länge: $geminiContextInjectionLength Zeichen")
            appendLine("   Enthält '=== FAKTEN ===': $geminiContextInjectionHasFacts")
            appendLine("   Enthält '=== REISEKONTEXT ===': $geminiContextInjectionHasTravelContext")
            appendLine("   Enthält '=== GOOGLE MAPS BASISDATEN ===': $geminiContextInjectionHasGoogleMapsBase")
            appendLine("   Vorschau Injection:\n$geminiContextInjectionPreview")
            appendLine("10. Finaler Contract Status: $finalContractStatus")
        }
    }
}

object LocationContextDiagnosticRunner {

    suspend fun runDiagnosis(
        context: Context,
        inputUrl: String = "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep",
        customPlaceName: String? = null
    ): LocationContextDiagnosticReport {
        val type = AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT
        val analysisTypeName = type.name

        // 1. Determine Place Name & Place Data
        val parsedResult = try {
            GoogleMapsUrlParser.parseGoogleMapsUrl(inputUrl, inputUrl, inputUrl, "SUCCESS")
        } catch (e: Exception) {
            null
        }

        val parsedPlace = if (!customPlaceName.isNullOrBlank()) customPlaceName else {
            parsedResult?.placeName ?: parsedResult?.searchQuery ?: "Wat Phra That Doi Suthep"
        }

        val placeData = "Name: $parsedPlace, PlaceId: ${parsedResult?.placeId ?: "N/A"}, Address: ${parsedResult?.address ?: "N/A"}, Lat/Lng: ${parsedResult?.latitude}/${parsedResult?.longitude}"

        // 2. Loaded Prompt
        val promptPath = "prompts/F_GOOGLE_MAPS_LOCATION_CONTEXT.md"
        val promptContent = try {
            context.assets.open(promptPath).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
        val promptSha256 = if (promptContent.isNotBlank()) sha256(promptContent) else "FILE_NOT_FOUND"

        // 3. Service & ContextEngine Call
        val locationInput = LocationContextInput(
            placeName = parsedPlace,
            latitude = parsedResult?.latitude,
            longitude = parsedResult?.longitude,
            address = parsedResult?.address,
            rawUrl = inputUrl
        )

        val service = GoogleMapsLocationContextService()
        var serviceCallStatus = "NOT_EXECUTED"
        var contextEngineStatus = "NOT_EXECUTED"
        var results = emptyList<ContextResult>()
        var formattedContext = ""

        try {
            results = service.fetchLocationContextResults(locationInput)
            serviceCallStatus = "SUCCESS (Called GoogleMapsLocationContextService)"
            contextEngineStatus = "SUCCESS (${results.size} ContextSource(s) processed)"
            formattedContext = service.fetchLocationContext(locationInput)
        } catch (e: Exception) {
            serviceCallStatus = "ERROR (${e.localizedMessage})"
            contextEngineStatus = "ERROR (${e.localizedMessage})"
        }

        // 4. Sources Results
        val wikiResult = results.firstOrNull { it.sourceName == "WIKIPEDIA" || it.sourceType == ContextSourceType.ENCYCLOPEDIA }
        val wikiTitle = wikiResult?.metadata?.get("title")?.toString() ?: if (wikiResult?.isSuccessful == true) parsedPlace else "N/A"
        val wikiStatus = when {
            wikiResult == null -> "NOT_RUN"
            wikiResult.isSuccessful -> "SUCCESS"
            else -> "NO_CONTEXT_FOUND (${wikiResult.errorMessage ?: "not found"})"
        }
        val wikiSnippet = wikiResult?.snippet ?: ""
        val wikiCharCount = wikiSnippet.length

        val voyageResult = results.firstOrNull { it.sourceName == "WIKIVOYAGE" || it.sourceType == ContextSourceType.TRAVEL_GUIDE }
        val voyageTitle = voyageResult?.metadata?.get("title")?.toString() ?: if (voyageResult?.isSuccessful == true) parsedPlace else "N/A"
        val voyageStatus = when {
            voyageResult == null -> "NOT_RUN"
            voyageResult.isSuccessful -> "SUCCESS"
            else -> "NO_CONTEXT_FOUND (${voyageResult.errorMessage ?: "not found"})"
        }
        val voyageSnippet = voyageResult?.snippet ?: ""
        val voyageCharCount = voyageSnippet.length

        val googleResult = results.firstOrNull { it.sourceName == "GOOGLE_MAPS" || it.sourceType == ContextSourceType.OFFICIAL_DATA }
        val googleStatus = when {
            googleResult == null -> "NOT_RUN"
            googleResult.isSuccessful -> "SUCCESS"
            else -> "NO_CONTEXT_FOUND (${googleResult.errorMessage ?: "not found"})"
        }
        val googleSnippet = googleResult?.snippet ?: ""
        val googleCharCount = googleSnippet.length

        // 5. Gemini Context Injection
        val hasFacts = formattedContext.contains("=== FAKTEN ===")
        val hasTravelContext = formattedContext.contains("=== REISEKONTEXT ===")
        val hasGoogleMapsBase = formattedContext.contains("=== GOOGLE MAPS BASISDATEN ===")
        val injectionPreview = if (formattedContext.length > 500) {
            formattedContext.take(500) + "\n... [gekürzt]"
        } else {
            formattedContext
        }

        // 6. Contract Status
        val finalContractStatus = if (
            serviceCallStatus.startsWith("SUCCESS") &&
            formattedContext.isNotBlank() &&
            promptContent.isNotBlank()
        ) {
            "PASS (VALID_CONTRACT)"
        } else {
            "FAIL (MISSING_DATA_OR_PROMPT)"
        }

        return LocationContextDiagnosticReport(
            inputUrl = inputUrl,
            placeName = parsedPlace,
            placeData = placeData,
            analysisType = analysisTypeName,
            loadedPromptPath = promptPath,
            loadedPromptSha256 = promptSha256,
            serviceCallStatus = serviceCallStatus,
            contextEngineStatus = contextEngineStatus,
            wikipediaResultTitle = wikiTitle,
            wikipediaResultStatus = wikiStatus,
            wikipediaResultSnippet = wikiSnippet,
            wikipediaResultCharCount = wikiCharCount,
            wikivoyageResultTitle = voyageTitle,
            wikivoyageResultStatus = voyageStatus,
            wikivoyageResultSnippet = voyageSnippet,
            wikivoyageResultCharCount = voyageCharCount,
            googleMapsResultStatus = googleStatus,
            googleMapsResultSnippet = googleSnippet,
            googleMapsResultCharCount = googleCharCount,
            geminiContextInjectionLength = formattedContext.length,
            geminiContextInjectionHasFacts = hasFacts,
            geminiContextInjectionHasTravelContext = hasTravelContext,
            geminiContextInjectionHasGoogleMapsBase = hasGoogleMapsBase,
            geminiContextInjectionPreview = injectionPreview,
            finalContractStatus = finalContractStatus
        )
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
