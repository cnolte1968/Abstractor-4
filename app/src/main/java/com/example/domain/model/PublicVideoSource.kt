package com.example.domain.model

enum class VideoSourceType {
    YouTube, DirectVideo, DirectSubtitle, PublicWebPage, Unsupported
}

data class PublicVideoSource(
    val originalUrl: String,
    val normalizedUrl: String,
    val type: VideoSourceType,
    val platform: String? = null,
    val extension: String? = null,
    val reason: String? = null
)
