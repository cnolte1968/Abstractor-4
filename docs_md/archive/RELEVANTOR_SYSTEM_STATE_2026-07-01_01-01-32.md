# RELEVANTOR – SYSTEM STATE & TECHNICAL ARCHITECTURE
*Dokumenten-Typ: Technische System-Spezifikation (Ist-Zustand)*
*Letztes Update: 01.07.2026, 01:01:32*

---

## 1. PROJEKTÜBERSICHT

**Relevantor** ist eine hochgradig spezialisierte Sicherheits-, Glaubwürdigkeits- und Inhalts-Analyse-Applikation für Android. Im Gegensatz zu herkömmlichen Zusammenfassungs-Werkzeugen, die oft nur flache Textverdichtungen liefern, seziert Relevantor digitale Quellen mit wissenschaftlicher und strategischer Präzision.

### Kernziele der Applikation
* **Rauschunterdrückung:** Eliminierung von Marketing-Sprech, Intros, Sponsorensegmenten und inhaltsleerem Füllmaterial.
* **Hebelwirkung für Wissensarbeiter:** Strukturierte Extraktion von harten Fakten, wissenschaftlichen Argumenten, blinden Flecken, Risiken und unkonventionellen Geschäftspotenzialen.
* **Echtzeit-Validierung:** Direktes Erfassen und Analysieren von URLs (Webseiten, YouTube-Videos), lokalen Dokumenten (PDFs, TXT, Word, Excel, PowerPoint) sowie Bildern.

### System-Umfang
Die App verarbeitet unterschiedliche Medienquellen vollständig lokal auf dem Gerät (Extraktion und Parsing) und delegiert die hochgradige semantische Analyse über strukturierte JSON-Schnittstellen an die Google Gemini API.

---

## 2. ARCHITEKTUR

Das Projekt folgt einer sauberen und robusten **MVVM (Model-View-ViewModel)**-Architektur, kombiniert mit **Domain-Driven-Design-Strukturen (Use Cases)** und dem **Repository-Pattern**.

### Datenfluss (End-to-End Pipeline)
1. **Benutzereingabe / Share-Intent:** Der Anwender gibt eine URL ein, wählt einen Analysetyp im Cockpit, fügt eine optionale freie Abfrage (`freeQuery`) hinzu oder teilt eine URL direkt aus einer externen App (z. B. Chrome, YouTube).
2. **Eingabe-Validierung & Preprocessing (ViewModel):**
   * URLs werden auf Formatfehler geprüft und Weiterleitungen (z. B. Kurz-URLs wie `bit.ly`, `lnkd.in`) über einen HTTP-HEAD-Request aufgelöst (`WebpageExtractor`).
   * Bei YouTube-URLs extrahiert der `YoutubeUrlDecoder` die Video-ID.
   * Lokale Dateien werden über den `ContentResolver` eingelesen.
3. **Inhaltsgewinnung (Local Content Extraction Layer):**
   * *Webseiten:* Der `WebpageExtractor` lädt das HTML via OkHttp, entfernt redundanten Code (`<script>`, `<style>`, `<nav>`, `<footer>`) und extrahiert Metadaten (Titel, Description) sowie den bereinigten Fließtext (maximal 15.000 Zeichen).
   * *YouTube:* `YoutubeTranscriptHelper` versucht, das offizielle XML-Transkript des Videos abzurufen. Schlägt dies fehl, greift (speziell im Multimedia-Modus) eine erweiterte HTML-Inhaltsgewinnung, welche die gesamte Beschreibung, Kapitel und Kapitel-Metadaten direkt aus dem eingebetteten Player-JSON zieht.
   * *Dokumente (Text/Office):* `FileProcessingHelper` liest Plaintext (TXT, MD, CSV, JSON) oder parst Word (`.docx`), Excel (`.xlsx`) und PowerPoint (`.pptx`) direkt über einen speicherschonenden `ZipInputStream`-XML-Parser.
   * *Dokumente (PDF/Bilder):* Binäre Formate und visuelle Dokumente werden in-memory in ein Base64-Format konvertiert, um sie als `inlineData` direkt an die Gemini API zu senden.
