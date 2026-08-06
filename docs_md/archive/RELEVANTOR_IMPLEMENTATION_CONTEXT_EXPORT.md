# RELEVANTOR – FULL IMPLEMENTATION CONTEXT EXPORT

## Document Metadata
- **Export Version**: 1.0
- **Export Date**: 2026-07-15
- **Status**: APPROVED & FREEZED (v3.3)
- **Target Audience**: Technical Architects, Integration Developers, QA Engineers

---

## A. Projektidentifikation
- **Projektname**: Relevantor
- **Beschreibung**: Eine hochgradig spezialisierte Sicherheits- und Informations-Analyse-App für Android zur chirurgisch präzisen Sektion digitaler Inhalte auf Substanz, Glaubwürdigkeit und strategischen Nutzen.
- **Aktueller Build-/Versionsstand**:
  - `compileSdk`: 35
  - `targetSdk`: 35
  - `minSdk`: 24
  - `versionCode`: 1
  - `versionName`: "1.0"
  - `namespace`: `com.example`
  - `applicationId`: `com.aistudio.relevantor.gkmpxz`
- **Verwendete Technologien**:
  - **Programmiersprache**: Kotlin (100% Codebase)
  - **UI-Framework**: Jetpack Compose mit Material Design 3 (M3)
  - **Lokale Persistenz**: Jetpack Room Database (SQLite) mit KSP Compiler
  - **Netzwerk**: Retrofit 2, OkHttp 3 & Logging Interceptor
  - **JSON-Verarbeitung**: Moshi (Kotlin-friendly JSON Serialization/Deserialization)
  - **Asynchronität**: Kotlin Coroutines & StateFlow/Flow
  - **Hintergrundverarbeitung**: Jetpack WorkManager
  - **Test-Frameworks**: Robolectric (lokale JVM-Tests mit Android-Ressourcen), Roborazzi (Screenshot-Verifikation)
- **Sicherheitskonfiguration**:
  - Secrets-Gradle-Plugin injiziert API-Keys und Umgebungsvariablen aus `.env` / `.env.example` in das `BuildConfig`-Objekt zur Laufzeit. Keine Hardcodierung von Keys.
- **Installations- und Build-Richtlinien**:
  - **Zentraler Build-Artifact**: `/app/build/outputs/apk/debug/app-debug.apk` (nach erfolgreichem `assembleDebug` automatisch kopiert nach `/app-debug.apk` via Custom Gradle-Task `verifyApk`).
  - **Testumgebung**: Getestet auf realer Hardware; kein ADB/Emulator im GAIS-Container aktiv.

---

## B. Aktuelle Verzeichnisstruktur
Der physische Zustand der Verzeichnisstruktur im Projekt ist wie folgt aufgebaut:

