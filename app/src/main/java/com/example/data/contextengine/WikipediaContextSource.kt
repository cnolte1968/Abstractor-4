package com.example.data.contextengine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WikipediaContextSource(
    private val language: String = "de",
    private val networkFetcher: suspend (String) -> String = { url -> defaultNetworkFetcher(url) }
) : ContextSource {

    override val sourceName: String = "WIKIPEDIA"
    override val sourceType: ContextSourceType = ContextSourceType.ENCYCLOPEDIA

    override suspend fun fetchContext(input: LocationContextInput): ContextResult {
        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            try {
                if (input.placeName.isBlank() && input.latitude == null) {
                    return@withContext ContextResult(
                        sourceName = sourceName,
                        sourceType = sourceType,
                        isSuccessful = false,
                        sourceUrl = null,
                        trustScore = 0.95,
                        confidenceScore = 0.0,
                        fetchedAtTimestamp = now,
                        metadata = mapOf("status" to "NO_CONTEXT_FOUND", "reason" to "Empty input")
                    )
                }

                var matchedTitle: String? = null
                var usedLanguage = language

                // 1. Geo-search if coordinates are present
                if (input.latitude != null && input.longitude != null) {
                    val geoCandidates = performGeoSearch(input.latitude, input.longitude, language)
                    matchedTitle = findBestMatchFromGeoCandidates(input, geoCandidates)
                }

                // 2. Title-search as primary query in configured language
                if (matchedTitle == null && input.placeName.isNotBlank()) {
                    val titleCandidates = performTitleSearch(input.placeName, language)
                    matchedTitle = findBestMatchFromTitleCandidates(input, titleCandidates)
                }

                // POI Fallback Strategy:
                val rawPlaceName = input.placeName.trim()
                val mainPoiName = extractMainPoiName(rawPlaceName)
                val parentLocation = extractParentLocation(rawPlaceName, input.address)

                // 3. Fallback: Cleaned main POI name (without comma/dash modifiers)
                if (matchedTitle == null && mainPoiName.isNotBlank() && !mainPoiName.equals(rawPlaceName, ignoreCase = true)) {
                    val poiCandidates = performTitleSearch(mainPoiName, language)
                    matchedTitle = findBestMatchFromTitleCandidates(input, poiCandidates)
                }

                // 4. Fallback: Multi-language search in English if primary language search yielded no match
                if (matchedTitle == null && language != "en") {
                    val enCandidates = performTitleSearch(rawPlaceName, "en")
                    matchedTitle = findBestMatchFromTitleCandidates(input, enCandidates)
                    if (matchedTitle != null) {
                        usedLanguage = "en"
                    } else if (mainPoiName.isNotBlank() && !mainPoiName.equals(rawPlaceName, ignoreCase = true)) {
                        val enPoiCandidates = performTitleSearch(mainPoiName, "en")
                        matchedTitle = findBestMatchFromTitleCandidates(input, enPoiCandidates)
                        if (matchedTitle != null) {
                            usedLanguage = "en"
                        }
                    }
                }

                // 5. Fallback: Parent Location / City search if the POI itself has no standalone Wikipedia article
                if (matchedTitle == null && !parentLocation.isNullOrBlank()) {
                    val cityCandidates = performTitleSearch(parentLocation, language)
                    matchedTitle = findBestMatchFromTitleCandidates(input, cityCandidates) ?: cityCandidates.firstOrNull()
                    if (matchedTitle == null && language != "en") {
                        val enCityCandidates = performTitleSearch(parentLocation, "en")
                        matchedTitle = findBestMatchFromTitleCandidates(input, enCityCandidates) ?: enCityCandidates.firstOrNull()
                        if (matchedTitle != null) {
                            usedLanguage = "en"
                        }
                    }
                }

                if (matchedTitle == null) {
                    return@withContext ContextResult(
                        sourceName = sourceName,
                        sourceType = sourceType,
                        isSuccessful = false,
                        sourceUrl = null,
                        trustScore = 0.95,
                        confidenceScore = 0.0,
                        fetchedAtTimestamp = now,
                        metadata = mapOf("status" to "NO_CONTEXT_FOUND")
                    )
                }

                // 6. Get extract for matched title
                var extract = getExtract(matchedTitle, usedLanguage)

                // If extract is empty or very short (< 100 chars), fallback to parent location extract
                if ((extract.isNullOrBlank() || extract.length < 100) && !parentLocation.isNullOrBlank() && !matchedTitle.equals(parentLocation, ignoreCase = true)) {
                    val cityCandidates = performTitleSearch(parentLocation, language)
                    val cityTitle = cityCandidates.firstOrNull()
                    if (cityTitle != null) {
                        val cityExtract = getExtract(cityTitle, language)
                        if (!cityExtract.isNullOrBlank() && cityExtract.length > (extract?.length ?: 0)) {
                            matchedTitle = cityTitle
                            extract = cityExtract
                            usedLanguage = language
                        }
                    }
                }

                if (extract.isNullOrBlank()) {
                    return@withContext ContextResult(
                        sourceName = sourceName,
                        sourceType = sourceType,
                        isSuccessful = false,
                        sourceUrl = null,
                        trustScore = 0.95,
                        confidenceScore = 0.0,
                        fetchedAtTimestamp = now,
                        metadata = mapOf("status" to "NO_CONTEXT_FOUND", "wikiTitle" to matchedTitle)
                    )
                }

                val wikiUrl = "https://$usedLanguage.wikipedia.org/wiki/${URLEncoder.encode(matchedTitle.replace(" ", "_"), "UTF-8")}"
                ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = extract,
                    sourceUrl = wikiUrl,
                    trustScore = 0.95,
                    confidenceScore = 0.9,
                    fetchedAtTimestamp = now,
                    metadata = mapOf(
                        "wikiTitle" to matchedTitle,
                        "wikiUrl" to wikiUrl,
                        "status" to "MATCHED",
                        "language" to usedLanguage
                    )
                )
            } catch (e: Exception) {
                ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = false,
                    sourceUrl = null,
                    trustScore = 0.95,
                    confidenceScore = 0.0,
                    fetchedAtTimestamp = now,
                    metadata = mapOf("status" to "NO_CONTEXT_FOUND", "error" to (e.localizedMessage ?: "Unknown error")),
                    errorMessage = e.localizedMessage ?: "Wikipedia fetch error"
                )
            }
        }
    }

    private fun extractMainPoiName(rawName: String): String {
        if (rawName.contains(",")) {
            return rawName.substringBefore(",").trim()
        }
        if (rawName.contains(" - ")) {
            return rawName.substringBefore(" - ").trim()
        }
        return rawName.trim()
    }

    private fun extractParentLocation(rawName: String, address: String?): String? {
        if (rawName.contains(",")) {
            val afterComma = rawName.substringAfter(",").trim()
            if (afterComma.isNotBlank()) return afterComma
        }
        if (rawName.contains(" - ")) {
            val afterDash = rawName.substringAfter(" - ").trim()
            if (afterDash.isNotBlank()) return afterDash
        }
        if (!address.isNullOrBlank()) {
            val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (part in parts.reversed()) {
                val cleanPart = part.replace("\\b\\d{4,6}\\b".toRegex(), "").trim()
                if (cleanPart.isNotBlank() && cleanPart.length > 2) {
                    return cleanPart
                }
            }
        }
        return null
    }

    private suspend fun performGeoSearch(lat: Double, lon: Double, lang: String = language): List<GeoCandidate> {
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=$lat|$lon&gsradius=10000&gslimit=10&format=json"
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            val searchResults = json.optJSONObject("query")?.optJSONArray("geosearch") ?: return emptyList()
            val list = mutableListOf<GeoCandidate>()
            for (i in 0 until searchResults.length()) {
                val obj = searchResults.getJSONObject(i)
                list.add(
                    GeoCandidate(
                        title = obj.getString("title"),
                        lat = obj.optDouble("lat", lat),
                        lon = obj.optDouble("lon", lon),
                        dist = obj.optDouble("dist", 0.0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun performTitleSearch(query: String, lang: String = language): List<String> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json"
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            val searchResults = json.optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
            val titles = mutableListOf<String>()
            for (i in 0 until searchResults.length()) {
                titles.add(searchResults.getJSONObject(i).getString("title"))
            }
            titles
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getExtract(title: String, lang: String = language): String? {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&redirects=true&prop=extracts&exintro=true&explaintext=true&titles=$encodedTitle&format=json"
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return null
            val pageId = pages.keys().next() ?: return null
            if (pageId == "-1") return null
            val extract = pages.getJSONObject(pageId).optString("extract", "")
            if (extract.isBlank()) null else extract
        } catch (e: Exception) {
            null
        }
    }

    private fun findBestMatchFromGeoCandidates(input: LocationContextInput, candidates: List<GeoCandidate>): String? {
        if (candidates.isEmpty()) return null
        val placeName = input.placeName.trim()

        for (candidate in candidates) {
            if (calculateNameMatch(placeName, candidate.title)) {
                return candidate.title
            }
        }
        return null
    }

    private fun findBestMatchFromTitleCandidates(input: LocationContextInput, candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        val placeName = input.placeName.trim()

        for (title in candidates) {
            if (calculateNameMatch(placeName, title)) {
                return title
            }
        }
        return null
    }

    private fun calculateNameMatch(name1: String, name2: String): Boolean {
        if (name1.isBlank() || name2.isBlank()) return false
        val clean1 = name1.lowercase().replace("[^a-z0-9äöüß\\s]".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")
        val clean2 = name2.lowercase().replace("[^a-z0-9äöüß\\s]".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")
        if (clean1 == clean2) return true
        if (clean2.contains(clean1)) return true

        val stopWords = setOf("the", "and", "in", "at", "of", "der", "die", "das", "und", "im", "von", "zu")
        val words1 = clean1.split(" ").filter { it.length > 2 }
        val words2 = clean2.split(" ").filter { it.length > 2 }

        if (words1.isEmpty() || words2.isEmpty()) return false

        val sig1 = words1.filter { it !in stopWords }.ifEmpty { words1 }
        val sig2 = words2.filter { it !in stopWords }.ifEmpty { words2 }

        val overlap = sig1.intersect(sig2.toSet())

        if (sig1.size >= 2 && sig2.size == 1) {
            return false
        }

        val ratio1 = overlap.size.toDouble() / sig1.size.toDouble()
        val ratio2 = overlap.size.toDouble() / sig2.size.toDouble()

        return ratio1 >= 0.6 || ratio2 >= 0.8
    }

    private data class GeoCandidate(
        val title: String,
        val lat: Double,
        val lon: Double,
        val dist: Double
    )

    companion object {
        private const val USER_AGENT = "RelevantorApp/1.0 (Contact: info@example.com)"

        private fun defaultNetworkFetcher(urlString: String): String {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode != 200) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            return response.toString()
        }
    }
}
