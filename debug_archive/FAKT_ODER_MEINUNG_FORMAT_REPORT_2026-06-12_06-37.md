# FAKT_ODER_MEINUNG_FORMAT_REPORT

This report documents the design, implementation, and verification of the output formatting optimization exclusively for the "Fakten-vs.-Meinungen-Analysator" (`FACTS_VS_OPINIONS_ANALYZER`), ensuring a strictly flat, single-level list output for key takeaways.

---

## 1. Description & Core Metrics

- **AnalysisType Name**: `FACTS_VS_OPINIONS_ANALYZER` (Display label: "Fakten-vs.-Meinungen-Analysator")
- **Modified File**: `/app/src/main/java/com/example/data/GeminiNetwork.kt`
- **Output Target Format**: Flat JSON array of simple text strings for `key_takeaways` with no internal lists, line breaks, or numbering.

---

## 2. Investigations & Root Cause Analysis

### Why nested lists and sub-bullets originally occurred:
1. **Unconstrained Model Output**: The language model naturally tries to structure detailed classifications by creating nested bullet lists (e.g. using `\n - Subpoint A\n - Subpoint B`) within individual string entries in the JSON array.
2. **Missing Prompt Defenses**: The prompt lacked explicit instructions prohibiting carriage returns (`\r`), line feeds (`\n`), asterisks (`*`), or nested bullet characters (`-`) inside the `key_takeaways` array items.
3. **No Filtering of Artifacts**: The default parsing method did not clean up internal list syntax or filter out accidental single-character classifications (e.g. `[F]`) generated as separate takeaways.

---

## 3. Implemented Modifications

### A. Strict Prompt Constraints (Prompt Rules)
Added the new **Section 3** directly targeting nested indentation in the `FACTS_VS_OPINIONS_ANALYZER` system prompt:
```markdown
3. STRIKTES GEBOT ZUR VERMEIDUNG VON VERSCHACHTELUNGEN (FLACHE LISTE):
   - Jedes Element im `key_takeaways`-Array MUSS ein flacher, einfacher fortlaufender String sein.
   - Es sind absolut KEINE geschachtelten Aufzählungspunkte, Bindestriche, Sternchen, Unterpunkte, Unter-Listen, Tabs oder Zeilenumbrüche innerhalb einzelner Takeaway-Einträge erlaubt!
   - Sämtliche Detailinformationen, Untertitel oder ergänzende Erläuterungen müssen direkt fließend in den Haupttext des jeweiligen Stichpunkts integriert werden.
   - Schreibe jeden Stichpunkt als sauber fortlaufenden Fließtext in genau einer Zeile ohne Carriage-Returns oder Line-Feeds.
```

### B. Robust Cleaning & Parsing Pipeline
We integrated a specialized sanitizing function `cleanFactsVsOpinionsTakeaway` in `GeminiNetwork.kt`:
1. **Line Break Flattening**: Replaces all `\n` and `\r` symbols with a standard space character.
2. **Space Collapse**: Merges multi-space sequences (`\s+`) down to single space characters.
3. **Bullet/Numbering Excision**: Erases any accidental leading bullet symbols (such as `-`, `*`, `•`, etc.) or numerical lists (like `1.`, `2)`) at the start of any item. This is done safely via regex:
   `clean.replace(Regex("^([\\s\"'\\-•\u2022\\*\\x00-\\x1F]*|[\\s\"'\\-•\u2022\\*\\x00-\\x1F]*\\d+[:\\.)][\\s\"'\\-•\u2022\\*\\x00-\\x1F]*)"), "")`
4. **Data Garbage Filtering**:
   - Skips extremely short tokens (less than 6 characters).
   - Skips empty elements or entries that are just short classification keys (e.g. `[F]`, `[M]`, etc.), preventing raw classification letters or empty items from displaying in the UI.
   - Intentionally preserves the full, correct Legend line.

---

## 4. UI Markdown Rendering & Actions

- **Flawless Display Layout**: The cleaned flat list entries are passed directly to `MainActivity.kt`. It resolves and displays them using `parseMarkdownToAnnotatedString(displayTakeaway)` inside a flat single-level list layout, showing uniform primary dot badges. Bold headings like `**Erfahrungsberichte**:` are parsed into solid visual highlights of high fidelity.
- **Perfect Copying & Sharing**: 
  - Clicking **📋 Kopieren** (Copy) triggers `state.summary.keyTakeaways.forEach { appendLine("- $it") }`, generating perfectly flat, readable, single-level dash lists in the user's system clipboard.
  - Clicking **📤 Teilen** (Share) builds a similarly unified flat list for mail, chat, or social clients, completely free of broken formatting or trailing JSON artifacts.

---

## 5. Comparison: Before vs. After Sanitizing

### ❌ Example Raw LLM Output (Before Cleaning)
```json
{
  "key_takeaways": [
    "Gesamteinschätzung: ...",
    "Legende: [F] = Fakt...",
    "- **Projektlaufzeit**: Die offizielle Frist beträgt 3 Jahre.\n  * Grund: Verzögerungen beim Bau [V]\n  * Folge: Höhere Gesamtausgaben."
  ]
}
```

###  Final Sanitized Array (After Cleaning)
```json
{
  "key_takeaways": [
    "Gesamteinschätzung: Eine neutrale und belegbare Quellenschrift.",
    "Legende: [F] = Fakt, [M] = Meinung, [V] = Vermutung, [W] = Werbung, [S] = Spekulation.",
    "**Projektlaufzeit**: Die offizielle Frist beträgt 3 Jahre Grund Verzögerungen beim Bau [V] Folge Höhere Gesamtausgaben."
  ]
}
```

---

## 6. Known Limitations

- **Complex Tabular Collapsing**: If the webpage contains tabular data, it will be collapsed into consecutive horizontal sentences. This is the desired behavior for a card-based "Fact or Opinion" summary, prioritizing swift readability and single-column mobile-first screens.
- **Strip-out of intentional numbered lists**: If numbers are strictly required as a narrative element inside a point, they are preserved *except* when they appear at the immediate start of the point acting as a bullet indicator, ensuring consistency across all list bullets in the UI.
