package com.example.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
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

enum class LoadingStep {
    IDLE,
    FETCHING_DATA,
    ANALYZING_INPUT,
    GENERATING_OUTPUT,
    SUCCESS,
    ERROR
}

sealed interface UiState {
    object Idle : UiState
    data class Loading(val step: LoadingStep = LoadingStep.FETCHING_DATA) : UiState
    data class Success(val summary: DomainSummary, val analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.STANDARD_WEBSEITE) : UiState
    data class Error(val isPaywallOrBlocked: Boolean, val message: String, val detail: String? = null) : UiState
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

    fun initIfNeeded(context: android.content.Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        val db = com.example.data.local.AbstractorDatabase.getInstance(context.applicationContext)
        val api = com.example.data.remote.BackendApiService.create()
        
        val analysisRepo = com.example.data.repository.AnalysisRepositoryImpl(db, api)
        val syncRepo = com.example.data.repository.SyncRepositoryImpl(db, api)
        userRepository = com.example.data.repository.UserRepositoryImpl(db, api)
        
        analyzeContentUseCase = AnalyzeContentUseCase(analysisRepo)
        saveAnalysisUseCase = SaveAnalysisUseCase(analysisRepo)
        loadHistoryUseCase = LoadHistoryUseCase(analysisRepo)
        syncUserDataUseCase = SyncUserDataUseCase(syncRepo)
        
        isInitialized = true
        observeHistory()
        updatePendingSyncCount()
        updateActiveUser()

        // Auto-save successful analyses
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
        _uiState.value = UiState.Success(summary, com.example.data.AnalysisType.STANDARD_WEBSEITE)
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

    private val _currentAnalysisType = MutableStateFlow<com.example.data.AnalysisType>(com.example.data.AnalysisType.STANDARD_WEBSEITE)
    val currentAnalysisType: StateFlow<com.example.data.AnalysisType> = _currentAnalysisType

    var cachedDirectContent: String? = null

    fun resetToIdle() {
        _uiState.value = UiState.Idle
        _currentUrl.value = ""
        _currentTitle.value = ""
        _currentAnalysisType.value = com.example.data.AnalysisType.STANDARD_WEBSEITE
        cachedDirectContent = null
    }

    fun setAnalysisType(type: com.example.data.AnalysisType) {
        _currentAnalysisType.value = type
    }

    fun setSharedText(sharedText: String, intent: Intent? = null) {
        val extractedUrl = extractUrl(sharedText)
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
        if (restText.length > 50 && !isYoutubeUrl(cleanUrl)) {
            cachedDirectContent = sharedText
        } else {
            cachedDirectContent = null
        }
    }

    fun clearSharedUrlToFill() {
        _sharedUrlToFill.value = ""
    }

    fun processSharedText(sharedText: String, intent: Intent? = null) {
        setSharedText(sharedText, intent)
    }

    fun fetchSummary(rawUrl: String, directContent: String? = null, analysisType: com.example.data.AnalysisType = com.example.data.AnalysisType.STANDARD_WEBSEITE) {
        _currentAnalysisType.value = analysisType
        _currentUrl.value = rawUrl
        _currentTitle.value = "Webseite analysieren"
        _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA)
        viewModelScope.launch {
            try {
                // Pre-process & normalize URL input
                val extracted = extractUrl(rawUrl) ?: rawUrl.trim()
                var inputUrl = if (!extracted.startsWith("http://", ignoreCase = true) && !extracted.startsWith("https://", ignoreCase = true)) {
                    "https://$extracted"
                } else {
                    extracted
                }

                // Check for basic URL validity before redirect resolution
                if (!inputUrl.contains(".") || inputUrl.length < 5) {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Ungültige Webadresse eingegeben.",
                        detail = "Bitte stelle sicher, dass du eine vollständige Adresse eingegeben hast, z. B. „spiegel.de“ oder einen Link aus deinem Browser."
                    )
                    return@launch
                }

                // Resolve redirects (like lnkd.in, fb.me, t.co) on Dispatchers.IO to find the final canonical destination
                _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA)
                val url = withContext(Dispatchers.IO) {
                    try {
                        com.example.data.WebpageExtractor.resolveUrl(inputUrl)
                    } catch (e: Exception) {
                        inputUrl
                    }
                }

                val socialMediaRegex = Regex(
                    ".*(facebook\\.com|instagram\\.com|fb\\.watch|fb\\.com|fb\\.me|instagr\\.am).*",
                    RegexOption.IGNORE_CASE
                )
                if (socialMediaRegex.matches(inputUrl) || socialMediaRegex.matches(url)) {
                    _currentUrl.value = url
                    _currentTitle.value = "Inhalt geschützt"
                    _uiState.value = UiState.Success(
                        DomainSummary(
                            title = "Inhalt geschützt",
                            originalUrl = url,
                            shortDescription = "Social Media Seiten können aus Gründen der Vertraulichkeit nicht berücksichtigt werden.",
                            keyTakeaways = listOf(
                                TakeawayItem(title = "Plattform blockiert", details = "Die Plattform blockiert den externen Zugriff."),
                                TakeawayItem(title = "Manuelle Alternative", details = "Nutze für diese Inhalte bitte den manuellen Text-Upload oder die Zwischenablage.")
                            )
                        )
                    )
                    return@launch
                }

                if (isYoutubeUrl(url)) {
                    val videoId = extractYoutubeVideoId(url)
                    if (videoId == null) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Ungültige YouTube Video-ID",
                            detail = "Details: Eine gültige 11-stellige YouTube Video-ID konnte nicht aus dem Link extrahiert werden. Bitte prüfe das Format der URL."
                        )
                        return@launch
                    }

