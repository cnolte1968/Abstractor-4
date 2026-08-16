package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExtractVideoRequestDto(
    @Json(name = "contractVersion") val contractVersion: String? = null,
    @Json(name = "sourceUrl") val sourceUrl: String,
    @Json(name = "sourcePlatform") val sourcePlatform: String,
    @Json(name = "requestedCapabilities") val requestedCapabilities: List<String>,
    @Json(name = "clientContext") val clientContext: Map<String, String>? = null,
    @Json(name = "requestId") val requestId: String? = null
)

@JsonClass(generateAdapter = true)
data class ExtractVideoResponseDto(
    @Json(name = "sourcePlatform") val sourcePlatform: String,
    @Json(name = "providerStatus") val providerStatus: String, // SUCCESS, DEGRADED, FAILED
    @Json(name = "accessStatus") val accessStatus: String, // PUBLIC, LOGIN_REQUIRED, PRIVATE, BLOCKED
    @Json(name = "capabilityStatus") val capabilityStatus: Map<String, String>,
    @Json(name = "metadata") val metadata: ProviderMetadataDto?,
    @Json(name = "content") val content: ProviderContentDto?,
    @Json(name = "errorInformation") val errorInformation: ProviderErrorDto?,
    @Json(name = "diagnostics") val diagnostics: ProviderDiagnosticsDto? = null
)

@JsonClass(generateAdapter = true)
data class ProviderMetadataDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "durationSeconds") val durationSeconds: Int? = null,
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "shortDescription") val shortDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class ProviderContentDto(
    @Json(name = "transcript") val transcript: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "timestamps") val timestamps: List<ProviderTimestampDto>? = null
)

@JsonClass(generateAdapter = true)
data class ProviderTimestampDto(
    @Json(name = "time") val time: Double,
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class ProviderErrorDto(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class ProviderDiagnosticsDto(
    @Json(name = "providerName") val providerName: String,
    @Json(name = "processingTimeMs") val processingTimeMs: Long,
    @Json(name = "diagnosticCode") val diagnosticCode: String? = null
)