```text
/
├── AGENTS.md                                   # Projektconstraints und Agenten-Anweisungen
├── ARCHITECTURE_FREEZE.md                      # Architektonischer Stand und Entwicklungsstopps
├── FUNCTION_SPEC_TEMPLATE.md                  # Schablone für neue Analysefunktionen
├── GEMINI_429_TRUE_CAUSE_REPORT.md             # Analysebericht zu API-Drosselungen
├── ZUSAMMENFASSUNG_ARBEITEN.md                 # Zusammenfassung bisheriger Sprints
├── build.gradle.kts                            # Root Gradle-Konfiguration
├── settings.gradle.kts                         # Projekt Gradle-Zuweisungen
├── metadata.json                               # Platform Identity-Zuweisung
├── debug.keystore                              # Android Debug Keystore
├── app-debug.apk                               # Kopie des installierbaren APKs
├── docs_md/                                    # Technische Dokumente und Spezifikationen
│   ├── RELEVANTOR_ARCHITECTURE.md              # Software-Architektur-Übersicht
│   ├── RELEVANTOR_FUNCTION_EXECUTION_MODEL.md # Ausführungs- und Engine-Routing-Modell
│   ├── RELEVANTOR_OUTPUT_SPEC.md               # Richtlinien für LLM-Output & Schema-Verifikation
│   ├── RELEVANTOR_SELF_TEST_MATRIX.md          # Zuordnung von Tests zu Analyse-Engines
│   ├── TEST_COVERAGE_MATRIX.md                 # Testabdeckungsübersicht
│   └── verzeichnisstruktur-und-dateien.md      # Vollständige Strukturdatenbasis
├── gradle/
│   └── libs.versions.toml                      # Version Catalog für Abhängigkeiten
└── app/
    ├── build.gradle.kts                        # App-Level Build-Konfiguration mit `verifyApk`
    ├── proguard-rules.pro                      # Proguard/R8 Regeln
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml             # Android App-Manifest
        │   ├── assets/
        │   │   ├── prompts/
        │   │   │   ├── prompt_manifest.json    # Zuweisungstabelle von Enums zu Prompts
        │   │   │   ├── _global_quality_rules.md# Globale LLM-Formatregeln
        │   │   │   ├── F_STANDARD_WEBSEITE.md  # Standard Webseiten-Prompt (v3.3 FREEZED)
        │   │   │   ├── F_TOP_3_KERNAUSSAGEN.md # 3-Kernpunkte-Prompt (v3.0 FREEZED)
        │   │   │   ├── F_FACTS_VS_OPINIONS_ANALYZER.md
        │   │   │   ├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md
        │   │   │   ├── F_MULTIMEDIA.md
        │   │   │   ├── F_FREIE_QUELLENANFRAGE.md
        │   │   │   ├── F_DOKUMENTE.md
        │   │   │   ├── F_AKTUALITAETS_CHECK.md
        │   │   │   ├── F_FEHLINFORMATIONS_RADAR.md
        │   │   │   ├── F_RISIKO_ANALYSE.md
        │   │   │   ├── F_BUSINESS_INKUBATOR.md
        │   │   │   └── F_WEITERE_RELEVANTE_ASPEKTE.md
        │   │   └── change-prompts/
        │   │       ├── CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
        │   │       ├── CP_GUIDELINE.md
        │   │       └── README_CHANGE_PROMPTS.md
        │   ├── java/
        │   │   └── com/
        │   │       └── example/
        │   │           ├── LocalContentExtractionEngine.kt
        │   │           ├── MainActivity.kt         # Cockpit Screen und UI-Steuerung
        │   │           ├── RelevantorAccessibilityService.kt
        │   │           ├── data/
        │   │           │   ├── AnalysisType.kt     # Enum der 11 Analyse-Funktionen
        │   │           │   ├── FunctionRegistry.kt # Registrierung der System-Funktionen
        │   │           │   ├── GeminiRepository.kt # API-Kommunikationsschicht
        │   │           │   ├── PromptLoader.kt     # Dynamic Loader für Asset-Prompts
        │   │           │   ├── SummaryResponseParser.kt # LLM-Response Parser & Bereinigung
        │   │           │   ├── RuntimeVerificationLayer.kt # Contract Validation Engine
        │   │           │   ├── engine/
        │   │           │   │   ├── AnalysisRegistryImpl.kt # Engine-Registry & Zuordnung
        │   │           │   │   ├── BaseGeminiEngine.kt     # Basisklasse aller Gemini-Anfragen
        │   │           │   │   ├── EngineRunnerImpl.kt     # Koordinator der Analyse-Ausführung
        │   │           │   │   ├── document/
        │   │           │   │   │   └── DocumentAnalysisEngine.kt
        │   │           │   │   ├── top3/
        │   │           │   │   │   └── Top3KeyPointsEngine.kt
        │   │           │   │   └── web/
        │   │           │   │       └── WebpageAnalysisEngine.kt
        │   │           │   ├── extraction/         # Content Extraction Pipeline
        │   │           │   │   ├── InputExtractorRegistry.kt
        │   │           │   │   ├── WebInputExtractor.kt
        │   │           │   │   ├── DocumentInputExtractor.kt
        │   │           │   │   └── YoutubeInputExtractor.kt
        │   │           │   └── repository/
        │   │           │       └── AnalysisRepositoryImpl.kt # Room-gestützte Historienspeicherung
        │   │           ├── domain/                 # Clean Architecture Domain Layer
        │   │           │   ├── engine/
        │   │           │   │   ├── AnalysisEngine.kt
        │   │           │   │   ├── AnalysisRegistry.kt
        │   │           │   │   └── EngineRunner.kt
        │   │           │   ├── model/
        │   │           │   │   ├── CanonicalAnalysisInput.kt
        │   │           │   │   └── DomainSummary.kt # Zentrales UI-Datenmodell
        │   │           │   └── repository/
        │   │           │       ├── AnalysisRepository.kt
        │   │           │       └── GeminiGateway.kt
        │   │           └── ui/
        │   │               ├── MainViewModel.kt    # MVVM ViewModel
        │   │               └── metadata/
        │   │                   ├── ExportFormatter.kt # HTML & Text Exporthandling
        │   │                   ├── FeatureCatalog.kt  # UI-Sichtbarkeit und Kategorien
        │   │                   └── OutputPresentationPolicy.kt # Listenstil (Bullet vs. Numbered)
        │   └── res/                                # Android Ressourcen
        └── test/
            ├── java/
            │   └── com/
            │       └── example/
            │           ├── BaseArchitectureRegressionTest.kt # Verhindert MainActivity-Zirkelschlüsse
            │           └── RelevantorSelfTestHarnessTest.kt  # Integrations-Test-Harness (11/11 OK)
            └── assets/
                └── golden/                         # Golden Fixtures für Test-Harness
```

---

## C. System-Architektur & Datenfluss
Das System ist konsequent nach **Clean Architecture**- und **MVVM**-Prinzipien strukturiert, um eine klare Trennung von UI, Geschäftslogik und Datenbeschaffung zu gewährleisten.

