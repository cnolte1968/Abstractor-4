# CHANGE-PROMPT: CP-01

## Document Metadata

- Document Version: 2.3
- Status: ACTIVE
- Created: 2026-07-15
- Last Modified: 2026-07-15
- Change Process: GOVERNANCE
- Change ID: CP-01-GOV-002

---

Dieses Protokoll ist bei **jeder** inhaltlichen, sprachlichen oder qualitativen Optimierung eines bestehenden Funktions-Prompts in Relevantor zwingend anzuwenden.

CP-01 folgt den übergeordneten Governance-Regeln aus CP_GUIDELINE.md.
Bei Konflikten gilt CP_GUIDELINE als übergeordnete Prozessdefinition.

---

## 1. VERSIONS- UND DATEIMANAGEMENT (HARDENED)

Zur Vermeidung von Inkonsistenzen und zum Schutz des Produktionssystems gilt für dieses Dokument und alle weiteren Prozess-Richtlinien ein striktes Dateimanagement:

1. **Dateinamens-Standard:**
   * Change-Prompts tragen keine Versionsnummer im Dateinamen.
   * Versionierung erfolgt ausschließlich innerhalb des Dokument-Headers.
   * Beispiele:
     * `CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md`
     * `CP-02_VERAENDERUNG_ARBEITSWEISE.md`
2. **Archivierung & Unveränderlichkeit:**
   * Ältere Versionen bleiben als unveränderte historische Dokumente im Repository archiviert, um die Nachvollziehbarkeit früherer Prozessschritte zu sichern.
   * Jede Überarbeitung oder Härtung einer Prozessrichtlinie wird zwingend unter dem gleichen Dateinamen, jedoch mit hochgezählter Versionsnummer im internen Dokumenten-Header durchgeführt.

---

## 1a. PROMPT-METADATEN UND INTERNE VERSIONIERUNG (MANDATORY)

Jede durch CP-1 optimierte Funktions-Prompt-Datei muss einen Prompt-Metadatenblock enthalten. Die technische Dateibenennung der Funktions-Prompt-Datei bleibt unverändert (keine Versionierung im Dateinamen, keine Umbenennung bestehender Prompt-Dateien).

### Pflichtfelder des Metadatenblocks:
- Function Key
- Prompt Version
- Status
- Created Datum
- Last Modified Datum
- Change Process
- Change ID
- Previous Version

### Standard-Format
Der verpflichtende Header einer Funktions-Prompt-Datei lautet:

```markdown
# SYSTEM-PROMPT: [FUNCTION_KEY]

## Prompt Metadata

- Function Key: [FUNCTION_KEY]
- Prompt Version: [MAJOR.MINOR]
- Status: PROD-LOCKED / DRAFT
- Created: YYYY-MM-DD
- Last Modified: YYYY-MM-DD
- Change Process: CP-01
- Change ID: CP-01-[DATUM]-[FUNCTION]
- Previous Version: [VERSION]
```

### Regeln für zukünftige CP-1 Änderungen
Bei jeder CP-1 Prompt-Änderung muss GAIS zwingend prüfen:
1. Existiert ein Metadatenblock?
2. Ist die Prompt-Version erhöht?
3. Ist das Änderungsdatum aktuell?
4. Ist die Change-ID vorhanden?
5. Ist die Vorgängerversion dokumentiert?
6. Ist der Prompt-Hash im Änderungsbericht dokumentiert?

---

## 2. ROLLENMODELL UND SYSTEMGRENZEN (CHATGPT vs. GAIS)

Dieses System basiert auf einer strikten Arbeitsteilung zwischen zwei getrennten, unabhängigen Systemen. Es ist verboten zu suggerieren, dass ChatGPT und GAIS eine gemeinsame Schnittstelle oder ein integriertes Gesamtsystem bilden.

