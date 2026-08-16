package com.example.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import com.example.data.YoutubeTranscriptHelper
import com.example.data.WebpageExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.domain.usecase.AnalyzeContentUseCase
import com.example.domain.usecase.SaveAnalysisUseCase
import com.example.domain.usecase.LoadHistoryUseCase
import com.example.domain.usecase.SyncUserDataUseCase
import com.example.domain.repository.UserRepository
import com.example.domain.model.EligibilityStatus
import com.example.domain.model.FunctionEligibility
import com.example.domain.model.SourceProfile
import com.example.domain.usecase.FunctionEligibilityResolver
import com.example.domain.usecase.SourceResolver
import com.example.ui.metadata.FeatureCatalog

enum class LoadingStep {
    IDLE,
    FETCHING_DATA,
    ANALYZING_INPUT,
    GENERATING_OUTPUT,
    SUCCESS,
    ERROR
}

sealed interface UiState {
    val analysisId: String get() = ""
    object Idle : UiState
    data class Loading(val step: LoadingStep = LoadingStep.FETCHING_DATA, override val analysisId: String = "") : UiState
    data class Success(val summary: DomainSummary, val analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.WEB_SUMMARY, override val analysisId: String = "") : UiState
    data class Error(val isPaywallOrBlocked: Boolean, val message: String, val detail: String? = null, override val analysisId: String = "") : UiState
}

sealed interface AuthStatus {
    object Guest : AuthStatus
    data class Authenticated(val userId: String, val username: String, val tokenPresent: Boolean = true) : AuthStatus
    data class Error(val message: String) : AuthStatus
}

class MainViewModel : ViewModel() {

    private var isInitialized = false
    private var appContext: android.content.Context? = null
    private lateinit var analyzeContentUseCase: AnalyzeContentUseCase
    private lateinit var saveAnalysisUseCase: SaveAnalysisUseCase
    private lateinit var loadHistoryUseCase: LoadHistoryUseCase
    private lateinit var syncUserDataUseCase: SyncUserDataUseCase
    private lateinit var extractContentUseCase: com.example.domain.usecase.ExtractContentUseCase
    lateinit var userRepository: UserRepository

    private val _savedHistories = MutableStateFlow<List<DomainSummary>>(emptyList())
    val savedHistories: StateFlow<List<DomainSummary>> = _savedHistories

    private val _syncPendingCount = MutableStateFlow(0)
    val syncPendingCount: StateFlow<Int> = _syncPendingCount

    private val _syncUiState = MutableStateFlow<String>("IDLE")
    val syncUiState: StateFlow<String> = _syncUiState

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage

    private val _activeUser = MutableStateFlow<String?>(null)
    val activeUser: StateFlow<String?> = _activeUser

    private val _authStatus = MutableStateFlow<AuthStatus>(AuthStatus.Guest)
    val authStatus: StateFlow<AuthStatus> = _authStatus

    private val defaultFavoritesList = listOf(
        "WEB_SUMMARY",
        "KEY_TAKEAWAYS",
        "FACTS_VS_OPINIONS",
        "FRESHNESS_CHECK",
        "RISK_ANALYSIS",
        "GOOGLE_MAPS_ANALYZER"
    )

    private val _favoritesList = MutableStateFlow<List<String>>(defaultFavoritesList)
    val favoritesList: StateFlow<List<String>> = _favoritesList

    fun toggleFavorite(functionId: String) {
        val current = _favoritesList.value
        val updated = if (current.contains(functionId)) {
            current - functionId
        } else {
            current + functionId
        }
        _favoritesList.value = updated
        appContext?.let { ctx ->
            com.example.data.local.SessionStorage.saveFavorites(ctx, updated)
        }
    }

    fun moveFavoriteUp(functionId: String) {
        val current = _favoritesList.value.toMutableList()
        val index = current.indexOf(functionId)
        if (index > 0) {
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            _favoritesList.value = current
            appContext?.let { ctx ->
                com.example.data.local.SessionStorage.saveFavorites(ctx, current)
            }
        }
    }

    fun moveFavoriteDown(functionId: String) {
        val current = _favoritesList.value.toMutableList()
        val index = current.indexOf(functionId)
        if (index in 0 until current.size - 1) {
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            _favoritesList.value = current
            appContext?.let { ctx ->
                com.example.data.local.SessionStorage.saveFavorites(ctx, current)
            }
        }
    }

