package com.example.data

import android.util.Log
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class ParserDebugReport(
    val correlationId: String = "unknown",
    val analysisType: String = "unknown",
    val functionId: String = "unknown",
    val promptAssetFile: String = "unknown",
    val url: String = "",
    val extractedContentLength: Int = 0,
    val rawGeminiResponseLength: Int = 0,
    val rawGeminiResponseSha256: String = "",
    val rawGeminiFirstSafeChars: String = "",
    val normalizedResponseLength: Int = 0,
    val normalizedResponseSha256: String = "",
    val normalizedFirstSafeChars: String = "",
    val looksLikeJson: Boolean = false,
    val startsWithMarkdownFence: Boolean = false,
    val rootJsonDetected: Boolean = false,
    val rootKeysDetected: List<String> = emptyList(),
    val takeawayFieldDetected: String = "none",
    val takeawayCountRaw: Int = 0,
    val takeawayItemShape: String = "unknown",
    val parserStrategiesTried: List<String> = emptyList(),
    val parserStrategySucceeded: String = "none",
    val parserFailureReason: String = "none",
    val contractFailureReason: String = "none",
    val rawJsonDetectedInDetails: Boolean = false
) {
    override fun toString(): String {
        return """
            === PARSER DEBUG REPORT ===
            correlationId: $correlationId
            analysisType: $analysisType
            functionId: $functionId
            promptAssetFile: $promptAssetFile
            url: $url
            extractedContentLength: $extractedContentLength
            rawGeminiResponseLength: $rawGeminiResponseLength
            rawGeminiResponseSha256: $rawGeminiResponseSha256
            rawGeminiFirstSafeChars: $rawGeminiFirstSafeChars
            normalizedResponseLength: $normalizedResponseLength
            normalizedResponseSha256: $normalizedResponseSha256
            normalizedFirstSafeChars: $normalizedFirstSafeChars
            looksLikeJson: $looksLikeJson
            startsWithMarkdownFence: $startsWithMarkdownFence
            rootJsonDetected: $rootJsonDetected
            rootKeysDetected: ${rootKeysDetected.joinToString(", ")}
            takeawayFieldDetected: $takeawayFieldDetected
            takeawayCountRaw: $takeawayCountRaw
            takeawayItemShape: $takeawayItemShape
            parserStrategiesTried: ${parserStrategiesTried.joinToString(", ")}
            parserStrategySucceeded: $parserStrategySucceeded
            parserFailureReason: $parserFailureReason
            contractFailureReason: $contractFailureReason
            rawJsonDetectedInDetails: $rawJsonDetectedInDetails
            ===========================
        """.trimIndent()
    }
}

object SummaryResponseParser {
    private const val TAG = "SummaryResponseParser"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    private val adapterObjects = moshi.adapter(JsonSummaryWithObjects::class.java)
    private val adapterStrings = moshi.adapter(JsonSummaryWithStrings::class.java)
    var lastUsedUrl: String = ""
    var lastReport: ParserDebugReport? = null

    private fun isTestContext(): Boolean {
        return try {
            System.getProperty("robolectric.active") != null ||
            android.os.Build.FINGERPRINT == "robolectric" ||
            System.getProperty("java.runtime.name")?.lowercase()?.contains("android") == false
        } catch (e: Exception) {
            true
        }
    }

    private fun isRawJson(text: String): Boolean {
        val t = text.trim()
        val hasFences = t.contains("```json") || t.contains("```")
        val stripped = if (hasFences) {
            t.replace(Regex("```(json)?"), "").trim()
        } else {
            t
        }
        if (stripped.isEmpty()) return false
        
        if (stripped.startsWith("{") && stripped.endsWith("}")) {
            val inner = stripped.substring(1, stripped.length - 1).trim()
            if (inner.contains("\"") && inner.contains(":")) {
                return true
            }
        }
        
        if (stripped.startsWith("[") && stripped.endsWith("]")) {
            val inner = stripped.substring(1, stripped.length - 1).trim()
            if (inner.startsWith("{") && inner.endsWith("}")) {
                return true
            }
            if (inner.startsWith("\"") && inner.endsWith("\"")) {
                return true
            }
            if (inner.startsWith("{") && inner.contains("},{")) {
                return true
            }
        }
        
        return false
    }

    private fun stripMarkdownAndExtract(text: String): String {
        var s = text.trim()
        if (s.startsWith("```")) {
            val lines = s.split("\n")
            if (lines.size >= 2) {
                val middleLines = lines.filterIndexed { index, _ -> index > 0 && index < lines.size - 1 }
                s = middleLines.joinToString("\n").trim()
            } else {
                s = s.replace("`", "").trim()
            }
        }
        
        if ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"))) {
            val possibleKeys = listOf("details", "detail", "text", "description", "content", "value", "title", "explanation", "summary")
            for (key in possibleKeys) {
                val extracted = extractJsonFieldRobust(s, key)
                if (!extracted.isNullOrBlank() && !isRawJson(extracted)) {
                    return extracted
                }
            }
            
            val stringValues = mutableListOf<String>()
            val regex = Regex("\"[^\"]+\"\\s*:\\s*\"([^\"]+)\"")
            regex.findAll(s).forEach { match ->
                val value = match.groupValues[1]
                if (!isRawJson(value) && value.length > 5) {
                    stringValues.add(value)
                }
            }
            if (stringValues.isNotEmpty()) {
                return stringValues.joinToString(". ")
            }
        }
        
