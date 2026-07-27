package com.example.data

import okhttp3.Call
import okhttp3.Dns
import okhttp3.EventListener
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GatewayDiagnostics {
    // General Gateway diagnostics
    var gatewayBaseUrl: String = "https://generativelanguage.googleapis.com/"
    var resolvedHostBeforeRequest: String = "FAIL"
    var httpClientName: String = "Retrofit / OkHttpClient"
    var requestStartTimestamp: String = ""
    var requestFailureStage: String = ""
    var exceptionClass: String = ""
    var exceptionMessage: String = ""
    var networkType: String = ""
    var correlationId: String = ""

    // Preflight check results
    var preflightDns: String = "UNKNOWN"
    var preflightHttps: String = "UNKNOWN"

    // OkHttp DNS instrumentation
    var okHttpDnsStartHost: String = ""
    var okHttpDnsResolvedAddresses: List<String> = emptyList()
    var okHttpDnsException: String = ""
    var dnsOutcome: String = "NOT_RUN"
    var resolvedAddressCount: Int = 0

    // OkHttp Connection instrumentation
    var connectStarted: String = "nein"
    var connectFailedReason: String = ""
    var connectOutcome: String = "NOT_STARTED"
    var failureStage: String = ""

    // Parser diagnostics
    var rawGeminiResponseLength: Int = 0
    var rawGeminiResponseSha256: String = ""
    var rawGeminiFirstSafeChars: String = ""
    var normalizedResponseLength: Int = 0
    var normalizedFirstSafeChars: String = ""
    var looksLikeJson: Boolean = false
    var rootKeysDetected: List<String> = emptyList()
    var takeawayFieldDetected: String = "none"
    var parserStrategiesTried: List<String> = emptyList()
    var parserStrategySucceeded: String = "none"
    var parserFailureReason: String = "none"

    // Prompt diagnostics fields requested for A.1
    var loadedFunctionId: String = ""
    var loadedAnalysisType: String = ""
    var loadedCanonicalAnalysisType: String = ""
    var loadedEngineName: String = ""
    var loadedPromptAssetFile: String = ""
    var loadedPromptResolvedAssetPath: String = ""
    var loadedPromptLength: Int = 0
    var loadedPromptSha256: String = ""
    var loadedPromptFirst300Chars: String = ""
    var loadedPromptContainsOutputLimits: Boolean = false
    var loadedPromptContainsBoilerplateExclusion: Boolean = false

    // Web Extraction Diagnostics
    var finalUrl: String = ""
    var rawHtmlLength: Int = 0
    var textBeforeCleaningLength: Int = 0
    var textAfterCleaningLength: Int = 0
    var selectedContentContainer: String = "none"
    var removedBlockCount: Int = 0
    var removedByRuleCounts: Map<String, Int> = emptyMap()
    var first1000CharsAfterCleaning: String = ""
    var containsExpectedArticleSignals: Map<String, Boolean> = emptyMap()
    var bodyReadLength: Int = 0
    var selectedContainerHtmlLength: Int = 0
    var selectedContainerTextLength: Int = 0

    // Preflight run tracking
    var preflightExecuted: Boolean = false
    var preflightDnsException: String = ""
    var preflightHttpsException: String = ""

    // Source Host Diagnostics
    var sourceUrl: String = ""
    var normalizedSourceUrl: String = ""
    var sourceHost: String = ""
    var sourceDnsOutcome: String = "NOT_RUN"
    var sourceResolvedAddressCount: Int = 0
    var sourceResolvedAddresses: List<String> = emptyList()
    var sourceDnsException: String = ""
    var sourceConnectStarted: String = "nein"
    var sourceConnectOutcome: String = "NOT_STARTED"
    var sourceConnectFailedReason: String = ""
    var sourceHttpStatus: Int = 0

    // YouTube Specific Diagnostics
    var ytTranscriptDiscoveryPath: String = "NONE"
    var ytPlayerClientName: String = "NONE"
    var ytPlayerClientVersion: String = "NONE"
    var ytPlayerHttpStatus: Int = 0
    var ytPlayabilityStatus: String = "NONE"
    var ytTracksFoundCount: Int = 0
    var ytSelectedTrackType: String = "NONE"
    var ytLanguage: String = "NONE"
    var ytCaptionHttpStatus: Int = 0
    var ytCaptionResponseLength: Int = 0
    var ytExtractedSegmentCount: Int = 0
    var ytFinalTranscriptLength: Int = 0
    var ytMetadataOnly: Boolean = false
    var ytFallbackFailureReason: String = ""
    var sourceContentLengthSent: Int = 0

    fun copyFromLastParserReport() {
        val report = SummaryResponseParser.lastReport
        if (report != null) {
            rawGeminiResponseLength = report.rawGeminiResponseLength
            rawGeminiResponseSha256 = report.rawGeminiResponseSha256
            rawGeminiFirstSafeChars = report.rawGeminiFirstSafeChars
            normalizedResponseLength = report.normalizedResponseLength
            normalizedFirstSafeChars = report.normalizedFirstSafeChars
            looksLikeJson = report.looksLikeJson
            rootKeysDetected = report.rootKeysDetected
            takeawayFieldDetected = report.takeawayFieldDetected
            parserStrategiesTried = report.parserStrategiesTried
            parserStrategySucceeded = report.parserStrategySucceeded
            parserFailureReason = report.parserFailureReason
        }
    }

    fun reset() {
        gatewayBaseUrl = "https://generativelanguage.googleapis.com/"
        resolvedHostBeforeRequest = "FAIL"
        httpClientName = "Retrofit / OkHttpClient"
        requestStartTimestamp = ""
        requestFailureStage = ""
        exceptionClass = ""
        exceptionMessage = ""
        networkType = ""
        correlationId = ""
        preflightDns = "UNKNOWN"
        preflightHttps = "UNKNOWN"
        okHttpDnsStartHost = ""
        okHttpDnsResolvedAddresses = emptyList()
        okHttpDnsException = ""
        dnsOutcome = "NOT_RUN"
        resolvedAddressCount = 0
        connectStarted = "nein"
        connectFailedReason = ""
        connectOutcome = "NOT_STARTED"
        failureStage = ""
        rawGeminiResponseLength = 0
        rawGeminiResponseSha256 = ""
        rawGeminiFirstSafeChars = ""
        normalizedResponseLength = 0
        normalizedFirstSafeChars = ""
        looksLikeJson = false
        rootKeysDetected = emptyList()
        takeawayFieldDetected = "none"
        parserStrategiesTried = emptyList()
        parserStrategySucceeded = "none"
        parserFailureReason = "none"
        loadedFunctionId = ""
        loadedAnalysisType = ""
        loadedCanonicalAnalysisType = ""
        loadedEngineName = ""
        loadedPromptAssetFile = ""
        loadedPromptResolvedAssetPath = ""
        loadedPromptLength = 0
        loadedPromptSha256 = ""
        loadedPromptFirst300Chars = ""
        loadedPromptContainsOutputLimits = false
        loadedPromptContainsBoilerplateExclusion = false

        finalUrl = ""
        rawHtmlLength = 0
        textBeforeCleaningLength = 0
        textAfterCleaningLength = 0
        selectedContentContainer = "none"
        removedBlockCount = 0
        removedByRuleCounts = emptyMap()
        first1000CharsAfterCleaning = ""
        containsExpectedArticleSignals = emptyMap()
        bodyReadLength = 0
        selectedContainerHtmlLength = 0
        selectedContainerTextLength = 0
        preflightExecuted = false
        preflightDnsException = ""
        preflightHttpsException = ""

        sourceUrl = ""
        normalizedSourceUrl = ""
        sourceHost = ""
        sourceDnsOutcome = "NOT_RUN"
        sourceResolvedAddressCount = 0
        sourceResolvedAddresses = emptyList()
        sourceDnsException = ""
        sourceConnectStarted = "nein"
        sourceConnectOutcome = "NOT_STARTED"
        sourceConnectFailedReason = ""
        sourceHttpStatus = 0

        // Reset YouTube Specific Diagnostics
        ytTranscriptDiscoveryPath = "NONE"
        ytPlayerClientName = "NONE"
        ytPlayerClientVersion = "NONE"
        ytPlayerHttpStatus = 0
        ytPlayabilityStatus = "NONE"
        ytTracksFoundCount = 0
        ytSelectedTrackType = "NONE"
        ytLanguage = "NONE"
        ytCaptionHttpStatus = 0
        ytCaptionResponseLength = 0
        ytExtractedSegmentCount = 0
        ytFinalTranscriptLength = 0
        ytMetadataOnly = false
        ytFallbackFailureReason = ""
        sourceContentLengthSent = 0
    }
}

object DiagnosticDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        GatewayDiagnostics.okHttpDnsStartHost = hostname
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            GatewayDiagnostics.okHttpDnsResolvedAddresses = addresses.map { it.hostAddress ?: "" }
            GatewayDiagnostics.dnsOutcome = "SUCCESS"
            GatewayDiagnostics.resolvedAddressCount = addresses.size
            return addresses
        } catch (e: UnknownHostException) {
            GatewayDiagnostics.okHttpDnsException = e.javaClass.name + ": " + (e.message ?: "")
            GatewayDiagnostics.dnsOutcome = "FAIL"
            GatewayDiagnostics.resolvedAddressCount = 0
            throw e
        }
    }
}

object DiagnosticEventListener : EventListener() {
    override fun dnsStart(call: Call, domainName: String) {
        GatewayDiagnostics.okHttpDnsStartHost = domainName
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        GatewayDiagnostics.okHttpDnsResolvedAddresses = inetAddressList.map { it.hostAddress ?: "" }
        GatewayDiagnostics.dnsOutcome = "SUCCESS"
        GatewayDiagnostics.resolvedAddressCount = inetAddressList.size
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        GatewayDiagnostics.connectStarted = "ja"
        GatewayDiagnostics.connectOutcome = "STARTED"
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: okhttp3.Protocol?,
        ioe: IOException
    ) {
        GatewayDiagnostics.connectFailedReason = ioe.javaClass.name + ": " + (ioe.message ?: ioe.toString())
        GatewayDiagnostics.connectOutcome = if (ioe is java.net.SocketTimeoutException || ioe.message?.contains("timeout", ignoreCase = true) == true) {
            "TIMEOUT"
        } else {
            "FAILED"
        }
    }
}
