package com.example.data

object YoutubeUrlDecoder {
    fun extractUrl(text: String): String? {
        val regex = Regex("https?://[^\\s\"]+")
        val match = regex.find(text) ?: return null
        var url = match.value
        // Clean trailing punctuation that is not part of the URL parameters
        while (url.isNotEmpty() && (url.endsWith(".") || url.endsWith(",") || url.endsWith(")") || url.endsWith("]") || url.endsWith("!"))) {
            url = url.substring(0, url.length - 1)
        }
        return url.ifEmpty { null }
    }

    fun isYoutubeUrl(url: String): Boolean {
        return url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)
    }

    fun extractYoutubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:v|V)=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("shorts/([a-zA-Z0-9_-]{11})"),
            Regex("embed/([a-zA-Z0-9_-]{11})"),
            Regex("live/([a-zA-Z0-9_-]{11})")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }

        try {
            // Find "v=" parameter manually
            val queryStart = url.indexOf('?')
            if (queryStart != -1 && queryStart < url.length - 1) {
                val query = url.substring(queryStart + 1)
                val params = query.split('&')
                for (param in params) {
                    val pair = param.split('=')
                    if (pair.size == 2 && pair[0].equals("v", ignoreCase = true)) {
                        val vParam = pair[1]
                        if (vParam.matches(Regex("[a-zA-Z0-9_-]{11}"))) {
                            return vParam
                        }
                    }
                }
            }

            // Find path segments
            val pathPart = if (queryStart != -1) url.substring(0, queryStart) else url
            val segments = pathPart.split('/')
            for (segment in segments) {
                if (segment.matches(Regex("[a-zA-Z0-9_-]{11}"))) {
                    return segment
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        return null
    }
}
