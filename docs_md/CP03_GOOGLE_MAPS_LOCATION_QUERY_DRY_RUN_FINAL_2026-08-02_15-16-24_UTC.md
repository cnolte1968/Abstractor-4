# CP03: Technischer Dry-Run "Frage zum Ort" (GOOGLE_MAPS_LOCATION_QUERY) — Finalfassung

- **Status**: DRY RUN FINAL PASS
- **Datum**: 2026-08-02
- **Autor**: GAIS
- **Ziel-Modul**: `/app/applet/app`
- **Ziel-Datei**: `/app/applet/docs_md/CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_2026-08-02.md`

---

## 1. Verifizierte Funktionsidentität & Registrierung

Anhand der Codebase wurde die exakte Namens- und Registrierungssystematik ermittelt:

1. **Function-ID**: `"GOOGLE_MAPS_LOCATION_QUERY"`
2. **AnalysisType**: `AnalysisType.GOOGLE_MAPS_LOCATION_QUERY` (in `AnalysisType.kt`)
3. **FeatureCatalog**: Registrierung unter Kategorie `"F"` (Google Maps) in `FeatureCatalog.kt`:
   - `FeatureMetadata("GOOGLE_MAPS_LOCATION_QUERY", AnalysisType.GOOGLE_MAPS_LOCATION_QUERY, "Frage zum Ort", "Spezifische Fragen zu einer Location beantworten", "F", 3, Icons.Default.QuestionAnswer, Color(0xFFEA4335), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB))`
4. **AnalysisRegistryImpl**: Zuordnung in `AnalysisRegistryImpl.kt`:
   - Mapping: `AnalysisType.GOOGLE_MAPS_LOCATION_QUERY -> "GOOGLE_MAPS_LOCATION_QUERY"`
   - Registrierung der `LocationQuestionEngine` für die Function-ID `"GOOGLE_MAPS_LOCATION_QUERY"`.
5. **Assets / Prompt Registrierung**:
   - `function_registry.json`: Hinzufügen des Eintrags mit `function_id: "GOOGLE_MAPS_LOCATION_QUERY"`, `prompt_file: "F_GOOGLE_MAPS_LOCATION_QUERY.md"`, `rendering_profile: "standard"`.
   - `prompt_manifest.json`: Hinzufügen des Eintrags `"GOOGLE_MAPS_LOCATION_QUERY": "F_GOOGLE_MAPS_LOCATION_QUERY.md"`.
6. **Prompt-Datei**: Markdown-Format unter `/app/applet/app/src/main/assets/prompts/F_GOOGLE_MAPS_LOCATION_QUERY.md`.

---

## 2. Finale Komponentenstruktur & Verantwortlichkeiten

Um eine Überlastung einzelner Klassen zu verhindern und saubere Testbarkeit (Single Responsibility) zu gewährleisten, wird die Verantwortlichkeit auf drei klar getrennte Komponenten verteilt:

```
           EngineRunner / AnalyzeContentUseCase
                            │
                            ▼
               LocationQuestionEngine (1)
               [AnalysEngine Implementation]
                            │
                            ▼
            LocationQuestionCoordinator (2)
      [Multi-Source Fetching & Execution Flow]
             │                     │
             ▼                     ▼
LocationQuestionPlanner (3)   External APIs & Engines
 [Pure Domain Logic / Rules]   (Places, Reviews, Wiki, Gemini)
```

### 2.1 `LocationQuestionEngine`
- **Verantwortung**: Implementiert `AnalysisEngine`. Dient als Einstiegspunkt für den Isolation-Runner (`EngineRunnerImpl`). Stellt den `EngineContract` bereit.
- **Input**: `CanonicalAnalysisInput` (enthält `rawText`/`metadata["url"]`, `freeQuery`).
- **Output**: `DomainSummary`.
- **Dateipfad**: `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionEngine.kt`
- **Begründung**: Eigenständige Klasse gemäß bestehender Architektur (`WebpageAnalysisEngine`, `Top3KeyPointsEngine`).

