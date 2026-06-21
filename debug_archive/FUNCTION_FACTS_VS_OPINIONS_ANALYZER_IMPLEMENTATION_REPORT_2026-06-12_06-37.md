# Umsetzungsbericht: Fakten-vs.-Meinungen-Analysator

Dieses Dokument dokumentiert die erfolgreiche Implementierung der neuen Analysefunktion **„Fakten-vs.-Meinungen-Analysator“** (technischer Bezeichner: `FACTS_VS_OPINIONS_ANALYZER`, UI-Label: „Fakt oder Meinung!?“).

---

## 1. Analyse der Ausgangslage & Dokumentation der Root Cause

Der Abstractor wurde bisher für Standard-Zusammenfassungen, 3 Kernpunkte, Aktualitätsprüfung, Fehlinformationsanalyse, Risiko-Identifizierung, Business-Aspekte und Multimedia-/Dokumentenverarbeitung genutzt.

### Herausforderung bei der Einbindung
*   **Keine Beeinträchtigung bestehender Features:** Andere kritische Pfade (wie die robusten Datenbereinigungen, der URL-Extractor und die 3-Kernpunkte-Validierungen) mussten vollständig stabil bleiben.
*   **Strict Extraction Directive:** Die Klassifizierung durfte keinesfalls auf reinem Weltwissen des Large Language Models (LLM) oder auf Vermutungen über die URL basieren. Sie musste stattdessen streng an realen, auslesbaren Seiteninhalt gebunden sein.
*   **Robustes JSON-Parsing:** Das existierende Deserialisierungs-Schema via Moshi und das Fallback-Parsing über Regex mussten ohne Schema-Erweiterungen die neue strukturierte Kategoriesierungs-Ausgabe aufnehmen können.

---

## 2. Detaillierte Liste aller geänderten Code-Stellen

Die Modifikationen wurden strikt isoliert und hochgradig modular in den bestehenden Strukturen integriert.

### A. `/app/src/main/java/com/example/data/GeminiModels.kt`
*   **Änderung:** Hinzufügen des neuen Enums `FACTS_VS_OPINIONS_ANALYZER` in die Aufzählungsklasse `AnalysisType`.
*   **Zweck:** Bereitstellung des neuen Typ-Glieds für die ViewModel-Statussteuerung und den API-Router im Repository.

### B. `/app/src/main/java/com/example/data/GeminiNetwork.kt`
*   **System Prompt Routing (`rawBaseSystemInstruction`):** Integration des neuen Promptblocks für `AnalysisType.FACTS_VS_OPINIONS_ANALYZER`. Dieser konfiguriert die unbestechlichen Faktenchecker-Instruktionen, Legenden-Formate und strikten Klassifikationsmuster.
*   **Temperatur-Steuerung (`temp`):** Zuweisung einer sehr niedrigen Temperatur (`0.1`) für den neuen Analysetyp, um maximale Konsistenz und deterministische Klassifikationen sicherzustellen.
*   **Robuste Deserialisierung (`parseSummaryRobustly`):** Aktivierung von `keepNumbering = true` für den `FACTS_VS_OPINIONS_ANALYZER`-Lauf, um sicherzustellen, dass die Legenden-Kürzel und eckigen Klammern `[F]`, `[M]` etc. unter keinen Umständen versehentlich vom Regex-Sanitizer gereinigt werden.

### C. `/app/src/main/java/com/example/ui/MainViewModel.kt`
*   **Eingangskontrolle für YouTube (`fetchSummary` - YouTube-Ast):** Durchführung einer `hasEnoughRealContent`-Prüfung für das geladene Transkript. Ist kein auslesbares Transkript extrahierbar (z.B. wegen Videolänge oder KI-Sperre), wechselt die App sofort in den dafür vorgesehenen, ehrlichen Fehler-Diagnosezustand.
*   **Eingangskontrolle für manuelle Zwischenablagen (`fetchSummary` - directContent-Ast):** Validierung, dass der vom Benutzer manuell eingegebene Text ausreichend Substanz (mindestens 500 Zeichen) besitzt.
*   **Eingangskontrolle für Webseiten (`fetchSummary` - Webseiten-Ast):** Durchführung der Standard-Sondierung via `WebpageExtractor`. Schlägt dies fehl, dient das Google Search Grounding als zweites Werkzeug. Liefert auch dieses kein verwertbares Dokument zurück, wird umgehend der Fehlerzustand aufgerufen, statt blind Halluzinationen auf Basis der URL zu erzeugen.

