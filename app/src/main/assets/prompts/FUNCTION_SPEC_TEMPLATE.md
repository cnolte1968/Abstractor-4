# FUNCTION_SPEC_TEMPLATE – SPEZIFIKATIONSVORLAGE FÜR NEUE GAA-FUNKTIONEN

Dieses Dokument dient als verbindliche Spezifikationsvorlage für die Definition, Implementierung und Härtung neuer Analysefunktionen im **Global Analytical Engine (GAA)**-Framework. 

Jede neue Funktion muss dieses Template vollständig ausfüllen und archivieren (z. B. unter `docs_md/specs/F_[FUNKTIONS_NAME].md`), bevor Code- oder Promptänderungen vorgenommen werden dürfen. Das Template ist so strukturiert, dass es direkt von Softwareentwicklern oder als System-Prompt für autonome **AI-Coding-Agents / Custom GPTs** interpretiert und umgesetzt werden kann.

---

## METADATEN DER FUNKTION

| Feld | Spezifikation | Beschreibung / Beispiel |
|---|---|---|
| **functionId** | | Eindeutige ID (z.B. `C.2`, `F.3`) |
| **Funktionsname** | | Aussagekräftiger Name (z.B. "Marktanalyse-Inkubator") |
| **Status** | `PROPOSED` | `PROPOSED` / `APPROVED` / `IMPLEMENTED` |
| **Ziel-Version** | `1.0.0` | SemVer-Format (z.B. `1.0.0`) |
| **Autor/Owner** | | Name oder Rolle des Erstellers |

---

## 1. FACHLICHER KONTEXT & NUTZUNGSKONTEXT

### 1.1 Nutzerproblem (User Pain Point)
*Beschreiben Sie präzise, vor welchem Problem der Anwender steht und warum diese Analysefunktion benötigt wird.*
* **Beispiel:** "Nutzer lesen komplexe Fachtexte, können aber schwer einschätzen, ob die darin enthaltenen Geschäftsideen marktüblich, umsetzbar oder wirtschaftlich tragfähig sind."

### 1.2 Ziel der Funktion (Goal)
*Welchen konkreten Mehrwert liefert die Auswertung? Was ist das übergeordnete Ziel des Prompts?*
* **Beispiel:** "Die Funktion identifiziert ungenutzte Ineffizienzen im Text und extrahiert bis zu drei konkrete, risikogeprüfte Geschäftskonzepte (SaaS/Nische) samt Werteversprechen."

### 1.3 Erlaubte Inputquellen (Allowed Input Sources)
*Welche Eingangskanäle sind für diese Funktion zulässig? (Zutreffendes markieren / ergänzen)*
* [ ] **WEB** (Standard-Webseiten, Blogs, Artikel)
* [ ] **DOCS** (Physische Dokumente, PDFs, DOCX, TXT)
* [ ] **MULTIMEDIA** (Bilder, Audios, Videos, Scans)
* [ ] **FREE_QUERY** (Freie Benutzerfragen auf Basis des Dokuments)

---

## 2. SYSTEM-KONFIGURATION & INTEGRATION

### 2.1 Engine-Typ
*Wie wird die Funktion technisch in der GAA-Architektur abgebildet?*
* [ ] **Standard-Web-Engine mit Custom Prompt:** (Wiederverwendung von `WebpageAnalysisEngine` mit neuem Prompt-Markdown in `AnalysisRegistryImpl.webFunctions`).
* [ ] **Spezialisierte Custom Engine:** (Erstellung einer eigenen Subklasse von `BaseGeminiEngine` für spezifisches Pre- oder Postprocessing, z. B. PDF-Extraktion).

**WICHTIGE ENGINE-REGELN (HARDENING):**
1. **Bestehende Engine bevorzugen:** Verwende immer eine bereits existierende Engine, wann immer möglich. Für alle standardmäßigen URL- und Webseitenanalyse-Funktionen ist die `WebpageAnalysisEngine` der absolute Standard und zwingend zu bevorzugen.
2. **Begründungspflicht für neue Engines:** Eine neue spezialisierte Custom Engine darf ausschließlich dann erstellt werden, wenn eine zwingende technische Notwendigkeit für spezifisches Preprocessing oder Postprocessing vorliegt. Die Erstellung einer neuen Engine darf niemals rein fachliche Bewertungslogiken als Grund haben und muss im Ausnahmeprotokoll (Schnittstelle zu Core-Preservation) detailliert begründet werden.

