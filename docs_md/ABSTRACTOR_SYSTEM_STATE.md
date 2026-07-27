# Technische Projekt-Dokumentation: Relevantor (Abstractor) System-Soll-Zustand

Dieses Dokument beschreibt den exakten technischen, architektonischen und promptseitigen Ist-Zustand des Relevantor-Systems (auch bekannt als Abstractor-App) mit Stand **15. Juli 2026**. Es dient als vollständige, integrationsbereite Datenbasis für Entwickler und LLMs zur nahtlosen Systemrekonstruktion.

---

## 1. PROJEKTÜBERSICHT

### 1.1 Was ist Relevantor (Abstractor)
Relevantor ist eine datenschutzfokussierte, performante Android-Anwendung, die strukturierte, qualitativ hochwertige Textanalysen und Informationsextraktionen auf Basis von Web-URLs, YouTube-Videos, hochgeladenen PDF-Dokumenten und manuellen Texteingaben bereitstellt. Das System nutzt modernste Generative-AI-Modelle (spezifisch Google Gemini), um Inhalte auf Deutsch prägnant zusammenzufassen und kritisch zu durchleuchten.

### 1.2 Ziel der App
Die App fungiert als intelligente Entscheidungshilfe und kognitiver Entlastungsfilter. Sie soll:
- Große, komplexe Textdatenmengen und multimediale Quellen in Sekundenschnelle erfassbar machen.
- Dem Anwender eine fundierte Entscheidungsgrundlage bieten, ob das zeitintensive Studium der Originalquelle notwendig ist.
- Fakten von Meinungen trennen, Fehlinformationen lokalisieren und zeitliche Relevanz analysieren.

### 1.3 Aktive Analysefunktionen
Das System verfügt über **12 aktive Hauptanalysefunktionen** (ausgedrückt im `AnalysisType`-System):
1. **Standard-Webseite (`STANDARD_WEBSEITE` / `WEB_SUMMARY`):** Ganzheitliche, strukturierte Inhaltsanalyse und Synthese zentraler Erkenntnislinien.
2. **Top 3 Kernaussagen (`TOP_3_KERNAUSSAGEN` / `KEY_TAKEAWAYS`):** Identifikation der 3 wichtigsten, eigenständigen Hauptaussagen in präziser, ein- bis zweisatzbezogener Form.
3. **Fakt-oder-Meinung (`FACTS_VS_OPINIONS_ANALYZER` / `FACTS_VS_OPINIONS`):** Rigorose Klassifikation von Aussagen in Fakten, Meinungen, Werbung, Mutmaßungen und Spekulationen.
4. **Perspektiven-Finder (`PERSPECTIVES_AND_COUNTERPOSITIONS` / `PERSPECTIVES_COUNTERPOSITIONS`):** Ermittlung alternativer Ansichten, Einseitigkeiten und Auslassungen in der Quellenargumentation.
5. **Video- & Multimedia-Analyse (`MULTIMEDIA` / `MULTIMEDIA_ANALYSIS`):** Filterung und Verdichtung von YouTube-Untertiteltranskripten ohne Intro- und Outro-Rauschen.
6. **Frage an die Quelle (`FREIE_QUELLENANFRAGE` / `FREE_SOURCE_QUERY`):** Gezielte Beantwortung nutzerdefinierter Freitext-Anfragen direkt auf Basis der Quelle.
7. **Dokument zusammenfassen (`DOKUMENTE` / `DOCUMENT_SUMMARY`):** Inhaltsanalyse hochgeladener PDF-Dokumente oder langer manuell eingefügter Rohdaten.
8. **Aktualitäts-Check (`AKTUALITAETS_CHECK` / `FRESHNESS_CHECK`):** Abgleich von Quellaussagen mit Live-Suchdaten (Grounding) zur Bewertung der zeitlichen Relevanz.
9. **Fehlinformations-Radar (`FEHLINFORMATIONS_RADAR` / `MISINFORMATION_RADAR`):** Lokalisierung von Clickbait, logischen Fehlschlüssen, unvollständigen Argumentationen und Manipulationen (Grounding-forced).
10. **Risikoanalyse (`RISIKO_ANALYSE` / `RISK_ANALYSIS`):** Konstruktion eines fundierten Risikoprofils mit Fokus auf Schwachstellen und Nachteile des Quellinhalts.
11. **Business-Inkubator (`BUSINESS_INKUBATOR`):** Extraktion von bis zu 3 geschäftlichen Konzepten, Startup-Szenarien oder SaaS-Ideen aus der Quelle.
12. **Weitere relevante Aspekte (`WEITERE_RELEVANTE_ASPEKTE` / `RELEVANT_ASPECTS`):** Erfassung ergänzender, konstruktiver Zusatzperspektiven ohne aggressive Quellkritik.

