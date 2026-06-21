# ARCHITEKTUR-STANDORTBESTIMMUNG & ENTKOPPLUNGS-SPEZIFIKATION (PHASE 3)

Dieses Dokument analysiert präzise und faktenbasiert die Kopplungsmuster zwischen der LLM-Rückgabe (Gemini), der Parsing-Schicht (`SummaryResponseParser`) und der Präsentationsschicht (`MainActivity` / `MainViewModel`) der Abstractor-App. Es spezifiziert die Root-Cause systemischer Fragilität und legt ein zukunftsweisendes Domain-Contract-Modell nahe.

---

## 1. IST-Zustand (Faktisch)

### 1.1 Datenstrukturen & Fluss
1. **Gemini API & Raw Text:** Die Prompt-Engine fordert ein JSON-Dokument an. Gemini liefert eine JSON-Zeichenkette zurück.
2. **Parser-Eingang:** `SummaryResponseParser.parse` nimmt diesen unstrukturierten bzw. teilstrukturierten `rawText` auf.
3. **Moshi-Deserialisierung:** Es wird versucht, den JSON-Text in das Datenmodell `AbstractorSummary` zu gießen:
   ```kotlin
   data class AbstractorSummary(
       val title: String,
       val originalUrl: String,
       val shortDescription: String,
       val keyTakeaways: List<String>,
       val owner: String? = null
   )
   ```
4. **Regex- & Bullet-Fallback:** Schlägt Moshi fehl, rekonstruiert der Parser die Felder mittels Key-Regexes. Liefert die LLM-Rückgabe gar kein JSON, sondern Flachtext, scannt das Fallback die Zeilen nach typischen Aufzählungs-Präfixen (z. B. `-`, `*`, `•`, Nummerierungen) ab und befüllt damit `keyTakeaways`.
5. **Datenfluss zur UI:** `MainViewModel` exponiert das identische `AbstractorSummary`-Modell im `UiState.Success`. Die UI iteriert über `keyTakeaways: List<String>`.

### 1.2 String-basierte Interpretation
Sämtliche Kernaussagen (`keyTakeaways`) verbleiben bis zum Renderzeitpunkt als rohe, unstrukturierte Strings. Erst unmittelbar beim Zeichnen der UI-Karten in `MainActivity.kt` (Zeile 837) wird jeder String durch eine lokale Hilfsfunktion interpretiert:
```kotlin
val parsed = parseTakeaway(takeaway) // parsed.first = Titel, parsed.second = Beschreibung
```

---

## 2. Architekturbrüche & Kopplungen (Coupling Points)

Das System weist an mehreren Schnittstellen signifikante Kopplungen auf, die eine Erhöhung der Prompt-Diversität erschweren:

### 2.1 Mehrfache Interpretation derselben Daten (Triple-Parsing)
Eine Kernaussage wird im System an drei Stellen unterschiedlich gescannt und manipuliert:
1. **Im Prompt:** Die LLM-Instruktion befiehlt das Format `**Konzept**: Beschreibung`.
2. **Im Parser (`SummaryResponseParser`):** Für bestimmte Auswertekanäle (z. B. `FACTS_VS_OPINIONS_ANALYZER`) filtert der Parser die Strings vorab, um syntaktische Marker (wie `[F]`, `[M]`) zu bereinigen.
3. **In der UI (`MainActivity`):** Das UI nutzt das Regex-Pattern `^\s*\*\*(.*?)\*\*\s*(?:[:\-]?\s*)?(.*)$` im Aufruf von `parseTakeaway`, um den String erneut zu zerlegen.

### 2.2 UI-abhängige Parsing-Logik
Der Parser führt Verzweigungen aus, die auf die visuelle Darstellung abzielen:
* Der Parameter `keepNumbering` steuert, ob Aufzählungsnummern aus dem Text geschnitten werden sollen oder nicht. Der Parser muss hierfür wissen, ob die nachgelagerte UI eine Liste mit festen Zahlen (z. B. 01, 02 bei den Top 3) rendert oder rein unnummerierte Bulletpoints anzeigt.

### 2.3 Prompt-abhängige UI-Logik (Impliziter Vertrag)
Die Präsentationsschicht verlässt sich implizit darauf, dass das Modell einen Doppelpunkt `:` oder Fettdruck-Markdown `**` liefert.
* Schreibt eine zukünftige Prompt-Variante einen fließenden Text oder nutzt geschweifte Klammern, versagt `parseTakeaway` geräuschlos.
* Die UI dichtet dann eigenmächtig das statische Label `"Erkenntnis"` als Titel ab und klatscht den gesamten Textzusammenhang in das Detail-Feld.

### 2.4 Modusspezifische Verzweigungen im Universal-Parser
In `SummaryResponseParser.kt` existieren Hard-Coded-Checks auf bestimmte Enumerationswerte des Typs `AnalysisType`:
```kotlin
val cleanedTakeaways = if (analysisType == AnalysisType.FACTS_VS_OPINIONS_ANALYZER) {
    summary.keyTakeaways.mapNotNull { cleanFactsVsOpinionsTakeaway(it) }
} else if (analysisType == AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS) {
    summary.keyTakeaways.map { cleanTakeawayItem(it, keepNumbering = true) }
} ...
```
Der Parser ist somit nicht generisch. Jede Skalierung auf neue Analysemodi erzwingt Eingriffe in die zentrale Parser-Logik.

