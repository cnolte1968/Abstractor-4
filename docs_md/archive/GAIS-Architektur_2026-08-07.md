# Technical Architecture: Relevantor

**Stand:** 2026-08-09 17:20:40 ICT  
**Status:** Produkionsnah, Verifiziert & Kompilierbar (Clean Architecture + Supabase Backend Foundation MVP 1C)  

---

## 1. Systemübersicht & Schichtentrennung

Die Anwendung **Relevantor** basiert auf einer modernen, entkoppelten Android-Architektur unter strikter Einhaltung der Clean-Architecture-Prinzipien und des MVVM-Musters (Model-View-ViewModel). Das System teilt sich in klar abgegrenzte Schichten auf, um Wartbarkeit, Testbarkeit und Erweiterbarkeit ohne Seiteneffekte zu gewährleisten.

### Schichtenmodell

- **UI Layer (Presentation)**: Jetpack Compose mit Material 3, Single-Activity-Architektur (`MainActivity`), adaptiven Layouts (`BoxWithConstraints`) und reaktiver Zustandshaltung via `MainViewModel` und `StateFlow`.
- **Domain Layer (Business Logic)**: Anwendungsfälle (`AnalyzeContentUseCase`, `ExtractContentUseCase`, `SaveAnalysisUseCase`), Koordinatoren (`LocationQuestionCoordinator`) und Vertragerfüllung (`ContractValidator`).
- **Data Layer (Data & Remote)**: Repositories (`AnalysisRepositoryImpl`, `ContentExtractionRepositoryImpl`), Inhalts-Extraktoren (`WebInputExtractor`, `YoutubeInputExtractor`, `DocumentInputExtractor`), lokale Datenbanken (`RelevantorDatabase`, `SessionStorage`) und Remote-Clients (`GeminiRepository`, `SupabaseApiService`).
- **Engine Layer (Processing & LLM Execution)**: Modularisierte Engines (`BaseGeminiEngine`, `WebpageAnalysisEngine`, `DocumentAnalysisEngine`, `LocationQuestionEngine`, `Top3KeyPointsEngine`) zur Vorbereitung von Requests und Kommunikation mit der Gemini REST API.
- **Backend / Supabase Layer (MVP 1C)**: Direct REST Connection zu Supabase PostgreSQL (`SupabaseApiService`), Status-Preflight-Checker (`SupabaseSystemStatusChecker`, `RuntimePreflight`), Singleton-Schema `system_status` (`backend_version = 1`).
- **Asset & Prompt Layer**: Dateibasierte Prompt-Verwaltung in `app/src/main/assets/prompts/` inklusive Manifesten (`prompt_manifest.json`, `function_registry.json`) und globalen Qualitätsregeln (`_global_quality_rules.md`).

### ASCII-Architekturdiagramm

```text
+-----------------------------------------------------------------------+
|                              UI LAYER                                 |
|   MainActivity (Jetpack Compose M3) <---> MainViewModel (StateFlow)   |
+-----------------------------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                             DOMAIN LAYER                              |
|   AnalyzeContentUseCase / ExtractContentUseCase / SaveAnalysisUseCase |
|                      ContractValidator & Coordinators                 |
+-----------------------------------------------------------------------+
                                    |
          +-------------------------+-------------------------+
          |                                                   |
          v                                                   v
+-----------------------------------+   +-------------------------------+
|            DATA LAYER             |   |     SUPABASE BACKEND LAYER    |
|      AnalysisRepositoryImpl       |   |       (MVP 1C Foundation)     |
|  +---------------+--------------+  |   |  RuntimePreflight             |
|  |               |              |  |   |  SupabaseSystemStatusChecker  |
|  v               v              v  |   |  SupabaseApiService (REST)    |
| InputExtractors AnalysisReg. GeminiRepo|   +---------------+---------------+
| (Web/YT/Doc) (Engines)    (Retrofit)|                   |
+-----------------------------------+                   v
                  |                             +-----------------------+
                  v                             | Supabase PostgreSQL   |
+-----------------------------------+           | Table: system_status  |
|      ENGINE & PROMPT LAYER        |           +-----------------------+
| BaseGeminiEngine <--- PromptLoader|
| <--- assets/prompts/*.md + manifest|
+-----------------------------------+
```

---

## 2. End-to-End-Datenfluss

### A. Inhaltsanalyse-Datenfluss (9 Teilschritte)
1. **UI-Eingabe**: Anwender fügt eine URL/Text ein oder teilt einen Link über das Android-Systemmenü (Direct Share Shortcuts).
2. **ViewModel-Annahme**: `MainActivity` empfängt den Intent, `MainViewModel.processDirectShare()` setzt `_sharedUrlToFill` und setzt den UI-Zustand auf `UiState.Loading`.
3. **Anwendungsfall (UseCase)**: `AnalyzeContentUseCase.execute()` wird aufgerufen.
4. **Data Extraktion**: `ContentExtractionRepositoryImpl` ermittelt über `InputExtractorRegistry` den passenden Extraktor:
   - `WebInputExtractor`: Extrahiert HTML via JSoup und bereinigt Text.
   - `YoutubeInputExtractor`: Lädt Transkripte und Videometadaten.
   - `DocumentInputExtractor`: Liest Text-/PDF-Dateien.
5. **Engine-Routing**: `AnalysisRegistryImpl` wählt anhand des `AnalysisType` die spezialisierte `AnalysisEngine`.
6. **Prompt-Montage**: `PromptLoader` liest `_global_quality_rules.md` und das spezifische `F_*.md`-Template aus den Assets und fügt den extrahierten Text sowie Benutzervariablen ein.
7. **API-Aufruf**: `GeminiRepository` sendet den Request per Retrofit (REST) an die Google Gemini API (`gemini-2.5-flash`).
8. **Parsing & Normalisierung**: `SummaryResponseParser` entfernt Markdown-Formatierung (`json ... `), extrahiert das rohe JSON und führt eine Schema-Validierung durch.
9. **UI-Render**: Das validierte Ergebnis wird als `DomainSummary` im `UiState.Success` im Jetpack Compose UI angezeigt.

