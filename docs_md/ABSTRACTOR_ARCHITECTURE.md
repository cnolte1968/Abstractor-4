# Technical Architecture: Abstractor

## 1. Systemübersicht & Schichtentrennung

Die Architektur der Abstractor-App folgt einer strikten Trennung der Verantwortlichkeiten, um maximale Wartbarkeit, robuste Verarbeitungsqualität und eine flexible Anpassung der KI-Auswertungskriterien zu ermöglichen:

```
┌────────────────────────────────────────────────────────┐
│                      COMPOSE UI                        │  <- Darstellung & Interaktion
│  (Cockpit, Ergebnisse, Dateipicker, Share, M3 Design)  │
└──────────────────────────┬─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                     VIEWMODEL LAYER                    │  <- UI-Zustand & Validierung
│  (MainViewModel, UiState, URL-DNS-Auflösung)           │
└──────────────────────────┬─────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────┐
│                   KOTLIN RUNTIME ENGINE                │  <- Payload-Bau, API, Robust-Parsing
│  (GeminiRepository, GeminiNetwork, Fallback-Modelle)   │
└─────────────────────┬────────────┬─────────────────────┘
                      │            │
                      ▼            ▼
┌───────────────────────────┐┌───────────────────────────┐
│       PROMPT ROUTING      ││      PROMPT CLIENT        │  <- Assets & Manifeste
│   (prompt_manifest.json)  ││   (PromptLoader.kt)       │
└─────────────────────┬─────┘└─────┬─────────────────────┘
                      │            │
                      ▼            ▼
┌────────────────────────────────────────────────────────┐
│                     LLM INTELLECT                      │  <- Qualitätsmaßbe & Prompts
│  (F_*.md, _global_quality_rules.md, Gemini API)        │
└────────────────────────────────────────────────────────┘
```

---

## 2. Der Datenfluss (End-to-End)

1. **Eingabe im UI-Cockpit (MainActivity / Compose):**
   * Der Anwender wählt eine Analyse-Kachel (`AnalysisType`) und gibt eine URL ein oder wählt eine lokale Datei aus.
   * Über das `MainViewModel` wird eine dns- und redirect-bereinigte Quell-URL validiert.
2. **Umlenkung im `GeminiRepository`:**
   * Vor dem Payload-Bau kontaktiert das Repo das Singleton `PromptLoader` für den passenden Analysemodus.
3. **Prompt-Laden via `PromptLoader`:**
   * Der `PromptLoader` liest die `prompt_manifest.json` und sucht die entsprechende `.md` Datei.
   * Der Inhalt der `.md` Datei wird mit den globalen Qualitätsregeln aus `_global_quality_rules.md` verschmolzen.
4. **API-Request Zusammenstellung:**
   * Der extrahierte Rohtext (Webseite, Transkript oder PDF) wird als Benutzerinhalt (User-Prompt) verpackt.
   * Die Systeminstruktionen (System-Prompt) werden dediziert an die Gemini API übergeben.
5. **API-Aufruf & Redundanz-Fallbacks:**
   * Der Request wird an das primäre Modell (`gemini-2.5-flash`) übermittelt.
   * Tritt ein Quota-Fehler (HTTP 429) oder Serverfehler (HTTP 503) auf, schwenkt das System augenblicklich auf das robuste Ausfallmodell (`gemini-3.5-flash`) um.
6. **Robust-Parsing & Bereinigung:**
   * Die Antwort durchläuft einen mehrstufigen Parser (Moshi-JSON -> Syntaktische JSON-Sanierung -> Regex-Fallback).
7. **Auslieferung ans UI:**
   * Die bereinigte Datenklasse `AbstractorSummary` wird an das Compose-UI übergeben und fehlerfrei dargestellt.

---

## 3. Schlüsselkomponenten im Detail

### 3.1. `PromptLoader.kt` & `prompt_manifest.json`
* **Aktiver Zustand:** Das Asset-Prompt-System ist vollständig produktiv und aktiv.
* **Verantwortung:** Dynamisches Laden der Systemprompts aus dem Dateisystem zur Laufzeit.
* **Manifest-Routing:** Die Datei `prompt_manifest.json` fungiert als primäres, produktives Routing:
  ```json
  [
    {
      "analysis_type": "STANDARD_WEBSEITE",
      "file": "F_STANDARD_WEBSEITE.md"
    },
    ...
  ]
  ```
