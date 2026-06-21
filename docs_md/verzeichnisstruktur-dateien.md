# Abstractor - Verzeichnisstruktur & Dateien

Diese Datei listet die vollständige und detaillierte Verzeichnisstruktur des Projekts "Abstractor" hierarchisch auf.

## Verzeichnishierarchie

```text
/ (Root)
├── .build-outputs/                      # Build-spezifische Ausgaben (System-interne Verzeichnisse)
├── app/                                 # Das Hauptmodul der Android-App
│   ├── build.gradle.kts                # Gradle-Konfigurationsskript des App-Moduls
│   ├── proguard-rules.pro              # Filter- und Absicherungsregeln für Code-Minimierung
│   ├── GEMINI_429_TRUE_CAUSE_REPORT.md # Dokumentation zur Fehlerbehandlung von API-Limits
│   └── src/                             # Quellcode-Ordner
│       ├── androidTest/                 # Instrumentierte Android Integrationstests
│       │   └── java/
│       │       └── com/
│       │           └── example/
│       │               └── ExampleInstrumentedTest.kt
│       ├── test/                        # Lokale Unit- und Screenshot-Tests (Robolectric)
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── example/
│       │   │           ├── ExampleRobolectricTest.kt
│       │   │           ├── ExampleUnitTest.kt
│       │   │           └── GreetingScreenshotTest.kt
│       │   └── screenshots/             # Lokale visuelle Regressionstests & Screenshots
│       └── main/                        # Hauptquellen der Applikation
│           ├── AndroidManifest.xml      # Deklaration von Berechtigungen, Services & Komponenten
│           ├── assets/                  # Strukturierte Prompt-Dateien und Manifeste
│           │   └── prompts/             # Spezialisierte KI-Analyse-Prompts für Gemini
│           │       ├── _function_prompt_template.md
│           │       ├── _global_quality_rules.md
│           │       ├── EVALUATION_PROTOCOL_STANDARD_WEBSEITE.md
│           │       ├── F_AKTUALITAETS_CHECK.md
│           │       ├── F_BUSINESS_INKUBATOR.md
│           │       ├── F_DOKUMENTE.md
│           │       ├── F_FACTS_VS_OPINIONS_ANALYZER.md
│           │       ├── F_FEHLINFORMATIONS_RADAR.md
│           │       ├── F_MULTIMEDIA.md
│           │       ├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md
│           │       ├── F_RISIKO_ANALYSE.md
│           │       ├── F_STANDARD_WEBSEITE.md
│           │       ├── F_TOP_3_KERNAUSSAGEN.md
│           │       ├── PRODUCTION_FEEDBACK_STANDARD_WEBSEITE.md
│           │       ├── prompt_manifest.json
│           │       └── REAL_WORLD_EVAL_STANDARD_WEBSEITE.md
│           ├── java/                    # Kotlin-Hauptquellcode
│           │   └── com/
│           │       └── example/         # Haupt-App-Paket
│           │           ├── AbstractorAccessibilityService.kt # Barrierefreiheitsdienst zum Auslesen von App-Inhalten
│           │           ├── LocalContentExtractionEngine.kt   # Systeminterne Extraktionslogik
│           │           ├── MainActivity.kt                  # System-Einstiegspunkt & Jetpack Compose Layouts
│           │           ├── data/        # Datenhaltung, APIs, DBs & Repositories
│           │           │   ├── local/   # SQLite Raum-Datenbanken (Room Db)
│           │           │   │   ├── AbstractorDatabase.kt
│           │           │   │   ├── LocalDaos.kt
│           │           │   │   └── LocalEntities.kt
│           │           │   ├── remote/  # Externe API Definitionen (Schnittstellen)
│           │           │   │   ├── BackendApiService.kt
│           │           │   │   └── BackendModels.kt
│           │           │   ├── repository/ # Implementierung von Repositories
│           │           │   │   ├── AnalysisRepositoryImpl.kt
│           │           │   │   ├── SyncRepositoryImpl.kt
│           │           │   │   └── UserRepositoryImpl.kt
│           │           │   ├── sync/    # Synchronisierungsdienste (WorkManager)
│           │           │   │   ├── SyncScheduler.kt
│           │           │   │   └── SyncWorker.kt
│           │           │   ├── AnalysisRuntimeConfig.kt
│           │           │   ├── BackendFeatureConfig.kt
│           │           │   ├── FileProcessingHelper.kt
│           │           │   ├── GeminiModels.kt
│           │           │   ├── GeminiNetwork.kt
│           │           │   ├── PromptEngine.kt
│           │           │   ├── PromptFallbackProvider.kt
│           │           │   ├── PromptLoader.kt
│           │           │   ├── SummaryResponseParser.kt
│           │           │   ├── WebpageExtractor.kt
│           │           │   ├── YoutubeTranscriptHelper.kt
│           │           │   └── YoutubeUrlDecoder.kt
│           │           ├── domain/      # Domain-Logik (Clean Architecture)
│           │           │   ├── model/
│           │           │   │   └── DomainSummary.kt
│           │           │   ├── repository/
│           │           │   │   ├── AnalysisRepository.kt
│           │           │   │   ├── SyncRepository.kt
│           │           │   │   └── UserRepository.kt
│           │           │   └── usecase/
│           │           │       ├── AnalyzeContentUseCase.kt
│           │           │       ├── LoadHistoryUseCase.kt
│           │           │       ├── SaveAnalysisUseCase.kt
│           │           │       └── SyncUserDataUseCase.kt
│           │           └── ui/          # Benutzeroberfläche & MVVM
│           │               ├── theme/   # Jetpack Compose Style-, Farb- & Typografie-Definitionen
│           │               │   ├── Color.kt
│           │               │   ├── Theme.kt
│           │               │   └── Type.kt
│           │               ├── CurrentStateData.kt
│           │               └── MainViewModel.kt
│           └── res/                     # Android Layout-Ressourcen (Bilder, Icons, Strings)
│               ├── drawable/            # Vektografiken und Standard-Zeichenelemente
│               ├── mipmap-.../          # Launcher-App-Icons in verschiedenen Pixeldichten
│               ├── values/              # strings.xml, colors.xml, themes.xml etc.
│               └── xml/                 # XML Metafestlegungen (z.B. Accessibility Service Config)
├── assets/                              # Root-Zusatzordner für Platform-Ressourcen
│   └── .aistudio/
│       └── .gitignore
├── debug_archive/                      # Gesammelte Berichte und Spezifikationen der Entwicklungszyklen
│   ├── ABSTRACTOR_ARCHITECTURE_BACKUP_2026-06-13_08-18.md
│   ├── API_KEY_RUNTIME_VERIFICATION_2026-06-12_06-50.md
│   ├── app_CURRENT_STATE_2026-06-12_06-37.md
│   ├── app_GEMINI_429_TRUE_CAUSE_REPORT_2026-06-12_06-37.md
│   ├── CURRENT_STATE_2026-06-12_06-37.md
│   ├── CURRENT_STATE_DE_2026-06-12_06-37.md
│   ├── FAKT_ODER_MEINUNG_FORMAT_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_503_COMPARISON_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_FACTS_VS_OPINIONS_ANALYZER_IMPLEMENTATION_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_PERSPECTIVES_AND_COUNTERPOSITIONS_IMPLEMENTATION_REPORT_2026-06-12_06-37.md
│   ├── GEMINI_429_TRUE_CAUSE_REPORT_2026-06-12_06-37.md
│   ├── GEMINI_MODEL_ROOT_CAUSE_REPORT_2026-06-12_06-37.md
│   ├── Google-AI-Specs_2026-06-12_06-37.md
│   ├── PROJECT_CONTEXT_ABSTRACTOR_FULL_2026-06-12_19-30.md
│   ├── PROMPT_ARCHITECTURE_INVENTORY_2026-06-12_19-15.md
│   ├── PROMPT_ASSET_LIBRARY_CREATION_REPORT_2026-06-12_12-45.md
│   ├── SMARTPHONE_BUILD_TEST_2026-06-12_06-54.md
│   ├── TOP3_ROOT_CAUSE_2026-06-12_06-37.md
│   └── UI_UX_OPTIMIZATION_REPORT_2026-06-12_17-55.md
├── gradle/                              # Gradle Wrapper & Versionskataloge
│   └── libs.versions.toml               # Zentraler Android-Abhängigkeitskatalog (Catalog)
├── .env.example                         # Vorlage für Umgebungsvariablen / API Keys
├── .gitignore                           # Git-Ausschlusskonfiguration
├── ABSTRACTOR_ARCHITECTURE.md           # Architektur-Spezifikation und System-Zusammenhänge
├── ABSTRACTOR_BASELINE_LOCAL_FIRST.md   # Richtlinie zur lokalen Vorverarbeitung im Backend
├── ABSTRACTOR_FUNCTION_EXECUTION_MODEL.md # Verarbeitungs- und Fehlerflussdiagramm
├── ABSTRACTOR_OUTPUT_SPEC.md            # Ausgabe- und Validierungs-Formatspezifikation
├── Abstractor_debug.apk                 # Kopie der signierten Debug-APK
├── Abstractor_debug_apk.zip             # Einzeln komprimierte Version der APK
├── abstractor-debug-export.zip          # Vollständiges Benutzer-Installationspaket
├── abstractor-debug.apk                 # Direktes Benutzer-Build der APK
├── abstractor-debug.apk.sha256.txt      # Prüfsumme (SHA-256) der APK zur Integritäts-Sicherstellung
├── build.gradle.kts                     # Haupt-Gradle-Projektkonfigurationsskript
├── debug.keystore.base64                # Base64-Form des Android Signatur-Schlüssels
├── DEBUG_REPORT_INDEX_2026-06-12_06-37.md # Fehlerberichte-Indexierung
├── EXPORT_VERIFICATION.txt              # Integritätsbericht und Prüfprotokoll des Exports
├── GEMINI_429_TRUE_CAUSE_REPORT.md      # KI-Dienstauslastungsbericht und Latenzschutz
├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md # Systemaudits
├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md # Fehlerkorrekturdokumentation
├── GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md # Qualitätssicherung
├── GOOGLE_AI_STUDIO_RESPONSE_RECOVERY_AUDIT_1.md # Systemwiederherstellungsprotokoll
├── GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md # Verifizierte App-Fähigkeiten
├── GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md # Agenten-Auditierbarkeit
├── gradle.properties                    # Globale Gradle-System- und JVM-Konfigurationsvariablen
├── metadata.json                        # Projektmetadaten zur Plattformdarstellung
├── PROJECT_CONTEXT_ABSTRACTOR.md        # Kernkontext der Abstractor KI-Engine
├── README_INSTALL.txt                   # Installationshandbuch für Benutzer und Geräte
├── settings.gradle.kts                  # Deklaration aller enthaltenen Gradle-Module
└── verzeichnisstruktur-dateien.md       # (Diese Datei) Hierarchische Verzeichnisübersicht
```