### 2.2 Prompt-Dateipfad
*Pfad zur System-Prompt-Datei im Assets-Verzeichnis:*
* `app/src/main/assets/prompts/F_[PROMPT_NAME_IN_GROSSBUCHSTABEN].md`

**WICHTIGE PROMPT-DATEI-REGELN (HARDENING):**
1. **Festgelegter Speicherort:** Alle System-Prompts müssen ausnahmslos im Verzeichnis `app/src/main/assets/prompts/` abgelegt werden.
2. **Dateiformat:** Die Prompts müssen im Markdown-Dateiformat vorliegen (Endung `.md`).
3. **Strenges Namensmuster:** Der Dateiname muss dem exakten Schema `F_[FUNKTIONSNAME_IN_GROSSBUCHSTABEN].md` folgen.
   * *Beispiele:* `F_EMPFEHLUNGS_VALIDATOR.md`, `F_AKTUALITAETS_CHECK.md`, `F_TOP_3_KERNAUSSAGEN.md`.

### 2.3 Registry-Anforderungen
*Wo und wie muss die Funktion registriert werden?*
* **AnalysisType Enum:** `com.example.data.AnalysisType.[ENUM_NAME]`
* **Registrierung:** Eintrag in `AnalysisRegistryImpl` mit korrekter Verknüpfung von `AnalysisType` -> `functionId`.

---

## 3. ARCHITECTURE IMPACT CHECK & CORE-PRESERVATION

Um die Stabilität der Anwendung zu sichern und ungeplante Nebeneffekte zu vermeiden, gilt die **No-Core-Change-Regel**: Neue Funktionen dürfen standardmäßig keine Core-Komponenten der Anwendung verändern. Eine neue Funktion ist modular zu integrieren.

### 3.1 Architecture Impact Checklist (Soll/Ist-Vergleich)
Jeder Entwurf muss die folgende Checkliste ausfüllen und mit **JA** oder **NEIN** beantworten:

| Frage / Core-Schnittstelle | Erwartet | Ist-Zustand (JA / NEIN) |
|---|---|---|
| **EngineRunner geändert?** | **NEIN** | |
| **AnalyzeContentUseCase geändert?** | **NEIN** | |
| **GeminiRepository geändert?** | **NEIN** | |
| **Parser-Grundlogik geändert?** | **NEIN** | |
| **DomainSummary geändert?** | **NEIN** | |
| **Bestehende Analyse-Funktionen geändert?** | **NEIN** | |
| **Änderung betrifft ausschließlich Prompt, Registry, Contract oder Test-Harness?** | **JA** | |

### 3.2 Ausnahme-Regelung (Core-Change-Exception)
Sollte die neue Funktion aus zwingenden Gründen eine Änderung an einer der oben genannten Core-Schnittstellen oder -Klassen erfordern, muss vor der Implementierung eine detaillierte Begründung vorgelegt und explizit freigegeben werden:

1. **Begründung (Justification):** Warum kann die Anforderung nicht über die vorhandene modulare GAA-Infrastruktur gelöst werden?
2. **Risikoanalyse (Risk Assessment):** Welche bestehenden Funktionen oder Abläufe könnten durch diesen Core-Eingriff beeinträchtigt werden?
3. **Alternative ohne Core-Change:** Welche alternativen Lösungswege existieren (z.B. Postprocessing im prompt/custom engine statt im zentralen Parser) und warum wurden sie verworfen?
4. **Explizite Freigabe erforderlich:** Ja, durch den Lead-Architekten bzw. Product Owner.

---

## 4. INPUT- & OUTPUT-SPEZIFIKATION (CONTRACT FIRST)

Die Funktion muss strikt den deklarierten GAA-Datentransfer-Kontrakt einhalten. Alle Felder der `DomainSummary` müssen gefüllt sein.

### 4.1 Erwarteter Input (`CanonicalAnalysisInput`)
* **rawText / enrichedText:** Darf nicht leer oder null sein.
* **metadata:** Welche Metadaten sind erforderlich? (z.B. `url`, `file_name`)
* **freeQuery:** Erforderlich, falls es sich um eine nutzergesteuerte Abfrage handelt.

### 4.2 Output-Mapping (`DomainSummary`)
Das vom LLM generierte strukturierte JSON-Dokument muss exakt auf die Felder der Kotlin-Klasse `DomainSummary` abgebildet werden.

