package com.example.data

import org.json.JSONArray
import org.json.JSONObject

class PipelineReport {
    val metadata = mutableMapOf<String, Any?>(
        "timestamp" to "",
        "appVersion" to "1.0.0",
        "device" to try { android.os.Build.MODEL ?: "UNKNOWN" } catch (e: Throwable) { "TEST_DEVICE" },
        "androidVersion" to try { android.os.Build.VERSION.RELEASE ?: "14" } catch (e: Throwable) { "14" },
        "apiLevel" to try { android.os.Build.VERSION.SDK_INT } catch (e: Throwable) { 34 },
        "networkType" to "UNKNOWN",
        "runId" to "",
        "correlationId" to "",
        "testId" to "",
        "isSmokeTest" to false,
        "isManualRun" to true,
        "sourceTrigger" to "MANUAL_URL",
        "currentScreen" to "START",
        "buildVariant" to "debug"
    )

    val input_intake = mutableMapOf<String, Any?>(
        "rawInput" to "",
        "rawInputLength" to 0,
        "inputTypeDetected" to "UNKNOWN",
        "inputSource" to "UNKNOWN",
        "shareIntentAction" to "",
        "shareIntentMimeType" to "",
        "receivedUrl" to "",
        "urlVisibleInInputField" to "",
        "userEditedInput" to false,
        "inputAccepted" to false,
        "rejectionReason" to "",
        "validationMessages" to ""
    )

    val notation_and_id_resolution = mutableMapOf<String, Any?>(
        "originalAnalysisType" to "",
        "canonicalAnalysisType" to "",
        "featureId" to "",
        "functionId" to "",
        "registryKey" to "",
        "promptKey" to "",
        "smokeId" to "",
        "goldenPath" to "",
        "engineId" to "",
        "validatorId" to "",
        "legacyTypeDetected" to false,
        "legacyTypeValue" to "",
        "canonicalMappingApplied" to false,
        "canonicalMappingSource" to "",
        "anyOldNotationDetected" to false,
        "oldNotationValuesDetected" to "",
        "mappingWarnings" to ""
    )

    val feature_routing = mutableMapOf<String, Any?>(
        "selectedFeatureTitle" to "",
        "selectedFeatureCategory" to "",
        "acceptedInputs" to "",
        "featureEnabled" to false,
        "featureVisible" to false,
        "routeSource" to "",
        "routeDecision" to "",
        "routeTargetUseCase" to "",
        "routeTargetAnalysisType" to ""
    )

    val extractor_selection = mutableMapOf<String, Any?>(
        "inputType" to "",
        "analysisType" to "",
        "canonicalAnalysisType" to "",
        "selectedExtractorName" to "",
        "selectedExtractorClass" to "",
        "extractorRegistryDecision" to "",
        "extractorFallbackUsed" to false,
        "extractorFallbackReason" to "",
        "rejectedExtractors" to "",
        "expectedExtractorForFunction" to "",
        "extractorMatchesExpected" to false
    )

    val url_normalization = mutableMapOf<String, Any?>(
        "rawUrl" to "",
        "trimmedUrl" to "",
        "decodedUrl" to "",
        "youtubeUrlDecoderUsed" to false,
        "schemeAdded" to false,
        "schemeBefore" to "",
        "schemeAfter" to "",
        "host" to "",
        "path" to "",
        "query" to "",
        "fragment" to "",
        "trailingSlashBefore" to "",
        "trailingSlashAfter" to "",
        "normalizedSourceUrl" to "",
        "urlPassedToWebpageExtractor" to "",
        "urlMutationWarnings" to ""
    )

    val source_network_preflight = mutableMapOf<String, Any?>(
        "sourceHost" to "",
        "youtube_com_dns" to "NOT_RUN",
        "wischnewski_unlimited_com_dns" to "NOT_RUN",
        "generativelanguage_googleapis_com_dns" to "NOT_RUN",
        "hosts" to JSONArray()
    )

    val source_http_fetch = mutableMapOf<String, Any?>(
        "httpClientName" to "OkHttp",
        "httpClientConfigSummary" to "Timeout: 30s",
        "requestMethod" to "GET",
        "requestUrl" to "",
        "requestScheme" to "",
        "requestHost" to "",
        "requestPath" to "",
        "requestHeadersSafe" to "",
        "followRedirects" to true,
        "followSslRedirects" to true,
        "connectTimeoutMs" to 30000,
        "readTimeoutMs" to 30000,
        "writeTimeoutMs" to 30000,
        "callStartAt" to "",
        "dnsStartAt" to "",
        "connectStartAt" to "",
        "secureConnectStartAt" to "",
        "responseHeadersStartAt" to "",
        "responseBodyStartAt" to "",
        "finalUrl" to "",
        "redirectCount" to 0,
        "redirectChain" to "",
        "httpStatus" to 0,
        "contentType" to "",
        "contentLengthHeader" to "",
        "bodyReadLength" to 0,
        "fetchOutcome" to "NOT_RUN",
        "exceptionClass" to "",
        "exceptionMessage" to "",
        "mappedErrorCategory" to ""
    )

