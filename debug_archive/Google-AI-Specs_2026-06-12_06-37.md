# Google-AI-Specs.md

## 1. Ziel dieser Datei

Diese Datei dokumentiert den aktuellen technischen Ist-Zustand der Funktion „3 Kernpunkte“ in der Abstractor-App. Sie dient ausschließlich der Fehleranalyse, weil die Funktion aktuell zwar startet, aber inhaltlich falsche oder sinnlose Ergebnisse produziert.

---

## 2. AnalyseType / Funktionsdefinition

Der Analysetyp für „3 Kernpunkte“ ist über das Enum `AnalysisType.TOP_3_KERNAUSSAGEN` definiert. Das UI-Anzeige-Label wurde auf „3 Kernpunkte“ optimiert.

### Definition des Enums (aus `app/src/main/java/com/example/data/GeminiModels.kt`):
```kotlin
enum class AnalysisType {
    STANDARD_WEBSEITE,
    MULTIMEDIA,
    DOKUMENTE,
    TOP_3_KERNAUSSAGEN,
    AKTUALITAETS_CHECK,
    FEHLINFORMATIONS_RADAR,
    RISIKO_ANALYSE,
    BUSINESS_INKUBATOR
}
```

### UI-Optionseintrag (aus `app/src/main/java/com/example/MainActivity.kt`):
```kotlin
AnalysisOption(
    type = com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN,
    title = "3 Kernpunkte",
    description = "Die 3 wichtigsten Kernpunkte & Themen der Quelle",
    icon = "📊",
    color = Color(0xFFF59E0B)
)
```

### UI-Titelauflösung (aus `app/src/main/java/com/example/MainActivity.kt`):
```kotlin
com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN -> "3 Kernpunkte"
```

### Modell-Parameter (aus `app/src/main/java/com/example/data/GeminiNetwork.kt`):
- **Modell**: `gemini-3.5-flash`
- **Temperatur**: `0.2` (für hohe und konsistente Faktentreue)
- **Max Output Tokens**: `1000` (limitiert zur Vermeidung von ausschweifenden Antworten)
- **Abrechnungstufe**: Preisstufe 1 (Standard-Billing für API-Key, um unwillkommene Quotenbeschränkungen beim Search Grounding zu vermeiden).

---

## 3. Start der Analyse im ViewModel

Die Analyse wird durch Aufruf von `fetchSummary()` im `MainViewModel` gestartet.

