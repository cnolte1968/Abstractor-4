package com.example.data

import android.util.Log
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.net.URLDecoder

data class GoogleMapsPoCResult(
    val originalSharedText: String,
    val extractedUrl: String?,
    val resolvedUrl: String?,
    val detectedUrlType: String?, // "SHORT_LINK", "LONG_MAPS", "SEARCH_MAPS", "UNKNOWN"
    val placeId: String?,
    val cid: String?,
    val featureId: String? = null,
    val placeName: String?,
    val address: String? = null,
    val searchQuery: String?,
    val latitude: Double?,
    val longitude: Double?,
    val zoom: Double?,
    val resolutionStatus: String, // "SUCCESS", "NOT_A_MAPS_URL", "UNSAFE_HOST", "REDIRECT_TO_NON_MAPS_HOST", "REDIRECT_TO_UNSAFE_HOST", "REDIRECT_LIMIT_EXCEEDED", "REDIRECT_LOOP_DETECTED", "REDIRECT_FAILED"
    val plusCode: String? = null,
    val warnings: List<String>
)

object GoogleMapsUrlParser {
    private const val TAG = "GoogleMapsUrlParser"

    fun isGoogleMapsUrl(url: String): Boolean {
        val cleanUrl = url.trim()
        val uri = try { URI(cleanUrl) } catch (e: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "https") {
            return false
        }
        val host = uri.host?.lowercase() ?: ""
        
        if (host == "maps.app.goo.gl") {
            return true
        }
        
        // Exact regex to match maps.google.<ccTLD> (e.g. maps.google.com, maps.google.de, maps.google.co.uk)
        // Avoid matches like maps.google.com.evil.example or maps.google.evil.com
        val mapsGoogleRegex = Regex("^maps\\.google\\.[a-z]{2,}(?:\\.[a-z]{2})?$")
        if (mapsGoogleRegex.matches(host)) {
            return true
        }
        
        // Match google.<ccTLD> or www.google.<ccTLD>
        val googleRegex = Regex("^(www\\.)?google\\.[a-z]{2,}(?:\\.[a-z]{2})?$")
        if (googleRegex.matches(host)) {
            val path = uri.path ?: ""
            if (path.startsWith("/maps")) {
                return true
            }
        }
        
        return false
    }

    fun isSafeHost(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h.isEmpty() || h == "localhost") return false
        
        // Block IPv4 loopback / private / link-local addresses
        // Loopback: 127.0.0.0/8
        // Private: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        // Link-local: 169.254.0.0/16
        val privateIpRegex = Regex(
            "^(?:127\\.\\d+\\.\\d+\\.\\d+|" +
            "10\\.\\d+\\.\\d+\\.\\d+|" +
            "192\\.168\\.\\d+\\.\\d+|" +
            "172\\.(?:1[6-9]|2\\d|3[01])\\.\\d+\\.\\d+|" +
            "169\\.254\\.\\d+\\.\\d+)$"
        )
        if (privateIpRegex.matches(h)) return false
        
        // Block IPv6 loopback / private / link-local / multicast
        if (h.contains(":")) {
            if (h == "::1") return false
            val ip6Prefixes = listOf("fc", "fd", "fe8", "fe9", "fea", "feb", "ff")
            for (prefix in ip6Prefixes) {
                if (h.startsWith(prefix)) {
                    return false
                }
            }
        }
        
