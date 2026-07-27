package com.example.data

import com.example.domain.model.PublicVideoSource
import com.example.domain.model.VideoSourceType

object PublicVideoSourceResolver {
    fun resolve(url: String): PublicVideoSource {
        val trimmedUrl = url.trim()
        val normalized = trimmedUrl.lowercase()
        
        // YouTube check
        if (normalized.contains("youtube.com/watch") ||
            normalized.contains("youtu.be/") ||
            normalized.contains("youtube.com/live/") ||
            normalized.contains("youtube.com/shorts/")) {
            return PublicVideoSource(trimmedUrl, normalized, VideoSourceType.YouTube, platform = "YouTube")
        }

        // Extensions
        val videoExts = listOf(".mp4", ".webm", ".mov", ".m4v")
        val subExts = listOf(".vtt", ".srt", ".ttml")

        for (ext in videoExts) {
            if (normalized.endsWith(ext)) {
                return PublicVideoSource(trimmedUrl, normalized, VideoSourceType.DirectVideo, extension = ext)
            }
        }
        for (ext in subExts) {
            if (normalized.endsWith(ext)) {
                return PublicVideoSource(trimmedUrl, normalized, VideoSourceType.DirectSubtitle, extension = ext)
            }
        }
        
        // Web Page
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return PublicVideoSource(trimmedUrl, normalized, VideoSourceType.PublicWebPage)
        }
        
        return PublicVideoSource(trimmedUrl, normalized, VideoSourceType.Unsupported, reason = "Unrecognized URL format")
    }
}
