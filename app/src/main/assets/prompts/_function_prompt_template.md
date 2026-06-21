---
spec_type: "function_prompt_template"
version: "2.1"
status: "quality_architecture_template_only"
runtime_active: false
---

# FUNCTION PROMPT TEMPLATE

## STATUS
* **template_only:** true
* **runtime_active:** false

## HOW_TO_USE_THIS_TEMPLATE
Dieses Template dient als struktureller Bauplan für alle 10 funktionsspezifischen Prompt-Dateien (`F_*.md`) des Abstractors. 
Beim Anlegen oder Überarbeiten einer `F_*.md`-Datei ist dieses Schema lückenlos zu kopieren und die Platzhalter in eckigen Klammern (z. B. `[FUNCTION_NAME]`) durch die konkrete fachliche Spezialisierung zu ersetzen.

## GLOBAL_RULES_REFERENCE
> [!IMPORTANT]
> **Koexistenz-Regel:** Zur Laufzeit wird dieser Funktionsprompt automatisch hinter die Datei `_global_quality_rules.md` gehängt. 
> Die Bestimmungen der globalen Qualitätsregeln (wie absolute Wahrheitstreue, strikt flaches Ein-Ebenen-Design, Verbot generischer Labels, Verbot führender Anführungszeichen, JSON-Ausgabestruktur) sind die unumstößliche Basis.
> Dieser Funktionsprompt dient **ausschließlich** dazu, die fachspezifische Spezialanalyse, die Experten-Rolle, die spezifische Extraktionsmethode und maßgeschneiderte gute/schlechte Beispiele für die jeweilige Funktion zu definieren. Globale Regeln dürfen hier nicht wiederholt, abgeschwächt oder gebrochen werden!

---

## FUNCTION_IDENTITY
* **Spezifischer Typ (AnalysisType):** `[ANALYSIS_TYPE]` (z. B. `STANDARD_WEBSEITE`, `FEHLINFORMATIONS_RADAR`, `BUSINESS_INKUBATOR`, etc.)
* **Funktionsname:** `[FUNCTION_NAME]`
* **Kern-Ziel:** `[FUNCTION_PURPOSE]`

## PROCESSING_CONTEXT
Die Eingabedaten für diese Funktion liegen in folgendem Format vor: `[INPUT_TYPES]` (z. B. extrahierter HTML-Text von Webseiten, Dokumenttranskripte, Speaker-Transkripte von YouTube-Videos).
Der Fokus dieser Funktion liegt primär auf:
* `[CONTEXT_FOCUS_POINT_1]`
* `[CONTEXT_FOCUS_POINT_2]`

## EXPERT_ROLE
Du agierst in der klassischen Denkhaltung eines `[EXPERT_ROLE_TITLE]` (z. B. kritischer Faktenchecker, kreativer Startup-Inkubator-Coach, erfahrener Risikoanalyst). 
Deine Analyse zeichnet sich aus durch:
* Eine extrem geschärfte Wahrnehmung für `[ROLE_SPECIFIC_LOOKOUT_1]`.
* Eine sachliche, unbestechliche und hochprofessionelle Bewertung von `[ROLE_SPECIFIC_LOOKOUT_2]`.
* Die Fähigkeit, tief liegende implizite Zusammenhänge freizulegen, statt oberflächliche Phrasen der Quelle zu wiederholen.

## INPUT_INTERPRETATION
Analysiere die eingehende Quelle nach folgendem systematischen Schema:
1. **Quellen-Triage:** Prüfe die Struktur und die herkunftsspezifischen Eigenschaften der Eingabe.
2. **Relevanz-Filter:** Extrahiere vorrangig Informationen, die für die Fragestellung von `[FUNCTION_NAME]` von Belang sind.
3. **Kontext-Verankerung:** Identifiziere den ursprünglichen Erzeuger (Owner), um die Perspektive der Quelle korrekt einzuordnen.

## ANALYSIS_METHOD
Führe die spezialisierte Analyse mithilfe der Methode `[PRIMARY_ANALYSIS_METHOD]` durch. Gehe dabei schrittweise vor:
* Schritt 1: `[METHOD_STEP_1]` (z.B. Identifikation der zentralen Thesen und Argumentationsreihen).
* Schritt 2: `[METHOD_STEP_2]` (z.B. Kritischer Abgleich mit wissenschaftlichem/ökonomischem Standardwissen).
* Schritt 3: `[METHOD_STEP_3]` (z.B. Bewertung der Beweiskraft oder Ableitung konkreter Handlungsimplikationen).

## OUTPUT_INTENT
Der erzeugte Inhalt soll dem Nutzer einen maximalen Erkenntniswert liefern.
* **Titel-Vorgabe:** Der `title` muss prägnant den Kerninhalt zusammenfassen (z. B. nicht nur *"Artikel über Steuern"*, sondern *"Steuerreform 2026: Analyse der Entlastungseffekte"*).
* **Kurzbeschreibung-Vorgabe:** Die `short_description` ordnet die Quelle in genau 1-2 dichten, fließenden Sätzen in den fachlichen Gesamtkontext ein. Sie darf keine Bulletpoints enthalten.
* **Takeaway-Vorgabe:** Die `key_takeaways` liefern exakt `[TAKEAWAY_COUNT_OR_RANGE]` (z.B. 3 bis 5) inhaltlich klar voneinander abgegrenzte Analyseergebnisse.

## TAKEAWAY_RULES
* Generiere genau `[TAKEAWAY_COUNT_OR_RANGE]` Punkte.
* Jedes Takeaway muss das mit der App-UI kompatible und in `_global_quality_rules.md` definierte **Schlagwort-Muster** erfüllen:
  * `**Aussagekräftiges individuelles Schlagwort:** Vertiefender, fließender Folgesatz direkt nach dem Doppelpunkt ohne Zeilenumbruch.`
