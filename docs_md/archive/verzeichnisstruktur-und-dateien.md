# Relevantor – Architektur- und Systemübersicht

Dieses Dokument dient als zentrale Referenz für die Verzeichnisstruktur, die Prompts und die Systemkomponenten von Relevantor.

---

## 1. VERZEICHNISSTRUKTUR

Die Verzeichnisstruktur der Android-App (App-Modul) und der Dokumentations-Dateien (ohne `.git`, `.gradle`, `build`, `outputs`):

```text
app/src/
    androidTest/
        java/
            com/
                example/
                    ExampleInstrumentedTest.kt
    main/
        AndroidManifest.xml
        assets/
            ARCHITECTURE_FREEZE.md
            change-prompts/
                CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
                CP-02_AENDERUNG_ARBEITSWEISE.md
                CP-03_NEUANLAGE_FUNKTION.md
                CP-07_UI_UX_AENDERUNG.md
                CP_GUIDELINE.md
                README_CHANGE_PROMPTS.md
            prompts/
                F_AKTUALITAETS_CHECK.md
                F_BUSINESS_INKUBATOR.md
                F_DOKUMENTE.md
                F_FACTS_VS_OPINIONS_ANALYZER.md
                F_FEHLINFORMATIONS_RADAR.md
                F_FREIE_QUELLENANFRAGE.md
                F_GOOGLE_MAPS_ANALYZER.md
                F_MULTIMEDIA.md
                F_PERSPECTIVES_AND_COUNTERPOSITIONS.md
                F_PHOTO_SCREENSHOT_ANALYSIS.md
                F_RISIKO_ANALYSE.md
                F_STANDARD_WEBSEITE.md
                F_TOP_3_KERNAUSSAGEN.md
                F_WEITERE_RELEVANTE_ASPEKTE.md
                _global_quality_rules.md
                function_registry.json
                prompt_manifest.json
        java/
            com/
                example/
                    LocalContentExtractionEngine.kt
                    MainActivity.kt
                    RelevantorAccessibilityService.kt
                    data/
                        AnalysisRuntimeConfigs.kt
                        AnalysisType.kt
                        BackendFeatureConfig.kt
                        FileProcessingHelper.kt
                        FunctionRegistry.kt
                        GatewayDiagnostics.kt
                        GeminiModels.kt
                        GeminiRepository.kt
                        GoogleMapsUrlParser.kt
                        PipelineReport.kt
                        PipelineReportStore.kt
                        PlacesApiService.kt
                        PlacesDataMapper.kt
                        PromptEngine.kt
                        PromptFallbackProvider.kt
                        PromptLoader.kt
                        PublicVideoSourceResolver.kt
                        ResponseNormalizer.kt
                        RetrofitClient.kt
                        RuntimePreflight.kt
                        RuntimeSmokeTestHarness.kt
                        RuntimeVerificationLayer.kt
                        SummaryResponseParser.kt
                        WebpageExtractor.kt
                        YoutubeTranscriptHelper.kt
                        YoutubeUrlDecoder.kt
                        engine/
                            AnalysisRegistryImpl.kt
                            AndroidAssetPromptLoader.kt
                            BaseGeminiEngine.kt
                            EngineRunnerImpl.kt
                            document/
                                DocumentAnalysisEngine.kt
                            top3/
                                Top3KeyPointsEngine.kt
                            web/
                                WebpageAnalysisEngine.kt
                        extraction/
                            DocumentInputExtractor.kt
                            InputExtractor.kt
                            InputExtractorRegistry.kt
                            WebInputExtractor.kt
                            YoutubeInputExtractor.kt
                        local/
                            RelevantorDatabase.kt
                            SessionStorage.kt
                        remote/
                            BackendApiService.kt
                        repository/
                            AnalysisRepositoryImpl.kt
                            ContentExtractionRepositoryImpl.kt
                            SyncRepositoryImpl.kt
                            UserRepositoryImpl.kt
                            YoutubeTranscriptProviderAdapter.kt
                        sync/
                            SyncScheduler.kt
                            SyncWorker.kt
                    domain/
                        engine/
                            AnalysisEngine.kt
                            AnalysisRegistry.kt
                            ContractValidator.kt
                            EngineRunner.kt
                            PromptAssetLoader.kt
                            validators/
                                A1ContractValidator.kt
                                A2ContractValidator.kt
                        model/
                            AnalysisTrace.kt
                            CanonicalAnalysisInput.kt
                            ContentExtractionResult.kt
                            DomainSummary.kt
                            ExtractedContent.kt
                            PublicVideoSource.kt
                            TranscriptProvider.kt
                        repository/
                            AnalysisRepository.kt
                            ContentExtractionRepository.kt
                            GeminiGateway.kt
                            SyncRepository.kt
                            UserRepository.kt
                        usecase/
                            AnalyzeContentUseCase.kt
                            ExtractContentUseCase.kt
                            LoadHistoryUseCase.kt
                            SaveAnalysisUseCase.kt
                            SyncUserDataUseCase.kt
                    ui/
                        MainViewModel.kt
                        components/
                            TakeawayCard.kt
                        metadata/
                            ExportFormatter.kt
                            FeatureCatalog.kt
                            OutputPresentationPolicy.kt
        res/
            drawable/
                ic_launcher_background.xml
                ic_launcher_foreground.xml
            drawable-nodpi/
                relevantor_home_coffeehouse.png
                relevantor_home_coffeehouse_background.png
                relevantor_home_coffeehouse_objective.png
            mipmap-anydpi-v26/
                ic_launcher.xml
                ic_launcher_round.xml
            values/
                strings.xml
                themes.xml
            xml/
                accessibility_service_config.xml
                backup_rules.xml
                data_extraction_rules.xml
    test/
        assets/
            golden/
                DOCUMENT_SUMMARY/
                FACTS_VS_OPINIONS/
                FREE_SOURCE_QUERY/
                FRESHNESS_CHECK/
                KEY_TAKEAWAYS/
                MISINFORMATION_RADAR/
                MULTIMEDIA_ANALYSIS/
                PERSPECTIVES_COUNTERPOSITIONS/
                RELEVANT_ASPECTS/
                RISK_ANALYSIS/
                WEB_SUMMARY/
        java/
            com/
                example/
                    A1ContractValidatorTest.kt
                    A2ContractValidatorTest.kt
                    BaseArchitectureRegressionTest.kt
                    BulkMigrationVerificationTest.kt
                    ContentExtractionRegressionTest.kt
                    ExampleRobolectricTest.kt
                    ExampleUnitTest.kt
                    GreetingScreenshotTest.kt
                    PipelineReportTest.kt
                    PlacesApiServiceTest.kt
                    RelevantorSelfTestHarnessTest.kt
                    TranscriptProviderTest.kt
                    contextpoc/
                        ContextCandidate.kt
                        ContextCandidateMatcher.kt
                        ContextMatchResult.kt
                        ContextMatchingTest.kt
                        ContextPlaceInput.kt
                        ContextResolutionStatus.kt
                        ContextResolver.kt
                        ContextResolverIntegrationTest.kt
                        ContextResolverResult.kt
                        ContextSourceMetadata.kt
                        ContextSourceType.kt
                        ContextSuitabilityEvaluator.kt
                        SimpleTest.kt
                        WikipediaApiClient.kt
                        WikipediaApiClientTest.kt
                        WikipediaMockFixtures.kt
                    data/
                        PublicVideoSourceResolverTest.kt
        screenshots/
            greeting.png
            gudrun_nolte_pdf_result.png
docs_md/
    ABSTRACTOR_SYSTEM_STATE.md
    GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md
    GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md
    GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md
    GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md
    GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md
    LOCAL_BUILD_HANDOFF.md
    PROJECT_CONTEXT_RELEVANTOR.md
    README_INSTALL.txt
    RELEVANTOR_ARCHITECTURE.md
    RELEVANTOR_BASELINE_LOCAL_FIRST.md
    RELEVANTOR_DEVELOPMENT_STATUS.md
    RELEVANTOR_FUNCTION_EXECUTION_MODEL.md
    RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md
    RELEVANTOR_OUTPUT_SPEC.md
    RELEVANTOR_SELF_TEST_MATRIX.md
    RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md
    TEST_COVERAGE_MATRIX.md
    ZUSAMMENFASSUNG_ARBEITEN.md
    verzeichnisstruktur-und-dateien.md
```