    val html_extraction = mutableMapOf<String, Any?>(
        "rawHtmlLength" to 0,
        "rawHtmlSha256" to "",
        "rawHtmlFirst500SafeChars" to "",
        "finalUrl" to "",
        "parserLibrary" to "jsoup",
        "documentTitle" to "",
        "metaDescription" to "",
        "candidateContainers" to "",
        "selectedContentContainer" to "none",
        "selectionReason" to "",
        "selectedContainerHtmlLength" to 0,
        "selectedContainerTextLength" to 0,
        "extractionOutcome" to "NOT_RUN",
        "extractionExceptionClass" to "",
        "extractionExceptionMessage" to ""
    )

    val content_cleaning = mutableMapOf<String, Any?>(
        "textBeforeCleaningLength" to 0,
        "textAfterCleaningLength" to 0,
        "removedBlockCount" to 0,
        "removedByRuleCounts" to "",
        "boilerplateRulesApplied" to "",
        "first1000CharsAfterCleaning" to "",
        "containsExpectedArticleSignals" to "",
        "minimumContentLengthRequired" to 0,
        "minimumContentLengthPassed" to false,
        "cleaningOutcome" to "NOT_RUN"
    )

    val engine_routing = mutableMapOf<String, Any?>(
        "analysisType" to "",
        "canonicalAnalysisType" to "",
        "functionId" to "",
        "registryKey" to "",
        "selectedEngineName" to "",
        "selectedEngineClass" to "",
        "expectedEngineClass" to "",
        "engineMatchesExpected" to false,
        "customValidator" to "",
        "engineCapabilities" to "",
        "engineRoutingOutcome" to "NOT_RUN"
    )

    val prompt_loading = mutableMapOf<String, Any?>(
        "promptKey" to "",
        "promptAssetFile" to "",
        "promptResolvedAssetPath" to "",
        "promptLength" to 0,
        "promptSha256" to "",
        "promptFirst300Chars" to "",
        "promptContainsOutputLimits" to false,
        "promptContainsBoilerplateExclusion" to false,
        "promptManifestUsed" to false,
        "promptManifestKey" to "",
        "promptLoadOutcome" to "NOT_RUN",
        "promptLoadExceptionClass" to "",
        "promptLoadExceptionMessage" to ""
    )

    val gemini_request = mutableMapOf<String, Any?>(
        "geminiRequestStarted" to false,
        "gatewayBaseUrl" to "",
        "modelName" to "",
        "requestStartTimestamp" to "",
        "requestPayloadLength" to 0,
        "finalUserContentLength" to 0,
        "sourceContentLengthSent" to 0,
        "promptLengthSent" to 0,
        "requestConfigSummary" to "",
        "apiKeyPresent" to false,
        "networkPreflightDns" to "",
        "networkPreflightHttps" to "",
        "requestOutcome" to "NOT_RUN",
        "requestExceptionClass" to "",
        "requestExceptionMessage" to ""
    )

    val gemini_response = mutableMapOf<String, Any?>(
        "geminiResponseReceived" to false,
        "responseTimestamp" to "",
        "httpStatus" to 0,
        "rawGeminiResponseLength" to 0,
        "rawGeminiResponseSha256" to "",
        "rawGeminiFirst1000SafeChars" to "",
        "finishReason" to "",
        "safetyBlockDetected" to false,
        "responseOutcome" to "NOT_RUN",
        "responseExceptionClass" to "",
        "responseExceptionMessage" to ""
    )

    val response_normalization = mutableMapOf<String, Any?>(
        "normalizedResponseLength" to 0,
        "normalizedFirst1000SafeChars" to "",
        "markdownFenceRemoved" to false,
        "looksLikeJson" to false,
        "rootKeysDetected" to "",
        "normalizationOutcome" to "NOT_RUN",
        "normalizationWarnings" to ""
    )

    val parsing = mutableMapOf<String, Any?>(
        "parserName" to "",
        "parserStrategiesTried" to "",
        "parserStrategySucceeded" to "",
        "takeawayFieldDetected" to "",
        "parsedTitle" to "",
        "parsedOriginalUrl" to "",
        "parsedOwner" to "",
        "parsedShortDescriptionLength" to 0,
        "parsedTakeawayCount" to 0,
        "parserSuccess" to false,
        "parserFailureReason" to "",
        "parserExceptionClass" to "",
        "parserExceptionMessage" to ""
    )