---

## 2. ARCHITEKTUR

Das System implementiert ein striktes **Clean Architecture**-Konzept in Kombination mit dem **MVVM (Model-View-ViewModel)**-Entwurfsmuster.

```
[UI-Ebene: Jetpack Compose] 
       │ ▲
       ▼ │ (StateFlow-Observierung)
[ViewModel-Ebene: MainViewModel]
       │ ▲
       ▼ │ (UseCase-Ausführung)
[Domain-Ebene: UseCases & Domänen-Modelle]
       │ ▲
       ▼ │ (Repository-Abstraktion)
[Data-Ebene: Repositories, Datenbanken, Netzwerk]
```

### 2.1 Schichtenverteilung
- **UI-Schicht (`com.example.ui`):** Container-basierte Jetpack Compose Oberflächen. Unterstützt adaptive Layouts (Compact/Expanded via NavigationRail oder Bottom Bar) für Smartphones und Tablets sowie systemweite Windows-Insets (`enableEdgeToEdge()`).
- **Domain-Schicht (`com.example.domain`):** Enthält reine, framework-unabhängige Geschäftslogik. Repräsentiert UseCases (`AnalyzeContentUseCase`, `ExtractContentUseCase`, `LoadHistoryUseCase`, `SaveAnalysisUseCase`, `SyncUserDataUseCase`) und Datenkontrakte (`CanonicalAnalysisInput`, `DomainSummary`, `TakeawayItem`, `AnalysisTrace`).
- **Data-Schicht (`com.example.data`):** Framework-spezifische Implementierungen wie HTTP-Clients (Retrofit/OkHttp), lokale SQLite-Persistenz (Room), PDF-Parsing, URL-Normalisierung, YouTube-Caption-Abfragen sowie die System-Engine zur Ausführung der Gemini API Requests.

### 2.2 Datenfluss
```
[Nutzer-Eingabe: URL / Datei / Freie Query]
                     │
                     ▼
       [MainActivity / MainViewModel]
                     │
                     ▼
          [ExtractContentUseCase]
                     │
                     ▼
      [ContentExtractionRepositoryImpl]
         ├── Web-Parsing   ──► [WebpageExtractor]
         ├── YouTube-API   ──► [YoutubeTranscriptHelper]
         └── PDF-Parsing   ──► [FileProcessingHelper]
                     │
                     ▼
          [AnalyzeContentUseCase]
                     │
                     ▼
         [AnalysisRepositoryImpl]
                     │
                     ▼
          [AnalysisRegistryImpl] ──► Mapped auf korrekte Engine
                     │
                     ▼
             [BaseGeminiEngine]
         ├── Lädt Prompts  ──► [AndroidAssetPromptLoader]
         ├── Lädt Fallback ──► [PromptFallbackProvider]
         ├── Generiert     ──► [PromptEngine] (Kombiniert spezifisch + _global_quality_rules.md)
         └── API-Call      ──► [GeminiApiService] (via Retrofit)
                     │
                     ▼
         [SummaryResponseParser] (JSON Extraktion, Bereinigung & Robustheits-Regex)
                     │
                     ▼
        [RuntimeVerificationLayer] (Syntaktische & semantische Validierung des Kontrakts)
                     │
                     ├───────────────( Wenn OK )───────────────┐
                     ▼                                         ▼
         [SaveAnalysisUseCase]                          [MainViewModel] (Success State)
                     │                                         │
                     ▼                                         ▼
         [Room Database] (analyses table)               [Jetpack Compose UI Update]
```

