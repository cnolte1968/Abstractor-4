# Verzeichnisstruktur und System-Soll-Zustand des Relevantor-Systems

*Erstellt am: 20.07.2026, 05:52:00 (Lokalzeit)*

Dieses Dokument enthält den vollständigen und überprüfbaren Ist-Zustand des Relevantor-Systems. Es liefert eine reine Datenbasis ohne Interpretation oder Optimierungsempfehlungen.

---

## VERZEICHNISSTRUKTUR

```text
/
├── ABSTRACTOR_SYSTEM_STATE.md
├── AGENTS.md
├── ARCHITECTURE_FREEZE.md
├── FUNCTION_SPEC_TEMPLATE.md
├── GEMINI_429_TRUE_CAUSE_REPORT.md
├── ZUSAMMENFASSUNG_ARBEITEN.md
├── build.gradle.kts
├── gradle.properties
├── metadata.json
├── settings.gradle.kts
├── debug.keystore
├── debug.keystore.base64
├── app-debug.apk
├── docs_md/
│   ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md
│   ├── LOCAL_BUILD_HANDOFF.md
│   ├── PROJECT_CONTEXT_RELEVANTOR.md
│   ├── README_INSTALL.txt
│   ├── RELEVANTOR_ARCHITECTURE.md
│   ├── RELEVANTOR_BASELINE_LOCAL_FIRST.md
│   ├── RELEVANTOR_FUNCTION_EXECUTION_MODEL.md
│   ├── RELEVANTOR_OUTPUT_SPEC.md
│   ├── RELEVANTOR_SELF_TEST_MATRIX.md
│   ├── RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md
│   ├── TEST_COVERAGE_MATRIX.md
│   └── verzeichnisstruktur-und-dateien.md
├── gradle/
│   └── libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── GEMINI_429_TRUE_CAUSE_REPORT.md
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── ARCHITECTURE_FREEZE.md
│       │   │   ├── prompts/
│       │   │   │   ├── F_AKTUALITAETS_CHECK.md
│       │   │   │   ├── F_BUSINESS_INKUBATOR.md
│       │   │   │   ├── F_DOKUMENTE.md
│       │   │   │   ├── F_FACTS_VS_OPINIONS_ANALYZER.md
│       │   │   │   ├── F_FEHLINFORMATIONS_RADAR.md
│       │   │   │   ├── F_FREIE_QUELLENANFRAGE.md
│       │   │   │   ├── F_MULTIMEDIA.md
│       │   │   │   ├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md
│       │   │   │   ├── F_RISIKO_ANALYSE.md
│       │   │   │   ├── F_STANDARD_WEBSEITE.md
│       │   │   │   ├── F_TOP_3_KERNAUSSAGEN.md
│       │   │   │   ├── F_WEITERE_RELEVANTE_ASPEKTE.md
│       │   │   │   ├── _global_quality_rules.md
│       │   │   │   ├── function_registry.json
│       │   │   │   └── prompt_manifest.json
│       │   │   └── change-prompts/
│       │   │       ├── CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
│       │   │       ├── CP_GUIDELINE.md
│       │   │       └── README_CHANGE_PROMPTS.md
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── example/
│       │   │           ├── LocalContentExtractionEngine.kt
│       │   │           ├── MainActivity.kt
│       │   │           ├── RelevantorAccessibilityService.kt
│       │   │           ├── data/
│       │   │           │   ├── AnalysisRuntimeConfigs.kt
│       │   │           │   ├── AnalysisType.kt
│       │   │           │   ├── BackendFeatureConfig.kt
│       │   │           │   ├── FileProcessingHelper.kt
│       │   │           │   ├── FunctionRegistry.kt
│       │   │           │   ├── GatewayDiagnostics.kt
│       │   │           │   ├── GeminiModels.kt
│       │   │           │   ├── GeminiRepository.kt
│       │   │           │   ├── PipelineReport.kt
│       │   │           │   ├── PipelineReportStore.kt
│       │   │           │   ├── PromptEngine.kt
│       │   │           │   ├── PromptFallbackProvider.kt
│       │   │           │   ├── PromptLoader.kt
│       │   │           │   ├── ResponseNormalizer.kt
│       │   │           │   ├── RetrofitClient.kt
│       │   │           │   ├── RuntimePreflight.kt
│       │   │           │   ├── RuntimeSmokeTestHarness.kt
│       │   │           │   ├── RuntimeVerificationLayer.kt
│       │   │           │   ├── SummaryResponseParser.kt
│       │   │           │   ├── WebpageExtractor.kt
│       │   │           │   ├── YoutubeTranscriptHelper.kt
│       │   │           │   ├── YoutubeUrlDecoder.kt
│       │   │           │   ├── engine/
│       │   │           │   │   ├── AnalysisRegistryImpl.kt
│       │   │           │   │   ├── AndroidAssetPromptLoader.kt
│       │   │           │   │   ├── BaseGeminiEngine.kt
│       │   │           │   │   ├── EngineRunnerImpl.kt
│       │   │           │   │   ├── document/
│       │   │           │   │   │   └── DocumentAnalysisEngine.kt
│       │   │           │   │   ├── top3/
│       │   │           │   │   │   └── Top3KeyPointsEngine.kt
│       │   │           │   │   └── web/
│       │   │           │   │       └── WebpageAnalysisEngine.kt
│       │   │           │   ├── extraction/
│       │   │           │   │   ├── InputExtractor.kt
│       │   │           │   │   ├── InputExtractorRegistry.kt
│       │   │           │   │   ├── WebInputExtractor.kt
│       │   │           │   │   ├── DocumentInputExtractor.kt
│       │   │           │   │   └── YoutubeInputExtractor.kt
│       │   │           │   ├── local/
│       │   │           │   │   ├── RelevantorDatabase.kt
│       │   │           │   │   └── SessionStorage.kt
│       │   │           │   ├── remote/
│       │   │           │   │   └── BackendApiService.kt
│       │   │           │   ├── repository/
│       │   │           │   │   ├── AnalysisRepositoryImpl.kt
│       │   │           │   │   ├── ContentExtractionRepositoryImpl.kt
│       │   │           │   │   ├── SyncRepositoryImpl.kt
│       │   │           │   │   ├── UserRepositoryImpl.kt
│       │   │           │   │   └── YoutubeTranscriptProviderAdapter.kt
│       │   │           │   └── sync/
│       │   │           │       ├── SyncScheduler.kt
│       │   │           │       └── SyncWorker.kt
│       │   │           ├── domain/
│       │   │           │   ├── engine/
│       │   │           │   │   ├── AnalysisEngine.kt
│       │   │           │   │   ├── AnalysisRegistry.kt
│       │   │           │   │   ├── ContractValidator.kt
│       │   │           │   │   ├── EngineRunner.kt
│       │   │           │   │   ├── PromptAssetLoader.kt
│       │   │           │   │   └── validators/
│       │   │           │   │       ├── A1ContractValidator.kt
│       │   │           │   │       └── A2ContractValidator.kt
│       │   │           │   ├── model/
│       │   │           │   │   ├── AnalysisTrace.kt
│       │   │           │   │   ├── CanonicalAnalysisInput.kt
│       │   │           │   │   ├── ContentExtractionResult.kt
│       │   │           │   │   ├── DomainSummary.kt
│       │   │           │   │   ├── ExtractedContent.kt
│       │   │           │   │   └── TranscriptProvider.kt
│       │   │           │   ├── repository/
│       │   │           │   │   ├── AnalysisRepository.kt
│       │   │           │   │   ├── ContentExtractionRepository.kt
│       │   │           │   │   ├── GeminiGateway.kt
│       │   │           │   │   ├── SyncRepository.kt
│       │   │           │   │   └── UserRepository.kt
│       │   │           │   └── usecase/
│       │   │           │       ├── AnalyzeContentUseCase.kt
│       │   │           │       ├── ExtractContentUseCase.kt
│       │   │           │       ├── LoadHistoryUseCase.kt
│       │   │           │       ├── SaveAnalysisUseCase.kt
│       │   │           │       └── SyncUserDataUseCase.kt
│       │   │           └── ui/
│       │   │               ├── MainViewModel.kt
│       │   │               ├── components/
│       │   │               │   └── TakeawayCard.kt
│       │   │               └── metadata/
│       │   │                   ├── ExportFormatter.kt
│       │   │                   ├── FeatureCatalog.kt
│       │   │                   └── OutputPresentationPolicy.kt
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       ├── mipmap-anydpi-v26/
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── accessibility_service_config.xml
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/
│           ├── java/
│           │   └── com/
│           │       └── example/
│           │           ├── A1ContractValidatorTest.kt
│           │           ├── A2ContractValidatorTest.kt
│           │           ├── BaseArchitectureRegressionTest.kt
│           │           ├── BulkMigrationVerificationTest.kt
│           │           ├── ContentExtractionRegressionTest.kt
│           │           ├── ExampleRobolectricTest.kt
│           │           ├── ExampleUnitTest.kt
│           │           ├── GreetingScreenshotTest.kt
│           │           ├── PipelineReportTest.kt
│           │           ├── RelevantorSelfTestHarnessTest.kt
│           │           └── TranscriptProviderTest.kt
│           ├── screenshots/
│           │   ├── greeting.png
│           │   └── gudrun_nolte_pdf_result.png
│           └── assets/
│               └── golden/
│                   ├── DOCUMENT_SUMMARY/
│                   ├── FACTS_VS_OPINIONS/
│                   ├── FREE_SOURCE_QUERY/
│                   ├── FRESHNESS_CHECK/
│                   ├── KEY_TAKEAWAYS/
│                   ├── MISINFORMATION_RADAR/
│                   ├── MULTIMEDIA_ANALYSIS/
│                   ├── PERSPECTIVES_COUNTERPOSITIONS/
│                   ├── RELEVANT_ASPECTS/
│                   ├── RISK_ANALYSIS/
│                   └── WEB_SUMMARY/
```