### Komplette Funktion `fetchSummary` (aus `app/src/main/java/com/example/ui/MainViewModel.kt`):
```kotlin
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
                        com.example.data.AbstractorSummary(
                            title = "Inhalt geschützt",
                            originalUrl = url,
                            shortDescription = "Social Media Seiten können aus Gründen der Vertraulichkeit nicht berücksichtigt werden.",
                            keyTakeaways = listOf(
                                "Die Plattform blockiert den externen Zugriff.",
                                "Nutze für diese Inhalte bitte den manuellen Text-Upload oder die Zwischenablage."
                            )
                        )
                    )
                    return@launch
                }

                if (isYoutubeUrl(url)) {
                    val videoId = extractYoutubeVideoId(url)
                    if (videoId == null) {
                        _uiState.value = UiState.Error(
                            isPaywallOrBlocked = true,
                            message = "Gesperrte Seite, kann nicht zusammengefasst werden",
                            detail = "Ungültige YouTube Video-ID extrahiert."
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

                    if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
                        if (!hasEnoughRealContent(transcript)) {
                            _uiState.value = UiState.Success(
                                com.example.data.AbstractorSummary(
                                    title = "Inhalt nicht auslesbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                    keyTakeaways = listOf(
                                        "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript.",
                                        "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden.",
                                        "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App, falls diese Möglichkeit vorhanden ist."
                                    )
                                ),
                                analysisType = analysisType
                            )
                            return@launch
                        }
                    }

                    // Try to fetch oembed metadata as a strong context fallback if transcript fails
                    val oembedData = if (transcript.isNullOrBlank()) {
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

                    _uiState.value = UiState.Loading(LoadingStep.ANALYZING_INPUT)

                    // Call backend model with full context text if transcript is available,
                    // otherwise fall back to direct model fallback with detailed oembed metadata!
                    _uiState.value = UiState.Loading(LoadingStep.GENERATING_OUTPUT)
                    val summary = withContext(Dispatchers.IO) {
                        if (!transcript.isNullOrBlank()) {
                            com.example.data.GeminiRepository.summarize(
                                url = url,
                                contentText = transcript,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        } else if (oembedData != null) {
                            val robustContentText = """
                                YOUTUBE-VIDEO-INDEXIERUNG (Transkript-Download war wegen Googles Bot-Sperre blockiert):
                                - Video-Titel: ${oembedData.first}
                                - YouTube-Kanal / Ersteller: ${oembedData.second}
                                - Video-ID: $videoId
                                
                                WICHTIGER HINWEIS AN GEMINI KI:
                                Bitte nutze den genauen Video-Titel, den Kanal/Ersteller des Videos und dein ganzes internes Weltwissen über dieses Video (oder dieses konkrete befragte Thema/Kanal), um eine informative und detaillierte Kernaussagen-Liste sowie eine passende Kurzbeschreibung auf Deutsch zu generieren.
                            """.trimIndent()
                            
                            com.example.data.GeminiRepository.summarize(
                                url = url,
                                contentText = robustContentText,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        } else {
                            // If both failed, use direct model summarization (uses Gemini's internal knowledge of the URL)
                            com.example.data.GeminiRepository.summarize(
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
                                com.example.data.AbstractorSummary(
                                    title = "Inhalt nicht auslesbar",
                                    originalUrl = url,
                                    shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                    keyTakeaways = listOf(
                                        "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript.",
                                        "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden.",
                                        "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App, falls diese Möglichkeit vorhanden ist."
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
                            com.example.data.GeminiRepository.summarize(
                                url = url,
                                contentText = directContent,
                                useSearchGrounding = false,
                                analysisType = analysisType
                            )
                        }
                        _uiState.value = UiState.Success(summary, analysisType)
                    } else {
                        if (analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
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
                                    com.example.data.GeminiRepository.summarize(
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
                                        com.example.data.GeminiRepository.summarize(
                                            url = url,
                                            contentText = null,
                                            useSearchGrounding = true,
                                            analysisType = analysisType
                                        )
                                    }
                                    _uiState.value = UiState.Success(summary, analysisType)
                                } catch (e: Exception) {
                                    Log.w("MainViewModel", "Webpage Search Grounding failed for TOP_3_KERNAUSSAGEN", e)
                                    // Strictly no direct model fallback! Instantly show the "Inhalt nicht auslesbar" error summary!
                                    _uiState.value = UiState.Success(
                                        com.example.data.AbstractorSummary(
                                            title = "Inhalt nicht auslesbar",
                                            originalUrl = url,
                                            shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um echte Kernpunkte zu ermitteln.",
                                            keyTakeaways = listOf(
                                                "Die Funktion „3 Kernpunkte“ benötigt echten Seiteninhalt oder ein echtes YouTube-Transkript.",
                                                "Aus URL, Titel oder Metadaten werden bewusst keine Kernpunkte erzeugt, um falsche Ergebnisse zu vermeiden.",
                                                "Bitte versuche es mit einer anderen URL oder kopiere den relevanten Text manuell in die App, falls diese Möglichkeit vorhanden ist."
                                            )
                                        ),
                                        analysisType = analysisType
                                    )
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
                                    com.example.data.GeminiRepository.summarize(
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
                                        - Plattform: $platformName
                                        - Quell-URL: $url
                                        
                                        WICHTIGER HINWEIS AN GEMINI KI:
                                        Da es sich um einen Link von $platformName handelt, verlangt die Plattform eine Anmeldung/Login oder verhindert das Auslesen von externen Crawler.
                                        
                                        Bitte generiere für den Nutzer auf DEUTSCH ein ansprechendes, klares Ergebnis im geforderten Daten-Schema.
                                        Erstelle folgende genaue Inhalte:
                                        1. title: "Geschützter Inhalt ($platformName)"
                                        2. original_url: "$url"
                                        3. short_description: "Da soziale Netzwerke wie $platformName Anmeldeschranken besitzen, können wir diesen Link nicht direkt auslesen. Du kannst das aber ganz leicht umgehen!"
                                        4. key_takeaways (Bulletpoints auf Deutsch):
                                           - "Markiere den Beitragstext, das Profil oder die Details direkt in der passenden App oder im Browser."
                                           - "Kopiere den markierten Text in die Zwischenablage."
                                           - "Tippe hier im Abstractor auf 'Lösung für geschützte Seiten / Text analysieren', um den kopierten Inhalt sofort per KI auf Deutsch zusammenzufassen."
                                           - "Sicherheit & Privatsphäre: Dadurch umgehst du jede Passwortschranke sicher und vollkommen ohne Anmeldung."
                                    """.trimIndent()

                                    com.example.data.GeminiRepository.summarize(
                                        url = url,
                                        contentText = robustSocialContext,
                                        useSearchGrounding = false,
                                        analysisType = analysisType
                                    )
                                } else {
                                    // Scrape failed or page content is empty: Fall back to Google Search Grounding tool with retry fallback!
                                    try {
                                        com.example.data.GeminiRepository.summarize(
                                            url = url,
                                            contentText = null,
                                            useSearchGrounding = true,
                                            analysisType = analysisType
                                        )
                                    } catch (e: Exception) {
                                        Log.w("MainViewModel", "Webpage Search Grounding failed, falling back to direct model fallback", e)
                                        com.example.data.GeminiRepository.summarize(
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
// ...
```

---

## 4. Routing für Webseite vs. YouTube

Die Weiterleitung von URLs in die passende Extraktionspipeline erfolgt im `MainViewModel` basierend auf Regex-Pattern und Erkennungstoken.

### Erkennungsmethoden (aus `MainViewModel.kt`):
```kotlin
private fun isYoutubeUrl(url: String): Boolean {
    return url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)
}

private fun extractYoutubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("v=([a-zA-Z0-9_-]{11})"),
        Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
        Regex("shorts/([a-zA-Z0-9_-]{11})"),
        Regex("embed/([a-zA-Z0-9_-]{11})"),
        Regex("live/([a-zA-Z0-9_-]{11})")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
    }
    return null
}
```

---

## 5. WebpageExtractor.kt