### 2.3 Zentrale Komponenten
- **`MainActivity`:** Einstiegspunkt der App. Aktiviert `enableEdgeToEdge()` für nahtlose Displays und steuert adaptive Screens basierend auf WindowSizeClasses.
- **`MainViewModel`:** Zentraler State-Halter für die Benutzeroberfläche. Steuert Ladezustände, Fehler, die Historien-Anzeige und die Parameter der aktuellen Analysekonfiguration.
- **`AnalysisRepository` / `AnalysisRepositoryImpl`:** Schnittstelle und Implementierung zur Delegation der Extraktionsergebnisse an die Analysis Engines.
- **`GeminiNetwork` (repräsentiert durch `GeminiGateway` und `GeminiRepository`):** Zuständig für die Authentifizierung (BuildConfig-Secrets), Netzwerkübertragung und das Mapping der Gemini API Payload-Strukturen.
- **`PromptLoader` / `AndroidAssetPromptLoader`:** Lädt die dedizierten markdown-basierten Systemprompts aus den Android-Assets (`assets/prompts/`).
- **`PromptFallbackProvider`:** Bietet hartcodierte, vollständige Systeminstruktionen im Kotlin-Code, um bei I/O-Fehlern oder Dateiausfällen Ausfallsicherheit zu garantieren.

---

## 3. ANALYSIS ENGINE & ROUTING SYSTEM

### 3.1 Das Routing-System
Wenn eine Analyse gestartet wird, ermittelt die `AnalysisRegistryImpl` anhand des übergebenen `AnalysisType` die zuständige Engine-Klasse. Jede Engine erbt von der abstrakten Klasse `BaseGeminiEngine`:

```kotlin
class WebpageAnalysisEngine(...) : BaseGeminiEngine(...) // Zuständig für STANDARD_WEBSEITE / WEB_SUMMARY
class Top3KeyPointsEngine(...) : BaseGeminiEngine(...)  // Zuständig für TOP_3_KERNAUSSAGEN / KEY_TAKEAWAYS
class DocumentAnalysisEngine(...) : BaseGeminiEngine(...) // Zuständig für DOKUMENTE / DOCUMENT_SUMMARY
```
Andere `AnalysisType`s werden über generische Instanzen von `BaseGeminiEngine` geroutet, die zur Instanziierung einen spezifischen `EngineContract` erhalten.

