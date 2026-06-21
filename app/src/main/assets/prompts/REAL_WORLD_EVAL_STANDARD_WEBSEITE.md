# REAL WORLD EVALUATION LAYER: STANDARD_WEBSEITE

This document defines the real-world operational evaluation framework and stress-testing suites designed to validate the robust execution of the `STANDARD_WEBSEITE` prompt under hostile web conditions, extreme inputs, and multilingual contexts.

---

## 1. Test Matrix & Scenarios

To certify the prompt behavior, it must be evaluated across the following five critical extraction categories under real-world input constraints, producing a binary PASS/FAIL outcome rather than subjective grading.

### Suite 1: Real Web Noise & Extraction Scaffolding
* **Input conditions:** Raw scraper dumps containing cookie walls, GDPR prompts, navigation bars, header elements, inline scripts, CSS styles, and unparsed boilerplate HTML markup.
* **PASS Criteria:** The prompt successfully filters out non-content noise, ignores boilerplate text, and extracts the primary content without referencing or being distracted by cookie/navigation elements in `title`, `short_description`, or `key_takeaways`.
* **FAIL Criteria:** Inclusion of GDPR/cookie notices, header artifacts, navigation phrases, or developer instructions in the final summarized output.

### Suite 2: Extreme Compression & High Volume Sources
* **Input conditions:** Very long articles (>10k characters up to the maximum input limits of the model context) containing dense narrative structures or long-form investigative reports.
* **PASS Criteria:** Global Event Priority and Relevance Ranking function correctly. High-importance narrative events (health, mechanics, bureaucracy) are fully preserved in detail while ambient context (scenery description, standard dining) is heavily compressed on a linear scale.
* **FAIL Criteria:** Truncating or omitting late-stage critical developments due to early context fatigue, or compressing high-value events into abstract categorical summaries.

### Suite 3: Ultra-Short & Minimalist Dumps
* **Input conditions:** 1–3 sentences of highly sparse or raw input (e.g. "We entered Bissau today. It was hot but fine.").
* **PASS Criteria:** The model acts completely with zero speculation, produces a valid JSON response without hallucinating or artificially expanding details, sets `owner` properly (or `""` if absent), and structures the single event accurately.
* **FAIL Criteria:** Hallucinating additional events, speculative expansion of findings, padding `key_takeaways` with generic content, or outputting invalid JSON due to missing arrays.

### Suite 4: Mixed Quality Sources & SEO Spam
* **Input conditions:** Low-quality pages, automated AI blog content, SEO-optimized summary layers, and pages with broken semantic structure.
* **PASS Criteria:** The model extracts the core factual actions, filters out marketing buzzwords or sensationalist claims, remains neutral, and successfully ranks primary critical events above promotional noise.
* **FAIL Criteria:** Replicating marketing hyperbole, creating takeaways from promotional clickbait, or losing the factual thread of the user's travel/informational journey.

### Suite 5: Multilingual & Hybrid Source Files
* **Input conditions:** Hybrid code mixtures (e.g., Portuguese dialogue inside a German narrative blog, English news with French segments, or purely non-German content).
* **PASS Criteria:** Perfectly translates foreign contents to factual German, maintains accurate entity spelling for locations, landmarks, and names, and structures the findings seamlessly in German.
* **FAIL Criteria:** Outputting mixed-language takeaways, inaccurate translation of specialized terms (like mechanical failures), or altering the meaning of complex linguistic constructs.

---

## 2. Real-World Failure Classification

Under this real-world validation protocol, any failed test case must be marked directly with its corresponding objective failure code:

* **[F1] Missing Critical Info:** Failing to capture highly critical journey-altering events (e.g., visa issues, mechanical failure, safety hazards).
* **[F2] Over-Compression (Category Slippage):** Aggregating specific, highly detailed actions into abstract generalizations (e.g., summarizing "rebuilding the steering rack with custom parts" as "handling minor vehicle problems").
* **[F3] Hallucinated Inference:** Inferring outcomes, timeframes, or intentions that are not directly supported by explicit statements in the source content.
* **[F4] Broken Narrative Chain:** Breaking the chronology or cause-and-effect relationship of an incident (e.g., reporting a solution without referencing the initiating conflict, or vice versa).
* **[F5] JSON Contract Violation:** Emitting schema errors, null pointers instead of empty string `""` for `owner`, missing array elements, or trailing commas breaking parsing.

---

## 3. Real-World Risk Assessment & Technical Evaluation

| Operational Risk | Severity | Mitigation Strategy | Prompt Anchor Reference |
| :--- | :--- | :--- | :--- |
| **Parsing & Schema Crash** | High | Force explicit default empty string output; declare `owner` as non-nullable. | `OUTPUT_CONTRACT`, `Akzeptanzkriterien` |
| **Boilerplate Pollution** | Medium-Low | Explicit exclusion instruction for GDPR, cookie walls, ads, and navigation links. | `Arbeitsschritte (Sichtung des Textkörpers)` |
| **Abstraction Loss** | Medium | Strict detail enforcement for narrative/experiential source types. | `Content-Type Adaptive Summarization` |
| **Chronological Interversity** | Medium | Sequence preservation and strict absteigend relevance ordering. | `Global Event Priority` & `Narrative Integrity Rule` |

---

## 4. Final Status Framework (Freeze Conditions)

An operational prompt configuration under development can only possess one of the following statuses:

1. **NOT READY:** Fails basic schema conformance, includes `null` for `owner` under any scenario, or frequently triggers **[F1]** or **[F5]** errors on any of the Suite 1-5 test configurations.
2. **CONDITIONALLY READY:** Passes JSON/Schema constraints cleanly to guarantee no application crashes. Correctly translates multilingual outputs, and filters boilerplate noise. However, there may still be occasional minor occurrences of **[F2]** (over-compression of complex narrative chains) or **[F4]** (chronological order mismatch in extremely long inputs). Prompt is safe to deploy under supervision but requires monitoring.
3. **READY (FREEZE CERTIFIED):** Meets all validation requirements without exception under real-world conditions:
   * **100% of tested domains pass** structural validation with absolute schema conformity.
   * **Zero Type [F1] and [F5] errors** observed across stressful testing sessions.
   * **Full chronological narrative integrity and priority enforcement** successfully verified on long, complex inputs.
