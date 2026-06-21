package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object WebpageExtractor {
    private const val TAG = "WebpageExtractor"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val quickClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun isShortenerOrWrapper(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("lnkd.in") ||
               lower.contains("fb.me") ||
               lower.contains("t.co") ||
               lower.contains("bit.ly") ||
               lower.contains("tinyurl.com") ||
               lower.contains("t.ly") ||
               lower.contains("shorturl.at") ||
               lower.contains("rebrand.ly") ||
               lower.contains("is.gd") ||
               lower.contains("buff.ly") ||
               lower.contains("goo.gl") ||
               lower.contains("ow.ly") ||
               lower.contains("instagr.am") ||
               lower.contains("facebook.com") ||
               lower.contains("instagram.com")
    }

    /**
     * Resolves redirects (e.g. lnkd.in, fb.me, t.co) to retrieve the final destination URL.
     */
    fun resolveUrl(url: String): String {
        // Optimization: Standard URLs do not need expensive redirect resolution pre-checks
        if (!isShortenerOrWrapper(url)) {
            return url
        }

        val request = Request.Builder()
            .url(url)
            .head() // Try HEAD request first for high speed and minimal data usage
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        return try {
            quickClient.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                Log.d(TAG, "Resolved URL path (HEAD): $url -> $finalUrl")
                finalUrl
            }
        } catch (e: Exception) {
            Log.w(TAG, "HEAD request failed for redirect resolution, trying quick GET.", e)
            try {
                val getRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build()
                quickClient.newCall(getRequest).execute().use { response ->
                    response.request.url.toString()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed resolving redirects for $url", ex)
                url // Return original URL on failure
            }
        }
    }

    fun fetchContent(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response code: ${response.code}")
                return null
            }
            val html = response.body?.string() ?: return null
            processHtmlResponse(html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching webpage content for $url, trying HTTP fallback if HTTPS", e)
            // Fallback to HTTP if HTTPS fails (helps with misconfigured SSL/certificates on some domains)
            if (url.startsWith("https://", ignoreCase = true)) {
                val httpUrl = "http://" + url.substring(8)
                Log.i(TAG, "Trying fallback to HTTP: $httpUrl")
                try {
                    val fallbackRequest = request.newBuilder().url(httpUrl).build()
                    val fallbackResponse = client.newCall(fallbackRequest).execute()
                    if (fallbackResponse.isSuccessful) {
                        val html = fallbackResponse.body?.string()
                        if (html != null) {
                            return processHtmlResponse(html)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback to HTTP also failed for $httpUrl", ex)
                }
            }
            null
        }
    }

    private fun processHtmlResponse(html: String): String? {
        // Extract Meta Data from header before stripping HTML!
        val metaInfo = extractMetaInfo(html)
        val pageTitle = metaInfo.first
        val pageDesc = metaInfo.second
        
        val cleaned = cleanHtml(html)
        
        val contentBuilder = StringBuilder()
        if (!pageTitle.isNullOrBlank()) {
            contentBuilder.append("ARTIKEL-TITEL / WEBSEITEN-TITEL: ").append(pageTitle).append("\n")
        }
        if (!pageDesc.isNullOrBlank()) {
            contentBuilder.append("META-BESCHREIBUNG / EINLEITUNG: ").append(pageDesc).append("\n\n")
        }
        
        if (cleaned.length >= 50) {
            contentBuilder.append("EXTRAHIERTER TEXT-INHALT:\n").append(cleaned)
        }
        
        val finalContent = contentBuilder.toString().trim()
        return if (finalContent.length < 50) {
            Log.w(TAG, "Cleaned content is too short (${finalContent.length} chars).")
            null
        } else {
            finalContent
        }
    }

    fun extractMetaInfo(html: String): Pair<String?, String?> {
        val titlePattern = Regex("<title>\\s*([^<]*?)\\s*</title>", RegexOption.IGNORE_CASE)
        val titleMatch = titlePattern.find(html)
        val plainTitle = titleMatch?.groupValues?.get(1)?.trim()

        val ogTitlePattern = Regex("<meta[^>]*?property\\s*=\\s*[\"']og:title[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val ogTitleMatch = ogTitlePattern.find(html) ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?property\\s*=\\s*[\"']og:title[\"']", RegexOption.IGNORE_CASE).find(html)
        val ogTitle = ogTitleMatch?.groupValues?.get(1)?.trim()

        val finalTitle = ogTitle ?: plainTitle

        // Extract description
        val descPattern = Regex("<meta[^>]*?name\\s*=\\s*[\"']description[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        var descMatch = descPattern.find(html) ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?name\\s*=\\s*[\"']description[\"']", RegexOption.IGNORE_CASE).find(html)
        
        if (descMatch == null) {
            descMatch = Regex("<meta[^>]*?property\\s*=\\s*[\"']og:description[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(html)
                ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?property\\s*=\\s*[\"']og:description[\"']", RegexOption.IGNORE_CASE).find(html)
        }
        val description = descMatch?.groupValues?.get(1)?.trim()

        return Pair(unescapeHtmlMeta(finalTitle), unescapeHtmlMeta(description))
    }

    private fun unescapeHtmlMeta(str: String?): String? {
        if (str == null) return null
        return str.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    private fun cleanHtml(html: String): String {
        var text = html

        // Remove head section
        val headRegex = Regex("<head>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)
        text = text.replace(headRegex, "")

        // Remove script tags
        val scriptRegex = Regex("<script[\\s\\S]*?>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
        text = text.replace(scriptRegex, "")

        // Remove style tags
        val styleRegex = Regex("<style[\\s\\S]*?>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
        text = text.replace(styleRegex, "")

        // Remove nav, footer, header tags to keep only core content
        val navRegex = Regex("<nav[\\s\\S]*?>[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE)
        text = text.replace(navRegex, "")

        val footerRegex = Regex("<footer[\\s\\S]*?>[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE)
        text = text.replace(footerRegex, "")

        val headerRegex = Regex("<header[\\s\\S]*?>[\\s\\S]*?</header>", RegexOption.IGNORE_CASE)
        text = text.replace(headerRegex, "")

        // Replace block tags with newlines to preserve readability
        val blockTags = listOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "tr", "br")
        for (tag in blockTags) {
            val tagRegex = Regex("<$tag[\\s\\S]*?>", RegexOption.IGNORE_CASE)
            text = text.replace(tagRegex, "\n")
        }

        // Remove all remaining HTML tags
        val anyTagRegex = Regex("<[^>]*>")
        text = text.replace(anyTagRegex, " ")

        // Decode basic HTML entities
        text = text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")

        // Normalize whitespace and newlines
        val lines = text.split("\n")
        val cleanedLines = lines.map { it.trim() }.filter { it.length > 5 }
        
        // Limit total length to around 15000 characters to keep within prompt limit and avoid token bloat
        val joined = cleanedLines.joinToString("\n")
        return if (joined.length > 15000) {
            joined.take(15000) + "\n...[Inhalt gekürzt aufgrund der Länge]..."
        } else {
            joined
        }
    }
}
