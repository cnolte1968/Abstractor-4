# TECHNISCHE SYSTEMSPEZIFIKATION & ARCHITEKTUR-ANALYSE (ABSTRACTOR-CORE)

Dieses Dokument bietet eine lückenlose, faktenbasierte und tiefgehende technische Spezifikation des bestehenden Abstractor-Systems. Es dient als exaktes Referenzdokument für künftige Entwicklungsphasen (insb. Entkopplung von Prompt Engine, Parser-Logik und UI-Rendermodulen).

---

## 1. Systemübersicht
Die **Abstractor-App** ist eine hoch-optimierte Android-Anwendung, die strukturierte und unstrukturierte Quellmedien (Webseiten, YouTube-Transkripte, lokale Dokumente oder Freitexte) erfasst, mithilfe der Google Gemini API analysiert und in eine visuell strukturierte, mobiloptimierte Listenansicht (Jetpack Compose) überführt.

Die funktionale Trennung im System ist wie folgt aufgebaut:
* **Assets / Prompts:** Deklarative Markdown- und JSON-Dateien im Asset-Verzeichnis zur Steuerung der sprachlichen und strukturellen Erwartungshaltung gegenüber der Gemini-Schnittstelle.
* **Prompt Engine:** Orchestriert und fusioniert globale Qualitätsrichtlinien mit funktionsspezifischen Analyseregeln zu einem zusammenhängenden System-Instruction-Set.
* **Gemini Schnittstelle & Netzwerk:** Verwaltet Authentifizierung, Netzverbindung, Search Grounding und asynchrone API-Anfragen.
* **Parser (SummaryResponseParser):** Transformiert unstrukturierte oder teil-strukturierte LLM-Textoutputs mittels mehrstufiger Parser-Trichter (JSON, Regex, Zeilen-Fallback) in das einheitliche Datenmodell `AbstractorSummary`.
* **UI Layer (MainActivity / MainViewModel):** Konsumiert das geparste Ergebnis über reaktive State-Streams (`StateFlow`), analysiert die Struktur der Takeaway-Zeilen für grafische Aufbereitung (Bold-Prefix Splitting) und rendert ansprechend gestaltete Material-Design-3-Cards.

---

## 2. Datenfluss (End-to-End)

Vom Benutzereingriff bis zum vollendeten Bildaufbau auf dem Bildschirm durchläuft das System folgende Phasen:

1. **Input-Erfassung & Normalisierung (`MainViewModel`):**
   * Empfang eines geteilten Textes/Links (z. B. via Share Intent) oder manuelle URL-Eingabe.
   * Bereinigung und Extraktion der Primär-URL via URL-Regex.
   * Auflösung von Linkverkürzern (Redirects wie `t.co`, `lnkd.in`) auf einem IO-Thread via `WebpageExtractor.resolveUrl`.

2. **Szenarien-Verzweigung (Inhalts-Besorgung):**
   * *Szenario YouTube:* Extrahiert Video-ID. Versucht, das offizielle oder automatische Transkript via `YoutubeTranscriptHelper` lokal zu laden. Schlägt dies fehl, werden die Oembed-Metadaten geladen.
   * *Szenario Dokument:* Extrahiert Text direkt aus Office-Formaten (.docx, .xlsx, .pptx) oder sendet Binärdaten (PDF/Bilder) direkt per Multimodal-Upload an Gemini.
   * *Szenario Standard-Webseite:* Versucht lokales Scraping via `WebpageExtractor.fetchContent`. Schlägt dies fehl oder ist die Seite leer, wird Google Search Grounding zugeschaltet.
   * *Szenario Social Media/Geschützte Seiten:* Diagnostiziert Login-Hürden und erzeugt einen intelligenten lokalen Prompt-Kontext zur Anleitung des Benutzers für Copy-and-Paste.