**WICHTIGE DOMAINSUMMARY-FELDNAMEN-REGELN (HARDENING):**
1. **Verbindliche Feldnamen:** Im JSON und dem gemappten Kotlin-Datenmodell müssen zwingend und ausschließlich folgende vier Felder verwendet werden:
   * `title`
   * `original_url`
   * `short_description`
   * `key_takeaways` (welche strukturierte Objekte mit `title` und `details` enthalten)
2. **Strenges Verbot von Alternativnamen:** Die Verwendung von Alternativbegriffen oder Abweichungen (wie z.B. `summary`, `keyPoints`, `key_points`, `takeaways`, `bulletpoints`) ist strengstens untersagt!
3. **Ausführliche Felddefinitionen:**
   * **`title`**: Prägnanter, fachlicher Titel der Analyse (max. 6-8 Wörter). Muster: `"[Funktionsname] der Quelle: [Themenbereich]"`.
   * **`original_url`**: Die originale URL oder der Dateiname aus den Eingabemetadaten.
   * **`short_description`**: Eine ungeschönte, präzise und kontextabhängige Zusammenfassung der Gesamtanalyse (exakt 2-3 Sätze). Keine werblichen Floskeln.
   * **`key_takeaways`**: Liste strukturierter Objekte mit `title` (prägnanter Aspekt) und `details` (erklärende Einzelheiten).
     * *Einschränkung:* Reine Textausgaben in `title` und `details`. Formatierungen wie fettgedruckte Symbole (z.B. `**...**`) im LLM-Output sind verboten; das visuelle Layout ist Sache der UI-Schicht.
     * *Anzahl:* Mindestens 2, maximal 6 strukturierte Einträge.
     * **MANDATORY key_takeaways-Füllregel:** Das Feld `key_takeaways` darf bei einer erfolgreichen Analyse (`Success`) unter keinen Umständen leer gelassen werden. Liegt kein spezifischer fachlicher Befund oder kein negatives Phänomen vor, muss mindestens ein Takeaway-Objekt generiert werden, welches einen sachlich fundierten Null-Befund beschreibt (z.B. ein Takeaway mit `title: "Keine veralteten Empfehlungen"` und `details: "Die untersuchten Handlungsempfehlungen stützen sich auf stabile, zeitlose Grundprinzipien und weisen keine Anzeichen von Veraltung auf."`).

---

## 5. RELEVANZ-, ANTI-HALLUZINATIONS- & SICHERHEITSREGELN

Um erfundene Befunde, Over-Engineering und künstliche ("AI-Slop") Floskeln zu vermeiden, gelten folgende Härtungsvorschriften:

### 5.1 Relevanz- & Triggerschwellen
*Wann ist der Text für diese Analyse relevant? Ab wann gilt er als ungeeignet?*
* **Beispiel:** "Der Text muss mindestens einen konkreten Arbeitsablauf, ein geschäftliches Problem oder eine technische Lösung beschreiben. Philosophische Essays, Gedichte oder reine Navigationsmenüs werden abgelehnt."

### 5.2 Gegenproben-Logik (Crosscheck Logic)
*Wie verhält sich das LLM, wenn das erwartete Phänomen (z.B. Risiken, Fehler, Geschäftsideen) **nicht** im Quelltext existiert?*
* **Regel:** Erzwinge keine Befunde!
* **Verhalten bei Null-Befund:** Wenn im Text kein Befund vorhanden ist, deklariere dies sachlich und führe stattdessen die stabilisierenden und sicheren Faktoren des Systems als Key-Takeaway auf (z.B. `Betriebliche Stabilität: Das System ist abgesichert...`). Keine künstlichen Befunde oder scheinbaren Probleme hinzudichten.