---

## 3. Root-Cause Analyse

Die eigentliche Ursache für diese Kopplung ist das **Fehlen einer expliziten Domänen- bzw. Transformationsschicht** zwischen dem unzuverlässigen LLM-Textstrom und dem typisierten Android-Datenmodell.

* **Die erste semantische Unsicherheit** entsteht im Moment der JSON-Deserialisierung: Das System schrumpft die strukturierten Gedanken des LLMs (welche semantisch immer aus einer Entität wie *Begriff/Titel* und einer zugehörigen *Erklärung/Detail* bestehen) auf ein flaches, primitives `List<String>`.
* Weil diese Strukturinformation im Modell vernichtet wird, muss die nachgelagerte Compose-UI diesen Verlust kompensieren, indem sie Ad-Hoc-Regex aufruft, um die getrennten Felder mühsam zurückzugewinnen.
* **Ergebnis:** Der Parser schützt die App zwar vor Abstürzen (technisch stabil), zwingt die Präsentationsschicht jedoch dazu, sprachliches Parsing zu betreiben (architektonisch fragil).

---

## 4. Zielarchitektur (Clean Design)

Um eine vollständige Kapselung der Komponenten zu erreichen, muss das primitive Stringmodell durch ein streng typisiertes Domänenmodell abgelöst werden.

```
[Gemini API] 
     │  (liefert JSON-String)
     ▼
[Parser (SummaryResponseParser)] 
     │  (parst JSON generisch + validiert syntaktische Integrität)
     ▼
[Domain Transformator] ────► [Domain Model: DomainSummary]
     │                       ├── title: String
     │                       ├── shortDescription: String
     │                       └── keyTakeaways: List<TakeawayItem>
     │                                         ├── title: String
     │                                         └── details: String
     ▼
[UI Layer (Jetpack Compose)] 
        (nimmt fertiges DomainSummary entgegen und zeichnet deklariert)
```

### 4.1 Das neue Domänenmodell (`DomainSummary`)
```kotlin
data class TakeawayItem(
    val title: String,
    val details: String,
    val visualMetadata: Map<String, String> = emptyMap() // Für modusspezifische Zusatzinfos
)

data class DomainSummary(
    val title: String,
    val originalUrl: String,
    val shortDescription: String,
    val keyTakeaways: List<TakeawayItem>,
    val owner: String? = null
)
```

### 4.2 Klare Trennung der Verantwortlichkeiten
1. **Der Prompt:** Definiert weiterhin das Zielformat (vorzugsweise direkt als JSON-Struktur, in der jedes Takeaway ein Objekt mit den Keys `title` und `details` darstellt).
2. **Der Parser:** Dekodiert das JSON-Dokument und transformiert es direkt in `DomainSummary`. Falls das LLM von der Struktur abweicht, bricht oder spaltet der Parser die Strings *auf Daten-Ebene* und liefert der UI garantiert gefüllte `title` und `details` Felder im `TakeawayItem`.
3. **Die UI:** Verhält sich absolut "stumm". Sie liest `item.title` und `item.details` aus und rendert diese direkt. Es existiert kein regulärer Ausdruck, kein Doppelpunkt-Scanning und keine Markdown-Säuberung mehr im Presentation-Code.

---

## 5. Migrationsstrategie (Minimal-Invasiv)

Eine schrittweise Entkopplung schützt das live-geschaltete System vor Seiteneffekten:

### Schritt 1: Einführung des Datenvertrags (`TakeawayItem`)
Erstellen der Datenklasse `TakeawayItem` auf Domänenebene. Das existierende `AbstractorSummary` wird so erweitert, dass `keyTakeaways` optional auch als `List<TakeawayItem>` vorliegen kann oder schrittweise migriert wird.

### Schritt 2: Vorverlagerung des Parsing-Prozesses
Die Logik aus `parseTakeaway` wird aus `MainActivity.kt` entfernt und vollständig in den `SummaryResponseParser` integriert. Der Parser transformiert den Flachtext-Output des LLM schon während der Moshi/Regex-Phase in eine Liste aus `TakeawayItem`-Objekten.

### Schritt 3: Bereinigung der Präsentationsschicht (UI)
Die Compose-Cards in `MainActivity.kt` werden so umgeformt, dass sie direkt auf `takeaway.title` und `takeaway.details` zugreifen. Der ad-hoc Aufruf von `parseTakeaway` im Render-Grid entfällt vollständig.

### Schritt 4: Standardisierung des JSON-Outputs im Prompt
Die funktionsspezifischen Analyseregister-Prompts (`F_*.md`) werden sukzessive dahingehend angepasst, dass sie das JSON-Schema mit expliziten Objekt-Arrays (`{"title": "...", "details": "..."}`) für die Takeaways einfordern. Dadurch entfällt langfristig auch die String-Spaltung im Parser-Fallback.