### 3.2 Prompt-Mapping über `prompt_manifest.json`
Das Laden der passenden Prompt-Dateien erfolgt dynamisch über ein Manifest (`app/src/main/assets/prompts/prompt_manifest.json`). Dieses ordnet jedem `AnalysisType` (inklusive kanonischer Aliase) einen Dateipfad zu:
- `STANDARD_WEBSEITE` -> `"F_STANDARD_WEBSEITE.md"`
- `WEB_SUMMARY` -> `"F_STANDARD_WEBSEITE.md"`
- `TOP_3_KERNAUSSAGEN` -> `"F_TOP_3_KERNAUSSAGEN.md"`
- `KEY_TAKEAWAYS` -> `"F_TOP_3_KERNAUSSAGEN.md"`
- `FACTS_VS_OPINIONS_ANALYZER` -> `"F_FACTS_VS_OPINIONS_ANALYZER.md"`
- `FACTS_VS_OPINIONS` -> `"F_FACTS_VS_OPINIONS_ANALYZER.md"`
- `PERSPECTIVES_AND_COUNTERPOSITIONS` -> `"F_PERSPECTIVES_AND_COUNTERPOSITIONS.md"`
- `PERSPECTIVES_COUNTERPOSITIONS` -> `"F_PERSPECTIVES_AND_COUNTERPOSITIONS.md"`
- `MULTIMEDIA` -> `"F_MULTIMEDIA.md"`
- `MULTIMEDIA_ANALYSIS` -> `"F_MULTIMEDIA.md"`
- `FREIE_QUELLENANFRAGE` -> `"F_FREIE_QUELLENANFRAGE.md"`
- `FREE_SOURCE_QUERY` -> `"F_FREIE_QUELLENANFRAGE.md"`
- `DOKUMENTE` -> `"F_DOKUMENTE.md"`
- `DOCUMENT_SUMMARY` -> `"F_DOKUMENTE.md"`
- `AKTUALITAETS_CHECK` -> `"F_AKTUALITAETS_CHECK.md"`
- `FRESHNESS_CHECK` -> `"F_AKTUALITAETS_CHECK.md"`
- `FEHLINFORMATIONS_RADAR` -> `"F_FEHLINFORMATIONS_RADAR.md"`
- `MISINFORMATION_RADAR` -> `"F_FEHLINFORMATIONS_RADAR.md"`
- `RISIKO_ANALYSE` -> `"F_RISIKO_ANALYSE.md"`
- `RISK_ANALYSIS` -> `"F_RISIKO_ANALYSE.md"`
- `BUSINESS_INKUBATOR` -> `"F_BUSINESS_INKUBATOR.md"`
- `WEITERE_RELEVANTE_ASPEKTE` -> `"F_WEITERE_RELEVANTE_ASPEKTE.md"`
- `RELEVANT_ASPECTS` -> `"F_WEITERE_RELEVANTE_ASPEKTE.md"`

### 3.3 Prompt-Fallback-Mechanismus
Sollte die I/O-Schnittstelle beim Lesen der Asset-Dateien eine Exception werfen, greift die `BaseGeminiEngine` auf den `PromptFallbackProvider` zu. Dieser hält textlich exakt deckungsgleiche Instanzen der Systeminstruktionen im Programmcode und verhindert so Fehlfunktionen und Abstürze zur Laufzeit.

---

## 4. PROMPT-SYSTEM

### 4.1 Struktur der Prompt-Dateien
Jede Prompt-Datei ist im Markdown-Format verfasst und besitzt einen normierten Header:
```markdown
# SYSTEM-PROMPT: [FUNCTION_KEY]

## Prompt Metadata
- Function Key: [FUNCTION_KEY]
- Prompt Version: [VERSION]
- Status: [FROZEN / PROD-LOCKED / EXPERIMENTAL]
- Created: [YYYY-MM-DD]
- Last Modified: [YYYY-MM-DD]
```
Es folgen Abschnitte für:
- **KI-Rolle & Fachpersona** (z. B. Senior Content Analyst, Fact Checker).
- **Nutzerziel** (Informationserleichterung, Zeitersparnis).
- **Analyseverfahren** (Stufe für Stufe Anleitung).
- **Formatierungs- & Ausschlussvorgaben**.

### 4.2 Prompt-Designprinzipien (v2.0 / v2.2)
- **Strikte JSON-Ausgaberegeln:** Keine textlichen Einleitungs- oder Endfloskeln. Die Ausgabe muss valides JSON sein.
- **Deutsch als Zielsprache:** Unabhängig von der Sprache des Quellmaterials ist die Ausgabe strikt auf Deutsch zu formulieren (ausgenommen Eigennamen).
- **Markdown-Verbot in JSON-Feldern (Header Cleanliness):** Die JSON-Strings (`title`, `key_takeaways[].title`) dürfen keinen Fettdruck (`**`) oder Listen-Aufzählungszeichen (z. B. `1.`, `-`, `•`) enthalten, um doppelte Formatierungen auf UI-Ebene zu unterbinden.
- **Kompakte Länge:** Key-Takeaway-Titel dürfen maximal 8 Wörter umfassen (kurze Leitmotive, keine ganzen Sätze). Details umfassen 1 bis maximal 3 Sätze.

