# PRODUCTION FEEDBACK & TELEMETRY LAYER: STANDARD_WEBSEITE

This document establishes the production telemetry framework and operational feedback loops for the `STANDARD_WEBSEITE` analytical function. It outlines how active runtime data, performance metrics, and user feedback are aggregated to identify structural and linguistic failure modes in live environments.

---

## 1. Runtime Telemetry Framework

To move beyond static local verification, the application logs key metrics from real-world executions, providing the data necessary for continuous prompt refinement.

### 1.1 Data Aggregation Schema
For each execution of the `STANDARD_WEBSEITE` prompt, the telemetry layer captures:
* **Source URL:** The normalized target URL.
* **Input Length:** Character count of the cleaned body text extracted by the `WebpageExtractor` or downstream scrapers.
* **Response Payload:** The raw JSON output string returned by Gemini.
* **Execution Duration:** Overall time (ms) representing network extraction plus model inference.
* **Exception Stack:** Error logs if the extraction, scraping, or JSON parsing steps failed.

### 1.2 Scraper & Extraction Performance Metrics
Live scraping success rates are tracked separately from model reasoning performance:
* **HTTP Resolution Status:** Success/failure rates of DNS resolutions, link shorteners, and OkHttp redirects via `WebpageExtractor.resolveUrl`.
* **Content Extraction Yield:** Character count of extracted content (detecting empty dumps due to anti-scraping walls or JavaScript dependencies).

---

## 2. Production Failure Log Categorization

To maintain a pristine user experience, any parsing anomaly or content deficiency identified at runtime must be logged using these five specific failure modes:

* **[F1] Missing Critical Info (Severity: High):** The output fails to capture severe logging or logistical issues (e.g., missed boundary updates, unaddressed vehicle malfunctions, health emergencies).
* **[F2] Over-Compression (Severity: Medium):** Highly descriptive and valuable sequence actions are compressed into simple abstract phrases (e.g., representing "rebuilding the front axle using hand tools" as simply "the user performed mechanical repairs").
* **[F3] Hallucinated Inference (Severity: Critical):** The model ventures beyond the factual parameters of the source body-text to speculate about future user actions, mental states, or historical assumptions.
* **[F4] Broken Narrative Chain (Severity: Medium):** Chronological sequence or logical steps of a key event are disrupted or lists cause-and-effect indicators in an inverted order.
* **[F5] JSON Contract Violation (Severity: Critical):** The model returns malformed JSON, outputs `null` instead of `""` for the `owner` field under empty conditions, uses unescaped control characters, or emits training comments outside the JSON brackets, triggering a parser exception.

---

## 3. Feedback Loop & Prompt Adjustment Cycles

When failure patterns are identified, they generate candidate prompt adjustments with defined severities:

### 3.1 Defect-driven Adaptation
```
[ Production Failure Logged ] ──> [ Pattern Classification (F1-F5) ]
              │
              ▼
[ Root Cause Analysis ] ──> [ Prompt Adjustment Candidate Drafted ]
              │
              ▼
[ Local Sandbox Verification ] ──> [ Live Deployment & Monitoring ]
```

### 3.2 Regression Prevention & Prompt Verification
Before a prompt adjustment candidate is committed to the main branch (`app/src/main/assets/prompts/F_STANDARD_WEBSEITE.md`), it must be validated against previous failure regression files.

---

## 4. Production Readiness & Freeze Conditions

To declare the prompt "FREEZE-CERTIFIED" for production, the telemetry layer mandates the following empirical proof:

### 4.1 Production Validation Thresholds
1. **Real-world Volume:** Minimum of **100 unique, active URLs processed** in live or staging environments.
2. **Parser Solidity:** **0% JSON parsing errors [F5]** over the last 50 sequential transactions.
3. **Critical Precision:** **0% [F1] (Missing Critical Info) or [F3] (Hallucinated Inference) failures** across tested live samples.
4. **Scraping Stability:** Active extraction yield > 500 characters on valid, non-blocked informational pages.

### 4.2 Local Readiness Evaluation (Active Development)

| Metric / Check | Required Value | Current Telemetry State | Result |
| :--- | :--- | :--- | :--- |
| **Total URLs Processed** | >= 100 Live runs | 0 Live runs (Local Sandbox Environment only) | **FAILED (No Prod Data)** |
| **F5 (JSON parsing) Rate**| 0.00% | 0.00% under local unit and compile tests | **PASSED (Local)** |
| **F1 / F3 Failure Rate** | 0.00% | 0.00% under local verification runs | **PASSED (Local)** |

---

## 5. Live System Verdict

* **CURRENT STATUS: PARTIALLY READY (ONLY WITH DATA)**
* **Reasoning:** The modified `F_STANDARD_WEBSEITE.md` prompt structures are compiled, validated, and syntactically flawless. The Android app build succeeds cleanly. However, because no active production feedback logs or telemetry from >=100 live URLs have been aggregated yet in this environment, a complete production "FREEZE" is pending.