### D. `/app/src/main/java/com/example/MainActivity.kt`
*   **UI-Header Routing (`getTitleForAnalysisType`):** Zuweisung des dynamic UI-Titels `"Fakt oder Meinung!?"` für den Typ `FACTS_VS_OPINIONS_ANALYZER`.
*   **Inhaltsüberschrift (`takeawaysHeaderTitle`):** Ausgabe der Überschrift `"FAKT ODER MEINUNG!?"` im Detailbereich der Arbeitsergebnisse.
*   **Cockpit-Button (`CockpitLayout`):** Integration der neuen Optionam Ende der Grid-Liste:
    *   **Label:** „Fakt oder Meinung!?“
    *   **Beschreibung:** „Analysiert zentrale Aussagen einer Quelle und markiert, ob sie eher Fakt, Meinung, Vermutung, Werbung oder Spekulation sind.“
    *   **Emoji:** `⚖️` (Wägeschale für neutrale Faktenanalyse)
    *   **Theme-Color:** `Color(0xFF0EA5E9)` (Modernes, klares Sky Blue)

---

## 3. Beschreibung der realisierten Prompts und Parameter

Der System-Prompt im `GeminiRepository` wurde sehr filigran auf Deutsch abgestimmt:

### Parameter-Setup
*   **Temperatur:** `0.1` (Maximale Genauigkeit und Systemkonformität).
*   **Search Grounding:** Dynamisch gesteuert über `useSearchGrounding` bei Webseiten, aber absolut unterbunden, falls direkter Text übergeben wurde, um API-Rauchsignale oder Quotenüberschreitungen zu minimieren.

### Kern-Klassifikationsregeln im Prompt
1.  **Fakt `[F]`:** Neutrale, überprüfbare Informationen aus der Quelle selbst.
2.  **Meinung `[M]`:** Subjektive Bewertungen, persönliche Einschätzungen oder Interpretationen des Urhebers.
3.  **Vermutung `[V]`:** Unsichere, andeutende oder unvollständig belegte Statements.
4.  **Werbung `[W]`:** Selbstpromotive, vertriebliche oder marketinglastige Textteile.
5.  **Spekulation `[S]`:** Mutmaßungen über Zukunftsentwicklungen oder Kausalität ohne Textbelege.

---

## 4. Nachweis des Fehler- und Diagnose-Setups

Falls eine Webseite ungeeignet ist (überhaupt keinen Text aufweist, z.B. nur Grafikbanner oder Anmeldebarrieren besitzt), greifen unsere neuen Sicherheitsprüfungen in `MainViewModel`.

Es wird in diesem Fall **keine** fachliche Vermutungs-Analyse erzeugt. Stattdessen wird im Detail-Kartenlayout der vordefinierte ehrliche Diagnosezustand gerendert:

*   **Titel:** `Inhalt nicht auswertbar`
*   **Kurzbeschreibung:** `Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.`
*   **Bulletpoints (Kernaussagen):**
    *   *Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle.*
    *   *Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt.*
    *   *Bitte prüfe die URL oder versuche eine andere Quelle.*

---

## 5. Rückwirkungsfreiheit auf den Bestand

*   **Keine Schemaänderung:** Das bestehende Datenmodell `AbstractorSummary` wurde unmodifiziert weiterverwendet. Dadurch waren keine Datenbankmigrationen im Room-Layer oder Änderungen im JSON-Parser erforderlich.
*   **Abgrenzung:** Alle anderen enums (`STANDARD_WEBSEITE`, `TOP_3_KERNAUSSAGEN` etc.) nutzen weiterhin unverändert ihre individuellen Temperaturen, Instruktionsblöcke und Standard-Regex-Reinigungen.
*   **Kombinierter Buildtest:** Ein vollständiger, erfolgreicher Testbuild des App-Projekts dokumentiert die Integrität der gesamten Kotlin- und Jetpack Compose Code-Basis.