### 2.2 `LocationQuestionCoordinator`
- **Verantwortung**: Technische Orchestrierung des Ablaufs. Steuert URL-Auflösung, Aufruf von `PlacesApiService`, Abruf von `LocationContext`, Wikipedia und Wikivoyage via Coroutines, Steuerung des Gemini Gateway und Mappings.
- **Input**: `CanonicalAnalysisInput`, `useSearchGrounding: Boolean`.
- **Output**: `DomainSummary`.
- **Dateipfad**: `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionCoordinator.kt`
- **Begründung**: Isoliert die asynchrone Multi-Source-Datenbeschaffung sauber von der Engine und der allgemeinen UseCase-Logik.

### 2.3 `LocationQuestionPlanner`
- **Verantwortung**: Reine, abfolge- und netzwerkfreie Fachlogik. Analysiert die freie Nutzerfrage, klassifiziert sie in eine `QuestionCategory`, bestimmt die benötigten Quellen (`SourceSelection`) und entscheidet deterministisch über Google Search Grounding.
- **Input**: `userQuestion: String`, `locationName: String`.
- **Output**: `ExecutionPlan` (enthält `category`, `requiredSources`, `optionalSources`, `requiresGrounding`).
- **Dateipfad**: `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionPlanner.kt`
- **Begründung**: Eigenständige, 100% unit-testbare Klasse ohne Android- oder Netzwerkabhängigkeiten.

---

## 3. Vollständiger Datenfluss

1. **Eingabe & Frage-Dialog**: Nutzer gibt Google-Maps-URL ein, wählt "Frage zum Ort", gibt spezifische Frage ein.
2. **ViewModel & UseCase**: `MainViewModel.fetchSummary` erzeugt `CanonicalAnalysisInput` mit `analysisType = GOOGLE_MAPS_LOCATION_QUERY` und `freeQuery = "..."`.
3. **Engine-Auflösung**: `AnalyzeContentUseCase` löst über `AnalysisRegistryImpl` die `LocationQuestionEngine` auf und übergibt diese an `EngineRunnerImpl`.
4. **Location Resolution**: `LocationQuestionCoordinator` löst die Google-Maps-URL über `GoogleMapsUrlParser` und `PlacesApiService` zu `GooglePlacesPoCResult` auf.
5. **Question Planning**: `LocationQuestionPlanner.planExecution(freeQuery, placeName)` analysiert den Fragetext und liefert den `ExecutionPlan`.
6. **Multi-Source Data Fetching (asynchron)**:
   - Places API Details & Reviews (im `GooglePlacesPoCResult` enthalten).
   - `GoogleMapsLocationContextService` (falls vom Plan gefordert).
   - `WikipediaContextSource` & `WikivoyageContextSource` (parallel via `async`, falls im Plan).
7. **Grounding-Entscheidung**: `ExecutionPlan.requiresGrounding` bestimmt, ob an Gemini `useSearchGrounding = true` übergeben wird.
8. **Prompt-Zusammenstellung**: Aufbereitung des Prompt-Kontexts mit strukturierten Ortsdaten, Review-Snippets, Kontextquellen und der Nutzerfrage für `F_GOOGLE_MAPS_LOCATION_QUERY.md`.
9. **Gemini Execution & Mappings**: Aufruf von `GeminiGateway`. Mappen des Responses auf ein valides `DomainSummary` (A1ContractValidator-kompatibel).
10. **Verlauf & Anzeige**: Speicherung im Room-Verlauf und Darstellung auf dem Standard-Ergebnis-Screen.

---

## 4. Verifizierte verfügbare Google-Maps- & Review-Daten

Die Analyse der bestehenden `PlacesApiService.kt` und `PlacesDataMapper.kt` zeigt exakt folgende verfügbare Datenfelder:

- **Place Details (`PlaceDetailsResponse`)**:
  - `id`, `name`, `formattedAddress`, `shortFormattedAddress`, `addressComponents`, `location` (Breiten-/Längengrad)
  - `types` (Kategorien wie `restaurant`, `museum`, etc.)
  - `displayName`, `rating` (z.B. 4.6), `userRatingCount` (z.B. 1240)
  - `editorialSummary` (Offizielle Kurzbeschreibung)
  - `priceLevel` (`PRICE_LEVEL_FREE`, `PRICE_LEVEL_INEXPENSIVE`, `PRICE_LEVEL_MODERATE`, `PRICE_LEVEL_EXPENSIVE`, `PRICE_LEVEL_VERY_EXPENSIVE`)
  - `websiteUri`
  - `regularOpeningHours` (`weekdayDescriptions` als String-Liste)
  - `reviews` (Liste von `Review`-Objekten)
- **Reviews (`Review`)**:
  - `authorAttribution.displayName`
  - `rating` (1.0 bis 5.0)
  - `text` / `originalText` (Bewertungstext)
  - `relativePublishTimeDescription` (z.B. "vor 2 Wochen")
  - `publishTime`
  - *Anzahl*: Google Places API v1 liefert maximal **5 Reviews** pro Place Details Anfrage.
- **Popular Times / Stoßzeiten-Diagramme**:
  - Technisch in den Standard-Places-API-v1-Feldern **nicht direkt enthalten**. Auswertung von Stoßzeiten erfolgt primär über `editorialSummary`, `reviews` (Nutzerberichte) und `regularOpeningHours`.

---

## 5. Question Planning & Source Selection Rules

### 5.1 Kategorien (`QuestionCategory`)
- `STOSSZEITEN`: Fragen zu Auslastung, Ruhezeiten, Wartezeiten.
- `ZUGANG_MOBILITAET`: Anfahrt, ÖPNV, Fußwege, Steigungen, Eingang.
- `BARRIEREFREIHEIT`: Rollstuhl, Aufzug, ebenerdiger Zugang, Behinderten-WC.
- `PARKEN`: Parkplätze, Parkhaus, Gebühren, Wohnmobil-Stellplätze.
- `ATMOSPHAERE_AUSSTATTUNG`: Lautstärke, WLAN, Steckdosen, Hunde, Stimmung.
- `FAMILIEN_KINDER`: Kinderwagen, Spielbereich, Kinderfreundlichkeit.
- `HISTORIE_KULTUR`: Geschichte, Baujahr, Architektur, historische Bedeutung.
- `PREISE_OEFFNUNGSZEITEN`: Eintrittspreise, Tickets, Feiertagsöffnungszeiten.
- `SAISON_EVENTS`: Saisonalität, Ausstellungen, Sonderveranstaltungen.
- `SONSTIGE`: Allgemeine oder nicht spezifisch zuordenbare Fragen.

### 5.2 Source Selection Matrix

| Kategorie | Places API | Reviews | Location Context | Wikipedia | Wikivoyage | Search Grounding |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| `STOSSZEITEN` | Ja | Ja | Ja | Nein | Nein | Optional |
| `ZUGANG_MOBILITAET` | Ja | Ja | Ja | Nein | Ja | Nein |
| `BARRIEREFREIHEIT` | Ja | Ja | Ja | Nein | Nein | Nein |
| `PARKEN` | Ja | Ja | Ja | Nein | Ja | Nein |
| `ATMOSPHAERE_AUSSTATTUNG` | Ja | Ja | Nein | Nein | Nein | Nein |
| `FAMILIEN_KINDER` | Ja | Ja | Ja | Nein | Nein | Nein |
| `HISTORIE_KULTUR` | Ja | Nein | Ja | **Ja** | **Ja** | Nein |
| `PREISE_OEFFNUNGSZEITEN` | Ja | Ja | Nein | Nein | Nein | **Ja** |
| `SAISON_EVENTS` | Ja | Ja | Nein | Nein | Nein | **Ja** |
| `SONSTIGE` | Ja | Ja | Ja | Optional | Optional | Optional |