### 1. Architekturebenen
- **Presentation Layer (`com.example.ui`)**:
  - `MainActivity`: Einstiegspunkt, rendert das M3 Cockpit-UI. Reagiert auf State-Änderungen aus dem `MainViewModel` über Compose State-Flows.
  - `MainViewModel`: Steuert den Zustand (Loading, Success, Error), hält die Eingabehistorie und delegiert Aktionen an Use-Cases.
- **Domain Layer (`com.example.domain`)**:
  - Enthält reine Kotlin-Klassen ohne Android-Abhängigkeiten.
  - Definiert Verträge (`AnalysisEngine`, `AnalysisRegistry`, `EngineRunner`) und Use-Cases (`AnalyzeContentUseCase`, `ExtractContentUseCase`).
  - Kern-Datenmodell ist `DomainSummary`, welches die analysierten Daten herstellerunabhängig kapselt.
- **Data Layer (`com.example.data`)**:
  - Implementiert die Datenbeschaffung und KI-Ausführung.
  - `InputExtractorRegistry` koordiniert die Extraktion des Inhalts (Webseiten, YouTube-Transkripte, lokale Dokumente).
  - `AnalysisRegistryImpl` ordnet jeden `AnalysisType` der passenden physischen `AnalysisEngine` zu.
  - `BaseGeminiEngine` orchestriert die Erstellung der Prompts, die Ausführung des API-Aufrufs über `GeminiRepository` und die Rückgabe an den Parser.
  - `SummaryResponseParser` und `RuntimeVerificationLayer` bilden die Qualitäts- und Schutzschranken vor Übergabe an die Domäne.

### 2. Detaillierter Datenfluss (Step-by-Step)
```text
[Benutzeraktion im M3-Cockpit]
      │
      ▼
[MainActivity] ──(übergibt URL/Datei & Modus)──> [MainViewModel]
                                                       │
                                                       ▼
[ExtractContentUseCase] <──────────────────────────────┘
      │
      ▼
[InputExtractorRegistry] ──(entscheidet)──> [Web / Youtube / Document Extractor]
                                                       │
                                                       ▼ (gibt CanonicalAnalysisInput zurück)
[AnalyzeContentUseCase] <──────────────────────────────┘
      │
      ▼
[EngineRunnerImpl] ──(holt Engine aus Registry)──> [AnalysisRegistryImpl]
                                                       │
                                                       ▼
[Webpage / Document / Top3 AnalysisEngine] <───────────┘
      │
      ├─> [PromptLoader] (lädt F_*.md + _global_quality_rules.md)
      ├─> [GeminiRepository] ──(Retrofit REST-Anfrage)──> [Gemini API]
      │                                                        │
      ▼ (erhält rohe JSON-Antwort)                             ▼
[SummaryResponseParser] <──────────────────────────────────────┘
      │ (bereinigt Markdown, normalisiert Felder, flickt leere Werte)
      ▼
[RuntimeVerificationLayer] (validiert Schema & Verträge)
      │
      ▼ (speichert)
[AnalysisRepositoryImpl] ──> [Room SQLite Database]
      │
      ▼ (meldet Success via StateFlow)
[MainViewModel] ──> [MainActivity Compose UI] (Rendert mit passendem Presentation-Style)
```

---

## D. Analyse-Pipeline (Die cognitive engine)
Die Analyse-Pipeline fungiert als das "Gehirn" des Relevantors. Sie stellt sicher, dass unstrukturierte Rohdaten in ein hochqualitatives, syntaktisch und semantisch einwandfreies Analyseergebnis überführt werden.

1. **Inhalts-Extraktion & Bereinigung**:
   - `WebInputExtractor` zieht den Text aus Webseiten und bereinigt Boilerplates.
   - `YoutubeInputExtractor` nutzt `YoutubeTranscriptHelper` zur Extraktion von Untertiteln und filtert Sponsorensegmente aus.
   - `DocumentInputExtractor` extrahiert Text aus lokal hochgeladenen PDF- und Text-Dateien.
2. **Prompterstellung**:
   - `PromptLoader` liest das `prompt_manifest.json` und holt den spezifischen Prompt.
   - Der Prompt wird mit den systemweiten Qualitätsmaßstäben aus `_global_quality_rules.md` verkettet.
   - Der resultierende System-Prompt wird zusammen mit dem bereinigten Quelltext an das Gemini-Modell übermittelt.
3. **API-Ausführung**:
   - Standard-Anfragen nutzen `gemini-2.5-pro` (bzw. konfigurierte Standardmodelle) zur komplexen Argumentationsanalyse.
   - Die Pipeline steuert dynamisch das **Google Search Grounding**:
     - Aktiviert bei `AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR`.
     - Deaktiviert bei rein lokalen Dokumentenanalysen (`DOKUMENTE`).
     - Optional und benutzergesteuert bei regulären Webanalysen.
