# Technical Architecture: Relevantor

## 1. Systemübersicht & Schichtentrennung

Die Anwendung **Relevantor** (Package `com.example`) ist eine native Android-Anwendung, die auf Basis von Kotlin und Jetpack Compose entwickelt wurde. Ihr primärer Zweck ist die strukturierte Analyse, Zusammenfassung und Qualitätsbewertung verschiedener Inhaltsquellen (Webseiten, PDF-Dokumente, YouTube-Videos, Bilder/Screenshots, Standortkontexte und Freitexte) mithilfe von Large Language Models (Gemini API) und integriertem Google Search Grounding.

### Architekturprinzip & Schichtenmodell

Das System folgt strikt den Prinzipien der **Clean Architecture** in Kombination mit dem **MVVM-Muster (Model-View-ViewModel)**. Es unterteilt die Codebasis klar in drei Hauptschichten:

1. **UI-Schicht (`com.example.ui` & `MainActivity.kt`)**:
   - Ausführung der Benutzeroberfläche vollständig in Jetpack Compose.
   - Entkopplung der UI-Logik über den `MainViewModel`.
   - Darstellung von Analyseergebnissen, Kategorien, Verläufen und detaillierten Qualitätsberichten basierend auf Dekoratoren (`OutputPresentationPolicy`) und Konfigurationskatalogen (`FeatureCatalog`).

2. **Domain-Schicht (`com.example.domain`)**:
   - Enthält die reine Geschäftslogik und Anwendungsfälle (Use Cases): `AnalyzeContentUseCase`, `ExtractContentUseCase`, `SaveAnalysisUseCase`, `LoadHistoryUseCase`, `SyncUserDataUseCase`.
   - Azyklische Interfaces für Repositories (`AnalysisRepository`, `ContentExtractionRepository`, `GeminiGateway`, `UserRepository`, `SyncRepository`).
   - Abstraktion des Engine-Systems (`AnalysisEngine`, `AnalysisRegistry`, `EngineContract`, `ContractValidator`).
   - Reine Domänenmodelle (`ExtractedContent`, `DomainSummary`, `CanonicalAnalysisInput`, `AnalysisTrace`).

3. **Data-Schicht (`com.example.data`)**:
   - **Extraction Subsystem (`com.example.data.extraction`)**: Registrierung und Ausführung von Inhalts-Extraktoren (`WebInputExtractor`, `YoutubeInputExtractor`, `DocumentInputExtractor`) über den `InputExtractorRegistry`.
   - **Engine Subsystem (`com.example.data.engine`)**: Konkrete Ausführung der KI-Analysen (`BaseGeminiEngine`, `WebpageAnalysisEngine`, `DocumentAnalysisEngine`, `Top3KeyPointsEngine`) gesteuert durch die `AnalysisRegistryImpl`.
   - **Prompt Subsystem (`com.example.data.engine`)**: Asset-basiertes Laden von Prompt-Dateien (`AndroidAssetPromptLoader`) und globale Regelverwaltung (`_global_quality_rules.md`, `function_registry.json`).
   - **Remote & API (`com.example.data.remote` & `com.example.data`)**: Retrofit2-Client (`RetrofitClient`), REST-Schnittstelle (`GeminiApiService`), API-Gateway-Implementierung (`GeminiRepository`).
   - **Parsing & Validierung (`com.example.data`)**: Robuster JSON-Parser (`SummaryResponseParser`), Normalisierer (`ResponseNormalizer`) und Vertragskontrolle (`A1ContractValidator`, `A2ContractValidator`).
   - **Local Storage (`com.example.data.local`)**: SQLite/Room-Datenbank (`RelevantorDatabase`, `AnalysisDao`) zur persistenten Speicherung der Historiendaten und Sitzungen (`SessionStorage`).
   - **Diagnostics & Monitoring (`com.example.data.diagnostics` & `PipelineReportStore`)**: Durchgängiges In-Memory-Tracking aller Pipelineschritte für Laufzeitdiagnostik und Self-Tests.

### ASCII-Architekturdiagramm