---

## 6. Grounding-Regeln

Deterministiche Steuerung in `LocationQuestionPlanner`:

1. **Grounding = TRUE**:
   - Die Kategorie ist `PREISE_OEFFNUNGSZEITEN` oder `SAISON_EVENTS`.
   - Der Fragetext enthält explizite Zeitbegriffe: `"heute"`, `"aktuell"`, `"jetzt"`, `"2026"`, `"Sonderausstellung"`, `"Feiertag"`, `"Eintritt"`, `"Ticket"`.
2. **Grounding = FALSE**:
   - Statische Sachfragen wie `BARRIEREFREIHEIT`, `HISTORIE_KULTUR`, `PARKEN`, `ZUGANG_MOBILITAET`.
3. **Verhalten bei Grounding-Ausfall**:
   - Schlägt die Anfrage mit `useSearchGrounding = true` fehl (Timeout oder API-Fehler), wird automatisch ein Fallback-Aufruf ohne Grounding ausgeführt.
   - In `keyTakeaways[2]` (Datenlage) wird vermerkt: *"Echtzeitsuche nicht verfügbar, Auswertung basiert auf bestehenden Orts- und Bewertungsdaten."*

---

## 7. Prompt-Konzept (`F_GOOGLE_MAPS_LOCATION_QUERY.md`)

- **Pfad**: `/app/applet/app/src/main/assets/prompts/F_GOOGLE_MAPS_LOCATION_QUERY.md`
- **Input-Parameter**:
  - `{{LOCATION_NAME}}`: Name des Ortes.
  - `{{USER_QUESTION}}`: Exakte Nutzerfrage.
  - `{{QUESTION_CATEGORY}}`: Ermittelte Fragekategorie.
  - `{{PLACES_DATA}}`: Formatierte Stammdaten der Places API (Adresse, Rating, Öffnungszeiten, PriceLevel, Description).
  - `{{REVIEWS_DATA}}`: Aufbereite Review-Texte (max. 5 Reviews).
  - `{{CONTEXT_DATA}}`: Aggregierte Texte aus Wikipedia / Wikivoyage / LocationContext.
- **Output Requirements**: Strict JSON Format entsprechend `A1ContractValidator` mit genau 3 `keyTakeaways`.

---

## 8. Output-Mapping auf DomainSummary

Das Ergebnis wird strikt in ein A1-Validator-konformes `DomainSummary` übersetzt:

- `title`: Name der Location (z. B. *"Museum für Naturkunde, Berlin"*)
- `owner`: Primäre Kategorie / Ortstyp (z. B. *"Museum / Sehenswürdigkeit"*)
- `originalUrl`: Eingegebene Google-Maps-URL
- `shortDescription`: Direkte, klare Antwort auf die Nutzerfrage (1–3 Sätze)
- `keyTakeaways`: Exakt drei strukturierte Punkte:
  1. **Begründung / Detailantwort**: Ausführliche Fakten zur Frage.
  2. **Evidenzhinweise**: Quellenbelege (z.B. *"Basiert auf 5 Nutzerbewertungen und Places-Stammdaten"*).
  3. **Datenlage & Unsicherheit**: Transparente Angabe von Vollständigkeit, Konsistenz oder verbleibenden Unsicherheiten.
- `displayMode`: `DisplayMode.STANDARD`

---

## 9. Vollständiger Dateiplan