---

## 2. PROMPT-SYSTEM (EXAKTER ZUSTAND)

### 2.1 Prompt-Dateien und Metadaten

| Dateiname | Mapping (AnalysisType) | Hauptfokus |
| :--- | :--- | :--- |
| `prompt_manifest.json` | - | Systemweites Mapping-Schema |
| `_global_quality_rules.md` | - | Globale Qualitätsrichtlinien und Tonalitätsvorgaben |
| `F_STANDARD_WEBSEITE.md` | `STANDARD_WEBSEITE` | Kompakte Zusammenfassung und Kernaussagen |
| `F_TOP_3_KERNAUSSAGEN.md` | `TOP_3_KERNAUSSAGEN` | Die exakt 3 wichtigsten Kernaussagen (1-3 Sätze, kein Markdown-Fettdruck in Details) |
| `F_FACTS_VS_OPINIONS_ANALYZER.md` | `FACTS_VS_OPINIONS_ANALYZER` | Strukturierung nach Fakten, Meinungen, Vermutungen, Werbung, Spekulation |
| `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | `PERSPECTIVES_AND_COUNTERPOSITIONS` | Aufdecken alternativer Sichtweisen und Gegenargumente |
| `F_MULTIMEDIA.md` | `MULTIMEDIA` | Video- & Transkript-Analyse mit Bereinigung von Sponsorenblöcken |
| `F_FREIE_QUELLENANFRAGE.md` | `FREIE_QUELLENANFRAGE` | Spezifische Fragebeantwortung zur Quelle |
| `F_DOKUMENTE.md` | `DOKUMENTE` | Inhaltserschließung strukturierter Texte und Dokumente |
| `F_AKTUALITAETS_CHECK.md` | `AKTUALITAETS_CHECK` | Zeitliche Relevanz und Gültigkeits-Check (Dimension A/B) |
| `F_FEHLINFORMATIONS_RADAR.md` | `FEHLINFORMATIONS_RADAR` | Erkennen rhetorischer Mängel, Clickbait oder ungestützter Behauptungen |
| `F_RISIKO_ANALYSE.md` | `RISIKO_ANALYSE` | Zeichnen eines stabilen Risikoprofils |
| `F_BUSINESS_INKUBATOR.md` | `BUSINESS_INKUBATOR` | Erarbeiten von bis zu 3 bahnbrechenden Geschäftsideen |
| `F_WEITERE_RELEVANTE_ASPEKTE.md` | `WEITERE_RELEVANTE_ASPEKTE` | Identifikation zusätzlicher Perspektiven ohne Quellenkritik |
| `F_GOOGLE_MAPS_ANALYZER.md` | `GOOGLE_MAPS_ANALYZER` | Ortsparameter und Places API Details analysieren |
| `F_PHOTO_SCREENSHOT_ANALYSIS.md` | `PHOTO_SCREENSHOT_ANALYSIS` | Foto & Screenshots auswerten und Bildinhalte einordnen |

---

### 2.2 Vollständiger Inhalt wichtiger Prompt-Dateien (Auszug)

#### `/assets/prompts/_global_quality_rules.md`
```markdown
=== GLOBAL QUALITY RULES ===
1. ABSOLUTE WAHRHEITSTREUE & PRÄZISION:
   - Erfinde niemals Fakten, Personen, Daten oder Links.
   - Falls Informationen unvollständig oder unklar sind, mache dies transparent deutlich.
