# GAA – CP-03 PHASE 4: LOCATION QUESTION COORDINATOR IMPLEMENTIERUNGSBERICHT

**Erstellt am:** 2026-08-03 02:48:00 UTC  
**Durchgeführt von:** GAIS  
**Projekt-Root:** `/app/applet`  
**App-Modul:** `/app/applet/app`  

---

## 1. Referenzen & Grundlagen

- **Master Dokument:** `/app/applet/docs_md/RELEVANTOR_WORKSPACE_MASTER_V1_2_2026-08-02_15-15-00_UTC.md`
- **Dry-Run SHA-256:** `50814c675857e51b70fb2c0846d4c76606d5239b9e774d197a22cc58544dd15a`

---

## 2. Zielsetzung der Phase 4

Implementierung des isolierten `LocationQuestionCoordinator`, der die fachliche Ablaufsteuerung und Aggregation für Fragen zu Standorten (`GOOGLE_MAPS_LOCATION_QUERY`) übernimmt:
- Entgegennahme von Location-Informationen, Nutzerfrage und `ExecutionPlan` vom `LocationQuestionPlanner`.
- Steuerung der Datenbeschaffung von Places API, Nutzer-Reviews, Location Context, Wikipedia und Wikivoyage.
- Ausfallsichere Aggregation aller verfügbaren Quellen in eine strukturierte Datenklasse `LocationQuestionAggregatedContext`.
- Vorgegebene Einschränkungen: Keine UI-Änderungen, keine Prompt-Dateien, keine neuen Engines, keine Registry-Erweiterungen.

---

## 3. Umgesetzte Dateien (Change Budget)

### Erstellte Produktionsdateien (1):
- `/app/applet/app/src/main/java/com/example/domain/engine/location/LocationQuestionCoordinator.kt`

### Erstellte Testdateien (1):
- `/app/applet/app/src/test/java/com/example/LocationQuestionCoordinatorTest.kt`

### Erstellte Dokumentationsdateien (1):
- `/app/applet/docs_md/CP03_PHASE4_COORDINATOR_2026-08-03_02-48-00_UTC.md`

### Modifizierte Produktionsdateien (0):
- Keine (Budget: 0)

---

## 4. Funktionsweise & Schnittstellen des Coordinators

### Data Class `LocationQuestionAggregatedContext`
Enthält alle aggregierten Ergebnisse und Metadaten:
- `userQuestion`: Nutzerfrage
- `locationName`: Ermittelter/bereinigter Ortsname
- `executionPlan`: Ausgeführter `ExecutionPlan` vom Planner
- `placesResult`: Optionale `GooglePlacesPoCResult` (Details, Rating, Opening Hours, Summary)
- `locationContextFormatted`: Formatierte Basisdaten des Location Context Service
- `wikipediaResult`: `ContextResult` der Wikipedia-Abfrage
- `wikivoyageResult`: `ContextResult` der Wikivoyage-Abfrage
- `reviews`: Extrahierter Katalog an Nutzer-Reviews
- `requiresGrounding`: Indikator für erforderliches Online-Grounding
- `sourcesStatus`: Map der Aufruf-Ergebnisse je `DataSourceType` (`true`/`false`)
- `formattedCombinedContext`: Einheitlicher Kontext-String zur Vorbereitung für die spätere Engine

### `LocationQuestionCoordinator`
Verfügt über Dependency Injection mit Default-Standardinstanzen und orchestriert die Datenbeschaffung:
1. **Ortsnamen- & Places-Auflösung**: Erkennt Google Maps URLs via `GoogleMapsUrlParser` oder nutzt vorparsierte `GooglePlacesPoCResult`-Instanzen.
2. **ExecutionPlan-Ermittlung**: Ruft den `LocationQuestionPlanner` auf, falls kein Plan übergeben wurde.
3. **Isolierte Quell-Abfragen**: Jeder Aufruf (Places API, Reviews, Location Context, Wikipedia, Wikivoyage) erfolgt in separaten `try-catch`-Blöcken.
4. **Ausfallsicherheit**: Einzelne fehlende oder fehlschlagende Quellen führen nicht zum Abbruch der Gesamtausführung.
5. **Kontext-Formatierung**: Fügt alle verfeinerten Abschnitte in `formattedCombinedContext` zusammen.

---

## 5. Testergebnisse (Unit Tests)

Alle 7 Testfälle in `LocationQuestionCoordinatorTest.kt` wurden erfolgreich auf der JVM ausgeführt (`BUILD SUCCESSFUL`):

1. `test1_AllSourcesAvailable_AggregatesCorrectly`: Prüft vollständige Aggregation aller Quellen.
2. `test2_SingleSourceMissing_HandlesGracefully`: Prüft Fehlverhalten einzelner Quellen ohne Absturz.
3. `test3_NoReviews_HandlesGracefully`: Prüft leere Review-Listen.
4. `test4_WikipediaError_DoesNotCrash`: Prüft Ausfall der Wikipedia-Quelle.
5. `test5_WikivoyageError_DoesNotCrash`: Prüft Ausfall der Wikivoyage-Quelle.
6. `test6_EmptyContextData_ReturnsBasicAggregatedResult`: Prüft Fallback bei unbekanntem Ort / fehlenden Kontextdaten.
7. `test7_CorrectAggregationFormatting`: Prüft Formatierung & Grounding-Notice.

---

## 6. Pre-Write & Path Verification Audit

- `pwd -P`: `/app/applet`
- Verzeichnis-Prüfung: Keine unzulässigen Pfad-Verschachtelungen (`/app/applet/app/applet` etc.).
- Keinstamp: `2026-08-03_02-48-00_UTC`