```
+-----------------------------------------------------------------------------------+
|                                 UI LAYER (Compose)                                |
|   MainActivity.kt / AppTab (Start, Verlauf, Favoriten) / TakeawayCard.kt           |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                  VIEWMODEL LAYER                                  |
|                                   MainViewModel                                   |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                 DOMAIN LAYER                                      |
| UseCases: AnalyzeContentUseCase | ExtractContentUseCase | SaveAnalysisUseCase     |
| Registries & Interfaces: AnalysisRegistry | AnalysisEngine | ContractValidator    |
+-----------------------------------------------------------------------------------+
                                          |
        +---------------------------------+---------------------------------+
        |                                 |                                 |
        v                                 v                                 v
+-----------------------+     +-----------------------+     +-----------------------+
|  EXTRACTION SUBSYSTEM |     |    ENGINE SUBSYSTEM   |     |    STORAGE SUBSYSTEM  |
| InputExtractorRegistry|     | AnalysisRegistryImpl  |     | AnalysisRepositoryImpl|
| - WebInputExtractor   |     | - WebpageAnalysisEng. |     | - RelevantorDatabase  |
| - YoutubeInputExtr.   |     | - DocumentAnalysisEng.|     |   (Room / AnalysisDao)|
| - DocumentInputExtr.  |     | - Top3KeyPointsEngine |     | - SessionStorage      |
+-----------------------+     +-----------------------+     +-----------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                               PROMPT & API SUBSYSTEM                              |
| - AndroidAssetPromptLoader (prompts/function_registry.json + *.md)                |
| - BaseGeminiEngine (Adaptive Empty-Candidate Fallback: 3-Segment Excerpt)          |
| - GeminiRepository -> RetrofitClient -> GeminiApiService (Google Gemini REST API)  |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                             PARSING & VALIDATION LAYER                            |
| - SummaryResponseParser (Raw Text -> Clean JSON)                                  |
| - ContractValidator (A1ContractValidator / A2ContractValidator)                  |
| - ResponseNormalizer -> DomainSummary                                             |
+-----------------------------------------------------------------------------------+
```

---

## 2. End-to-End-Datenfluss

Der vollständige Lebenszyklus einer Inhaltsanalyse verläuft von der Benutzereingabe in der UI bis zur Darstellung des validierten Analyseergebnisses über folgende strukturierte Phasen:

```
[1. UI-Eingabe]
       │
       ▼
[2. MainViewModel]
       │
       ▼
[3. AnalyzeContentUseCase]
       │
       ├──────────────────────────────► [4. ExtractContentUseCase]
       │                                       │
       │                                       ▼
       │                                [InputExtractorRegistry]
       │                                       │
       │                                       ▼
       │                                [Web / Youtube / Document Extractor]
       │                                       │
       │                                       ▼
       │                                (ExtractedContent)
       │
       ▼
[5. AnalysisRegistryImpl] ──► Ermittelt EngineContract & AnalysisEngine
       │
       ▼
[6. AnalysisEngine.analyze()] (z.B. BaseGeminiEngine / WebpageAnalysisEngine)
       │
       ├──────────────────────────────► [AndroidAssetPromptLoader]
       │                                       │
       │                                       ▼
       │                                Lade _global_quality_rules.md + F_*.md
       │
       ├──────────────────────────────► [GeminiGateway / GeminiRepository]
       │                                       │
       │                                       ▼
       │                                Build GenerateContentRequest
       │                                (Prompt + Text/PDF + Google Search Tool)
       │                                       │
       │                                       ▼
       │                                HTTP POST -> GeminiApiService
       │
       ├──────────────────────────────► [Adaptive Fallback Logic]
       │                                (Falls Attempt 1 Empty Candidate & Len > 12k
       │                                 -> Erzeuge 3-Segment Excerpt -> Attempt 2)
       │
       ▼
[7. Raw Response Text]
       │
       ▼
[8. SummaryResponseParser] ──► Extrahiert JSON, bereinigt Markdown & Syntax
       │
       ▼
[9. ContractValidator] ──────► Prüft A1/A2 Schema-Vorgaben
       │
       ▼
[10. ResponseNormalizer] ────► Mappt auf typed DomainSummary
       │
       ▼
[11. SaveAnalysisUseCase] ───► Speichert Entity in Room DB (RelevantorDatabase)
       │
       ▼
[12. UI Output Render] ──────► MainViewModel StateFlow -> TakeawayCard & Dynamic UI
```

### Detaillierter Ablauf je Phase

1. **UI-Eingabe (`MainActivity.kt`)**: Der Anwender gibt eine URL, einen Freitext oder ein Dokument/Bild ein und wählt eine Analysefunktion aus dem `FeatureCatalog` (z. B. `TOP_3_KERNAUSSAGEN`, `AKTUALITAETS_CHECK`, `FEHLINFORMATIONS_RADAR`).
2. **ViewModel-Aktivierung (`MainViewModel.kt`)**: Der ViewModel empfängt die Aktion und startet den asynchronen Koroutinen-Flow über `AnalyzeContentUseCase.execute()`.
3. **Inhaltsextraktion (`ExtractContentUseCase.kt`)**: Der `InputExtractorRegistry` prüft den MIME-Typ bzw. die URL-Struktur des Inputs und delegiert an den passenden Extraktor:
   - `WebInputExtractor`: Extrahiert HTML via `WebpageExtractor` (JSoup) und säubert den Fließtext.
   - `YoutubeInputExtractor`: Dekodiert YouTube-URLs und lädt Transkripte über den `YoutubeTranscriptHelper` / `PublicVideoSourceResolver`.
   - `DocumentInputExtractor`: Verarbeitet PDF-/Text-Dokumente und extrahiert Rohtext sowie Binärdaten.
   - Ergebnis ist ein unifiziertes `ExtractedContent`-Objekt.