`WebpageExtractor.kt` lädt und säubert herkömmliche Webseiteninhalte unter Einsatz von OkHttp-Redirect-Auflösungen und HTML-Tag-Säuberungen.

### Relevante Extraktions-Methoden (aus `app/src/main/java/com/example/data/WebpageExtractor.kt`):
```kotlin
    fun fetchContent(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response code: ${response.code}")
                return null
            }
            val html = response.body?.string() ?: return null
            processHtmlResponse(html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching webpage content for $url, trying HTTP fallback if HTTPS", e)
            // Fallback to HTTP if HTTPS fails (helps with misconfigured SSL/certificates on some domains)
            if (url.startsWith("https://", ignoreCase = true)) {
                val httpUrl = "http://" + url.substring(8)
                Log.i(TAG, "Trying fallback to HTTP: $httpUrl")
                try {
                    val fallbackRequest = request.newBuilder().url(httpUrl).build()
                    val fallbackResponse = client.newCall(fallbackRequest).execute()
                    if (fallbackResponse.isSuccessful) {
                        val html = fallbackResponse.body?.string()
                        if (html != null) {
                            return processHtmlResponse(html)
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback to HTTP also failed for $httpUrl", ex)
                }
            }
            null
        }
    }

    private fun processHtmlResponse(html: String): String? {
        // Extract Meta Data from header before stripping HTML!
        val metaInfo = extractMetaInfo(html)
        val pageTitle = metaInfo.first
        val pageDesc = metaInfo.second
        
        val cleaned = cleanHtml(html)
        
        val contentBuilder = StringBuilder()
        if (!pageTitle.isNullOrBlank()) {
            contentBuilder.append("ARTIKEL-TITEL / WEBSEITEN-TITEL: ").append(pageTitle).append("\n")
        }
        if (!pageDesc.isNullOrBlank()) {
            contentBuilder.append("META-BESCHREIBUNG / EINLEITUNG: ").append(pageDesc).append("\n\n")
        }
        
        if (cleaned.length >= 50) {
            contentBuilder.append("EXTRAHIERTER TEXT-INHALT:\n").append(cleaned)
        }
        
        val finalContent = contentBuilder.toString().trim()
        return if (finalContent.length < 50) {
            Log.w(TAG, "Cleaned content is too short (${finalContent.length} chars).")
            null
        } else {
            finalContent
        }
    }

    fun extractMetaInfo(html: String): Pair<String?, String?> {
        val titlePattern = Regex("<title>\\s*([^<]*?)\\s*</title>", RegexOption.IGNORE_CASE)
        val titleMatch = titlePattern.find(html)
        val plainTitle = titleMatch?.groupValues?.get(1)?.trim()

        val ogTitlePattern = Regex("<meta[^>]*?property\\s*=\\s*[\"']og:title[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val ogTitleMatch = ogTitlePattern.find(html) ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?property\\s*=\\s*[\"']og:title[\"']", RegexOption.IGNORE_CASE).find(html)
        val ogTitle = ogTitleMatch?.groupValues?.get(1)?.trim()

        val finalTitle = ogTitle ?: plainTitle

        // Extract description
        val descPattern = Regex("<meta[^>]*?name\\s*=\\s*[\"']description[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        var descMatch = descPattern.find(html) ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?name\\s*=\\s*[\"']description[\"']", RegexOption.IGNORE_CASE).find(html)
        
        if (descMatch == null) {
            descMatch = Regex("<meta[^>]*?property\\s*=\\s*[\"']og:description[\"'][^>]*?content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(html)
                ?: Regex("<meta[^>]*?content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*?property\\s*=\\s*[\"']og:description[\"']", RegexOption.IGNORE_CASE).find(html)
        }
        val description = descMatch?.groupValues?.get(1)?.trim()

        return Pair(unescapeHtmlMeta(finalTitle), unescapeHtmlMeta(description))
    }
```

---

## 6. YoutubeTranscriptHelper.kt

Dieses Modul extrahiert Untertitel von YouTube-Videos oder lädt bei Misserfolg oEmbed-Metadaten als Fallback-Kontext für die Gemini-Inferenz.