---

## 1. PROMPT-SYSTEM (EXAKTER ZUSTAND)

### 1.1 Prompt-Dateien und Metadaten

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

---

### 1.2 Vollständiger Inhalt wichtiger Prompt-Dateien (Auszug)

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

#### `/assets/prompts/prompt_manifest.json`
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
  "WEITERE_RELEVANTE_ASPEKTE": "F_WEITERE_RELEVANTE_ASPEKTE.md"
}
```

---

## 2. EXECUTION PIPELINE (CODE WIRKLICHKEIT)

### 2.1 Domain-Driven Engines

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

## 3. FEATURE REGISTRY (SYSTEM-REALITÄT)

| SortOrder | Kategorie | UI Label | AnalysisType | Status |
| :--- | :--- | :--- | :--- | :--- |
| A.1 | Verstehen & Verdichten | Zusammenfassung | `STANDARD_WEBSEITE` | OK (Aktiv) |
| A.2 | Verstehen & Verdichten | 3 Kernaussagen | `TOP_3_KERNAUSSAGEN` | OK (Aktiv) |
| A.3 | Verstehen & Verdichten | Frage an die Quelle | `FREIE_QUELLENANFRAGE` | OK (Aktiv) |
| A.4 | Verstehen & Verdichten | Video- & Multimedia-Analyse | `MULTIMEDIA` | OK (Aktiv) |
| B.1 | Qualität & Einordnung | Aktualitäts-Check | `AKTUALITAETS_CHECK` | OK (Grounding) |
| B.2 | Qualität & Einordnung | Fehlinformations-Radar | `FEHLINFORMATIONS_RADAR`| OK (Grounding) |
| B.3 | Qualität & Einordnung | Fakt-oder-Meinung | `FACTS_VS_OPINIONS_ANALYZER`| OK (Aktiv) |
| B.4 | Qualität & Einordnung | Risikoanalyse | `RISIKO_ANALYSE` | OK (Aktiv) |
| B.5 | Qualität & Einordnung | Perspektiven-Finder | `PERSPECTIVES_AND_COUNTERPOSITIONS`| OK (Aktiv) |
| B.6 | Qualität & Einordnung | Weitere relevante Aspekte | `WEITERE_RELEVANTE_ASPEKTE`| OK (Aktiv) |
| E.1 | Arbeiten mit Dateien | Dokument zusammenfassen | `DOKUMENTE` | OK (Aktiv) |

*Hinweis: Visualisierungs- und Social-Media-Generierungs-Optionen sind als interaktive Platzhalter im `FeatureCatalog` markiert (`isPlaceholder = true`), welche bei Klick eine informative Dialogbox anzeigen.*