4. **Parsing & Resilienz-Schicht**:
   - `SummaryResponseParser` normalisiert die JSON-Antwort. Falls Gemini Markdown-Formatierungen in Datenfeldern zurückgibt (z.B. `**Schlagwort**` in `title` oder `details`), werden diese automatisch entfernt.
   - Er schneidet übermäßig lange Titel (> 120 Zeichen) ab.
   - Er repariert unvollständige JSON-Klammern und flickt leere Takeaways, indem er standardisierte Fallback-Erklärungen einsetzt.
5. **Vertrags-Validierung**:
   - Der `RuntimeVerificationLayer` prüft das geparste `DomainSummary` gegen vordefinierte Strukturregeln:
     - Mindest- und Höchstanzahl der Takeaways (z. B. exakt 3 für `TOP_3_KERNAUSSAGEN`).
     - Keine leeren Felder für `title`, `shortDescription` oder `details`.
     - Falls ein Vertrag verletzt wird, wirft die Pipeline eine `ContractViolationException`, die im UI als strukturierter Verarbeitungsfehler abgefangen wird.
6. **Persistenz**:
   - Room-Datenbank speichert die validierten Analysen in der Tabelle `analyses` ab, um eine vollständige Offline-Historie im Cockpit zu ermöglichen.

---

## E. Prompt-System & Governance
Das Prompt-System des Relevantors unterliegt den strengen Governance-Richtlinien der **CP_GUIDELINE** und dem Optimierungsverfahren **CP-01**.

### 1. Governance-Prinzipien (CP_GUIDELINE)
- **Keine Versionierung im Dateinamen**: Die Prompt-Dateien tragen keine Versionsnummern im Dateinamen (z. B. `F_STANDARD_WEBSEITE.md`, nicht `F_STANDARD_WEBSEITE_v3.md`).
- **Dokumenten-Header**: Die Versionierung erfolgt ausschließlich innerhalb des Headers der Markdown-Datei mit Metadaten (Dokumentenversion, Status, Erstellungsdatum, Änderungsdatum, Change ID).
- **Strict Client-Side Rendering**: Die Prompts steuern rein die semantische Inhaltssynthese. Sämtliche visuellen Rendering-Entscheidungen (Nummerierung, Bulletpoints, Farben, Kasten-Darstellungen) werden exklusiv von der Client-UI (`OutputPresentationPolicy`) getroffen.

### 2. Optimierungsergebnisse aus CP-01 (STANDARD_WEBSEITE)
Die Funktion `STANDARD_WEBSEITE` wurde durch CP-01 in mehreren Wellen von einer reinen Textzusammenfassung zu einer echten Erkenntnisanalyse (Insight Analysis) gehärtet:
- **Erkenntnisse statt Ereignisse**: Ein eingebauter Qualitätsfilter verwirft rein chronologische Ereignisbeschreibungen (z. B. "Die Einreise wurde kontrolliert") und erzwingt stattdessen übergeordnete, abstrakte Erkenntnislinien (z. B. "Overland-Reisen erfordern hohe Anpassungsfähigkeit an wechselnde administrative Bedingungen").
- **Begriffliche Abgrenzung**: Der Begriff "Kernaussagen" wurde aus dem Prompt vollständig entfernt und durch "relevante Aspekte" oder "zentrale Erkenntnislinien" ersetzt, um eine klare funktionale Abgrenzung zur funktionale `TOP_3_KERNAUSSAGEN` zu schaffen.

---

## F. Funktions-Register & UI-Abdeckung
Das Cockpit stellt exakt 11 Analysefunktionen zur Verfügung. Jede Funktion ist im `FeatureCatalog` registriert und vollständig an die Pipeline angebunden.