4. **Use Case & Repository Vermittlung:**
   * `AnalyzeContentUseCase` steuert die Verteilung.
   * `AnalysisRepositoryImpl` delegiert an `GeminiRepository` unter Beigabe der extrahierten Daten, des Typs und der `freeQuery`.
5. **Prompt-Orchestrierung:**
   * Der `PromptLoader` liest das Manifest (`prompt_manifest.json`) und lädt den typspezifischen Prompt (`F_*.md`) sowie die globalen Systemrichtlinien (`_global_quality_rules.md`) aus den Assets.
   * `PromptEngine` bündelt beide Texte zu einer konsistenten System-Instruction.
6. **Gemini API Call:**
   * `GeminiRepository` konfiguriert die Parameter (Temperatur, Modell, JSON-Schema) basierend auf den `AnalysisRuntimeConfigs`.
   * Ein HTTP-POST-Request wird an den API-Endpunkt von `gemini-2.5-flash` abgesetzt.
   * **Exception & Format Handling:** Erkennt das System einen Format- oder Verarbeitungsfehler, führt das Repository bis zu 2 Versuche (`maxAttempts = 2`) mit dem primären Modell aus. Schlagen beide Versuche fehl, greift ein sicherer, lokaler Struktur-Fallback.
7. **Parsing & Speicherung:**
   * Die JSON-Antwort von Gemini wird von `SummaryResponseParser` validiert und in ein stark typisiertes `DomainSummary`-Objekt überführt.
   * Die erfolgreiche Analyse wird automatisch lokal über Room (`RelevantorDatabase` / `AnalysisDao`) persistiert.
8. **UI-Aktualisierung:** Der `UiState` im ViewModel wechselt auf `Success` und rendert die detaillierten Ergebnisse (Titel, Beschreibung, strukturierte Bulletpoints).

### Zentrale Komponenten
* **`MainActivity.kt`:** Enthält das gesamte Jetpack Compose UI-System, registriert Activity-Launcher für den System-Dateipicker und verwaltet den Lifecycle (insb. die direkte Auswertung von geteilten Share-Intents).
* **`MainViewModel.kt`:** Verwaltet den Zustand der Benutzeroberfläche (`UiState`), steuert asynchrone Coroutine-Scopes und koordiniert die Extraktions- und Analyse-Schnittstellen.
* **`AnalysisRepositoryImpl.kt`:** Brücke zwischen der lokalen Raumdatenbank (Historie) und dem Remote-Service.
* **`GeminiRepository.kt`:** Steuert die HTTP-Interaktion mit der Generative Language API, bündelt Payloads, implementiert das strukturierte JSON-Response-Schema und verwaltet die Retry-Logik.
* **`PromptLoader.kt` / `PromptEngine.kt`:** Liest dynamic Assets aus, implementiert einen In-Memory Cache für geladene Prompts und kombiniert systemspezifische Qualitätsregeln mit fachspezifischen Analyseaufgaben.
* **`FileProcessingHelper.kt`:** Ein optimierter, bibliotheksfreier Extraktor für Office-Dateien und Konverter für Binärdaten.

---

## 3. ANALYSIS ENGINE

Die Steuerung des Analyse-Verhaltens erfolgt vollständig über das **AnalysisType Routing-System**.

