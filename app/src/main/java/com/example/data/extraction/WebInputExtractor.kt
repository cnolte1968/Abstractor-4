package com.example.data.extraction

import com.example.data.AnalysisType
import com.example.data.GoogleMapsUrlParser
import com.example.data.WebpageExtractor
import com.example.domain.model.CapabilityState
import com.example.domain.model.CapabilityStatus
import com.example.domain.model.ContentExtractionResult
import com.example.domain.model.ExtractedContent
import com.example.domain.model.SourceCapability
import com.example.domain.model.SourcePlatform
import com.example.domain.model.SourceProfile
import com.example.domain.model.SourceType
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import java.util.UUID

class WebInputExtractor : InputExtractor {

    override fun supports(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): Boolean {
        // WebInputExtractor handles any standard web address
        val lower = normalizedUrl.lowercase()
        return (lower.startsWith("http://") || lower.startsWith("https://"))
    }

    override suspend fun extract(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType,
        freeQuery: String?,
        analysisId: String
    ): ContentExtractionResult {
        com.example.data.GatewayDiagnostics.sourceUrl = rawUrl
        com.example.data.GatewayDiagnostics.normalizedSourceUrl = normalizedUrl

        if (isGoogleMapsUrl(rawUrl, normalizedUrl)) {
            val parsed = GoogleMapsUrlParser.parseGoogleMapsUrl(rawUrl, rawUrl, normalizedUrl, "SUCCESS")
            val placeTitle = parsed.placeName ?: parsed.searchQuery ?: "Google Maps Ort"
            val mapsContent = "Google Maps Ortskontext: $placeTitle\nURL: $normalizedUrl"
            return ContentExtractionResult.Success(
                ExtractedContent(
                    sourceType = SourceType.WEB,
                    rawText = mapsContent,
                    enrichedText = mapsContent,
                    metadata = mapOf(
                        "url" to normalizedUrl,
                        "placeName" to placeTitle,
                        "latitude" to (parsed.latitude?.toString() ?: ""),
                        "longitude" to (parsed.longitude?.toString() ?: ""),
                        "address" to (parsed.address ?: "")
                    ),
                    useSearchGrounding = false,
                    confirmedProfile = buildConfirmedProfile(rawUrl, normalizedUrl, isSuccess = true)
                )
            )
        }

        val socialMediaRegex = Regex(
            ".*(facebook\\.com|instagram\\.com|fb\\.watch|fb\\.com|fb\\.me|instagr\\.am).*",
            RegexOption.IGNORE_CASE
        )

        if (socialMediaRegex.matches(rawUrl) || socialMediaRegex.matches(normalizedUrl)) {
            return ContentExtractionResult.Predefined(
                DomainSummary(
                    id = UUID.randomUUID().toString(),
                    title = "Inhalt geschützt",
                    originalUrl = normalizedUrl,
                    shortDescription = "Social Media Seiten können aus Gründen der Vertraulichkeit nicht berücksichtigt werden.",
                    keyTakeaways = listOf(
                        TakeawayItem(title = "Plattform blockiert", details = "Die Plattform blockiert den externen Zugriff."),
                        TakeawayItem(title = "Manuelle Alternative", details = "Nutze für diese Inhalte bitte den manuellen Text-Upload oder die Zwischenablage.")
                    ),
                    analysisId = analysisId
                )
            )
        }

        val canonicalType = analysisType.canonical()
        if (canonicalType == AnalysisType.KEY_TAKEAWAYS ||
            canonicalType == AnalysisType.FACTS_VS_OPINIONS ||
            canonicalType == AnalysisType.PERSPECTIVES_COUNTERPOSITIONS ||
            canonicalType == AnalysisType.FREE_SOURCE_QUERY
        ) {
            var scrapeException: Exception? = null
            val scrapedInput = try {
                WebpageExtractor.extractWebpageContent(normalizedUrl)
            } catch (e: Exception) {
                scrapeException = e
                null
            }

            if (scrapedInput != null && hasEnoughRealContent(scrapedInput.rawText)) {
                return ContentExtractionResult.Success(
                    ExtractedContent(
                        sourceType = scrapedInput.sourceType,
                        rawText = scrapedInput.rawText,
                        enrichedText = scrapedInput.enrichedText,
                        metadata = scrapedInput.metadata,
                        useSearchGrounding = false,
                        confirmedProfile = buildConfirmedProfile(rawUrl, normalizedUrl, isSuccess = true)
                    )
                )
            } else {
                if (canonicalType == AnalysisType.FREE_SOURCE_QUERY) {
                    if (scrapeException != null) {
                        return mapScrapeException(scrapeException)
                    }
                    return ContentExtractionResult.Predefined(
                        DomainSummary(
                            id = UUID.randomUUID().toString(),
                            title = "Quelle nicht auslesbar",
                            originalUrl = normalizedUrl,
                            shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden.",
                            keyTakeaways = listOf(
                                TakeawayItem(title = "Inhalt benötigt", details = "Um deine Frage zur Quelle beantworten zu können, muss der Text ausgelesen werden können."),
                                TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus der URL alleine können keine präzisen Antworten ermittelt werden."),
                                TakeawayItem(title = "Alternative", details = "Bitte kopiere den Text manuell in das Textfeld, um die Analyse durchzuführen.")
                            ),
                            analysisId = analysisId
                        )
                    )
                } else if (canonicalType == AnalysisType.FACTS_VS_OPINIONS) {
                    if (scrapeException != null) {
                        return mapScrapeException(scrapeException)
                    }
                    return ContentExtractionResult.Predefined(
                        DomainSummary(
                            id = UUID.randomUUID().toString(),
                            title = "Inhalt nicht auswertbar",
                            originalUrl = normalizedUrl,
                            shortDescription = "Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.",
                            keyTakeaways = listOf(
                                TakeawayItem(title = "Inhalt benötigt", details = "Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle."),
                                TakeawayItem(title = "Keine Metadaten-Generierung", details = "Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt."),
                                TakeawayItem(title = "Alternative", details = "Bitte prüfe die URL oder versuche eine andere Quelle.")
                            ),
                            analysisId = analysisId
                        )
                    )
                } else {
                    if (scrapeException != null) {
                        return mapScrapeException(scrapeException)
                    }
                    // Direct scraping failed/not enough content -> fallback to Google Search Grounding
                    return ContentExtractionResult.Success(
                        ExtractedContent(
                            sourceType = SourceType.WEB,
                            rawText = scrapedInput?.rawText ?: "",
                            enrichedText = scrapedInput?.enrichedText ?: "",
                            metadata = mapOf("url" to normalizedUrl),
                            useSearchGrounding = true,
                            confirmedProfile = buildConfirmedProfile(rawUrl, normalizedUrl, isSuccess = false, detailMessage = "Direkt-Scraping fehlgeschlagen / Fallback verwendet")
                        )
                    )
                }
            }
        } else {
            // Other analysis types (e.g. Standard Webpage)
            val isSocial = isSocialMediaOrWalledUrl(normalizedUrl)

            var scrapeException: Exception? = null
            val scrapedInput = try {
                WebpageExtractor.extractWebpageContent(normalizedUrl)
            } catch (e: Exception) {
                scrapeException = e
                null
            }

            if (scrapedInput != null && hasEnoughRealContent(scrapedInput.rawText)) {
                return ContentExtractionResult.Success(
                    ExtractedContent(
                        sourceType = scrapedInput.sourceType,
                        rawText = scrapedInput.rawText,
                        enrichedText = scrapedInput.enrichedText,
                        metadata = scrapedInput.metadata,
                        useSearchGrounding = false,
                        confirmedProfile = buildConfirmedProfile(rawUrl, normalizedUrl, isSuccess = true)
                    )
                )
            } else if (isSocial) {
                val platformName = when {
                    normalizedUrl.lowercase().contains("instagram") || normalizedUrl.lowercase().contains("instagr.am") -> "Instagram"
                    normalizedUrl.lowercase().contains("facebook") || normalizedUrl.lowercase().contains("fb.") || normalizedUrl.lowercase().contains("fb/share") -> "Facebook"
                    normalizedUrl.lowercase().contains("linkedin") || normalizedUrl.lowercase().contains("lnkd.in") -> "LinkedIn"
                    normalizedUrl.lowercase().contains("tiktok") -> "TikTok"
                    normalizedUrl.lowercase().contains("twitter") || normalizedUrl.lowercase().contains("x.com") || normalizedUrl.lowercase().contains("t.co") -> "X (Twitter)"
                    normalizedUrl.lowercase().contains("threads") -> "Threads"
                    normalizedUrl.lowercase().contains("pinterest") -> "Pinterest"
                    normalizedUrl.lowercase().contains("xing") -> "Xing"
                    else -> "Social Media"
                }
                val robustSocialContext = """
                    SOZIALE NETZWERKE DIAGNOSE (Inhalte hinter Login-Schranke):
                    - Plattform: $platformName
                    - Quell-URL: $normalizedUrl
                    
                    WICHTIGER HINWEIS AN GEMINI KI:
                    Da es sich um einen Link von $platformName handelt, verlangt die Plattform eine Anmeldung/Login oder verhindert das Auslesen von externen Crawlern.
                    
                    Bitte generiere für den Nutzer auf DEUTSCH ein ansprechendes, klares Ergebnis im geforderten Daten-Schema.
                    Erstelle folgende genaue Inhalte:
                    1. title: "Geschützter Inhalt ($platformName)"
                    2. original_url: "$normalizedUrl"
                    3. short_description: "Da soziale Netzwerke wie $platformName Anmeldeschranken besitzen, können wir diesen Link nicht direkt auslesen. Du kannst das aber ganz leicht umgehen!"
                    4. key_takeaways (Bulletpoints auf Deutsch):
                       - "Markiere den Beitragstext, das Profil oder die Details direkt in der passenden App oder im Browser."
                       - "Kopiere den markierten Text in die Zwischenablage."
                       - "Tippe hier im Relevantor auf 'Lösung für geschützte Seiten / Text analysieren', um den kopierten Inhalt sofort per KI auf Deutsch zusammenzufassen."
                       - "Sicherheit & Privatsphäre: Dadurch umgehst du jede Passwortschranke sicher und vollkommen ohne Anmeldung."
                """.trimIndent()

                return ContentExtractionResult.Success(
                    ExtractedContent(
                        sourceType = SourceType.WEB,
                        rawText = robustSocialContext,
                        enrichedText = robustSocialContext,
                        metadata = mapOf("url" to normalizedUrl),
                        useSearchGrounding = false,
                        confirmedProfile = buildSocialConfirmedProfile(rawUrl, normalizedUrl, platformName)
                    )
                )
            } else {
                if (scrapeException != null) {
                    return mapScrapeException(scrapeException)
                }
                // Webpage direct scrape empty/failed -> try Google Search Grounding fallback
                return ContentExtractionResult.Success(
                    ExtractedContent(
                        sourceType = SourceType.WEB,
                        rawText = scrapedInput?.rawText ?: "",
                        enrichedText = scrapedInput?.enrichedText ?: "",
                        metadata = mapOf("url" to normalizedUrl),
                        useSearchGrounding = true,
                        confirmedProfile = buildConfirmedProfile(rawUrl, normalizedUrl, isSuccess = false, detailMessage = "Direkt-Scraping fehlgeschlagen / Fallback verwendet")
                    )
                )
            }
        }
    }

