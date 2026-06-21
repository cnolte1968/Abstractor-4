# BLOCK 1 – OUTPUT QUALITÄTSVERIFIKATION

## BLOCK STATUS

* **OUTPUT STRUCTURE:** **OK**
* **TAKEAWAYS:** **OK**
* **ICON SYSTEM:** **OK**
* **COPY/UI ALIGNMENT:** **OK**
* **ENGINE CENTRALIZATION:** **OK** (Structure is centralized, with slight distributed string parsing)

---

## EVIDENCE

### 1. Unified Output Structure (`AbstractorSummary` in `GeminiModels.kt`)
The entire application operates on a single central data class which enforces a unified schema:
```kotlin
// In app/src/main/java/com/example/data/GeminiModels.kt, Line 96-102:
data class AbstractorSummary(
    @param:Json(name = "title") val title: String,
    @param:Json(name = "original_url") val originalUrl: String,
    @param:Json(name = "short_description") val shortDescription: String,
    @param:Json(name = "key_takeaways") val keyTakeaways: List<String>,
    @param:Json(name = "owner") val owner: String? = null
)
```
There are no class-level structure deviations between analysis modes, which ensures structural rigidity.

---

### 2. Takeaway Level & Labels (`MainActivity.kt` & `SummaryResponseParser.kt`)
* **Exactly 1 Level/Flag List:** Takeaways are returned as a flat `List<String>`.
* **Dynamic Title Extraction:** To render clean visual cards with bullet-headers, `MainActivity.kt` parses the takeaways:
```kotlin
// In MainActivity.kt, Line 1750-1767:
fun parseTakeaway(text: String): Pair<String, String> {
    val boldRegex = Regex("^\\s*\\*\\*(.*?)\\*\\*\\s*(?:[:\\-]?\\s*)?(.*)$")
    val matchResult = boldRegex.find(text)
    if (matchResult != null) {
        val title = matchResult.groupValues[1].trim()
        val description = matchResult.groupValues[2].trim()
        return Pair(title, description)
    }

    val colonIndex = text.indexOf(":")
    if (colonIndex > 0 && colonIndex < 35) {
        val title = text.substring(0, colonIndex).replace("*", "").trim()
        val description = text.substring(colonIndex + 1).trim()
        return Pair(title, description)
    }

    return Pair("Erkenntnis", text.replace("*", "").trim())
}
```
If a takeaway does not present a bold title prefix (e.g. `**Titel**: Beschreibung`), the UI dynamically falls back to prepending `"Erkenntnis"`. No nested markdown lists are supported in the layout, ensuring consistent spacing and visual harmony.

---

### 3. Icon System Mapping (`MainActivity.kt`)
Interactive items have fixed mappings based on the `AnalysisType` enum:
```kotlin
// In MainActivity.kt, Line 1739-1748:
fun getIconForAnalysisType(analysisType: com.example.data.AnalysisType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (analysisType) {
        com.example.data.AnalysisType.RISIKO_ANALYSE -> Icons.Default.Warning
        com.example.data.AnalysisType.BUSINESS_INKUBATOR -> Icons.Default.Lightbulb
        com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> Icons.Default.Verified
        com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR -> Icons.Default.Warning
        com.example.data.AnalysisType.AKTUALITAETS_CHECK -> Icons.Default.Search
        else -> Icons.Default.CheckCircle
    }
}
```
In addition, a round aesthetic variation is introduced dynamically based on the list item index:
```kotlin
// In MainActivity.kt, Line 1728-1737:
fun getTakeawayStyleConfig(index: Int): TakeawayStyleConfig {
    return when (index) {
        0 -> TakeawayStyleConfig(Icons.Default.TrackChanges, Color(0xFFEFF2FF), Color(0xFF536EF1))
        1 -> TakeawayStyleConfig(Icons.Default.BusinessCenter, Color(0xFFFEF3C7), Color(0xFFD97706))
        2 -> TakeawayStyleConfig(Icons.Default.Person, Color(0xFFCCFBF1), Color(0xFF0D9488))
        3 -> TakeawayStyleConfig(Icons.Default.Business, Color(0xFFFCE7F3), Color(0xFFDB2777))
        4 -> TakeawayStyleConfig(Icons.Default.Verified, Color(0xFFD1FAE5), Color(0xFF059669))
        else -> TakeawayStyleConfig(Icons.Default.Lightbulb, Color(0xFFF3E8FF), Color(0xFF7C3AED))
    }
}
```

---

### 4. Copy vs UI Structural Alignment (`MainActivity.kt`)
Copying to clipboard or sharing text is handled centrally by `buildPlainTextShareOrCopyText` which cleanly structures the identical fields (Title, Owner, URL, Ganz Kurz, and Key Takeaways) and strips markdown tags for presentation:
```kotlin
// In MainActivity.kt, Line 1826-1877:
fun buildPlainTextShareOrCopyText(
    summary: AbstractorSummary,
    analysisType: com.example.data.AnalysisType,
    fallbackUrl: String
): String {
    // ... constructs structured plaintext matching the UI cards ...
}
```

---

### 5. Engine Centralization Structure (`SummaryResponseParser.kt`)
The parser enforces structural conformance on any raw LLM responses, ensuring they are cleanly loaded into `AbstractorSummary`. 
However, string cleanup logic is distributed:
* Prompt level: Instructs LLM on format templates (`**Key**: Value`).
* Parse level: Cleans string tails, strips bullets, coordinates fallback.
* UI level: Parses `**Key**: Value` into separate Title/Body Pair representations (via `parseTakeaway`).

---

## RISIKEN

1. **Unerwartete Prompt-Formatierungen (KI-Ausreiße):**
   Falls ein Funktionsprompt `F_*.md` die KI anweist, alternative Formate zu nutzen (z.B. keine fetten Hauptpunkte oder verschachtelte Aufzählungszeichen zu generieren), bricht das Pattern `**Titel**: Details` im UI auf, und davor wird im UI-Card das Label `"Erkenntnis"` hartkodiert eingeblendet.
2. **Spezielles Parsing für Facts & Opinions:**
   In `SummaryResponseParser.kt` existiert ein Spezialfall für `FACTS_VS_OPINIONS_ANALYZER` (Line 41-47), welcher `cleanFactsVsOpinionsTakeaway` aufruft. Dies filtert bestimmte vordefinierte Tags wie `[F]`, `[M]` etc. heraus. Wenn zukünftige Prompts neue Symbole einführen, greift diese filternde Bereinigung ins Leere.