### Mapping über das Prompt-Manifest
Die Zuordnung zwischen dem internen Code-Enum und den Markdown-Anweisungen in den Assets wird über die Datei `app/src/main/assets/prompts/prompt_manifest.json` konfiguriert:

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
  "BUSINESS_INKUBATOR": "F_BUSINESS_INKUBATOR.md"
}
```

### Fallback-Konstrukt bei Asset-Fehlern
Sollte das Lesen einer Prompt-Datei scheitern (z. B. bei Beschädigung des APKs oder Pfadkonflikten), greift eine zweistufige Absicherung:
1. **Internes Hardcoded-Mapping:** `PromptLoader` greift auf ein internes `FALLBACK_MAPPING` zu, um den standardmäßigen Dateinamen zu ermitteln.
2. **Text-Sicherheitsnetz:** Schlägt auch dies fehl, liefert der `PromptFallbackProvider` vordefinierte, komprimierte Instruktionen im Kotlin-Code zurück. Dadurch ist ein Absturz der Applikation aufgrund fehlender Prompts ausgeschlossen.

---

## 4. PROMPT-SYSTEM

Die Prompts in Relevantor sind nach strengen ingenieurwissenschaftlichen Qualitätsmaßstäben konzipiert, modularisiert und architektonisch sauber getrennt.

### Clean-Architecture bei den Funktionsprompts
Die vor kurzem aktualisierten Prompts (`F_STANDARD_WEBSEITE.md` (v2.0 CLEAN), `F_TOP_3_KERNAUSSAGEN.md` (v3.0 CLEAN ARCHITECTURE) und `F_MULTIMEDIA.md` (v2.0 CLEAN)) folgen einer strengen architektonischen Trennung zwischen Inhaltsanalyse und Benutzeroberfläche:
* **Keine UI-Vorgaben:** Diese Prompts beschreiben rein inhaltliche Logik und verzichten vollständig auf Formatierungs- oder Layoutannahmen (wie z. B. Icons, Abstände, Schriftarten, UI-Elemente oder Nummerierungsdarstellungen).
* **UI-Souveränität:** Sämtliche Präsentations- und Formatentscheidungen werden vollständig und souverän durch die Client-UI und die dortige Implementierung gesteuert.

### Globale Qualitätsschranke (`_global_quality_rules.md`)
Diese Datei enthält systemübergreifende Qualitätsanforderungen, die vor jeden typspezifischen Prompt gehängt werden:
* **Verbot von Floskeln:** Keine Einleitungen wie *"Hier ist die Zusammenfassung..."* oder *"Basierend auf Ihren Daten..."*. Die Ausgabe startet direkt mit dem JSON-Objekt.
* **Umfangs-Adäquanz:** Die Detailtiefe muss sich proportional an der Länge und Komplexität des Quellmaterials ausrichten.
* **Format-Einschränkungen:** Verbot von tiefer verschachtelten Listen. Bulletpoints müssen klar strukturiert sein und immer mit einem **fettgedruckten Leitbegriff** beginnen.

### JSON Output Contract (`DomainSummary`)
Alle Prompts zwingen die KI zur Ausgabe eines exakten JSON-Formats. Das Schema entspricht folgendem Kotlin-Datenmodell:

```kotlin
data class DomainSummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val originalUrl: String,
    val shortDescription: String,
    val keyTakeaways: List<TakeawayItem>,
    val owner: String? = null,
    val timestamp: String = getCurrentFormattedTimestamp()
)

data class TakeawayItem(
    val title: String,
    val details: String,
    val isFavorite: Boolean = false
)
```

*Hinweis zum Parser:* Die JSON-Antwort von Gemini liefert strukturell ein strukturiertes Objekt mit `key_takeaways` als Array von Objekten (mit `title` und `details`) oder alternativ Strings. Der robust konfigurierte `SummaryResponseParser` beherrscht beide Varianten nahtlos und überführt sie sicher in das einheitliche App-Domänenmodell.

---

## 5. CONTENT PIPELINE (KRITISCH)

Relevantor nutzt dedizierte, asynchrone Pipelines zur Inhaltsakquise vor der Übergabe an das LLM.

```
                  ┌──────────────────────────────┐
                  │          URL-Eingabe         │
                  └──────────────┬───────────────┘
                                 ▼
                    Ist es eine YouTube-URL?
                    ├─────── JA ───────┐
                    │                  ▼
                    │       [YoutubeUrlDecoder]
                    │       Extrahiert Video-ID
                    │                  │
                    │                  ▼
                    │       [YoutubeTranscriptHelper]
                    │       Holt XML-Transkript / Captions
                    │                  │
                    │       Transkript erfolgreich?
                    │       ├── JA ──► Übergabe an Gemini Network
                    │       │
                    │       └── NEIN (oder sehr kurz)
                    │             │
                    │             ▼
                    │       Ist AnalysisType == MULTIMEDIA?
                    │       ├── JA ──► [Extended Content Fetch]
                    │       │          Extrahiert Titel, Kanal, Vollbeschreibung & Kapitel
                    │       │          aus eingebettetem Player-HTML und übergibt Text an Gemini.
                    │       │
                    │       └── NEIN ─► [OembedMetadata]
                    │                   Standard-Metadaten-Fallback
                    ▼
          [WebpageExtractor]
          - HEAD-Redirect-Auflösung
          - HTML-Scraping & Säuberung
          - Stripping von Scripts/Styles/Nav
          - Meta-Tag-Extraktion (Titel, Desc)