1. **ChatGPT (Der Dialog-Partner):**
   * ChatGPT ist das primäre Interface für den menschlichen Benutzer.
   * ChatGPT führt den Benutzer intelligent durch den fachlichen Dialog und wendet die Kriterien dieses Change-Prompts an.
   * ChatGPT fragt fachliche Lücken ab und konsolidiert den fachlichen Wunsch.
   * Am Ende des Dialogs erzeugt ChatGPT eine strukturierte, maschinenlesbare Handlungsanweisung, die als **Global Analytical Action (GAA)** formuliert wird.
   
2. **GAIS (Der Ausführende Code-Agent):**
   * GAIS (Google AI Studio Coding Agent) ist ein reiner Code- und Build-Agent, der im Projekt-Workspace arbeitet.
   * GAIS führt keinen fachlichen CP-Erhebungsdialog. GAIS bearbeitet den von ChatGPT erzeugten GAA und kann nur technische Rückfragen oder Ausführungsberichte im GAIS-Kontext liefern.
   * GAIS darf vom Benutzer keine technischen Vorkenntnisse über die Codebasis verlangen. Alle technischen Pflichtfelder (SHA-Hashes, Kotlin-Dateipfade, Parser-Klassen) muss GAIS im Trockenlauf eigenständig über Code-Analyse (Selbstermittlung) herausfinden.
   
3. **Der Benutzer (Der Brückenbauer):**
   * Der Benutzer kopiert die fachlichen Anforderungen zu ChatGPT.
   * Nach der Generierung durch ChatGPT kopiert der Benutzer den resultierenden GAA in den GAIS-Arbeitsbereich.
   * Nach der Ausführung durch GAIS kopiert der Benutzer die technischen Rückmeldungen, Testergebnisse oder den Copy PR zurück zu ChatGPT zur kritischen Gegenkontrolle.

---

## 3. CHANGE-GRENZEN UND ABGRENZUNG (DECISION TREE)

Um zu verhindern, dass komplexe strukturelle Änderungen unzulässig unter dem vereinfachten CP-01-Prozess durchgeführt werden, sind die Systemgrenzen wie folgt definiert:

### A. Was darf CP-01 ändern? (Inklusion)
* **Ausschließlicher Geltungsbereich:** Inhaltliche, sprachliche, qualitative, stilistische oder übersetzungsspezifische Optimierung bestehender Markdown-Dateien im Verzeichnis `/app/src/main/assets/prompts/` (z. B. `F_WEITERE_RELEVANTE_ASPEKTE.md`).
* **Erlaubte Test-Anpassungen:** Minimale Korrekturen in Unit-Test-Assertions (z. B. Aktualisierung von erwarteten String-Ausgaben in JUnit-Tests), sofern diese unmittelbar durch die Textänderung fehlschlagen würden.

### B. Wann muss auf andere Change-Prompts verwiesen werden? (Exklusion)
* **CP-02 (Veränderung der Arbeitsweise einer Funktion):** Sobald sich die logischen Verarbeitungsschritte oder die Daten-Sequenzierung innerhalb der Gemini-Engine verändern (z. B. veränderte Token-Grenzwerte, sequenzielle Prompt-Verkettung), das JSON-Ausgabeschema jedoch identisch bleibt.
* **CP-03 (Neuanlage einer Funktion):** Sobald eine komplett neue Analysefunktion in das System integriert werden soll (erfordert Prompt-Neuanlage, Registrierungs-Anpassungen und Menü-Erweiterung).
* **CP-04 (Löschung einer Funktion):** Sobald eine bestehende Funktion rückstandsfrei aus Registries, Menüs und dem Code gelöscht werden soll.
* **CP-05 (Veränderung des Verarbeitungs-Fensters):** Bei Anpassungen an der HTML-Bereinigung, Netzwerk-DNS-Preflights, Tokenizer-Logiken oder Token-Limits.
* **CP-06 (Veränderung am Ausgabe-Fenster):** Sobald ein JSON-Ausgabeschema (die JSON-Struktur oder Feldtypen), ein Contract (z. B. `A1Contract`), ein zugeordneter Parser oder ein Validator verändert wird.
* **CP-07 (Änderung an CD / UI / UX):** Bei Änderungen an den Compose-Views, Menüstrukturen, Farbthemen, Button-Aktionen oder Dialogen.

