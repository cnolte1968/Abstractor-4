package com.example.data.engine

import android.util.Log
import com.example.data.*
import com.example.data.remote.*
import com.example.domain.engine.AnalysisEngine
import com.example.domain.engine.EngineContract
import com.example.domain.engine.PromptAssetLoader
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NonRetryableGeminiException(
    override val message: String,
    val category: String
) : IllegalStateException(message)

abstract class BaseGeminiEngine(
    protected val gateway: com.example.domain.repository.GeminiGateway,
    protected val promptAssetLoader: PromptAssetLoader
) : AnalysisEngine {

    private fun ByteArray.sha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }

    protected open fun resolvePromptPath(input: CanonicalAnalysisInput): String {
        val functionId = contract.functionId
        val canonicalType = input.analysisType.canonical()
        if (functionId == "FREE_SOURCE_QUERY" || canonicalType == AnalysisType.FREE_SOURCE_QUERY) {
            if (isMultimediaSource(input)) {
                return "prompts/F_MULTIMEDIA_SOURCE_QA.md"
            }
        }
        return contract.promptPath
    }

    private fun isMultimediaSource(input: CanonicalAnalysisInput): Boolean {
        if (input.sourceType == com.example.domain.model.SourceType.YOUTUBE) return true
        val platform = input.metadata["sourcePlatform"]?.uppercase()
        if (platform == "YOUTUBE") return true
        val st = input.metadata["sourceType"]?.uppercase()
        if (st == "VIDEO" || st == "YOUTUBE_VIDEO" || st == "MULTIMEDIA") return true
        val extractor = input.metadata["extractor"]
        if (extractor == "RemoteVideoInputExtractor" || extractor == "YoutubeInputExtractor") return true
        val url = input.metadata["url"] ?: input.metadata["uri"] ?: ""
        if (url.contains("youtube.com") || url.contains("youtu.be")) return true
        return false
    }

    override suspend fun analyze(input: CanonicalAnalysisInput): DomainSummary {
        val startTime = System.currentTimeMillis()
        val analysisType = input.analysisType

        // Start and pass Engine Routing step at runtime
        PipelineReportStore.startStep("engine_routing", "Engine Routing", "Routing for engine: ${this.javaClass.simpleName}")
        PipelineReportStore.updateSection("engine_routing") { map ->
            map["engineRoutingOutcome"] = "SUCCESS"
            map["selectedEngineName"] = this.javaClass.simpleName
            map["selectedEngineClass"] = this.javaClass.name
            map["expectedEngineClass"] = this.javaClass.name
            map["engineMatchesExpected"] = true
            map["decision"] = "Load prompt for selected engine"
            map["nextStep"] = "prompt_loading"
        }
        PipelineReportStore.endStepPass(
            "engine_routing",
            "Routed to ${this.javaClass.simpleName} successfully",
            decision = "Load prompt for selected engine",
            nextStep = "prompt_loading"
        )

        // Resolve effective prompt path based on functionId and input source
        val effectivePromptPath = resolvePromptPath(input)

        // Load prompt via abstraction layer
        PipelineReportStore.startStep("prompt_loading", "Prompt Loading", "Path: $effectivePromptPath")
        val basePrompt = try {
            val loaded = promptAssetLoader.loadAsset(effectivePromptPath)
            PipelineReportStore.endStepPass(
                "prompt_loading",
                "Loaded base system instruction ($effectivePromptPath). Length: ${loaded.length} characters",
                decision = "Load global rules"
            )
            loaded
        } catch (e: Exception) {
            Log.e("BaseGeminiEngine", "Failed to load system instruction from path: $effectivePromptPath", e)
            PipelineReportStore.endStepFail("prompt_loading", e)
            throw IllegalStateException("Failed to load prompt for ${contract.functionId}: ${e.message}", e)
        }

        val globalRules = try {
            promptAssetLoader.loadAsset("prompts/_global_quality_rules.md")
        } catch (e: Exception) {
            ""
        }

        val systemInstructionText = if (globalRules.isNotBlank()) {
            "$basePrompt\n\n=== GLOBAL QUALITY RULES ===\n$globalRules"
        } else {
            basePrompt
        }

        val promptHash = systemInstructionText.toByteArray(Charsets.UTF_8).sha256()
        val selectedPromptFile = effectivePromptPath
        val globalRulesLoaded = systemInstructionText.contains("=== GLOBAL QUALITY RULES ===")

        com.example.data.GatewayDiagnostics.loadedFunctionId = contract.functionId
        com.example.data.GatewayDiagnostics.loadedAnalysisType = analysisType?.name ?: ""
        com.example.data.GatewayDiagnostics.loadedCanonicalAnalysisType = analysisType?.canonical()?.name ?: ""
        com.example.data.GatewayDiagnostics.loadedEngineName = contract.capabilities.name
        com.example.data.GatewayDiagnostics.loadedPromptAssetFile = effectivePromptPath
        com.example.data.GatewayDiagnostics.loadedPromptResolvedAssetPath = "assets/$effectivePromptPath"
        com.example.data.GatewayDiagnostics.loadedPromptLength = systemInstructionText.length
        com.example.data.GatewayDiagnostics.loadedPromptSha256 = promptHash
        com.example.data.GatewayDiagnostics.loadedPromptFirst300Chars = if (systemInstructionText.length > 300) systemInstructionText.substring(0, 300) else systemInstructionText
        com.example.data.GatewayDiagnostics.loadedPromptContainsOutputLimits = systemInstructionText.contains("max. 2 Sätze", ignoreCase = true) || systemInstructionText.contains("exakt 3 bis 5", ignoreCase = true) || systemInstructionText.contains("8 Wörter", ignoreCase = true)
        com.example.data.GatewayDiagnostics.loadedPromptContainsBoilerplateExclusion = systemInstructionText.contains("Website-Rahmeninhalten", ignoreCase = true) || systemInstructionText.contains("Kommentaren", ignoreCase = true) || systemInstructionText.contains("Kategorien", ignoreCase = true)

        val url = input.metadata["url"] ?: input.metadata["uri"] ?: "file://${input.metadata["fileName"] ?: "unknown"}"
        val contentText = input.enrichedText

        // Build main request prompt
        val promptBuilder = StringBuilder()
        promptBuilder.append("Analyse für die URL/Datei: $url\n")
        if (input.mimeType == "application/pdf" && input.rawBytes != null) {
            promptBuilder.append("Das Dokument liegt als angehängte PDF-Datei vor. Bitte analysiere den gesamten Inhalt dieser PDF-Datei direkt.\n")
        } else {
            if (!contentText.isNullOrBlank()) {
                promptBuilder.append("Inhalt der Quelle:\n$contentText\n")
            }
        }
        val freeQuery = input.freeQuery
        if (!freeQuery.isNullOrBlank()) {
            promptBuilder.append("Anwender-Anfrage (Freie Quellenanfrage):\n$freeQuery\n")
        }

        val requestPartText = promptBuilder.toString()

        // Get runtime config
        val runtimeConfig = AnalysisRuntimeConfigs.forType(analysisType)
        val activeGrounding = input.useSearchGrounding || runtimeConfig.forceGrounding
        val temp = runtimeConfig.temperature

        // Define JSON schema
        val relevantorSummarySchema = ResponseSchema(
            type = "OBJECT",
            properties = mapOf(
                "title" to SchemaProperty(type = "STRING", description = "Reine Identifikation des Analyseobjekts. Keine Beschreibung, keine Bewertung."),
                "original_url" to SchemaProperty(type = "STRING", description = "Die übergebene Original-URL der Quelle"),
                "short_description" to SchemaProperty(type = "STRING", description = "Grundverständnis. Erklärt was das Objekt ist, Zweck und Kontext. Keine Vorwegnahme von Kernaussagen."),
                "key_takeaways" to SchemaProperty(
                    type = "ARRAY",
                    description = "Zusätzliche Erkenntnisse (additional insights only). Jeder Punkt muss neue Informationen liefern. Keine Wiederholung von title oder short_description.",
                    items = SchemaProperty(
                        type = "OBJECT",
                        properties = mapOf(
                            "title" to SchemaProperty(type = "STRING", description = "Das Leitmotiv oder Kernthema des Takeaways (kurz, maximal 8 Wörter, kein Markdown-Fettdruck)"),
                            "details" to SchemaProperty(type = "STRING", description = "Die detaillierte Ausführung oder Begründung")
                        ),
                        required = listOf("title", "details")
                    )
                ),
                "owner" to SchemaProperty(type = "STRING", description = "Der Urheber, Autor oder Ersteller der Quelle (falls auffindbar)")
            ),
            required = listOf("title", "original_url", "short_description", "key_takeaways")
        )

        // Build config without schema conflict when grounding is enabled
        val generationConfig = if (activeGrounding) {
            GenerationConfig(
                responseMimeType = null,
                temperature = temp,
                maxOutputTokens = runtimeConfig.maxOutputTokens
            )
        } else {
            GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = relevantorSummarySchema,
                temperature = temp,
                maxOutputTokens = runtimeConfig.maxOutputTokens
            )
        }

        val tools = if (activeGrounding) {
            listOf(Tool(googleSearch = emptyMap()))
        } else {
            null
        }

        val model = "gemini-2.5-flash"

        val partsList = mutableListOf<Part>()
        if (input.rawBytes != null && input.mimeType != null) {
            val base64Data = android.util.Base64.encodeToString(input.rawBytes, android.util.Base64.NO_WRAP)
            partsList.add(Part(inlineData = Blob(mimeType = input.mimeType, data = base64Data)))
        }
        partsList.add(Part(text = requestPartText))

        val isPdf = (input.mimeType == "application/pdf" || (input.rawBytes != null && input.metadata["fileName"]?.lowercase()?.endsWith(".pdf") == true))
        
        if (isPdf) {
            check(partsList.any { it.inlineData?.mimeType == "application/pdf" }) {
                "PDF_INLINE_DATA_MISSING"
            }
        }
        check(partsList.any { it.text?.isNotBlank() == true }) {
            "PROMPT_TEXT_MISSING"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = partsList)),
            generationConfig = generationConfig,
            tools = tools,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        val functionId = contract.functionId

        val isYoutubeUrl = YoutubeUrlDecoder.isYoutubeUrl(url)
        val sourceType = when {
            isYoutubeUrl -> "YOUTUBE"
            functionId == "DOCUMENT_SUMMARY" -> "DOCUMENT"
            else -> "WEBPAGE"
        }

        val byteSize = input.rawBytes?.size?.toLong() ?: input.enrichedText?.toByteArray(Charsets.UTF_8)?.size?.toLong() ?: 0L
        val requestMode = if (activeGrounding) "grounding" else "direct"

        var attempt = 1
        val maxAttempts = 2
        var lastException: Throwable? = null
        var lastParsedSummary: DomainSummary? = null
        var parserSuccess = false
        var validationStatus = "FAIL"
        var finalOutputItemCount = 0
        var httpStatus: Int = 200
        var rawResponsePreview: String = ""
        var rawResponseLength = 0

        var currentRequest = request
        var emptyCandidateFallbackTriggered = false
        var fallbackSourceContentLength = 0
        var attempt1CandidateCount = 0
        var attempt1PartsCount = 0
        var attempt1TextPartCount = 0
        var attempt1FinishReason = ""

        while (attempt <= maxAttempts) {
            try {
                Log.i("RelevantorRuntime", "Content generation attempt $attempt of $maxAttempts for functionId: $functionId")
                Log.i("RUNTIME_SMOKE", "GEMINI_REQUEST_START - Function: $functionId, Attempt: $attempt, Model: $model")
                
                GatewayDiagnostics.gatewayBaseUrl = "https://generativelanguage.googleapis.com/"
                GatewayDiagnostics.httpClientName = "Retrofit / OkHttpClient"
                GatewayDiagnostics.requestStartTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                GatewayDiagnostics.correlationId = input.analysisId
                GatewayDiagnostics.networkType = input.metadata["networkType"] ?: ""
                GatewayDiagnostics.resolvedHostBeforeRequest = if (GatewayDiagnostics.preflightDns == "PASS") "PASS" else "FAIL"

                PipelineReportStore.startStep("gemini_request", "Gemini Request", "Attempt $attempt, Model: $model")
                val response = gateway.generateContent(model, currentRequest)
                httpStatus = 200
                PipelineReportStore.endStepPass(
                    "gemini_request",
                    "Gemini content generated successfully on attempt $attempt",
                    decision = "Process Gemini response"
                )

                PipelineReportStore.startStep("gemini_response", "Gemini Response")
                val candidates = response.candidates
                val candidateCount = candidates.size
                val promptFeedback = response.promptFeedback
                val promptBlockReason = promptFeedback?.resolvedBlockReason
                val promptFeedbackPresent = promptFeedback != null

                var selectedCandidate: Candidate? = null
                var extractedResponseText: String? = null

                for (c in candidates) {
                    val textParts = c.content?.parts?.mapNotNull { it.text }?.filter { it.isNotBlank() } ?: emptyList()
                    if (textParts.isNotEmpty()) {
                        selectedCandidate = c
                        extractedResponseText = textParts.joinToString("\n")
                        break
                    }
                }

                val firstCandidate = candidates.firstOrNull()
                val finishReason = selectedCandidate?.resolvedFinishReason 
                    ?: firstCandidate?.resolvedFinishReason 
                    ?: promptBlockReason 
                    ?: "NONE"

                val contentPresent = firstCandidate?.content != null
                val partsPresent = (firstCandidate?.content?.parts?.isNotEmpty() == true)
                val partsCount = firstCandidate?.content?.parts?.size ?: 0
                val textPartCount = firstCandidate?.content?.parts?.count { !it.text.isNullOrBlank() } ?: 0

                val isSafetyBlocked = finishReason.equals("SAFETY", ignoreCase = true) 
                    || !promptBlockReason.isNullOrEmpty() 
                    || firstCandidate?.safetyRatings?.any { it.blocked == true } == true

                val isRecitationBlocked = finishReason.equals("RECITATION", ignoreCase = true)

                val responseText = extractedResponseText

                if (!responseText.isNullOrBlank()) {
                    rawResponseLength = responseText.length
                    rawResponsePreview = responseText.take(120)

                    PipelineReportStore.updateSection("gemini_response") { map ->
                        map["geminiResponseReceived"] = true
                        map["responseOutcome"] = "SUCCESS"
                        map["httpStatus"] = 200
                        map["candidateCount"] = candidateCount
                        map["contentPresent"] = contentPresent
                        map["partsPresent"] = partsPresent
                        map["partsCount"] = partsCount
                        map["textPartCount"] = textPartCount
                        map["finishReason"] = finishReason
                        map["safetyBlocked"] = isSafetyBlocked
                        map["promptFeedbackPresent"] = promptFeedbackPresent
                        map["promptBlockReason"] = promptBlockReason ?: ""
                        map["retryCount"] = attempt - 1
                        map["tolerantParseOutcome"] = "TEXT_FOUND"
                        map["finalMappedError"] = "NONE"
                        map["safetyBlockDetected"] = isSafetyBlocked
                        map["rawGeminiResponseLength"] = rawResponseLength
                        map["rawGeminiFirst1000SafeChars"] = responseText.take(1000)
                        if (response.usageMetadata != null) {
                            map["promptTokenCount"] = response.usageMetadata.promptTokenCount ?: 0
                            map["candidatesTokenCount"] = response.usageMetadata.candidatesTokenCount ?: 0
                            map["totalTokenCount"] = response.usageMetadata.totalTokenCount ?: 0
                        }
                        if (emptyCandidateFallbackTriggered) {
                            map["emptyCandidateFallbackTriggered"] = true
                            map["originalSourceContentLength"] = contentText?.length ?: 0
                            map["fallbackSourceContentLength"] = fallbackSourceContentLength
                            map["fallbackStrategy"] = "BALANCED_BEGIN_MIDDLE_END"
                            map["retryReason"] = "EMPTY_CANDIDATE_CONTENT"
                            map["attempt1CandidateCount"] = attempt1CandidateCount
                            map["attempt1PartsCount"] = attempt1PartsCount
                            map["attempt1TextPartCount"] = attempt1TextPartCount
                            map["attempt1FinishReason"] = attempt1FinishReason
                            map["attempt2CandidateCount"] = candidateCount
                            map["attempt2PartsCount"] = partsCount
                            map["attempt2TextPartCount"] = textPartCount
                            map["attempt2FinishReason"] = finishReason
                            map["fallbackOutcome"] = "SUCCESS"
                        }
                    }

                    Log.i("RUNTIME_SMOKE", "GEMINI_RESPONSE_RECEIVED - Function: $functionId, Response length: $rawResponseLength")
                    Log.d("BaseGeminiEngine", "RAW GEMINI RESPONSE (len=${responseText.length}):\n$responseText")
                    PipelineReportStore.endStepPass(
                        "gemini_response",
                        "Gemini response received. Candidates: $candidateCount, Selected text length: $rawResponseLength",
                        decision = "Normalize response"
                    )

                    try {
                        java.io.File("raw_gemini_response.json").writeText(responseText)
                    } catch (e: Exception) {
                        println("API_DEBUG: Failed to write raw response to file: ${e.message}")
                    }
                } else {
                    val errorCategory = when {
                        isSafetyBlocked -> "SAFETY_BLOCK"
                        isRecitationBlocked -> "RECITATION_BLOCK"
                        candidates.isEmpty() -> "NO_CANDIDATES"
                        contentPresent && partsCount == 0 -> "EMPTY_CANDIDATE_CONTENT"
                        contentPresent && textPartCount == 0 -> "EMPTY_CANDIDATE_CONTENT"
                        !contentPresent -> "EMPTY_CANDIDATE_CONTENT"
                        else -> "UNKNOWN_EMPTY_RESPONSE"
                    }

                    val userErrorMessage = when (errorCategory) {
                        "SAFETY_BLOCK" -> "Die KI konnte für diesen Inhalt keine Antwort erzeugen. Der Inhalt wurde möglicherweise durch eine Sicherheitsregel blockiert."
                        "RECITATION_BLOCK" -> "Die KI konnte keine Antwort erzeugen, da der Inhalt urheberrechtlich geschützte Rezitationen enthalten könnte."
                        "NO_CANDIDATES" -> "Die KI hat keine Ergebnisse geliefert. Bitte versuche es erneut."
                        else -> "Die KI hat keine auswertbare Antwort geliefert. Bitte versuche es erneut."
                    }

                    val canTriggerAdaptiveFallback = activeGrounding &&
                        attempt == 1 &&
                        errorCategory == "EMPTY_CANDIDATE_CONTENT" &&
                        !isSafetyBlocked &&
                        !isRecitationBlocked &&
                        promptBlockReason.isNullOrEmpty() &&
                        (finishReason.equals("STOP", ignoreCase = true) || finishReason.equals("NONE", ignoreCase = true) || finishReason.isBlank()) &&
                        !contentText.isNullOrBlank() &&
                        contentText.length > 12000

                    if (canTriggerAdaptiveFallback) {
                        emptyCandidateFallbackTriggered = true
                        attempt1CandidateCount = candidateCount
                        attempt1PartsCount = partsCount
                        attempt1TextPartCount = textPartCount
                        attempt1FinishReason = finishReason

                        val fallbackText = buildBalancedExcerpt(contentText, 12000)
                        fallbackSourceContentLength = fallbackText.length

                        val fallbackPromptBuilder = StringBuilder()
                        fallbackPromptBuilder.append("Analyse für die URL/Datei: $url\n")
                        if (input.mimeType == "application/pdf" && input.rawBytes != null) {
                            fallbackPromptBuilder.append("Das Dokument liegt als angehängte PDF-Datei vor. Bitte analysiere den gesamten Inhalt dieser PDF-Datei direkt.\n")
                        } else {
                            fallbackPromptBuilder.append("Inhalt der Quelle:\n$fallbackText\n")
                        }
                        if (!freeQuery.isNullOrBlank()) {
                            fallbackPromptBuilder.append("Anwender-Anfrage (Freie Quellenanfrage):\n$freeQuery\n")
                        }
                        val fallbackRequestText = fallbackPromptBuilder.toString()

                        val fallbackPartsList = mutableListOf<Part>()
                        if (input.rawBytes != null && input.mimeType != null) {
                            val base64Data = android.util.Base64.encodeToString(input.rawBytes, android.util.Base64.NO_WRAP)
                            fallbackPartsList.add(Part(inlineData = Blob(mimeType = input.mimeType, data = base64Data)))
                        }
                        fallbackPartsList.add(Part(text = fallbackRequestText))

                        currentRequest = GenerateContentRequest(
                            contents = listOf(Content(parts = fallbackPartsList)),
                            generationConfig = generationConfig,
                            tools = tools,
                            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
                        )

                        Log.i("RelevantorRuntime", "Empty candidate fallback triggered for attempt 2 with reduced text length: ${fallbackText.length}")
                        PipelineReportStore.updateSection("gemini_response") { map ->
                            map["emptyCandidateFallbackTriggered"] = true
                            map["originalSourceContentLength"] = contentText.length
                            map["fallbackSourceContentLength"] = fallbackSourceContentLength
                            map["fallbackStrategy"] = "BALANCED_BEGIN_MIDDLE_END"
                            map["retryReason"] = "EMPTY_CANDIDATE_CONTENT"
                            map["attempt1CandidateCount"] = attempt1CandidateCount
                            map["attempt1PartsCount"] = attempt1PartsCount
                            map["attempt1TextPartCount"] = attempt1TextPartCount
                            map["attempt1FinishReason"] = attempt1FinishReason
                        }

                        attempt++
                        continue
                    }

                    PipelineReportStore.updateSection("gemini_response") { map ->
                        map["geminiResponseReceived"] = true
                        map["responseOutcome"] = "FAIL"
                        map["httpStatus"] = 200
                        map["candidateCount"] = candidateCount
                        map["contentPresent"] = contentPresent
                        map["partsPresent"] = partsPresent
                        map["partsCount"] = partsCount
                        map["textPartCount"] = textPartCount
                        map["finishReason"] = finishReason
                        map["safetyBlocked"] = isSafetyBlocked
                        map["promptFeedbackPresent"] = promptFeedbackPresent
                        map["promptBlockReason"] = promptBlockReason ?: ""
                        map["retryCount"] = attempt - 1
                        map["tolerantParseOutcome"] = errorCategory
                        map["finalMappedError"] = errorCategory
                        map["safetyBlockDetected"] = isSafetyBlocked
                        if (emptyCandidateFallbackTriggered) {
                            map["emptyCandidateFallbackTriggered"] = true
                            map["originalSourceContentLength"] = contentText?.length ?: 0
                            map["fallbackSourceContentLength"] = fallbackSourceContentLength
                            map["fallbackStrategy"] = "BALANCED_BEGIN_MIDDLE_END"
                            map["retryReason"] = "EMPTY_CANDIDATE_CONTENT"
                            map["attempt1CandidateCount"] = attempt1CandidateCount
                            map["attempt1PartsCount"] = attempt1PartsCount
                            map["attempt1TextPartCount"] = attempt1TextPartCount
                            map["attempt1FinishReason"] = attempt1FinishReason
                            map["attempt2CandidateCount"] = candidateCount
                            map["attempt2PartsCount"] = partsCount
                            map["attempt2TextPartCount"] = textPartCount
                            map["attempt2FinishReason"] = finishReason
                            map["fallbackOutcome"] = "FAIL"
                        }
                    }

                    val ex = NonRetryableGeminiException(userErrorMessage, errorCategory)
                    PipelineReportStore.endStepFail("gemini_response", ex)
                    throw ex
                }

                PipelineReportStore.startStep("response_normalization", "Response Normalization", "Raw Length: $rawResponseLength")
                val normalizedResponse = if (responseText.trim().startsWith("```")) {
                    responseText.trim().removeSurrounding("```json", "```").trim()
                } else {
                    responseText
                }
                PipelineReportStore.endStepPass(
                    "response_normalization",
                    "Stripped markdown code fences if any. Normalized length: ${normalizedResponse.length} characters",
                    decision = "Parse response"
                )

                Log.i("RUNTIME_SMOKE", "PARSER_START - Function: $functionId")
                PipelineReportStore.startStep("parsing", "Parsing", "Response preview: $rawResponsePreview")
                val parsedSummary = SummaryResponseParser.parse(responseText, url, analysisType, input.analysisId)
                parserSuccess = true
                Log.i("RUNTIME_SMOKE", "PARSER_SUCCESS - Function: $functionId, Takeaways: ${parsedSummary.keyTakeaways.size}")
                lastParsedSummary = parsedSummary
                GatewayDiagnostics.copyFromLastParserReport()
                PipelineReportStore.endStepPass(
                    "parsing",
                    "Successfully parsed JSON. Takeaways extracted: ${parsedSummary.keyTakeaways.size}",
                    decision = "Validate contract schema"
                )

                val isFallback = parsedSummary.keyTakeaways.size == 1 &&
                    parsedSummary.keyTakeaways.first().title == "Analyse" &&
                    parsedSummary.keyTakeaways.first().details.contains("nicht strukturiert extrahiert")

                if (isFallback) {
                    Log.e("RUNTIME_SMOKE", "PARSER_FAILURE - Function: $functionId, Error: STRUCTURED_EXTRACTION_FAILED")
                    val fallbackEx = java.io.IOException("STRUCTURED_EXTRACTION_FAILED")
                    PipelineReportStore.endStepFail("parsing", fallbackEx)
                    throw fallbackEx
                }

                val verificationContext = RuntimeVerificationLayer.VerificationContext(
                    functionId = functionId,
                    promptHash = promptHash,
                    analysisType = analysisType,
                    sourceUrl = url
                )

                PipelineReportStore.startStep("contract_validation", "Contract Validation")
                val validationResult = RuntimeVerificationLayer.validate(parsedSummary, verificationContext)

                if (validationResult.isValid) {
                    validationStatus = "PASS"
                    finalOutputItemCount = parsedSummary.keyTakeaways.size
                    Log.i("RUNTIME_SMOKE", "CONTRACT_VALIDATION_SUCCESS - Function: $functionId")
                    lastException = null
                    
                    PipelineReportStore.endStepPass(
                        "contract_validation",
                        "Validation successful. JSON conforms perfectly to schema.",
                        decision = "Deliver analysis result to UI"
                    )

                    Log.i("RelevantorRuntime", """
                        === RELEVANTOR ANALYSIS RUN ===
                        AnalysisType: ${analysisType.name}
                        function_id: $functionId
                        prompt_source_path: $selectedPromptFile
                        prompt_hash: $promptHash
                        global_rules_loaded: $globalRulesLoaded
                        response_normalized: true
                        parser_success: $parserSuccess
                        runtime_verification_status: $validationStatus
                        retry_count: ${attempt - 1}
                        final_output_item_count: $finalOutputItemCount
                        source_type: $sourceType
                        is_youtube_url: $isYoutubeUrl
                        selected_prompt_file: $selectedPromptFile
                        ================================
                    """.trimIndent())
                    
                    break
                } else {
                    validationStatus = "FAIL"
                    val reason = validationResult.failureReason ?: "Unknown validation error"
                    Log.e("RUNTIME_SMOKE", "CONTRACT_VALIDATION_FAILURE - Function: $functionId, Reason: $reason")
                    Log.w("RelevantorRuntime", "Validation attempt $attempt failed: $reason")
                    GatewayDiagnostics.requestFailureStage = "CONTRACT_VALIDATION_FAILURE"
                    GatewayDiagnostics.failureStage = "CONTRACT_VALIDATION_FAILURE"
                    val contractEx = IllegalStateException("Validation failed: $reason")
                    com.example.data.PipelineReportStore.endStepFail("contract_validation", contractEx)
                    lastException = contractEx
                }
            } catch (e: Exception) {
                val runningStep = com.example.data.PipelineReportStore.getReport()?.steps?.find { it.status == "RUNNING" }
                if (runningStep != null) {
                    com.example.data.PipelineReportStore.endStepFail(runningStep.stepId, e)
                }
                parserSuccess = false
                validationStatus = "FAIL"
                val errorMsg = e.message ?: ""
                GatewayDiagnostics.exceptionClass = e.javaClass.name
                GatewayDiagnostics.exceptionMessage = errorMsg
                
                GatewayDiagnostics.copyFromLastParserReport()

                if (e is NonRetryableGeminiException) {
                    httpStatus = 200
                    GatewayDiagnostics.requestFailureStage = e.category
                    GatewayDiagnostics.failureStage = e.category
                    Log.e("RUNTIME_SMOKE", "${e.category} - Function: $functionId, Error: ${e.message}")
                    Log.e("RelevantorRuntime", "Non-retryable error on attempt $attempt: ${e.message}")
                    lastException = e
                    break
                }
                
                val hasResolvedAddresses = GatewayDiagnostics.okHttpDnsResolvedAddresses.isNotEmpty() || GatewayDiagnostics.dnsOutcome == "SUCCESS"
                val isSocketTimeout = e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException || e.localizedMessage?.contains("timeout", ignoreCase = true) == true
                val isUnknownHost = e is java.net.UnknownHostException || e.localizedMessage?.contains("Unable to resolve host", ignoreCase = true) == true
                val isConnect = e is java.net.ConnectException || e.localizedMessage?.contains("Connect", ignoreCase = true) == true
                val isStructuredFailed = errorMsg.contains("STRUCTURED_EXTRACTION_FAILED") || errorMsg.contains("ParserFailure")

                if (isStructuredFailed) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "PARSER_FAILURE"
                    GatewayDiagnostics.failureStage = "PARSER_FAILURE"
                    Log.e("RUNTIME_SMOKE", "PARSER_FAILURE - Function: $functionId, Error: $errorMsg")
                } else if (errorMsg.contains("Validation failed")) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "CONTRACT_VALIDATION_FAILURE"
                    GatewayDiagnostics.failureStage = "CONTRACT_VALIDATION_FAILURE"
                    Log.e("RUNTIME_SMOKE", "CONTRACT_VALIDATION_FAILURE - Function: $functionId, Error: $errorMsg")
                } else if (e is retrofit2.HttpException) {
                    httpStatus = e.code()
                    GatewayDiagnostics.requestFailureStage = "GEMINI_HTTP_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_HTTP_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_HTTP_FAILURE - Function: $functionId, Code: $httpStatus, Msg: $errorMsg")
                } else if (isUnknownHost && !hasResolvedAddresses) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_DNS_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_DNS_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_DNS_FAILURE - Function: $functionId, Error: $errorMsg")
                } else if (hasResolvedAddresses && (isSocketTimeout || GatewayDiagnostics.connectOutcome == "TIMEOUT")) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_CONNECT_TIMEOUT"
                    GatewayDiagnostics.failureStage = "GEMINI_CONNECT_TIMEOUT"
                    Log.e("RUNTIME_SMOKE", "GEMINI_CONNECT_TIMEOUT - Function: $functionId, Error: $errorMsg")
                } else if (hasResolvedAddresses && (isConnect || GatewayDiagnostics.connectOutcome == "FAILED")) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_CONNECT_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_CONNECT_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_CONNECT_FAILURE - Function: $functionId, Error: $errorMsg")
                } else if (isUnknownHost) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_DNS_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_DNS_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_DNS_FAILURE - Function: $functionId, Error: $errorMsg")
                } else if (isSocketTimeout) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_CONNECT_TIMEOUT"
                    GatewayDiagnostics.failureStage = "GEMINI_CONNECT_TIMEOUT"
                    Log.e("RUNTIME_SMOKE", "GEMINI_CONNECT_TIMEOUT - Function: $functionId, Error: $errorMsg")
                } else if (isConnect) {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_CONNECT_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_CONNECT_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_CONNECT_FAILURE - Function: $functionId, Error: $errorMsg")
                } else {
                    httpStatus = 0
                    GatewayDiagnostics.requestFailureStage = "GEMINI_HTTP_FAILURE"
                    GatewayDiagnostics.failureStage = "GEMINI_HTTP_FAILURE"
                    Log.e("RUNTIME_SMOKE", "GEMINI_HTTP_FAILURE - Function: $functionId, Error: $errorMsg")
                }
                Log.e("RelevantorRuntime", "Error on content generation attempt $attempt: ${e.message}", e)
                lastException = e
            }
            attempt++
        }

        val durationMs = System.currentTimeMillis() - startTime

        val trace = com.example.domain.model.AnalysisTrace(
            analysisId = input.analysisId,
            functionId = functionId,
            sourceType = sourceType,
            displayName = input.metadata["fileName"] ?: url,
            mimeType = input.mimeType ?: "",
            byteSize = byteSize,
            promptFile = selectedPromptFile,
            promptHash = promptHash,
            model = model,
            requestMode = requestMode,
            httpStatus = httpStatus,
            rawResponseLength = rawResponseLength,
            parserSuccess = parserSuccess,
            takeawayCount = lastParsedSummary?.keyTakeaways?.size ?: 0,
            fallbackUsed = lastParsedSummary?.fallbackUsed ?: false,
            exceptionClass = lastException?.javaClass?.simpleName ?: "None",
            exceptionMessage = lastException?.message ?: "None",
            durationMs = durationMs
        )

        trace.validateOrThrow()
        trace.log()

        if (lastException != null) {
            throw lastException
        }

        return lastParsedSummary ?: throw IllegalStateException("Analysis resulted in empty summary without throwing exception.")
    }

    companion object {
        fun buildBalancedExcerpt(text: String, targetMaxChars: Int = 12000): String {
            if (text.length <= targetMaxChars) return text

            val seg1Target = 5000
            val seg2Target = 3000
            val seg3Target = 4000

            val textLength = text.length

            val rawEnd1 = seg1Target.coerceAtMost(textLength)
            val end1 = findBestBoundary(text, rawEnd1, searchRadius = 300, searchBackward = true)

            val midIndex = textLength / 2
            val rawStart2 = (midIndex - seg2Target / 2).coerceAtLeast(end1 + 50)
            val start2 = findBestBoundary(text, rawStart2, searchRadius = 300, searchBackward = true)

            val rawEnd2 = (start2 + seg2Target).coerceAtMost(textLength)
            val end2 = findBestBoundary(text, rawEnd2, searchRadius = 300, searchBackward = true)

            val rawStart3 = (textLength - seg3Target).coerceAtLeast(end2 + 50)
            val start3 = findBestBoundary(text, rawStart3, searchRadius = 300, searchBackward = true)

            val part1 = text.substring(0, end1).trim()
            val part2 = if (start2 in end1 until end2) text.substring(start2, end2).trim() else ""
            val part3 = if (start3 in (end2 + 1) until textLength) text.substring(start3, textLength).trim() else ""

            val sb = StringBuilder()
            sb.append("[AUSZUG ANFANG]\n").append(part1)
            if (part2.isNotBlank()) {
                sb.append("\n\n[AUSZUG MITTE]\n").append(part2)
            }
            if (part3.isNotBlank()) {
                sb.append("\n\n[AUSZUG ENDE]\n").append(part3)
            }

            val result = sb.toString()
            return if (result.length > targetMaxChars + 500) {
                result.take(targetMaxChars)
            } else {
                result
            }
        }

        private fun findBestBoundary(text: String, pos: Int, searchRadius: Int = 300, searchBackward: Boolean = true): Int {
            if (pos <= 0) return 0
            if (pos >= text.length) return text.length

            val range = if (searchBackward) {
                (pos - searchRadius).coerceAtLeast(0) .. pos
            } else {
                pos .. (pos + searchRadius).coerceAtMost(text.length)
            }

            val newlineIdx = if (searchBackward) {
                text.lastIndexOf('\n', pos).takeIf { it in range }
            } else {
                text.indexOf('\n', pos).takeIf { it in range }
            }
            if (newlineIdx != null && newlineIdx != -1) return newlineIdx

            val punctuation = listOf(". ", "? ", "! ")
            for (p in punctuation) {
                val idx = if (searchBackward) {
                    text.lastIndexOf(p, pos).takeIf { it in range }
                } else {
                    text.indexOf(p, pos).takeIf { it in range }
                }
                if (idx != null && idx != -1) return idx + p.length
            }

            val spaceIdx = if (searchBackward) {
                text.lastIndexOf(' ', pos).takeIf { it in range }
            } else {
                text.indexOf(' ', pos).takeIf { it in range }
            }
            if (spaceIdx != null && spaceIdx != -1) return spaceIdx

            return pos
        }
    }
}