3. **Prompt-Orchestrator (`PromptEngine`):**
   * Zuordnung des `AnalysisType` (z. B. `FEHLINFORMATIONS_RADAR`) zum passenden Markdown-Asset via `PromptLoader`.
   * Laden der systemweiten `_global_quality_rules.md`.
   * Fusionierung: `Global_Rules` + `Separating_Marker` + `Function_Prompt` = Finale System-Instruction.

4. **Kognitive Verrechnung (Gemini API):**
   * Sendung der System-Instruction, des extrahierten Inhalts (oder der Bild-/PDF-Dateibytes) und optionaler Parameter (wie Search Grounding) an die Gemini-Schnittstelle.
   * Gemini liefert einen Textstream zurück, der im Idealfall ein reines JSON-Dokument ohne Markdown-Fencing beheimatet.

5. **Sanierung & Typisierung (`SummaryResponseParser`):**
   * Sanierung von beschädigten JSON-Zeichenketten (z. B. Behebung überflüssiger trailing commas, Extraktion des inneren `{...}`-Bereichs).
   * Automatische Korrektur von camelCase (z. B. `keyTakeaways`) zu snake_case-Vorgaben (`key_takeaways`).
   * Deserialisierung via Moshi Parser in `AbstractorSummary`.
   * *Fallback:* Versagt Moshi, tritt ein hochauflösender Regex- und Line-by-Line-Extraktionstrichter in Kraft, der das Datenmodell eigenständig rekonstruiert.

6. **UI-Schlagwortanalyse & Rendering (`MainActivity`):**
   * Das `MainViewModel` schaltet auf `UiState.Success`, um die UI neu zu komponieren.
   * Die Compose `LazyColumn` iteriert über das Takeaways-Feld im Summary-Objekt.
   * Jede Takeaway-Zeile wird via `parseTakeaway` analysiert. Ist das geforderte `**Schlagwort:**`-Muster vorhanden, wird das Schlagwort fett formatiert und separat von den restlichen Details gerendert.
   * Dynamische Zuordnung nummerierter Badges (bei Zahlen-Modi wie `TOP_3_KERNAUSSAGEN`) oder farblich akzentuierter Icons basierend auf dem Item-Index.

---

## 3. Komponentenanalyse

### 3.1 PromptEngine (`PromptEngine.kt`)
* **Verantwortlichkeiten:** 
  Als zentraler Orchestrator koordiniert dieses Singleton das Zusammenspiel der systemweiten Qualitätsstandards mit den anwendungsspezifischen Aufgabenstellungen.
* **Input-Parameter:** 
  * `Context?` (erforderlich für den Zugriff auf den Android AssetManager)
  * `analysisType: AnalysisType` (Einsteller für die gewünschte Analyse)
* **Output-Struktur:** Ein zusammenhängender UTF-8 codierter String zur direkten Übergabe als System-Instruction an das Gemini Model.
* **Abhängigkeiten:** 
  * Greift auf `PromptLoader` zum asynchronen/synchronen Parsen der lokalen Assets zu.
  * Greift auf `PromptFallbackProvider` zu, um bei Ladefehlern ungestörte Hardcoded-Prompts zu servieren.

### 3.2 Parser (`SummaryResponseParser.kt`)
* **Parsing-Logik:** Der Parser arbeitet hochgradig defensiv über drei hintereinandergeschaltete Erkennungsebenen:
  1. *Ebene 1 (Moshi):* Behebt syntaktische Sonderzeichen (Trailing Commas, Key-Variationen) und versucht die Typkonvertierung.
  2. *Ebene 2 (Feld-Regex):* Suche nach `"key"`-Mustern mit flexibler Berücksichtigung deutscher Feldbezeichner (z. B. `"titel"`, `"autor"`).
  3. *Ebene 3 (Line-Scraping):* Zerlegt das Dokument in Code-Zeilen und identifiziert Bulletpoint- oder Aufzählungszeilen direkt über Textmarker (`-`, `•`, `*`, `1. `).