### C. Wann ist ein übergeordneter Architektur-Change erforderlich?
* Sobald systemische, übergreifende Infrastrukturen modifiziert werden, die nicht durch einzelne Funktions-CPs abgedeckt sind.
* Dazu gehören:
  * Änderungen an der Engine-Basisklasse (`BaseGeminiEngine`) oder der abstrakten Verarbeitungslogik.
  * Änderungen an der zentralen Registry-Logik (`AnalysisRegistry`) oder dem Pipeline-Bericht-Speicher (`PipelineReportStore`).
  * Änderungen an den Gradle-Konfigurationsdateien (`build.gradle.kts`, `libs.versions.toml`).
  * Aktualisierung oder Hinzufügen von externen Programmbibliotheken (z. B. neue Versionen des Gemini-SDKs, Ktor, Room).

---

## 4. CHATGPT-DIALOGPFLICHTEN UND FACHLICHES ERHEBUNGSVERFAHREN

Bevor ChatGPT einen GAA für GAIS formulieren darf, muss es folgende **13 fachliche und analytische Säulen** lückenlos im Dialog mit dem Benutzer erheben und definieren:

### 1. Funktionsidentität
* **Name & ID:** Welcher Analyse-Dienst (gemäß `function_registry.json`) soll optimiert werden?
* **Zweck & Abgrenzung:** Was ist die Kernaufgabe dieser Funktion und wie grenzt sie sich trennscharf von benachbarten Funktionen ab (z. B. Abgrenzung von "Top 3 Kernaussagen" zu "Weitere relevante Aspekte")?

### 2. Nutzerproblem
* **Friction Points:** Welches konkrete Problem erlebt der Anwender aktuell bei der Nutzung der Funktion (z. B. ungenaue Antworten, zu viel Fülltext, falsche sprachliche Gewichtung)?
* **Ist-Defizit:** Warum ist der bisherige Output fachlich unvollständig, unpräzise oder ungenügend?

### 3. Nutzer-Nutzen & Zielsetzung
* **Value Addition:** Welchen konkreten analytischen Mehrwert erhält der Anwender durch die optimierte Funktion?
* **Erkenntnisgewinn:** Welche spezifische Entscheidung oder welches Verständnis wird durch die Verbesserung maßgeblich erleichtert?

### 4. KI-Rolle & Fachpersona
* **Expertenrolle:** Welche präzise Fach-Persona muss das Modell einnehmen (z. B. *Research Analyst*, *Fachexperte*, *Risikoanalyst*, *Innovationsberater*, *Faktenprüfer*)?
* **Persona-Struktur:** Definition des professionellen Hintergrunds, des analytischen Bewertungsverfahrens und des Fachvokabulars der Persona.

### 5. Input-Spezifikation
* **Datenquellen:** Welche genauen Eingabedaten erhält der Prompt zur Verarbeitung (z. B. extrahierte URL-Texte, hochgeladene PDF-Dokumente, Meeting-Transkripte, benutzerdefinierte Einzelfragen oder Metadaten wie Zeitstempel und Quelle)?

### 6. Verfügbare und fehlende Daten (Datengrenzen)
* **Verfügbare Daten:** Welche Datenfelder sind garantiert vorhanden?
* **Fehlende Daten:** Welche Daten stehen dem Prompt ausdrücklich *nicht* zur Verfügung (z. B. Live-Internetzugriff im Moment der Modellausführung, historische Archive oder geschützte Datenbanken)? Welche Annahmen darf das Modell keinesfalls treffen?

### 7. Analyse-Methode
* **Verarbeitungsschritte:** Welche logische Abfolge von fachlichen Prüfschritten muss das Modell intern durchlaufen (z. B. erst Strukturieren, dann Gewichten, dann Validieren)?
* **Analyseverfahren:** Welche präzise Methodik (z. B. induktive/deduktive Textanalyse, logische Konsistenzprüfung) soll angewendet werden?