### 9.1 Neue Produktionsdateien (4)
1. `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionEngine.kt` — Engine-Implementierung.
2. `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionCoordinator.kt` — Orchestrierung & Multi-Source Fetching.
3. `/app/applet/app/src/main/java/com/example/data/engine/location/LocationQuestionPlanner.kt` — Pure Domain Logic (Planner/Category/Grounding).
4. `/app/applet/app/src/main/assets/prompts/F_GOOGLE_MAPS_LOCATION_QUERY.md` — Prompt Template.

### 9.2 Geänderte Produktions-/Asset-Dateien (5)
1. `/app/applet/app/src/main/java/com/example/data/AnalysisType.kt` — Hinzufügen des Enum-Werts `GOOGLE_MAPS_LOCATION_QUERY`.
2. `/app/applet/app/src/main/java/com/example/ui/metadata/FeatureCatalog.kt` — Registrierung im FeatureCatalog.
3. `/app/applet/app/src/main/java/com/example/data/engine/AnalysisRegistryImpl.kt` — Mapping & Engine-Registrierung.
4. `/app/applet/app/src/main/assets/prompts/function_registry.json` — JSON-Funktionsregistrierung.
5. `/app/applet/app/src/main/assets/prompts/prompt_manifest.json` — Prompt-Manifest-Zuordnung.

### 9.3 Neue Testdateien (1)
1. `/app/applet/app/src/test/java/com/example/LocationQuestionPlannerTest.kt` — Unit-Tests für Frageklassifizierung und Grounding-Steuerung.

### 9.4 Ausdrücklich unveränderte Kernkomponenten
- `/app/applet/app/src/main/java/com/example/domain/usecase/AnalyzeContentUseCase.kt`
- `/app/applet/app/src/main/java/com/example/data/PlacesApiService.kt`
- `/app/applet/app/src/main/java/com/example/data/GoogleMapsUrlParser.kt`
- `/app/applet/app/src/main/java/com/example/domain/model/DomainSummary.kt`
- `/app/applet/app/src/main/java/com/example/MainActivity.kt` (unterstützt `freeQuery` und `analysisType` bereits vollständig)

---

## 10. Konkrete Testmatrix (31 Testfälle)