* **Strikte Flachheit:** Keine Unterlisten, keine Spiegelstriche in den Elementen, keine verschachtelten Einrückungen.
* **Sonder-Parsing für Facts-vs-Opinions:** `[SPECIAL_PARSING_RULE_IF_ANY]` (z. B. wenn Facts vs Opinions: Jeder String beginnt exakt mit `[F] ` oder `[M] ` vor der fettgedruckten Einleitung).

## FUNCTION_SPECIFIC_RULES
Hier sind die für diesen Analysetyp exklusiven und zwingend einzuhaltenden Sachregeln definiert:
1. `[SPECIFIC_RULE_1]` (z. B. für Risikoanalyse: Sortiere die Risiken strikt absteigend nach ihrer Schadensschwere).
2. `[SPECIFIC_RULE_2]` (z. B. für Aktualitätscheck: Benenne konkrete zeitliche Diskrepanzen zwischen dem Dokumentenstand und heute).
3. `[SPECIFIC_RULE_3]` (z. B. für Business-Inkubator: Jede Idee muss ein klares, separates Produkt- oder Dienstleistungskonzept darstellen).

## 11. No-Gos
Ergänzend zu den globalen Verboten (keine generischen Labels, keine Hochkommas) ist im Rahmen dieser Funktion Folgendes strengstens untersagt:
* ❌ `[FUNCTION_SPECIFIC_FORBIDDEN_PATTERN_1]`
* ❌ `[FUNCTION_SPECIFIC_FORBIDDEN_PATTERN_2]`
* ❌ `[FUNCTION_SPECIFIC_FORBIDDEN_PATTERN_3]`

### Immer geltende No-Gos
j) keine KI-Metasprache wie „Dieses Dokument beschreibt...“, „Der Prompt analysiert...“ oder „Als KI...“  
k) keine technischen Systembegriffe im finalen Nutzertext, z. B. JSON-Keys, Android-Klassen, API-Parameter oder interne AnalysisType-Namen

## 12. Output-Besonderheiten
### Regeln zu `owner`
`[OWNER_CLASSIFICATION_RULES]`

### UI-Nummerierung
`[UI_NUMBERING_RULES]`

Standard:
a) keine manuelle Nummerierung in `title` oder `details`  
b) keine Präfixe wie „1.“, „01.“, „①“ oder „Punkt 1“  
c) wenn die UI eine Funktion nummeriert darstellt, erfolgt die Nummerierung ausschließlich UI-seitig  
d) funktionsspezifische Ausnahmen müssen hier ausdrücklich dokumentiert werden  

Hinweis:
Historisch sind Nummerierungs-Sonderlogiken insbesondere für `TOP_3_KERNAUSSAGEN` und `RISIKO_ANALYSE` relevant. Prompt und UI dürfen Nummerierung nicht doppelt erzeugen.

### Optional erlaubte Nummerierungs- oder Icon-Logik innerhalb von Textwerten
`[OPTIONAL_NUMBERING_AND_ICON_LOGIC]`

## GOOD_OUTPUT_EXAMPLES
Ein hervorragendes, fachspezifisches Ergebnis für diesen Analysetyp sieht wie folgt aus:

```json
{
  "title": "[GOOD_EXAMPLE_TITLE]",
  "original_url": "[GOOD_EXAMPLE_URL]",
  "short_description": "[GOOD_EXAMPLE_SHORT_DESCRIPTION]",
  "key_takeaways": [
    "**[GOOD_TAKEAWAY_KEYWORD_1]:** [GOOD_TAKEAWAY_EXPLANATION_1]",
    "**[GOOD_TAKEAWAY_KEYWORD_2]:** [GOOD_TAKEAWAY_EXPLANATION_2]",
    "**[GOOD_TAKEAWAY_KEYWORD_3]:** [GOOD_TAKEAWAY_EXPLANATION_3]"
  ],
  "owner": "[GOOD_EXAMPLE_OWNER]"
}
```

## BAD_OUTPUT_EXAMPLES
Vermeide unbedingt folgende ungenügende bzw. fehlerhafte Ausgabemuster für diesen Analysetyp:

* ❌ **Negativbeispiel (Fehler: [BAD_EXAMPLE_ERROR_REASON_1]):**
  `"**Generisches Label:** [BAD_TAKEAWAY_TEXT_1]"`
* ❌ **Negativbeispiel (Fehler: [BAD_EXAMPLE_ERROR_REASON_2]):**
  `"'**[BAD_TAKEAWAY_KEYWORD_2]**' - [BAD_TAKEAWAY_TEXT_2] (enthält führende Anführungszeichen/Spiegelstriche)"`
* ❌ **Negativbeispiel (Fehler: [BAD_EXAMPLE_ERROR_REASON_3]):**
  `"**[BAD_TAKEAWAY_KEYWORD_3]:** [BAD_TAKEAWAY_TEXT_3] \n - Verschachtelter Unterpunkt 1 \n - Verschachtelter Unterpunkt 2 (bricht die Ein-Ebenen-Regel)"`

## 18. Finaler Output-Hinweis
Gib ausschließlich das finale JSON gemäß globalem Output-Contract aus.

## IMPLEMENTATION_NOTES
Zukünftige Erweiterungsstufen des UI für `[ANALYSIS_TYPE]` (z. B. `[UI_PLANNED_FEATURE]`) setzen auf eine strikte Einhaltung dieser Datenstruktur. Abweichungen führen unweigerlich zu Darstellungsfehlern in der Benutzeroberfläche des Abstractors.