### 8. Relevanzlogik
* **Filterregeln:** Nach welchen Kriterien bestimmt das Modell, ob eine Information wichtig oder unwichtig ist?
* **Priorisierungsregeln:** In welcher Reihenfolge oder Gewichtung müssen Erkenntnisse im Prompt bewertet werden?

### 9. Verwerfungsregeln
* **Filterung von Rauschen:** Welche Informationen müssen explizit ignoriert und verworfen werden (z. B. werbliche Slogans, Selbstdarstellungen, redundante Einleitungsfloskeln, irrelevante Metadaten)?

### 10. Output-Spezifikation
* **Strukturvorgaben:** Welches Format (z. B. Markdown mit präzisen Headings, Bulletpoints) wird gefordert?
* **Feldspezifikation:** Genaue Festlegung und semantische Bedeutung der einzelnen Textblöcke.
* **Längenbeschränkungen:** Genaue Regeln zur Textdichte (z. B. *"maximal 3 Sätze pro Punkt"*, *"höchstens 150 Wörter insgesamt"*).
* **Verbotene Ausgaben:** Ausschluss von Floskeln, Meta-Diskussionen (z. B. *"Als KI-Modell..."*), oder Legacy-Kategorisierungen.

### 11. Nicht-Ziele & Sicherheitsanker
* **Halluzinationsschutz:** Strikte Anweisung, dass keine externen Fakten hinzuerfunden werden dürfen.
* **Auslegungsverbote:** Keine Spekulationen über nicht belegbare Absichten des Autors.
* **Keine unberechtigte Quellenkritik:** Der Prompt darf die Qualität der Quelle nur bewerten, wenn dies explizit Teil der Funktionsidentität ist (z. B. beim "Fehlinformations-Radar"), ansonsten ist die Quelle als gegeben zu analysieren.

### 12. Beispiele (Gute vs. Schlechte Ausgaben)
* **Positiv-Beispiel (Best Practice):** Ein konkreter Beispiel-Output, der Stil, Dichte und Format perfekt trifft.
* **Negativ-Beispiel (Bad Practice):** Ein realistischer Negativ-Output mit Erläuterung, warum dieser das Qualitätsziel verfehlt.

### 13. Akzeptanz- und Abnahmekriterien
* **Messbare Schwellenwerte:** Wann gilt die Optimierung als fachlich erfolgreich abgeschlossen (z. B. vollständige Abdeckung der Quell-Perspektiven, exakte Einhaltung der Längenvorgaben)?

---

## 5. GAIS-SELBSTERMITTLUNG IM TROCKENLAUF

Wenn GAIS den Befehl für einen Trockenlauf (Phase 1) erhält, darf GAIS nicht erwarten, dass der Benutzer technische Details nennt. GAIS muss die Codebasis und die JSON-Ressourcen scannen und folgende **technische Parameter eigenständig ermitteln**:

### 14. Technische Selbsterkennung durch GAIS
* **Pfad zur Prompt-Datei:** Scan von `assets/prompts/` zur Identifizierung der betroffenen Markdown-Datei.
* **Kanonische ID (`functionId`):** Auflösung über die `function_registry.json`.
* **Kanonischer Typ (`analysisType` / `canonicalAnalysisType`):** Auflösung über die Kotlin-Enums in `AnalysisType.kt`.
* **Aktueller SHA-256-Hash:** Berechnung des SHA-256-Prüfwerts der Prompt-Datei als Baseline.
* **Zugehörige System-Komponenten:**
  * Zugeordneter Gemini-Engine-Klasse (z. B. `A1Engine` oder `BaseGeminiEngine` Implementierung).
  * Zugeordneter Parser (Deserializer).
  * Zugeordnete Contract-Klasse (z. B. `A1Contract`, `A2Contract`) und zugehöriger Validator.
* **Erwartetes JSON-Schema:** Auslesung der Strukturdefinition des Contracts, um das exakte Outputschema zu dokumentieren.
* **Relevante Test-Suites:** Ermittlung der JUnit/Robolectric-Tests, die diese Prompt- oder Validierungspfade abdecken.
* **Copy-PR-Prüfkriterien:** Festlegung, welche Felder im Pipeline-Report nach der Änderung auf PASS stehen müssen.
* **Sicherheitsausschlüsse:** Eindeutige Deklaration der Dateien, die bei diesem spezifischen Change keinesfalls angefasst werden dürfen.