        return true
    }

    fun detectUrlType(url: String): String {
        val cleanUrl = url.trim()
        val uri = try { URI(cleanUrl) } catch (e: Exception) { return "UNKNOWN" }
        val host = uri.host?.lowercase() ?: ""
        if (host == "maps.app.goo.gl") {
            return "SHORT_LINK"
        }
        val path = uri.path ?: ""
        if (path.contains("/maps/place")) {
            return "LONG_MAPS"
        }
        if (uri.query?.contains("q=") == true || uri.query?.contains("query=") == true) {
            return "SEARCH_MAPS"
        }
        return "UNKNOWN"
    }

    fun resolveShortUrl(url: String, maxRedirects: Int = 5): Pair<String, String> {
        val visited = mutableSetOf<String>()
        var currentUrl = url.trim()
        var redirectsCount = 0
        val statusCodesList = mutableListOf<String>()
        val locationHeadersList = mutableListOf<String>()

        // Initial safety check
        if (!isGoogleMapsUrl(currentUrl)) {
            return Pair(currentUrl, "NOT_A_MAPS_URL")
        }
        val initialUri = try { URI(currentUrl) } catch (e: Exception) { return Pair(currentUrl, "INVALID_URL") }
        val initialHost = initialUri.host?.lowercase() ?: ""
        if (!isSafeHost(initialHost)) {
            return Pair(currentUrl, "UNSAFE_HOST")
        }

        PipelineReportStore.updateSection("google_maps_analysis") { map ->
            map["shortLinkOriginalUrl"] = url
            map["shortLinkUrlType"] = if (initialHost == "maps.app.goo.gl") "SHORTLINK" else "STANDARD"
        }

        while (redirectsCount < maxRedirects) {
            visited.add(currentUrl)
            try {
                val host = try { URI(currentUrl).host?.lowercase() ?: "" } catch (e: Exception) { "" }
                if (host != "maps.app.goo.gl" && redirectsCount > 0) {
                    break
                }

                val request = Request.Builder()
                    .url(currentUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()

                val singleStepClient = WebpageExtractor.client.newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .readTimeout(java.time.Duration.ofSeconds(5))
                    .writeTimeout(java.time.Duration.ofSeconds(5))
                    .build()

                singleStepClient.newCall(request).execute().use { response ->
                    statusCodesList.add(response.code.toString())
                    val location = response.header("Location")
                    locationHeadersList.add(location ?: "null")
                    
                    if (response.isRedirect) {
                        if (!location.isNullOrBlank()) {
                            val nextUrl = try {
                                response.request.url.resolve(location)?.toString() ?: location
                            } catch (e: Exception) {
                                location
                            }
                            
                            // Safety checks on redirect target
                            if (!isGoogleMapsUrl(nextUrl)) {
                                PipelineReportStore.updateSection("google_maps_analysis") { map ->
                                    map["shortLinkRedirectAttempts"] = redirectsCount
                                    map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                                    map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                                    map["shortLinkFinalUrl"] = currentUrl
                                }
                                return Pair(currentUrl, "REDIRECT_TO_NON_MAPS_HOST")
                            }
                            val nextUri = try { URI(nextUrl) } catch (e: Exception) { null }
                            val nextHost = nextUri?.host?.lowercase() ?: ""
                            if (!isSafeHost(nextHost)) {
                                PipelineReportStore.updateSection("google_maps_analysis") { map ->
                                    map["shortLinkRedirectAttempts"] = redirectsCount
                                    map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                                    map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                                    map["shortLinkFinalUrl"] = currentUrl
                                }
                                return Pair(currentUrl, "REDIRECT_TO_UNSAFE_HOST")
                            }

                            if (visited.contains(nextUrl)) {
                                PipelineReportStore.updateSection("google_maps_analysis") { map ->
                                    map["shortLinkRedirectAttempts"] = redirectsCount
                                    map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                                    map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                                    map["shortLinkFinalUrl"] = currentUrl
                                }
                                return Pair(currentUrl, "REDIRECT_LOOP_DETECTED")
                            }
                            currentUrl = nextUrl
                            redirectsCount++
                        } else {
                            PipelineReportStore.updateSection("google_maps_analysis") { map ->
                                map["shortLinkRedirectAttempts"] = redirectsCount
                                map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                                map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                                map["shortLinkFinalUrl"] = currentUrl
                            }
                            return Pair(currentUrl, "SUCCESS")
                        }
                    } else {
                        PipelineReportStore.updateSection("google_maps_analysis") { map ->
                            map["shortLinkRedirectAttempts"] = redirectsCount
                            map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                            map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                            map["shortLinkFinalUrl"] = response.request.url.toString()
                        }
                        return Pair(response.request.url.toString(), "SUCCESS")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving short link: ${e.message}", e)
                PipelineReportStore.updateSection("google_maps_analysis") { map ->
                    map["shortLinkRedirectAttempts"] = redirectsCount
                    map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
                    map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
                    map["shortLinkFinalUrl"] = currentUrl
                }
                return Pair(currentUrl, "REDIRECT_FAILED: ${e.message}")
            }
        }
        PipelineReportStore.updateSection("google_maps_analysis") { map ->
            map["shortLinkRedirectAttempts"] = redirectsCount
            map["shortLinkHttpStatusPerRedirect"] = statusCodesList.joinToString(", ")
            map["shortLinkLocationHeader"] = locationHeadersList.joinToString(", ")
            map["shortLinkFinalUrl"] = currentUrl
        }
        if (redirectsCount >= maxRedirects) {
            return Pair(currentUrl, "TOO_MANY_REDIRECTS")
        }
        return Pair(currentUrl, "SUCCESS")
    }

    fun parseGoogleMapsUrl(originalText: String, url: String, resolvedUrl: String, resolutionStatus: String): GoogleMapsPoCResult {
        val warnings = mutableListOf<String>()
        val finalUrlToParse = resolvedUrl

        // Validate final URL host
        val finalUri = try { URI(finalUrlToParse) } catch (e: Exception) { null }
        val finalHost = finalUri?.host?.lowercase() ?: ""
        if (!isGoogleMapsUrl(finalUrlToParse) && resolutionStatus == "SUCCESS") {
            warnings.add("Finaler Host ($finalHost) ist keine Google-Maps-Domain")
        }

        // 1. Place-ID
        var placeId: String? = null
        val chijRegex = Regex("(ChIJ[a-zA-Z0-9_-]{23,})")
        val chijMatch = chijRegex.find(finalUrlToParse)
        if (chijMatch != null) {
            placeId = chijMatch.groupValues[1]
        }

        // Also check query parameters
        val queryParams = parseQueryParams(finalUrlToParse)
        val queryPlaceId = queryParams["query_place_id"] ?: queryParams["place_id"] ?: queryParams["placeid"]
        if (queryPlaceId != null) {
            if (placeId == null) {
                placeId = queryPlaceId
            } else if (placeId != queryPlaceId) {
                warnings.add("Divergierende Place-IDs gefunden: $placeId vs $queryPlaceId")
            }
        }

        // 2. CID (Google Customer ID)
        var cid: String? = null
        var featureId: String? = null
        val cidParam = queryParams["cid"]
        if (cidParam != null) {
            cid = cidParam
        } else {
            val hexCidRegex = Regex("0x([0-9a-fA-F]+):0x([0-9a-fA-F]+)")
            val hexCidMatch = hexCidRegex.find(finalUrlToParse)
            if (hexCidMatch != null) {
                featureId = hexCidMatch.groupValues[1]
                val hexCid = hexCidMatch.groupValues[2]
                try {
                    cid = java.lang.Long.toUnsignedString(java.lang.Long.parseUnsignedLong(hexCid, 16))
                } catch (e: Exception) {
                    warnings.add("Fehler beim Dekodieren der Hex-CID: $hexCid")
                }
            }
        }

        // 3. Ortsname (Place name)
        var placeName: String? = null
        var address: String? = null
        val placePathRegex = Regex("/maps/place/([^/@?#]+)")
        val placePathMatch = placePathRegex.find(finalUrlToParse)
        if (placePathMatch != null) {
            val rawName = placePathMatch.groupValues[1]
            val decodedName = try {
                URLDecoder.decode(rawName, "UTF-8")
            } catch (e: Exception) {
                rawName.replace("+", " ")
            }
            val cleanName = decodedName.replace("\"", "")
            val nameParts = cleanName.split("*", limit = 2)
            placeName = nameParts[0].trim()
            if (nameParts.size > 1) {
                address = nameParts[1].trim()
            }
        }

        // 4. Suchbegriff (Search query)
        var searchQuery: String? = null
        var plusCode: String? = null
        val qParam = queryParams["q"] ?: queryParams["query"]
        if (qParam != null) {
            val coordsRegex = Regex("^-?\\d+\\.\\d+,-?\\d+\\.\\d+$")
            if (!coordsRegex.matches(qParam)) {
                searchQuery = qParam
                
                // Extract Plus Code if present (e.g. QXV3+893 or 8FW4V888+88)
                val plusCodeRegex = Regex("([A-Z0-9]{4,8}\\+[A-Z0-9]{2,3})")
                val plusCodeMatch = plusCodeRegex.find(qParam)
                if (plusCodeMatch != null) {
                    plusCode = plusCodeMatch.groupValues[1]
                }
            }
        }

        // 4b. Extract Plus Code from place path or url if not found in query
        if (plusCode == null) {
            val plusCodeRegex = Regex("([A-Z0-9]{4,8}\\+[A-Z0-9]{2,3})")
            val plusCodeMatch = (placeName?.let { plusCodeRegex.find(it) })
                ?: (address?.let { plusCodeRegex.find(it) })
                ?: plusCodeRegex.find(finalUrlToParse)
            if (plusCodeMatch != null) {
                plusCode = plusCodeMatch.groupValues[1]
            }
        }

        // 5. Koordinaten (Latitude, Longitude)
        var latitude: Double? = null
        var longitude: Double? = null
        
        // 5a. Exakte Marker-Koordinaten (!3d lat !4d lng)
        val exactCoordsRegex = Regex("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)")
        val exactMatch = exactCoordsRegex.find(finalUrlToParse)
        
        if (exactMatch != null) {
            latitude = exactMatch.groupValues[1].toDoubleOrNull()
            longitude = exactMatch.groupValues[2].toDoubleOrNull()
        }
        
        // 5b. Fallback: Viewport-Koordinaten (@lat,lng)
        if (latitude == null || longitude == null) {
            val atCoordsRegex = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
            val atCoordsMatch = atCoordsRegex.find(finalUrlToParse)
            if (atCoordsMatch != null) {
                latitude = atCoordsMatch.groupValues[1].toDoubleOrNull()
                longitude = atCoordsMatch.groupValues[2].toDoubleOrNull()
            }
        }

        if (latitude == null || longitude == null) {
            val llParam = queryParams["ll"] ?: queryParams["q"] ?: queryParams["query"]
            if (llParam != null) {
                val split = llParam.split(",")
                if (split.size >= 2) {
                    val latVal = split[0].trim().toDoubleOrNull()
                    val lonVal = split[1].trim().toDoubleOrNull()
                    if (latVal != null && lonVal != null) {
                        latitude = latVal
                        longitude = lonVal
                    }
                }
            }
        }

        // 5c. Fallback: Plus Code coordinates
        if ((latitude == null || longitude == null) && plusCode != null) {
            try {
                val olc = com.google.openlocationcode.OpenLocationCode(plusCode)
                if (olc.isFull) {
                    val decoded = olc.decode()
                    latitude = decoded.centerLatitude
                    longitude = decoded.centerLongitude
                }
            } catch (e: Exception) {
                // Ignore decoding errors
            }
        }

        // 6. Zoomstufe (Zoom level)
        var zoom: Double? = null
        val zoomRegex = Regex("@-?\\d+\\.\\d+,-?\\d+\\.\\d+,(\\d+(?:\\.\\d+)?)z")
        val zoomMatch = zoomRegex.find(finalUrlToParse)
        if (zoomMatch != null) {
            zoom = zoomMatch.groupValues[1].toDoubleOrNull()
        } else {
            val zParam = queryParams["z"]
            if (zParam != null) {
                zoom = zParam.toDoubleOrNull()
            }
        }

        if (placeId == null && cid == null && placeName == null && searchQuery == null && latitude == null && longitude == null) {
            warnings.add("Keine Identifikationsmerkmale extrahierbar")
        }

        PipelineReportStore.updateSection("google_maps_analysis") { map ->
            map["extractedLocationTitle"] = placeName ?: ""
            map["extractedAddress"] = address ?: ""
            map["extractedPlusCode"] = plusCode ?: ""
            map["extractedPlaceId"] = placeId ?: ""
            map["extractedCid"] = cid ?: ""
            map["extractedLatitude"] = latitude?.toString() ?: ""
            map["extractedLongitude"] = longitude?.toString() ?: ""

            map["parserPlaceId"] = placeId ?: ""
            map["parserLat"] = latitude?.toString() ?: ""
            map["parserLng"] = longitude?.toString() ?: ""
            map["parserPlaceName"] = placeName ?: ""
            map["parserAddress"] = address ?: ""
            map["parserPlusCode"] = plusCode ?: ""
            map["parserSearchQuery"] = searchQuery ?: ""
        }

        return GoogleMapsPoCResult(
            originalSharedText = originalText,
            extractedUrl = url,
            resolvedUrl = finalUrlToParse,
            detectedUrlType = detectUrlType(finalUrlToParse),
            placeId = placeId,
            cid = cid,
            featureId = featureId,
            placeName = placeName,
            address = address,
            searchQuery = searchQuery,
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
            resolutionStatus = if (warnings.contains("Keine Identifikationsmerkmale extrahierbar") && resolutionStatus == "SUCCESS") "EXTRACTION_FAILED" else resolutionStatus,
            plusCode = plusCode,
            warnings = warnings
        )
    }

    private fun parseQueryParams(url: String): Map<String, String> {
        val queryMap = mutableMapOf<String, String>()
        try {
            val uri = URI(url)
            val query = uri.rawQuery ?: return emptyMap()
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx > 0) {
                    val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                    val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    queryMap[key] = value
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return queryMap
    }
}
