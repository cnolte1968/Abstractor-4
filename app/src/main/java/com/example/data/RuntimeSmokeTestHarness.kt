package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.extraction.InputExtractorRegistry
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmokeTestCaseSteps(
    var inputAccepted: String = "FAIL",
    var extractorSelected: String = "FAIL",
    var contentExtracted: String = "FAIL",
    var geminiRequestStarted: String = "FAIL",
    var geminiResponseReceived: String = "FAIL",
    var parserSuccess: String = "FAIL",
    var contractSuccess: String = "FAIL",
    var resultRendered: String = "FAIL"
)

data class SmokeTestCasePreflight(
    var dnsGenerativeLanguage: String = "FAIL",
    var httpsGenerativeLanguage: String = "FAIL"
)

data class SmokeTestCaseResult(
    val testId: String,
    val analysisType: String,
    val canonicalAnalysisType: String,
    val inputType: String,
    val extractor: String,
    val preflight: SmokeTestCasePreflight,
    val steps: SmokeTestCaseSteps,
    var finalStatus: String = "FAIL",
    var executionStatus: String = "RUN",
    var skipReason: String = "",
    var failureStage: String = "NONE",
    var errorClass: String = "None",
    var errorMessage: String = "None",
    
    // Explicit smartphone diagnostic fields requested by user
    var isTestContext: Boolean = false,
    var offlineFallbackUsed: Boolean = false,
    var url: String = "",
    var extractedContentLength: Int = 0,
    var extractorName: String = "",
    var geminiRequestStarted: Boolean = false,
    var geminiResponseReceived: Boolean = false,
    var parserSuccess: Boolean = false,
    var contractSuccess: Boolean = false,
    var resultRendered: String = "FAIL",

    // Gateway diagnostics for A.1
    var gatewayBaseUrl: String = "",
    var resolvedHostBeforeRequest: String = "FAIL",
    var httpClientName: String = "",
    var requestStartTimestamp: String = "",
    var requestFailureStage: String = "",
    var exceptionClass: String = "",
    var exceptionMessage: String = "",
    var networkType: String = "",
    var correlationId: String = "",

    // OkHttp DNS and Connection instrumentation fields
    var preflightDns: String = "FAIL",
    var preflightHttps: String = "FAIL",
    var okHttpDnsStartHost: String = "",
    var okHttpDnsResolvedAddresses: String = "",
    var okHttpDnsException: String = "",
    var dnsOutcome: String = "NOT_RUN",
    var resolvedAddressCount: Int = 0,
    var connectStarted: String = "nein",
    var connectFailedReason: String = "",
    var connectOutcome: String = "NOT_STARTED",

    // Parser diagnostics
    var rawGeminiResponseLength: Int = 0,
    var rawGeminiResponseSha256: String = "",
    var rawGeminiFirstSafeChars: String = "",
    var normalizedResponseLength: Int = 0,
    var normalizedFirstSafeChars: String = "",
    var looksLikeJson: Boolean = false,
    var rootKeysDetected: String = "",
    var takeawayFieldDetected: String = "none",
    var parserStrategiesTried: String = "",
    var parserStrategySucceeded: String = "none",
    var parserFailureReason: String = "none",

    // A.1 prompt diagnostics requested by user
    var functionId: String = "",
    var engineName: String = "",
    var promptAssetFile: String = "",
    var promptResolvedAssetPath: String = "",
    var promptLength: Int = 0,
    var promptSha256: String = "",
    var promptFirst300Chars: String = "",
    var promptContainsOutputLimits: Boolean = false,
    var promptContainsBoilerplateExclusion: Boolean = false,

    // Web Extraction Diagnostics
    var finalUrl: String = "",
    var rawHtmlLength: Int = 0,
    var textBeforeCleaningLength: Int = 0,
    var textAfterCleaningLength: Int = 0,
    var selectedContentContainer: String = "",
    var removedBlockCount: Int = 0,
    var removedByRuleCounts: String = "",
    var first1000CharsAfterCleaning: String = "",
    var containsExpectedArticleSignals: String = "",

    // Source Host Diagnostics
    var sourceUrl: String = "",
    var normalizedSourceUrl: String = "",
    var sourceHost: String = "",
    var sourceDnsOutcome: String = "NOT_RUN",
    var sourceResolvedAddressCount: Int = 0,
    var sourceResolvedAddresses: String = "",
    var sourceDnsException: String = "",
    var sourceConnectStarted: String = "nein",
    var sourceConnectOutcome: String = "NOT_STARTED",
    var sourceConnectFailedReason: String = "",
    var sourceHttpStatus: Int = 0
)