* **Robustheit & Fallbacks:** Sollte eine Prompt-Datei in den Assets fehlen, beschädigt oder leer bzw. rein whitespace-basiert sein, greift das System vollautomatisch auf seine hardcodierten Kotlin-Varianten als Sicherheitsnetz zurück. Der `PromptLoader` loggt solche Vorfälle und mangelndes Routing im Manifest explizit zur Fehlererkennung.
* **Performance-Vorteil:** Bereits gelesene Markdown-Prompts werden im Arbeitsspeicher performant gecacht (`ConcurrentHashMap`), um unnötige Festplatten-I/O-Zyklen bei wiederholten Aufrufen zu vermeiden.

### 3.2. `GeminiNetwork.kt` (`GeminiRepository`)
* **Verantwortung:** Der technische Kern für die Kommunikation mit der Google AI Studio API.
* **Moshi-JSON-Schnittstelle:** Erzwingt strukturierte Ausgaben über definierte Deserialisierungsklassen (`AbstractorSummary`).
* **Grounding & Web-Search-Verhalten:** Bei `AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR` (oder optionalen User-Befehlen) wird Google Search Grounding als Tool injiziert. Weil Grounding und feste JSON-Schemata in der Gemini API inkompatibel sind, deaktiviert das Repo bei aktivem Search-Grounding die JSON-Schema-Pflicht und verlässt sich auf die unstrukturierte Auswertung.

### 3.3. Zuverlässigkeits- & Parsing-Strategie
Da LLMs trotz klarer Vorgaben fehlerhafte JSON-Strukturen oder unvollständige Datenströme zurückliefern können, greift eine dreistufige Absicherungsarchitektur:

1. **Ebene A (Moshi Deserialisierung):** Der Standardversuch, ein glattes, schemakonformes JSON-Objekt auszulesen.
2. **Ebene B (Syntaktische Vor-Korrektur):**
   * Entfernung von typischen LLM-Formatfehlern (wie ungültigen abschließenden Kommata `,\s*\}`).
   * Automatischer Key-Mapping-Abgleich (Konvertierung von CamelCase in SnakeCase bei Schlüsselfeldern).
3. **Ebene C (Robust Regex Fallback Parser):**
   * Sollte das JSON-Parsing komplett fehlschlagen, durchforstet ein toleranter Regex-Parser die Rohantwort des LLMs.
   * Er extrahiert über reguläre Ausdrücke die Felder `title`, `short_description` und `owner` sowie die Bulletpoints aus `key_takeaways`.
   * Notfalls werden unstrukturierte Zeilen mit standardmäßigen Aufzählungszeichen (`-`, `•`, `*`) oder Ziffern vollautomatisch in saubere Bulletpoint-Objekte konvertiert.

---

## 4. Übersicht der Schichten (Clear separation of concerns)

1. **LLM-Intelligenz (`F_*.md` & `_global_quality_rules.md`):**
   Definiert die journalistischen Qualitätsmaßstäbe, Filterregeln, Zielgruppenfokussierung und das visuelle Bulletpoint-Design.
2. **Routing / Konfiguration (`prompt_manifest.json`):**
   Verknüpft die fachlichen Analysemodi mit den physischen Textdateien, ohne dass eine Zeile Kotlin-Code geändert werden muss.
3. **Runtime, API & Parsing (`Kotlin-Module`):**
   Übernimmt die DNS-Auflösung von Quelltexten, steuert die Modell-Zuverläßigkeit (Flash 2.5 & 3.5), wickelt Timeouts ab, saniert JSON-Rückgaben und führt Fallback-Analysen aus.
4. **Präsentationsschicht (`Jetpack Compose UI`):**
   Moderne, barrierefreie und reaktive Oberfläche mit komfortablen Nutzerrezeptoren (Dateiauswahl, Teilen-Dialog, interaktives Dashboard).