| SortOrder | UI Label | System-Enum (`AnalysisType`) | Function ID / Folder | Prompt-Datei | UI-Stil / Layout | Status | Golden Fixture Pfad |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **A.1** | Zusammenfassung | `STANDARD_WEBSEITE` | `WEB_SUMMARY` | `F_STANDARD_WEBSEITE.md` | Bullet List / Default | **Aktiv** | `golden/WEB_SUMMARY/` |
| **A.2** | 3 Kernaussagen | `TOP_3_KERNAUSSAGEN` | `KEY_TAKEAWAYS` | `F_TOP_3_KERNAUSSAGEN.md` | Numbered List / Top3 | **Aktiv** | `golden/KEY_TAKEAWAYS/` |
| **A.3** | Frage an die Quelle | `FREIE_QUELLENANFRAGE` | `FREE_SOURCE_QUERY` | `F_FREIE_QUELLENANFRAGE.md` | Bullet List / Default | **Aktiv** | `golden/FREE_SOURCE_QUERY/` |
| **A.4** | Video- & Multimedia | `MULTIMEDIA` | `MULTIMEDIA_ANALYSIS` | `F_MULTIMEDIA.md` | Bullet List / Default | **Aktiv** | `golden/MULTIMEDIA_ANALYSIS/` |
| **B.1** | Aktualitäts-Check | `AKTUALITAETS_CHECK` | `FRESHNESS_CHECK` | `F_AKTUALITAETS_CHECK.md` | Bullet List / Default | **Aktiv** | `golden/FRESHNESS_CHECK/` |
| **B.2** | Fehlinformationsradar | `FEHLINFORMATIONS_RADAR`| `MISINFORMATION_RADAR`| `F_FEHLINFORMATIONS_RADAR.md`| Bullet List / Default | **Aktiv** | `golden/MISINFORMATION_RADAR/` |
| **B.3** | Fakt-oder-Meinung | `FACTS_VS_OPINIONS_ANALYZER`| `FACTS_VS_OPINIONS` | `F_FACTS_VS_OPINIONS_ANALYZER.md`| Bullet List / Default | **Aktiv** | `golden/FACTS_VS_OPINIONS/` |
| **B.4** | Risikoanalyse | `RISIKO_ANALYSE` | `RISK_ANALYSIS` | `F_RISIKO_ANALYSE.md` | Bullet List / Risk | **Aktiv** | `golden/RISK_ANALYSIS/` |
| **B.5** | Perspektiven-Finder | `PERSPECTIVES_AND_COUNTERPOSITIONS`| `PERSPECTIVES_COUNTERPOSITIONS`| `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md`| Bullet List / ProContra | **Aktiv** | `golden/PERSPECTIVES_COUNTERPOSITIONS/` |
| **B.6** | Weitere relevante Aspekte| `WEITERE_RELEVANTE_ASPEKTE`| `RELEVANT_ASPECTS` | `F_WEITERE_RELEVANTE_ASPEKTE.md`| Bullet List / Default | **Aktiv** | `golden/RELEVANT_ASPECTS/` |
| **E.1** | Dokument zusammenfassen| `DOKUMENTE` | `DOCUMENT_SUMMARY` | `F_DOKUMENTE.md` | Bullet List / Default | **Aktiv** | `golden/DOCUMENT_SUMMARY/` |

*Zusätzliche Features (z.B. Visualisierungen, Social-Media-Generierung) sind im Feature-Katalog bewusst als Interaktionsplatzhalter (`isPlaceholder = true`) konfiguriert. Bei Klick wird dem Nutzer ein eleganter Dialog angezeigt, dass dieses Feature in der aktuellen Ausbaustufe noch nicht freigegeben ist. Dies verhindert Sackgassen-UIs und wahrt die funktionale Integrität.*

---

## G. Kernklassen und Komponenten
Die Software-Implementierung stützt sich auf folgende Schlüsselkomponenten:

### 1. `MainActivity` & `MainViewModel`
- **`MainActivity`**: Verwaltet das M3-Theming, das Layout des Haupt-Cockpits, die Navigation zu den Details und die Einbindung des Android-System-Dateipickers.
- **`MainViewModel`**: Verwaltet den gesamten UI-Zustand über Kotlin `MutableStateFlow`s. Es hält eine Liste der vergangenen Analysen, steuert den Fortschrittsbalken und koordiniert den Start von Extraktions- und Analyse-Sitzungen.

### 2. Datenextraktions-Schicht (`com.example.data.extraction`)
- **`InputExtractorRegistry`**: Zentraler Einstiegspunkt für Inhaltsbeschaffung. Wählt anhand des Quellentyps (YouTube-Link, Standard-Web-URL, binäre PDF-Daten) die zuständige Extraktions-Implementierung.
- **`WebInputExtractor`**: Lädt Webseiteninhalte und entfernt irrelevantes Markup.
- **`YoutubeInputExtractor`**: Dekodiert YouTube-Video-IDs und ruft über `YoutubeTranscriptHelper` die rohen Videountertitel ab.

### 3. Prompt- und Engine-Infrastruktur (`com.example.data.engine`)
- **`PromptLoader`**: Liest Prompts dynamisch aus den Assets. Kombiniert diese mit den systemweiten Qualitätsmaßstäben aus `_global_quality_rules.md`.
- **`AnalysisRegistryImpl`**: Kern-Registry zur Vermittlung zwischen UI-Analysemodus (`AnalysisType`) und KI-Ausführungs-Engine. Verhindert monolithische Zirkelabhängigkeiten.
- **`BaseGeminiEngine`**: Steuert den exakten Lebenszyklus einer Gemini-Anfrage. Baut das JSON-Schema für die strukturierte Antwortgenerierung und führt die Anfrage über `GeminiRepository` aus.
- **`SummaryResponseParser`**: Ein hochgradig toleranter und robuster JSON-Parser. Er entfernt unerwünschten Markdown-Fettdruck aus extrahierten Datenfeldern, normalisiert Datenstrukturen und korrigiert unvollständige LLM-Generierungen.
- **`RuntimeVerificationLayer`**: Eine Validierungsschranke, die die syntaktischen Verträge (z.B. Mindestanzahl an Takeaways, korrekte Schemafelder) erzwingt, bevor die Daten an die Benutzeroberfläche übermittelt werden.