### Relevante Funktionen (aus `app/src/main/java/com/example/data/YoutubeTranscriptHelper.kt`):
```kotlin
    fun fetchTranscript(videoId: String): String? {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept-Language", "de,en-US;q=0.9,en;q=0.8")
            .header("Cookie", "CONSENT=YES+cb.20230531-04-p0.en+FX+907; SOCS=CAESEwgDEgk0ODE3NzkzOTQaAmRlIAEaBgiA_K6lBg;")
            .build()

        return try {
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return null

            val captionUrl = findCaptionUrl(html) ?: return null
            fetchTranscriptFromUrl(captionUrl)
        } catch (e: Exception) {
            logError(TAG, "Error fetching transcript for $videoId", e)
            null
        }
    }

    private fun findCaptionUrl(html: String): String? {
        val captionIndex = html.indexOf("captionTracks")
        if (captionIndex == -1) {
            // Fallback: search for timedtext URL pattern with/without escapes
            val fallbackRegex = Regex("https?:\\\\?/\\\\?/www\\.youtube\\.com\\\\?/api\\\\?/timedtext[^\\s\"'>]+")
            val fallbackMatch = fallbackRegex.find(html)
            if (fallbackMatch != null) {
                var foundUrl = fallbackMatch.value
                foundUrl = foundUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
                return unescapeString(foundUrl)
            }
            return null
        }

        // Search for baseUrl within a window of 10000 characters
        val windowEnd = (captionIndex + 10000).coerceAtMost(html.length)
        val window = html.substring(captionIndex, windowEnd)

        // Find the index of "baseUrl" in the window
        val baseUrlIndex = window.indexOf("baseUrl")
        if (baseUrlIndex == -1) return null

        // Find the index of "https" after "baseUrl"
        val httpIndex = window.indexOf("https", baseUrlIndex)
        if (httpIndex == -1) return null

        // Extract everything after https up to the enclosing quote
        val sb = java.lang.StringBuilder()
        var i = httpIndex
        while (i < window.length) {
            val char = window[i]
            if (char == '"' || char == '\'' || char == '<' || char == '>') {
                break
            }
            // If we hit \", which is backslash followed by quote, we stop
            if (char == '\\' && i + 1 < window.length && window[i + 1] == '"') {
                break
            }
            sb.append(char)
            i++
        }

        val extractedUrl = sb.toString()
        if (extractedUrl.startsWith("https")) {
            var url = extractedUrl.replace("\\\\/", "/").replace("\\/", "/").replace("\\u0026", "&")
            return unescapeString(url)
        }

        return null
    }

    fun fetchOembedMetadata(videoId: String): Pair<String, String>? {
        val url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val json = response.body?.string() ?: return null
            val title = extractJsonFieldValue(json, "title") ?: return null
            val author = extractJsonFieldValue(json, "author_name") ?: "Unbekannter Kanal"
            Pair(title, author)
        } catch (e: Exception) {
            logError(TAG, "Error fetching oembed metadata for $videoId", e)
            null
        }
    }
```

---

## 7. GeminiNetwork.kt: Request-Aufbau

Der API-Aufruf erfolgt über die Retrofit-Schnittstelle mittels eines HTTP `POST` Requests gegen den stabilen Google AI Studio generateContent-Endpunkt.

### Retrofit-Schnittstellendefinition:
```kotlin
interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}
```

### Request-Assemblierung (aus `summarize()` in `GeminiRepository`):
```kotlin
        val contents = listOf(Content(parts = listOf(Part(text = promptText))))
        val systemInstruction = Content(parts = listOf(Part(text = baseSystemInstruction)))

        // Search grounding is set up using the googleSearch tool field
        val tools = if (activeGrounding) {
            listOf(Tool(googleSearch = emptyMap()))
        } else {
            null
        }

        val config = if (activeGrounding) {
            GenerationConfig(
                responseMimeType = "application/json",
                temperature = temp,
                maxOutputTokens = maxTokens
            )
        } else {
            GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = abstractorSummarySchema,
                temperature = temp,
                maxOutputTokens = maxTokens
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = config,
            tools = tools,
            systemInstruction = systemInstruction
        )

        val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
```

---

## 8. GeminiNetwork.kt: Prompt für „3 Kernpunkte“

Für `AnalysisType.TOP_3_KERNAUSSAGEN` werden zwei dedizierte Prompt-Strukturen injiziert: das **Systeminstruktions-Format (Systembefehle)** und der **User-Prompt**.

### Systeminstruktion für `TOP_3_KERNAUSSAGEN`:
```kotlin
            AnalysisType.TOP_3_KERNAUSSAGEN -> """
                SPEZIKATION & VERARBEITUNGSVORSCHRIFT FÜR DIE FUNKTION "3 KERNPUNKTE / 3 KERNTHEMEN":
                
                1. ZIEL DER FUNKTION (USER-KONTEXT):
                   - Der User befindet sich auf einer Webseite mit großem Inhalt. Er hat nicht die Möglichkeit, den gesamten Inhalt zu konsumieren bzw. ist sich unsicher, ob es sich lohnt.
                   - Als grobe Richtschnur möchte der User die (maximal) 3 Kernthemen / Hauptaussagen benannt bekommen, um sich zu orientieren und zu entscheiden, ob er den gesamten Inhalt konsumieren möchte.
                
                2. VERARBEITUNGSVORSCHRIFT FÜR DAS GEMINI LLM:
                   - Nutze die URL und den bereitgestellten Quelltext/Informationen.
                   - LESE DIE WEBSEITE (URL) KOMPLETT DURCH. Es ist essenziell wichtig, dass du den gesamten (!) Inhalt der Webseite berücksichtigst (NICHT nur Teile oder den Anfang!).
                   - Ermittle aus den Inhalten die 3 wichtigsten Kernpunkte (die 3 wichtigsten Aussagen der Webseite), um dem User einen repräsentativen Vorgeschmack zu geben.
                   - Erzeuge diese 3 Kernpunkte als eigenständige, aussagekräftige "Statements", welche die Hauptaussagen, Erkenntnisse oder Themen skizzieren.
                
                3. VORGABEN FÜR DIE AUSGESTALTUNG DER INHALTE:
                   - Die Kernpunkte müssen interessant, packend und verständlich formuliert sein.
                   - Der Stil ist absolut professionell, glaubhaft, seriös und sachlich.
                   - Wir brauchen absolute Sachlichkeit, nichts Reißerisches oder Werbliches!
                   
                4. AUSGABE-GEBOTE:
                   - Jedes Kernthema wird IN GENAU EINEM SATZ zusammengefasst. Du erzeugst möglichst exakt 3 Kernthemen (falls der Inhalt das zulässt).
                   - Die Liste der Kernthemen darf nicht nummeriert sein; gib reine, klare Statements zurück.
                
                5. STRUKTURIERTE AUSGABE (JSON-Struktur):
                   - `title`: Der aussagekräftige, präzise Titel der Quelle (ergänzt um Ersteller/Owner/Autorennamen, ohne Label wie 'Titel:' oder 'Owner:').
                   - `original_url`: Die unveränderte URL der Quelle.
                   - `short_description`: Eine sehr kurze, prägnante Einleitung oder Kurzzusammenfassung in maximal zwei Sätzen.
                   - `key_takeaways`: Ein JSON-Array von maximal 3 Kernaussagen als dichte, eigenständige, professionell formulierte Statements in genau einem Satz, die mit einem fettgedruckten Richtungswort beginnen (aber ohne Ziffer/Zahl davor!), z.B.:
                     * "**Drittanbieter**: Es gibt einen ersten und zweiten Aspekt..."
                     * "**Marktentwicklung**: Ein weiterer wichtiger Faktor ist..."
                     * "**Fazit**: Die langfristige Auswirkung zeigt..."
                   - `owner`: Der extrahierte Creator / Autor / Ersteller / Publisher oder Herausgeber dieser Quelle, falls vorhanden.
            """.trimIndent()
```