---

## SPERR- UND ABBRUCHBEDINGUNGEN (MANDATORY CHECK)

Bevor eine Änderung durchgeführt wird, müssen ChatGPT und GAIS prüfen, ob der angeforderte Change die Grenzen von CP-01 überschreitet. 
**Trifft auch nur einer der in Abschnitt 3.B definierten Ausschlussgründe zu, MUSS die Bearbeitung unter CP-01 sofort abgebrochen und an den entsprechenden Change-Prompt verwiesen werden.**

---

## PHASE 1: TROCKENLAUF (DRY RUN)

GAIS darf vor Freigabe des Trockenlaufs **keinerlei Code- oder Prompt-Änderungen** vornehmen. In dieser Phase analysiert GAIS den Ist-Zustand durch Selbstermittlung und füllt das folgende Formular vollständig aus:

### 1. CP-01 Stammdatenblatt (Ermittelt durch GAIS)

* **Change-ID:** CP-01-[JJJJMMTT]-[KURZTITEL]
* **Change-Typ:** CP-01 v2.1 (Optimierung Funktions-Prompt)
* **Betroffene Funktion:** [Name der Funktion]
* **Betroffene Prompt-Datei:** /app/src/main/assets/prompts/[PROMPT_DATEI.md]
* **Kanonische ID / Registry-Keys:**
  * `functionId`: [Wert]
  * `analysisType` / `canonicalAnalysisType`: [Wert]
* **Aktuelle Baseline SHA-256 (Hash der Prompt-Datei):** [Selbst berechneter SHA-256-Hash]
* **Output-Vertrag (Contract-Klasse):** [Zugeordnete Klasse]
* **Zugehöriger Validator:** [Zugeordneter Validator]
* **Zugehöriger Parser:** [Zugeordneter Parser]

### 2. Fachliche Spezifikation (Übergeben von ChatGPT)

* **Funktionsidentität & Zweck:** [Definition]
* **Nutzerproblem & Friction Points:** [Fehlerbeschreibung]
* **Nutzer-Nutzen & Zielsetzung:** [Mehrwert]
* **KI-Rolle & Fachpersona:** [Rolle und Bewertungslogik]
* **Input-Spezifikation & Datengrenzen:** [Verfügbare vs. fehlende Daten]
* **Analyse-Methode & Relevanzlogik:** [Schritte, Priorisierung und Verwerfung]
* **Soll-Ausgabe (Format & Länge):** [Genaue Output-Spezifikation]
* **Nicht-Ziele & Sicherheitsanker:** [Grenzbereiche]
* **Gute vs. Schlechte Beispiele:** [Zusammenfassung der Referenz-Mocks]
* **Akzeptanzkriterien:** [Konkrete Abnahmekriterien]

### 3. Technische Risikoanalyse & Constraints (Ermittelt durch GAIS)

* **Erwartetes JSON-Schema:**
```json
{
  "key": "value"
}
```
* **Erhöhte Regressionsrisiken:** [Spezifische Risiken für den JSON-Parser]
* **Erlaubte Dateien für Änderungen:**
  * `/app/src/main/assets/prompts/[PROMPT_DATEI.md]`
  * Ggf. entsprechende Testdateien zur Anpassung von Erwartungswerten.
* **Verbotene Dateien für Änderungen:**
  * Alle Kotlin-Dateien in `src/main/java/` (insb. Engines, Extractor, Parser, Validatoren, Registries, UI, ViewModel)
  * `app/src/main/assets/prompts/function_registry.json`
  * `app/src/main/assets/prompts/prompt_manifest.json`
  * `app/src/main/assets/prompts/_global_quality_rules.md`
  * `app/src/main/java/com/example/data/PipelineReportStore.kt`