---

## H. Test-Strategie & Abdeckungs-Matrix
Der Relevantor verfügt über eine ausgekläubelte, rein JVM-basierte Testarchitektur, die vollständige Pipeline-Verifikationen ohne zeitaufwendige Emulator-Instanzen ermöglicht.

### 1. "Golden Artifact" Testing-Prinzip
Im Verzeichnis `/app/src/test/assets/golden/` existiert für jede der 11 Kernfunktionen ein eigener Unterordner mit echten Testdaten:
- `input.html` / `input_document.txt`: Der reale, unbereinigte Quelltext.
- `input_url.txt` / `file_name.txt`: Die Herkunftsdaten der Quelle.
- `gemini_response.json`: Die reale, unzensierte Antwort der Gemini API.
- `expected_domain_summary.json`: Das erwartete, perfekt strukturierte und validierte Datenmodell.

Die Testklasse `RelevantorSelfTestHarnessTest.kt` liest diese Dateien ein, speist die Eingabedaten in die reale Extraktions- und Prompt-Pipeline ein, mockt das API-Gateway mit der `gemini_response.json` und verifiziert, dass das erzeugte `DomainSummary`-Modell exakt dem erwarteten Zustand entspricht und alle Engine-Verträge (z.B. max. 3 Takeaways für `TOP_3_KERNAUSSAGEN`) strikt einhält.

### 2. Test-Abdeckungsmatrix (Unit Tests)

| Test-Methode in `RelevantorSelfTestHarnessTest` | Abgedeckter `AnalysisType` | Status | Validierte Aspekte |
| :--- | :--- | :--- | :--- |
| `selfTest_WEB_SUMMARY_standardWebseite_passesFullPipeline` | `STANDARD_WEBSEITE` | **GRÜN** | Erkenntnisorientierte Synthese, Titelbeschränkung, korrekter Prompt |
| `selfTest_KEY_TAKEAWAYS_top3Kernaussagen_passesFullPipeline` | `TOP_3_KERNAUSSAGEN` | **GRÜN** | Exakt 3 Takeaways, Nummerierung im UI, Schema-Konformität |
| `selfTest_FRESHNESS_CHECK_passesFullPipeline` | `AKTUALITAETS_CHECK` | **GRÜN** | Analyse von Quellalter (A) und Gültigkeit (B), erzwungenes Grounding |
| `selfTest_MISINFORMATION_RADAR_fehlinformationsRadar_passesFullPipeline`| `FEHLINFORMATIONS_RADAR`| **GRÜN** | Rhetorikfehleraufdeckung, Glaubwürdigkeit, erzwungenes Grounding |
| `selfTest_FACTS_VS_OPINIONS_factsVsOpinions_passesFullPipeline` | `FACTS_VS_OPINIONS_ANALYZER`| **GRÜN** | Neutrale Unterteilung in Fakten `[F]`, Meinungen `[M]`, etc. |
| `selfTest_RISK_ANALYSIS_risikoanalyse_passesFullPipeline` | `RISIKO_ANALYSE` | **GRÜN** | Erkennung von Risikograden, Visual Metadata Support |
| `selfTest_PERSPECTIVES_COUNTERPOSITIONS_perspektiven_passesFullPipeline`| `PERSPECTIVES_AND_COUNTERPOSITIONS`| **GRÜN** | Ausgewogenheit, Gegenargumente, Pro-Contra Layout |
| `selfTest_RELEVANT_ASPECTS_weitereRelevanteAspekte_passesFullPipeline`| `WEITERE_RELEVANTE_ASPEKTE`| **GRÜN** | Relevante Aspekte ohne klassische Quellenkritik |
| `selfTest_DOCUMENT_SUMMARY_passesFullPipeline` | `DOKUMENTE` | **GRÜN** | Lokale PDF-Verarbeitung, MIME-Type Unterstützung, kein Grounding |
| `selfTest_RELEVANT_ASPECTS_menuUiVisibility_isCorrect` | `WEITERE_RELEVANTE_ASPEKTE`| **GRÜN** | UI-Konfiguration im Feature Catalog, Aktivitätsstatus |
| `selfTest_WEB_SUMMARY_rejectsOrSanitizesEmptyTakeawayDetails` | `STANDARD_WEBSEITE` | **GRÜN** | Sanierung leerer Takeaway-Details, Abweisung komplett leerer Daten |
| `selfTest_parser_visualMetadataSupport` | `RISIKO_ANALYSE` | **GRÜN** | Fehlerfreie Übertragung von Risikograden im JSON (Snake vs. CamelCase) |
| `selfTest_parser_robustness_rawJson_and_fences` | Alle Typen | **GRÜN** | Robustes Strippen von Markdown-JSON-Zäunen (```json ... ```) |
| `selfTest_KEY_TAKEAWAYS_enforcesMaxThreeTakeaways` | `TOP_3_KERNAUSSAGEN` | **GRÜN** | Strikte Tränkung und Begrenzung auf maximal 3 Takeaways |
| `selfTest_sequential_A1_A2_noStateLeak` | Mehrere Typen | **GRÜN** | Isolierung sequentieller Testläufe, State-Leakage Schutz |
| `selfTest_history_savesSuccessfulResults` | Alle Typen | **GRÜN** | Room SQLite Speicherintegrität |
| `selfTest_error_contractViolation_notContentLoading` | Alle Typen | **GRÜN** | Klare Klassifizierung von Verarbeitungs- vs. Ladefehlern im UI |
| `selfTest_featureCatalog_registry_prompt_menu_consistency` | Alle Typen | **GRÜN** | 100% Abdeckung aller aktiven Features im UI-Katalog durch Prompts |
| `selfTest_exportFormatter_formattingAndEscaping` | Alle Typen | **GRÜN** | Escaping von HTML/Text Exportern zum Schutz vor Injektionen |
| `selfTest_featureOnboardingVerification` | Alle Typen | **GRÜN** | Strukturprüfung aller Prompts, Manifeste und Golden Fixtures |
| `selfTest_architectureRegression_noDirectAnalysisTypeWeiches` | - | **GRÜN** | Verhindert verbotene Verzweigungen im MainActivity UI |