                    // Fetch the YouTube transcript on dispatcher IO
                    val transcript = withContext(Dispatchers.IO) {
                        try {
                            YoutubeTranscriptHelper.fetchTranscript(videoId)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Try to fetch oembed metadata as a strong context fallback if transcript fails or is too short
                    val oembedData = if (!hasEnoughRealContent(transcript)) {
                        withContext(Dispatchers.IO) {
                            try {
                                YoutubeTranscriptHelper.fetchOembedMetadata(videoId)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    } else {
                        null
                    }

                    if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
                        if (!hasEnoughRealContent(transcript) && oembedData == null) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auslesbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Seiteninhalt benötigt", details = "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden."),
                                        TakeawayItem(title = "Alternative", details = "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        }
                    } else if (analysisType == com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER) {
                        if (!hasEnoughRealContent(transcript) && oembedData == null) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auswertbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                        TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        }
                    } else if (analysisType == com.example.data.AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS) {
                        if (!hasEnoughRealContent(transcript) && oembedData == null) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auswertbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                        TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        }
                    }

                    _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)

                    // Call backend model with full context text if transcript is available,
                    // otherwise fall back to direct model fallback with detailed oembed metadata!
                    _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                    val summary = withContext(Dispatchers.IO) {
                        if (hasEnoughRealContent(transcript)) {
                            analyzeContentUseCase.execute(
                                url = url,
                                contentText = transcript,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        } else if (oembedData != null) {
                            val shortTranscriptText = if (!transcript.isNullOrBlank()) {
                                "\n- Fragmentarisches Transkript (sehr kurz): $transcript"
                            } else {
                                ""
                            }
                            val robustContentText = """
                                YOUTUBE-METADATEN-FALLBACK (Kein vollständiges Transkript verfügbar):
                                - Video-Titel: ${oembedData.first}
                                - YouTube-Kanal / Ersteller: ${oembedData.second}
                                - Video-ID: $videoId
                                - Original-URL: $url$shortTranscriptText
                                
                                WICHTIGE SICHERHEITS-INSTRUKTION FÜR DIE ANALYSE:
                                Es ist KEIN vollständiges Transkript verfügbar. Halluziniere KEINE fiktiven Videoinhalte oder Details, die nicht aus dem Titel, Kanal und dem eventuell vorhandenen kurzen Fragment hervorgehen.
                                Erstelle eine vorsichtige, informative Analyse ausschließlich auf Basis dieser Metadaten (Titel, Ersteller/Kanal, Thema).
                                Mache im Ausgabetext (z.B. in der Kurzbeschreibung oder als erster Punkt) unmissverständlich klar, dass für diese Analyse kein vollständiges Video-Transkript ausgelesen werden konnte und die Ergebnisse auf den verfügbaren Metadaten basieren.
                                Jede Behauptung über spezifischen gesprochenen Inhalt ohne Beleg ist zu vermeiden, um absolute Fakten-Integrität zu garantieren.
                            """.trimIndent()
                            
                            analyzeContentUseCase.execute(
                                url = url,
                                contentText = robustContentText,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        } else {
                            // If both failed, use direct model summarization (uses Gemini's internal knowledge of the URL)
                            analyzeContentUseCase.execute(
                                url = url,
                                contentText = null,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        }
                    }
                    _uiState.value = UiState.Success(summary, analysisType)

                } else {
                    // Standard webpage
                    if (!directContent.isNullOrBlank()) {
                        if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN && !hasEnoughRealContent(directContent)) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auslesbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Seiteninhalt benötigt", details = "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden."),
                                        TakeawayItem(title = "Alternative", details = "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        } else if (analysisType == com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER && !hasEnoughRealContent(directContent)) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auswertbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                        TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        } else if (analysisType == com.example.data.AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS && !hasEnoughRealContent(directContent)) {
                            _uiState.value = UiState.Success(
                                DomainSummary(
                                    title = "Inhalt nicht auswertbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                                    keyTakeaways = listOf(
                                        TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                        TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                        TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        }

                        // Der Nutzer hat direkt Text mitgeliefert (lokal abgefangen, Clipboard etc.)
                        // Wir verwenden diesen Text und deaktiveren Search Grounding!
                        _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)
                        _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                        val summary = withContext(Dispatchers.IO) {
                            analyzeContentUseCase.execute(
                                url = url,
                                contentText = directContent,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        }
                        _uiState.value = UiState.Success(summary, analysisType)
                    } else {
                        if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN ||
                            analysisType == com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER ||
                            analysisType == com.example.data.AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS
                        ) {
                            val scrapedText = withContext(Dispatchers.IO) {
                                try {
                                    WebpageExtractor.fetchContent(url)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (!scrapedText.isNullOrBlank() && hasEnoughRealContent(scrapedText)) {
                                _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)
                                _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                                val summary = withContext(Dispatchers.IO) {
                                    analyzeContentUseCase.execute(
                                        url = url,
                                        contentText = scrapedText,
                                        useSearchGrounding = false,
                                        analysisType = analysisType
                                    )
                                }
                                _uiState.value = UiState.Success(summary, analysisType)
                            } else {
                                // Direct scraping failed or has not enough content -> fall back to Google Search Grounding
                                _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)
                                _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                                try {
                                    val summary = withContext(Dispatchers.IO) {
                                        analyzeContentUseCase.execute(
                                            url = url,
                                            contentText = null,
                                            useSearchGrounding = true,
                                            analysisType = analysisType
                                        )
                                    }
                                    _uiState.value = UiState.Success(summary, analysisType)
                                } catch (e: Exception) {
                                    Log.w("MainViewModel", "Webpage Search Grounding failed for analysisType $analysisType", e)
                                    // Strictly no direct model fallback! Instantly show the appropriate error summary!
                                    if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
                                        _uiState.value = UiState.Success(
                                            DomainSummary(
                                                title = "Inhalt nicht auslesbar",
                                                originalUrl = url,
                                                shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                                keyTakeaways = listOf(
                                                    TakeawayItem(title = "Seiteninhalt benötigt", details = "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript."),
                                                    TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden."),
                                                    TakeawayItem(title = "Alternative", details = "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App.")
                                                )
                                            ),
                                            analysisType = analysisType
                                        )
                                    } else {
                                        _uiState.value = UiState.Success(
                                            DomainSummary(
                                                title = "Inhalt nicht auswertbar",
                                                originalUrl = url,
                                                shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                                                keyTakeaways = listOf(
                                                    TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                                    TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                                    TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                                                )
                                            ),
                                            analysisType = analysisType
                                        )
                                    }
                                }
                            }
                        } else {
                            // Check if it is a social media or walled platform
                            val isSocial = isSocialMediaOrWalledUrl(url)

                            // Try direct scraping first for super-fast execution
                            val scrapedText = withContext(Dispatchers.IO) {
                                try {
                                    WebpageExtractor.fetchContent(url)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)
                            _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                            val summary = withContext(Dispatchers.IO) {
                                if (!scrapedText.isNullOrBlank()) {
                                    // Scrape succeeded: Use normal summarization (instant and works on FREE tier!)
                                    analyzeContentUseCase.execute(
                                        url = url,
                                        contentText = scrapedText,
                                        useSearchGrounding = false,
                                        analysisType = analysisType
                                    )
                                } else if (isSocial) {
                                    // For social media, do NOT use search grounding to avoid long hangs or quota/blocked errors.
                                    // Instead, use a helpful diagnostic context telling the user how to easily copy and paste.
                                    val platformName = when {
                                        url.lowercase().contains("instagram") || url.lowercase().contains("instagr.am") -> "Instagram"
                                        url.lowercase().contains("facebook") || url.lowercase().contains("fb.") || url.lowercase().contains("fb/share") -> "Facebook"
                                        url.lowercase().contains("linkedin") || url.lowercase().contains("lnkd.in") -> "LinkedIn"
                                        url.lowercase().contains("tiktok") -> "TikTok"
                                        url.lowercase().contains("twitter") || url.lowercase().contains("x.com") || url.lowercase().contains("t.co") -> "X (Twitter)"
                                        url.lowercase().contains("threads") -> "Threads"
                                        url.lowercase().contains("pinterest") -> "Pinterest"
                                        url.lowercase().contains("xing") -> "Xing"
                                        else -> "Social Media"
                                    }
                                    val robustSocialContext = """
                                        SOZIALE NETZWERKE DIAGNOSE (Inhalte hinter Login-Schranke):
                                        - Plattform: ${'$'}platformName
                                        - Quell-URL: ${'$'}url
                                        
                                        WICHTIGER HINWEIS AN GEMINI KI:
                                        Da es sich um einen Link von ${'$'}platformName handelt, verlangt die Plattform eine Anmeldung/Login oder verhindert das Auslesen von externen Crawlern.
                                        
                                        Bitte generiere für den Nutzer auf DEUTSCH ein ansprechendes, klares Ergebnis im geforderten Daten-Schema.
                                        Erstelle folgende genaue Inhalte:
                                        1. title: "Geschützter Inhalt (${'$'}platformName)"
                                        2. original_url: "${'$'}url"
                                        3. short_description: "Da soziale Netzwerke wie ${'$'}platformName Anmeldeschranken besitzen, können wir diesen Link nicht direkt auslesen. Du kannst das aber ganz leicht umgehen!"
                                        4. key_takeaways (Bulletpoints auf Deutsch):
                                           - "Markiere den Beitragstext, das Profil oder die Details direkt in der passenden App oder im Browser."
                                           - "Kopiere den markierten Text in die Zwischenablage."
                                           - "Tippe hier im Abstractor auf 'Lösung für geschützte Seiten / Text analysieren', um den kopierten Inhalt sofort per KI auf Deutsch zusammenzufassen."
                                           - "Sicherheit & Privatsphäre: Dadurch umgehst du jede Passwortschranke sicher und vollkommen ohne Anmeldung."
                                    """.trimIndent()

                                    analyzeContentUseCase.execute(
                                        url = url,
                                        contentText = robustSocialContext,
                                        useSearchGrounding = false,
                                        analysisType = analysisType
                                    )
                                } else {
                                    // Scrape failed or page content is empty: Fall back to Google Search Grounding tool with retry fallback!
                                    try {
                                        analyzeContentUseCase.execute(
                                            url = url,
                                            contentText = null,
                                            useSearchGrounding = true,
                                            analysisType = analysisType
                                        )
                                    } catch (e: Exception) {
                                        Log.w("MainViewModel", "Webpage Search Grounding failed, falling back to direct model fallback", e)
                                        analyzeContentUseCase.execute(
                                            url = url,
                                            contentText = null,
                                            useSearchGrounding = false,
                                            analysisType = analysisType
                                        )
                                    }
                                }
                            }
                            _uiState.value = UiState.Success(summary, analysisType)
                        }
                    }
                }

            } catch (e: IllegalArgumentException) {
                if (e.message == "API_KEY_MISSING") {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Der Gemini API-Schlüssel fehlt oder ist ungültig.",
                        detail = "Bitte trage deinen Google AI Studio API-Key im Secrets panel der AI Studio Benutzeroberfläche ein."
                    )
                } else {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Fehler bei der Vorgabe-Verarbeitung.",
                        detail = e.localizedMessage
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                val isHttpError = e is retrofit2.HttpException
                
                if (isHttpError && e is retrofit2.HttpException) {
                    val code = e.code()
                    val errorBody = try { e.response()?.errorBody()?.string() ?: "" } catch (ex: Exception) { "" }
                    
                    if (code == 404 && (errorBody.contains("model is not found") || errorBody.contains("not found") || errorBody.contains("NOT_FOUND"))) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Gemini-Modell nicht verfügbar",
                            detail = "Die App versucht ein Gemini-Modell aufzurufen, das für diesen API-Endpunkt nicht verfügbar ist. Bitte prüfe die Modellkonfiguration der App.\n\nHTTP 404 / NOT_FOUND\nAPI-Antwort:\n$errorBody"
                        )
                    } else if (code == 429 || errorBody.contains("RESOURCE_EXHAUSTED") || errorBody.contains("quota")) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Gemini-Limit erreicht",
                            detail = "Das API-Anfragelimit (Quota/Billing) wurde überschritten oder dein Budget auf diesem API-Schlüssel ist erschöpft. Bitte prüfe deine Service-Limits und Kontingente im Google AI Studio.\n\nHTTP 429 / RESOURCE_EXHAUSTED\nAPI-Antwort:\n$errorBody"
                        )
                    } else if (code == 401 || code == 403 || errorBody.contains("API_KEY_INVALID") || errorBody.contains("INVALID_ARGUMENT") || errorBody.contains("unauthorized") || errorBody.contains("permission")) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = true,
                            message = "API-Key oder Berechtigung fehlerhaft",
                            detail = "Deine Anfrage wurde abgewiesen. Bitte prüfe deinen API-Key, dein Projekt, die Berechtigungen im Secrets panel oder ob die Abrechnung (Billing) korrekt eingerichtet ist.\n\nHTTP $code\nAPI-Antwort:\n$errorBody"
                        )
                    } else if (code == 503 || errorBody.contains("UNAVAILABLE") || errorBody.contains("experiencing high demand") || errorBody.contains("temporary")) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Gemini ist vorübergehend überlastet",
                            detail = "Das Gemini-Modell ist zurzeit überlastet (503 Service Unavailable / High Demand) und kann keine Anfragen entgegennehmen. Bitte versuche es in wenigen Minuten erneut.\n\nHTTP 503 / UNAVAILABLE\nAPI-Antwort:\n$errorBody"
                        )
                    } else {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Fehler bei der Gemini-KI-Anfrage (HTTP $code)",
                            detail = "Es gab ein Problem beim Aufrufen der Gemini-API.\n\nAPI-Antwort:\n$errorBody"
                        )
                    }
                } else {
                    // Regular network, paywall, scraping or timeout errors
                    val isBlocked = errorMsg.contains("403") || errorMsg.contains("401") || errorMsg.contains("blocked") || errorMsg.contains("Paywall") || errorMsg.contains("robots")
                    val isTimeout = e is java.net.SocketTimeoutException || errorMsg.contains("timeout", ignoreCase = true) || errorMsg.contains("timed out", ignoreCase = true)

                    if (isTimeout) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = false,
                            message = "Verbindungs-Timeout (Zeitüberschreitung)",
                            detail = "Das Google Search Grounding Tool von Gemini oder die Verbindung hat zu lange für die Live-Antwort gebraucht. Bei sehr detaillierten Webseiten oder Video-Suchen kann dies vorkommen.\n\nBitte klicke einfach auf „Erneut versuchen“!"
                        )
                    } else {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = isBlocked,
                            message = if (isBlocked) "Gesperrte Seite, kann nicht zusammengefasst werden" else "Inhalt konnte nicht geladen werden",
                            detail = errorMsg
                        )
                    }
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        return com.example.data.YoutubeUrlDecoder.extractUrl(text)
    }

    private fun hasEnoughRealContent(content: String?): Boolean {
        return !content.isNullOrBlank() && content.trim().length >= 500
    }

    private fun isYoutubeUrl(url: String): Boolean {
        return com.example.data.YoutubeUrlDecoder.isYoutubeUrl(url)
    }

    private fun isSocialMediaOrWalledUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") ||
               lower.contains("instagr.am") ||
               lower.contains("facebook.com") ||
               lower.contains("fb.watch") ||
               lower.contains("fb.com") ||
               lower.contains("fb.me") ||
               lower.contains("linkedin.com") ||
               lower.contains("lnkd.in") ||
               lower.contains("tiktok.com") ||
               lower.contains("twitter.com") ||
               lower.contains("x.com") ||
               lower.contains("t.co") ||
               lower.contains("threads.net") ||
               lower.contains("pinterest.com") ||
               lower.contains("xing.com")
    }

    private fun extractYoutubeVideoId(url: String): String? {
        return com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId(url)
    }

    fun summarizeFileUri(context: android.content.Context, uri: android.net.Uri) {
        _currentAnalysisType.value = com.example.data.AnalysisType.DOKUMENTE
        _uiState.value = UiState.Loading(LoadingStep.FETCHING_DATA)
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val fileName = getFileName(contentResolver, uri) ?: "Dokument"
                _currentUrl.value = uri.toString()
                _currentTitle.value = fileName
                _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)
                val mimeType = com.example.data.FileProcessingHelper.getMimeType(contentResolver, uri)
                
                if (com.example.data.FileProcessingHelper.isExtractableTextType(mimeType, fileName)) {
                    // Modern local text extraction bypasses multimodal binary upload issues entirely
                    val extractedText = withContext(Dispatchers.IO) {
                        com.example.data.FileProcessingHelper.extractTextFromUri(contentResolver, uri, mimeType, fileName)
                    }
                    if (extractedText != null && extractedText.isNotBlank()) {
                        _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                        val summary = withContext(Dispatchers.IO) {
                            analyzeContentUseCase.executeFromText(extractedText, fileName)
                        }
                        _uiState.value = UiState.Success(summary, com.example.data.AnalysisType.DOKUMENTE)
                        return@launch
                    }
                }

                // If not extractable as text locally (like PDF, images, etc.), use the multimodal pipeline
                // Check if the file type is supported organically by Gemini as raw inlineData
                val isImageOrPdf = mimeType.startsWith("image/", ignoreCase = true) || mimeType == "application/pdf"
                if (!isImageOrPdf) {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Dateiformat wird nicht vollständig unterstützt",
                        detail = "Die lokale XML-Text-Extraktion konnte für diese Datei keinen lesbaren Inhalt finden. Diese App unterstützt Textdateien, modern formatierte Microsoft Office-Dokumente (.docx, .xlsx, .pptx), PDF-Dokumente sowie Bilder (Screenshots/Fotos)."
                    )
                    return@launch
                }

                val bytes = withContext(Dispatchers.IO) {
                    com.example.data.FileProcessingHelper.readUriToByteArray(contentResolver, uri)
                }

                if (bytes == null || bytes.isEmpty()) {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Datei konnte nicht gelesen werden.",
                        detail = "Die ausgewählte Datei konnte von der App nicht geladen oder decodiert werden."
                    )
                    return@launch
                }

                _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                val summary = withContext(Dispatchers.IO) {
                    analyzeContentUseCase.executeFromFile(bytes, mimeType, fileName)
                }
                _uiState.value = UiState.Success(summary, com.example.data.AnalysisType.DOKUMENTE)
            } catch (e: IllegalArgumentException) {
                if (e.message == "API_KEY_MISSING") {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Der Gemini API-Schlüssel fehlt oder ist ungültig.",
                        detail = "Bitte trage deinen Google AI Studio API-Key im Secrets panel der AI Studio Benutzeroberfläche ein."
                    )
                } else {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Fehler bei der Datei-Analyse.",
                        detail = e.localizedMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    isPaywallOrBlocked = false,
                    message = "Fehler bei der Datei-Zusammenfassung",
                    detail = e.localizedMessage ?: "Ein unbekannter Fehler ist aufgetreten."
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