    private fun hasEnoughRealContent(content: String?): Boolean {
        return !content.isNullOrBlank() && content.trim().length >= 500
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

    private fun mapScrapeException(e: Exception): ContentExtractionResult.Failure {
        val message: String
        val detail: String
        val errorType = ContentExtractionResult.Failure.ErrorType.GENERAL_ERROR

        val isUnexpectedEndOfStream = e.message?.contains("unexpected end of stream", ignoreCase = true) == true

        when {
            e is java.net.UnknownHostException -> {
                message = "DNS/Hostname-Fehler oder keine Internetverbindung"
                detail = "Der Hostname konnte nicht aufgelöst werden. Bitte überprüfe deine Netzwerkverbindung und den eingegebenen Link."
            }
            e is java.net.SocketTimeoutException -> {
                message = "Ladezeit überschritten"
                detail = "Die Webseite reagiert nicht rechtzeitig. Möglicherweise ist der Server überlastet oder offline."
            }
            e is java.net.ConnectException -> {
                message = "Verbindung fehlgeschlagen"
                detail = "Es konnte keine Verbindung zur Webseite aufgebaut werden. Möglicherweise ist die Seite offline oder blockiert Zugriffe."
            }
            e is java.net.SocketException || isUnexpectedEndOfStream || e is java.io.EOFException -> {
                message = "Verbindungsabbruch"
                detail = "Die Verbindung zur Webseite wurde unerwartet unterbrochen."
            }
            e is javax.net.ssl.SSLHandshakeException || e is javax.net.ssl.SSLException -> {
                message = "SSL/TLS-Fehler"
                detail = "Die verschlüsselte Verbindung (SSL/TLS) zur Webseite ist fehlgeschlagen. Dies geschieht oft bei abgelaufenen oder ungültigen Sicherheitszertifikaten."
            }
            e is java.io.IOException && e.message?.startsWith("HTTP_ERROR_") == true -> {
                val codeStr = e.message?.substringAfter("HTTP_ERROR_") ?: "unknown"
                val code = codeStr.toIntOrNull() ?: 0
                message = "HTTP-Fehler ($code)"
                when (code) {
                    403 -> {
                        detail = "Der Zugriff wurde verweigert (HTTP 403). Die Webseite blockiert automatisierte Zugriffe (z.B. Cloudflare-Schutz, Cookie-Schranke oder Paywall)."
                    }
                    401 -> {
                        detail = "Anmeldung erforderlich (HTTP 401). Diese Seite ist passwortgeschützt."
                    }
                    404 -> {
                        detail = "Seite nicht gefunden (HTTP 404). Bitte prüfe die URL auf Tippfehler."
                    }
                    429 -> {
                        detail = "Zu viele Anfragen (HTTP 429). Der Server hat die Verbindung vorübergehend blockiert."
                    }
                    in 500..599 -> {
                        detail = "Die Webseite hat ein internes Serverproblem (HTTP $code)."
                    }
                    else -> {
                        detail = "Der Server hat mit einem HTTP-Fehlercode $code geantwortet."
                    }
                }
            }
            e.message?.contains("Could not fetch webpage content", ignoreCase = true) == true ||
            e.message?.contains("Cleaned webpage content", ignoreCase = true) == true ||
            e.message?.contains("too short", ignoreCase = true) == true -> {
                message = "Inhalt leer/nicht verwertbar"
                detail = "Die Seite enthält keinen lesbaren Text oder der extrahierte Inhalt ist zu kurz für eine Analyse."
            }
            else -> {
                message = "Inhalt konnte nicht geladen werden"
                detail = e.localizedMessage ?: "Ein unerwarteter Netzwerkfehler ist aufgetreten."
            }
        }

        return ContentExtractionResult.Failure(errorType, message, detail)
    }

    private fun isGoogleMapsUrl(rawUrl: String, normalizedUrl: String): Boolean {
        return GoogleMapsUrlParser.isGoogleMapsUrl(rawUrl) ||
                GoogleMapsUrlParser.isGoogleMapsUrl(normalizedUrl) ||
                isGoogleMapsUrlPattern(rawUrl) ||
                isGoogleMapsUrlPattern(normalizedUrl)
    }

    private fun buildConfirmedProfile(
        rawUrl: String,
        normalizedUrl: String,
        isSuccess: Boolean,
        detailMessage: String? = null
    ): SourceProfile {
        val isMaps = GoogleMapsUrlParser.isGoogleMapsUrl(rawUrl) ||
                GoogleMapsUrlParser.isGoogleMapsUrl(normalizedUrl) ||
                isGoogleMapsUrlPattern(rawUrl) ||
                isGoogleMapsUrlPattern(normalizedUrl)

        return if (isMaps) {
            SourceProfile(
                sourceType = SourceProfile.SourceType.PLACE,
                platform = SourcePlatform.GOOGLE_MAPS,
                rawInput = rawUrl,
                normalizedUrl = normalizedUrl,
                capabilities = mapOf(
                    SourceCapability.PLACE_CONTEXT to CapabilityState(
                        capability = SourceCapability.PLACE_CONTEXT,
                        status = CapabilityStatus.AVAILABLE
                    )
                ),
                isPostFetchConfirmed = true
            )
        } else {
            val status = if (isSuccess) CapabilityStatus.AVAILABLE else CapabilityStatus.FAILED
            SourceProfile(
                sourceType = SourceProfile.SourceType.WEB_PAGE,
                platform = SourcePlatform.WEB,
                rawInput = rawUrl,
                normalizedUrl = normalizedUrl,
                capabilities = mapOf(
                    SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                        capability = SourceCapability.PAGE_ARTICLE_TEXT,
                        status = status,
                        detailMessage = detailMessage
                    )
                ),
                isPostFetchConfirmed = true
            )
        }
    }

