package com.example.data.diagnostics

import com.example.data.AnalysisType

/**
 * Zentrale Source of Truth für Test-Referenzen aller aktiven Relevantor-Funktionen.
 * 
 * Trennt strikt zwischen:
 * - LIVE: Tatsächlich erreichbare, reale Produktiv-Quellen (URLs / Medien)
 * - SYNTHETIC: Lokale Golden-Case / Fixture-Mock-Quellen
 */
object TestReferenceRegistry {

    enum class TestMode {
        LIVE,
        SYNTHETIC
    }

    enum class VerificationStatus {
        VERIFIED,
        MISSING,
        UNSURE,
        OUT_OF_SCOPE
    }

    data class TestReference(
        val reference: String,
        val purpose: String,
        val expected: String,
        val testMode: TestMode,
        val testQuery: String? = null,
        val resolvedReference: String? = null
    )

    data class FunctionTestEntry(
        val functionId: String,
        val analysisType: AnalysisType?,
        val easy: TestReference?,
        val difficult: TestReference?,
        val source: String,
        val verificationStatus: VerificationStatus,
        val isQualityGateRelevant: Boolean = true,
        val specialNotes: String? = null
    )

    val ENTRIES: List<FunctionTestEntry> = listOf(
        FunctionTestEntry(
            functionId = "WEB_SUMMARY",
            analysisType = AnalysisType.WEB_SUMMARY,
            easy = TestReference(
                reference = "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/",
                purpose = "Standard Webseiten-Analyse eines strukturierten Reiseberichts mit Text und HTML",
                expected = "Erfolgreiche Zusammenfassung mit strukturierten Kernaussagen und Kontext",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://share.google/OLQ1vkrSTzTWyCwoM",
                purpose = "Realer Android News share.google Redirect auf Focus Online Artikel (Sam Altman Falschmeldung)",
                expected = "Redirect-Auflösung auf www.focus.de, saubere Content-Extraktion und strukturierte Zusammenfassung PASS",
                testMode = TestMode.LIVE
            ),
            source = "Android News Share (share.google) & ExampleUnitTest",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "KEY_TAKEAWAYS",
            analysisType = AnalysisType.KEY_TAKEAWAYS,
            easy = TestReference(
                reference = "https://en.wikipedia.org/wiki/Main_Page",
                purpose = "Standardfall zur Extraktion der 3 wesentlichen Kernbotschaften",
                expected = "Genau 3 prägnante, voneinander abgegrenzte Kernaussagen",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://studienportal.de/homeoffice-produktivitaet-studie",
                purpose = "Synthetischer Golden-Case für komplexe Studienergebnisse",
                expected = "Strukturierte Ableitung von 3 Kernresultaten",
                testMode = TestMode.SYNTHETIC
            ),
            source = "MainActivity.DebugUrlSelection & Golden Dataset KEY_TAKEAWAYS",
            verificationStatus = VerificationStatus.VERIFIED,
            specialNotes = "Real-Live difficult URL ist aktuell MISSING"
        ),
        FunctionTestEntry(
            functionId = "FREE_SOURCE_QUERY",
            analysisType = AnalysisType.FREE_SOURCE_QUERY,
            easy = TestReference(
                reference = "https://freie-anfrage.de",
                purpose = "Synthetischer Golden-Case für freie Q&A-Quellenabfrage",
                expected = "Präzise faktenbasierte Beantwortung der Nutzerfrage aus dem Quellentext",
                testMode = TestMode.SYNTHETIC
            ),
            difficult = TestReference(
                reference = "https://share.google/OLQ1vkrSTzTWyCwoM",
                resolvedReference = "https://www.focus.de/panorama/welt/panne-bei-google-suchmaschine-zeigt-kurzzeitig-tod-von-openai-chef-sam-altman-an_25885e2a-8b33-443e-8e2f-333dffa6ea1f.html",
                testQuery = "Was genau ist laut Artikel bei Google passiert und wie wurde der Fehler erklärt?",
                purpose = "Reale freie Quellenfrage über Android-News-Redirect.",
                expected = "share.google Redirect korrekt aufgelöst, FOCUS-Artikel extrahiert, Frage ausschließlich auf Basis der Quelle beantwortet, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            source = "Android News Share (share.google)",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "MULTIMEDIA_ANALYSIS",
            analysisType = AnalysisType.MULTIMEDIA_ANALYSIS,
            easy = TestReference(
                reference = "https://www.youtube.com/watch?v=hJP5GqnTrNo",
                purpose = "YouTube-Video mit vollständig verfügbarem Transkript (Sal Khan TED Talk)",
                expected = "Multimedia-Analyse aktiv, keine Degraded-Warnung, vollständige Timestamp-Zusammenfassung",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://www.youtube.com/watch?v=5qap5aO4i9A",
                purpose = "YouTube-Livestream ohne Transkript (Lofi Girl) als Degraded-Grenzfall",
                expected = "Multimedia-Analyse wechselt kontrolliert auf DEGRADED, Zusammenfassung rein aus Metadaten",
                testMode = TestMode.LIVE
            ),
            source = "MANUAL_SMARTPHONE_TEST_CASES.md (YT-01 & YT-02)",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "FRESHNESS_CHECK",
            analysisType = AnalysisType.FRESHNESS_CHECK,
            easy = TestReference(
                reference = "https://techportal.de/trends-2026",
                purpose = "Synthetischer Golden-Case für Aktualitätsprüfung von Zeitangaben und Fakten",
                expected = "Klare Bewertung der zeitlichen Relevanz und Identifikation veralteter Punkte",
                testMode = TestMode.SYNTHETIC
            ),
            difficult = TestReference(
                reference = "https://share.google/OLQ1vkrSTzTWyCwoM",
                purpose = "Realer Android News Fall zur zeitkritischen Aktualitätsprüfung einer Google-Suchergebnis-Panne",
                expected = "Redirect/Extraction PASS und zeitliche Einordnung des Vorfalls fachlich verwertbar",
                testMode = TestMode.LIVE
            ),
            source = "Android News Share (share.google)",
            verificationStatus = VerificationStatus.VERIFIED,
            specialNotes = "Real-Live easy URL ist aktuell MISSING"
        ),
        FunctionTestEntry(
            functionId = "MISINFORMATION_RADAR",
            analysisType = AnalysisType.MISINFORMATION_RADAR,
            easy = TestReference(
                reference = "https://klimabericht.de/co2-diskussion",
                purpose = "Synthetischer Golden-Case für Erkennung von Scheinargumenten und Bias",
                expected = "Identifikation potentiell irreführender Formulierungen und Plausibilitätscheck",
                testMode = TestMode.SYNTHETIC
            ),
            difficult = TestReference(
                reference = "https://share.google/OLQ1vkrSTzTWyCwoM",
                purpose = "Realer Android News Fall über eine KI-/Suchmaschinen-Fehlinformation (angeblicher Tod von Sam Altman)",
                expected = "Redirect/Extraction PASS und differenzierte Analyse des Fehlinformationsrisikos bzw. Berichtsursprungs",
                testMode = TestMode.LIVE
            ),
            source = "Android News Share (share.google)",
            verificationStatus = VerificationStatus.VERIFIED,
            specialNotes = "Real-Live easy URL ist aktuell MISSING"
        ),
        FunctionTestEntry(
            functionId = "FACTS_VS_OPINIONS",
            analysisType = AnalysisType.FACTS_VS_OPINIONS,
            easy = TestReference(
                reference = "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/",
                purpose = "Reisebericht mit Mischung aus überprüfbaren Fakten und subjektiven Impressionen",
                expected = "Disjunkte Trennung zwischen Faktenblock und Meinungs-/Wertungsblock",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://immo-news.de/preisentwicklung",
                purpose = "Synthetischer Golden-Case für getarnte Werbe-/Interessenstexte",
                expected = "Aufdeckung subjektiver Prognosen trotz faktenartiger Präsentation",
                testMode = TestMode.SYNTHETIC
            ),
            source = "ExampleUnitTest & Golden Dataset FACTS_VS_OPINIONS",
            verificationStatus = VerificationStatus.VERIFIED,
            specialNotes = "Real-Live difficult URL ist aktuell MISSING"
        ),
        FunctionTestEntry(
            functionId = "RISK_ANALYSIS",
            analysisType = AnalysisType.RISK_ANALYSIS,
            easy = TestReference(
                reference = "https://www.who.int/news-room/fact-sheets/detail/radon-and-health",
                purpose = "Reale WHO-Information mit konkreten Gesundheitsrisiken, Synergie-Effekten beim Rauchen und Folgen für Gebäude.",
                expected = "Quelle vollständig extrahiert, konkrete Risiken strukturiert erkannt, Risikoarten und mögliche Folgen sauber getrennt, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://finanz-blog.de/krypto-vorsorge",
                purpose = "Synthetischer Golden-Case zur Identifikation finanzieller und regulatorischer Risiken",
                expected = "Strukturierte Risiko-Kategorisierung und Schadenspotenzial-Einschätzung",
                testMode = TestMode.SYNTHETIC
            ),
            source = "World Health Organization (WHO) & Golden Dataset",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "PERSPECTIVES_COUNTERPOSITIONS",
            analysisType = AnalysisType.PERSPECTIVES_COUNTERPOSITIONS,
            easy = TestReference(
                reference = "https://www.pewresearch.org/2025/04/03/views-of-risks-opportunities-and-regulation-of-ai/",
                purpose = "Reale Studie mit unterschiedlichen Perspektiven von Öffentlichkeit und KI-Experten auf Chancen, Risiken und Regulierung.",
                expected = "unterbrochene/unterschiedliche Positionen klar getrennt, Gemeinsamkeiten und Gegensätze korrekt herausgearbeitet, keine künstliche Gleichsetzung der Perspektiven, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://debattenportal.de/tempolimit",
                purpose = "Synthetischer Golden-Case für polarisierende gesellschaftliche Fragestellungen",
                expected = "Ausgewogene Gegenüberstellung von Pro- und Contra-Perspektiven",
                testMode = TestMode.SYNTHETIC
            ),
            source = "Pew Research Center & Golden Dataset",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "RELEVANT_ASPECTS",
            analysisType = AnalysisType.RELEVANT_ASPECTS,
            easy = TestReference(
                reference = "https://www.gov.uk/government/publications/agentic-ai-and-consumers/agentic-ai-and-consumers",
                purpose = "Reale Analyse zu Agentic AI mit Verbraucher-, Rechts-, Unternehmens- und Risikodimensionen.",
                expected = "über den Basisinhalt hinaus relevante Dimensionen identifiziert, Recht, Verbraucherschutz, Unternehmensfolgen und Risiken sinnvoll strukturiert, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://recht-portal.de/homeoffice-gesetz",
                purpose = "Synthetischer Golden-Case für weiterführende Dimensionen und Konsequenzen",
                expected = "Strukturierte Liste relevanter, im Basistext nicht vertiefter Nebenaspekte",
                testMode = TestMode.SYNTHETIC
            ),
            source = "GOV.UK & Golden Dataset",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "GOOGLE_MAPS_ANALYZER",
            analysisType = AnalysisType.GOOGLE_MAPS_ANALYZER,
            easy = TestReference(
                reference = "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep",
                purpose = "Eindeutiger Standard-POI mit klarem Suchbegriff und bekannter Ortsidentität",
                expected = "Erfolgreiche Places-Auflösung und vollständige Analyse ohne Disambiguierungs-Konflikt",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://maps.app.goo.gl/WgXTvya1yCDJjameA",
                purpose = "Schwieriger Shortlink mit Plus-Code (QXV3+893), Thai-Bezeichnung und dezimaler CID-Disambiguierung",
                expected = "Eindeutige Auflösung zu ChIJJ7Q_mWE72jARcEMQAjiMjLw via CID-Matching und vollständige Places-Analyse",
                testMode = TestMode.LIVE
            ),
            source = "MainActivity.DebugUrlSelection & LiveSmokeOrchestratorTest / GoogleMapsDisambiguatorTest",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "GOOGLE_MAPS_LOCATION_CONTEXT",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT,
            easy = TestReference(
                reference = "https://www.google.com/maps/place/Brandenburger+Tor/",
                purpose = "Historisches Wahrzeichen mit reichhaltigem Umfeld- und Enzyklopädie-Kontext",
                expected = "Erfolgreiche Location-Context-Generierung inklusive Wikipedia/Wikivoyage-Anreicherung",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://maps.app.goo.gl/WgXTvya1yCDJjameA",
                purpose = "Grenzfall-POI in Chiang Mai über Shortlink zur Prüfung des asiatischen Kontext-Netzwerks",
                expected = "Stabile Erstellung des Location-Context-Profils ohne Pipeline-Abbruch",
                testMode = TestMode.LIVE
            ),
            source = "GoogleMapsLocationContextServiceTest & LiveSmokeOrchestratorTest",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "GOOGLE_MAPS_LOCATION_QUERY",
            analysisType = AnalysisType.GOOGLE_MAPS_LOCATION_QUERY,
            easy = TestReference(
                reference = "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep",
                purpose = "Standard-Ortsabfrage mit gezielten Fragen zur Infrastruktur / Zugänglichkeit",
                expected = "Genaue ortsbezogene Beantwortung der Nutzerfrage anhand der Places- und Kontextdaten",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://maps.app.goo.gl/WgXTvya1yCDJjameA",
                purpose = "Ortsfrage an einen über CID disambiguierten thailändischen Straßenküchen-POI",
                expected = "Präzise Beantwortung von Fragen (z. B. Öffnungszeiten, Spezialitäten) für den Zielort",
                testMode = TestMode.LIVE
            ),
            source = "LocationQuestionCoordinatorTest & LiveSmokeOrchestratorTest",
            verificationStatus = VerificationStatus.VERIFIED
        ),
        FunctionTestEntry(
            functionId = "DOCUMENT_SUMMARY",
            analysisType = AnalysisType.DOCUMENT_SUMMARY,
            easy = TestReference(
                reference = "https://www.gov.uk/government/publications/ai-cyber-threats-open-letter-to-business-leaders",
                purpose = "Kurzes offizielles Dokument / verlinkte PDF mit überschaubarer Struktur.",
                expected = "Dokument/PDF erfolgreich verarbeitet, zentrale Handlungsempfehlungen vollständig zusammengefasst, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://cdn.websitebuilder.service.justice.gov.uk/uploads/sites/54/2025/07/AI-paper-PDF.pdf",
                purpose = "Komplexeres mehrseitiges juristisches Diskussionspapier zu AI and the Law.",
                expected = "PDF vollständig verarbeitet, zentrale Themen und Argumentationsblöcke korrekt strukturiert, keine wesentlichen Kapitel ausgelassen, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            source = "GOV.UK & Judiciary.uk",
            verificationStatus = VerificationStatus.UNSURE,
            specialNotes = "Aktiv implementiert, realer Produktpfad File Picker/content://, GenericLiveSmokeTest nicht repräsentativ, kein bekanntes Produktproblem"
        ),
        FunctionTestEntry(
            functionId = "PHOTO_SCREENSHOT_ANALYSIS",
            analysisType = AnalysisType.PHOTO_SCREENSHOT_ANALYSIS,
            easy = TestReference(
                reference = "https://www.jpl.nasa.gov/images/pia00123-earth-pacific-ocean/",
                purpose = "Einfaches reales NASA-Bild mit eindeutigem Hauptmotiv.",
                expected = "Hauptmotiv Erde/Pazifik korrekt erkannt, keine erfundenen Details, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            difficult = TestReference(
                reference = "https://science.nasa.gov/resource/earth-poster-version-d/",
                purpose = "Komplexer Poster-/Infografik-Fall mit Bild-, Text- und Layoutinformationen.",
                expected = "visuelle Hauptelemente korrekt erkannt, Text-/Grafikbestandteile sinnvoll zusammengeführt, keine Verwechslung von Bildinhalt und Begleittext, funktionaler PASS",
                testMode = TestMode.LIVE
            ),
            source = "NASA JPL & NASA Science",
            verificationStatus = VerificationStatus.UNSURE,
            specialNotes = "Aktiv implementiert, realer Produktpfad File/Image Picker -> content:// -> Bytes, Self-/Regression-Tests vorhanden, kein Generic-Live-Test"
        ),

        // --- PLATZHALTER / NOCH NICHT ENTWICKELT (AUSSERHALB DES QUALITY GATES) ---
        FunctionTestEntry(
            functionId = "AI_IMAGE_DETECTOR",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category E)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "INFOGRAPHIC_GENERATOR",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category C - Visualisierung)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "STRUCTURE_VISUALIZER",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category C - Visualisierung)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "IMAGE_IDEA_GENERATOR",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category C - Visualisierung)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "SOCIAL_MEDIA_GENERATOR",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category D - Inhalte verarbeiten)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "COMMUNICATION_GENERATOR",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category D - Inhalte verarbeiten)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        ),
        FunctionTestEntry(
            functionId = "MULTI_URL_SUMMARY",
            analysisType = null,
            easy = null,
            difficult = null,
            source = "FeatureCatalog (Category D - Inhalte verarbeiten)",
            verificationStatus = VerificationStatus.OUT_OF_SCOPE,
            isQualityGateRelevant = false,
            specialNotes = "Platzhalter / noch nicht entwickelt – aktuell außerhalb des Quality Gates"
        )
    )

    /**
     * Liefert alle aktiv implementierten, testpflichtigen Quality-Gate-Funktionen.
     */
    fun getActiveEntries(): List<FunctionTestEntry> {
        return ENTRIES.filter { it.isQualityGateRelevant && it.verificationStatus != VerificationStatus.OUT_OF_SCOPE }
    }

    fun getByAnalysisType(type: AnalysisType): FunctionTestEntry? {
        val target = type.canonical()
        return ENTRIES.find { it.analysisType == target || it.analysisType == type }
    }

    fun getByFunctionId(id: String): FunctionTestEntry? {
        return ENTRIES.find { it.functionId.equals(id, ignoreCase = true) }
    }

    /**
     * Liefert alle eindeutigen verifizierten LIVE-Testreferenzen für die Schnell-Auswahl im DEBUG-Modus.
     */
    fun getLiveDebugReferences(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        list.add("Maps-Short" to "https://maps.app.goo.gl/WgXTvya1yCDJjameA")
        list.add("Maps-Q" to "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep")
        list.add("News-Focus" to "https://share.google/OLQ1vkrSTzTWyCwoM")
        list.add("Wiki" to "https://en.wikipedia.org/wiki/Main_Page")
        list.add("Wischnewski" to "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/")
        list.add("YT-Trans" to "https://www.youtube.com/watch?v=hJP5GqnTrNo")
        list.add("YT-Degr" to "https://www.youtube.com/watch?v=5qap5aO4i9A")
        return list
    }
}