### 4.3 Standard-Ausgabekontrakt (`DomainSummary`)
Alle Ausgaben müssen exakt folgendem JSON-Schema entsprechen:
```json
{
  "title": "String (Eindeutiger Quellentitel mit Urheber/Autor falls auffindbar)",
  "original_url": "String (Original-URL oder Dateibezeichnung)",
  "short_description": "String (Ungeschönte Inhaltszusammenfassung in max. 2 Sätzen)",
  "key_takeaways": [
    {
      "title": "String (Maximal 8 Wörter, prägnantes Leitmotiv, kein Markdown, keine Nummerierung)",
      "details": "String (1-3 erklärende Sätze, kein Markdown, keine Nummerierung)"
    }
  ],
  "owner": "String oder null (Herausgeber, Kanalname, Autor)"
}
```

---

## 5. CONTENT PIPELINE

### 5.1 URL-Eingabe-Verarbeitung und Redirects
1. **Normalisierung:** Ergänzt fehlendes `http://` / `https://`, entfernt Leerzeichen und validiert syntaktische Mindestlänge (>= 5 Zeichen).
2. **Redirect-Auflösung:** Über `WebpageExtractor.resolveUrl` wird ein synchroner `HEAD`-HTTP-Request abgesetzt, um gekürzte Link-Dienste (z. B. `t.co`, `lnkd.in`, `bit.ly`) in die kanonische Ziel-URL aufzulösen.
3. **Walled Interception:** Plattform-Regex-Checks fangen URLs ab, die hinter einer Login-Schranke liegen (Facebook, Instagram, LinkedIn, TikTok, Twitter/X, Threads, Pinterest, Xing). Die Pipeline wird gestoppt und liefert ein predefiniertes Hilfelayout, das dem Nutzer rät, den Text zu kopieren und manuell einzufügen.

### 5.2 Webscraping & HTML-Demolierung
- Das Scrapen erfolgt über OkHttp unter Verwendung eines modernen Chrome Desktop User-Agents.
- Bei SSL- oder Handshake-Fehlern erfolgt ein automatischer, unverschlüsselter HTTP-Verbindungsversuch (HTTP-Fallback).
- Der HTML-Extraktor bereinigt den Quelltext durch radikales Entfernen von `<script>`, `<style>`, `<header>`, `<footer>`, `<nav>`, `<aside>` und HTML-Kommentaren. Extrapoliert werden Artikel-Überschrift (`<h1>`, `<title>`) sowie Metatags (`og:title`, `og:description`).

### 5.3 YouTube-Transkripte
- Extraktion der 11-stelligen Video-ID via URL-Decoder.
- `YoutubeTranscriptHelper` kontaktiert Google/YouTube APIs, um Caption-Spuren (Untertitel) oder Video-Beschreibungen auszulesen.
- Wenn kein Transkript geladen werden kann, wird die Verarbeitung nicht abgebrochen, sondern liefert dem Anwender eine klare und detaillierte Fehler-Visualisierung ("Video nicht auslesbar").

### 5.4 Dokumentenverarbeitung (PDF)
- Wird eine PDF-Datei hochgeladen, prüft das System die Dateigröße (Limit: 20 MB).
- Im Modus `USE_DIRECT_PDF_PROCESSING` werden die Rohdaten als Base64-Byte-Array direkt als `inlineData`-Part in die Gemini Request Payload eingespielt (Multi-modales PDF-Grounding).
- Als Fallback fungiert ein lokales Textextraktions-Modul (`FileProcessingHelper.extractTextFromPdf`), um den Klartext offline zu extrahieren.

### 5.5 Multimedia-Verarbeitung
- Der `MULTIMEDIA`-Analysemodus bereitet das Transkript speziell auf: Strukturierung durch Sprecherwechsel, Bereinigung von Füllwörtern und Eliminierung von Standard-Phrasen ("Abonniert den Kanal", "Klickt die Glocke").

### 5.6 Freie Quellenanfrage
- Wird der Typ `FREIE_QUELLENANFRAGE` gewählt, wird der Inhalt des Eingabefelds `freeQuery` ausgelesen und als separater Kontextblock in die Gemini Request Payload injiziert, um gezielte inhaltliche Fragen zu beantworten.