* **Erwartetes Format:** 
  Ein valides JSON, das sich an das Schema der `_global_quality_rules.md` hält (title, original_url, short_description, key_takeaways, owner).
* **Failure Handling:** Es werden niemals Exceptions nach außen geworfen. Bei totaler Formatzerstörung wird ein valides `AbstractorSummary`-Objekt mit informativen Fehlertexten oder vordefinierten Hinweisen an die UI-Pipeline zurückgegeben.

### 3.3 UI-Schlagzeilen-Extraktion (`MainActivity.kt` & `MainViewModel.kt`)
* **Datenannahme:** 
  `MainViewModel` übermittelt den Zustand per `StateFlow<UiState>`. Ein Erfolg enthält das analysierte `AbstractorSummary` sowie den aktiven `AnalysisType`.
* **Rendering-Verfahren:**
  Rendert den Header-Bereich (Titel, URL, Plattform-Emoji, Owner) und baut anschließend für die Takeaways dynamische Material-Design-Cards.
* **Herausforderung im UI-Code:**
  Das Splitting des Takeaway-Textes in eine Titel- und eine Beschreibungszeile erfolgt ad-hoc direkt während des Renderprozesses mittels `parseTakeaway(text)`.
* **Abhängigkeiten:** 
  Enge Kopplung mit der Format-Vorgabe aus `_global_quality_rules.md` (Regex-Erwartung von `**Fettgedrucktem Begriff:**`).

---

## 4. Kritische Abhängigkeiten & Kopplungsschwachstellen

Bei der genauen Analyse des Quellcodes fallen erhebliche Kopplungsschwachstellen zwischen Prompt-Output, Parsing-Logik und Benutzeroberfläche auf:

1. **Die Koppelung des Schlagwort-Musters:**
   * *Problem:* Das UI erwartet über `parseTakeaway` zwingend, dass Takeaways im Text die Syntax `**Begriff**: Erläuterung` einhalten.
   * *Abhängigkeit:* Weicht das Modell ab (z. B. schreibt `Begriff: Erläuterung` ohne Sternchen oder verwendet Semikolons), greift das Regex ins Leere. Als Konsequenz blendet das UI vor jeder Info-Card den statischen Default-Titel `"Erkenntnis"` ein, was den Nutzwert und das Schriftbild abwertet.

2. **Verteilte Stringbereinigung:**
   * *Problem:* Die Bereinigung von Strings findet an unübersichtlichen Stellen im Projekt statt. `SummaryResponseParser` führt spezifische Whitespace-Filter und Regex-Ersetzungen für Sonderzeichen aus, während `parseTakeaway` im UI-Code verbleibende Markdown-Reste (wie `*`) entfernt. 
   * *Abhängigkeit:* Änderungen an Format-Konventionen erfordern Code-Eingriffe in vollkommen unterschiedlichen Software-Schichten (Datenverarbeitung sowie Präsentationsschicht).

3. **Individuelles Sonder-Parsing für einzelne Modi im Universal-Parser:**
   * *Problem:* In `SummaryResponseParser.kt` existieren harte Verzweigungen für spezifische Enum-Varianten:
     ```kotlin
     if (analysisType == AnalysisType.FACTS_VS_OPINIONS_ANALYZER) {
         summary.keyTakeaways.map { cleanFactsVsOpinionsTakeaway(it) }
     }
     ```
   * *Abhängigkeit:* Der "Universal"-Parser ist nicht generisch. Er muss über jeden neuen Analysemodus Bescheid wissen und schleift modusspezifische Bereinigungslogik direkt im Parse-Hauptstrang mit.

