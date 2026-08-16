package com.example.data.diagnostics

import com.example.data.PipelineReport

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportSanitizer {
    private const val MAX_REPORT_SIZE_BYTES = 65536
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private val SENSITIVE_KEY_PATTERNS = listOf(
        "key", "api_key", "apikey", "token", "auth", "authorization",
        "bearer", "cookie", "session", "secret", "password"
    )

    fun buildAndSanitizeReport(
        report: PipelineReport,
        contributors: List<DiagnosticContributor> = listOf(LocationContextDiagnosticContributor())
    ): String {
        var redactionsCount = 0

        // Helper to redact string value
        fun sanitizeStringValue(key: String, value: String): String {
            val lowerKey = key.lowercase(Locale.ROOT)
            val isSensitiveKey = SENSITIVE_KEY_PATTERNS.any { lowerKey.contains(it) }
            if (isSensitiveKey && value.isNotBlank()) {
                redactionsCount++
                return "[REDACTED]"
            }
            if (value.startsWith("Bearer ", ignoreCase = true) || value.startsWith("AIzaSy", ignoreCase = false)) {
                redactionsCount++
                return "[REDACTED]"
            }
            return value
        }

        // Helper to sanitize Map
        fun sanitizeMap(map: Map<String, Any?>): JSONObject {
            val json = JSONObject()
            for ((key, rawValue) in map) {
                when (rawValue) {
                    is String -> json.put(key, sanitizeStringValue(key, rawValue))
                    is Number, is Boolean -> json.put(key, rawValue)
                    is List<*> -> {
                        val arr = JSONArray()
                        for (item in rawValue) {
                            if (item is String) {
                                arr.put(sanitizeStringValue(key, item))
                            } else if (item is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                arr.put(sanitizeMap(item as Map<String, Any?>))
                            } else {
                                arr.put(item ?: JSONObject.NULL)
                            }
                        }
                        json.put(key, arr)
                    }
                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        json.put(key, sanitizeMap(rawValue as Map<String, Any?>))
                    }
                    else -> json.put(key, rawValue ?: JSONObject.NULL)
                }
            }
            return json
        }

        fun calculateSha256(text: String): String {
            if (text.isEmpty()) return ""
            return try {
                val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                ""
            }
        }

        fun makeTextMetadata(text: String, contentType: String, status: String): JSONObject {
            val meta = JSONObject()
            meta.put("char_count", text.length)
            meta.put("sha256", calculateSha256(text))
            meta.put("content_type", contentType)
            meta.put("status", status)
            return meta
        }

        val root = JSONObject()

        // Core required V1 schema fields
        root.put("report_schema_version", "1.0.0")
        root.put("generated_at", timeFormat.format(Date()))

        // 1. metadata
        val metadataObj = sanitizeMap(report.metadata)
        root.put("metadata", metadataObj)

        // 2. ui_state
        val uiStateObj = JSONObject()
        uiStateObj.put("current_screen", report.metadata["currentScreen"] ?: "START")
        uiStateObj.put("user_actions", sanitizeMap(report.user_actions))
        root.put("ui_state", uiStateObj)

        // 3. current_function
        val currentFunctionObj = JSONObject()
        currentFunctionObj.put("function_id", report.notation_and_id_resolution["functionId"] ?: "")
        currentFunctionObj.put("canonical_analysis_type", report.notation_and_id_resolution["canonicalAnalysisType"] ?: "")
        currentFunctionObj.put("original_analysis_type", report.notation_and_id_resolution["originalAnalysisType"] ?: "")
        root.put("current_function", currentFunctionObj)

        // 4. routing
        val routingObj = JSONObject()
        routingObj.put("feature_routing", sanitizeMap(report.feature_routing))
        routingObj.put("engine_routing", sanitizeMap(report.engine_routing))
        root.put("routing", routingObj)

        // 5. extraction
        val rawHtmlText = (report.html_extraction["rawHtmlFirst500SafeChars"] as? String) ?: ""
        val cleanedText = (report.content_cleaning["first1000CharsAfterCleaning"] as? String) ?: ""

        val htmlExtMap = report.html_extraction.toMutableMap()
        if (rawHtmlText.isNotEmpty()) {
            htmlExtMap["raw_html_metadata"] = makeTextMetadata(rawHtmlText, "text/html", "LOADED")
            htmlExtMap.remove("rawHtmlFirst500SafeChars")
        }

        val contentCleanMap = report.content_cleaning.toMutableMap()
        if (cleanedText.isNotEmpty()) {
            contentCleanMap["cleaned_text_metadata"] = makeTextMetadata(cleanedText, "text/plain", "CLEANED")
            contentCleanMap.remove("first1000CharsAfterCleaning")
        }

        val extractionObj = JSONObject()
        extractionObj.put("input_intake", sanitizeMap(report.input_intake))
        extractionObj.put("extractor_selection", sanitizeMap(report.extractor_selection))
        extractionObj.put("url_normalization", sanitizeMap(report.url_normalization))
        extractionObj.put("source_network_preflight", sanitizeMap(report.source_network_preflight))
        extractionObj.put("source_http_fetch", sanitizeMap(report.source_http_fetch))
        extractionObj.put("html_extraction", sanitizeMap(htmlExtMap))
        extractionObj.put("content_cleaning", sanitizeMap(contentCleanMap))
        root.put("extraction", extractionObj)

        // 6. enrichment
        val enrichmentObj = JSONObject()
        enrichmentObj.put("location_context", sanitizeMap(report.location_context))
        enrichmentObj.put("google_maps_analysis", sanitizeMap(report.google_maps_analysis))
        root.put("enrichment", enrichmentObj)

        // 7. gemini
        val promptText = (report.prompt_loading["promptFirst300Chars"] as? String) ?: ""
        val promptLoadMap = report.prompt_loading.toMutableMap()
        if (promptText.isNotEmpty()) {
            promptLoadMap["prompt_text_metadata"] = makeTextMetadata(promptText, "text/plain", "LOADED")
            promptLoadMap.remove("promptFirst300Chars")
        }

        val geminiResponseText = (report.gemini_response["rawGeminiFirst1000SafeChars"] as? String) ?: ""
        val geminiRespMap = report.gemini_response.toMutableMap()
        if (geminiResponseText.isNotEmpty()) {
            geminiRespMap["response_text_metadata"] = makeTextMetadata(geminiResponseText, "text/plain", "RECEIVED")
            geminiRespMap.remove("rawGeminiFirst1000SafeChars")
        }

        val geminiObj = JSONObject()
        geminiObj.put("prompt_loading", sanitizeMap(promptLoadMap))
        geminiObj.put("gemini_request", sanitizeMap(report.gemini_request))
        geminiObj.put("gemini_response", sanitizeMap(geminiRespMap))
        geminiObj.put("response_normalization", sanitizeMap(report.response_normalization))
        root.put("gemini", geminiObj)

        // 8. parsing
        root.put("parsing", sanitizeMap(report.parsing))

        // 9. contract
        root.put("contract", sanitizeMap(report.contract_validation))

        // 10. rendering
        root.put("rendering", sanitizeMap(report.rendering))

        // 11. fallbacks
        val fallbacksObj = JSONObject()
        val extractorFallback = report.extractor_selection["extractorFallbackUsed"] as? Boolean ?: false
        fallbacksObj.put("extractor_fallback_used", extractorFallback)

        val fallbackStepsArr = JSONArray()
        for (step in report.steps) {
            if (step.fallbackUsed) {
                fallbackStepsArr.put(step.stepId)
            }
        }
        fallbacksObj.put("fallback_steps", fallbackStepsArr)
        root.put("fallbacks", fallbacksObj)

        // 12. errors
        val errorsObj = JSONObject()
        errorsObj.put("final_status", report.final_result["finalStatus"] ?: "NOT_RUN")
        errorsObj.put("technical_status", report.final_result["technicalStatus"] ?: "NOT_RUN")
        errorsObj.put("functional_status", report.final_result["functionalStatus"] ?: "NOT_RUN")
        errorsObj.put("stability_status", report.final_result["stabilityStatus"] ?: "UNKNOWN")
        errorsObj.put("run_iteration", report.final_result["runIteration"] ?: 1)
        errorsObj.put("payload_input_hash", report.final_result["payloadInputHash"] ?: "")
        errorsObj.put("external_candidate_count", report.final_result["externalCandidateCount"] ?: 0)
        errorsObj.put("parser_strict_rejection_reason", report.final_result["parserStrictRejectionReason"] ?: "")
        errorsObj.put("semantic_outcome_reason", report.final_result["semanticOutcomeReason"] ?: "")
        errorsObj.put("failure_stage", report.final_result["failureStage"] ?: "")
        errorsObj.put("failure_step_id", report.final_result["failureStepId"] ?: "")
        errorsObj.put("user_visible_error_title", report.final_result["userVisibleErrorTitle"] ?: "")
        errorsObj.put("user_visible_error_message", report.final_result["userVisibleErrorMessage"] ?: "")
        errorsObj.put("technical_error_category", report.final_result["technicalErrorCategory"] ?: "")
        errorsObj.put("root_cause_candidate", report.final_result["rootCauseCandidate"] ?: "")
        root.put("errors", errorsObj)

        // 13. timeline
        fun buildStepsArray(maxSummaryLen: Int = -1): JSONArray {
            val arr = JSONArray()
            for (step in report.steps) {
                val stepObj = JSONObject()
                stepObj.put("stepId", step.stepId)
                stepObj.put("stepName", step.stepName)
                stepObj.put("status", step.status)
                stepObj.put("startedAt", step.startedAt)
                stepObj.put("endedAt", step.endedAt)
                stepObj.put("durationMs", step.durationMs)
                stepObj.put("inputSummary", if (maxSummaryLen > 0 && step.inputSummary.length > maxSummaryLen) step.inputSummary.take(maxSummaryLen) + "..." else step.inputSummary)
                stepObj.put("outputSummary", if (maxSummaryLen > 0 && step.outputSummary.length > maxSummaryLen) step.outputSummary.take(maxSummaryLen) + "..." else step.outputSummary)
                stepObj.put("decision", step.decision)
                stepObj.put("nextStep", step.nextStep)
                stepObj.put("exceptionClass", step.exceptionClass)
                stepObj.put("exceptionMessage", step.exceptionMessage)
                stepObj.put("fallbackUsed", step.fallbackUsed)
                stepObj.put("notes", if (maxSummaryLen > 0 && step.notes.length > maxSummaryLen) step.notes.take(maxSummaryLen) + "..." else step.notes)
                arr.put(stepObj)
            }
            return arr
        }

        val timelineArr = buildStepsArray()
        root.put("timeline", timelineArr)

        // 14. function_specific_context
        val funcCtxObj = JSONObject()
        for (contributor in contributors) {
            if (contributor.appliesTo(report)) {
                funcCtxObj.put(contributor.contributorId, sanitizeMap(contributor.contribute(report)))
            }
        }
        root.put("function_specific_context", funcCtxObj)

        // Legacy top-level mapping for backwards compatibility
        root.put("input_intake", sanitizeMap(report.input_intake))
        root.put("notation_and_id_resolution", sanitizeMap(report.notation_and_id_resolution))
        root.put("feature_routing", sanitizeMap(report.feature_routing))
        root.put("extractor_selection", sanitizeMap(report.extractor_selection))
        root.put("url_normalization", sanitizeMap(report.url_normalization))
        root.put("source_network_preflight", sanitizeMap(report.source_network_preflight))
        root.put("source_http_fetch", sanitizeMap(report.source_http_fetch))
        root.put("html_extraction", sanitizeMap(htmlExtMap))
        root.put("content_cleaning", sanitizeMap(contentCleanMap))
        root.put("engine_routing", sanitizeMap(report.engine_routing))
        root.put("prompt_loading", sanitizeMap(promptLoadMap))
        root.put("gemini_request", sanitizeMap(report.gemini_request))
        root.put("gemini_response", sanitizeMap(geminiRespMap))
        root.put("response_normalization", sanitizeMap(report.response_normalization))
        root.put("contract_validation", sanitizeMap(report.contract_validation))
        root.put("user_actions", sanitizeMap(report.user_actions))
        root.put("final_result", sanitizeMap(report.final_result))
        root.put("location_context", sanitizeMap(report.location_context))
        root.put("google_maps_analysis", sanitizeMap(report.google_maps_analysis))
        root.put("pipeline_steps", timelineArr)

        // 15. redaction_summary
        val redactionsObj = JSONObject()
        redactionsObj.put("redacted_keys_count", redactionsCount)
        val categoriesArr = JSONArray()
        categoriesArr.put("API_KEYS")
        categoriesArr.put("AUTHORIZATION")
        categoriesArr.put("TOKENS")
        categoriesArr.put("COOKIES")
        categoriesArr.put("SESSION_IDS")
        categoriesArr.put("SECRETS")
        redactionsObj.put("redacted_patterns", categoriesArr)
        root.put("redaction_summary", redactionsObj)

        // Initial size check & truncation
        var jsonString = root.toString(2)
        val initialSizeBytes = jsonString.toByteArray(Charsets.UTF_8).size
        val truncationActions = mutableListOf<String>()
        var isTruncated = false

        if (initialSizeBytes > MAX_REPORT_SIZE_BYTES) {
            isTruncated = true
            truncationActions.add("TRUNCATED_STEP_SUMMARIES")
            var truncatedSteps = buildStepsArray(maxSummaryLen = 50)
            root.put("timeline", truncatedSteps)
            root.put("pipeline_steps", truncatedSteps)
            jsonString = root.toString(2)

            if (jsonString.toByteArray(Charsets.UTF_8).size > MAX_REPORT_SIZE_BYTES) {
                truncationActions.add("TRUNCATED_STEP_SUMMARIES_AGGRESSIVE")
                truncatedSteps = buildStepsArray(maxSummaryLen = 20)
                root.put("timeline", truncatedSteps)
                root.put("pipeline_steps", truncatedSteps)
                jsonString = root.toString(2)
            }

            if (jsonString.toByteArray(Charsets.UTF_8).size > MAX_REPORT_SIZE_BYTES && report.steps.size > 20) {
                truncationActions.add("TRUNCATED_TIMELINE_STEPS_COUNT")
                val keptSteps = report.steps.take(10) + report.steps.takeLast(10)
                val cappedArr = JSONArray()
                for (step in keptSteps) {
                    val stepObj = JSONObject()
                    stepObj.put("stepId", step.stepId)
                    stepObj.put("stepName", step.stepName)
                    stepObj.put("status", step.status)
                    stepObj.put("startedAt", step.startedAt)
                    stepObj.put("endedAt", step.endedAt)
                    stepObj.put("durationMs", step.durationMs)
                    stepObj.put("inputSummary", if (step.inputSummary.length > 20) step.inputSummary.take(20) + "..." else step.inputSummary)
                    stepObj.put("outputSummary", if (step.outputSummary.length > 20) step.outputSummary.take(20) + "..." else step.outputSummary)
                    stepObj.put("decision", step.decision)
                    stepObj.put("nextStep", step.nextStep)
                    stepObj.put("exceptionClass", step.exceptionClass)
                    stepObj.put("exceptionMessage", step.exceptionMessage)
                    stepObj.put("fallbackUsed", step.fallbackUsed)
                    stepObj.put("notes", if (step.notes.length > 20) step.notes.take(20) + "..." else step.notes)
                    cappedArr.put(stepObj)
                }
                root.put("timeline", cappedArr)
                root.put("pipeline_steps", cappedArr)
                jsonString = root.toString(2)
            }
        }

        // 16. truncation_summary
        val truncationObj = JSONObject()
        truncationObj.put("original_size_bytes", initialSizeBytes)
        truncationObj.put("final_size_bytes", 0)
        truncationObj.put("truncated", isTruncated)
        val actionsArr = JSONArray()
        for (action in truncationActions) {
            actionsArr.put(action)
        }
        truncationObj.put("truncation_actions", actionsArr)
        root.put("truncation_summary", truncationObj)

        jsonString = root.toString(2)
        val finalSizeBytes = jsonString.toByteArray(Charsets.UTF_8).size
        truncationObj.put("final_size_bytes", finalSizeBytes)
        root.put("truncation_summary", truncationObj)

        return root.toString(2)
    }
}