---

## 6. GEMINI INTEGRATION

### 6.1 Aufbau des GeminiRequests
Die Abfrage erfolgt als HTTP-POST-Request an die Schnittstelle:
`v1beta/models/gemini-2.5-flash:generateContent?key=[API_KEY]`

### 6.2 Übertragene Payloads
- **`systemInstruction`:** Kombination aus spezifischem Funktionsprompt (z. B. `F_STANDARD_WEBSEITE.md`) und den globalen Qualitätsvorgaben (`_global_quality_rules.md`).
- **`contents`:** Eine Liste aus Parts:
  - `text`-Part mit der Original-URL bzw. dem Dateinamen.
  - `inlineData`-Part mit den Base64-PDF-Bytes (falls anwendbar).
  - `text`-Part mit dem bereinigten Scraping-Text (`enrichedText`).
  - `text`-Part mit der Freitextabfrage (`freeQuery` - falls aktiv).
- **`generationConfig`:**
  - `responseMimeType`: `"application/json"` (HINWEIS: Wird bei aktivem Grounding zwingend auf `null` gesetzt, da die Gemini API keine simultane Nutzung von Google Search Grounding und JSON-Schemas gestattet).
  - `responseSchema`: Vollständiges JSON-Schema (nur wenn Grounding inaktiv ist).
  - `temperature`: Variiert je nach Modus von `0.1` (Faktenprüfung) bis `0.8` (Kreative Ideenfindung).
  - `maxOutputTokens`: Standardmäßig `4096`.

---

## 7. GROUNDING-STRATEGIE (GOOGLE SEARCH)

| Modus | Beschreibung | Verwendet in |
| :--- | :--- | :--- |
| **FORCED** | Google Search Grounding ist zwingend aktiv, um Echtzeit-Daten zu beziehen. | `AKTUALITAETS_CHECK`, `FEHLINFORMATIONS_RADAR` |
| **OPTIONAL / FALLBACK** | Tritt in Kraft, wenn das Scraping fehlschlägt oder < 500 Zeichen liefert. | Andere Standard-Webanalysen |
| **DISABLED** | Grounding ist vollständig deaktiviert. | YouTube, PDFs, Freie Eingaben, Walled Gardens |

### Coexistence Constraint (API-Konflikt)
Da Gemini keine strukturierten JSON-Schemas zusammen mit Google Search Grounding unterstützt, gilt:
- Bei aktivem Grounding entfällt das JSON-Schema im Request (`responseMimeType = null`).
- Die strukturierten Daten werden durch die Engine über hochgradig optimierte Regular Expressions im `SummaryResponseParser` robust extrahiert, um die Schema-Konformität zu bewahren.

---

## 8. BEKANNTE PROBLEME & BUGS

1. **FACTS_VS_OPINIONS DNS/SSL-Historie:** Einige restriktive Domains blockieren Standard-HTTP-Verbindungen oder schlagen bei DNS-Lookups fehl. Gelöst durch robustes Fail-Over auf Plain-HTTP-Scraping und informative "Inhalt nicht auswertbar"-Ansichten im UI.
2. **Fehlinformations-Radar Overclassification:** Da das Modell extrem kritisch geschult ist, neigt es in Einzelfällen dazu, harmlose stilistische Übertreibungen als manipulative Falschbehauptungen zu werten. Gegenmaßnahme: Strikte Drosselung der Temperatur auf `0.1`, um rein rationale Faktensynthesen zu erzwingen.
3. **YouTube Content Extraction Gap:** Bei Videos, die keinerlei Captions besitzen und deren Beschreibung kurz ist, liefert das System eine predefinierte "Video nicht auslesbar"-Ansicht statt eines leeren JSON-Modell-Absturzes.

---

## 9. UI-SYSTEM (JETPACK COMPOSE)