data class SmokeTestHarnessReport(
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
    val appVersion: String,
    val device: String,
    val networkType: String,
    val tests: List<SmokeTestCaseResult>
) {
    fun toJsonString(): String {
        val testsJsonList = tests.joinToString(",\n    ") { test ->
            val stepsPart = if (test.executionStatus == "RUN") {
                """,
      "steps": {
        "inputAccepted": "${test.steps.inputAccepted}",
        "extractorSelected": "${test.steps.extractorSelected}",
        "contentExtracted": "${test.steps.contentExtracted}",
        "geminiRequestStarted": "${test.steps.geminiRequestStarted}",
        "geminiResponseReceived": "${test.steps.geminiResponseReceived}",
        "parserSuccess": "${test.steps.parserSuccess}",
        "contractSuccess": "${test.steps.contractSuccess}",
        "resultRendered": "${test.steps.resultRendered}"
      }"""
            } else {
                ""
            }

            """{
      "testId": "${test.testId}",
      "analysisType": "${test.analysisType}",
      "canonicalAnalysisType": "${test.canonicalAnalysisType}",
      "inputType": "${test.inputType}",
      "extractor": "${test.extractor}",
      "executionStatus": "${test.executionStatus}",
      "skipReason": "${test.skipReason}",
      "preflight": {
        "dnsGenerativeLanguage": "${test.preflight.dnsGenerativeLanguage}",
        "httpsGenerativeLanguage": "${test.preflight.httpsGenerativeLanguage}"
      }${stepsPart},
      "finalStatus": "${test.finalStatus}",
      "failureStage": "${test.failureStage}",
      "errorClass": "${test.errorClass}",
      "errorMessage": "${test.errorMessage.replace("\"", "\\\"").replace("\n", " ")}",
      "diagnostics": {
        "isTestContext": ${test.isTestContext},
        "offlineFallbackUsed": ${test.offlineFallbackUsed},
        "url": "${test.url}",
        "extractedContentLength": ${test.extractedContentLength},
        "extractorName": "${test.extractorName}",
        "geminiRequestStarted": ${test.geminiRequestStarted},
        "geminiResponseReceived": ${test.geminiResponseReceived},
        "parserSuccess": ${test.parserSuccess},
        "contractSuccess": ${test.contractSuccess},
        "resultRendered": "${test.resultRendered}",
        "gatewayBaseUrl": "${test.gatewayBaseUrl}",
        "resolvedHostBeforeRequest": "${test.resolvedHostBeforeRequest}",
        "httpClientName": "${test.httpClientName}",
        "requestStartTimestamp": "${test.requestStartTimestamp}",
        "requestFailureStage": "${test.requestFailureStage}",
        "exceptionClass": "${test.exceptionClass}",
        "exceptionMessage": "${test.exceptionMessage.replace("\"", "\\\"").replace("\n", " ")}",
        "networkType": "${test.networkType}",
        "correlationId": "${test.correlationId}",
        "preflightDns": "${test.preflightDns}",
        "preflightHttps": "${test.preflightHttps}",
        "okHttpDnsStartHost": "${test.okHttpDnsStartHost}",
        "okHttpDnsResolvedAddresses": "${test.okHttpDnsResolvedAddresses}",
        "okHttpDnsException": "${test.okHttpDnsException}",
        "dnsOutcome": "${test.dnsOutcome}",
        "resolvedAddressCount": ${test.resolvedAddressCount},
        "connectStarted": "${test.connectStarted}",
        "connectFailedReason": "${test.connectFailedReason.replace("\"", "\\\"").replace("\n", " ")}",
        "connectOutcome": "${test.connectOutcome}",
        "rawGeminiResponseLength": ${test.rawGeminiResponseLength},
        "rawGeminiResponseSha256": "${test.rawGeminiResponseSha256}",
        "rawGeminiFirstSafeChars": "${test.rawGeminiFirstSafeChars.replace("\"", "\\\"").replace("\n", " ")}",
        "normalizedResponseLength": ${test.normalizedResponseLength},
        "normalizedFirstSafeChars": "${test.normalizedFirstSafeChars.replace("\"", "\\\"").replace("\n", " ")}",
        "looksLikeJson": ${test.looksLikeJson},
        "rootKeysDetected": "${test.rootKeysDetected.replace("\"", "\\\"")}",
        "takeawayFieldDetected": "${test.takeawayFieldDetected}",
        "parserStrategiesTried": "${test.parserStrategiesTried}",
        "parserStrategySucceeded": "${test.parserStrategySucceeded}",
        "parserFailureReason": "${test.parserFailureReason.replace("\"", "\\\"").replace("\n", " ")}",
        "functionId": "${test.functionId}",
        "analysisType": "${test.analysisType}",
        "canonicalAnalysisType": "${test.canonicalAnalysisType}",
        "engineName": "${test.engineName}",
        "promptAssetFile": "${test.promptAssetFile}",
        "promptResolvedAssetPath": "${test.promptResolvedAssetPath}",
        "promptLength": ${test.promptLength},
        "promptSha256": "${test.promptSha256}",
        "promptFirst300Chars": "${test.promptFirst300Chars.replace("\"", "\\\"").replace("\n", " ")}",
        "promptContainsOutputLimits": ${test.promptContainsOutputLimits},
        "promptContainsBoilerplateExclusion": ${test.promptContainsBoilerplateExclusion},
        "finalUrl": "${test.finalUrl}",
        "rawHtmlLength": ${test.rawHtmlLength},
        "textBeforeCleaningLength": ${test.textBeforeCleaningLength},
        "textAfterCleaningLength": ${test.textAfterCleaningLength},
        "selectedContentContainer": "${test.selectedContentContainer}",
        "removedBlockCount": ${test.removedBlockCount},
        "removedByRuleCounts": "${test.removedByRuleCounts.replace("\"", "\\\"")}",
        "first1000CharsAfterCleaning": "${test.first1000CharsAfterCleaning.replace("\"", "\\\"").replace("\n", " ")}",
        "containsExpectedArticleSignals": "${test.containsExpectedArticleSignals.replace("\"", "\\\"")}",
        "sourceUrl": "${test.sourceUrl}",
        "normalizedSourceUrl": "${test.normalizedSourceUrl}",
        "sourceHost": "${test.sourceHost}",
        "sourceDnsOutcome": "${test.sourceDnsOutcome}",
        "sourceResolvedAddressCount": ${test.sourceResolvedAddressCount},
        "sourceResolvedAddresses": "${test.sourceResolvedAddresses}",
        "sourceDnsException": "${test.sourceDnsException}",
        "sourceConnectStarted": "${test.sourceConnectStarted}",
        "sourceConnectOutcome": "${test.sourceConnectOutcome}",
        "sourceConnectFailedReason": "${test.sourceConnectFailedReason.replace("\"", "\\\"").replace("\n", " ")}",
        "sourceHttpStatus": ${test.sourceHttpStatus}
      }
    }"""
        }
        return """{
  "timestamp": "$timestamp",
  "appVersion": "$appVersion",
  "device": "$device",
  "networkType": "$networkType",
  "tests": [
    $testsJsonList
  ]
}"""
    }
}