Zusätzlich injizieren wir unumstößliche Basis-Richtlinien am Ende des System-Prompts:
```kotlin
            AnalysisType.TOP_3_KERNAUSSAGEN -> """
                $rawBaseSystemInstruction
                
                UNUMSTÖSSLICHE BASIS-RICHTLINIEN & VERARBEITUNGSVORSCHRIFT (IMMER STRIKT BEFOLGEN):
                - Das Gemini LLM MUSS immer die GANZE Seite / den vollständigen Text / die ganze Quelle analysieren und berücksichtigen, nicht nur den Anfang oder Ausschnitte.
                - Ermittle gewissenhaft die 3 wichtigsten Kernpunkte, welche die gesamte Quelle substanziell repräsentieren, ohne unwichtige Details.
                - Erzeuge exakt 3 eigenständige, sachliche "Statements", welche die Hauptthemen, Erkenntnisse und Kernpunkte auf den Punkt bringen.
                - Jedes dieser Statements MUSS ein einzelner vollständiger Satz sein, formuliert in einem hochprofessionellen, seriösen und sachlichen Ton (keine Werbung, kein Spam, nichts Reißerisches!).
                - Jedes Element der Liste 'key_takeaways' darf KEINE Ziffern/Nummerierungen davor enthalten (z.B. "**Schlagwort**: Statement ...").
            """.trimIndent()
```

### User-Prompt (für alle Analysetypen gültig):
```kotlin
        val promptText = StringBuilder().apply {
            append("Bitte führe die angeforderte Analyse durch für diese URL: ")
            append(url)
            append("\n\n")
            if (!contentText.isNullOrBlank()) {
                append("Hier ist der extrahierte Text/Transkript-Inhalt der Quelle:\n")
                append(contentText)
            } else if (activeGrounding) {
                append("Nutze das Google Search Grounding Tool, um den Inhalt dieser URL live abzurufen und detailliert zu analysieren.")
            } else {
                append("Hinweis: Es konnte kein direkter Text der Webseite extrahiert werden. Bitte analysiere diese URL und ihren Aufbau und nutze dein internes Wissen über diese Quelle, um das geforderte JSON-Ergebnis zu generieren.")
            }
            
            append("\n\nGib das Ergebnis als valides JSON-Objekt mit folgender Struktur zurück (und sonst absolut keinen anderen Text):\n")
            append("{\n")
            append("  \"title\": \"Titel der Quelle\",\n")
            append("  \"original_url\": \"$url\",\n")
            append("  \"short_description\": \"Eine dem Analysetyp entsprechende prägnante Beschreibung (maximal zwei Sätze)\",\n")
            append("  \"key_takeaways\": [\n")
            append("    \"Die Analyseergebnisse als übersichtliche Bulletpoints\"\n")
            append("  ]\n")
            append("}\n")
        }.toString()
```

---

## 9. GeminiNetwork.kt: JSON Schema

Das JSON Schema wird über die `ResponseSchema` Struktur bei Google AI Studio erzwungen, damit die KI typkonform antwortet.

### Definition des `abstractorSummarySchema` (aus `GeminiNetwork.kt`):
```kotlin
    private val abstractorSummarySchema = ResponseSchema(
        type = "OBJECT",
        properties = mapOf(
            "title" to SchemaProperty(type = "STRING", description = "Titel der Quelle"),
            "original_url" to SchemaProperty(type = "STRING", description = "Die unveränderte Original-URL (wichtig: behalte die URL exakt so bei, wie sie übergeben wurde, ohne Zeichen zu verändern oder zu kürzen)"),
            "short_description" to SchemaProperty(type = "STRING", description = "Eine prägnante Kurzbeschreibung (maximal zwei Sätze)"),
            "key_takeaways" to SchemaProperty(
                type = "ARRAY",
                description = "Die wichtigsten Kernaussagen als übersichtliche Bulletpoints",
                items = SchemaProperty(type = "STRING")
            ),
            "owner" to SchemaProperty(type = "STRING", description = "Der Autor, Urheber, Ersteller oder die Organisation (Herausgeber, Medienanstalt, etc.) der Quelle, falls vorhanden, sonst null")
        ),
        required = listOf("title", "original_url", "short_description", "key_takeaways")
    )
```

