package com.example.domain.usecase

import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import java.util.Locale

class SourceResolver {

    /**
     * Syntactically analyzes the given raw input (URL or text) and returns a pre-fetch [SourceProfile].
     * No network calls, DNS checks, or provider fetches are performed.
     */
    fun resolvePreFetchProfile(input: String): SourceProfile {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            return SourceProfile(
                sourceType = SourceProfile.SourceType.UNKNOWN,
                platform = SourcePlatform.UNKNOWN,
                rawInput = input,
                normalizedUrl = null,
                capabilities = emptyMap(),
                isPostFetchConfirmed = false
            )
        }

        val isUrlLike = isUrl(trimmedInput)
        if (!isUrlLike) {
            val capabilities = mapOf(
                SourceCapability.RAW_TEXT to CapabilityState(
                    capability = SourceCapability.RAW_TEXT,
                    status = CapabilityStatus.AVAILABLE
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.RAW_TEXT,
                platform = SourcePlatform.UNKNOWN,
                rawInput = input,
                normalizedUrl = null,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        val normalizedUrl = normalizeUrl(trimmedInput)
        val lowerUrl = normalizedUrl.lowercase(Locale.ROOT)

        // 1. YouTube
        if (lowerUrl.contains("youtube.com/watch") ||
            lowerUrl.contains("youtu.be/") ||
            lowerUrl.contains("youtube.com/shorts")
        ) {
            val capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.POTENTIAL
                ),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.VIDEO,
                platform = SourcePlatform.YOUTUBE,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 2. TikTok
        if (lowerUrl.contains("tiktok.com/")) {
            val capabilities = mapOf(
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.POTENTIAL
                ),
                SourceCapability.TRANSCRIPT_TEXT to CapabilityState(
                    capability = SourceCapability.TRANSCRIPT_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.VIDEO,
                platform = SourcePlatform.TIKTOK,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 3. Instagram
        if (lowerUrl.contains("instagram.com/")) {
            val capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.POTENTIAL
                ),
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.WEB_PAGE,
                platform = SourcePlatform.INSTAGRAM,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 4. Facebook
        if (lowerUrl.contains("facebook.com/") || lowerUrl.contains("fb.watch/")) {
            val capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.POTENTIAL
                ),
                SourceCapability.VIDEO_METADATA to CapabilityState(
                    capability = SourceCapability.VIDEO_METADATA,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.WEB_PAGE,
                platform = SourcePlatform.FACEBOOK,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 5. Google Maps / Place
        if (lowerUrl.contains("maps.google.") ||
            lowerUrl.contains("google.com/maps") ||
            (lowerUrl.contains("google.") && lowerUrl.contains("/maps")) ||
            lowerUrl.contains("goo.gl/maps") ||
            lowerUrl.contains("maps.app.goo.gl")
        ) {
            val capabilities = mapOf(
                SourceCapability.PLACE_CONTEXT to CapabilityState(
                    capability = SourceCapability.PLACE_CONTEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.PLACE,
                platform = SourcePlatform.GOOGLE_MAPS,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 6. Local file or Document extension
        val isLocalFile = trimmedInput.startsWith("file://") || trimmedInput.startsWith("/")
        val cleanPath = lowerUrl.substringBefore("?").substringBefore("#")
        val isDocExtension = cleanPath.endsWith(".pdf") ||
                cleanPath.endsWith(".doc") ||
                cleanPath.endsWith(".docx") ||
                cleanPath.endsWith(".txt")

        if (isLocalFile || isDocExtension) {
            val capabilities = mapOf(
                SourceCapability.DOCUMENT_TEXT to CapabilityState(
                    capability = SourceCapability.DOCUMENT_TEXT,
                    status = CapabilityStatus.POTENTIAL
                )
            )
            return SourceProfile(
                sourceType = SourceProfile.SourceType.DOCUMENT,
                platform = if (isLocalFile) SourcePlatform.LOCAL_FILE else SourcePlatform.WEB,
                rawInput = input,
                normalizedUrl = normalizedUrl,
                capabilities = capabilities,
                isPostFetchConfirmed = false
            )
        }

        // 7. General Web Page
        val capabilities = mapOf(
            SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                capability = SourceCapability.PAGE_ARTICLE_TEXT,
                status = CapabilityStatus.POTENTIAL
            )
        )
        return SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = SourcePlatform.WEB,
            rawInput = input,
            normalizedUrl = normalizedUrl,
            capabilities = capabilities,
            isPostFetchConfirmed = false
        )
    }

    private fun isUrl(input: String): Boolean {
        if (input.startsWith("http://", ignoreCase = true) ||
            input.startsWith("https://", ignoreCase = true) ||
            input.startsWith("file://", ignoreCase = true)
        ) {
            return true
        }
        if (input.startsWith("/") && (input.endsWith(".pdf", ignoreCase = true) || input.endsWith(".txt", ignoreCase = true))) {
            return true
        }
        val lower = input.lowercase(Locale.ROOT)
        if (lower.startsWith("www.") || lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return true
        }
        val domainRegex = Regex("""^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(/.*)?$""")
        return domainRegex.matches(input)
    }

    private fun normalizeUrl(input: String): String {
        return when {
            input.startsWith("http://", ignoreCase = true) ||
            input.startsWith("https://", ignoreCase = true) ||
            input.startsWith("file://", ignoreCase = true) -> input
            input.startsWith("/") -> "file://$input"
            else -> "https://$input"
        }
    }
}