4. **Engine-Routing (`AnalysisRegistryImpl.kt`)**: Auf Basis des geforderten `AnalysisType` schlägt die Registry den entsprechenden `EngineContract` nach. Dieser enthält die zugewiesene `AnalysisEngine`, den relativen Asset-Prompt-Pfad, die Standard-Grounding-Einstellung und den zugewiesenen `ContractValidator`.
5. **Prompt-Laden (`AndroidAssetPromptLoader.kt`)**: Der Prompt-Loader liest die universellen Qualitätsregeln (`_global_quality_rules.md`) sowie das spezifische Prompt-Template (z. B. `F_AKTUALITAETS_CHECK.md`) aus den Android-Assets und fügt diese mit der Benutzereingabe zusammen.
6. **API-Aufruf & Adaptiver Fallback (`BaseGeminiEngine.kt`)**:
   - Die Engine baut das `GenerateContentRequest`-DTO auf und sendet es über das `GeminiGateway` an die Gemini REST-API.
   - **Adaptiver Fallback**: Falls der erste Aufruf mit aktivem Search Grounding technisch mit `STOP`/`NONE` antwortet, aber keine Text-Parts zurückliefert (`EMPTY_CANDIDATE_CONTENT`) und der Quelltext länger als 12.000 Zeichen ist, greift die deterministische 3-Segment-Kürzung (`buildBalancedExcerpt`: 5.000 Zeichen Anfang, 3.000 Zeichen Mitte, 4.000 Zeichen Ende). Genau ein zweiter Versuch wird mit dem reduzierten Kontext ausgeführt.
7. **Parsing & Bereinigung (`SummaryResponseParser.kt`)**: Die rohe Textantwort des Modells wird von Markdown-Code-Block-Injektionen (` ```json ... ``` `) befreit, auf valides JSON getrimmt und syntaktisch bereinigt.
8. **Vertragskontrolle (`A1ContractValidator` / `A2ContractValidator`)**: Der Parser-Output wird gegen die Schnittstellen-Spezifikation (A1 = Standard 3-5 Kernaussagen; A2 = Erweiterte Sektions-Analyse) auf Vollständigkeit und Struktur geprüft.
9. **Speicherung & Präsentation (`SaveAnalysisUseCase.kt` & `MainActivity.kt`)**: Das Ergebnis wird als `DomainSummary` zurückgegeben, über `SaveAnalysisUseCase` in der lokalen Room-Datenbank persistiert und im `MainViewModel` als StateFlow bereitgestellt.

---

## 3. Schlüsselkomponenten