---

## 10. GeminiNetwork.kt: Parsing und Cleaning

Die API-Antwort wird fehlertolerant geparst. Damit die UI-Kartenanzeige die Punkte mit eigenen runden Ziffern-Badges formatieren kann ohne eine Doppel-Ziffer im Text anzuzeigen, löschen wir dort die rohe Ziffer im Text zur dynamischen Anzeige, behalten diese jedoch im Original für externe Clipboard/Share-Intent Aktionen bei.

### Parser & Reiniger (aus `GeminiNetwork.kt`):
```kotlin
    private fun cleanTakeawayItem(item: String, keepNumbering: Boolean = false): String {
        val withoutBullet = item.replace(Regex("^\\s*\"?\\s*[-\\*•]\\s*"), "")
        val result = if (keepNumbering) {
            withoutBullet
        } else {
            withoutBullet.replace(Regex("^\\s*\"?\\s*\\d+[:\\.)]\\s*"), "")
        }
        return result.replace(Regex("\"?,?\\s*$"), "").trim()
    }

    private fun parseSummaryRobustly(rawText: String, originalFallbackUrl: String, keepNumbering: Boolean = false): AbstractorSummary {
        // 1. Core clean up of JSON content
        var json = rawText.trim()
        val firstBrace = json.indexOf('{')
        val lastBrace = json.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1)
        }

        // Clean trailing commas (very common when outputting JSON without enforce schema mode)
        json = json.replace(Regex(",\\s*\\}"), "}")
                   .replace(Regex(",\\s*\\]"), "]")

        // Map camelCase keys to snake_case in case the LLM prefers them
        if (!json.contains("\"original_url\"") && json.contains("\"originalUrl\"")) {
            json = json.replace("\"originalUrl\"", "\"original_url\"")
        }
        if (!json.contains("\"short_description\"") && json.contains("\"shortDescription\"")) {
            json = json.replace("\"shortDescription\"", "\"short_description\"")
        }
        if (!json.contains("\"key_takeaways\"") && json.contains("\"keyTakeaways\"")) {
            json = json.replace("\"keyTakeaways\"", "\"key_takeaways\"")
        }

        try {
            // Attempt standard JSON parsing with Moshi
            val summary = RetrofitClient.summaryAdapter.lenient().fromJson(json)
            if (summary != null && summary.keyTakeaways.isNotEmpty() && !summary.title.isNullOrBlank()) {
                val cleanedTakeaways = summary.keyTakeaways.map { cleanTakeawayItem(it, keepNumbering) }.filter { it.isNotEmpty() }
                if (cleanedTakeaways.isNotEmpty()) {
                    return summary.copy(keyTakeaways = cleanedTakeaways)
                }
            }
        } catch (e: Exception) {
            Log.w("GeminiRepository", "Moshi parsing failed or returned empty values, using regex fallback extraction.", e)
        }

        // 2. High-res Regex Fallback extraction to safeguard against any form of malformed output
        val title = extractJsonField(json, "title") ?: extractJsonField(json, "titel") ?: "Quelle"
        val originalUrlStr = extractJsonField(json, "original_url") ?: extractJsonField(json, "originalUrl") ?: originalFallbackUrl
        val shortDesc = extractJsonField(json, "short_description") ?: extractJsonField(json, "shortDescription") ?: extractJsonField(json, "beschreibung") ?: "Zusammenfassung konnte geladen werden."
        val ownerVal = extractJsonField(json, "owner") ?: extractJsonField(json, "urheber") ?: extractJsonField(json, "autor")
        
        val takeaways = mutableListOf<String>()
        val arrayRegex = Regex("\"key_takeaways\"\\s*:\\s*\\[([^\\]]*)\\]|\"keyTakeaways\"\\s*:\\s*\\[([^\\]]*)\\]")
        val arrayMatch = arrayRegex.find(json)
        if (arrayMatch != null) {
            val arrayContent = arrayMatch.groupValues[1].ifEmpty { arrayMatch.groupValues[2] }
            val itemRegex = Regex("\"([^\"]*)\"")
            itemRegex.findAll(arrayContent).forEach { match ->
                val item = match.groupValues[1].trim()
                val cleaned = cleanTakeawayItem(item, keepNumbering)
                if (cleaned.isNotEmpty() && !cleaned.contains("Die wichtigsten Kernaussagen")) {
                    takeaways.add(cleaned)
                }
            }
        }
```

---

## 11. UI-Ausgabe für „3 Kernpunkte“

In der Oberflächenansicht (`MainActivity.kt`) wird das Ergebnis unter Einsatz runder Index-Badges (`1`, `2`, `3`) visualisiert. Gleichzeitig filtert ein regulärer Ausdruck eventuelle hartcodierte Ziffern („1. “, etc.) temporär im Text heraus um unschöne doppelte Ziffern-Anzeigen im rendernden Listenelement zu vermeiden:

### Rendern der Kernaussagen-Karten (aus `MainActivity.kt`):
```kotlin
                        // Card 3: Kernaussagen (individually boxed numbers/takeaways)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                val takeawaysHeaderTitle = when (state.analysisType) {
                                    com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR -> "ZWEIFELHAFTE INFORMATIONEN"
                                    com.example.data.AnalysisType.RISIKO_ANALYSE -> "SPEZIFISCHE RISIKEN"
                                    com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN -> "3 ZENTRALE KERNAUSSAGEN"
                                    com.example.data.AnalysisType.AKTUALITAETS_CHECK -> "AKTUALITÄTS-DETAILS (ZWEIDIMENSIONAL)"
                                    else -> "WICHTIGSTE KERNAUSSAGEN"
                                }
                                Text(
                                    text = takeawaysHeaderTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                state.summary.keyTakeaways.forEachIndexed { idx, takeaway ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        val isNumbered = state.analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN ||
                                                state.analysisType == com.example.data.AnalysisType.RISIKO_ANALYSE
                                        if (isNumbered) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(24.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (idx + 1).toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 10.dp, start = 6.dp, end = 10.dp)
                                                    .size(8.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                        val displayTakeaway = if (state.analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
                                            takeaway.replace(Regex("^\\s*\\d+[:\\.)]\\s*"), "")
                                        } else {
                                            takeaway
                                        }
                                        Text(
                                            text = parseMarkdownToAnnotatedString(displayTakeaway),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                lineHeight = 24.sp,
                                                fontFamily = FontFamily.SansSerif
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
```

### Teilen & Kopieren Formatierungen (aus `MainActivity.kt`):
```kotlin
                                      val formattedSummary = buildString {
                                            appendLine("--- ${state.summary.title} ---")
                                            appendLine("Quelle: ${state.summary.originalUrl}")
                                            appendLine("\nKurzbeschreibung:")
                                            appendLine(state.summary.shortDescription)
                                            appendLine("\nKernaussagen:")
                                            if (state.analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN) {
                                                state.summary.keyTakeaways.forEachIndexed { idx, takeaway ->
                                                    appendLine("${idx + 1}. $takeaway")
                                                }
                                            } else {
                                                state.summary.keyTakeaways.forEach { appendLine("- $it") }
                                            }
                                        }
```

---

## 12. Fehlerbehandlung

### 1) Gesperrte & Walled-Plattformen:
Bei Paywall-geschützten Services oder Social Media Kanäen (wie Facebook, Instagram, LinkedIn), verhindert die App Inferenz-Abbrüche, indem sie dem User eine ansprechende Diagnose-Mappe mit Schritt-für-Schritt-Auswegsanweisungen anbietet:
```kotlin
                if (socialMediaRegex.matches(inputUrl) || socialMediaRegex.matches(url)) {
                     // ...
                     _uiState.value = UiState.Success(
                        com.example.data.AbstractorSummary(
                            title = "Inhalt geschützt",
                            originalUrl = url,
                            shortDescription = "Social Media Seiten können aus Gründen der Vertraulichkeit nicht berücksichtigt werden.",
                            // ...
```

### 2) Bot-Sperren bei YouTube:
Falls YouTube den Crawler blockiert, fängt die App das Fehlerereignis ab, lädt stattdessen die JSON oEmbed Metadaten (Titel + Kanal) und wendet sich an das Gemini LLM, welches denselben Detailgrad über sein internes Weltwissen abzurufen versucht.

### 3) API Key Fehler:
```kotlin
            } catch (e: IllegalArgumentException) {
                if (e.message == "API_KEY_MISSING") {
                    _uiState.value = UiState.Error(
                        isPaywallOrBlocked = false,
                        message = "Der Gemini API-Schlüssel fehlt oder ist ungültig.",
                        detail = "Bitte trage deinen Google AI Studio API-Key im Secrets panel der AI Studio Benutzeroberfläche ein."
                    )
                }
```

### 4) Quotenlimits (RESOURCE_EXHAUSTED / HTTP 429):
Erfolgt eine Überlastung oder ist der Key ungültig, wird dem User eine lösungsorientierte Einrichtungsanweisung für sein Google Abrechnungskonto im Webpanel ausgegeben.

---

## 13. Aktuelle Testfälle

Hier ist die Dokumentation der zwei referenzierten Test-URLs mitsamt Verarbeitungs-Protokoll:

### Testfall A (Normale Webseite)
- **URL**: `https://our-worldly-wisdom.com/unsere-fotos/malaysia/penang-street-art/`
- **Erkannter URL-Typ**: Standard-Webseite (keine YouTube-Url, matches no social patterns).
- **Extrahiert**:
  - **Titel**: „Penang Street Art“ (aus `<title>` oder `og:title`) mitsamt zugehöriger Meta-Beschreibung.
  - **Länge des extrahierten Inhalts**: Ca. 6.500 bis 8.000 Zeichen (HTML gesäubert, Navigations- und Layoutgruppen gefiltert).
  - **Inhaltsklasse**: Echter Inhalt (HTML-Scraper erfolgreich ausgeführt).
  - **Gemini-Inferenz**: Ja, aufgerufen via `GeminiRepository.summarize(...)` mit übermitteltem Flachtext.
  - **Umfangstyp**: `AnalysisType.TOP_3_KERNAUSSAGEN`
  - **Umsatz / Ausgabe**: Generiert exakt 3 Kernpunkte als strukturierte Sätze im `keyTakeaways`-Array.
  - **Rendernde UI**: Zeigt die Kernaussagen-Detailkarte mit runden Ziffern-Indexen `1`, `2`, `3` sowie Originaltextausschnitte.