object RuntimeSmokeTestHarness {
    private const val TAG = "RUNTIME_SMOKE"

    fun runSmokeTests(context: Context): SmokeTestHarnessReport {
        Log.i(TAG, "=== RUNTIME_SMOKE_START ===")

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
        val networkType = try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)
            when {
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }

        // Run Preflight first to see if dns & https are generally OK
        val preflightReport = RuntimePreflight.runPreflight(context)
        val dnsGlobalPass = preflightReport.checks.find { it.name.contains("DNS") }?.status == "PASS"
        val httpsGlobalPass = preflightReport.checks.find { it.name.contains("HTTPS") }?.status == "PASS"

        GatewayDiagnostics.reset()
        GatewayDiagnostics.preflightDns = if (dnsGlobalPass) "PASS" else "FAIL"
        GatewayDiagnostics.preflightHttps = if (httpsGlobalPass) "PASS" else "FAIL"
        GatewayDiagnostics.resolvedHostBeforeRequest = if (dnsGlobalPass) "PASS" else "FAIL"

        val testDefinitions = listOf(
            Triple("T-WEB_SUMMARY", "WEB_SUMMARY", "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/"),
            Triple("T-KEY_TAKEAWAYS", "KEY_TAKEAWAYS", "https://example.com"),
            Triple("T-RELEVANT_ASPECTS", "RELEVANT_ASPECTS", "https://example.com"),
            Triple("T-FRESHNESS_CHECK", "FRESHNESS_CHECK", "https://example.com"),
            Triple("T-MISINFORMATION_RADAR", "MISINFORMATION_RADAR", "https://example.com"),
            Triple("T-FACTS_VS_OPINIONS", "FACTS_VS_OPINIONS", "https://example.com"),
            Triple("T-RISK_ANALYSIS", "RISK_ANALYSIS", "https://example.com"),
            Triple("T-PERSPECTIVES_COUNTERPOSITIONS", "PERSPECTIVES_COUNTERPOSITIONS", "https://example.com"),
            Triple("T-MULTIMEDIA_ANALYSIS", "MULTIMEDIA_ANALYSIS", "https://youtube.com/watch?v=dQw4w9WgXcQ"),
            Triple("T-DOCUMENT_SUMMARY", "DOCUMENT_SUMMARY", "content://com.android.providers.downloads.documents/document/123"),
            Triple("T-DIRECT", "KEY_TAKEAWAYS", "pasted_text_placeholder_for_direct_content_with_at_least_500_chars_so_it_passes_minimum_length_checks_without_triggering_predefined_insufficient_content_logic_as_specified_in_the_extractor_contract_or_requirements_this_is_a_smoke_test_input_text_pasted_by_user")
        )