- **Eingabe- & Steuerungszentrum:** Dynamische Textfelder, URL-Schnittstellen und Datei-Picker. Das Eingabefeld für Freitextfragen blendet sich kontextsensitiv nur ein, wenn `FREIE_QUELLENANFRAGE` aktiv ist.
- **Ergebnisdarstellung:** Rendert das Analyseergebnis nicht als unformatierten Textklumpen, sondern in klaren Abschnitten. Takeaways werden in Material 3 Custom Cards (`TakeawayCard.kt`) strukturiert und animiert ausgegeben.
- **Historie:** Lokale Offline-Caching-Übersicht aller erfolgreich durchgeführten Analysen in einer Room-Datenbank. Analysen können kopiert, geteilt oder direkt gelöscht werden.

---

## 10. DATEI- & PROMPTSTRUKTUR

### 10.1 Vollständige Asset-Prompt-Liste
Alle Prompts liegen im Verzeichnis `app/src/main/assets/prompts/`:
- `F_STANDARD_WEBSEITE.md` (Version 3.3 - **STATUS: FROZEN**)
- `F_TOP_3_KERNAUSSAGEN.md` (Version 2.2 - **STATUS: PROD-LOCKED**)
- `F_FACTS_VS_OPINIONS_ANALYZER.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_MULTIMEDIA.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_FREIE_QUELLENANFRAGE.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_DOKUMENTE.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_AKTUALITAETS_CHECK.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_FEHLINFORMATIONS_RADAR.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_RISIKO_ANALYSE.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_BUSINESS_INKUBATOR.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `F_WEITERE_RELEVANTE_ASPEKTE.md` (Version 2.0 - **STATUS: PROD-LOCKED**)
- `_global_quality_rules.md` (Globale Formatierungs- und Qualitätsvorgaben)

---

## 11. IMPLEMENTIERUNGS- & LAUFZEITMATRIX

| Funktion | Code-Engine | Primäre Datenquelle | Grounding-Status | System-Status |
| :--- | :--- | :--- | :--- | :--- |
| **Webseite Standard** | `WebpageAnalysisEngine` | Scraped Body Content | Optional (Fallback) | **OK** (Production) |
| **Top 3 Kernaussagen** | `Top3KeyPointsEngine` | Scraped Body Content | Optional (Fallback) | **OK** (Production) |
| **Fakt-oder-Meinung** | `BaseGeminiEngine` | Scraped Body Content | Inaktiv | **OK** (Production) |
| **Perspektiven Finder**| `BaseGeminiEngine` | Scraped Body Content | Inaktiv | **OK** (Production) |
| **Multimedia Analyse** | `BaseGeminiEngine` | Captions (YT-Transcript) | Inaktiv | **OK** (Production) |
| **Frage an die Quelle**| `BaseGeminiEngine` | Scraped Content + Query | Inaktiv | **OK** (Production) |
| **Dokument auswerten** | `DocumentAnalysisEngine` | PDF Multi-modal / Text | Inaktiv | **OK** (Production) |
| **Aktualitätscheck** | `BaseGeminiEngine` | Scraped Body + Search | **FORCED (Aktiv)** | **OK** (Production) |
| **Fehlinfo Radar** | `BaseGeminiEngine` | Scraped Body + Search | **FORCED (Aktiv)** | **OK** (Production) |
| **Risikoanalyse** | `BaseGeminiEngine` | Scraped Body Content | Inaktiv | **OK** (Production) |
| **Business Inkubator**| `BaseGeminiEngine` | Scraped Body Content | Inaktiv | **OK** (Production) |
| **Weitere Aspekte** | `BaseGeminiEngine` | Scraped Body Content | Inaktiv | **OK** (Production) |

---

## 12. REGELN FÜR SYSTEMINTERAKTION

Dieses Dokument beschreibt ausschließlich den verifizierten Ist-Zustand des Gesamtsystems. Anpassungen an den Prompts müssen über die definierte Change-Prompt-Pipeline (`CP-01` ff.) erfolgen. Strukturelle Änderungen am JSON-Ausgabekontrakt sind streng verboten, da sie die nachgeschaltete `RuntimeVerificationLayer` verletzen und zu unmittelbaren API-Parser-Abbrüchen führen.