| Komponente | Dateipfad | Hauptverantwortung | Interaktionen |
| :--- | :--- | :--- | :--- |
| **`MainActivity`** | `app/src/main/java/com/example/MainActivity.kt` | Haupteinstiegspunkt der UI. Verfassen der Compose-Layouts, Bottom Navigation, Modals, Karten und Themes. | Nutzt `MainViewModel`. |
| **`MainViewModel`** | `app/src/main/java/com/example/ui/MainViewModel.kt` | Verfassen des UI-States (`StateFlow`), Ausführung von UseCases, Fehlerbehandlung und UI-Events. | Steuert `AnalyzeContentUseCase`, `LoadHistoryUseCase`, `SaveAnalysisUseCase`. |
| **`AnalyzeContentUseCase`** | `app/src/main/java/com/example/domain/usecase/AnalyzeContentUseCase.kt` | Zentrale Orchestrierung der Inhaltsanalyse von Extraction über Engine-Ausführung bis Ergebnis-Mapping. | Koordiniert `ExtractContentUseCase`, `AnalysisRegistry`, `EngineRunner`. |
| **`InputExtractorRegistry`** | `app/src/main/java/com/example/data/extraction/InputExtractorRegistry.kt` | Verwaltung aller verfügbaren Inhalts-Extraktoren. | Hält Instanzen von `WebInputExtractor`, `YoutubeInputExtractor`, `DocumentInputExtractor`. |
| **`AnalysisRegistryImpl`** | `app/src/main/java/com/example/data/engine/AnalysisRegistryImpl.kt` | Mapping von `AnalysisType` auf konkrete `EngineContract`-Objekte. | Verknüpft Engines, Prompts, Validators und Grounding-Defaults. |
| **`BaseGeminiEngine`** | `app/src/main/java/com/example/data/engine/BaseGeminiEngine.kt` | Abstrakte Basisklasse aller KI-Engines. Enthält die Ausführungsschleife, Retry-Logik, Grounding-Steuerung, JSON-Extraktion und den Adaptiven Fallback. | Nutzt `GeminiGateway`, `PromptLoader`, `SummaryResponseParser`, `PipelineReportStore`. |
| **`AndroidAssetPromptLoader`** | `app/src/main/java/com/example/data/engine/AndroidAssetPromptLoader.kt` | Liest Markdown-Prompt-Dateien und Konfigurationen synchron aus dem Android `assets/prompts/`-Verzeichnis. | Nutzt Android `AssetManager`, verwendet `PromptFallbackProvider` bei Fehlern. |
| **`SummaryResponseParser`** | `app/src/main/java/com/example/data/SummaryResponseParser.kt` | Robustes Extrahieren und Bereinigen von JSON-Datenstrukturen aus unstrukturiertem Modell-Antworttext. | Verwendet Moshi JSON-Adapters. |
| **`A1ContractValidator`** | `app/src/main/java/com/example/domain/engine/validators/A1ContractValidator.kt` | Prüft, ob das gerenderte JSON die Mindestanforderungen des Vertrags A1 erfüllt (Titel, Beschreibung, 3-5 Key Takeaways). | Wird von `BaseGeminiEngine` nach dem Parsing aufgerufen. |
| **`GeminiRepository`** | `app/src/main/java/com/example/data/GeminiRepository.kt` | Implementierung des `GeminiGateway`-Interfaces. Übermittelt Requests an die Retrofit API-Schnittstelle. | Steuert `GeminiApiService`. |
| **`RelevantorDatabase`** | `app/src/main/java/com/example/data/local/RelevantorDatabase.kt` | Room Database Manager für lokale Entitäten (`AnalysisEntity`). | Bietet DAO-Zugriff für `AnalysisRepositoryImpl`. |
| **`PipelineReportStore`** | `app/src/main/java/com/example/data/PipelineReportStore.kt` | Thread-sicheres In-Memory-Tracking aller Analyse-Schritte, HTTP-Metriken, Fallback-Ereignisse und Validierungsergebnisse. | Wird von allen Systemschichten zur Diagnose genutzt. |

---

## 4. Prompt-Architektur

Das Prompt-System von Relevantor basiert auf einer strikten Trennung zwischen Anwendungscode und Instruktionslogik. Alle Prompts liegen als eigenständige Markdown-Dateien im Asset-Ordner.

### Speicherort & Ordnerstruktur

Die Dateien befinden sich unter `app/src/main/assets/prompts/`:

```
app/src/main/assets/prompts/
├── _global_quality_rules.md            # Globale Qualitätsvorgaben für alle Analysen
├── function_registry.json              # Typisiertes Manifest aller Analysefunktionen
├── prompt_manifest.json                # Versionierung und Mapping der Prompts
├── F_AKTUALITAETS_CHECK.md             # Prompt: Aktualitäts-Check
├── F_BUSINESS_INKUBATOR.md             # Prompt: Business-Inkubator
├── F_DOKUMENTE.md                      # Prompt: Dokumenten-Analyse
├── F_FACTS_VS_OPINIONS_ANALYZER.md     # Prompt: Fakten vs. Meinungen
├── F_FEHLINFORMATIONS_RADAR.md         # Prompt: Fehlinformations-Radar
├── F_FREIE_QUELLENANFRAGE.md           # Prompt: Freie Quellenanfrage
├── F_GOOGLE_MAPS_ANALYZER.md           # Prompt: Google Maps Orte-Analyse
├── F_GOOGLE_MAPS_LOCATION_CONTEXT.md   # Prompt: Google Maps Standortkontext
├── F_MULTIMEDIA.md                     # Prompt: Audio/Video/YouTube-Analyse
├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md # Prompt: Perspektiven & Gegenpositionen
├── F_PHOTO_SCREENSHOT_ANALYSIS.md      # Prompt: Fotos & Screenshots
├── F_RISIKO_ANALYSE.md                 # Prompt: Risiko-Analyse
├── F_STANDARD_WEBSEITE.md              # Prompt: Standard Webseiten-Analyse
├── F_TOP_3_KERNAUSSAGEN.md             # Prompt: Top-3 Kernaussagen
└── F_WEITERE_RELEVANTE_ASPEKTE.md      # Prompt: Weitere relevante Aspekte
```

### Manifest- & Registrierungsdateien

- **`function_registry.json`**: Definiert typisiert jede Funktion inkl. `function_id`, `prompt_file`, `contract_type` (A1/A2), `engine_id`, `default_grounding` und Beschreibungsfeldern.
- **`prompt_manifest.json`**: Enthält Metadaten zu Versionen und Prüfsummen der geladenen Prompts.