### B. Supabase Preflight Health Check (MVP 1C Datenfluss)
1. **App Start**: `RuntimePreflight` initiiert beim App-Start die Systemstatus-Prüfung.
2. **REST API Call**: `SupabaseSystemStatusChecker` ruft über `SupabaseApiService` den Endpoint `/rest/v1/system_status?select=status,backend_version` ab.
3. **Header Authentication**: Authentifizierung via `apikey: SUPABASE_PUBLISHABLE_KEY` und `Authorization: Bearer SUPABASE_PUBLISHABLE_KEY`.
4. **Parsing & Mapping**:
   - Parse JSON DTO array.
   - Robuste Konvertierung von `backend_version`: Unterstützt sowohl Integer-Darstellung (`1`) als auch SemVer-Strings (`"1.0"` -> `1`).
5. **Preflight Result**: Verifiziert, dass `status == "online"` und `backend_version >= 1`.

---

## 3. Schlüsselkomponenten

### AnalysisRepositoryImpl
- **Aufgabe**: Zentrale Koordination des Analyseprozesses.
- **Verantwortlichkeit**: Verbindet Inhaltsextraktion, Engine-Ausführung und Speicherlogik.

### EngineRunnerImpl & AnalysisRegistryImpl
- **Aufgabe**: Registrierung und Ausführung spezifischer Analyse-Engines.
- **Verantwortlichkeit**: Mappt `AnalysisType` dynamisch auf Instanzen von `AnalysisEngine`.

### PromptLoader (AndroidAssetPromptLoader)
- **Aufgabe**: Ladeinstanz für Prompt-Dateien aus den Android-Assets.
- **Verantwortlichkeit**: Verknüpft `_global_quality_rules.md` mit den jeweiligen `F_*.md`-Dateien basierend auf `prompt_manifest.json`.

### SupabaseApiService & SupabaseSystemStatusChecker
- **Aufgabe**: Direkte Kommunikation mit dem Supabase Backend.
- **Verantwortlichkeit**: Sichere REST-Anfragen gegen Supabase PostgreSQL REST-Endpunkte, Auswertung von Systemstatus und Version-Mapping.

### RuntimePreflight
- **Aufgabe**: Preflight-Härtung vor Ausführen komplexer Workflows.
- **Verantwortlichkeit**: Überprüft Laufzeit-Voraussetzungen (Netzwerk, Supabase Connectivity, lokale DB Integrity).

---

## 4. Prompt-Architektur

- **Speicherort**: `app/src/main/assets/prompts/`
- **Manifeste**:
  - `prompt_manifest.json`: Zuordnung von `function_id` zu Prompt-Dateiname (`F_*.md`).
  - `function_registry.json`: Technische Parameter (Min/Max Items, UI-Rendering-Profile, Fehlerverhalten).
- **Globale Regeln**: `_global_quality_rules.md` wird jedem Funktions-Prompt vorangestellt. Garantiert strikte Faktenausrichtung, deutsche Sprache und reines JSON-Ausgabeformat.
- **Merger & Caching**: Prompts werden zur Laufzeit über `AndroidAssetPromptLoader` aus den Assets gelesen, im Speicher zwischengespeichert und mit Kontextvariablen befüllt.

---

## 5. Backend- & Supabase-Architektur (MVP 1C)

- **Backend Provider**: Supabase Cloud / Self-hosted Supabase Stack.
- **Database Schema**:
  - Migration file: `supabase/migrations/20260807000000_mvp1_system_status.sql`.
  - Tabelle: `public.system_status` (Singleton Row `id = 1`).
  - Felder: `id` (int8), `status` (text), `backend_version` (int4 / text), `updated_at` (timestamptz).
  - RLS (Row Level Security): `SELECT` erlaubt für anonyme Nutzer (`anon`).
- **App Integration**:
  - Keinesfalls schwere SDKs zwingend; performante REST-Einbindung über Retrofit 2 + Moshi/Kotlinx.Serialization.
  - Testabdeckung: `SupabaseSystemStatusTest` verifiziert DTO-Parsing, Header-Injektion und Version-String-Normalisierung via MockWebServer.

---

## 6. Architekturprinzipien

- **Clean Architecture & Dependency Inversion**: Höhere Schichten hängen nur von Abstraktionen (Interfaces) ab.
- **Local-First & Sync Ready**: Alle Sitzungsdaten werden primär lokal in Room gespeichert und sind für spätere Backend-Synchronisationen vorbereitet.
- **Contract-Driven Design**: Schnittstellen zwischen LLM, Backend und Client sind durch explizite JSON-Verträge abgesichert.
- **Path & Workspace Governance**: Strikte Trennung von logischem Workspace `/` und Plattform-Laufzeitumgebung.

---

## 7. Technische Besonderheiten & Grenzen

- **Direct Share Integration**: `DirectShareManager` stellt Shortcuts im Android-Teilen-Menü bereit.
- **Multi-Level Extraction Pipeline**: Intelligente Erkennung von Sonderformaten (YouTube-Transkripte, Google Maps Links, PDFs).
- **Zero-Risk Deployment**: Ausführung ausschließlich auf physischen Testgeräten via APK-Download (`/app/build/outputs/apk/debug/app-debug.apk`).
- **Post-Push Health Gate**: Automatisierte Überprüfung der Git-Repository-Integrität nach Pushes via `tools/git_post_ui_push_health_gate.sh`.