| ID | Testfall-Beschreibung | Erwartetes Ergebnis |
| :--- | :--- | :--- |
| TC-01 | Standard Google-Maps-Link | URL wird aufgelöst, Place Details geladen |
| TC-02 | Shortlink (`maps.app.goo.gl/...`) | Shortlink wird expandiert und Place aufgelöst |
| TC-03 | Ungültige URL | Fehlermeldung "Ungültige URL" wird zurückgegeben |
| TC-04 | Location nicht auflösbar | Graceful Error Handling, Fehlermeldung im UI |
| TC-05 | Ortsfremde/unpassende Frage | Engine erkennt fehlende Evidenz, Hinweis in Takeaway 3 |
| TC-06 | Leere Frage | Validierung schlägt fehl, Prompt zur Fragen-Eingabe |
| TC-07 | Frage zu Stoßzeiten | Kategorie `STOSSZEITEN`, Reviews & Places geladen |
| TC-08 | Frage zu Zugang/Anfahrt | Kategorie `ZUGANG_MOBILITAET`, LocationContext & Wikivoyage |
| TC-09 | Frage zu Barrierefreiheit | Kategorie `BARRIEREFREIHEIT`, Grounding `FALSE` |
| TC-10 | Frage zu Parkmöglichkeiten | Kategorie `PARKEN`, Reviews & Places ausgewertet |
| TC-11 | Frage zu Atmosphäre | Kategorie `ATMOSPHAERE_AUSSTATTUNG`, Reviews priorisiert |
| TC-12 | Frage zu Historie/Geschichte | Kategorie `HISTORIE_KULTUR`, Wikipedia & Wikivoyage geladen |
| TC-13 | Frage zu Öffnungszeiten | Kategorie `PREISE_OEFFNUNGSZEITEN`, Grounding `TRUE` |
| TC-14 | Frage zu saisonalen Sperrungen | Kategorie `SAISON_EVENTS`, Grounding `TRUE` |
| TC-15 | Grounding explizit aktiviert | `useSearchGrounding = true` an Gemini übergeben |
| TC-16 | Grounding deaktiviert | Direct Model Call ohne Search Grounding |
| TC-17 | Grounding-Ausfall | Fallback-Anfrage ohne Grounding, Warnung in Takeaway 3 |
| TC-18 | Keine Reviews verfügbar (0 Reviews) | Analyse basiert rein auf Places API & Context |
| TC-19 | Wikipedia nicht verfügbar (404) | Ausweichen auf Wikivoyage & Places Daten |
| TC-20 | Wikivoyage nicht verfügbar | Ausweichen auf Wikipedia & Places Daten |
| TC-21 | Ausfall einzelner Kontextquelle | Gesamtprozess läuft erfolgreich ohne Abbruch weiter |
| TC-22 | Schwache Evidenz in Daten | Takeaway 3 weist explizit auf geringe Evidenz hin |
| TC-23 | Widersprüchliche Reviews | Takeaway 3 nennt Widersprüche in Bewertungen |
| TC-24 | Keine Evidenz vorhanden | Klare Rückmeldung über unzureichende Datenbasis |
| TC-25 | API Rate Limit (Places API) | Retry-Mechanismus oder Fehlermeldung |
| TC-26 | API Timeout | Abbruch nach Timeout-Interval, PipelineReport-Eintrag |
| TC-27 | Contract Validation (DomainSummary) | Output entspricht A1ContractValidator Schema |
| TC-28 | Verlaufs-Speicherung | Ergebnis in Room-Datenbank gespeichert |
| TC-29 | Copy-PR-Nachweis | Relevante IDs in Registry & Catalog registriert |
| TC-30 | Smartphone End-to-End Test | Kompletter Durchlauf von Eingabe bis Result-Screen |
| TC-31 | Code-Isolation / No Regression | `FREE_SOURCE_QUERY` & `GOOGLE_MAPS_ANALYZER` unberührt |

---

## 11. Risiken & Minderung

- **Performance bei Multi-Source Fetching**:
  *Minderung*: Sämtliche unabhängige Datenquellen werden über Kotlin Coroutines (`async`) parallel abgerufen.
- **Context-Size bei vielen Quellen**:
  *Minderung*: Reviews werden strikt auf die vorhandenen Max-5 beschränkt und geglättet.
- **Widersprüchliche Fakten**:
  *Minderung*: Prompt zwingt Gemini zur transparenten Angabe von Unsicherheiten in `keyTakeaways[2]`.

---

## 12. Empfohlene Implementierungsreihenfolge

1. **Phase 1 (Anpassung Konfig/Enums)**: Hinzufügen von `AnalysisType.GOOGLE_MAPS_LOCATION_QUERY`, `FeatureCatalog`, `function_registry.json`, `prompt_manifest.json`.
2. **Phase 2 (Domain/Planner)**: Implementierung von `LocationQuestionPlanner.kt` und `LocationQuestionPlannerTest.kt`.
3. **Phase 3 (Coordinator/Engine)**: Implementierung von `LocationQuestionCoordinator.kt` und `LocationQuestionEngine.kt`.
4. **Phase 4 (Prompt Asset)**: Erstellung der Prompt-Datei `F_GOOGLE_MAPS_LOCATION_QUERY.md`.
5. **Phase 5 (Registry)**: Registrierung in `AnalysisRegistryImpl.kt`.
6. **Phase 6 (Verification & Build)**: Ausführen von `compile_applet` und `gradle :app:testDebugUnitTest`.

---

## 13. Verbleibende offene Fragen
- Keine technischen oder architektonischen Fragen offen. Alle Komponenten, Interfaces und Datenstrukturen sind eindeutig spezifiziert und einsatzbereit.

---

## 14. Abschlussstatus

**DRY RUN FINAL PASS**