### Ladeprozess & Merging

1. Der `AndroidAssetPromptLoader` lädt bei Ausführung einer Engine den Inhalt der `_global_quality_rules.md`.
2. Anschließend wird die im `EngineContract` definierte Prompt-Datei (z. B. `F_TOP_3_KERNAUSSAGEN.md`) geladen.
3. Die globalen Regeln werden als System-Instruktion (`systemInstruction`) im `GenerateContentRequest` gesetzt, während der funktionsspezifische Prompt zusammen mit den Quelltext-Segmenten als Benutzer-Payload übergeben wird.
4. **Fallback-Mechanismus**: Sollte eine Prompt-Datei im Asset-Ordner fehlen oder nicht lesbar sein, greift der `PromptFallbackProvider` ein und liefert fest im Code hinterlegte Notfall-Prompts.

---

## 5. API- & KI-Integration

Die KI-Integration nutzt die offizielle REST-Schnittstelle von Google Gemini (v1beta).

### Verwendete Modelle & Konfiguration

- **Standardmodell**: `gemini-2.5-flash` (konfiguriert in `AnalysisRuntimeConfigs.kt`).
- **Alternative Modellzuordnungen**: `gemini-1.5-flash`, `gemini-2.0-flash` (über System-Eigenschaften steuerbar).
- **GenerationConfig**:
  - `temperature`: `0.2` (für deterministische, faktenbasierte Ausgaben).
  - `topP`: `0.95`
  - `maxOutputTokens`: `8192`
  - `responseMimeType`: `"application/json"` (wo vom Vertragsmodell unterstützt).

### Google Search Grounding

Bestimmte Qualitätsfunktionen (z. B. `F_AKTUALITAETS_CHECK`, `F_FEHLINFORMATIONS_RADAR`) erfordern Echtzeit-Daten aus dem Web.
In diesen Fällen wird im `GenerateContentRequest` das `Tool`-Objekt mit aktiviertem `googleSearch` übermittelt:

```json
"tools": [
  {
    "googleSearch": {}
  }
]
```

### Adaptiver Fallback bei leeren Candidate-Antworten (EMPTY_CANDIDATE_CONTENT)

Wenn das Modell bei aktiviertem Grounding aufgrund übergroßer Eingabetexte einen HTTP 200 Response mit `finishReason = "STOP"` oder `"NONE"`, aber ohne Text-Parts liefert (`EMPTY_CANDIDATE_CONTENT`), greift der adaptive Fallback in `BaseGeminiEngine.kt`:

1. **Bedingungsprüfung**:
   - `activeGrounding == true`
   - Versuch 1 war technisch erfolgreich (HTTP 200).
   - `finishReason` ist `STOP`, `NONE` oder leer.
   - Kein Safety-Block, kein Recitation-Block, kein Prompt-Block Reason.
   - Kein verwertbarer Text-Part vorhanden.
   - Quelltextlänge > 12.000 Zeichen.
2. **Kürzung via `buildBalancedExcerpt`**:
   - Es wird ein deterministisches Auszug-Segment von maximal 12.000 Zeichen erzeugt.
   - **Segmentverteilung**: Erste 5.000 Zeichen (Anfang) + Mittlere 3.000 Zeichen (Mitte) + Letzte 4.000 Zeichen (Ende).
   - Einfügen interner Trenner: `[AUSZUG ANFANG]`, `[AUSZUG MITTE]`, `[AUSZUG ENDE]`.
   - Wort- und Absatzgrenzen werden durch Rückwärtssuche (`findBestBoundary`) eingehalten.
3. **Zweiter Versuch**: Genau ein Retry wird mit dem ausbalancierten Quelltext ausgeführt.

---

## 6. Parsing- und Validierungsarchitektur

Die Antwortverarbeitung transformiert unstrukturierte Modelltext-Antworten in strikt typisierte JSON-Domänenobjekte.

### Verarbeitungsreihenfolge

```
[Gemini Text Response]
          │
          ▼
[SummaryResponseParser.parseSummaryResponse()]
          │
          ├─► 1. Regex-Extraktion von Code-Blöcken (```json ... ```)
          ├─► 2. Beseitigung führender/nachfolgender Steuerzeichen
          ├─► 3. Korrektur unvollständiger Anführungszeichen & Trailing Commas
          ├─► 4. Moshi JSON-Deserialisierung -> Raw Object
          │
          ▼
[ContractValidator.validate()]
          │
          ├─► A1ContractValidator: Prüft Pflichtfelder title, short_description, key_takeaways (3-5 Items)
          └─► A2ContractValidator: Prüft erweiterte Abschnitte & Sektions-Typen
          │
          ▼
[ResponseNormalizer.normalize()]
          │
          ▼
[Typed DomainSummary]
```