2. STRIKTE GEBOT ZUR REINEN AUSGABE:
   - Antworte immer ausschließlich im geforderten JSON-Format.
   - Keine einleitenden Floskeln, Begrüßungen ("Hier ist die Analyse...") oder abschließenden Bemerkungen außerhalb des JSON.
3. FORMALE REGELN FÜR DIE STRUKTUR:
   - Jede Kernaussage in 'key_takeaways' MUSS einleitend ein fettgedrucktes Schlagwort als Titel erhalten, gefolgt von einem Doppelpunkt und der Ausführung (z. B. "**Zielsetzung**: Das Hauptziel...").
   - Platziere keine künstlichen Nummerierungen vor den Einträgen im JSON-Array (z. B. kein "1.", "2." davor setzen).
4. DEUTSCHE TONALITÄT:
   - Alle Analysen und Felder müssen standardmäßig in fehlerfreiem, professionellem Deutsch verfasst sein.
```

#### `/assets/prompts/prompt_manifest.json` (gekürzter Auszug)
```json
{
  "STANDARD_WEBSEITE": "F_STANDARD_WEBSEITE.md",
  "TOP_3_KERNAUSSAGEN": "F_TOP_3_KERNAUSSAGEN.md",
  "FACTS_VS_OPINIONS_ANALYZER": "F_FACTS_VS_OPINIONS_ANALYZER.md",
  "PERSPECTIVES_AND_COUNTERPOSITIONS": "F_PERSPECTIVES_AND_COUNTERPOSITIONS.md",
  "MULTIMEDIA": "F_MULTIMEDIA.md",
  "FREIE_QUELLENANFRAGE": "F_FREIE_QUELLENANFRAGE.md",
  "DOKUMENTE": "F_DOKUMENTE.md",
  "AKTUALITAETS_CHECK": "F_AKTUALITAETS_CHECK.md",
  "FEHLINFORMATIONS_RADAR": "F_FEHLINFORMATIONS_RADAR.md",
  "RISIKO_ANALYSE": "F_RISIKO_ANALYSE.md",
  "BUSINESS_INKUBATOR": "F_BUSINESS_INKUBATOR.md",
  "WEITERE_RELEVANTE_ASPEKTE": "F_WEITERE_RELEVANTE_ASPEKTE.md",
  "GOOGLE_MAPS_ANALYZER": "F_GOOGLE_MAPS_ANALYZER.md",
  "PHOTO_SCREENSHOT_ANALYSIS": "F_PHOTO_SCREENSHOT_ANALYSIS.md"
}
```

---

## 3. EXECUTION PIPELINE (CODE WIRKLICHKEIT)

### 3.1 Domain-Driven Engines
Das System nutzt die Basisklasse `BaseGeminiEngine` zur Kapselung aller Gemini-Anfragen über strukturierte JSON-Schemas.

#### `BaseGeminiEngine.kt` (Zentraler Request-Builder & Tracer)
- Lädt spezifische Prompts über `PromptAssetLoader` und kombiniert sie mit `_global_quality_rules.md`.
- Erstellt eine standardisierte `ResponseSchema` Vorgabe (Moshi / JSON Schema) mit den Feldern `title`, `original_url`, `short_description`, `key_takeaways` und `owner`.
- Unterstützt Google Search Grounding (`activeGrounding = useSearchGrounding || runtimeConfig.forceGrounding`).
- Nimmt bei PDF-Dateien Binärdaten (`rawBytes` / `mimeType`) als Inline-Daten auf.
- Verifiziert Ergebnisse über `RuntimeVerificationLayer` und loggt detaillierte Metriken als `AnalysisTrace`.

#### `SummaryResponseParser.kt` (Ergebnis-Bereinigung und Fallback-Handling)
- Nimmt Antworten von Gemini entgegen und parst sie via Moshi.
- Bereinigt Textausschnitte (entfernt Markdown-Bold-Zeichen `**` aus Titeln und Details, entfernt Nummerierungen).
- Falls Details leer oder identisch mit dem Titel sind, fügt es standardisiert `"Ergänzende Detailausführungen sind dem Quelltext direkt zu entnehmen."` hinzu, um die Integrität im UI zu wahren.
- Begrenzt übermäßig lange Titel (> 120 Zeichen) durch Wort-Trimming.

---

## 4. FEATURE REGISTRY (SYSTEM-REALITÄT)

| UI Label | AnalysisType | Kategorie (Code) | Kategorie (Label) | Status | Input(s) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Zusammenfassung | `WEB_SUMMARY` | `A` | Verstehen & Verdichten | OK (Aktiv) | WEB |
| 3 Kernaussagen | `KEY_TAKEAWAYS` | `A` | Verstehen & Verdichten | OK (Aktiv) | WEB |
| Frage an die Quelle | `FREE_SOURCE_QUERY` | `A` | Verstehen & Verdichten | OK (Aktiv) | WEB |
| Video- & Multimedia-Analyse | `MULTIMEDIA_ANALYSIS` | `A` | Verstehen & Verdichten | OK (Aktiv) | WEB, MULTIMEDIA |
| Google Maps Analyser | `GOOGLE_MAPS_ANALYZER` | `A` | Verstehen & Verdichten | OK (Aktiv) | WEB |
| Aktualitäts-Check | `FRESHNESS_CHECK` | `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Fehlinformations-Radar | `MISINFORMATION_RADAR`| `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Fakt-oder-Meinung | `FACTS_VS_OPINIONS` | `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Risikoanalyse | `RISK_ANALYSIS` | `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Perspektiven- & Gegenpositionen-Finder | `PERSPECTIVES_COUNTERPOSITIONS`| `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Weitere relevante Aspekte | `RELEVANT_ASPECTS` | `B` | Qualität, Kritik & Einordnung | OK (Aktiv) | WEB |
| Dokument zusammenfassen | `DOCUMENT_SUMMARY` | `E` | Arbeiten mit Dateien | OK (Aktiv) | DOCUMENT |
| Foto & Screenshots auswerten | `PHOTO_SCREENSHOT_ANALYSIS` | `E` | Arbeiten mit Dateien | OK (Aktiv) | IMAGE |
| Bild mit KI erzeugt? | - | `E` | Arbeiten mit Dateien | Placeholder | IMAGE |
| Infografik-Generator | - | `C` | Visualisierung | Placeholder | WEB |
| Struktur-Visualisierer | - | `C` | Visualisierung | Placeholder | WEB |
| Bildideen-Generator | - | `C` | Visualisierung | Placeholder | WEB |
| Social-Media-Generator | - | `D` | Inhalte verarbeiten | Placeholder | WEB |
| Kommunikations-Generator | - | `D` | Inhalte verarbeiten | Placeholder | WEB |
| Zusammenfassung aus mehreren URL | - | `D` | Inhalte verarbeiten | Placeholder | MULTI_URL |

*Hinweis: Optionen ohne AnalysisType sind im `FeatureCatalog` als Platzhalter markiert (`isPlaceholder = true`), welche bei Klick eine informative Dialogbox anzeigen.*