```

### URL-Verarbeitung & Scraping (`WebpageExtractor`)
* **Redirect Resolution:** Filtert Kurz-Links (z. B. `lnkd.in`, `t.co`) über einen schnellen HTTP-HEAD-Request und löst diese in die tatsächliche Ziel-URL auf.
* **HTML-Säuberung:** Entfernt unnötigen Code (CSS, JS, Header, Navigationsleisten, Footer) und isoliert den reinen Text-Inhalt.
* **Längen-Begrenzung:** Der bereinigte Fließtext wird hart auf maximal 15.000 Zeichen gekürzt, um Token-Bloat zu verhindern und im sicheren Prompt-Fenster des Modells zu bleiben.

### YouTube Inhaltsgewinnung Layer (`YoutubeTranscriptHelper`)
Da Metadaten allein nicht ausreichen, um ein Video inhaltlich tiefgründig zu bewerten, nutzt die Pipeline folgendes Schichten-Modell:
1. **Echte Captions/Transkripte:** Das System kontaktiert die internen YouTube-Schnittstellen, um das gesprochene Wort als strukturiertes XML-Transkript zu laden. XML-Entitäten werden vollständig dekodiert.
2. **Erweiterter Inhaltsgewinnung-Fallback (Extended Content Fetch - Speziell für `MULTIMEDIA`):**
   * Steht kein Transkript zur Verfügung (z. B. da automatische Untertitel vom Ersteller deaktiviert wurden oder das Video nur Musik enthält), parst die App das YouTube-HTML der Video-URL direkt.
   * Über eine gezielte JSON-Feldextraktion im HTML werden der **Vollständige Titel**, der **Kanal/Ersteller** und die **vollständige Beschreibung** (inklusive vom Uploader hinterlegter Video-Kapitel, Timecodes und detaillierter Fakten) geladen.
   * Dieses ausgiebige Datenpaket wird strukturiert formatiert und als Inhaltsquelle an Gemini übermittelt.
3. **Oembed-Metadaten (Standard Fallback):** Bei anderen Analysetypen, die eine YouTube-URL erhalten, wird auf das leichtgewichtige Oembed-Modell ausgewichen.

### Dokumentenextraktion (`FileProcessingHelper`)
* **Office XML Parser:** Die Formate `.docx`, `.xlsx` und `.pptx` werden ohne schwere externe Java-Bibliotheken (wie Apache POI) direkt über einen lokalen XML-Parser ausgewertet. Die App entpackt das ZIP-Archiv im Stream, liest die Kern-XMLs (`document.xml`, `sharedStrings.xml`, `slide*.xml`) und extrahiert den Text speichereffizient.
* **Multimodaler Fallback:** PDFs und Bilddateien (PNG, JPEG, etc.) werden als Roh-Bytes eingelesen, in Base64 kodiert und als nativer `inlineData`-Inhaltsteil des API-Requests an Gemini gesendet, um das optische Verständnis des Modells direkt zu nutzen.

### Freie Anfrage (`freeQuery` Injection)
Wenn der Anwender eine Freitextfrage im Suchschlitz des Cockpits formuliert, wird diese über die `PromptEngine` priorisiert. Der Prompt zwingt die KI, die gesamte vordefinierte System-Anweisung beizubehalten, sich beim Beantworten jedoch strikt und ausschließlich auf den Kontext der extrahierten Quelle zu beziehen.

---

## 6. GEMINI INTEGRATION

Die Anbindung an die Google AI-Infrastruktur erfolgt über die offizielle REST-Schnittstelle mittels Retrofit.

### Request-Payload Struktur
Der HTTP-POST-Body wird als JSON-Struktur mit folgenden Hauptfeldern übertragen:
* `systemInstruction`: Enthält das aggregierte System-Prompt-Paket (Global Rules + Function-Prompt).
* `contents`: Array von Inhalten. Enthält den Benutzer-Prompt (URL-Verweis, extrahierter Quelltext oder Base64-Inlinedaten).
* `generationConfig`:
  * `temperature`: Definiert den Kreativitätsspielraum (0.1 für Faktenanalysen bis 0.8 für Business-Inkubation).
  * `responseMimeType`: Wird auf `application/json` gesetzt (außer bei aktivem Grounding).
  * `responseSchema`: Übergibt das vordefinierte `ResponseSchema` für strukturierte JSON-Antworten.
* `tools`: Übergibt optional die Konfiguration für Live-Google-Suchen.

### Modell-Hierarchie & Fehlertoleranz
* **Primäres Modell:** `gemini-2.5-flash` – Äußerst stabil, schnell, kosteneffizient und bietet vollen JSON-Schema-Support.
* **Robustheit:** Die Anwendung steuert alle Versuche unter Verwendung von `gemini-2.5-flash`. Schlagen die automatischen Validierungen fehl, fängt ein lokaler Fallback den Ablauf ab, um einen Absturz im UI zu verhindern.

---

## 7. GROUNDING STRATEGIE

Um Halluzinationen zu minimieren und bei aktuellen Ereignissen Zugriff auf Echtzeit-Informationen zu haben, nutzt das System das **Google Search Grounding Tool**.

### Grounding-Modi in den Runtime-Configs
Das Verhalten wird je nach `AnalysisType` gesteuert:
1. **Forced Grounding (`forceGrounding = true`):** Bei Modulen wie `AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR` ist die Websuche zwingend aktiv, um Behauptungen live gegen aktuelle Nachrichtenquellen abzugleichen.
2. **Optional Grounding (`allowUserGrounding = true`):** Bei Standard-Webseiten- oder Multimedia-Analysen entscheidet der Nutzer über einen Toggle im UI, ob die Websuche hinzugeschaltet werden soll.
3. **Deaktiviert (`allowUserGrounding = false`):** Bei der Dokumenten-Analyse (`DOKUMENTE`) ist die Websuche gesperrt, um den Fokus rein auf der hochgeladenen Datei zu belassen.

*Wichtige technische Einschränkung:* Sobald Search Grounding aktiv ist, verbietet die Google API die gleichzeitige Verwendung eines strengen `responseSchema` (JSON-Schema-Erzwingung). In diesem Fall konfiguriert `GeminiRepository` die Payload dynamisch um (MIME-Typ und Schema-Felder werden entfernt), fordert das JSON-Format jedoch weiterhin explizit im Freitext-Prompt ein.

---

## 8. BEKANNTE PROBLEME & HISTORISCHE BUGS

* **Grounding Quota-Engpässe (HTTP 429):**
  Obwohl Google im Dashboard oft hohe Quotas anzeigt, unterliegt das Search Grounding im Free-Tier sehr strengen und intransparenten Limits. Dies fängt die App über ihre robuste Retry-Struktur ab.
* **Scraping-Blockaden:**
  Einige moderne Portale (z. B. LinkedIn, Facebook, Cloudflare-geschützte Webseiten) blockieren standardmäßige OkHttp-Anfragen. In diesen Fällen liefert der WebpageExtractor keinen Text. Das System weist den Anwender im Fehlerfall an, den Text manuell per Copy-Paste in die App einzufügen.
* **Fehlinformations-Radar Overclassification:**
  Durch das extrem strenge Qualitätsprofil im `FEHLINFORMATIONS_RADAR` tendiert die KI dazu, rein stilistische Nuancen oder werbliche Übertreibungen direkt als "Fehlinformation" einzustufen. Der Prompt wurde dahingehend optimiert, klar zwischen "Rhetorik/Marketing" und "sachlich falschen Behauptungen" zu trennen.

---

## 9. UI SYSTEM

Das Cockpit von Relevantor wurde als barrierefreie, performante Ein-Screen-Oberfläche in Jetpack Compose umgesetzt.

### UI-Komponenten & Interaktionen
* **Eingabe-Sektion:** Ein markantes Textfeld für die URL-Eingabe, gekoppelt mit einem Button zum direkten Einfügen aus der Zwischenablage und einem Lösch-Button.
* **Dateiauswahl:** Ein flexibler Button öffnet den nativen System-Dateipicker für Dokumente oder Bilder.
* **Analyseoptionen-Raster:** Ein horizontales, scrollbares Band oder Grid präsentiert die verschiedenen Analysemodule mit verständlichen Beschreibungen und Icons.
* **Suchschlitz für freie Rückfragen (`freeQuery`):** Erlaubt es dem Nutzer, spezifische Fragen an die Inhaltsquelle zu stellen.
* **Ergebnis-Präsentation (Zusammenfassungs-Card):**
  * Zeigt den ermittelten Titel, die Original-URL und eine prägnante Kurzbeschreibung.
  * Die wichtigsten Takeaways werden als strukturierte Liste gerendert.
  * Integrierte Schnellaktionen erlauben das direkte Kopieren, Teilen (über das Android Share-System) oder Neuladen des Ergebnisses.

---

## 10. DATEI- & PROMPTSTRUKTUR

Die wichtigsten System- und Ressourcendateien im Überblick:

```
├── app/
│   ├── build.gradle.kts                       # Build-Konfiguration und Signierungsparameter
│   └── src/main/
│       ├── assets/
│       │   └── prompts/
│       │       ├── prompt_manifest.json       # Routing-Tabelle für alle Analysetypen
│       │       ├── _global_quality_rules.md   # Systemweite Qualitätsmaßstäbe
│       │       ├── F_STANDARD_WEBSEITE.md     # Prompt: Standard Webseiten-Analyse (v2.0 CLEAN)
│       │       ├── F_MULTIMEDIA.md            # Prompt: Transkripte und YouTube-Inhalte (v2.0 CLEAN)
│       │       ├── F_DOKUMENTE.md             # Prompt: Lokale Dokumenten-Analyse
│       │       ├── F_TOP_3_KERNAUSSAGEN.md    # Prompt: 3-Kernpunkte-Verdichtung (v3.0 CLEAN ARCHITECTURE)
│       │       ├── F_AKTUALITAETS_CHECK.md    # Prompt: Relevanz- und Zeitprüfung
│       │       ├── F_FEHLINFORMATIONS_RADAR.md# Prompt: Glaubwürdigkeitsanalyse
│       │       ├── F_RISIKO_ANALYSE.md        # Prompt: Gefahrenkatalog
│       │       ├── F_BUSINESS_INKUBATOR.md    # Prompt: Ableiten von Geschäftsmodellen
│       │       ├── F_FACTS_VS_OPINIONS_ANALYZER.md # Prompt: Fakten vs. Meinungen
│       │       ├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md # Prompt: Perspektivenprüfung
│       │       └── F_FREIE_QUELLENANFRAGE.md  # Prompt: Freie Benutzeranfragen
│       ├── java/com/example/
│       │   ├── MainActivity.kt                # UI-Cockpit und Share-Intent-Handling
│       │   ├── LocalContentExtractionEngine.kt# Brücke zur Inhaltsgewinnung
│       │   ├── ui/
│       │   │   └── MainViewModel.kt           # State-Machine und Geschäftslogik
│       │   └── data/
│       │       ├── AnalysisRuntimeConfigs.kt  # Temperatur, Grounding, Max-Token etc.
│       │       ├── AnalysisType.kt            # Enum der 11 Analysemodi
│       │       ├── BackendFeatureConfig.kt    # Optionale Backend-Flags
│       │       ├── FileProcessingHelper.kt    # Office-Dokumenten-Parser
│       │       ├── GeminiModels.kt            # REST Request/Response Datenstrukturen
│       │       ├── GeminiRepository.kt        # Kern für HTTP-Request und Retry-Logik
│       │       ├── PromptEngine.kt            # Prompt-Kombinations-Logik
│       │       ├── PromptLoader.kt            # Asset-Reader und Cache
│       │       ├── PromptFallbackProvider.kt  # Lokale Ausfallsicherungs-Prompts
│       │       ├── ResponseNormalizer.kt      # Bereinigung und Formatsanierung
│       │       ├── RetrofitClient.kt          # HTTP-Client-Singleton
│       │       ├── RuntimeVerificationLayer.kt# Validierung der LLM-Rückgabe
│       │       ├── SummaryResponseParser.kt   # Moshi & Parser für LLM-JSON
│       │       ├── WebpageExtractor.kt        # Web-Scraper und Redirect-Auflöser
│       │       ├── YoutubeUrlDecoder.kt       # YouTube ID-Parser
│       │       ├── YoutubeTranscriptHelper.kt # YouTube Transkript & Extended Extraction
│       │       ├── local/
│       │       │   ├── RelevantorDatabase.kt  # Room Datenbank-Klasse
│       │       │   └── SessionStorage.kt      # Sitzungs- und Tokenverwaltung
│       │       ├── remote/
│       │       │   └── BackendApiService.kt   # Remote API-Service Definition
│       │       ├── repository/
│       │       │   ├── AnalysisRepositoryImpl.kt
│       │       │   ├── SyncRepositoryImpl.kt
│       │       │   └── UserRepositoryImpl.kt
│       │       └── sync/
│       │           └── SyncScheduler.kt       # Datenabgleich im Hintergrund
│       └── res/
│           └── values/
│               └── strings.xml                # Launcher-Label ("Relevantor")
```

---

## 11. AKTUELLER IMPLEMENTIERUNGSSTATUS

| Analysefunktion | Status | Datenquelle | Grounding-Status |
| :--- | :--- | :--- | :--- |
| **Standard Webseite** | **OK (Produktion)** | `WebpageExtractor` Fließtext | Optional (Standard: Inaktiv) |
| **Multimedia** | **OK (Produktion)** | `YoutubeTranscriptHelper` (Transkript) oder `Extended Content Fetch` (Beschreibung/Kapitel) | Optional (Standard: Inaktiv) |
| **Dokumente** | **OK (Produktion)** | `FileProcessingHelper` (Office-Text) oder Base64 (PDF/Bilder) | **Deaktiviert (Immer Inaktiv)** |
| **3 Kernpunkte** | **OK (Produktion)** | `WebpageExtractor` Fließtext | Optional (Standard: Inaktiv) |
| **Aktualitätsprüfung** | **OK (Produktion)** | `WebpageExtractor` Fließtext + Google Search | **Erzwungen Aktiv** |
| **Fehlinformationsradar** | **OK (Produktion)** | `WebpageExtractor` Fließtext + Google Search | **Erzwungen Aktiv** |
| **Risikoanalyse** | **OK (Produktion)** | `WebpageExtractor` Fließtext | Optional (Standard: Inaktiv) |
| **Business Inkubator** | **OK (Produktion)** | `WebpageExtractor` Fließtext | Optional (Standard: Inaktiv) |
| **Fakt vs. Meinung** | **OK (Produktion)** | `WebpageExtractor` Fließtext | **Deaktiviert (Immer Inaktiv)** |
| **Perspektivenanalyse** | **OK (Produktion)** | `WebpageExtractor` Fließtext | Optional (Standard: Inaktiv) |
| **Freie Quellenanfrage** | **OK (Produktion)** | `WebpageExtractor` Fließtext | **Deaktiviert (Immer Inaktiv)** |

---

## 12. OUTPUT-REGELN & ENTWICKLER-DIREKTIVEN
*Dieses Dokument beschreibt präzise den realen IST-Zustand des Quellcodes und der Produktiv-Assets.*

1. **Keine Code-Veränderungen ohne dieses Manifest zu konsultieren.**
2. **Die Schnittstellen-Kompaktheit für das JSON-Ausgabe-Format (`DomainSummary`) muss zwingend über alle Prompts hinweg beibehalten werden.**
3. **Bei Änderungen an der Inhaltsgewinnung (Scraping/Dateien) muss sichergestellt werden, dass keine unvorhergesehenen Token-Auslastungen (Limit: 15.000 Zeichen bei Text) entstehen.**
