package com.example

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * High-performance Two-Stage Local Content Extraction Engine for bypassing Walled Gardens
 * (such as Facebook, Instagram, LinkedIn, news portals, etc.) without network crawling.
 */
object LocalContentExtractionEngine {

    @Volatile
    private var lastScrapedText: String? = null
    
    @Volatile
    private var lastScrapedPackage: String? = null

    /**
     * Dynamically receives text captured from active background windows by the Accessibility Service.
     */
    fun updateScrapedText(text: String, packageName: String?) {
        if (text.isNotBlank() && text.length > 60) {
            lastScrapedText = text
            lastScrapedPackage = packageName
            Log.d("ExtractionEngine", "Successfully received screen scraper text from package: $packageName, length: ${text.length}")
        }
    }

    /**
     * Consumes and resets the captured accessibility screen scraper text.
     */
    fun getScrapedScreenTextAndReset(): String? {
        val backup = lastScrapedText
        lastScrapedText = null
        lastScrapedPackage = null
        if (backup != null && backup.isNotBlank()) {
            Log.d("ExtractionEngine", "Consuming active Accessibility scraper text cache. Length: ${backup.length}")
            return backup
        }
        return null
    }

    /**
     * Checks if a target URL references a walled garden or credentialed platform.
     */
    fun isSocialMediaOrWalledUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") || lower.contains("fb.") || lower.contains("fb/share") ||
               lower.contains("instagram.com") || lower.contains("instagr.am") ||
               lower.contains("linkedin.com") || lower.contains("lnkd.in") ||
               lower.contains("xing.com") || lower.contains("xing.de") ||
               lower.contains("tiktok.com") || lower.contains("threads.net") ||
               lower.contains("pinterest.com") || lower.contains("x.com") || lower.contains("twitter.com")
    }

    /**
     * STAGE 1: Extract Intent Text Payload (Zero-Network-Bypass).
     * If sharing triggers with a complete description or caption alongside the URL in the intent,
     * we extract it directly here to bypass scraping entirely.
     */
    fun extractIntentTextPayload(intent: Intent, url: String): String? {
        val extText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val clipData = intent.clipData
        var altText = ""
        if (clipData != null && clipData.itemCount > 0) {
            altText = clipData.getItemAt(0).text?.toString() ?: ""
        }
        
        val fullPayload = if (extText.length > altText.length) extText else altText
        if (fullPayload.isBlank()) return null

        val cleanUrl = url.trim()
        val leftover = fullPayload
            .replace(cleanUrl, "")
            .replace("https://$cleanUrl", "")
            .replace("http://$cleanUrl", "")
            .replace("\n", " ")
            .trim()

        // If leftover textual content exceeds a reasonable paragraph/caption threshold (around 30 chars),
        // we can summarize it directly!
        if (leftover.length > 30) {
            Log.d("ExtractionEngine", "Stage 1 Zero-Network-Bypass Success! Leftover payload length: ${leftover.length}")
            return fullPayload
        }
        return null
    }
}