* **Sicherheitserklärung:** 
  * *"Hiermit bestätigt GAIS, dass die Änderung des Prompts die globalen Qualitätsregeln aus `_global_quality_rules.md` respektiert und keinerlei A/B/E- oder legacy-Notationen einführt."*

### 4. Vorgeschlagene Minimaländerung (Diff-Entwurf)

```diff
- alt
+ neu
```

---

## PHASE 2: FREIGEGEBENE UMSETZUNG

Nachdem der Benutzer das Trockenlauf-Ergebnis an ChatGPT übergeben hat und ChatGPT die Freigabe erteilt hat, führt GAIS die physische Umsetzung im Projekt aus.

### Strikte Arbeitsgrenzen der Umsetzung:

1. **Surgical Edits:** Führe die Änderung präzise und minimalinvasiv mithilfe des `edit_file`-Werkzeugs durch.
2. **Keine Seiteneffekte:** Modifiziere ausschließlich die freigegebene Prompt-Datei (und ggf. Test-Referenzwerte).
3. **Verbot von Code-Anpassungen:** Es ist strengstens untersagt, Parser-Logiken oder Validatoren an den neuen Prompt anzupassen. Der Prompt muss sich nach den bestehenden Validatoren richten, nicht umgekehrt.
4. **Kein Bypass:** Versuche nicht, Validierungskriterien durch Mock-Daten oder Deaktivierung von Tests zu umgehen.

---

## PHASE 3: VERIFIKATION & LAUFZEIT-CHECK

Nach dem Schreiben der geänderten Datei führt GAIS automatisch die folgenden Verifikationsschritte aus und dokumentiert die Ergebnisse:

### 1. Statische Verifikation
* **Kompilierung prüfen:** Führe `compile_applet` aus.
  * *Ergebnis:* [SUCCESS / FAIL]
* **Unit-Tests ausführen:** Führe `gradle :app:testDebugUnitTest` aus.
  * *Ergebnis:* [Anzahl Tests] passed / [Anzahl Tests] failed
  * *Kritischer Check:* Laufen alle Prompt- und Contract-Tests (z. B. `PipelineReportTest`, `ExampleUnitTest`) fehlerfrei durch?

### 2. Runtime Verifikation via Pipeline-Report (Copy PR)
Führe die geänderte Funktion in der Runtime aus (oder simuliere sie über ein Test-Harness) und verifiziere das JSON-Ergebnis des **Copy PR (Pipeline-Report)**:
* **Eingebettetes Feature Routing:** 
  * Entspricht `selectedFeatureTitle` dem neuen Wert?
  * Ist `selectedFeatureCategory` korrekt auf den professionellen Kategorienamen (und nicht die Legacy-Notation) gemappt?
* **Engine Routing:**
  * Wurde die korrekte Gemini-Engine aufgerufen?
  * Startet der Schritt `engine_routing` vor `prompt_loading`?
  * Sind `decision` und `nextStep` korrekt befüllt?
* **Validator-Status:**
  * Wurde der Output erfolgreich vom Contract-Validator validiert (`status == "PASS"`)?
  * Gab es Warnungen oder bereinigte Fehler im Report?

---

## FREIGABE-ENTSCHEIDUNG / ROLLBACK-EMPFEHLUNG

GAIS legt dem Benutzer die Ergebnisse der Phase 3 in einem übersichtlichen Abschlussbericht vor, welchen der Benutzer zur finalen Bewertung an ChatGPT kopiert:

* **Zusammenfassung der Änderung:** [Kurze, sachliche Beschreibung]
* **Kompilierungsstatus:** 🟢 GRÜN (erfolgreich)
* **Teststatus:** 🟢 GRÜN (alle Tests erfolgreich)
* **Laufzeitstatus (Copy PR):** 🟢 PASS (alle Pipeline-Schritte erfolgreich validiert)
* **Neuer SHA-256 Hash des Prompts:** [Neuer SHA-256]

*Bei Abweichungen, Fehlern oder unerwarteten Regressionen im Pipeline-Report empfiehlt ChatGPT zwingend einen Rollback auf den Baseline-SHA-Hash, und GAIS führt diesen umgehend aus.*