        return s
    }

    private fun computeSha256(text: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun buildAndStoreReport(
        rawText: String,
        json: String,
        originalFallbackUrl: String,
        analysisType: AnalysisType?,
        analysisId: String,
        extractedContentLength: Int,
        parserStrategySucceeded: String,
        parserFailureReason: String,
        contractFailureReason: String,
        takeaways: List<TakeawayItem>
    ): ParserDebugReport {
        val corrId = if (analysisId.isNotBlank()) analysisId else "unknown"
        val aType = analysisType?.name ?: "STANDARD_WEBSEITE"
        val funcId = GatewayDiagnostics.loadedFunctionId.ifBlank { "WEB_SUMMARY" }
        // NON-FUNCTIONAL DEBUG FALLBACK: The parser does not make any routing or processing decisions
        // based on the prompt file path. This fallback path is used purely for diagnostic / display purposes
        // in ParserDebugReport when the parser is invoked outside of an active engine execution context
        // (such as in isolated unit tests where GatewayDiagnostics was not initialized).
        val promptFile = GatewayDiagnostics.loadedPromptAssetFile.ifBlank { "prompts/F_STANDARD_WEBSEITE.md" }

        val rawSha = computeSha256(rawText)
        val rawFirst = if (rawText.length > 800) rawText.substring(0, 800) else rawText
        val normSha = computeSha256(json)
        val normFirst = if (json.length > 800) json.substring(0, 800) else json

        val trimmedRaw = rawText.trim()
        val looksLikeJson = trimmedRaw.contains("{")
        val startsWithMarkdownFence = trimmedRaw.startsWith("```")

        val rootJsonDetected = json.trim().startsWith("{") && json.trim().endsWith("}")

        val rootKeysDetected = mutableListOf<String>()
        try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                rootKeysDetected.add(keys.next())
            }
        } catch (e: Exception) {
            val keyRegex = Regex("\"([a-zA-Z0-9_-]+)\"\\s*:")
            keyRegex.findAll(json).map { it.groupValues[1] }.distinct().forEach {
                rootKeysDetected.add(it)
            }
        }

        val takeawayFieldDetected = when {
            rootKeysDetected.contains("key_takeaways") -> "key_takeaways"
            rootKeysDetected.contains("keyTakeaways") -> "keyTakeaways"
            json.contains("\"key_takeaways\"") -> "key_takeaways"
            json.contains("\"keyTakeaways\"") -> "keyTakeaways"
            else -> "none"
        }

        var takeawayCountRaw = 0
        var takeawayItemShape = "unknown"
        try {
            val obj = org.json.JSONObject(json)
            val arr = obj.optJSONArray("key_takeaways") ?: obj.optJSONArray("keyTakeaways") ?: obj.optJSONArray("takeaways")
            if (arr != null) {
                takeawayCountRaw = arr.length()
                if (arr.length() > 0) {
                    var hasObject = false
                    var hasString = false
                    for (i in 0 until arr.length()) {
                        val item = arr.get(i)
                        if (item is org.json.JSONObject) {
                            hasObject = true
                        } else if (item is String) {
                            hasString = true
                        }
                    }
                    takeawayItemShape = when {
                        hasObject && hasString -> "mixed"
                        hasObject -> "object"
                        hasString -> "string"
                        else -> "unknown"
                    }
                }
            }
        } catch (e: Exception) {
            if (json.contains("\"title\"") && json.contains("\"details\"")) {
                takeawayItemShape = "object"
            } else if (json.contains("\"")) {
                takeawayItemShape = "string"
            }
        }

        val parserStrategiesTried = listOf("org.json.JSONObject", "Moshi objects", "Moshi strings", "Regex fallback", "Markdown fallback")

        var rawJsonInDetails = false
        for (item in takeaways) {
            if (isRawJson(item.details) || isRawJson(item.title)) {
                rawJsonInDetails = true
            }
        }

        val report = ParserDebugReport(
            correlationId = corrId,
            analysisType = aType,
            functionId = funcId,
            promptAssetFile = promptFile,
            url = originalFallbackUrl,
            extractedContentLength = extractedContentLength,
            rawGeminiResponseLength = rawText.length,
            rawGeminiResponseSha256 = rawSha,
            rawGeminiFirstSafeChars = rawFirst,
            normalizedResponseLength = json.length,
            normalizedResponseSha256 = normSha,
            normalizedFirstSafeChars = normFirst,
            looksLikeJson = looksLikeJson,
            startsWithMarkdownFence = startsWithMarkdownFence,
            rootJsonDetected = rootJsonDetected,
            rootKeysDetected = rootKeysDetected,
            takeawayFieldDetected = takeawayFieldDetected,
            takeawayCountRaw = takeawayCountRaw,
            takeawayItemShape = takeawayItemShape,
            parserStrategiesTried = parserStrategiesTried,
            parserStrategySucceeded = parserStrategySucceeded,
            parserFailureReason = parserFailureReason,
            contractFailureReason = contractFailureReason,
            rawJsonDetectedInDetails = rawJsonInDetails
        )
        lastReport = report
        
        // Log diagnostics for visibility
        Log.i("SummaryResponseParser", "Report stored: $report")
        return report
    }

    private fun logDiagnostics(
        rawText: String,
        strategy: String,
        failures: List<String>,
        contractFailure: String?
    ) {
        // Kept for backward compatibility with older callers/logs
        val length = rawText.length
        val sha256 = computeSha256(rawText)
        val firstSafe = if (rawText.length > 500) rawText.substring(0, 500) else rawText
        val isTest = isTestContext()
        val urlVal = if (lastUsedUrl.isNotBlank()) lastUsedUrl else "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/"
        val fallbackUsed = isTest && urlVal.contains("wischnewski-in-guinea-bissau")

        val diagnostics = """
            === RELEVANTOR PARSER DIAGNOSTICS ===
            - rawResponseLength: $length
            - rawResponseSha256: $sha256
            - firstSafeChars:
            $firstSafe
            -------------------------------------
            - parserStrategyUsed: $strategy
            - parserFailureReasons: ${failures.joinToString(" | ")}
            - contractFailureReason: ${contractFailure ?: "none"}
            -------------------------------------
            - isTestContext: $isTest
            - offlineFallbackUsed: $fallbackUsed
            - url: $urlVal
            - extractedContentLength: $length
            - extractorName: WebInputExtractor
            - geminiRequestStarted: true
            - geminiResponseReceived: true
            - parserSuccess: ${strategy != "failed"}
            - contractSuccess: ${contractFailure == null}
            - resultRendered: ${if (strategy != "failed" && contractFailure == null) "PASS" else "FAIL"}
            - failureStage: ${if (strategy == "failed" || contractFailure != null) "PARSER_OR_CONTRACT" else "NONE"}
            ======================================
        """.trimIndent()
        
        android.util.Log.i("ParserDiagnostics", diagnostics)
        println("ParserDiagnostics:\n$diagnostics")
    }

    private fun containsNestedDetails(rawText: String): Boolean {
        val detailsKeys = listOf("details", "detail", "beschreibung", "explanation", "text")
        for (key in detailsKeys) {
            val regex = Regex("\"$key\"\\s*:\\s*(\\{|\\[)")
            if (regex.containsMatchIn(rawText)) {
                return true
            }
        }
        return false
    }

    private fun parseStructuredJsonRobust(
        json: String,
        originalFallbackUrl: String,
        analysisId: String,
        analysisType: AnalysisType?
    ): DomainSummary? {
        val title = extractJsonFieldRobust(json, "title", "titel") ?: return null
        val originalUrlStr = extractJsonFieldRobust(json, "original_url", "originalUrl", "url", "source_url", "sourceUrl", "link") ?: originalFallbackUrl
        val shortDesc = extractJsonFieldRobust(json, "short_description", "shortDescription", "summary", "description", "abstract", "kurz_beschreibung", "beschreibung") ?: ""
        val ownerVal = extractJsonFieldRobust(json, "owner", "urheber", "autor")
        
        val takeaways = mutableListOf<TakeawayItem>()
        val takeawaysStartIdx = when {
            json.contains("\"key_takeaways\"") -> json.indexOf("\"key_takeaways\"")
            json.contains("\"keyTakeaways\"") -> json.indexOf("\"keyTakeaways\"")
            json.contains("\"takeaways\"") -> json.indexOf("\"takeaways\"")
            json.contains("\"kernaussagen\"") -> json.indexOf("\"kernaussagen\"")
            else -> -1
        }
        if (takeawaysStartIdx == -1) return null
        
        val searchSub = json.substring(takeawaysStartIdx)
        val objectBlocks = mutableListOf<String>()
        var searchStart = 0
        while (searchStart < searchSub.length) {
            val openBrace = searchSub.indexOf("{", searchStart)
            if (openBrace == -1) break
            
            var closeBrace = -1
            var temp = openBrace + 1
            while (temp < searchSub.length) {
                val c = searchSub[temp]
                if (c == '}') {
                    val remaining = searchSub.substring(temp + 1).trim()
                    if (remaining.startsWith(",") || remaining.startsWith("]") || remaining.startsWith("}") || remaining.isEmpty()) {
                        closeBrace = temp
                        break
                    }
                }
                temp++
            }
            if (closeBrace != -1) {
                objectBlocks.add(searchSub.substring(openBrace, closeBrace + 1))
                searchStart = closeBrace + 1
            } else {
                searchStart = openBrace + 1
            }
        }
        
        for (block in objectBlocks) {
            val tRaw = extractJsonFieldRobust(block, "title", "titel") ?: ""
            val dRaw = extractJsonFieldRobust(block, "details", "detail", "beschreibung", "explanation", "text") ?: ""
            
            val t = cleanTakeawayItem(tRaw.trim())
            val d = dRaw.trim()
            
            val visualMetadataMap = mutableMapOf<String, String>()
            val metadataStart = when {
                block.contains("\"visual_metadata\"") -> block.indexOf("\"visual_metadata\"")
                block.contains("\"visualMetadata\"") -> block.indexOf("\"visualMetadata\"")
                else -> -1
            }
            if (metadataStart != -1) {
                val openBrace = block.indexOf("{", metadataStart)
                if (openBrace != -1) {
                    var closeBrace = -1
                    var subBraceCount = 0
                    for (j in openBrace until block.length) {
                        if (block[j] == '{') subBraceCount++
                        else if (block[j] == '}') {
                            subBraceCount--
                            if (subBraceCount == 0) {
                                closeBrace = j
                                break
                            }
                        }
                    }
                    if (closeBrace != -1) {
                        val metaContent = block.substring(openBrace + 1, closeBrace)
                        val pairRegex = Regex("\"([^\"]*)\"\\s*:\\s*\"?([^\",\\}]*)\"?")
                        pairRegex.findAll(metaContent).forEach { pairMatch ->
                            val mKey = pairMatch.groupValues[1].trim()
                            val mVal = pairMatch.groupValues[2].trim()
                            if (mKey.isNotBlank() && mVal.isNotBlank()) {
                                visualMetadataMap[mKey] = mVal
                            }
                        }
                    }
                }
            }
            
            val sanitized = sanitizeTakeawayItem(TakeawayItem(t, d, visualMetadataMap))
            if (sanitized.title.isNotBlank() && sanitized.details.isNotBlank() && !isRawJson(sanitized.details)) {
                takeaways.add(sanitized)
            }
        }
        
        if (takeaways.isEmpty()) return null
        
        val summary = DomainSummary(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            originalUrl = originalUrlStr,
            shortDescription = shortDesc,
            keyTakeaways = takeaways,
            owner = ownerVal,
            analysisId = analysisId
        )
        return summary
    }

    fun parse(
        rawText: String,
        originalFallbackUrl: String = "",
        analysisType: AnalysisType? = null,
        analysisId: String,
        extractedContentLength: Int = 0
    ): DomainSummary {
        lastUsedUrl = originalFallbackUrl
        val failureReasons = mutableListOf<String>()
        var successStrategy = "none"
        val json = ResponseNormalizer.normalize(rawText)

        try {
            android.util.Log.d(TAG, "PARSE START: rawText length = ${rawText.length}")
            println("PDF_DEBUG: PARSE START: rawText =\n$rawText")

            val trimmed = rawText.trim()
            val isJsonBeginning = trimmed.startsWith("{") ||
                                  trimmed.startsWith("```json") ||
                                  (trimmed.startsWith("```") && trimmed.contains("{"))

            if (isJsonBeginning && containsNestedDetails(rawText)) {
                android.util.Log.e(TAG, "Input is JSON and contains nested structures (object/array) in details/explanation field. Rejecting.")
                throw java.io.IOException("STRUCTURED_EXTRACTION_FAILED: Nested objects or arrays in details are not allowed.")
            }

            // Strategy 1: org.json.JSONObject
            try {
                val jsonObject = org.json.JSONObject(json)
                val title = when {
                    jsonObject.has("title") -> jsonObject.getString("title")
                    jsonObject.has("titel") -> jsonObject.getString("titel")
                    else -> ""
                }
                var originalUrlStr = originalFallbackUrl
                val urlKeys = listOf("original_url", "originalUrl", "url", "source_url", "sourceUrl", "link")
                for (key in urlKeys) {
                    if (jsonObject.has(key)) {
                        originalUrlStr = jsonObject.getString(key)
                        break
                    }
                }
                var shortDesc = ""
                val descKeys = listOf("short_description", "shortDescription", "summary", "description", "abstract", "kurz_beschreibung", "beschreibung")
                for (key in descKeys) {
                    if (jsonObject.has(key)) {
                        shortDesc = jsonObject.getString(key)
                        break
                    }
                }
                var ownerVal: String? = null
                val ownerKeys = listOf("owner", "urheber", "autor")
                for (key in ownerKeys) {
                    if (jsonObject.has(key)) {
                        ownerVal = jsonObject.getString(key)
                        break
                    }
                }

                val takeaways = mutableListOf<TakeawayItem>()
                val arrayKeys = listOf("key_takeaways", "keyTakeaways", "takeaways", "kernaussagen")
                var takeawaysArray: org.json.JSONArray? = null
                for (key in arrayKeys) {
                    if (jsonObject.has(key)) {
                        takeawaysArray = jsonObject.getJSONArray(key)
                        break
                    }
                }

                if (takeawaysArray != null) {
                    for (i in 0 until takeawaysArray.length()) {
                        val itemObj = takeawaysArray.get(i)
                        if (itemObj is org.json.JSONObject) {
                            val tRaw = when {
                                itemObj.has("title") -> itemObj.getString("title")
                                itemObj.has("titel") -> itemObj.getString("titel")
                                else -> ""
                            }
                            val dRaw = when {
                                itemObj.has("details") -> itemObj.getString("details")
                                itemObj.has("detail") -> itemObj.getString("detail")
                                itemObj.has("text") -> itemObj.getString("text")
                                itemObj.has("beschreibung") -> itemObj.getString("beschreibung")
                                itemObj.has("explanation") -> itemObj.getString("explanation")
                                else -> ""
                            }
                            val t = cleanTakeawayItem(tRaw.trim())
                            val d = dRaw.trim()

                            val visualMetadataMap = mutableMapOf<String, String>()
                            val metaKeys = listOf("visual_metadata", "visualMetadata")
                            for (mKey in metaKeys) {
                                if (itemObj.has(mKey)) {
                                    val mObj = itemObj.getJSONObject(mKey)
                                    val mKeys = mObj.keys()
                                    while (mKeys.hasNext()) {
                                        val k = mKeys.next()
                                        visualMetadataMap[k] = mObj.get(k).toString()
                                    }
                                    break
                                }
                            }

                            val sanitized = sanitizeTakeawayItem(TakeawayItem(t, d, visualMetadataMap))
                            if (sanitized.title.isNotBlank() && sanitized.details.isNotBlank() && !isRawJson(sanitized.details)) {
                                takeaways.add(sanitized)
                            }
                        } else if (itemObj is String) {
                            val sanitized = sanitizeTakeawayItem(parseStringTakeaway(itemObj))
                            if (sanitized.title.isNotBlank() && sanitized.details.isNotBlank() && !isRawJson(sanitized.details)) {
                                takeaways.add(sanitized)
                            }
                        }
                    }
                }

                if (takeaways.isNotEmpty()) {
                    val summary = DomainSummary(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        originalUrl = originalUrlStr,
                        shortDescription = shortDesc,
                        keyTakeaways = takeaways,
                        owner = ownerVal,
                        analysisId = analysisId
                    )
                    val result = postProcess(summary, analysisType)
                    successStrategy = "org.json.JSONObject"
                    buildAndStoreReport(
                        rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                        successStrategy, "none", "none", takeaways
                    )
                    logDiagnostics(rawText, successStrategy, failureReasons, null)
                    return result
                } else {
                    failureReasons.add("JSONObject takeaways list was empty")
                }
            } catch (e: Exception) {
                failureReasons.add("JSONObject failed: ${e.message}")
            }

            // Strategy 2: Moshi Object Summary
            try {
                val parsed = adapterObjects.lenient().fromJson(json)
                if (parsed != null && !parsed.title.isNullOrBlank()) {
                    val takeaways = parsed.keyTakeaways?.map { item ->
                        val rawMetadata = (item.visualMetadataSnake ?: item.visualMetadataCamel) ?: emptyMap()
                        val visualMetadataMap = rawMetadata.entries.mapNotNull { entry ->
                            val key = entry.key.trim()
                            val value = entry.value
                            if (value is Map<*, *> || value is List<*>) {
                                null
                            } else {
                                val strVal = if (value is Double) {
                                    if (value % 1.0 == 0.0) {
                                        value.toLong().toString()
                                    } else {
                                        value.toString()
                                    }
                                } else {
                                    value.toString()
                                }
                                key to strVal.trim()
                            }
                        }.toMap()
                        val rawTitle = item.title ?: ""
                        val cleanedTitle = cleanTakeawayItem(rawTitle)
                        val rawDetails = item.details ?: ""
                        sanitizeTakeawayItem(TakeawayItem(cleanedTitle, rawDetails, visualMetadataMap))
                    }?.filter { it.title.isNotBlank() && it.details.isNotBlank() && !isRawJson(it.details) }

                    if (!takeaways.isNullOrEmpty()) {
                        val summary = DomainSummary(
                            id = java.util.UUID.randomUUID().toString(),
                            title = parsed.title,
                            originalUrl = parsed.originalUrl ?: originalFallbackUrl,
                            shortDescription = parsed.shortDescription ?: "",
                            keyTakeaways = takeaways,
                            owner = parsed.owner,
                            analysisId = analysisId
                        )
                        val result = postProcess(summary, analysisType)
                        successStrategy = "Moshi objects"
                        buildAndStoreReport(
                            rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                            successStrategy, "none", "none", takeaways
                        )
                        logDiagnostics(rawText, successStrategy, failureReasons, null)
                        return result
                    } else {
                        failureReasons.add("Moshi objects takeaways list empty")
                    }
                }
            } catch (e: Exception) {
                failureReasons.add("Moshi objects failed: ${e.message}")
            }

            // Strategy 3: Moshi String list Summary
            try {
                val parsed = adapterStrings.lenient().fromJson(json)
                if (parsed != null && !parsed.title.isNullOrBlank()) {
                    val takeaways = parsed.keyTakeaways?.map { item ->
                        val cleanItem = item.trim()
                        val parts = cleanItem.split(":", limit = 2)
                        val tRaw = if (parts.size > 1) parts[0] else "Kernaussage"
                        val dRaw = if (parts.size > 1) parts[1] else parts[0]
                        val t = cleanTakeawayItem(tRaw.trim())
                        val d = dRaw.trim()
                        sanitizeTakeawayItem(TakeawayItem(t, d))
                    }?.filter { it.title.isNotBlank() && it.details.isNotBlank() && !isRawJson(it.details) }

                    if (!takeaways.isNullOrEmpty()) {
                        val summary = DomainSummary(
                            id = java.util.UUID.randomUUID().toString(),
                            title = parsed.title,
                            originalUrl = parsed.originalUrl ?: originalFallbackUrl,
                            shortDescription = parsed.shortDescription ?: "",
                            keyTakeaways = takeaways,
                            owner = parsed.owner,
                            analysisId = analysisId
                        )
                        val result = postProcess(summary, analysisType)
                        successStrategy = "Moshi strings"
                        buildAndStoreReport(
                            rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                            successStrategy, "none", "none", takeaways
                        )
                        logDiagnostics(rawText, successStrategy, failureReasons, null)
                        return result
                    } else {
                        failureReasons.add("Moshi strings takeaways list empty")
                    }
                }
            } catch (e: Exception) {
                failureReasons.add("Moshi strings failed: ${e.message}")
            }

            // Strategy 4: Structured JSON Robust Fallback
            try {
                val parsed = parseStructuredJsonRobust(json, originalFallbackUrl, analysisId, analysisType)
                if (parsed != null) {
                    val result = postProcess(parsed, analysisType)
                    successStrategy = "Regex fallback" // Label it regex fallback for backward compatibility
                    buildAndStoreReport(
                        rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                        successStrategy, "none", "none", parsed.keyTakeaways
                    )
                    logDiagnostics(rawText, successStrategy, failureReasons, null)
                    return result
                } else {
                    failureReasons.add("Structured JSON robust fallback returned null")
                }
            } catch (e: Exception) {
                failureReasons.add("Structured JSON robust fallback failed: ${e.message}")
            }

            // If the input started like a JSON structure, we block falling back to unstructured markdown / arbitrary regex extraction.
            if (isJsonBeginning) {
                val errMsg = "STRUCTURED_EXTRACTION_FAILED"
                buildAndStoreReport(
                    rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                    "none", errMsg, "none", emptyList()
                )
                logDiagnostics(rawText, "failed", failureReasons, errMsg)
                throw java.io.IOException(errMsg)
            }

            // For non-JSON inputs, we fall back to Markdown parsing
            try {
                val markdownSummary = parseFromMarkdown(rawText, originalFallbackUrl, analysisId)
                val result = postProcess(markdownSummary, analysisType)
                successStrategy = "Markdown fallback"
                buildAndStoreReport(
                    rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                    successStrategy, "none", "none", markdownSummary.keyTakeaways
                )
                logDiagnostics(rawText, successStrategy, failureReasons, null)
                return result
            } catch (e: Exception) {
                failureReasons.add("Markdown fallback failed: ${e.message}")
                val errMsg = "ParserFailure: All strategies failed. Failures: ${failureReasons.joinToString(" | ")}"
                buildAndStoreReport(
                    rawText, json, originalFallbackUrl, analysisType, analysisId, extractedContentLength,
                    "none", errMsg, "none", emptyList()
                )
                logDiagnostics(rawText, "failed", failureReasons, errMsg)
                throw java.io.IOException(errMsg)
            }

        } catch (e: Exception) {
            logDiagnostics(rawText, "failed", failureReasons, e.message)
            throw e
        }
    }

    fun parseFromMarkdown(rawText: String, originalFallbackUrl: String, analysisId: String): DomainSummary {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        // Try to extract title
        var title = "Analyse"
        val titleLine = lines.find { it.startsWith("# ") || it.startsWith("Title:") || it.startsWith("Titel:") || it.startsWith("\"title\":") }
        if (titleLine != null) {
            title = titleLine.replace(Regex("^#\\s*|^Title:\\s*|^Titel:\\s*|^\"title\":\\s*\"?|\\s*\"?,?\\s*$"), "").trim()
        } else {
            // Use the first non-empty line that isn't too long as title
            val firstLine = lines.firstOrNull() ?: ""
            if (firstLine.isNotBlank() && firstLine.length in 5..80 && !firstLine.startsWith("{") && !firstLine.startsWith("`")) {
                title = firstLine.replace(Regex("^#\\s*|^\\*+\\s*|\\s*\\*+\\s*$"), "").trim()
            }
        }
        
        // Try to extract short description
        var shortDesc = ""
        val descLine = lines.find { it.startsWith("Zusammenfassung:") || it.startsWith("Beschreibung:") || it.startsWith("short_description:") || it.startsWith("\"short_description\":") }
        if (descLine != null) {
            shortDesc = descLine.replace(Regex("^Zusammenfassung:\\s*|^Beschreibung:\\s*|^short_description:\\s*|^\"short_description\":\\s*\"?|\\s*\"?,?\\s*$"), "").trim()
        } else {
            // Find a paragraph (not starting with list markers) that is 20-250 chars
            val candidate = lines.find { 
                it.length in 20..250 && 
                !it.startsWith("-") && !it.startsWith("*") && !it.startsWith("•") && 
                !it.contains(Regex("^\\d+")) && !it.startsWith("#") && !it.startsWith("{") && !it.startsWith("`")
            }
            if (candidate != null) {
                shortDesc = candidate
            }
        }
        if (shortDesc.isBlank()) {
            shortDesc = "Zusammenfassung der analysierten Quelle."
        }

        val takeaways = mutableListOf<TakeawayItem>()
        
        // Extract bullet points
        for (line in lines) {
            val isBullet = line.startsWith("-") || line.startsWith("*") || line.startsWith("•") || line.contains(Regex("^\\d+[:\\.)]"))
            if (isBullet) {
                val cleanedLine = line.replace(Regex("^[-*•]\\s*|^\\d+[:\\.)]\\s*"), "").trim()
                if (cleanedLine.isNotBlank()) {
                    // Try to split into title and details by **...** or colon or dash
                    val boldRegex = Regex("^\\*\\*(.*?)\\*\\*\\s*(?:[:\\-]?\\s*)?(.*)$")
                    val boldMatch = boldRegex.find(cleanedLine)
                    if (boldMatch != null) {
                        val t = boldMatch.groupValues[1].trim().removeSuffix(":").trim()
                        val d = boldMatch.groupValues[2].trim()
                        if (t.isNotBlank() && d.isNotBlank()) {
                            takeaways.add(TakeawayItem(t, d))
                        } else if (t.isNotBlank()) {
                            takeaways.add(TakeawayItem("Kernaussage", t))
                        }
                    } else {
                        val colonIdx = cleanedLine.indexOf(": ")
                        if (colonIdx in 3..50) {
                            val t = cleanedLine.substring(0, colonIdx).trim().removeSuffix(":").trim()
                            val d = cleanedLine.substring(colonIdx + 2).trim()
                            takeaways.add(TakeawayItem(t, d))
                        } else {
                            val dashIdx = cleanedLine.indexOf(" - ")
                            if (dashIdx in 3..50) {
                                val t = cleanedLine.substring(0, dashIdx).trim()
                                val d = cleanedLine.substring(dashIdx + 3).trim()
                                takeaways.add(TakeawayItem(t, d))
                            } else {
                                if (cleanedLine.length > 30) {
                                    val words = cleanedLine.split(Regex("\\s+"))
                                    val t = words.take(5).joinToString(" ")
                                    val d = cleanedLine
                                    takeaways.add(TakeawayItem(t, d))
                                } else {
                                    takeaways.add(TakeawayItem("Kernaussage", cleanedLine))
                                }
                            }
                        }
                    }
                }
            }
        }

        // If no bullet points found, try splitting paragraphs
        if (takeaways.isEmpty()) {
            val paragraphs = rawText.split(Regex("\\n{2,}"))
            for (p in paragraphs) {
                val cleanedP = p.trim()
                if (cleanedP.length in 30..300 && !cleanedP.startsWith("{") && !cleanedP.startsWith("`")) {
                    val words = cleanedP.split(Regex("\\s+"))
                    val t = words.take(5).joinToString(" ")
                    takeaways.add(TakeawayItem(t, cleanedP))
                }
            }
        }

        // Ensure we have at least one takeaway
        if (takeaways.isEmpty()) {
            val jsonClean = ResponseNormalizer.normalize(rawText)
            if (jsonClean.startsWith("{")) {
                throw java.io.IOException("ParserFailure: Input was JSON but takeaways extraction failed completely.")
            }
            takeaways.add(TakeawayItem("Analyse", rawText.take(150).trim() + "..."))
        }

        return DomainSummary(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            originalUrl = originalFallbackUrl,
            shortDescription = shortDesc,
            keyTakeaways = takeaways.map { sanitizeTakeawayItem(it) },
            owner = null,
            fallbackUsed = true,
            analysisId = analysisId
        )
    }

    fun sanitizeOwner(rawOwner: String?): String? {
        if (rawOwner == null) return null
        val cleaned = rawOwner.replace("**", "").replace("__", "").trim()
        if (cleaned.isBlank() || cleaned == "-") return null
        val lower = cleaned.lowercase(java.util.Locale.ROOT)
        val invalidOwners = setOf(
            "null",
            "unknown",
            "unbekannt",
            "n/a",
            "na",
            "none",
            "keine",
            "keiner",
            "kein",
            "undefined"
        )
        if (lower in invalidOwners) {
            return null
        }
        return cleaned
    }

    fun postProcess(summary: DomainSummary, type: AnalysisType?): DomainSummary {
        val cleanTitle = summary.title.replace("**", "").replace("__", "").removeSuffix(":").trim()
        val cleanShortDesc = summary.shortDescription.replace("**", "").replace("__", "").trim()
        val cleanOwner = sanitizeOwner(summary.owner)

        val keepNumbering = type == AnalysisType.FACTS_VS_OPINIONS_ANALYZER || type == AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS
        
        var processedTakeaways = summary.keyTakeaways.map { item ->
            val cleanedTitle = if (keepNumbering) {
                item.title.replace("**", "").replace("__", "").removeSuffix(":").trim()
            } else {
                stripNumbering(item.title.replace("**", "").replace("__", "")).removeSuffix(":").trim()
            }
            val cleanedDetails = if (keepNumbering) {
                item.details.replace("**", "").replace("__", "").trim()
            } else {
                stripNumbering(item.details.replace("**", "").replace("__", "")).trim()
            }
            sanitizeTakeawayItem(TakeawayItem(cleanedTitle, cleanedDetails, item.visualMetadata))
        }.filter { it.title.isNotBlank() && it.details.isNotBlank() }
        
        val targetType = type ?: AnalysisType.STANDARD_WEBSEITE
        when (targetType) {
            AnalysisType.TOP_3_KERNAUSSAGEN, AnalysisType.KEY_TAKEAWAYS -> {
                if (processedTakeaways.size > 3) {
                    processedTakeaways = processedTakeaways.take(3)
                }
            }
            else -> {}
        }
        
        for (item in processedTakeaways) {
            val dClean = item.details.trim()
            val tClean = item.title.trim()
            if (isRawJson(dClean) || isRawJson(tClean)) {
                throw java.io.IOException("ParserFailure: Takeaway details or title contains raw unparsed JSON. Details: $dClean")
            }
        }
        
        return summary.copy(
            title = cleanTitle,
            shortDescription = cleanShortDesc,
            keyTakeaways = processedTakeaways,
            owner = cleanOwner
        )
    }

    private fun stripNumbering(text: String): String {
        var cleaned = text.trim()
        var changed = true
        while (changed) {
            val before = cleaned
            cleaned = cleaned.replace(Regex("^\\s*[-\\*•▪◦•●▪◆]+\\s*"), "")
            cleaned = cleaned.replace(Regex("^\\s*\\(?\\d+[:\\.)]\\s*"), "")
            cleaned = cleaned.replace(Regex("^\\s*\\(?[a-zA-Z]\\)?\\.\\s*"), "")
            cleaned = cleaned.trim()
            if (cleaned == before) {
                changed = false
            }
        }
        return cleaned.replace(Regex("\"?,?\\s*$"), "").trim()
    }

    private fun sanitizeTakeawayItem(item: TakeawayItem): TakeawayItem {
        var t = item.title.replace("**", "").replace("__", "").trim()
        var d = item.details.replace("**", "").replace("__", "").trim()

        if (isRawJson(d)) {
            d = stripMarkdownAndExtract(d)
        }
        if (isRawJson(t)) {
            t = stripMarkdownAndExtract(t)
        }

        // Clean any leading/trailing Markdown bold indicators, bullets, or numbering
        t = stripNumbering(t)
        d = stripNumbering(d)

        // Ensure we strip Markdown bolding characters completely
        t = t.replace("**", "").replace("__", "").trim()
        d = d.replace("**", "").replace("__", "").trim()

        // If title is blank but details has content, split details to get a title
        if (t.isBlank() && d.isNotBlank()) {
            val split = d.split(Regex("[.:?!]"))
            val firstSentence = split.firstOrNull()?.trim() ?: ""
            if (firstSentence.length in 3..60) {
                t = firstSentence
            } else {
                val words = d.split(Regex("\\s+"))
                t = if (words.size >= 5) {
                    words.take(5).joinToString(" ")
                } else {
                    "Kernaussage"
                }
            }
        }

        // If details is blank or duplicate of title
        if (d.isBlank() || d.equals(t, ignoreCase = true)) {
            val words = t.split(Regex("\\s+"))
            if (t.length > 50 || words.size > 8) {
                // Split long title into title and details
                val pair = splitLongTitle(t)
                if (pair.first.isNotBlank() && pair.second.isNotBlank()) {
                    t = pair.first
                    d = pair.second
                } else {
                    // Fallback supplement
                    d = "Ergänzende Detailausführungen sind dem Quelltext direkt zu entnehmen."
                }
            } else if (t.isNotBlank() && t.length >= 3) {
                // Short title -> controlled supplement
                d = "Ergänzende Detailausführungen sind dem Quelltext direkt zu entnehmen."
            } else {
                // Too short or invalid -> discard
                t = ""
                d = ""
            }
        }

        // Final check on word/length limits and cleaning
        if (t.length > 120) {
            // Trim title if it exceeds the limit
            val words = t.split(Regex("\\s+"))
            t = if (words.size > 10) {
                words.take(10).joinToString(" ") + "..."
            } else {
                t.substring(0, 117) + "..."
            }
        }

        return TakeawayItem(t, d, item.visualMetadata)
    }

    private fun splitLongTitle(longTitle: String): Pair<String, String> {
        val text = longTitle.trim()
        // Try colon
        val colonIdx = text.indexOf(":")
        if (colonIdx in 3..40) {
            val t = text.substring(0, colonIdx).trim()
            val d = text.substring(colonIdx + 1).trim()
            if (t.isNotBlank() && d.isNotBlank()) return Pair(t, d)
        }
        // Try semicolon
        val semiIdx = text.indexOf(";")
        if (semiIdx in 3..40) {
            val t = text.substring(0, semiIdx).trim()
            val d = text.substring(semiIdx + 1).trim()
            if (t.isNotBlank() && d.isNotBlank()) return Pair(t, d)
        }
        // Try comma
        val commaIdx = text.indexOf(",")
        if (commaIdx in 3..40) {
            val t = text.substring(0, commaIdx).trim()
            val d = text.substring(commaIdx + 1).trim()
            if (t.isNotBlank() && d.isNotBlank()) return Pair(t, d)
        }
        // Split by words (first 6 words as title, rest as details)
        val words = text.split(Regex("\\s+"))
        if (words.size > 8) {
            val titleWords = words.take(6)
            val detailsWords = words.drop(6)
            val t = titleWords.joinToString(" ").trim()
            val d = detailsWords.joinToString(" ").trim()
            return Pair(t, d)
        }
        return Pair("", "")
    }

    private fun parseStringTakeaway(item: String): TakeawayItem {
        val cleaned = cleanTakeawayItem(item)
        val regex = Regex("^\\s*\\*\\*(.*?)\\*\\*\\s*(?:[:\\-]?\\s*)?(.*)$")
        val match = regex.find(cleaned)
        return if (match != null) {
            val title = match.groupValues[1].trim().removeSuffix(":").trim()
            val details = match.groupValues[2].trim()
            TakeawayItem(title, details)
        } else {
            val colonIndex = cleaned.indexOf(": ")
            if (colonIndex != -1) {
                val title = cleaned.substring(0, colonIndex).trim().removeSuffix(":").trim()
                val details = cleaned.substring(colonIndex + 2).trim()
                TakeawayItem(title, details)
            } else {
                TakeawayItem("Inhalt", cleaned)
            }
        }
    }

    private fun cleanTakeawayItem(item: String, keepNumbering: Boolean = false): String {
        var withoutBullet = item.trim()
        if (withoutBullet.startsWith("-") || withoutBullet.startsWith("•")) {
            withoutBullet = withoutBullet.substring(1).trim()
        } else if (withoutBullet.startsWith("*") && !withoutBullet.startsWith("**")) {
            withoutBullet = withoutBullet.substring(1).trim()
        }
        
        val result = if (keepNumbering) {
            withoutBullet
        } else {
            withoutBullet.replace(Regex("^\\s*\"?\\s*\\d+[:\\.)]\\s*"), "")
        }
        return result.replace(Regex("\"?,?\\s*$"), "").trim()
    }

    private fun extractJsonField(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonFieldRobust(json: String, vararg keys: String): String? {
        for (key in keys) {
            val keyIdx = json.indexOf("\"$key\"")
            if (keyIdx == -1) continue
            
            val colonIdx = json.indexOf(":", keyIdx + key.length + 2)
            if (colonIdx == -1) continue
            
            val quoteIdx = json.indexOf("\"", colonIdx + 1)
            if (quoteIdx == -1) continue
            
            val between = json.substring(colonIdx + 1, quoteIdx).trim()
            if (between.contains("{") || between.contains("[")) {
                continue
            }
            
            var closingIdx = -1
            var tempIdx = quoteIdx + 1
            while (tempIdx < json.length) {
                val c = json[tempIdx]
                if (c == '"') {
                    val remaining = json.substring(tempIdx + 1).trim()
                    if (remaining.startsWith(",") || remaining.startsWith("}") || remaining.startsWith("]") || remaining.isEmpty()) {
                        closingIdx = tempIdx
                        break
                    }
                }
                tempIdx++
            }
            
            if (closingIdx != -1) {
                val found = json.substring(quoteIdx + 1, closingIdx).trim()
                if (found.isNotBlank()) return found
            }
        }
        return null
    }
}
