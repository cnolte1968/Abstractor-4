package com.example.domain.model

enum class SourcePlatform {
    WEB,
    YOUTUBE,
    TIKTOK,
    INSTAGRAM,
    FACEBOOK,
    GOOGLE_MAPS,
    LOCAL_FILE,
    UNKNOWN
}

enum class CapabilityStatus {
    AVAILABLE,
    POTENTIAL,
    UNAVAILABLE,
    DEGRADED,
    FAILED,
    UNKNOWN
}

data class CapabilityState(
    val capability: SourceCapability,
    val status: CapabilityStatus,
    val detailMessage: String? = null
)

data class SourceProfile(
    val sourceType: SourceType,
    val platform: SourcePlatform,
    val rawInput: String,
    val normalizedUrl: String? = null,
    val capabilities: Map<SourceCapability, CapabilityState> = emptyMap(),
    val isPostFetchConfirmed: Boolean = false
) {
    enum class SourceType {
        WEB_PAGE,
        VIDEO,
        DOCUMENT,
        RAW_TEXT,
        PLACE,
        UNKNOWN
    }

    fun getStatus(capability: SourceCapability): CapabilityStatus {
        return capabilities[capability]?.status ?: CapabilityStatus.UNKNOWN
    }

    fun isAvailable(capability: SourceCapability): Boolean {
        return getStatus(capability) == CapabilityStatus.AVAILABLE
    }

    fun isPotential(capability: SourceCapability): Boolean {
        return getStatus(capability) == CapabilityStatus.POTENTIAL
    }

    fun isDegraded(capability: SourceCapability): Boolean {
        return getStatus(capability) == CapabilityStatus.DEGRADED
    }

    fun isUnavailable(capability: SourceCapability): Boolean {
        val status = getStatus(capability)
        return status == CapabilityStatus.UNAVAILABLE || status == CapabilityStatus.FAILED || status == CapabilityStatus.UNKNOWN
    }

    fun withPostFetchConfirmed(
        updatedCapabilities: Map<SourceCapability, CapabilityState> = this.capabilities
    ): SourceProfile {
        return copy(
            capabilities = updatedCapabilities,
            isPostFetchConfirmed = true
        )
    }

    fun withCapabilityStatus(
        capability: SourceCapability,
        status: CapabilityStatus,
        detailMessage: String? = null
    ): SourceProfile {
        val newCapabilities = capabilities.toMutableMap()
        newCapabilities[capability] = CapabilityState(capability, status, detailMessage)
        return copy(capabilities = newCapabilities)
    }
}