    fun initIfNeeded(context: android.content.Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        this.appContext = appContext
        com.example.data.GeminiRepository.staticContext = appContext
        val db = com.example.data.local.RelevantorDatabase.getInstance(appContext)
        val api = com.example.data.remote.BackendApiService.create()
        
        val analysisRepo = com.example.data.repository.AnalysisRepositoryImpl(db, api, appContext)
        val syncRepo = com.example.data.repository.SyncRepositoryImpl(db, api, appContext)
        userRepository = com.example.data.repository.UserRepositoryImpl(appContext, api)
        
        analyzeContentUseCase = AnalyzeContentUseCase(analysisRepo, com.example.data.GeminiRepository, appContext)
        saveAnalysisUseCase = SaveAnalysisUseCase(analysisRepo)
        loadHistoryUseCase = LoadHistoryUseCase(analysisRepo)
        syncUserDataUseCase = SyncUserDataUseCase(syncRepo)
        val extractionRepo = com.example.data.repository.ContentExtractionRepositoryImpl(appContext)
        extractContentUseCase = com.example.domain.usecase.ExtractContentUseCase(extractionRepo)
        
        isInitialized = true
        _favoritesList.value = com.example.data.local.SessionStorage.getFavorites(appContext)
        observeHistory()
        updatePendingSyncCount()
        updateActiveUser()

        // Auto-save successful analyses & update Pipeline Report
        viewModelScope.launch {
            _uiState.collect { state ->
                if (state is UiState.Success) {
                    try {
                        saveAnalysisUseCase.execute(state.summary)
                        updatePendingSyncCount()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to auto-save analysis", e)
                    }
                }
                
                // Tracing for Pipeline Report
                val report = com.example.data.PipelineReportStore.getReport()
                if (report != null) {
                    when (state) {
                        is UiState.Success -> {
                            com.example.data.PipelineReportStore.startStep("rendering", "Rendering")
                            com.example.data.PipelineReportStore.updateSection("rendering") { map ->
                                map["resultRendered"] = true
                                map["targetScreen"] = "RESULT"
                                map["renderedTitle"] = state.summary.title
                                map["renderedSectionCount"] = 2
                                map["renderedTakeawayCount"] = state.summary.keyTakeaways.size
                            }
                            com.example.data.PipelineReportStore.endStepPass(
                                "rendering",
                                "Rendered summary with ${state.summary.keyTakeaways.size} takeaways",
                                decision = "Render analysis result screen successfully"
                            )
                            
                            com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                            com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                                val isDegraded = state.summary.fallbackUsed || state.summary.shortDescription == "TRANSCRIPT_UNAVAILABLE"
                                val isInsufficient = state.summary.shortDescription == "INSUFFICIENT_CONTENT" || state.summary.shortDescription.contains("INSUFFICIENT_CONTENT")
                                map["technicalStatus"] = "PASS"
                                map["stabilityStatus"] = "UNKNOWN"
                                map["pipelineCompleted"] = true
                                if (isInsufficient) {
                                    map["finalStatus"] = "INSUFFICIENT_CONTENT"
                                    map["functionalStatus"] = "INSUFFICIENT_CONTENT"
                                    map["semanticOutcomeReason"] = "Source content was insufficient for structured summary"
                                } else if (isDegraded) {
                                    map["finalStatus"] = "DEGRADED"
                                    map["functionalStatus"] = "DEGRADED"
                                    map["technicalErrorCategory"] = "TRANSCRIPT_UNAVAILABLE"
                                    map["semanticOutcomeReason"] = "Metadata fallback used due to unavailable transcript"
                                } else {
                                    map["finalStatus"] = "PASS"
                                    map["functionalStatus"] = "PASS"
                                    map["semanticOutcomeReason"] = "Normal analysis success"
                                }
                            }
                            val endMsg = if (state.summary.fallbackUsed || state.summary.shortDescription == "TRANSCRIPT_UNAVAILABLE") {
                                "Pipeline run completed with degraded status: TRANSCRIPT_UNAVAILABLE"
                            } else {
                                "Pipeline run completed successfully"
                            }
                            com.example.data.PipelineReportStore.endStepPass(
                                "final_result",
                                endMsg,
                                decision = "Idle"
                            )
                            
                            com.example.data.PipelineReportStore.updateSection("user_actions") { map ->
                                map["copyAvailable"] = true
                                map["shareAvailable"] = true
                                map["pdfAvailable"] = true
                                map["debugAvailable"] = true
                                map["pipelineReportAvailable"] = true
                            }
                            
                            appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                        }
                        is UiState.Error -> {
                            com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                            com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                                val isGeminiSuccess = com.example.data.GatewayDiagnostics.rawGeminiResponseLength > 0
                                val isParserOrContractFailure = state.detail?.contains("ParserFailure", ignoreCase = true) == true ||
                                        state.detail?.contains("STRUCTURED_EXTRACTION_FAILED", ignoreCase = true) == true ||
                                        state.detail?.contains("Validation failed", ignoreCase = true) == true ||
                                        state.detail?.contains("Contract violation", ignoreCase = true) == true ||
                                        state.message.contains("Parser", ignoreCase = true)
                                val isInsufficient = state.detail?.contains("INSUFFICIENT_CONTENT", ignoreCase = true) == true ||
                                        state.message.contains("INSUFFICIENT_CONTENT", ignoreCase = true)

                                map["stabilityStatus"] = "UNKNOWN"
                                map["pipelineCompleted"] = false
                                map["userVisibleErrorTitle"] = state.message
                                map["userVisibleErrorMessage"] = state.detail ?: ""
                                map["failureStage"] = com.example.data.GatewayDiagnostics.failureStage.ifEmpty { "EXECUTION_ERROR" }
                                map["failureStepId"] = "execution_failure"

                                if (isInsufficient) {
                                    map["finalStatus"] = "INSUFFICIENT_CONTENT"
                                    map["technicalStatus"] = "PASS"
                                    map["functionalStatus"] = "INSUFFICIENT_CONTENT"
                                    map["semanticOutcomeReason"] = "INSUFFICIENT_CONTENT: Source had insufficient extractable text"
                                } else if (isGeminiSuccess && isParserOrContractFailure) {
                                    map["finalStatus"] = "FAIL"
                                    map["technicalStatus"] = "PASS"
                                    map["functionalStatus"] = "FAIL"
                                    val rejReason = com.example.data.GatewayDiagnostics.parserFailureReason.ifEmpty { state.detail ?: "STRUCTURED_EXTRACTION_FAILED" }
                                    map["parserStrictRejectionReason"] = rejReason
                                    map["semanticOutcomeReason"] = "Gemini API call succeeded technically (HTTP 200), but parser or contract validation failed: $rejReason"
                                } else {
                                    map["finalStatus"] = "FAIL"
                                    map["technicalStatus"] = "FAIL"
                                    map["functionalStatus"] = "FAIL"
                                    map["semanticOutcomeReason"] = state.message
                                }
                            }
                            com.example.data.PipelineReportStore.endStepFail(
                                "final_result",
                                null,
                                notes = "Failed on: ${state.message}. Detail: ${state.detail}"
                            )
                            
                            com.example.data.PipelineReportStore.updateSection("user_actions") { map ->
                                map["copyAvailable"] = false
                                map["shareAvailable"] = false
                                map["pdfAvailable"] = false
                                map["debugAvailable"] = true
                                map["pipelineReportAvailable"] = true
                            }
                            
                            appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            loadHistoryUseCase.execute().collect { list ->
                _savedHistories.value = list
            }
        }
    }

    fun openSavedAnalysis(summary: DomainSummary) {
        _currentUrl.value = summary.originalUrl
        _currentTitle.value = summary.title
        val savedType = if (summary.analysisId.contains("|")) {
            val parts = summary.analysisId.split("|")
            try {
                com.example.data.AnalysisType.valueOf(parts[1])
            } catch (e: Exception) {
                com.example.data.AnalysisType.WEB_SUMMARY
            }
        } else {
            com.example.data.AnalysisType.WEB_SUMMARY
        }
        _currentAnalysisType.value = savedType
        _uiState.value = UiState.Success(summary, savedType, summary.analysisId)
    }

    fun updatePendingSyncCount() {
        viewModelScope.launch {
            try {
                _syncPendingCount.value = syncUserDataUseCase.getPendingCount()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to get pending sync count", e)
            }
        }
    }

    fun updateActiveUser() {
        if (!com.example.data.BackendFeatureConfig.authEnabled) {
            _activeUser.value = null
            _authStatus.value = AuthStatus.Guest
            return
        }
        viewModelScope.launch {
            try {
                val username = userRepository.getActiveUsername()
                val token = userRepository.getActiveToken()
                if (username != null && token != null) {
                    _activeUser.value = username
                    _authStatus.value = AuthStatus.Authenticated(
                        userId = "database_user",
                        username = username,
                        tokenPresent = true
                    )
                } else {
                    _activeUser.value = null
                    _authStatus.value = AuthStatus.Guest
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to get active user", e)
                _activeUser.value = null
                _authStatus.value = AuthStatus.Error(e.localizedMessage ?: "Fehler beim Laden des Benutzers")
            }
        }
    }

    fun triggerSync() {
        if (!com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
            _syncUiState.value = "ERROR"
            _syncErrorMessage.value = "Synchronisation deaktiviert – Local-First Modus ist aktiv."
            return
        }
        viewModelScope.launch {
            _syncUiState.value = "SYNCING"
            _syncErrorMessage.value = null
            try {
                syncUserDataUseCase.execute()
                _syncUiState.value = "SUCCESS"
                updatePendingSyncCount()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Sync failed", e)
                _syncUiState.value = "ERROR"
                _syncErrorMessage.value = e.localizedMessage ?: "Synchronisierungsfehler"
            }
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _currentTitle = MutableStateFlow("")
    val currentTitle: StateFlow<String> = _currentTitle

    private val _sharedUrlToFill = MutableStateFlow("")
    val sharedUrlToFill: StateFlow<String> = _sharedUrlToFill

    private val _currentAnalysisType = MutableStateFlow<com.example.data.AnalysisType>(com.example.data.AnalysisType.WEB_SUMMARY)
    val currentAnalysisType: StateFlow<com.example.data.AnalysisType> = _currentAnalysisType

    private val sourceResolver = SourceResolver()
    private val eligibilityResolver = FunctionEligibilityResolver()

    private val _rawInput = MutableStateFlow("")
    val rawInput: StateFlow<String> = _rawInput

    private val _sourceProfile = MutableStateFlow<SourceProfile?>(sourceResolver.resolvePreFetchProfile(""))
    val sourceProfile: StateFlow<SourceProfile?> = _sourceProfile

    private val _featureEligibilityMap = MutableStateFlow<Map<String, FunctionEligibility>>(
        calculateEligibilityMap(sourceResolver.resolvePreFetchProfile(""))
    )
    val featureEligibilityMap: StateFlow<Map<String, FunctionEligibility>> = _featureEligibilityMap

    fun updateRawInput(input: String) {
        _rawInput.value = input
        val profile = sourceResolver.resolvePreFetchProfile(input)
        _sourceProfile.value = profile
        _featureEligibilityMap.value = calculateEligibilityMap(profile)
    }

    private fun calculateEligibilityMap(profile: SourceProfile): Map<String, FunctionEligibility> {
        val result = mutableMapOf<String, FunctionEligibility>()
        for (feature in FeatureCatalog.features) {
            val analysisType = feature.analysisType ?: continue
            val eligibility = if (feature.requiredAlternativeGroups.isNotEmpty()) {
                eligibilityResolver.resolveEligibilityWithAlternatives(
                    analysisType = analysisType,
                    sourceProfile = profile,
                    requiredAlternativeGroups = feature.requiredAlternativeGroups,
                    optionalCapabilities = feature.optionalCapabilities,
                    allowedSourceTypes = feature.allowedSourceTypes
                )
            } else {
                eligibilityResolver.resolveEligibility(
                    analysisType = analysisType,
                    sourceProfile = profile,
                    requiredCapabilities = feature.requiredCapabilities,
                    optionalCapabilities = feature.optionalCapabilities,
                    allowedSourceTypes = feature.allowedSourceTypes
                )
            }
            result[feature.functionId] = eligibility
        }
        return result
    }

    var cachedDirectContent: String? = null

    fun resetToIdle() {
        _uiState.value = UiState.Idle
        _currentAnalysisType.value = com.example.data.AnalysisType.WEB_SUMMARY
    }

    fun setAnalysisType(type: com.example.data.AnalysisType) {
        _currentAnalysisType.value = type
    }

    fun setSharedText(sharedText: String, intent: Intent? = null) {
        val extractedUrl = com.example.data.YoutubeUrlDecoder.extractUrl(sharedText)
        if (extractedUrl == null) {
            val trimmedText = sharedText.trim()
            if (trimmedText.length > 20) {
                _sharedUrlToFill.value = "https://local.shared.content"
                cachedDirectContent = trimmedText
            } else {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Inhalt zu kurz zum Zusammenfassen.",
                    detail = "Der geteilte Inhalt enthält weder eine Web-Adresse noch einen ausreichenden Textabschnitt."
                )
            }
            return
        }

        if (com.example.data.GoogleMapsUrlParser.isGoogleMapsUrl(extractedUrl)) {
            val trimmedUrl = extractedUrl.trim()
            _sharedUrlToFill.value = trimmedUrl
            return
        }

        val cleanUrl = extractedUrl.trim()
        _sharedUrlToFill.value = cleanUrl

        val restText = sharedText.replace(cleanUrl, "").replace("https://$cleanUrl", "").replace("http://$cleanUrl", "").trim()
        
        // Check if the URL addresses a restricted walled garden/social platform
        if (com.example.LocalContentExtractionEngine.isSocialMediaOrWalledUrl(cleanUrl)) {
            // Stage 1: Attempt to extract substantial caption/text from the incoming Intent (Zero-Network-Bypass)
            val stage1Payload = intent?.let { com.example.LocalContentExtractionEngine.extractIntentTextPayload(it, cleanUrl) }
            if (stage1Payload != null && stage1Payload.isNotBlank()) {
                cachedDirectContent = stage1Payload
                return
            }

            // Stage 2: Retrieve any scrapings harvested by our background Accessibility Service
            val stage2Payload = com.example.LocalContentExtractionEngine.getScrapedScreenTextAndReset()
            if (stage2Payload != null && stage2Payload.isNotBlank()) {
                cachedDirectContent = stage2Payload
                return
            }
        }

        // Wenn der restliche Text signifikant länger ist als nur die URL (z.B. > 50 Zeichen),
        // und es keine YouTube-Url ist, übergeben wir den gesamten Text direkt an die Gemini API!
        if (restText.length > 50 && !com.example.data.YoutubeUrlDecoder.isYoutubeUrl(cleanUrl)) {
            cachedDirectContent = sharedText
        } else {
            cachedDirectContent = null
        }
    }

    fun clearSharedUrlToFill() {
        _sharedUrlToFill.value = ""
    }

    fun processSharedText(sharedText: String, intent: Intent? = null) {
        resetToIdle()
        setSharedText(sharedText, intent)
    }

    fun processDirectShare(sharedText: String, analysisType: com.example.data.AnalysisType, intent: Intent? = null) {
        val extractedUrl = com.example.data.YoutubeUrlDecoder.extractUrl(sharedText)
        val isMaps = analysisType == com.example.data.AnalysisType.GOOGLE_MAPS_ANALYZER ||
                     analysisType == com.example.data.AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT

        val urlToCheck = extractedUrl ?: sharedText

        val isValid = if (isMaps) {
            extractedUrl != null && com.example.data.GoogleMapsUrlParser.isGoogleMapsUrl(urlToCheck.trim())
        } else {
            extractedUrl != null || sharedText.trim().length > 20
        }

        if (!isValid) {
            // Fallback to normal share if invalid for the selected function
            processSharedText(sharedText, intent)
            return
        }

        val rawUrl = extractedUrl?.trim() ?: "https://local.shared.content"
        val directContent = if (extractedUrl == null) sharedText.trim() else null

        // Populate shared URL to fill so input bar displays the shared link/text
        _sharedUrlToFill.value = rawUrl

        if (directContent != null) {
            cachedDirectContent = directContent
        } else if (extractedUrl != null) {
            val cleanUrl = extractedUrl.trim()
            val restText = sharedText.replace(cleanUrl, "").replace("https://$cleanUrl", "").replace("http://$cleanUrl", "").trim()
            if (com.example.LocalContentExtractionEngine.isSocialMediaOrWalledUrl(cleanUrl)) {
                val stage1Payload = intent?.let { com.example.LocalContentExtractionEngine.extractIntentTextPayload(it, cleanUrl) }
                val stage2Payload = com.example.LocalContentExtractionEngine.getScrapedScreenTextAndReset()
                if (stage1Payload != null && stage1Payload.isNotBlank()) {
                    cachedDirectContent = stage1Payload
                } else if (stage2Payload != null && stage2Payload.isNotBlank()) {
                    cachedDirectContent = stage2Payload
                }
            } else if (restText.length > 50 && !com.example.data.YoutubeUrlDecoder.isYoutubeUrl(cleanUrl)) {
                cachedDirectContent = sharedText
            }
        }

        // Doppelstartschutz: Not start if already loading identical content
        if (_uiState.value is UiState.Loading && _currentAnalysisType.value == analysisType && _currentUrl.value == rawUrl) {
            return
        }

        // Remove intent extra is handled in MainActivity.
        // Direct start without confirmation dialog
        fetchSummary(rawUrl = rawUrl, directContent = cachedDirectContent, analysisType = analysisType)
    }

    fun fetchSummary(rawUrl: String, directContent: String? = null, analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.WEB_SUMMARY, freeQuery: String? = null) {
        // Central Diagnostic Lifecycle Reset before ANY analysis execution
        com.example.data.GatewayDiagnostics.reset()
        com.example.data.GatewayDiagnostics.sourceUrl = rawUrl
        com.example.data.GatewayDiagnostics.loadedFunctionId = analysisType.canonical().name
        com.example.data.GatewayDiagnostics.loadedAnalysisType = analysisType.canonical().name

        val sourceTrigger = if (!directContent.isNullOrBlank()) "DIRECT_TEXT" else "MANUAL_URL"
        com.example.data.PipelineReportStore.startNewReport(sourceTrigger = sourceTrigger)

        if (analysisType == com.example.data.AnalysisType.GOOGLE_MAPS_ANALYZER) {
            val analysisId = java.util.UUID.randomUUID().toString() + "|" + analysisType.name
            _currentAnalysisType.value = analysisType
            _currentUrl.value = rawUrl
            _currentTitle.value = "Google Maps Analyzer (Stufe 1)"

            // Populate initial PipelineReport context for Google Maps
            com.example.data.PipelineReportStore.startStep("input_intake", "Input Intake", "URL: $rawUrl")
            com.example.data.PipelineReportStore.updateSection("input_intake") { map ->
                map["rawInput"] = rawUrl
                map["directContentProvided"] = "false"
            }
            com.example.data.PipelineReportStore.endStepPass("input_intake")
            
            com.example.data.PipelineReportStore.startStep("notation_and_id_resolution", "Notation and ID Resolution")
            com.example.data.PipelineReportStore.endStepPass("notation_and_id_resolution")
            
            com.example.data.PipelineReportStore.startStep("feature_routing", "Feature Routing")
            com.example.data.PipelineReportStore.endStepPass("feature_routing", notes = "Route zu Google Maps Sonderpfad.")
            
            com.example.data.PipelineReportStore.startStep("url_normalization", "URL Normalization")
            com.example.data.PipelineReportStore.updateSection("url_normalization") { map ->
                map["rawUrl"] = rawUrl
                map["trimmedUrl"] = rawUrl.trim()
            }
            com.example.data.PipelineReportStore.endStepPass("url_normalization")
            
            com.example.data.PipelineReportStore.endStepSkipped("engine_routing", "Google Maps Sonderpfad verwendet keine generische LLM Engine.")
            com.example.data.PipelineReportStore.endStepSkipped("prompt_loading", "Google Maps Sonderpfad übersprungen.")
            com.example.data.PipelineReportStore.endStepSkipped("gemini_request", "Übersprungen (API Call erfolgt über Places API).")
            com.example.data.PipelineReportStore.endStepSkipped("gemini_response", "Übersprungen.")
            com.example.data.PipelineReportStore.endStepSkipped("response_normalization", "Übersprungen.")

            _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA, analysisId)

            viewModelScope.launch {
                try {
                    val trimmedUrl = rawUrl.trim()
                    
                    if (!com.example.data.GoogleMapsUrlParser.isGoogleMapsUrl(trimmedUrl)) {
                        com.example.data.GatewayDiagnostics.failureStage = "MAPS_URL_VALIDATION"
                        com.example.data.GatewayDiagnostics.exceptionMessage = "Ungültiger Google Maps Link"
                        com.example.data.PipelineReportStore.endStepFail("extractor_selection", null, "Ungültiger Google Maps Link")
                        
                        appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                        com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                        com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                            map["finalStatus"] = "ERROR"
                            map["userVisibleErrorMessage"] = "Ungültiger Google Maps Link"
                        }
                        com.example.data.PipelineReportStore.endStepFail("final_result", null, "URL Validation Failed")

                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Ungültiger Google Maps Link",
                            detail = "Der eingegebene Link ist keine gültige Google Maps URL. Bitte verwende einen gültigen Google Maps Link (z.B. mit 'maps.app.goo.gl' oder 'google.com/maps').",
                            analysisId = analysisId
                        )
                        return@launch
                    }

                    _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA, analysisId)
                    
                    val pocResult = withContext(Dispatchers.IO) {
                        val resolved = com.example.data.GoogleMapsUrlParser.resolveShortUrl(trimmedUrl)
                        com.example.data.GoogleMapsUrlParser.parseGoogleMapsUrl(
                            originalText = trimmedUrl,
                            url = trimmedUrl,
                            resolvedUrl = resolved.first,
                            resolutionStatus = resolved.second
                        )
                    }

                    _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT, analysisId)

                    val placesResult = withContext(Dispatchers.IO) {
                        com.example.data.PlacesApiService.performStufe1Analysis(trimmedUrl, pocResult)
                    }

                    _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT, analysisId)

                    val isSuccessful = pocResult.resolutionStatus == "SUCCESS" && placesResult.apiStatus == "PLACE_DETAILS_SUCCESS"

                    if (isSuccessful) {
                        val mapperText = com.example.data.PlacesDataMapper.mapToGeminiInput(placesResult)
                        val canonicalInput = com.example.domain.model.CanonicalAnalysisInput(
                            sourceType = com.example.domain.model.SourceType.WEB,
                            rawText = trimmedUrl,
                            enrichedText = mapperText,
                            metadata = mapOf(
                                "url" to trimmedUrl,
                                "title" to (placesResult.displayName?.text ?: placesResult.urlDerivedName ?: "Google Maps Location")
                            ),
                            analysisId = analysisId,
                            analysisType = com.example.data.AnalysisType.GOOGLE_MAPS_ANALYZER
                        )

                        val summary = try {
                            analyzeContentUseCase.execute(canonicalInput)
                        } catch (geminiException: Exception) {
                            Log.e("MainViewModel", "Gemini analysis failed, executing fallback visual summary", geminiException)
                            
                            val takeaways = mutableListOf<com.example.domain.model.TakeawayItem>()
                            
                            val locationName = placesResult.displayName?.text ?: pocResult.placeName ?: "Unbekannter Ort"
                            takeaways.add(com.example.domain.model.TakeawayItem("Überblick & Konzept", "Konzeptanalyse für: $locationName. Eine KI-Analyse war aufgrund eines Verbindungsfehlers nicht möglich."))
                            
                            placesResult.formattedAddress?.let { 
                                takeaways.add(com.example.domain.model.TakeawayItem("Formatierte Adresse", it)) 
                            }
                            
                            placesResult.types?.let { types ->
                                if (types.isNotEmpty()) {
                                    takeaways.add(com.example.domain.model.TakeawayItem("Kategorien", types.joinToString(", ")))
                                }
                            }
                            
                            if (placesResult.rating != null) {
                                takeaways.add(com.example.domain.model.TakeawayItem("Bewertungen", "${placesResult.rating} von 5 Sternen (${placesResult.userRatingCount ?: 0} Bewertungen)"))
                            }
                            
                            if (placesResult.latitude != null && placesResult.longitude != null) {
                                takeaways.add(com.example.domain.model.TakeawayItem("Koordinaten", "Latitude: ${placesResult.latitude}, Longitude: ${placesResult.longitude}"))
                            }
                            
                            if (placesResult.warnings.isNotEmpty()) {
                                takeaways.add(com.example.domain.model.TakeawayItem("Informationen", placesResult.warnings.joinToString("\n")))
                            }

                            com.example.domain.model.DomainSummary(
                                id = "maps_fallback_${System.currentTimeMillis()}",
                                title = locationName,
                                originalUrl = trimmedUrl,
                                shortDescription = "Technische Datenansicht von $locationName (KI-Analyse-Fallback).",
                                keyTakeaways = takeaways,
                                analysisId = "maps_poc"
                            )
                        }

                        _uiState.value = UiState.Success(
                            summary = summary,
                            analysisType = com.example.data.AnalysisType.GOOGLE_MAPS_ANALYZER,
                            analysisId = analysisId
                        )

                        appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                        com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                        com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                            map["finalStatus"] = "SUCCESS"
                        }
                        com.example.data.PipelineReportStore.endStepPass(
                            "final_result",
                            "Summary generated successfully",
                            notes = "Google Maps Result: ${summary.title}"
                        )
                    } else {
                        val errorDetail = StringBuilder()
                        errorDetail.append("Status: ${pocResult.resolutionStatus}\n")
                        if (placesResult.warnings.isNotEmpty()) {
                            errorDetail.append("Meldungen: ${placesResult.warnings.joinToString()}\n")
                        } else {
                            errorDetail.append("Es konnten keine Ortsparameter extrahiert werden.\n")
                        }
                        val detailStr = errorDetail.toString().trim()
                        com.example.data.GatewayDiagnostics.failureStage = "MAPS_POC_ANALYSIS"
                        com.example.data.GatewayDiagnostics.exceptionMessage = "Google Maps Analyzer Fehler: $detailStr"
                        com.example.data.PipelineReportStore.endStepFail("source_http_fetch", null, detailStr)
                        
                        appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                        com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                        com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                            map["finalStatus"] = "ERROR"
                            map["userVisibleErrorMessage"] = "Google Maps Analyzer Fehler"
                        }
                        com.example.data.PipelineReportStore.endStepFail("final_result", null, detailStr)

                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Google Maps Analyzer Fehler",
                            detail = errorDetail.toString(),
                            analysisId = analysisId
                        )
                    }
                } catch (e: Exception) {
                    com.example.data.GatewayDiagnostics.failureStage = "MAPS_EXECUTION"
                    com.example.data.GatewayDiagnostics.exceptionMessage = e.localizedMessage ?: e.toString()
                    com.example.data.PipelineReportStore.endStepFail("source_http_fetch", e, "Maps execution exception")
                    
                    appContext?.let { com.example.data.PipelineReportStore.populateFromDiagnostics(it) }
                    com.example.data.PipelineReportStore.startStep("final_result", "Final Result")
                    com.example.data.PipelineReportStore.updateSection("final_result") { map ->
                        map["finalStatus"] = "ERROR"
                        map["userVisibleErrorMessage"] = e.localizedMessage ?: e.toString()
                    }
                    com.example.data.PipelineReportStore.endStepFail("final_result", e, "Maps execution exception")

                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Google Maps Analyzer Fehler",
                        detail = e.localizedMessage ?: e.toString(),
                        analysisId = analysisId
                    )
                }
            }
            return
        }

        val analysisId = java.util.UUID.randomUUID().toString() + "|" + analysisType.name
        _currentAnalysisType.value = analysisType
        _currentUrl.value = rawUrl
        _currentTitle.value = "Webseite analysieren"
        _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA, analysisId)
        android.util.Log.i("RUNTIME_SMOKE", "INPUT_SELECTED - AnalysisType: $analysisType, URL: $rawUrl, HasDirect: ${!directContent.isNullOrBlank()}")
        
        com.example.data.PipelineReportStore.startStep("input_intake", "Input Intake", "URL: $rawUrl")
        com.example.data.PipelineReportStore.updateSection("input_intake") { map ->
            map["rawInput"] = rawUrl
            map["rawInputLength"] = rawUrl.length
            map["inputTypeDetected"] = if (!directContent.isNullOrBlank()) "DIRECT_TEXT" else "WEB_URL"
            map["inputSource"] = if (!directContent.isNullOrBlank()) "DIRECT_TEXT" else "MANUAL_URL"
            map["receivedUrl"] = rawUrl
            map["urlVisibleInInputField"] = rawUrl
            map["inputAccepted"] = true
        }
        com.example.data.PipelineReportStore.endStepPass("input_intake", "Input accepted successfully", decision = "Execute ID resolution")

        com.example.data.PipelineReportStore.startStep("notation_and_id_resolution", "Notation and ID Resolution")
        com.example.data.PipelineReportStore.updateSection("notation_and_id_resolution") { map ->
            map["originalAnalysisType"] = analysisType.name
            map["canonicalAnalysisType"] = analysisType.canonical().name
            map["functionId"] = analysisType.canonical().name
            map["featureId"] = analysisType.canonical().name
            map["registryKey"] = analysisType.canonical().name
            map["promptKey"] = analysisType.canonical().name
            map["legacyTypeDetected"] = (analysisType != analysisType.canonical())
            map["legacyTypeValue"] = analysisType.name
            map["canonicalMappingApplied"] = true
            map["canonicalMappingSource"] = "Enum Canonical Mapping"
        }
        com.example.data.PipelineReportStore.endStepPass("notation_and_id_resolution", "ID resolution passed. Canonical Type: ${analysisType.canonical().name}", decision = "Perform feature routing")

        com.example.data.PipelineReportStore.startStep("feature_routing", "Feature Routing")
        com.example.data.PipelineReportStore.updateSection("feature_routing") { map ->
            val matchedFeat = com.example.ui.metadata.FeatureCatalog.features.find {
                it.functionId == analysisType.canonical().name || it.analysisType == analysisType.canonical()
            }
            val catName = matchedFeat?.let { feat ->
                com.example.ui.metadata.FeatureCatalog.categories.find { it.id == feat.category }?.name
            } ?: "Verstehen & Verdichten"
            map["selectedFeatureTitle"] = matchedFeat?.name ?: analysisType.canonical().name
            map["selectedFeatureCategory"] = catName
            map["acceptedInputs"] = matchedFeat?.acceptedInputs?.joinToString { it.name } ?: "WEB"
            map["featureEnabled"] = matchedFeat?.enabled ?: true
            map["featureVisible"] = matchedFeat?.visible ?: true
            map["routeSource"] = "UI Trigger"
            map["routeDecision"] = "Route to content extraction"
            map["routeTargetAnalysisType"] = analysisType.canonical().name
        }
        com.example.data.PipelineReportStore.endStepPass("feature_routing", "Feature routing successfully completed", decision = "Proceed to content extraction")

        viewModelScope.launch {
            try {
                if (analysisType.canonical() == com.example.data.AnalysisType.FREE_SOURCE_QUERY && freeQuery.isNullOrBlank()) {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Bitte stelle eine Frage zur Quelle.",
                        detail = "Die freie Quellenanfrage erfordert eine konkrete Angabe im Eingabefeld, damit die KI antworten kann.",
                        analysisId = analysisId
                    )
                    return@launch
                }

                _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA, analysisId)
                val extractionResult = extractContentUseCase.execute(rawUrl, directContent, analysisType, freeQuery, analysisId)

                when (extractionResult) {
                    is com.example.domain.model.ContentExtractionResult.Failure -> {
                        android.util.Log.e("RUNTIME_SMOKE", "RUNTIME_SMOKE_FAIL - Extraction failed: ${extractionResult.message}")
                        com.example.data.GatewayDiagnostics.failureStage = "CONTENT_EXTRACTION"
                        val isBlocked = extractionResult.errorType == com.example.domain.model.ContentExtractionResult.Failure.ErrorType.BLOCKED_SOURCE
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = isBlocked,
                            message = extractionResult.message,
                            detail = extractionResult.detail,
                            analysisId = analysisId
                        )
                    }
                    is com.example.domain.model.ContentExtractionResult.Predefined -> {
                        android.util.Log.i("RUNTIME_SMOKE", "EXTRACTION_SUCCESS - Predefined content loaded.")
                        _currentUrl.value = extractionResult.summary.originalUrl
                        _currentTitle.value = extractionResult.summary.title
                        _uiState.value = UiState.Success(extractionResult.summary, analysisType, analysisId)
                        android.util.Log.i("RUNTIME_SMOKE", "RESULT_RENDERED - Predefined content rendered")
                        android.util.Log.i("RUNTIME_SMOKE", "RUNTIME_SMOKE_PASS - Complete flow finished.")
                    }
                    is com.example.domain.model.ContentExtractionResult.Degraded -> {
                        _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT, analysisId)
                        _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT, analysisId)

                        val content = extractionResult.content
                        android.util.Log.i("RUNTIME_SMOKE", "EXTRACTION_DEGRADED - SourceType: ${content.sourceType}")
                        _currentUrl.value = content.metadata["url"] ?: rawUrl

                        val metadataHeader = """
                            [METADATEN-FALLBACK - KEIN TRANSKRIPT VERFÜGBAR]
                            Achtung: Dieses Video hat kein Transkript. Führe eine reine Metadatenanalyse auf Basis der verfügbaren Videobeschreibung und der Metadaten durch.
                            
                        """.trimIndent()

                        val finalInput = CanonicalAnalysisInput(
                            sourceType = content.sourceType,
                            rawText = metadataHeader + content.rawText,
                            enrichedText = metadataHeader + content.enrichedText,
                            metadata = content.metadata,
                            analysisId = analysisId
                        )
                        com.example.data.GatewayDiagnostics.sourceContentLengthSent = finalInput.rawText.length

                        val targetType = if (content.sourceType == SourceType.YOUTUBE && (analysisType == com.example.data.AnalysisType.STANDARD_WEBSEITE || analysisType == com.example.data.AnalysisType.WEB_SUMMARY)) {
                            com.example.data.AnalysisType.MULTIMEDIA_ANALYSIS
                        } else {
                            analysisType
                        }

                        try {
                            val summary = withContext(Dispatchers.IO) {
                                analyzeContentUseCase.execute(
                                    input = finalInput,
                                    useSearchGrounding = content.useSearchGrounding,
                                    analysisType = targetType,
                                    freeQuery = freeQuery
                                )
                            }
                            // Ensure fallbackUsed is set to true and preserve Gemini's generated shortDescription
                            val fallbackTitle = if (summary.title.isBlank() || summary.title == "Unbekannter Titel" || summary.title == "Video nicht auslesbar") {
                                content.metadata["title"]?.ifBlank { null } ?: summary.title
                            } else {
                                summary.title
                            }
                            val fallbackOwner = if (summary.owner.isNullOrBlank()) {
                                content.metadata["channel"]?.ifBlank { null }
                            } else {
                                summary.owner
                            }
                            val finalSummary = summary.copy(
                                title = fallbackTitle,
                                owner = fallbackOwner,
                                fallbackUsed = true
                            )
                            _currentTitle.value = finalSummary.title
                            _uiState.value = UiState.Success(finalSummary, targetType, analysisId)
                            android.util.Log.i("RUNTIME_SMOKE", "RESULT_RENDERED - Degraded analysis result rendered successfully")
                            android.util.Log.i("RUNTIME_SMOKE", "RUNTIME_SMOKE_PASS - Complete flow finished.")
                        } catch (e: Exception) {
                            android.util.Log.e("RUNTIME_SMOKE", "RUNTIME_SMOKE_FAIL - Degraded flow failed with exception: ${e.message}")
                            handleError(e, analysisId)
                        }
                    }
                    is com.example.domain.model.ContentExtractionResult.Success -> {
                        _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT, analysisId)
                        _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT, analysisId)

                        val content = extractionResult.content
                        android.util.Log.i("RUNTIME_SMOKE", "EXTRACTION_SUCCESS - SourceType: ${content.sourceType}")
                        _currentUrl.value = content.metadata["url"] ?: rawUrl

                        val finalInput = CanonicalAnalysisInput(
                            sourceType = content.sourceType,
                            rawText = content.rawText,
                            enrichedText = content.enrichedText,
                            metadata = content.metadata,
                            analysisId = analysisId
                        )
                        com.example.data.GatewayDiagnostics.sourceContentLengthSent = finalInput.rawText.length

                        val targetType = if (content.sourceType == SourceType.YOUTUBE && (analysisType == com.example.data.AnalysisType.STANDARD_WEBSEITE || analysisType == com.example.data.AnalysisType.WEB_SUMMARY)) {
                            com.example.data.AnalysisType.MULTIMEDIA_ANALYSIS
                        } else {
                            analysisType
                        }

                        try {
                            val summary = withContext(Dispatchers.IO) {
                                analyzeContentUseCase.execute(
                                    input = finalInput,
                                    useSearchGrounding = content.useSearchGrounding,
                                    analysisType = targetType,
                                    freeQuery = freeQuery
                                )
                            }
                            _uiState.value = UiState.Success(summary, targetType, analysisId)
                            android.util.Log.i("RUNTIME_SMOKE", "RESULT_RENDERED - Analysis result rendered successfully")
                            android.util.Log.i("RUNTIME_SMOKE", "RUNTIME_SMOKE_PASS - Complete flow finished.")
                        } catch (e: Exception) {
                            android.util.Log.e("RUNTIME_SMOKE", "RUNTIME_SMOKE_FAIL - Flow failed with exception: ${e.message}")
                            handleError(e, analysisId)
                        }
                    }
                }

            } catch (e: Exception) {
                handleError(e, analysisId)
            }
        }
    }

    private fun handleError(e: Throwable, analysisId: String) {
        val errorMsg = e.localizedMessage ?: ""
        val isHttpError = e is retrofit2.HttpException
        
        if (e is retrofit2.HttpException) {
            val code = e.code()
            val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (ex: Exception) { "" }
            
            if (code == 404 && (errorBody.contains("model is not found") || errorBody.contains("not found") || errorBody.contains("NOT_FOUND"))) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Gemini-Modell nicht verfügbar",
                    detail = "Die App versucht ein Gemini-Modell aufzurufen, das für diesen API-Endpunkt nicht verfügbar ist. Bitte prüfe die Modellkonfiguration der App.\n\nHTTP 404 / NOT_FOUND\nAPI-Antwort:\n$errorBody",
                    analysisId = analysisId
                )
            } else if (code == 429 || errorBody.contains("RESOURCE_EXHAUSTED") || errorBody.contains("quota")) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Gemini-Limit erreicht",
                    detail = "Das API-Anfragelimit (Quota/Billing) wurde überschritten oder dein Budget auf diesem API-Schlüssel ist erschöpft. Bitte prüfe deine Service-Limits und Kontingente im Google AI Studio.\n\nHTTP 429 / RESOURCE_EXHAUSTED\nAPI-Antwort:\n$errorBody",
                    analysisId = analysisId
                )
            } else if (code == 401 || code == 403 || errorBody.contains("API_KEY_INVALID") || errorBody.contains("INVALID_ARGUMENT") || errorBody.contains("unauthorized") || errorBody.contains("permission")) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = true,
                    message = "API-Key oder Berechtigung fehlerhaft",
                    detail = "Deine Anfrage wurde abgewiesen. Bitte prüfe deinen API-Key, dein Projekt, die Berechtigungen im Secrets panel oder ob die Abrechnung (Billing) korrekt eingerichtet ist.\n\nHTTP $code\nAPI-Antwort:\n$errorBody",
                    analysisId = analysisId
                )
            } else if (code == 503 || errorBody.contains("UNAVAILABLE") || errorBody.contains("experiencing high demand") || errorBody.contains("temporary")) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Gemini ist vorübergehend überlastet",
                    detail = "Das Gemini-Modell ist zurzeit überlastet (503 Service Unavailable / High Demand) und kann keine Anfragen entgegennehmen. Bitte versuche es in wenigen Minuten erneut.\n\nHTTP 503 / UNAVAILABLE\nAPI-Antwort:\n$errorBody",
                    analysisId = analysisId
                )
            } else {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Fehler bei der Gemini-KI-Anfrage (HTTP $code)",
                    detail = "Es gab ein Problem beim Aufrufen der Gemini-API.\n\nAPI-Antwort:\n$errorBody",
                    analysisId = analysisId
                )
            }
        } else if (e is IllegalArgumentException && e.message == "API_KEY_MISSING") {
            _uiState.value = UiState.Error(
                isPaywallOrBlocked = false,
                message = "Der Gemini API-Schlüssel fehlt oder ist ungültig.",
                detail = "Bitte trage deinen Google AI Studio API-Key im Secrets panel der AI Studio Benutzeroberfläche ein.",
                analysisId = analysisId
            )
        } else {
            val isDnsOrConnectError = e is java.net.UnknownHostException || e is java.net.ConnectException || 
                    errorMsg.contains("Unable to resolve host", ignoreCase = true) || 
                    errorMsg.contains("No address associated with hostname", ignoreCase = true) ||
                    errorMsg.contains("Failed to connect to", ignoreCase = true)
            val isContractViolation = errorMsg.contains("Contract violation", ignoreCase = true)
            val isParserOrContractFailure = errorMsg.contains("ParserFailure", ignoreCase = true) || 
                    errorMsg.contains("Validation failed", ignoreCase = true) || 
                    errorMsg.contains("STRUCTURED_EXTRACTION_FAILED", ignoreCase = true) || 
                    errorMsg.contains("unparsed JSON", ignoreCase = true) || 
                    isContractViolation
            val isBlocked = errorMsg.contains("403") || errorMsg.contains("401") || errorMsg.contains("blocked") || errorMsg.contains("Paywall") || errorMsg.contains("robots")
            val isTimeout = e is java.net.SocketTimeoutException || errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("timed out", ignoreCase = true)

            if (isDnsOrConnectError) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "KI-Dienst nicht erreichbar",
                    detail = "Die App konnte keine Verbindung zum Google Gemini-Dienst aufbauen.\n\nBitte prüfe, ob dein Smartphone eine aktive Internetverbindung hat und ob DNS-Anfragen für 'generativelanguage.googleapis.com' erlaubt sind (z. B. Adblocker oder Firmen-VPN deaktivieren).\n\nFehler: $errorMsg",
                    analysisId = analysisId
                )
            } else if (isParserOrContractFailure) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "KI-Ergebnis konnte nicht verarbeitet werden",
                    detail = "Die KI-Antwort enthielt ein nicht korrekt verarbeitetes JSON-Format.",
                    analysisId = analysisId
                )
            } else if (isTimeout) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Verbindungs-Timeout (Zeitüberschreitung)",
                    detail = "Das Google Search Grounding Tool von Gemini oder die Verbindung hat zu lange für die Live-Antwort gebraucht. Bei sehr detaillierten Webseiten oder Video-Suchen kann dies vorkommen.\n\nBitte klicke einfach auf „Erneut versuchen“!",
                    analysisId = analysisId
                )
            } else {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = isBlocked,
                    message = if (isBlocked) "Gesperrte Seite, kann nicht zusammengefasst werden" else "Inhalt konnte nicht geladen werden",
                    detail = errorMsg,
                    analysisId = analysisId
                )
            }
        }
    }

    fun summarizeFileUri(
        context: android.content.Context,
        uri: android.net.Uri,
        analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.DOCUMENT_SUMMARY
    ) {
        val analysisId = java.util.UUID.randomUUID().toString() + "|" + analysisType.name
        _currentAnalysisType.value = analysisType
        _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA, analysisId)
        
        com.example.data.PipelineReportStore.startNewReport(sourceTrigger = "FILE_PICKER")
        
        com.example.data.PipelineReportStore.startStep("input_intake", "Input Intake", "URI: $uri")
        com.example.data.PipelineReportStore.updateSection("input_intake") { map ->
            map["rawInput"] = uri.toString()
            map["rawInputLength"] = uri.toString().length
            map["inputTypeDetected"] = "FILE_URI"
            map["inputSource"] = "FILE_PICKER"
            map["receivedUrl"] = uri.toString()
            map["inputAccepted"] = true
        }
        com.example.data.PipelineReportStore.endStepPass("input_intake", "File input accepted", decision = "Execute ID resolution")

        com.example.data.PipelineReportStore.startStep("notation_and_id_resolution", "Notation and ID Resolution")
        com.example.data.PipelineReportStore.updateSection("notation_and_id_resolution") { map ->
            map["originalAnalysisType"] = analysisType.name
            map["canonicalAnalysisType"] = analysisType.canonical().name
            map["functionId"] = analysisType.canonical().name
            map["featureId"] = analysisType.canonical().name
            map["registryKey"] = analysisType.canonical().name
            map["promptKey"] = analysisType.canonical().name
            map["legacyTypeDetected"] = (analysisType != analysisType.canonical())
            map["legacyTypeValue"] = analysisType.name
            map["canonicalMappingApplied"] = true
            map["canonicalMappingSource"] = "Enum Canonical Mapping"
        }
        com.example.data.PipelineReportStore.endStepPass("notation_and_id_resolution", "ID resolution passed. Canonical Type: ${analysisType.canonical().name}", decision = "Perform feature routing")

        com.example.data.PipelineReportStore.startStep("feature_routing", "Feature Routing")
        com.example.data.PipelineReportStore.updateSection("feature_routing") { map ->
            val matchedFeat = com.example.ui.metadata.FeatureCatalog.features.find {
                it.functionId == analysisType.canonical().name || it.analysisType == analysisType.canonical()
            }
            val catName = matchedFeat?.let { feat ->
                com.example.ui.metadata.FeatureCatalog.categories.find { it.id == feat.category }?.name
            } ?: "Arbeiten mit Dateien"
            map["selectedFeatureTitle"] = matchedFeat?.name ?: analysisType.canonical().name
            map["selectedFeatureCategory"] = catName
            map["acceptedInputs"] = matchedFeat?.acceptedInputs?.joinToString { it.name } ?: "DOCUMENT"
            map["featureEnabled"] = matchedFeat?.enabled ?: true
            map["featureVisible"] = matchedFeat?.visible ?: true
            map["routeSource"] = "UI Trigger"
            map["routeDecision"] = "Route to file content extractor"
            map["routeTargetAnalysisType"] = analysisType.canonical().name
        }
        com.example.data.PipelineReportStore.endStepPass("feature_routing", "Feature routing completed", decision = "Proceed to content extraction")

        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(contentResolver, uri) ?: "Dokument"
                _currentUrl.value = uri.toString()
                _currentTitle.value = fileName
                _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT, analysisId)
                val mimeType = com.example.data.FileProcessingHelper.getMimeType(contentResolver, uri)
                val lower = fileName.lowercase()
                val isPdf = lower.endsWith(".pdf") || mimeType == "application/pdf"
                
                android.util.Log.d("PDF_DEBUG", "START: Dateiname: $fileName, URI: $uri, MIME-Type: $mimeType, PDF erkannt: $isPdf")
                println("PDF_DEBUG: START: Dateiname: $fileName, URI: $uri, MIME-Type: $mimeType, PDF erkannt: $isPdf")

                val bytes = withContext(Dispatchers.IO) {
                    com.example.data.FileProcessingHelper.readUriToByteArray(contentResolver, uri)
                }
                if (bytes == null) {
                    android.util.Log.e("PDF_DEBUG", "FEHLER: Konnte Bytes von URI nicht lesen.")
                    println("PDF_DEBUG: FEHLER: Konnte Bytes von URI nicht lesen.")
                    throw java.io.IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                }
                
                val byteSize = bytes.size
                android.util.Log.d("PDF_DEBUG", "Dateigröße: $byteSize Bytes")
                println("PDF_DEBUG: Dateigröße: $byteSize Bytes")

                if (byteSize > 20 * 1024 * 1024) {
                    throw java.io.IOException("FILE_TOO_LARGE")
                }

                val isImage = mimeType.startsWith("image/") || analysisType == com.example.data.AnalysisType.PHOTO_SCREENSHOT_ANALYSIS
                val summary = if (isPdf || isImage) {
                    if (isPdf) {
                        val useDirect = com.example.domain.usecase.AnalyzeContentUseCase.USE_DIRECT_PDF_PROCESSING
                        android.util.Log.d("PDF_DEBUG", "Direct-PDF-Pfad gewählt: $useDirect, lokaler PDF-Parser aufgerufen: ${!useDirect}")
                        println("PDF_DEBUG: Direct-PDF-Pfad gewählt: $useDirect, lokaler PDF-Parser aufgerufen: ${!useDirect}")
                    } else {
                        android.util.Log.d("PDF_DEBUG", "Bilddatei erkannt: $fileName, MIME-Type: $mimeType. Direkte Übergabe an multimodalen Gemini-Pfad.")
                        println("PDF_DEBUG: Bilddatei erkannt: $fileName, MIME-Type: $mimeType. Direkte Übergabe an multimodalen Gemini-Pfad.")
                    }
                    
                    _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT, analysisId)
                    withContext(Dispatchers.IO) {
                        analyzeContentUseCase.executeFromFile(bytes, mimeType, fileName, uri.toString(), analysisId, analysisType)
                    }
                } else {
                    android.util.Log.d("PDF_DEBUG", "Nicht-PDF/Nicht-Bild Datei erkannt. Fallback auf Standardextraktion.")
                    println("PDF_DEBUG: Nicht-PDF/Nicht-Bild Datei erkannt. Fallback auf Standardextraktion.")
                    if (com.example.data.FileProcessingHelper.isExtractableTextType(mimeType, fileName)) {
                        val extractedText = withContext(Dispatchers.IO) {
                            com.example.data.FileProcessingHelper.extractTextFromUri(contentResolver, uri, mimeType, fileName)
                        }
                        if (extractedText != null && extractedText.isNotBlank()) {
                            _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT, analysisId)
                            withContext(Dispatchers.IO) {
                                analyzeContentUseCase.executeFromText(extractedText, fileName, uri.toString(), analysisId)
                            }
                        } else {
                            throw java.io.IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                        }
                    } else {
                        throw java.io.IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                    }
                }

                // Check if Gemini returned INSUFFICIENT_DOCUMENT_CONTENT inside the fields
                val hasInsufficientCode = summary.title.contains("INSUFFICIENT_DOCUMENT_CONTENT") ||
                        summary.shortDescription.contains("INSUFFICIENT_DOCUMENT_CONTENT") ||
                        summary.keyTakeaways.any { 
                            it.title.contains("INSUFFICIENT_DOCUMENT_CONTENT") || 
                            it.details.contains("INSUFFICIENT_DOCUMENT_CONTENT") 
                        }
                
                if (hasInsufficientCode) {
                    android.util.Log.e("PDF_DEBUG", "Summary contains INSUFFICIENT_DOCUMENT_CONTENT")
                    throw java.io.IOException("INSUFFICIENT_DOCUMENT_CONTENT")
                }
                
                _uiState.value = UiState.Success(summary, analysisType, analysisId)
                return@launch
            } catch (e: IllegalArgumentException) {
                if (e.message == "API_KEY_MISSING") {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Der Gemini API-Schlüssel fehlt oder ist ungültig.",
                        detail = "Bitte trage deinen Google AI Studio API-Key im Secrets panel der AI Studio Benutzeroberfläche ein.",
                        analysisId = analysisId
                    )
                } else {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Fehler bei der Datei-Analyse.",
                        detail = e.localizedMessage,
                        analysisId = analysisId
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PDF_DEBUG", "Exception gefangen: ${e.message}", e)
                println("PDF_DEBUG: Exception gefangen: ${e.message}")
                val errorMsg = e.localizedMessage ?: ""
                val isDnsOrConnectError = e is java.net.UnknownHostException || e is java.net.ConnectException || 
                        errorMsg.contains("Unable to resolve host", ignoreCase = true) || 
                        errorMsg.contains("No address associated with hostname", ignoreCase = true) ||
                        errorMsg.contains("Failed to connect to", ignoreCase = true)
                val isTooLarge = e.message == "FILE_TOO_LARGE" || e.localizedMessage?.contains("FILE_TOO_LARGE") == true
                val isInsufficient = e.message == "INSUFFICIENT_DOCUMENT_CONTENT" || 
                        e.localizedMessage?.contains("INSUFFICIENT_DOCUMENT_CONTENT") == true
                val isStructuredFailed = e.message == "STRUCTURED_EXTRACTION_FAILED" ||
                        e.localizedMessage?.contains("STRUCTURED_EXTRACTION_FAILED") == true
                
                val detailMsg = if (isDnsOrConnectError) {
                    "Die App konnte keine Verbindung zum Google Gemini-Dienst aufbauen.\n\nBitte prüfe, ob dein Smartphone eine aktive Internetverbindung hat und ob DNS-Anfragen für 'generativelanguage.googleapis.com' erlaubt sind (z. B. Adblocker oder Firmen-VPN deaktivieren)."
                } else if (isTooLarge) {
                    "Das Dokument überschreitet die maximale Dateigröße von 20 MB."
                } else if (isInsufficient) {
                    "Das Dokument enthält keinen extrahierbaren Textlayer (z. B. ein gescanntes PDF ohne OCR oder ein reines Bild). Für die Analyse ist ein Dokument mit lesbarem Text erforderlich."
                } else if (isStructuredFailed) {
                    "Die strukturierte Datenextraktion aus dem Dokument ist fehlgeschlagen."
                } else {
                    e.localizedMessage ?: "Ein unbekannter Fehler ist aufgetreten."
                }
                val titleMsg = if (isDnsOrConnectError) {
                    "KI-Dienst nicht erreichbar"
                } else if (isTooLarge) {
                    "Datei zu groß"
                } else if (isInsufficient) {
                    "Inhalt unzureichend"
                } else if (isStructuredFailed) {
                    "Strukturierte Extraktion fehlgeschlagen"
                } else {
                    "Fehler bei der Datei-Zusammenfassung"
                }
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = titleMsg,
                    detail = detailMsg,
                    analysisId = analysisId
                )
            }
        }
    }

    private fun getFileName(contentResolver: android.content.ContentResolver, uri: android.net.Uri): String? {
        var name: String? = null
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return name ?: uri.lastPathSegment
    }
}