    private fun isGoogleMapsUrlPattern(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("maps.google.") ||
                lower.contains("google.com/maps") ||
                (lower.contains("google.") && lower.contains("/maps")) ||
                lower.contains("goo.gl/maps") ||
                lower.contains("maps.app.goo.gl")
    }

    private fun buildSocialConfirmedProfile(
        rawUrl: String,
        normalizedUrl: String,
        platformName: String
    ): SourceProfile {
        val platform = when (platformName.lowercase()) {
            "instagram" -> SourcePlatform.INSTAGRAM
            "facebook" -> SourcePlatform.FACEBOOK
            "tiktok" -> SourcePlatform.TIKTOK
            else -> SourcePlatform.WEB
        }
        return SourceProfile(
            sourceType = SourceProfile.SourceType.WEB_PAGE,
            platform = platform,
            rawInput = rawUrl,
            normalizedUrl = normalizedUrl,
            capabilities = mapOf(
                SourceCapability.PAGE_ARTICLE_TEXT to CapabilityState(
                    capability = SourceCapability.PAGE_ARTICLE_TEXT,
                    status = CapabilityStatus.UNAVAILABLE,
                    detailMessage = "Geschützter Inhalt hinter Login-Schranke ($platformName)"
                )
            ),
            isPostFetchConfirmed = true
        )
    }
}