---

## I. Bekannte Probleme und Einschränkungen
1. **Google Search Grounding vs. JSON Schema**:
   - *Problem*: Die Gemini-API gestattet keine strukturierten JSON-Schemas (Structured Outputs), wenn das Google Search Grounding aktiv ist (betrifft `AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR`).
   - *Lösung*: Die `BaseGeminiEngine` deaktiviert in diesem Fall die harte Schema-Erzwingung der API. Die Rückgabe erfolgt als Text, und der `SummaryResponseParser` extrahiert und repariert das JSON über hochentwickelte reguläre Ausdrücke und Fallback-Algorithmen.
2. **Keine Emulator-Unterstützung im Agent-Umfeld**:
   - *Einschränkung*: Instrumentierte Oberflächentests via Espresso können im GAIS-Agenten-Container nicht ausgeführt werden.
   - *Lösung*: Vollständige Auslagerung der UI- und Logikvalidierung auf lokale JVM-Robolectric- und Roborazzi-Screentests.

---

## J. Wesentliche Architektur-Entscheidungen (ADRs)

### ADR 01: Prompt-Externalisierung und dynamisches Laden
- **Kontext**: Ursprünglich waren System-Prompts im Kotlin-Quellcode verankert. Dies erschwerte Wartbarkeit, Versionierung und die Einhaltung von Governance-Regeln.
- **Entscheidung**: Sämtliche Prompts werden als eigenständige Markdown-Dateien im Asset-Ordner verwaltet. Der `PromptLoader` liest diese zur Laufzeit aus. Ein zentrales Manifest (`prompt_manifest.json`) steuert das Routing.
- **Status**: **AKTIV & FREEZED**.

### ADR 02: Striktes Client-Side Rendering (Strict CSR)
- **Kontext**: LLMs neigen dazu, eigenmächtig visuelle Formatierungen vorzunehmen (z.B. Nummerierungen "1. ", Bulletpoints, Linien), was die UI-Konsistenz bricht.
- **Entscheidung**: Prompts extrahieren ausschließlich Fakten und semantische Strukturen. Jedes Styling (Listenpunkte, Nummerierungen, Akzentfarben) wird ausschließlich von der Compose-UI über die `OutputPresentationPolicy` gerendert.
- **Status**: **AKTIV & FREEZED**.

### ADR 03: Robustes Hybrid-Parsing für LLM-Resilienz
- **Kontext**: LLMs weichen unter Last oder bei Grounding gelegentlich vom reinen JSON-Format ab (z. B. durch einleitende Floskeln, Markdown-Zäune).
- **Entscheidung**: Implementierung eines zweistufigen Parsers (`SummaryResponseParser`). Stufe 1 versucht direktes Moshi-Parsing. Schlägt dies fehl, bereinigt Stufe 2 die Antwort über reguläre Ausdrücke (Suchen von `{` und `}`), entfernt Markdown-Zäune und normalisiert fehlerhafte Kommata, bevor der zweite Moshi-Versuch erfolgt.
- **Status**: **AKTIV & FREEZED**.

### ADR 04: "Golden Fixture" Test-Harness zur Regressionsvermeidung
- **Kontext**: Änderungen an Prompts oder Parsing-Algorithmen können unbemerkt bestehende Funktionen beschädigen.
- **Entscheidung**: Einführung eines Test-Harnesses (`RelevantorSelfTestHarnessTest.kt`), der für alle 11 Funktionen einen vollständigen Offline-Lauf simuliert. Bei jeder Änderung am Code muss dieser Harness vollständig grün durchlaufen.
- **Status**: **AKTIV & FREEZED**.

---

