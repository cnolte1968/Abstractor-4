package com.example

import android.content.Intent

object LocalContentExtractionEngine {
    private var scrapedText: String? = null

    fun isSocialMediaOrWalledUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") ||
               lower.contains("facebook.com") ||
               lower.contains("twitter.com") ||
               lower.contains("x.com") ||
               lower.contains("linkedin.com") ||
               lower.contains("tiktok.com")
    }

    fun extractIntentTextPayload(intent: Intent, url: String): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val full = if (subject != null) "$subject\n$text" else text
        return if (full.replace(url, "").trim().length > 10) full else null
    }

    fun getScrapedScreenTextAndReset(): String? {
        val text = scrapedText
        scrapedText = null
        return text
    }

    fun setScrapedText(text: String) {
        scrapedText = text
    }
}
