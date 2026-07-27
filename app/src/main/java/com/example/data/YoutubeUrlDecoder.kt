package com.example.data

import java.util.regex.Pattern

object YoutubeUrlDecoder {
    fun extractUrl(text: String): String? {
        val pattern = Pattern.compile(
            "\\b(https?://)?(www\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/[\\s\\S]*)?\\b"
        )
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    fun isYoutubeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    fun extractYoutubeVideoId(url: String): String? {
        val pattern = Pattern.compile(
            "(?:youtube\\.com/(?:[^/]+/\\S+/|(?:v|e(?:mbed)?|shorts|live)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        )
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}