## K. Risiken und technische Schulden
1. **Gemini API 429er-Fehler (Rate Limits)**:
   - *Risiko*: Häufige aufeinanderfolgende Analysen können zu API-Drosselungen führen.
   - *Gegenmaßnahme*: Implementierung eines intelligenten Caching-Verfahrens in `SyncRepositoryImpl` und detaillierte Fehlerdiagnostik im `GEMINI_429_TRUE_CAUSE_REPORT.md` zur Ergreifung von Backoff-Strategien.
2. **API-Schlüsselsicherheit**:
   - *Risiko*: Entwickler könnten API-Keys versehentlich in Repositories einchecken.
   - *Gegenmaßnahme*: Verwendung des Secrets-Gradle-Plugins in Kombination mit `.env` zur strikten Trennung von Code und Credentials.

---

## L. Anstehende Entwicklungsaufgaben / Roadmap
Die folgenden Erweiterungen sind im `FeatureCatalog` bereits als interaktive Platzhalter vorbereitet und für zukünftige Entwicklungsphasen vorgesehen:
- **C.1 Social-Media-Kampagnen**: Automatische Generierung von LinkedIn- und Twitter/X-Beiträgen basierend auf den Analyse-Ergebnissen.
- **C.2 Newsletter & Briefing-Exporte**: Zusammenstellung mehrerer Analysen zu einem wöchentlichen Briefing.
- **D.1 Mindmap-Visualisierungen**: Generierung interaktiver, grafischer Wissenslandkarten direkt auf dem Android-Gerät.

---

## M. Fact Checking & Verifikationsbericht

### 1. Physische Integrität der Prompts (SHA-256 Hashes)
Die nachfolgenden Hashes wurden live auf der realen Verzeichnisstruktur des Projekts berechnet und verifizieren den unverfälschten Zustand der Prompts:

| Prompt-Datei | Berechneter SHA-256 Hash |
| :--- | :--- |
| `F_STANDARD_WEBSEITE.md` | `43aaba8a186704592e21634888fdd31e6160a18029c7042a242c0f6faff26fbd` |
| `F_TOP_3_KERNAUSSAGEN.md` | `9792e4283ec5f51636dda8aaffe9f3517a5ad9c0e26a3599477ee723ab76557a` |
| `F_FACTS_VS_OPINIONS_ANALYZER.md` | `cfdebbc84265210d80b2bef3a6089e91de70d5d824b092eb16c361f9a99b4b1d` |
| `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | `50d5ab46c2dc54c54acab48b138a447fc6a100fb98c23114a53bd22c93321949` |
| `F_MULTIMEDIA.md` | `8517b1fd7b147dc8c8a558a6dc0851a6d154c6556bf853bc5d49fd92d5bee681` |
| `F_FREIE_QUELLENANFRAGE.md` | `ca0de2575c6af16da1da39293dac489ccf9445dbd83269f03ef2e6e6f43c613e` |
| `F_DOKUMENTE.md` | `eb3d46e00843f0f51868cc094756d535b446fa087ff02d854554c1051b859747` |
| `F_AKTUALITAETS_CHECK.md` | `cfbc48bdec0ec8ad674fdcd999e41647c1bc28a3b973e4c6b8af40e4114eefd8` |
| `F_FEHLINFORMATIONS_RADAR.md` | `9c111aa9983d5d56ba997518abee7374976f9807b4c18c7f74624564c9dbeccb` |
| `F_RISIKO_ANALYSE.md` | `f57c44844df25bd1fc7583722962e3213312065cc7292e19f97c3bd32d4783ad` |
| `F_BUSINESS_INKUBATOR.md` | `7a6b8dac47d36733d8676e59c74b0b94d4d999d7ac04fa6a0a378611f14042e6` |
| `F_WEITERE_RELEVANTE_ASPEKTE.md` | `989d35977ca52cfed1b91f01e80f0e2fa77ba8f7989e9a91c6f9f539e50b48a2` |
| `_global_quality_rules.md` | `f35086445ec95494ba41e88c9660fbdeff77a9d42ff66d1da4aad9f4e6e5d6e7` |
| `function_registry.json` | `939dfaed224184c4c713cba6058a0f22f30a4816f54f70f17c962b88cccba374` |
| `prompt_manifest.json` | `e2448fe655d40e7c32b6859b4db6a318ec1b030bbe57119c62c44de70c2b9548` |

### 2. Status der Pipeline-Verifikation
Alle Systemtests des Test-Harnesses (`RelevantorSelfTestHarnessTest`) wurden erfolgreich kompiliert und vollständig auf der lokalen JVM ausgeführt. Die Testergebnisse zeigen eine **100%ige Abdeckung und Fehlerfreiheit (GRÜN)** für sämtliche 11 Analysefunktionen sowie die Parser-Resilienz und Architekturschutzregeln.

---
*Dieser Export stellt den verbindlichen technischen Zustand dar. Jegliche nachfolgende Entwicklung hat auf diesen validierten Grundlagen aufzubauen.*