### 5.3 No-Go-Regeln
*Explizite Verbote, um Halluzinationen und Fehlalarme zu blockieren:*
1. **Keine Erfindungen:** Es dürfen keine hypothetischen Risiken, falschen Manipulationsvorwürfe oder realitätsfernen Geschäftsideen hinzugedichtet werden, die nicht zweifelsfrei durch den Quelltext gestützt werden.
2. **Keine externen Fakten:** Es dürfen keine historischen Daten, Marktgrößen, Veröffentlichungsdaten oder Fakten eingebracht werden, die nicht im bereitgestellten Quelltext oder dessen Metadaten belegbar sind.
3. **Keine Code- / UI-Anweisungen (Prompt-Purity):** Der System-Prompt darf keinerlei Anweisungen zur UI-Darstellung, zur visuellen Formatierung, zu CSS-Klassen oder Layout-Strukturen enthalten. Die Prompt-Datei dient ausschließlich der fachlichen Datenaufbereitung und Inhaltsextraktion. Visuelle und gestalterische Entscheidungen sind einzig Aufgabe des Jetpack Compose Frontends.
4. **Keine überzogenen Aussagen oder Superlative:** Es ist verboten, im Prompt oder den resultierenden Analysen werbliche, übertriebene, absolute oder unbegründet reißerische Begriffe wie "risikofrei", "perfekt", "unbegrenzt", "vollständig gesichert", "absolut fehlerfrei" oder ähnliche Superlative zu verwenden. Alle Bewertungen und Formulierungen müssen stets sachlich, objektiv, nüchtern und differenziert ausgedrückt werden.

### 5.4 Fehlerverhalten, unvollständige Daten & `INSUFFICIENT_CONTENT`
*Wie reagiert das System auf ungeeignete oder lückenhafte Eingabedaten und wie greift die Grounding-Policy?*

* **Grounding & Insufficient Content Policy (Unified Rules):**
  1. **Grounding ist Ergänzung, kein Quellen-Ersatz:** Google Search Grounding dient ausschließlich der zusätzlichen Absicherung, zeitlichen Verifizierung oder Recherche. Es darf niemals dazu verwendet werden, fehlenden oder extrem dünnen Primärquellen-Inhalt künstlich wegzuretuschieren.
  2. **Kategorie A (Reine Extraktions- / Zusammenfassungsfunktionen):**
     * *Beispiele:* `A.1 Standard-Webseite`, `A.2 Top 3 Kernaussagen`.
     * *Fehlerverhalten:* Wenn der bereitgestellte Webseiten-Quelltext unzureichend, leer oder zu kurz ist, muss das System zwingend mit `INSUFFICIENT_CONTENT` abbrechen. Die Generierung eines künstlichen Erfolgs ("Success" mit erfundenem Inhalt) oder die Verwendung von Grounding als Inhaltsersatz ist strengstens verboten.
  3. **Kategorie B (Recherche-, Prüf- & Fragefunktionen):**
     * *Beispiele:* `A.3 Freie Quellenanfrage`, `B.1 Aktualitätscheck`.
     * *Fehlerverhalten:* Diese Funktionen dürfen Google Search Grounding nutzen, sofern `supportsSearchGrounding = true` im `EngineContract` deklariert ist. Trotzdem muss jegliche Unsicherheit transparent benannt werden. Grounding darf niemals dazu führen, dass ein extern recherchierter Fakt fälschlicherweise als ein angeblicher Quellenbefund der analysierten Primärseite ausgegeben wird.

* **Fehlersignalisierung im Modell:**
  * Alle Pflichtfelder der Datenstruktur `DomainSummary` müssen strukturell befüllt sein, dürfen jedoch keine erfundenen Inhalte enthalten.
  * **INSUFFICIENT_CONTENT:** Wenn der bereitgestellte Text zu kurz ist (z. B. weniger als 150 Zeichen), oder keinen thematischen Bezug zur Analysefunktion aufweist:
    * Das Feld `short_description` muss transparent darauf hinweisen und die Bewertung explizit auf `INSUFFICIENT_CONTENT` setzen.
    * In `key_takeaways` ist sachlich zu begründen, warum der Text nicht ausreichte.
  * **Unbestimmbarkeit:** Wenn eine Bewertung mangels ausreichender Daten nicht eindeutig vorgenommen werden kann:
    * Deklariere das Ergebnis im entsprechenden Feld (z.B. in der `short_description` oder im `details`-Feld der Key-Takeaways) explizit als `"nicht eindeutig bestimmbar"` und benenne die Datenlücken transparent, anstatt zu spekulieren.

---

## 6. TEST-SPEZIFIKATION

Jede Spezifikation erfordert ein definiertes Test-Szenario für JVM-basierte Regressionstests (`ExampleRobolectricTest.kt`).

