package com.example.data

import android.util.Log
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.IOException

object YoutubeTranscriptHelper {
    private const val TAG = "YoutubeTranscriptHelper"

    var client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun cleanYoutubeText(text: String): String {
        val bp = "Enjoy the videos and music that you love, upload original content, and share it all with friends, family, and the world on YouTube."
        var cleaned = text.replace(bp, "", ignoreCase = true)
        
        val segments = listOf(
            "Enjoy the videos and music that you love",
            "upload original content, and share it all with friends",
            "family, and the world on YouTube"
        )
        for (seg in segments) {
            cleaned = cleaned.replace(seg, "", ignoreCase = true)
        }
        return cleaned.trim()
    }

    fun extractYoutubeContent(videoId: String, url: String, analysisId: String = java.util.UUID.randomUUID().toString()): CanonicalAnalysisInput {
        // Try fetching transcript first
        val rawTranscript = fetchTranscript(videoId)
        val transcript = rawTranscript?.let { cleanYoutubeText(it) }
        val oembed = fetchOembedMetadata(videoId)
        val title = oembed?.first ?: "YouTube Video"
        val channel = oembed?.second ?: "Unbekannter Kanal"
        
        if (!transcript.isNullOrBlank() && transcript.length >= 20) {
            return CanonicalAnalysisInput(
                sourceType = SourceType.YOUTUBE,
                rawText = transcript,
                enrichedText = transcript,
                metadata = mapOf(
                    "title" to title,
                    "url" to url,
                    "videoId" to videoId,
                    "channel" to channel
                ),
                structuredExtras = emptyMap(),
                analysisId = analysisId
            )
        }
        
        // Fallback to extended content
        val extendedContent = fetchExtendedYoutubeContent(videoId)
        if (!extendedContent.isNullOrBlank()) {
            return CanonicalAnalysisInput(
                sourceType = SourceType.YOUTUBE,
                rawText = extendedContent,
                enrichedText = extendedContent,
                metadata = mapOf(
                    "title" to title,
                    "url" to url,
                    "videoId" to videoId,
                    "channel" to channel
                ),
                structuredExtras = mapOf("extended" to "true"),
                analysisId = analysisId
            )
        }
        
        // Prevent fallback that only delivers metadata without text
        throw IOException("No transcript or extended description text could be extracted for YouTube video $videoId")
    }

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (e: Exception) {
            println("[$tag] ERROR: $msg ${tr?.let { " - " + it.stackTraceToString() } ?: ""}")
        }
    }

    fun fetchTranscript(videoId: String): String? {
        // Reset YouTube diagnostics
        GatewayDiagnostics.ytTranscriptDiscoveryPath = "NONE"
        GatewayDiagnostics.ytTracksFoundCount = 0
        GatewayDiagnostics.ytSelectedTrackType = "NONE"
        GatewayDiagnostics.ytLanguage = "NONE"
        GatewayDiagnostics.ytCaptionHttpStatus = 0
        GatewayDiagnostics.ytCaptionResponseLength = 0
        GatewayDiagnostics.ytExtractedSegmentCount = 0
        GatewayDiagnostics.ytFinalTranscriptLength = 0
        GatewayDiagnostics.ytFallbackFailureReason = ""

        val url = "https://www.youtube.com/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .header("Cookie", "CONSENT=YES+cb.20230531-04-p0.en+FX+907; SOCS=CAESEwgDEgk0ODE3NzkzOTQaAmRlIAEaBgiA_K6lBg;")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: return null

                val captionUrl = findCaptionUrl(html)
                if (captionUrl != null) {
                    GatewayDiagnostics.ytTranscriptDiscoveryPath = "WATCH_HTML"
                    val transcript = fetchTranscriptFromUrl(captionUrl)
                    if (transcript != null) {
                        GatewayDiagnostics.ytFinalTranscriptLength = transcript.length
                        return transcript
                    }
                }
                
                // Fallback to Player API if WATCH_HTML didn't find any caption track or fetching failed
                try {
                    val fallbackTranscript = fetchTranscriptViaPlayerApi(videoId, html)
                    if (fallbackTranscript != null) {
                        return fallbackTranscript
                    }
                } catch (fallbackEx: Exception) {
                    GatewayDiagnostics.ytFallbackFailureReason = fallbackEx.message ?: fallbackEx.toString()
                    logError(TAG, "Fallback failed for $videoId: ${fallbackEx.message}", fallbackEx)
                }
                null
            }
        } catch (e: Exception) {
            logError(TAG, "Error fetching transcript for $videoId", e)
            null
        }
    }

    private fun fetchTranscriptViaPlayerApi(videoId: String, watchHtml: String): String? {
        // Set diagnostics fields
        GatewayDiagnostics.ytPlayerClientName = "MWEB"
        GatewayDiagnostics.ytPlayerClientVersion = "2.20240718.01.00"

        val apiKeyRegex = Regex("\"INNERTUBE_API_KEY\"\\s*:\\s*\"([^\"]+)\"")
        val apiKeyMatch = apiKeyRegex.find(watchHtml)
        val apiKey = apiKeyMatch?.groupValues?.get(1)
        
        val playerUrl = if (apiKey != null) {
            "https://www.youtube.com/youtubei/v1/player?key=$apiKey"
        } else {
            "https://www.youtube.com/youtubei/v1/player"
        }

        val jsonPayload = """
            {
              "videoId": "$videoId",
              "context": {
                "client": {
                  "clientName": "MWEB",
                  "clientVersion": "2.20240718.01.00",
                  "hl": "de",
                  "gl": "DE"
                }
              }
            }
        """.trimIndent()
        
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonPayload
        )

        val postRequest = Request.Builder()
            .url(playerUrl)
            .post(requestBody)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            .header("Content-Type", "application/json")
            .header("Referer", "https://m.youtube.com/")
            .build()

        client.newCall(postRequest).execute().use { response ->
            GatewayDiagnostics.ytPlayerHttpStatus = response.code
            val jsonResponse = response.body?.string() ?: throw IOException("Empty response from Player API")
            
            // Extract playability status from JSON
            val playabilityStatusIdx = jsonResponse.indexOf("\"playabilityStatus\"")
            val statusStr = if (playabilityStatusIdx != -1) {
                val sub = jsonResponse.substring(playabilityStatusIdx, (playabilityStatusIdx + 200).coerceAtMost(jsonResponse.length))
                extractJsonFieldValue(sub, "status") ?: "NONE"
            } else {
                "NONE"
            }
            GatewayDiagnostics.ytPlayabilityStatus = statusStr

            if (!response.isSuccessful) {
                throw IOException("Player API HTTP error: ${response.code} ${response.message}")
            }

            val captionUrl = findCaptionUrl(jsonResponse)
            if (captionUrl == null) {
                throw IOException("No caption tracks found in Player API response. playabilityStatus: $statusStr")
            }

            GatewayDiagnostics.ytTranscriptDiscoveryPath = "PLAYER_API_FALLBACK"
            val transcript = fetchTranscriptFromUrl(captionUrl)
            if (transcript != null) {
                GatewayDiagnostics.ytFinalTranscriptLength = transcript.length
                return transcript
            } else {
                throw IOException("Failed to parse subtitles from caption URL: $captionUrl")
            }
        }
    }

    private fun findCaptionUrl(html: String): String? {
        val index = html.indexOf("\"captionTracks\"")
        if (index == -1) {
            // Fallback: search for timedtext URL pattern with/without escapes
            val fallbackRegex = Regex("https?:\\\\?/\\\\?/www\\.youtube\\.com\\\\?/api\\\\?/timedtext[^\\s\"'>]+")
            val fallbackMatch = fallbackRegex.find(html)
            if (fallbackMatch != null) {
                var foundUrl = fallbackMatch.value
                foundUrl = foundUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
                GatewayDiagnostics.ytTracksFoundCount = 1
                GatewayDiagnostics.ytSelectedTrackType = "manual"
                GatewayDiagnostics.ytLanguage = "unknown"
                return unescapeString(foundUrl)
            }
            return null
        }
        
        val startIndex = html.indexOf('[', index)
        if (startIndex == -1) return null
        
        // Balanced bracket parsing for the JSON array
        var depth = 0
        var endIndex = -1
        for (i in startIndex until html.length) {
            val c = html[i]
            if (c == '[') depth++
            else if (c == ']') {
                depth--
                if (depth == 0) {
                    endIndex = i + 1
                    break
                }
            }
        }
        if (endIndex == -1) return null
        val jsonArrayStr = html.substring(startIndex, endIndex)
        
        // Extract each object within the array by matching balanced curly braces
        val tracks = mutableListOf<CaptionTrack>()
        var i = 0
        while (i < jsonArrayStr.length) {
            val objStart = jsonArrayStr.indexOf('{', i)
            if (objStart == -1) break
            var depthObj = 0
            var objEnd = -1
            for (j in objStart until jsonArrayStr.length) {
                val c = jsonArrayStr[j]
                if (c == '{') depthObj++
                else if (c == '}') {
                    depthObj--
                    if (depthObj == 0) {
                        objEnd = j + 1
                        break
                    }
                }
            }
            if (objEnd == -1) break
            val objStr = jsonArrayStr.substring(objStart, objEnd)
            
            // Extract baseUrl, languageCode, kind from objStr
            val baseUrl = extractField(objStr, "baseUrl")
            val languageCode = extractField(objStr, "languageCode")
            val kind = extractField(objStr, "kind")
            val vssId = extractField(objStr, "vssId")
            
            if (baseUrl != null) {
                tracks.add(CaptionTrack(baseUrl, languageCode, kind, vssId))
            }
            i = objEnd
        }
        
        if (tracks.isEmpty()) {
            // Fallback: search for timedtext URL pattern with/without escapes
            val fallbackRegex = Regex("https?:\\\\?/\\\\?/www\\.youtube\\.com\\\\?/api\\\\?/timedtext[^\\s\"'>]+")
            val fallbackMatch = fallbackRegex.find(html)
            if (fallbackMatch != null) {
                var foundUrl = fallbackMatch.value
                foundUrl = foundUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
                GatewayDiagnostics.ytTracksFoundCount = 1
                GatewayDiagnostics.ytSelectedTrackType = "manual"
                GatewayDiagnostics.ytLanguage = "unknown"
                return unescapeString(foundUrl)
            }
            return null
        }
        
        GatewayDiagnostics.ytTracksFoundCount = tracks.size

        // Selection logic: German manual, English manual, other manual, German ASR, English ASR, other ASR
        val selectedTrack = tracks.find { (it.kind == null || !it.kind.contains("asr", ignoreCase = true)) && it.languageCode == "de" }
            ?: tracks.find { (it.kind == null || !it.kind.contains("asr", ignoreCase = true)) && it.languageCode == "en" }
            ?: tracks.find { it.kind == null || !it.kind.contains("asr", ignoreCase = true) }
            ?: tracks.find { it.languageCode == "de" }
            ?: tracks.find { it.languageCode == "en" }
            ?: tracks.firstOrNull()

        if (selectedTrack != null) {
            GatewayDiagnostics.ytSelectedTrackType = if (selectedTrack.kind != null && selectedTrack.kind.contains("asr", ignoreCase = true)) "asr" else "manual"
            GatewayDiagnostics.ytLanguage = selectedTrack.languageCode ?: "unknown"
            return unescapeString(selectedTrack.baseUrl)
        }
        return null
    }

    private fun extractField(objStr: String, key: String): String? {
        val search = "\"$key\":\""
        val idx = objStr.indexOf(search)
        if (idx == -1) return null
        val start = idx + search.length
        val sb = java.lang.StringBuilder()
        var escaped = false
        for (i in start until objStr.length) {
            val c = objStr[i]
            if (escaped) {
                sb.append(c)
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }
    
    private data class CaptionTrack(val baseUrl: String, val languageCode: String?, val kind: String?, val vssId: String?)

    private fun decodeJsonString(encoded: String): String {
        return unescapeString(encoded)
    }

    private fun unescapeString(str: String): String {
        return str.replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
            .replace("&amp;", "&")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
    }

    private fun fetchTranscriptFromUrl(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                GatewayDiagnostics.ytCaptionHttpStatus = response.code
                val xml = response.body?.string() ?: return null
                GatewayDiagnostics.ytCaptionResponseLength = xml.length

                val cleanText = parseXmlSubtitles(xml)
                if (cleanText.isBlank()) null else cleanText
            }
        } catch (e: Exception) {
            GatewayDiagnostics.ytCaptionHttpStatus = -1
            GatewayDiagnostics.ytFallbackFailureReason = "Caption fetch error: ${e.message}"
            logError(TAG, "Error fetching subtitles from XML URL", e)
            null
        }
    }

    private fun parseXmlSubtitles(xml: String): String {
        var unescapedXml = xml.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        val pattern = Regex("<text[^>]*>([^<]*)</text>")
        val matches = pattern.findAll(unescapedXml)
        val sb = java.lang.StringBuilder()
        var count = 0
        for (match in matches) {
            val textContent = match.groupValues[1].trim()
            if (textContent.isNotEmpty()) {
                sb.append(textContent).append(" ")
                count++
            }
        }
        GatewayDiagnostics.ytExtractedSegmentCount = count
        return sb.toString().trim()
    }

    fun fetchOembedMetadata(videoId: String): Pair<String, String>? {
        val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val json = response.body?.string() ?: return null
                val title = extractJsonFieldValue(json, "title") ?: return null
                val author = extractJsonFieldValue(json, "author_name") ?: "Unbekannter Kanal"
                Pair(title, author)
            }
        } catch (e: Exception) {
            logError(TAG, "Error fetching oembed metadata for $videoId", e)
            null
        }
    }

    fun fetchExtendedYoutubeContent(videoId: String): String? {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .header("Cookie", "CONSENT=YES+cb.20230531-04-p0.en+FX+907; SOCS=CAESEwgDEgk0ODE3NzkzOTQaAmRlIAEaBgiA_K6lBg;")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null

                val title = extractJsonFieldFromHtml(html, "title") ?: extractMetaTag(html, "title")
                val author = extractJsonFieldFromHtml(html, "author") ?: extractMetaTag(html, "author")
                var description = extractJsonFieldFromHtml(html, "shortDescription") ?: extractMetaTag(html, "description")
                if (description != null) {
                    description = cleanYoutubeText(description)
                }

                if (title.isNullOrBlank() && description.isNullOrBlank()) {
                    return null
                }

                buildString {
                    append("ERWEITERTE YOUTUBE-INHALTSGEWINNUNG (Kein Transkript vorhanden, Inhaltsanalyse basiert auf Video-Metadaten und Vollbeschreibung):\n")
                    append("- Video-Titel: ").append(title ?: "Unbekannter Titel").append("\n")
                    append("- Kanal / Ersteller: ").append(author ?: "Unbekannter Kanal").append("\n")
                    append("- Video-ID: ").append(videoId).append("\n")
                    if (!description.isNullOrBlank()) {
                        append("\nVOLLSTÄNDIGE VIDEO-BESCHREIBUNG / KAPITEL / INHALT:\n")
                        append(description)
                    }
                }
            }
        } catch (e: Exception) {
            logError(TAG, "Error fetching extended content for $videoId", e)
            null
        }
    }

    private fun extractMetaTag(html: String, name: String): String? {
        val patterns = listOf(
            Regex("<meta[^>]*?name\\s*=\\s*[\"']$name[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?name\\s*=\\s*[\"']$name[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]*?property\\s*=\\s*[\"']og:$name[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE),
            Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?property\\s*=\\s*[\"']og:$name[\"']", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                return unescapeString(match.groupValues[1])
            }
        }
        if (name == "title") {
            val titlePattern = Regex("<title>\\s*([^<]*?)\\s*</title>", RegexOption.IGNORE_CASE)
            val titleMatch = titlePattern.find(html)
            if (titleMatch != null) {
                return unescapeString(titleMatch.groupValues[1])
            }
        }
        return null
    }

    private fun extractJsonFieldFromHtml(html: String, key: String): String? {
        val searchToken = "\"$key\":\""
        val index = html.indexOf(searchToken)
        if (index == -1) return null
        val start = index + searchToken.length
        val sb = java.lang.StringBuilder()
        var escaped = false
        for (i in start until html.length) {
            val c = html[i]
            if (escaped) {
                sb.append(c)
                escaped = false
            } else if (c == '\\') {
                escaped = true
                sb.append(c)
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
            }
        }
        return unescapeString(sb.toString())
    }

    private fun extractJsonFieldValue(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        val match = pattern.find(json) ?: return null
        val rawValue = match.groupValues[1]
        
        var cleanValue = rawValue.replace(Regex("\\\\u([0-9a-fA-F]{4})")) { matchResult ->
            try {
                matchResult.groupValues[1].toInt(16).toChar().toString()
            } catch (e: Exception) {
                matchResult.value
            }
        }
        
        cleanValue = cleanValue.replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            
        return cleanValue
    }
}