    val contract_validation = mutableMapOf<String, Any?>(
        "validatorName" to "",
        "customValidatorUsed" to false,
        "contractSuccess" to false,
        "validationErrors" to "",
        "validationWarnings" to "",
        "requiredFieldsPresent" to "",
        "outputShape" to "",
        "expectedOutputShape" to "",
        "contractExceptionClass" to "",
        "contractExceptionMessage" to ""
    )

    val rendering = mutableMapOf<String, Any?>(
        "resultRendered" to false,
        "targetScreen" to "",
        "presentationPolicy" to "",
        "renderedTitle" to "",
        "renderedSectionCount" to 0,
        "renderedTakeawayCount" to 0,
        "renderingWarnings" to "",
        "renderingExceptionClass" to "",
        "renderingExceptionMessage" to ""
    )

    val user_actions = mutableMapOf<String, Any?>(
        "copyAvailable" to false,
        "shareAvailable" to false,
        "pdfAvailable" to false,
        "debugAvailable" to false,
        "pipelineReportAvailable" to false,
        "lastMenuAction" to "",
        "copyPROutcome" to ""
    )

    val final_result = mutableMapOf<String, Any?>(
        "finalStatus" to "NOT_RUN",
        "failureStage" to "",
        "failureStepId" to "",
        "userVisibleErrorTitle" to "",
        "userVisibleErrorMessage" to "",
        "technicalErrorCategory" to "",
        "rootCauseCandidate" to "",
        "nextDiagnosticHint" to "",
        "pipelineCompleted" to false
    )

    val location_context = mutableMapOf<String, Any?>(
        "originalUrl" to "",
        "normalizedUrl" to "",
        "incomingPlaceName" to "",
        "resolvedPlaceName" to "",
        "parserStatus" to "NOT_RUN",
        "wikipediaStatus" to "NOT_RUN",
        "wikivoyageStatus" to "NOT_RUN",
        "googleMapsBaseStatus" to "NOT_RUN",
        "fallbackUsed" to false,
        "generatedContextSections" to listOf<String>(),
        "noContextFound" to false
    )

    val steps = mutableListOf<PipelineStep>()

    fun toJsonString(): String {
        val root = JSONObject()
        root.put("metadata", JSONObject(metadata))
        root.put("input_intake", JSONObject(input_intake))
        root.put("notation_and_id_resolution", JSONObject(notation_and_id_resolution))
        root.put("feature_routing", JSONObject(feature_routing))
        root.put("extractor_selection", JSONObject(extractor_selection))
        root.put("url_normalization", JSONObject(url_normalization))
        root.put("source_network_preflight", JSONObject(source_network_preflight))
        root.put("source_http_fetch", JSONObject(source_http_fetch))
        root.put("html_extraction", JSONObject(html_extraction))
        root.put("content_cleaning", JSONObject(content_cleaning))
        root.put("engine_routing", JSONObject(engine_routing))
        root.put("prompt_loading", JSONObject(prompt_loading))
        root.put("gemini_request", JSONObject(gemini_request))
        root.put("gemini_response", JSONObject(gemini_response))
        root.put("response_normalization", JSONObject(response_normalization))
        root.put("parsing", JSONObject(parsing))
        root.put("contract_validation", JSONObject(contract_validation))
        root.put("rendering", JSONObject(rendering))
        root.put("user_actions", JSONObject(user_actions))
        root.put("final_result", JSONObject(final_result))

        val stepsArray = JSONArray()
        for (step in steps) {
            val stepObj = JSONObject()
            stepObj.put("stepId", step.stepId)
            stepObj.put("stepName", step.stepName)
            stepObj.put("status", step.status)
            stepObj.put("startedAt", step.startedAt)
            stepObj.put("endedAt", step.endedAt)
            stepObj.put("durationMs", step.durationMs)
            stepObj.put("inputSummary", step.inputSummary)
            stepObj.put("outputSummary", step.outputSummary)
            stepObj.put("decision", step.decision)
            stepObj.put("nextStep", step.nextStep)
            stepObj.put("exceptionClass", step.exceptionClass)
            stepObj.put("exceptionMessage", step.exceptionMessage)
            stepObj.put("fallbackUsed", step.fallbackUsed)
            stepObj.put("notes", step.notes)
            stepsArray.put(stepObj)
        }
        root.put("pipeline_steps", stepsArray)

        return root.toString(4)
    }
}

data class PipelineStep(
    val stepId: String,
    val stepName: String,
    var status: String = "NOT_RUN", // NOT_RUN / RUNNING / PASS / FAIL / SKIPPED
    var startedAt: String = "",
    var endedAt: String = "",
    var durationMs: Long = 0,
    var inputSummary: String = "",
    var outputSummary: String = "",
    var decision: String = "",
    var nextStep: String = "",
    var exceptionClass: String = "",
    var exceptionMessage: String = "",
    var fallbackUsed: Boolean = false,
    var notes: String = ""
)