### 6.1 Positivtest (Szenario mit eindeutigem Befund)
* **Test-Input:** (Ein repräsentativer Textabschnitt, der den Befund erzwingt)
  * *Beispiel:* "Wir betreiben unsere Server ohne Backups und Verschlüsselung im Keller."
* **Erwartetes Ergebnis:** (Welche konkreten Aussagen müssen im Output enthalten sein?)
  * *Beispiel:* `title` enthält "Risikoprofil", `key_takeaways` enthält ein Objekt mit dem Titel "Datenverlustrisiko" (oder ähnlich strukturiertem Begriff).

### 6.2 Negativtest (Szenario ohne Befund / Null-Befund)
* **Test-Input:** (Ein neutraler Textabschnitt, der keinen Befund liefert)
  * *Beispiel:* "Unser System ist durch eine dreifach redundante Stromversorgung und 24/7-Überwachung abgesichert."
* **Erwartetes Ergebnis:** (Wie greift die Gegenproben-Logik?)
  * *Beispiel:* Keine erfundenen Risiken. `short_description` hebt die hohe Stabilität hervor. Ein Key-Takeaway deklariert sachlich "Betriebliche Stabilität" (oder ähnlich strukturiert).

### 6.3 INSUFFICIENT_CONTENT-Test (Szenario mit unzureichendem Input)
* **Test-Input:** "Hallo Welt."
* **Erwartetes Ergebnis:**
  * `short_description` oder Bewertung verweist explizit auf "INSUFFICIENT_CONTENT" oder signalisiert unzureichenden Inhalt.

---

## 7. UI-HINWEISE & USER EXPERIENCE (INTEGRATIONSHINWEISE)

*WICHTIG: Die nachfolgenden Hinweise dienen ausschließlich der Frontend-Integration in der Kotlin/Compose-App. Sie dürfen NICHT in die Prompt-Datei kopiert werden! Funktionsprompts müssen frei von UI- oder Layoutanweisungen bleiben.*

* **Touch-Targets:** Alle interaktiven Elemente (z.B. Starten der Analyse, Filtern nach dieser Funktion) müssen eine Mindestgröße von **48dp x 48dp** aufweisen.
* **Ladezustand:** Während der Ausführung der Engine muss ein eindeutiger Ladeindikator (z.B. CircularProgressIndicator oder Shimmer-Effekt) mit dem korrekten Funktionstitel angezeigt werden.
* **Fehleranzeige:** Tritt ein Kontraktbruch auf, darf die App nicht abstürzen, sondern muss eine verständliche Fehlermeldung mit Wiederholungsoption (Retry-Button) ausgeben.

---

## 8. AKZEPTANZKRITERIEN (DEFINITION OF DONE)

Eine Funktion gilt erst dann als fertiggestellt und freigegeben, wenn folgende Kriterien erfüllt sind:

- [ ] **Architecture Impact Check im grünen Bereich:** Der Architecture Impact Check wurde durchgeführt, alle Kontrollfragen entsprechen den Zielwerten ("NEIN" für Core-Änderungen, "JA" für modulare Erweiterungen) oder eine begründete Ausnahmegenehmigung liegt vor.
- [ ] **Prompt-Purity:** Der Prompt liegt als reine Markdown-Datei in `prompts/` vor, enthält keine Parser- oder JSON-Templates und ist gänzlich frei von UI-Layoutanweisungen.
- [ ] **Contract Compliance:** Die Custom- oder Web-Engine deklariert einen validen `EngineContract` mit SemVer-Version (z.B. `1.0.0`) und korrekten Schemata.
- [ ] **Registry-Eintrag:** Die Funktion ist sauber in der `AnalysisRegistryImpl` kartografiert. Keine Verteilungslogik außerhalb der Registry.
- [ ] **Zero-Regression-Prüfung:** Alle vorhandenen Analysefunktionen wurden durch die Änderung nicht beeinträchtigt.
- [ ] **Harness-Testlauf:** Sowohl der Positivtest als auch der Negativtest wurden erfolgreich im lokalen JVM-Test-Harness (`ExampleRobolectricTest`) ausgeführt.
- [ ] **Umgang mit Abwesenheit von Befunden:** Im Negativtest-Lauf wurden keine fiktiven Daten oder Scheinergebnisse erzeugt; nicht bestimmbare Daten wurden korrekt deklariert.
- [ ] **Kompilierbarkeit:** `compile_applet` läuft fehlerfrei durch. Alle Unit-Tests sind erfolgreich.
