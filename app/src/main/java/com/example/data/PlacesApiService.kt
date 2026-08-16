package com.example.data

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class TextSearchRequest(
    val textQuery: String,
    val locationBias: LocationBias? = null
)

@JsonClass(generateAdapter = true)
data class LocationBias(
    val circle: CircleBias
)

@JsonClass(generateAdapter = true)
data class CircleBias(
    val center: LatLng,
    val radius: Double
)

@JsonClass(generateAdapter = true)
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
data class TextSearchResponse(
    val places: List<PlaceIdName>? = null
)

@JsonClass(generateAdapter = true)
data class PlaceIdName(
    val id: String,
    val name: String,
    val formattedAddress: String? = null,
    val location: LatLng? = null,
    val googleMapsUri: String? = null,
    val displayName: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class AddressComponent(
    val longText: String?,
    val shortText: String?,
    val types: List<String>?,
    val languageCode: String?
)

@JsonClass(generateAdapter = true)
data class Viewport(
    val low: LatLng?,
    val high: LatLng?
)

@JsonClass(generateAdapter = true)
data class PlusCode(
    val globalCode: String?,
    val compoundCode: String?
)

@JsonClass(generateAdapter = true)
data class LocalizedText(
    val text: String? = null,
    val languageCode: String? = null
)

@JsonClass(generateAdapter = true)
data class OpeningHours(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String>? = null,
    val nextOpenTime: String? = null,
    val nextCloseTime: String? = null
)

@JsonClass(generateAdapter = true)
data class Review(
    val name: String? = null,
    val relativePublishTimeDescription: String? = null,
    val rating: Double? = null,
    val text: LocalizedText? = null,
    val originalText: LocalizedText? = null,
    val authorAttribution: AuthorAttribution? = null,
    val publishTime: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthorAttribution(
    val displayName: String? = null,
    val uri: String? = null,
    val photoUri: String? = null
)

@JsonClass(generateAdapter = true)
data class PlaceDetailsResponse(
    val id: String?,
    val name: String?,
    val formattedAddress: String?,
    val shortFormattedAddress: String?,
    val addressComponents: List<AddressComponent>?,
    val location: LatLng?,
    val types: List<String>?,
    val viewport: Viewport?,
    val plusCode: PlusCode?,
    val displayName: LocalizedText? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val editorialSummary: LocalizedText? = null,
    val priceLevel: String? = null,
    val websiteUri: String? = null,
    val regularOpeningHours: OpeningHours? = null,
    val reviews: List<Review>? = null
)

data class GooglePlacesPoCResult(
    val originalSharedUrl: String,
    val resolvedUrl: String,
    val placeId: String?,
    val placeResourceName: String?,
    val urlDerivedName: String?,
    val urlDerivedAddress: String?,
    val formattedAddress: String?,
    val shortFormattedAddress: String?,
    val addressComponents: List<AddressComponent>?,
    val latitude: Double?,
    val longitude: Double?,
    val types: List<String>?,
    val viewport: Viewport?,
    val plusCode: PlusCode?,
    val placeIdResolutionMethod: String, // DIRECT_PLACE_ID, TEXT_SEARCH_EXACT_MATCH, TEXT_SEARCH_BEST_MATCH, TEXT_SEARCH_AMBIGUOUS, TEXT_SEARCH_NO_MATCH, NONE
    val placeMatchStatus: String, // EXACT, BEST, AMBIGUOUS, NONE
    val apiStatus: String, // PLACE_DETAILS_SUCCESS, PLACE_DETAILS_FAILED, API_KEY_MISSING, NETWORK_ERROR, HTTP_ERROR
    val warnings: List<String>,
    val displayName: LocalizedText? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val editorialSummary: LocalizedText? = null,
    val priceLevel: String? = null,
    val websiteUri: String? = null,
    val regularOpeningHours: OpeningHours? = null,
    val reviews: List<Review>? = null
)

object PlacesApiService {
    private const val TAG = "PlacesApiService"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val textSearchRequestAdapter = moshi.adapter(TextSearchRequest::class.java)
    private val textSearchResponseAdapter = moshi.adapter(TextSearchResponse::class.java)
    private val placeDetailsResponseAdapter = moshi.adapter(PlaceDetailsResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String? {
        var key = try {
            com.example.BuildConfig.PLACES_API_KEY
        } catch (e: Throwable) {
            null
        }
        if (key.isNullOrBlank() || key == "MY_PLACES_API_KEY") {
            key = try {
                com.example.BuildConfig.Gemini_Relevantor
            } catch (e: Throwable) {
                null
            }
        }
        if (key.isNullOrBlank() || key == "MY_GEMINI_KEY") {
            key = try {
                com.example.BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                null
            }
        }
        if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
            return null
        }
        return key
    }

    fun buildQueryText(mapsResult: GoogleMapsPoCResult): String? {
        val queryParts = listOfNotNull(mapsResult.placeName, mapsResult.address).filter { it.isNotBlank() }
        val combinedName = if (queryParts.isNotEmpty()) queryParts.joinToString(" ") else null
        return combinedName ?: mapsResult.searchQuery
    }

    /**
     * Resolves Google Maps link and queries Places API (New) End-To-End.
     */
    fun performStufe1Analysis(
        originalText: String,
        mapsResult: GoogleMapsPoCResult
    ): GooglePlacesPoCResult {
        val warnings = mutableListOf<String>()
        warnings.addAll(mapsResult.warnings)

        val originalSharedUrl = mapsResult.originalSharedText
        val resolvedUrl = mapsResult.resolvedUrl ?: mapsResult.originalSharedText

        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return GooglePlacesPoCResult(
                originalSharedUrl = originalSharedUrl,
                resolvedUrl = resolvedUrl,
                placeId = mapsResult.placeId,
                placeResourceName = null,
                urlDerivedName = mapsResult.placeName,
                urlDerivedAddress = null,
                formattedAddress = null,
                shortFormattedAddress = null,
                addressComponents = null,
                latitude = mapsResult.latitude,
                longitude = mapsResult.longitude,
                types = null,
                viewport = null,
                plusCode = null,
                placeIdResolutionMethod = if (mapsResult.placeId != null) "DIRECT_PLACE_ID" else "NONE",
                placeMatchStatus = if (mapsResult.placeId != null) "EXACT" else "NONE",
                apiStatus = "API_KEY_MISSING",
                warnings = warnings + "Google Places API-Key fehlt oder ist nicht konfiguriert."
            )
        }

        var currentPlaceId = mapsResult.placeId
        var resolutionMethod = "NONE"
        var matchStatus = "NONE"

        // Step 1: Place-ID determination
        if (currentPlaceId != null) {
            resolutionMethod = "DIRECT_PLACE_ID"
            matchStatus = "EXACT"
            Log.i(TAG, "Direkte Place-ID aus URL gefunden: $currentPlaceId")
            com.example.data.PipelineReportStore.updateSection("google_maps_analysis") { map ->
                map["placesApiMode"] = "PLACE_DETAILS"
                map["placesApiPlaceId"] = currentPlaceId
            }
        } else {
            // No Place-ID in URL, need Text Search (New)
            val queryText = buildQueryText(mapsResult)
            
            if (queryText.isNullOrBlank()) {
                return GooglePlacesPoCResult(
                    originalSharedUrl = originalSharedUrl,
                    resolvedUrl = resolvedUrl,
                    placeId = null,
                    placeResourceName = null,
                    urlDerivedName = mapsResult.placeName,
                    urlDerivedAddress = null,
                    formattedAddress = null,
                    shortFormattedAddress = null,
                    addressComponents = null,
                    latitude = mapsResult.latitude,
                    longitude = mapsResult.longitude,
                    types = null,
                    viewport = null,
                    plusCode = null,
                    placeIdResolutionMethod = "TEXT_SEARCH_NO_MATCH",
                    placeMatchStatus = "NONE",
                    apiStatus = "PLACE_DETAILS_FAILED",
                    warnings = warnings + "Weder Ortname noch Suchbegriff zur Place-ID-Suche vorhanden."
                )
            }

            Log.i(TAG, "Starte Text-Search-Auflösung für Query: '$queryText'")
            val locationBiasVar = if (mapsResult.latitude != null && mapsResult.longitude != null) {
                LocationBias(
                    circle = CircleBias(
                        center = LatLng(mapsResult.latitude, mapsResult.longitude),
                        radius = 1000.0
                    )
                )
            } else null

            com.example.data.PipelineReportStore.updateSection("google_maps_analysis") { map ->
                map["placesApiMode"] = "TEXT_SEARCH"
                map["placesApiQuery"] = queryText
                map["placesApiLocationBias"] = if (locationBiasVar != null) "${locationBiasVar.circle?.center?.latitude}, ${locationBiasVar.circle?.center?.longitude}" else "null"
            }

            val searchRequest = TextSearchRequest(
                textQuery = queryText,
                locationBias = locationBiasVar
            )

            try {
                val searchResult = executeTextSearch(searchRequest, apiKey)
                if (searchResult.isSuccessful) {
                    val places = searchResult.body?.places
                    if (places.isNullOrEmpty()) {
                        resolutionMethod = "TEXT_SEARCH_NO_MATCH"
                        matchStatus = "NONE"
                        warnings.add("Text Search ergab keine Treffer für '$queryText'")
                    } else if (places.size == 1) {
                        currentPlaceId = places[0].id
                        resolutionMethod = "TEXT_SEARCH_EXACT_MATCH"
                        matchStatus = "EXACT"
                        Log.i(TAG, "Eindeutiger Treffer gefunden: $currentPlaceId")
                    } else {
                        var finalLat = mapsResult.latitude
                        var finalLng = mapsResult.longitude
                        
                        if (mapsResult.plusCode != null) {
                            try {
                                val olc = com.google.openlocationcode.OpenLocationCode(mapsResult.plusCode)
                                if (olc.isFull) {
                                    val decoded = olc.decode()
                                    finalLat = decoded.centerLatitude
                                    finalLng = decoded.centerLongitude
                                } else if (olc.isShort && places.isNotEmpty()) {
                                    val refLat = places[0].location?.latitude
                                    val refLng = places[0].location?.longitude
                                    if (refLat != null && refLng != null) {
                                        val recovered = olc.recover(refLat, refLng)
                                        val decodedRecovered = recovered.decode()
                                        finalLat = decodedRecovered.centerLatitude
                                        finalLng = decodedRecovered.centerLongitude
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error decoding Plus Code: ${mapsResult.plusCode}", e)
                            }
                        }

                        val urlInfo = GoogleMapsDisambiguator.UrlInfo(
                            placeName = mapsResult.placeName,
                            address = mapsResult.address,
                            lat = finalLat,
                            lng = finalLng,
                            placeId = mapsResult.placeId,
                            cid = mapsResult.cid
                        )
                        val candidates = places.map { place ->
                            val candidateCid = place.googleMapsUri?.let { uri ->
                                Regex("[?&]cid=(\\d+)").find(uri)?.groupValues?.get(1)
                            }
                            GoogleMapsDisambiguator.Candidate(
                                id = place.id,
                                name = place.displayName?.text ?: place.name,
                                address = place.formattedAddress,
                                lat = place.location?.latitude,
                                lng = place.location?.longitude,
                                cid = candidateCid
                            )
                        }

                        com.example.data.PipelineReportStore.updateSection("google_maps_analysis") { map ->
                            map["disambiguatorCandidateCount"] = candidates.size
                            map["disambiguatorCandidateNames"] = candidates.joinToString(", ") { it.name ?: "Unknown" }
                            map["disambiguatorCandidatePlaceIds"] = candidates.joinToString(", ") { it.id }
                            map["disambiguatorCandidateCoordinates"] = candidates.joinToString(", ") { "${it.lat ?: "null"},${it.lng ?: "null"}" }
                            map["disambiguatorScoringInput"] = "urlInfo: lat=${urlInfo.lat}, lng=${urlInfo.lng}, name=${urlInfo.placeName}"
                        }

                        val bestMatch = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
                        
                        if (bestMatch != null) {
                            currentPlaceId = bestMatch.id
                            resolutionMethod = "TEXT_SEARCH_DISAMBIGUATED"
                            matchStatus = "EXACT"
                            Log.i(TAG, "Treffer durch Disambiguator ausgewählt: $currentPlaceId")
                        } else {
                            // Ambiguous results
                            resolutionMethod = "TEXT_SEARCH_AMBIGUOUS"
                            matchStatus = "AMBIGUOUS"
                            warnings.add("Mehrdeutige Treffer (${places.size}) für '$queryText'. Disambiguierung nicht eindeutig. Suche abgebrochen.")
                        }
                    }
                } else {
                    resolutionMethod = "TEXT_SEARCH_NO_MATCH"
                    matchStatus = "NONE"
                    warnings.add("Text Search fehlgeschlagen: HTTP ${searchResult.code} - ${searchResult.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler während der Text-Search-Abfrage", e)
                return GooglePlacesPoCResult(
                    originalSharedUrl = originalSharedUrl,
                    resolvedUrl = resolvedUrl,
                    placeId = null,
                    placeResourceName = null,
                    urlDerivedName = mapsResult.placeName,
                    urlDerivedAddress = null,
                    formattedAddress = null,
                    shortFormattedAddress = null,
                    addressComponents = null,
                    latitude = mapsResult.latitude,
                    longitude = mapsResult.longitude,
                    types = null,
                    viewport = null,
                    plusCode = null,
                    placeIdResolutionMethod = "TEXT_SEARCH_NO_MATCH",
                    placeMatchStatus = "NONE",
                    apiStatus = "NETWORK_ERROR",
                    warnings = warnings + "Netzwerkfehler während Text Search: ${e.message}"
                )
            }
        }

        // If we still don't have a placeId, we must abort
        val finalPlaceId = currentPlaceId
        if (finalPlaceId == null) {
            return GooglePlacesPoCResult(
                originalSharedUrl = originalSharedUrl,
                resolvedUrl = resolvedUrl,
                placeId = null,
                placeResourceName = null,
                urlDerivedName = mapsResult.placeName,
                urlDerivedAddress = null,
                formattedAddress = null,
                shortFormattedAddress = null,
                addressComponents = null,
                latitude = mapsResult.latitude,
                longitude = mapsResult.longitude,
                types = null,
                viewport = null,
                plusCode = null,
                placeIdResolutionMethod = resolutionMethod,
                placeMatchStatus = matchStatus,
                apiStatus = "PLACE_DETAILS_FAILED",
                warnings = warnings + "Keine Place-ID ermittelt."
            )
        }

        // Step 2: Fetch Place Details Essentials
        Log.i(TAG, "Rufe Place Details Essentials für ID $finalPlaceId ab")
        try {
            val detailsResult = executePlaceDetails(finalPlaceId, apiKey)
            if (detailsResult.isSuccessful && detailsResult.body != null) {
                val body = detailsResult.body
                return GooglePlacesPoCResult(
                    originalSharedUrl = originalSharedUrl,
                    resolvedUrl = resolvedUrl,
                    placeId = finalPlaceId,
                    placeResourceName = body.name,
                    urlDerivedName = mapsResult.placeName,
                    urlDerivedAddress = null,
                    formattedAddress = body.formattedAddress,
                    shortFormattedAddress = body.shortFormattedAddress,
                    addressComponents = body.addressComponents,
                    latitude = body.location?.latitude ?: mapsResult.latitude,
                    longitude = body.location?.longitude ?: mapsResult.longitude,
                    types = body.types,
                    viewport = body.viewport,
                    plusCode = body.plusCode,
                    placeIdResolutionMethod = resolutionMethod,
                    placeMatchStatus = matchStatus,
                    apiStatus = "PLACE_DETAILS_SUCCESS",
                    warnings = warnings,
                    displayName = body.displayName,
                    rating = body.rating,
                    userRatingCount = body.userRatingCount,
                    editorialSummary = body.editorialSummary,
                    priceLevel = body.priceLevel,
                    websiteUri = body.websiteUri,
                    regularOpeningHours = body.regularOpeningHours,
                    reviews = body.reviews
                )
            } else {
                return GooglePlacesPoCResult(
                    originalSharedUrl = originalSharedUrl,
                    resolvedUrl = resolvedUrl,
                    placeId = finalPlaceId,
                    placeResourceName = null,
                    urlDerivedName = mapsResult.placeName,
                    urlDerivedAddress = null,
                    formattedAddress = null,
                    shortFormattedAddress = null,
                    addressComponents = null,
                    latitude = mapsResult.latitude,
                    longitude = mapsResult.longitude,
                    types = null,
                    viewport = null,
                    plusCode = null,
                    placeIdResolutionMethod = resolutionMethod,
                    placeMatchStatus = matchStatus,
                    apiStatus = "HTTP_ERROR",
                    warnings = warnings + "Place Details fehlgeschlagen: HTTP ${detailsResult.code} - ${detailsResult.message}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler während der Place-Details-Abfrage", e)
            return GooglePlacesPoCResult(
                originalSharedUrl = originalSharedUrl,
                resolvedUrl = resolvedUrl,
                placeId = finalPlaceId,
                placeResourceName = null,
                urlDerivedName = mapsResult.placeName,
                urlDerivedAddress = null,
                formattedAddress = null,
                shortFormattedAddress = null,
                addressComponents = null,
                latitude = mapsResult.latitude,
                longitude = mapsResult.longitude,
                types = null,
                viewport = null,
                plusCode = null,
                placeIdResolutionMethod = resolutionMethod,
                placeMatchStatus = matchStatus,
                apiStatus = "NETWORK_ERROR",
                warnings = warnings + "Netzwerkfehler während Place Details: ${e.message}"
            )
        }
    }

    data class ApiResponse<T>(
        val isSuccessful: Boolean,
        val code: Int,
        val message: String,
        val body: T?
    )

    private fun executeTextSearch(
        searchRequest: TextSearchRequest,
        apiKey: String
    ): ApiResponse<TextSearchResponse> {
        val jsonPayload = textSearchRequestAdapter.toJson(searchRequest)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchText")
            .post(requestBody)
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "places.id,places.name,places.formattedAddress,places.location,places.googleMapsUri,places.displayName")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            val message = response.message
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val bodyObj = textSearchResponseAdapter.fromJson(bodyStr)
                return ApiResponse(true, code, message, bodyObj)
            } else {
                return ApiResponse(false, code, message, null)
            }
        }
    }

    private fun executePlaceDetails(
        placeId: String,
        apiKey: String
    ): ApiResponse<PlaceDetailsResponse> {
        val request = Request.Builder()
            .url("https://places.googleapis.com/v1/places/$placeId")
            .get()
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", "id,name,formattedAddress,shortFormattedAddress,addressComponents,location,types,viewport,plusCode,displayName,rating,userRatingCount,editorialSummary,priceLevel,websiteUri,regularOpeningHours,reviews")
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            val message = response.message
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val bodyObj = placeDetailsResponseAdapter.fromJson(bodyStr)
                return ApiResponse(true, code, message, bodyObj)
            } else {
                return ApiResponse(false, code, message, null)
            }
        }
    }
}