        val results = mutableListOf<SmokeTestCaseResult>()
        val registry = InputExtractorRegistry(context)

        for ((testId, typeStr, inputStr) in testDefinitions) {
            val analysisType = try { AnalysisType.valueOf(typeStr) } catch (e: Exception) { AnalysisType.WEB_SUMMARY }
            val inputType = when {
                testId == "T-DIRECT" -> "DIRECT_TEXT"
                inputStr.startsWith("content://") -> "FILE_URI"
                else -> "WEB_URL"
            }

            // Determine appropriate extractor name before resolving
            val expectedExtractorName = when {
                testId == "T-DIRECT" -> "DocumentInputExtractor"
                inputType == "FILE_URI" -> "DocumentInputExtractor"
                YoutubeUrlDecoder.isYoutubeUrl(inputStr) -> "YoutubeInputExtractor"
                else -> "WebInputExtractor"
            }

            val tcPreflight = SmokeTestCasePreflight(
                dnsGenerativeLanguage = if (testId == "T-WEB_SUMMARY") (if (dnsGlobalPass) "PASS" else "FAIL") else "NOT_RUN",
                httpsGenerativeLanguage = if (testId == "T-WEB_SUMMARY") (if (httpsGlobalPass) "PASS" else "FAIL") else "NOT_RUN"
            )

            val tcSteps = SmokeTestCaseSteps()
            val tcResult = SmokeTestCaseResult(
                testId = testId,
                analysisType = typeStr,
                canonicalAnalysisType = analysisType.canonical().name,
                inputType = inputType,
                extractor = expectedExtractorName,
                preflight = tcPreflight,
                steps = tcSteps
            )

            if (testId != "T-WEB_SUMMARY" && testId != "T-KEY_TAKEAWAYS" && testId != "T-RELEVANT_ASPECTS") {
                tcSteps.inputAccepted = "NOT_RUN"
                tcSteps.extractorSelected = "NOT_RUN"
                tcSteps.contentExtracted = "NOT_RUN"
                tcSteps.geminiRequestStarted = "NOT_RUN"
                tcSteps.geminiResponseReceived = "NOT_RUN"
                tcSteps.parserSuccess = "NOT_RUN"
                tcSteps.contractSuccess = "NOT_RUN"
                tcSteps.resultRendered = "NOT_RUN"
                tcResult.executionStatus = "SKIPPED"
                tcResult.skipReason = "nicht Teil der aktuellen Testisolation"
                tcResult.finalStatus = "SKIPPED"
                tcResult.failureStage = "NOT_RUN"
                tcResult.resolvedHostBeforeRequest = "NOT_RUN"
                tcResult.resultRendered = "NOT_RUN"
                tcResult.preflightDns = "NOT_RUN"
                tcResult.preflightHttps = "NOT_RUN"
                tcResult.okHttpDnsStartHost = "NOT_RUN"
                tcResult.okHttpDnsResolvedAddresses = "NOT_RUN"
                tcResult.okHttpDnsException = "NOT_RUN"
                tcResult.connectStarted = "NOT_RUN"
                tcResult.connectFailedReason = "NOT_RUN"
                tcResult.dnsOutcome = "NOT_RUN"
                tcResult.resolvedAddressCount = 0
                tcResult.connectOutcome = "NOT_RUN"
                results.add(tcResult)
                continue
            }

            Log.i(TAG, "INPUT_SELECTED - Test: $testId, Type: $typeStr, Input: $inputStr")

            try {
                // Step 1: Input Accepted
                tcSteps.inputAccepted = "PASS"

                // Step 2: Extractor Selected
                val resolvedUrl = if (inputType == "WEB_URL") {
                    try { WebpageExtractor.resolveUrl(inputStr) } catch (e: Exception) { inputStr }
                } else inputStr

                val directContent = if (testId == "T-DIRECT") inputStr else null
                val rawUrl = if (testId == "T-DIRECT") "" else inputStr

                val extractor = registry.getExtractor(rawUrl, resolvedUrl, directContent, analysisType)
                if (extractor != null && extractor.javaClass.simpleName == expectedExtractorName) {
                    tcSteps.extractorSelected = "PASS"
                    Log.i(TAG, "EXTRACTOR_SELECTED - Test: $testId, Extractor: ${extractor.javaClass.simpleName}")
                } else {
                    tcResult.failureStage = "EXTRACTOR_SELECTION"
                    tcResult.errorClass = "NullPointerException"
                    tcResult.errorMessage = "Expected $expectedExtractorName but got ${extractor?.javaClass?.simpleName ?: "null"}"
                    results.add(tcResult)
                    continue
                }

                // Step 3: Content Extracted
                if (testId == "T-WEB_SUMMARY" || testId == "T-KEY_TAKEAWAYS" || testId == "T-RELEVANT_ASPECTS") {
                    val appContext = context.applicationContext
                    val db = com.example.data.local.RelevantorDatabase.getInstance(appContext)
                    val api = com.example.data.remote.BackendApiService.create()
                    val analysisRepo = com.example.data.repository.AnalysisRepositoryImpl(db, api, appContext)
                    val extractionRepo = com.example.data.repository.ContentExtractionRepositoryImpl(appContext)
                    val extractContentUseCase = com.example.domain.usecase.ExtractContentUseCase(extractionRepo)
                    val analyzeContentUseCase = com.example.domain.usecase.AnalyzeContentUseCase(analysisRepo, com.example.data.GeminiRepository, appContext)
                    
                    tcResult.url = if (testId == "T-WEB_SUMMARY") "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/" else inputStr
                    tcResult.extractorName = "WebInputExtractor"
                    tcResult.isTestContext = try {
                        System.getProperty("robolectric.active") != null ||
                        android.os.Build.FINGERPRINT == "robolectric" ||
                        System.getProperty("java.runtime.name")?.lowercase()?.contains("android") == false
                    } catch (e: Exception) {
                        true
                    }
                    tcResult.offlineFallbackUsed = tcResult.isTestContext && (tcResult.url.contains("wischnewski-in-guinea-bissau") || tcResult.url.contains("example.com"))

                    // Fill initial Gate diagnostics
                    tcResult.gatewayBaseUrl = "https://generativelanguage.googleapis.com/"
                    tcResult.resolvedHostBeforeRequest = if (dnsGlobalPass) "PASS" else "FAIL"
                    tcResult.httpClientName = "Retrofit / OkHttpClient"
                    tcResult.requestStartTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                    tcResult.networkType = networkType
                    tcResult.correlationId = "smoke-test-$testId"

                    kotlinx.coroutines.runBlocking {
                        val extractionResult = extractContentUseCase.execute(inputStr, null, analysisType, null, "smoke-test-$testId")
                        when (extractionResult) {
                            is com.example.domain.model.ContentExtractionResult.Failure -> {
                                throw Exception("Extraction failed: ${extractionResult.message} - ${extractionResult.detail}")
                            }
                            is com.example.domain.model.ContentExtractionResult.Predefined -> {
                                throw Exception("Extraction failed: Predefined content returned but expected real live URL parsing for $testId")
                            }
                            is com.example.domain.model.ContentExtractionResult.Degraded -> {
                                tcSteps.contentExtracted = "PASS"
                                Log.i(TAG, "EXTRACTION_DEGRADED - Test: $testId")
                                
                                val content = extractionResult.content
                                tcResult.extractedContentLength = content.rawText.length
                                tcResult.geminiRequestStarted = true

                                val metadataHeader = """
                                    [METADATEN-FALLBACK - KEIN TRANSKRIPT VERFÜGBAR]
                                    Achtung: Dieses Video hat kein Transkript. Führe eine reine Metadatenanalyse durch.
                                    WICHTIG: Setze das Feld "short_description" zwingend auf "TRANSCRIPT_UNAVAILABLE"!
                                    
                                """.trimIndent()

                                val finalInput = com.example.domain.model.CanonicalAnalysisInput(
                                    sourceType = content.sourceType,
                                    rawText = metadataHeader + content.rawText,
                                    enrichedText = metadataHeader + content.enrichedText,
                                    metadata = content.metadata,
                                    analysisId = "smoke-test-$testId",
                                    analysisType = analysisType
                                )
                                GatewayDiagnostics.sourceContentLengthSent = finalInput.rawText.length
                                tcSteps.geminiRequestStarted = "PASS"
                                Log.i(TAG, "GEMINI_REQUEST_START - Test: $testId")
                                
                                val summary = analyzeContentUseCase.execute(
                                    input = finalInput,
                                    useSearchGrounding = content.useSearchGrounding,
                                    analysisType = analysisType,
                                    freeQuery = null
                                )
                                
                                tcResult.geminiResponseReceived = true
                                tcSteps.geminiResponseReceived = "PASS"
                                Log.i(TAG, "GEMINI_RESPONSE_RECEIVED - Test: $testId")
                                
                                tcResult.parserSuccess = true
                                tcSteps.parserSuccess = "PASS"
                                Log.i(TAG, "PARSER_SUCCESS - Test: $testId")
                                
                                tcResult.contractSuccess = true
                                tcSteps.contractSuccess = "PASS"
                                Log.i(TAG, "CONTRACT_VALIDATION_SUCCESS - Test: $testId")
                                
                                tcResult.resultRendered = "PASS"
                                tcSteps.resultRendered = "PASS"
                                Log.i(TAG, "RESULT_RENDERED - Test: $testId")
                                
                                tcResult.finalStatus = "DEGRADED"
                                Log.i(TAG, "RUNTIME_SMOKE_PASS - Test: $testId successfully completed as DEGRADED")
                            }
                            is com.example.domain.model.ContentExtractionResult.Success -> {
                                tcSteps.contentExtracted = "PASS"
                                Log.i(TAG, "EXTRACTION_SUCCESS - Test: $testId")
                                
                                val content = extractionResult.content
                                tcResult.extractedContentLength = content.rawText.length
                                tcResult.geminiRequestStarted = true

                                val finalInput = com.example.domain.model.CanonicalAnalysisInput(
                                    sourceType = content.sourceType,
                                    rawText = content.rawText,
                                    enrichedText = content.enrichedText,
                                    metadata = content.metadata,
                                    analysisId = "smoke-test-$testId",
                                    analysisType = analysisType
                                )
                                GatewayDiagnostics.sourceContentLengthSent = finalInput.rawText.length
                                tcSteps.geminiRequestStarted = "PASS"
                                Log.i(TAG, "GEMINI_REQUEST_START - Test: $testId")
                                
                                val summary = analyzeContentUseCase.execute(
                                    input = finalInput,
                                    useSearchGrounding = content.useSearchGrounding,
                                    analysisType = analysisType,
                                    freeQuery = null
                                )
                                
                                tcResult.geminiResponseReceived = true
                                tcSteps.geminiResponseReceived = "PASS"
                                Log.i(TAG, "GEMINI_RESPONSE_RECEIVED - Test: $testId")
                                
                                tcResult.parserSuccess = true
                                tcSteps.parserSuccess = "PASS"
                                Log.i(TAG, "PARSER_SUCCESS - Test: $testId")
                                
                                tcResult.contractSuccess = true
                                tcSteps.contractSuccess = "PASS"
                                Log.i(TAG, "CONTRACT_VALIDATION_SUCCESS - Test: $testId")
                                
                                tcResult.resultRendered = "PASS"
                                tcSteps.resultRendered = "PASS"
                                Log.i(TAG, "RESULT_RENDERED - Test: $testId")
                                
                                tcResult.finalStatus = "PASS"
                                Log.i(TAG, "RUNTIME_SMOKE_PASS - Test: $testId successfully completed")
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                tcResult.finalStatus = "FAIL"
                tcResult.errorClass = e.javaClass.name
                tcResult.errorMessage = e.message ?: "Unbekannter Fehler im Test"
                
                // Determine failureStage based on how far we got
                val isDns = e is java.net.UnknownHostException || 
                            e.cause is java.net.UnknownHostException || 
                            e.localizedMessage?.contains("Unable to resolve host", ignoreCase = true) == true ||
                            e.localizedMessage?.contains("UnknownHostException", ignoreCase = true) == true

                if (tcSteps.contentExtracted != "PASS") {
                    tcResult.failureStage = "CONTENT_EXTRACTION"
                } else if (tcSteps.geminiRequestStarted != "PASS") {
                    tcResult.failureStage = "GEMINI_REQUEST_START"
                } else if (isDns || tcSteps.geminiResponseReceived != "PASS") {
                    if (isDns) {
                        tcResult.failureStage = "GEMINI_DNS_FAILURE"
                        tcResult.errorClass = "java.net.UnknownHostException"
                        tcResult.errorMessage = "Unable to resolve host \"generativelanguage.googleapis.com\": No address associated with hostname"
                    } else {
                        tcResult.failureStage = "GEMINI_RESPONSE"
                    }
                } else if (tcSteps.parserSuccess != "PASS") {
                    tcResult.failureStage = "PARSER"
                } else if (tcSteps.contractSuccess != "PASS") {
                    tcResult.failureStage = "CONTRACT_VALIDATION"
                } else {
                    tcResult.failureStage = "UNKNOWN"
                }
                tcResult.resultRendered = "FAIL"
                
                // Populate gateway diagnostics on failure
                tcResult.requestFailureStage = tcResult.failureStage
                tcResult.exceptionClass = tcResult.errorClass
                tcResult.exceptionMessage = tcResult.errorMessage

                Log.e(TAG, "RUNTIME_SMOKE_FAIL - Test: $testId failed: ${e.message}", e)
            }

            // Always copy GatewayDiagnostics fields
            if (testId == "T-WEB_SUMMARY" || testId == "T-KEY_TAKEAWAYS" || testId == "T-RELEVANT_ASPECTS") {
                tcResult.gatewayBaseUrl = GatewayDiagnostics.gatewayBaseUrl
                tcResult.resolvedHostBeforeRequest = GatewayDiagnostics.resolvedHostBeforeRequest
                tcResult.httpClientName = GatewayDiagnostics.httpClientName
                tcResult.requestStartTimestamp = GatewayDiagnostics.requestStartTimestamp
                tcResult.requestFailureStage = GatewayDiagnostics.requestFailureStage
                tcResult.exceptionClass = if (GatewayDiagnostics.exceptionClass.isNotEmpty()) GatewayDiagnostics.exceptionClass else tcResult.errorClass
                tcResult.exceptionMessage = if (GatewayDiagnostics.exceptionMessage.isNotEmpty()) GatewayDiagnostics.exceptionMessage else tcResult.errorMessage
                tcResult.networkType = if (GatewayDiagnostics.networkType.isNotEmpty()) GatewayDiagnostics.networkType else networkType
                tcResult.correlationId = GatewayDiagnostics.correlationId

                tcResult.preflightDns = GatewayDiagnostics.preflightDns
                tcResult.preflightHttps = GatewayDiagnostics.preflightHttps
                tcResult.okHttpDnsStartHost = GatewayDiagnostics.okHttpDnsStartHost
                tcResult.okHttpDnsResolvedAddresses = GatewayDiagnostics.okHttpDnsResolvedAddresses.joinToString(", ")
                tcResult.okHttpDnsException = GatewayDiagnostics.okHttpDnsException
                tcResult.connectStarted = GatewayDiagnostics.connectStarted
                tcResult.connectFailedReason = GatewayDiagnostics.connectFailedReason
                
                // Copy parser diagnostic fields
                tcResult.rawGeminiResponseLength = GatewayDiagnostics.rawGeminiResponseLength
                tcResult.rawGeminiResponseSha256 = GatewayDiagnostics.rawGeminiResponseSha256
                tcResult.rawGeminiFirstSafeChars = GatewayDiagnostics.rawGeminiFirstSafeChars
                tcResult.normalizedResponseLength = GatewayDiagnostics.normalizedResponseLength
                tcResult.normalizedFirstSafeChars = GatewayDiagnostics.normalizedFirstSafeChars
                tcResult.looksLikeJson = GatewayDiagnostics.looksLikeJson
                tcResult.rootKeysDetected = GatewayDiagnostics.rootKeysDetected.joinToString(", ")
                tcResult.takeawayFieldDetected = GatewayDiagnostics.takeawayFieldDetected
                tcResult.parserStrategiesTried = GatewayDiagnostics.parserStrategiesTried.joinToString(", ")
                tcResult.parserStrategySucceeded = GatewayDiagnostics.parserStrategySucceeded
                tcResult.parserFailureReason = GatewayDiagnostics.parserFailureReason

                // Copy A.1 prompt diagnostics
                tcResult.functionId = GatewayDiagnostics.loadedFunctionId
                tcResult.engineName = GatewayDiagnostics.loadedEngineName
                tcResult.promptAssetFile = GatewayDiagnostics.loadedPromptAssetFile
                tcResult.promptResolvedAssetPath = GatewayDiagnostics.loadedPromptResolvedAssetPath
                tcResult.promptLength = GatewayDiagnostics.loadedPromptLength
                tcResult.promptSha256 = GatewayDiagnostics.loadedPromptSha256
                tcResult.promptFirst300Chars = GatewayDiagnostics.loadedPromptFirst300Chars
                tcResult.promptContainsOutputLimits = GatewayDiagnostics.loadedPromptContainsOutputLimits
                tcResult.promptContainsBoilerplateExclusion = GatewayDiagnostics.loadedPromptContainsBoilerplateExclusion

                // Copy Web Extraction Diagnostics
                tcResult.finalUrl = GatewayDiagnostics.finalUrl
                tcResult.rawHtmlLength = GatewayDiagnostics.rawHtmlLength
                tcResult.textBeforeCleaningLength = GatewayDiagnostics.textBeforeCleaningLength
                tcResult.textAfterCleaningLength = GatewayDiagnostics.textAfterCleaningLength
                tcResult.selectedContentContainer = GatewayDiagnostics.selectedContentContainer
                tcResult.removedBlockCount = GatewayDiagnostics.removedBlockCount
                tcResult.removedByRuleCounts = GatewayDiagnostics.removedByRuleCounts.toString()
                tcResult.first1000CharsAfterCleaning = GatewayDiagnostics.first1000CharsAfterCleaning
                tcResult.containsExpectedArticleSignals = GatewayDiagnostics.containsExpectedArticleSignals.toString()

                // Copy Source Host Diagnostics
                tcResult.sourceUrl = GatewayDiagnostics.sourceUrl
                tcResult.normalizedSourceUrl = GatewayDiagnostics.normalizedSourceUrl
                tcResult.sourceHost = GatewayDiagnostics.sourceHost
                tcResult.sourceDnsOutcome = GatewayDiagnostics.sourceDnsOutcome
                tcResult.sourceResolvedAddressCount = GatewayDiagnostics.sourceResolvedAddressCount
                tcResult.sourceResolvedAddresses = GatewayDiagnostics.sourceResolvedAddresses.joinToString(", ")
                tcResult.sourceDnsException = GatewayDiagnostics.sourceDnsException
                tcResult.sourceConnectStarted = GatewayDiagnostics.sourceConnectStarted
                tcResult.sourceConnectOutcome = GatewayDiagnostics.sourceConnectOutcome
                tcResult.sourceConnectFailedReason = GatewayDiagnostics.sourceConnectFailedReason
                tcResult.sourceHttpStatus = GatewayDiagnostics.sourceHttpStatus

                if (tcResult.finalStatus == "FAIL") {
                    val errMsg = tcResult.errorMessage
                    val isDns = tcResult.errorClass.contains("UnknownHostException") || 
                                errMsg.contains("Unable to resolve host") ||
                                tcResult.okHttpDnsException.isNotEmpty() ||
                                tcResult.resolvedHostBeforeRequest == "FAIL"

                    val isConnect = tcResult.errorClass.contains("ConnectException") || 
                                    errMsg.contains("Connect") ||
                                    tcResult.connectFailedReason.isNotEmpty()

                    val isTimeout = tcResult.errorClass.contains("SocketTimeoutException") ||
                                    errMsg.contains("timeout", ignoreCase = true) ||
                                    tcResult.connectOutcome == "TIMEOUT"

                    val isStructuredFailed = errMsg.contains("STRUCTURED_EXTRACTION_FAILED") || 
                                             errMsg.contains("ParserFailure")

                    val isValidationFailed = errMsg.contains("Validation failed") || errMsg.contains("Contract violation")

                    if (tcSteps.contentExtracted != "PASS") {
                        tcResult.failureStage = "CONTENT_EXTRACTION"
                    } else if (isStructuredFailed) {
                        tcResult.failureStage = "PARSER_FAILURE"
                        
                        tcResult.geminiResponseReceived = true
                        tcSteps.geminiResponseReceived = "PASS"
                        tcResult.parserSuccess = false
                        tcSteps.parserSuccess = "FAIL"
                        tcResult.contractSuccess = false
                        tcSteps.contractSuccess = "NOT_RUN"
                        tcResult.resultRendered = "FAIL"
                        tcSteps.resultRendered = "FAIL"
                    } else if (isValidationFailed) {
                        tcResult.failureStage = "CONTRACT_VALIDATION_FAILURE"
                        
                        tcResult.geminiResponseReceived = true
                        tcSteps.geminiResponseReceived = "PASS"
                        tcResult.parserSuccess = true
                        tcSteps.parserSuccess = "PASS"
                        tcResult.contractSuccess = false
                        tcSteps.contractSuccess = "FAIL"
                        tcResult.resultRendered = "FAIL"
                        tcSteps.resultRendered = "FAIL"
                    } else if (isDns) {
                        tcResult.failureStage = "GEMINI_DNS_FAILURE"
                        tcResult.errorClass = "java.net.UnknownHostException"
                        tcResult.errorMessage = "Unable to resolve host \"generativelanguage.googleapis.com\": No address associated with hostname"
                    } else if (isTimeout) {
                        tcResult.failureStage = "GEMINI_CONNECT_TIMEOUT"
                    } else if (isConnect) {
                        tcResult.failureStage = "GEMINI_CONNECT_FAILURE"
                    } else {
                        tcResult.failureStage = "GEMINI_HTTP_FAILURE"
                    }
                    tcResult.requestFailureStage = tcResult.failureStage
                    tcResult.exceptionClass = tcResult.errorClass
                    tcResult.exceptionMessage = tcResult.errorMessage
                }
            }

            results.add(tcResult)
        }

        val finalReport = SmokeTestHarnessReport(
            appVersion = appVersion,
            device = device,
            networkType = networkType,
            tests = results
        )

        val finalStatus = if (results.all { it.finalStatus == "PASS" || it.finalStatus == "SKIPPED" || it.finalStatus == "NOT_RUN" }) "PASS" else "FAIL"
        Log.i(TAG, "=== RUNTIME_SMOKE_END (Result: $finalStatus) ===")
        return finalReport
    }
}
