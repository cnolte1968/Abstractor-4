package com.example.domain.engine.location

import com.example.data.GoogleMapsPoCResult
import com.example.data.GoogleMapsUrlParser
import com.example.data.GooglePlacesPoCResult
import com.example.data.PlacesApiService
import com.example.data.Review
import com.example.data.contextengine.ContextResult
import com.example.data.contextengine.ContextSource
import com.example.data.contextengine.GoogleMapsLocationContextService
import com.example.data.contextengine.LocationContextInput
import com.example.data.contextengine.WikipediaContextSource
import com.example.data.contextengine.WikivoyageContextSource

data class LocationQuestionAggregatedContext(
    val userQuestion: String,
    val locationName: String,
    val executionPlan: ExecutionPlan?,
    val placesResult: GooglePlacesPoCResult? = null,
    val locationContextFormatted: String? = null,
    val wikipediaResult: ContextResult? = null,
    val wikivoyageResult: ContextResult? = null,
    val reviews: List<Review> = emptyList(),
    val requiresGrounding: Boolean = false,
    val sourcesStatus: Map<DataSourceType, Boolean> = emptyMap(),
    val formattedCombinedContext: String = ""
)

class LocationQuestionCoordinator(
    private val placesApiService: PlacesApiService = PlacesApiService,
    private val locationContextService: GoogleMapsLocationContextService = GoogleMapsLocationContextService(),
    private val wikipediaContextSource: ContextSource = WikipediaContextSource(),
    private val wikivoyageContextSource: ContextSource = WikivoyageContextSource(),
    private val planner: LocationQuestionPlanner = LocationQuestionPlanner
) {
    suspend fun coordinate(
        rawLocationInput: String,
        userQuestion: String,
        existingPlan: ExecutionPlan? = null,
        preParsedPlacesResult: GooglePlacesPoCResult? = null
    ): LocationQuestionAggregatedContext {
        var placesResult: GooglePlacesPoCResult? = preParsedPlacesResult
        val sourcesStatus = mutableMapOf<DataSourceType, Boolean>()

        // 1. Resolve Location Details & Places API if not pre-parsed
        var resolvedLocationName = preParsedPlacesResult?.displayName?.text
            ?: preParsedPlacesResult?.urlDerivedName
            ?: ""

        var lat: Double? = preParsedPlacesResult?.latitude
        var lng: Double? = preParsedPlacesResult?.longitude
        var address: String? = preParsedPlacesResult?.formattedAddress
        var rawUrl: String? = preParsedPlacesResult?.resolvedUrl ?: preParsedPlacesResult?.originalSharedUrl

        if (placesResult == null && rawLocationInput.isNotBlank()) {
            try {
                if (GoogleMapsUrlParser.isGoogleMapsUrl(rawLocationInput)) {
                    val (resolvedUrl, resStatus) = GoogleMapsUrlParser.resolveShortUrl(rawLocationInput)
                    val mapsPoCResult = GoogleMapsUrlParser.parseGoogleMapsUrl(
                        originalText = rawLocationInput,
                        url = rawLocationInput,
                        resolvedUrl = resolvedUrl,
                        resolutionStatus = resStatus
                    )
                    placesResult = placesApiService.performStufe1Analysis(rawLocationInput, mapsPoCResult)
                    sourcesStatus[DataSourceType.PLACES] = placesResult.apiStatus == "PLACE_DETAILS_SUCCESS"

                    resolvedLocationName = placesResult.displayName?.text
                        ?: placesResult.urlDerivedName
                        ?: mapsPoCResult.placeName
                        ?: ""
                    lat = placesResult.latitude ?: mapsPoCResult.latitude
                    lng = placesResult.longitude ?: mapsPoCResult.longitude
                    address = placesResult.formattedAddress ?: mapsPoCResult.address
                    rawUrl = resolvedUrl
                } else {
                    resolvedLocationName = rawLocationInput.trim()
                }
            } catch (e: Exception) {
                sourcesStatus[DataSourceType.PLACES] = false
                if (resolvedLocationName.isBlank()) {
                    resolvedLocationName = rawLocationInput.trim()
                }
            }
        } else if (placesResult != null) {
            sourcesStatus[DataSourceType.PLACES] = placesResult.apiStatus == "PLACE_DETAILS_SUCCESS"
        }

        // 2. Resolve Execution Plan
        val plan = existingPlan ?: planner.planExecution(userQuestion, resolvedLocationName)
        val activeSources = plan?.priorityOrder?.toSet()
            ?: (plan?.requiredSources.orEmpty() + plan?.optionalSources.orEmpty())

        val shouldFetchPlaces = activeSources.contains(DataSourceType.PLACES) || activeSources.isEmpty()
        val shouldFetchReviews = activeSources.contains(DataSourceType.REVIEWS) || activeSources.isEmpty()
        val shouldFetchLocationContext = activeSources.contains(DataSourceType.LOCATION_CONTEXT) || activeSources.isEmpty()
        val shouldFetchWikipedia = activeSources.contains(DataSourceType.WIKIPEDIA)
        val shouldFetchWikivoyage = activeSources.contains(DataSourceType.WIKIVOYAGE)

        // 3. Extract Reviews
        val reviews = if (shouldFetchReviews && placesResult != null) {
            val list = placesResult.reviews.orEmpty()
            sourcesStatus[DataSourceType.REVIEWS] = list.isNotEmpty()
            list
        } else {
            sourcesStatus[DataSourceType.REVIEWS] = false
            emptyList()
        }

        // 4. Fetch Location Context
        val locationContextInput = LocationContextInput(
            placeName = resolvedLocationName,
            latitude = lat,
            longitude = lng,
            address = address,
            category = placesResult?.types?.firstOrNull(),
            rawUrl = rawUrl
        )

        var locationContextFormatted: String? = null
        if (shouldFetchLocationContext && resolvedLocationName.isNotBlank()) {
            try {
                val formatted = locationContextService.fetchLocationContext(locationContextInput)
                if (formatted.isNotBlank()) {
                    locationContextFormatted = formatted
                    sourcesStatus[DataSourceType.LOCATION_CONTEXT] = true
                } else {
                    sourcesStatus[DataSourceType.LOCATION_CONTEXT] = false
                }
            } catch (e: Exception) {
                sourcesStatus[DataSourceType.LOCATION_CONTEXT] = false
            }
        }

        // 5. Fetch Wikipedia
        var wikipediaResult: ContextResult? = null
        if (shouldFetchWikipedia && resolvedLocationName.isNotBlank()) {
            try {
                val res = wikipediaContextSource.fetchContext(locationContextInput)
                if (res.isSuccessful) {
                    wikipediaResult = res
                    sourcesStatus[DataSourceType.WIKIPEDIA] = true
                } else {
                    sourcesStatus[DataSourceType.WIKIPEDIA] = false
                }
            } catch (e: Exception) {
                sourcesStatus[DataSourceType.WIKIPEDIA] = false
            }
        }

        // 6. Fetch Wikivoyage
        var wikivoyageResult: ContextResult? = null
        if (shouldFetchWikivoyage && resolvedLocationName.isNotBlank()) {
            try {
                val res = wikivoyageContextSource.fetchContext(locationContextInput)
                if (res.isSuccessful) {
                    wikivoyageResult = res
                    sourcesStatus[DataSourceType.WIKIVOYAGE] = true
                } else {
                    sourcesStatus[DataSourceType.WIKIVOYAGE] = false
                }
            } catch (e: Exception) {
                sourcesStatus[DataSourceType.WIKIVOYAGE] = false
            }
        }

        val requiresGrounding = plan?.requiresGrounding ?: false
        if (requiresGrounding) {
            sourcesStatus[DataSourceType.SEARCH_GROUNDING] = true
        }

        // 7. Format Combined Context
        val formattedCombined = buildCombinedContextString(
            userQuestion = userQuestion,
            locationName = resolvedLocationName,
            plan = plan,
            placesResult = placesResult,
            reviews = reviews,
            locationContextFormatted = locationContextFormatted,
            wikipediaResult = wikipediaResult,
            wikivoyageResult = wikivoyageResult,
            requiresGrounding = requiresGrounding
        )

        return LocationQuestionAggregatedContext(
            userQuestion = userQuestion,
            locationName = resolvedLocationName,
            executionPlan = plan,
            placesResult = placesResult,
            locationContextFormatted = locationContextFormatted,
            wikipediaResult = wikipediaResult,
            wikivoyageResult = wikivoyageResult,
            reviews = reviews,
            requiresGrounding = requiresGrounding,
            sourcesStatus = sourcesStatus,
            formattedCombinedContext = formattedCombined
        )
    }

    private fun buildCombinedContextString(
        userQuestion: String,
        locationName: String,
        plan: ExecutionPlan?,
        placesResult: GooglePlacesPoCResult?,
        reviews: List<Review>,
        locationContextFormatted: String?,
        wikipediaResult: ContextResult?,
        wikivoyageResult: ContextResult?,
        requiresGrounding: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("=== NATIVE FRAGE ZUM ORT ===\n")
        sb.append("Ort: $locationName\n")
        sb.append("Frage: $userQuestion\n")
        if (plan != null) {
            sb.append("Kategorie: ${plan.primaryCategory}\n")
            sb.append("Grounding erforderlich: $requiresGrounding\n")
        }
        sb.append("\n")

        if (placesResult != null) {
            sb.append("=== PLACES API DETAILS ===\n")
            if (!placesResult.formattedAddress.isNullOrBlank()) {
                sb.append("Adresse: ${placesResult.formattedAddress}\n")
            }
            if (placesResult.rating != null) {
                sb.append("Bewertung: ${placesResult.rating} (${placesResult.userRatingCount ?: 0} Bewertungen)\n")
            }
            if (!placesResult.priceLevel.isNullOrBlank()) {
                sb.append("Preisniveaustufe: ${placesResult.priceLevel}\n")
            }
            if (placesResult.regularOpeningHours != null) {
                val hours = placesResult.regularOpeningHours.weekdayDescriptions
                if (!hours.isNullOrEmpty()) {
                    sb.append("Öffnungszeiten:\n${hours.joinToString("\n")}\n")
                }
            }
            if (placesResult.editorialSummary?.text?.isNotBlank() == true) {
                sb.append("Zusammenfassung: ${placesResult.editorialSummary.text}\n")
            }
            sb.append("\n")
        }

        if (reviews.isNotEmpty()) {
            sb.append("=== NUTZER-REVIEWS (${reviews.size}) ===\n")
            reviews.take(5).forEachIndexed { idx, rev ->
                val author = rev.authorAttribution?.displayName ?: "Anonym"
                val text = rev.text?.text ?: rev.originalText?.text ?: ""
                val rating = rev.rating?.let { " ($it Stern(e))" } ?: ""
                if (text.isNotBlank()) {
                    sb.append("[Review ${idx + 1}]$rating von $author: $text\n")
                }
            }
            sb.append("\n")
        }

        if (!locationContextFormatted.isNullOrBlank()) {
            sb.append("=== LOCATION CONTEXT ===\n")
            sb.append(locationContextFormatted)
            sb.append("\n\n")
        }

        if (wikipediaResult?.isSuccessful == true && !wikipediaResult.snippet.isNullOrBlank()) {
            sb.append("=== WIKIPEDIA ETRAG ===\n")
            sb.append(wikipediaResult.snippet)
            sb.append("\n\n")
        }

        if (wikivoyageResult?.isSuccessful == true && !wikivoyageResult.snippet.isNullOrBlank()) {
            sb.append("=== WIKIVOYAGE REISEFÜHRER ===\n")
            sb.append(wikivoyageResult.snippet)
            sb.append("\n\n")
        }

        if (requiresGrounding) {
            sb.append("=== SUCHE & GROUNDING NOTICE ===\n")
            sb.append("Diese Frage erfordert aktuelle Online-Suche/Grounding für Echtzeitdaten.\n")
        }

        return sb.toString().trim()
    }
}