4. **Identitätskopplung (Copy-to-Clipboard):**
   * *Problem:* Die Clipboard-Funktion `buildPlainTextShareOrCopyText` in `MainActivity.kt` baut die Struktur des Plaintexts für das Teilen manuell Zeile für Zeile nach, indem sie die hierarchischen Felder von `AbstractorSummary` neu formatiert.
   * *Abhängigkeit:* Wenn sich der XML/Compose-Aufbau der UI-Cards ändert, läuft der kopierte Plaintext aus der Zwischenablage asynchron zur tatsächlichen Bildschirm-Präsentation.

---

## 5. Risikoanalyse

| Bedrohungsszenario | Verhaltensweise & Auswirkung des Systems | Risikoklasse |
| :--- | :--- | :---: |
| **Änderung des JSON-Schemas** (z.B. Feldumbenennung im Prompt) | Der Moshi JSON-Deserialisierer schlägt sofort fehl. Das System fängt das asynchron ab; das Regex-Fallback ermittelt noch title/description, aber die Takeaways bleiben leer und weichen auf Standard-Placeholder ab. | **HOCH** |
| **Verzicht auf Bold-Prefix im Prompt** (z. B. einfache Bulletpoints vom LLM) | Der Parser verarbeitet die Zeilen fehlerfrei. In der App-UI bricht das Layout jedoch optisch ein: Über allen Cards wird der redundante Titel `"Erkenntnis"` angezeigt. | **MITTEL** |
| **Verschachtelter Markdown-Output** (z. B. Einrückungen bei Aufzählungen) | Moshi parst den String inklusive Zeilenumbrüchen `\n` und Tabulatoren. Die Compose-UI bricht die Zeilen hässlich um, zerschießt die Zeilenabstände innerhalb der M3-Karten und ignoriert die flache Design-Richtlinie. | **HOCH** |
| **Silent Failures im Regex-Extraktor** | Liefert das LLM völlig unstrukturierten Freitext, meldet die App keinen Absturz, sondern präsentiert dem Benutzer statische Sätze ("Inhalt erfolgreich strukturiert") mit leeren Inhaltsflächen. | **MITTEL** |

---

## 6. Technische Empfehlungen für künftige Refactorings (Zukünftige Phase 3)

Um eine robuste Betriebskompatibilität zu gewährleisten, sollten die nachfolgenden Entkopplungsschritte in der nächsten Systemarchitekturphase umgesetzt werden:

1. **Einführung eines geparsten Daten-Intermediats (UI-Model-Entkopplung):**
   * *Konzept:* Das Domänenmodell `AbstractorSummary` sollte keine rohen Markdown-Takeaways mehr enthalten. Stattdessen wird ein typisiertes Datenobjekt `TakeawayItem` eingeführt:
     ```kotlin
     data class TakeawayItem(val title: String, val detail: String)
     ```
   * *Vorteil:* Das Trennen (Splitting) des fettgedruckten Terminus vom Beschreibungstext erfolgt zwingend im Parser auf der Datenschicht. Die UI-Ebene zeichnet rein deklarativ und führt keinerlei String-Manipulationen oder Regex-Operationen mehr aus.

2. **Isolierung und Kapselung der Formatbereinigungen (Parser-Entkopplung):**
   * *Konzept:* Entwicklung dedizierter `Sanitizer`-Pakete. Spezialisierte Parser-Strategien registrieren sich für bestimmte `AnalysisTypes`. 
   * *Vorteil:* Der Haupt-Parser `SummaryResponseParser` bleibt schlank, rein generisch und frei von modusspezifischen String-Ersetzungen. Newcomer-Modi können angelegt werden, ohne den Core-Code anfassen zu müssen.

3. **Einführung strukturierter Plaintext-Exporter (Copy-Entkopplung):**
   * *Konzept:* Integration einer Schnittstelle `TextRepresentable`, welche das Datenmodell standardisiert zur Verfügung stellt:
     ```kotlin
     interface TextRepresentable { fun toFormattedPlaintext(): String }
     ```
   * *Vorteil:* Schutz des Kopierverhaltens vor Designänderungen des UI-Layout-Quellcodes.