### Robuste JSON-Bereinigung (`SummaryResponseParser.kt`)

Der `SummaryResponseParser` fängt häufige LLM-Ausgabeformate ab:
- Entfernt umgebende Markdown-Tags.
- Isoliert das erste gültige `{ ... }` JSON-Fragment mittels Klammer-Matching.
- Ersetzt typografische Anführungszeichen (`„`, `“`) durch Standard-Double-Quotes.
- Repariert abgeschnittene Arrays oder fehlende Schlussklammern im Notfall.

---

## 7. Routing-Architektur

Das Routing verknüpft UI-Funktionsauswahlen dynamisch mit den zugrundeliegenden Systemkomponenten.

```
[UI / FeatureCatalog] ──(AnalysisType)──► [AnalysisRegistryImpl]
                                                 │
      ┌──────────────────────────────────────────┴──────────────────────────────────────────┐
      ▼                                          ▼                                          ▼
[EngineContract A1]                        [EngineContract A2]                        [Custom Engine]
- Engine: WebpageAnalysisEngine            - Engine: DocumentAnalysisEngine           - Engine: Top3KeyPointsEngine
- Prompt: prompts/F_AKTUALITAETS_CHECK.md  - Prompt: prompts/F_DOKUMENTE.md           - Prompt: prompts/F_TOP_3_KERNAUSSAGEN.md
- Validator: A1ContractValidator           - Validator: A2ContractValidator           - Validator: A1ContractValidator
- Grounding: true                          - Grounding: false                         - Grounding: false
```

### Routing-Tabelle (Auszug der Zuordnungen)