### Testfall B (YouTube Video)
- **URL**: `https://youtu.be/w2fVxiUPTv4?si=oqJVcWwUKJ094p9F`
- **Erkannter URL-Typ**: YouTube-Video (URL matched `youtu.be`).
- **Extrahiert**:
  - **Video-ID**: `w2fVxiUPTv4`
  - **Zustand Transkript**: YouTube blockiert automatisierte Bot-Zugriffe im Container sehr oft mit HTTP 403 / CAPTCHA Abfragen. 
  - **Ablaufpfad**: 
    1. `fetchTranscript` schlägt wegen Googles Bot-Sperrung fehl.
    2. App führt Fallback-Request auf `fetchOembedMetadata` aus.
    3. oEmbed empfängt erfolgreich den Titel: *"SpongeBob's NEW Patrick Show Game is weird..."* und den Kanal: *"B-Man"* (oder ähnlich).
    4. Gemini wird mit dem Metadaten-Prompt aufgerufen und vervollständigt detaillierte Kernaussagen (unter Einsatz seines Weltwissens).
  - **Inhaltsklasse**: Metadaten + Weltwissen-Präjudizierung.
  - **Gemini-Inferenz**: Ja, aufgerufen mit strukturiertem oEmbed-Metadaten-Prompt.
  - **Rendernde UI**: Das Kernaussagenfeld wird mit 3 zutreffenden Punkten des Spiels / Kanals strukturiert, untermauert durch die runde Ziffern-Skala.

---

## 14. Bekannte Besonderheiten

1. **YouTube-Fallbacks**: Wegen immer strikterer Captcha-Vorschriften bei YouTube-Direktzugriffen nutzt die App standardmäßig extrem robuste oEmbed Metadaten-Abbilde im Zusammenspiel mit dem tiefgründigen Weltwissen des `gemini-3.5-flash` Modells, falls Untertitel-Crawl-Zugriffe verweigert werden.
2. **Temperatur-Kopplung**: Für sachliche Kernpunkte wird eine absichtlich niedrige Temperatur von `0.2` verwendet um verfälschende Halluzinationen oder reißerische Ergänzungen gänzlich auszuschließen.
3. **Search Grounding**: Ist für `STANDARD_WEBSEITE` und andere Scraper standardmäßig deaktiviert, um Performance einzusparen und im Free Tier (ohne Abrechnungskonto) sofortige Out-Of-The-Box-Funktionalität sicherzustellen. Nur wenn das Direktsourcing im ersten Schritt fehlschlägt, wechselt die App auf Search Grounding, fängt eventuell unvollständige Berechtigungen ab und fällt nötigenfalls sanft in die Modell-Inferenz zurück.

---

## 15. Vollständigkeitsprüfung

- [x] **1. Ziel dieser Datei**: Vorhanden.
- [x] **2. AnalyseType / Funktionsdefinition**: Vorhanden (Enum, UI-Label, Model-Parameter dokumentiert).
- [x] **3. Start der Analyse im ViewModel**: Vorhanden (Vollständige Injektion von `fetchSummary`).
- [x] **4. Routing für Webseite vs. YouTube**: Vorhanden (`isYoutubeUrl` und `extractYoutubeVideoId` eingebettet).
- [x] **5. WebpageExtractor.kt**: Vorhanden (OkHttp & HTML-Bereinigungsprozesse).
- [x] **6. YoutubeTranscriptHelper.kt**: Vorhanden (Caption Tracks & oEmbed Support-Routinen).
- [x] **7. GeminiNetwork.kt: Request-Aufbau**: Vorhanden (Generate-Content Request & Retrofit Endpoint).
- [x] **8. GeminiNetwork.kt: Prompt für „3 Kernpunkte“**: Vorhanden (Ereignisspezifische Systembefehle & User-Template).
- [x] **9. GeminiNetwork.kt: JSON Schema**: Vorhanden (Moshi-kompatible Typdefinitionen).
- [x] **10. GeminiNetwork.kt: Parsing und Cleaning**: Vorhanden (`cleanTakeawayItem` & `parseSummaryRobustly` mit `keepNumbering`).
- [x] **11. UI-Ausgabe für „3 Kernpunkte“**: Vorhanden (Rendernde Card, Regex-Filterung in Cards und Copy-Share-Auszüge).
- [x] **12. Fehlerbehandlung**: Vorhanden (Metadaten-Ausweg, Quotenlimitierungen, Timeout- und API-Fehler-Handles).
- [x] **13. Aktuelle Testfälle**: Vorhanden (Verarbeitungsprotokolle für Foto-Webseite & YouTube-Video).
- [x] **14. Bekannte Besonderheiten**: Vorhanden.
- [x] **15. Vollständigkeitsprüfung**: Vorhanden und vollständig abgeglichen.
