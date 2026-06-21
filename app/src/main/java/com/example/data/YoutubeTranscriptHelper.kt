package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object YoutubeTranscriptHelper {
    private const val TAG = "YoutubeTranscriptHelper"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            Log.e(tag, msg, tr)
        } catch (e: Exception) {
            println("[$tag] ERROR: $msg ${tr?.let { " - " + it.stackTraceToString() } ?: ""}")
        }
    }

    fun fetchTranscript(videoId: String): String? {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .header("Cookie", "CONSENT=YES+cb.20230531-04-p0.en+FX+907; SOCS=CAESEwgDEgk0ODE3NzkzOTQaAmRlIAEaBgiA_K6lBg;")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return null

            val captionUrl = findCaptionUrl(html) ?: return null
            fetchTranscriptFromUrl(captionUrl)
        } catch (e: Exception) {
            logError(TAG, "Error fetching transcript for $videoId", e)
            null
        }
    }

    private fun findCaptionUrl(html: String): String? {
        val captionIndex = html.indexOf("captionTracks")
        if (captionIndex == -1) {
            // Fallback: search for timedtext URL pattern with/without escapes
            val fallbackRegex = Regex("https?:\\\\?/\\\\?/www\\.youtube\\.com\\\\?/api\\\\?/timedtext[^\\s\"'>]+")
            val fallbackMatch = fallbackRegex.find(html)
            if (fallbackMatch != null) {
                var foundUrl = fallbackMatch.value
                foundUrl = foundUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
                return unescapeString(foundUrl)
            }
            return null
        }

        // Search for baseUrl within a window of 10000 characters
        val windowEnd = (captionIndex + 10000).coerceAtMost(html.length)
        val window = html.substring(captionIndex, windowEnd)

        // Find the index of "baseUrl" in the window
        val baseUrlIndex = window.indexOf("baseUrl")
        if (baseUrlIndex == -1) return null

        // Find the index of "https" after "baseUrl"
        val httpIndex = window.indexOf("https", baseUrlIndex)
        if (httpIndex == -1) return null

        // Extract everything after https up to the enclosing quote
        val sb = java.lang.StringBuilder()
        var i = httpIndex
        while (i < window.length) {
            val char = window[i]
            if (char == '"' || char == '\'' || char == '<' || char == '>') {
                break
            }
            // If we hit \", which is backslash followed by quote, we stop
            if (char == '\\' && i + 1 < window.length && window[i + 1] == '"') {
                break
            }
            sb.append(char)
            i++
        }

        val extractedUrl = sb.toString()
        if (extractedUrl.startsWith("https")) {
            var url = extractedUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
            return unescapeString(url)
        }

        return null
    }

    private fun decodeJsonString(encoded: String): String {
        return unescapeString(encoded)
    }

    private fun unescapeString(str: String): String {
        return str.replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("\\\\", "\\")
            .replace("&amp;", "&")
    }

    private fun fetchTranscriptFromUrl(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val xml = response.body?.string() ?: return null

            val cleanText = parseXmlSubtitles(xml)
            if (cleanText.isBlank()) null else cleanText
        } catch (e: Exception) {
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
        for (match in matches) {
            val textContent = match.groupValues[1].trim()
            if (textContent.isNotEmpty()) {
                sb.append(textContent).append(" ")
            }
        }
        return sb.toString().trim()
    }

    fun fetchOembedMetadata(videoId: String): Pair<String, String>? {
        val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = response.body?.string() ?: return null
            val title = extractJsonFieldValue(json, "title") ?: return null
            val author = extractJsonFieldValue(json, "author_name") ?: "Unbekannter Kanal"
            Pair(title, author)
        } catch (e: Exception) {
            logError(TAG, "Error fetching oembed metadata for $videoId", e)
            null
        }
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