| AnalysisType | Asset-Prompt-Datei | Engine-Klasse | Contract Validator | Default Grounding |
| :--- | :--- | :--- | :--- | :--- |
| `TOP_3_KERNAUSSAGEN` | `prompts/F_TOP_3_KERNAUSSAGEN.md` | `Top3KeyPointsEngine` | `A1ContractValidator` | `false` |
| `STANDARD_WEBSEITE` | `prompts/F_STANDARD_WEBSEITE.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `AKTUALITAETS_CHECK` | `prompts/F_AKTUALITAETS_CHECK.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |
| `FEHLINFORMATIONS_RADAR` | `prompts/F_FEHLINFORMATIONS_RADAR.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |
| `FACTS_VS_OPINIONS` | `prompts/F_FACTS_VS_OPINIONS_ANALYZER.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `RISIKO_ANALYSE` | `prompts/F_RISIKO_ANALYSE.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `PERSPECTIVES_COUNTERPOSITIONS` | `prompts/F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |
| `BUSINESS_INKUBATOR` | `prompts/F_BUSINESS_INKUBATOR.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `WEITERE_RELEVANTE_ASPEKTE` | `prompts/F_WEITERE_RELEVANTE_ASPEKTE.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `FREIE_QUELLENANFRAGE` | `prompts/F_FREIE_QUELLENANFRAGE.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |
| `DOKUMENTE` | `prompts/F_DOKUMENTE.md` | `DocumentAnalysisEngine` | `A2ContractValidator` | `false` |
| `MULTIMEDIA` | `prompts/F_MULTIMEDIA.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `false` |
| `PHOTO_SCREENSHOT` | `prompts/F_PHOTO_SCREENSHOT_ANALYSIS.md` | `DocumentAnalysisEngine` | `A1ContractValidator` | `false` |
| `GOOGLE_MAPS` | `prompts/F_GOOGLE_MAPS_ANALYZER.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |
| `GOOGLE_MAPS_LOCATION_CONTEXT` | `prompts/F_GOOGLE_MAPS_LOCATION_CONTEXT.md` | `WebpageAnalysisEngine` | `A1ContractValidator` | `true` |

---

## 8. Schichtenmodell (Separation of Concerns)

Das Schichtenmodell stellt sicher, dass Abhängigkeiten strikt von außen nach innen verlaufen:

1. **UI Layer (`com.example.ui`)**:
   - **Verantwortung**: Rendering von Layouts, Reaktion auf Gesten, Theme-Konfiguration, Accessibility.
   - **Abgrenzung**: Darf keine Business-Logik oder direkte API-Aufrufe enthalten. Kommuniziert ausschließlich über den `MainViewModel`.
2. **ViewModel Layer (`MainViewModel`)**:
   - **Verantwortung**: Zustandshaltung via Kotlin `StateFlow`, Entgegennahme von Benutzereingaben, Transformation von Domain-Ergebnissen in UI-States.
   - **Abgrenzung**: Keine direkten Datenbank- oder Netzzugriffe.
3. **Domain Layer (`com.example.domain`)**:
   - **Verantwortung**: Definition aller Anwendungsfälle (`UseCases`), Domänenmodelle, Engine-Interfaces und Validierungsregeln.
   - **Abgrenzung**: Völlig unabhängig von Android-Framework-Klassen (außer Standard-Utilities) und konkreten Frameworks.
4. **Data Layer (`com.example.data`)**:
   - **Verantwortung**: Implementierung der Repositories, Datenbankschemata (Room), Netzzugriffe (Retrofit), Inhalts-Extraktion, Engine-Ausführungslogik und Prompt-Ladevorgänge.
   - **Abgrenzung**: Verbirgt die Datenquellen vollständig vor der Domain-Schicht.

---

## 9. Architekturprinzipien

In der Codebasis von Relevantor sind folgende Prinzipien durchgängig nachweisbar implementiert:

1. **Clean Architecture**: Strikte Unabhängigkeit des Domänenkerns von Datenbanken, UI-Frameworks und externen APIs.
2. **Separation of Concerns**: Jede Schicht besitzt eindeutig zugewiesene Verantwortlichkeiten.
3. **Single Responsibility Principle (SRP)**:
   - Extraktoren verarbeiten ausschließlich Rohmedien.
   - Parser transformieren ausschließlich JSON.
   - Validatoren prüfen ausschließlich Verträge.
4. **Registry-Muster**:
   - `InputExtractorRegistry` für Extraktoren.
   - `AnalysisRegistryImpl` für Analyse-Engines.
5. **Prompt-Driven & Contract-Driven Design**:
   - Logik und Anweisungen sind in externen Asset-Markdown-Dateien definiert.
   - Ausgaben müssen maschinell verifizierbare Schnittstellen-Verträge (`A1ContractValidator`, `A2ContractValidator`) erfüllen.
6. **Configuration over Code**: Funktionsverhalten, Grounding-Schalter, Prompt-Pfade und Icons werden zentral in `function_registry.json` und `FeatureCatalog.kt` gepflegt.

---

## 10. Technische Besonderheiten

### Adaptiver Empty-Candidate Fallback (`buildBalancedExcerpt`)
In `BaseGeminiEngine.kt` befindet sich eine deterministische Textkürzungslogik. Bei übergroßen Dokumenten (> 12.000 Zeichen), die unter Search Grounding leere Antworten produzieren, werden drei gleichmäßige Textbereiche (Kopf, Mitte, Fuß) mit optisch neutralen Trennern (`[AUSZUG ANFANG]`, `[AUSZUG MITTE]`, `[AUSZUG ENDE]`) unter Wahrung von Satz- und Absatzgrenzen extrahiert.

### YouTube Transcript & Public Video Source Resolver
Der `YoutubeInputExtractor` verknüpft `YoutubeTranscriptHelper` und `PublicVideoSourceResolver`, um automatisch Transkripte von YouTube-Videos zu laden. Falls kein direktes Transkript verfügbar ist, erfolgt ein sanfter Fallback auf die Metadaten der Video-Quellseite.

### In-Memory Pipeline Reporting (`PipelineReportStore`)
Jeder Schritt einer Analyse – von der Inhalts-Extraktion über HTTP-Statuscodes, Prompt-Token-Counts, Fallback-Ereignisse bis zur JSON-Validierung – wird in einem In-Memory-Speicher protokolliert. Dieses System dient als Datenbasis für integrierte Self-Tests (`RelevantorSelfTestHarnessTest`) und Echtzeit-Diagnosen.

---

## 11. Implementierungs- & Laufzeitmatrix

| Funktion | Engine | Datenquelle | Grounding | Systemstatus |
| :--- | :--- | :--- | :--- | :--- |
| `TOP_3_KERNAUSSAGEN` | `Top3KeyPointsEngine` | Web / Text / Doc | Deaktiviert | Produktiv |
| `STANDARD_WEBSEITE` | `WebpageAnalysisEngine` | Web HTML / Text | Deaktiviert | Produktiv |
| `AKTUALITAETS_CHECK` | `WebpageAnalysisEngine` | Web HTML / Text | Aktiviert (`GoogleSearch`) | Produktiv |
| `FEHLINFORMATIONS_RADAR` | `WebpageAnalysisEngine` | Web HTML / Text | Aktiviert (`GoogleSearch`) | Produktiv |
| `FACTS_VS_OPINIONS` | `WebpageAnalysisEngine` | Web HTML / Text | Deaktiviert | Produktiv |
| `RISIKO_ANALYSE` | `WebpageAnalysisEngine` | Web HTML / Text | Deaktiviert | Produktiv |
| `PERSPECTIVES_COUNTERPOSITIONS` | `WebpageAnalysisEngine` | Web HTML / Text | Aktiviert (`GoogleSearch`) | Produktiv |
| `BUSINESS_INKUBATOR` | `WebpageAnalysisEngine` | Web HTML / Text | Deaktiviert | Produktiv |
| `WEITERE_RELEVANTE_ASPEKTE` | `WebpageAnalysisEngine` | Web HTML / Text | Deaktiviert | Produktiv |
| `FREIE_QUELLENANFRAGE` | `WebpageAnalysisEngine` | Web HTML / Text | Aktiviert (`GoogleSearch`) | Produktiv |
| `DOKUMENTE` | `DocumentAnalysisEngine` | PDF / Raw Bytes | Deaktiviert | Produktiv |
| `MULTIMEDIA` | `WebpageAnalysisEngine` | YouTube / Video | Deaktiviert | Produktiv |
| `PHOTO_SCREENSHOT` | `DocumentAnalysisEngine` | Image / Screenshot | Deaktiviert | Produktiv |
| `GOOGLE_MAPS` | `WebpageAnalysisEngine` | Maps URL / Location | Aktiviert (`GoogleSearch`) | Produktiv |
| `GOOGLE_MAPS_LOCATION_CONTEXT` | `WebpageAnalysisEngine` | Maps / Location | Aktiviert (`GoogleSearch`) | Produktiv |

---

## 12. Build- und Testsystem

### Buildprozess

- **Build-Tool**: Gradle mit Kotlin DSL (`build.gradle.kts`).
- **Android Gradle Plugin (AGP)**: Version 8.8.0.
- **Kotlin-Version**: 2.0.21 mit KSP (Kotlin Symbol Processing) für Room-Code-Generierung.
- **Kompilierung**: Aufruf über `compile_applet` oder `gradle :app:assembleDebug`.

### Teststruktur

Die Anwendung verfügt über ein umfangreiches automatisiertes Test-Suite-System unter `app/src/test/java/`:

1. **Unit-Tests (`gradle :app:testDebugUnitTest`)**:
   - `A1ContractValidatorTest` / `A2ContractValidatorTest`: Überprüfung der JSON-Schema-Validierung.
   - `GeminiMissingPartsTest`: Verifiziert den Adaptiven Empty-Candidate Fallback und das Verhalten bei fehlenden Text-Parts.
   - `GoogleMapsUrlParserTest` & `GoogleMapsDisambiguatorTest`: Tests der Standort-URL-Dekodierung.
2. **Integrationstests & Test-Harness**:
   - `RelevantorSelfTestHarnessTest`: Umfassender Regressionstest-Satz, der den vollständigen Pipeline-Ablauf mit Mock-Gateways und realen Test-Fixtures verifiziert.
   - `BaseArchitectureRegressionTest`: Prüft die Einhaltung der Architekturgrenzen und Paketstrukturen.
3. **Robolectric & Roborazzi Screenshot-Tests**:
   - `ExampleRobolectricTest` & `GreetingScreenshotTest`: Lokale JVM-Tests für Jetpack Compose UI-Komponenten ohne physisches Emulator-Requirement.

---

## 13. Bekannte Systemeinschränkungen

1. **Kein integrierter Android-Emulator im Container**: Ausführung von Instrumentierungstests (`androidTest`) oder ADB-Befehlen ist in der Agenten-Laufzeitumgebung nicht möglich. Verifikationen erfolgen über JVM-Robolectric-Tests und `compile_applet`.
2. **Search Grounding Token Overhead**: Bei sehr langen HTML-Inhalten in Verbindung mit `googleSearch` Grounding neigt das Gemini API Modell in Einzelfällen zu leeren `EMPTY_CANDIDATE_CONTENT` Antworten, was durch den Adaptiven 3-Segment-Fallback abgefangen wird.
3. **PDF-Inline-Größenbeschränkungen**: Das Parsen großer PDF-Dateien als Base64-Inline-Bytes unterliegt den RAM-Grenzen der JVM-Laufzeit.

---

## 14. Regeln für Systeminteraktion (Governance)

1. **Architecture Freeze (`ARCHITECTURE_FREEZE.md`)**:
   - Die grundlegende Verzeichnis- und Modulstruktur ist geschützt.
   - Das Erstellen neuer Schichten oder das Aufbrechen der `Clean Architecture` ist untersagt.
2. **Prompt-Governance**:
   - Prompts müssen in `app/src/main/assets/prompts/` gepflegt und in `function_registry.json` registriert werden.
   - Hardcoded Prompts im Kotlin-Quellcode sind außer als Fallback-Sicherung untersagt.
3. **Contract-Schutz**:
   - Die Ausgaben aller Qualitätsfunktionen müssen die definierten Schnittstellen-Verträge (A1 / A2) einhalten.
   - Änderungen an Vertragsstrukturen erfordern eine entsprechende Anpassung der Validatoren (`A1ContractValidator`, `A2ContractValidator`).
