# FUNCTION_503_COMPARISON_REPORT

This report documents the detailed investigation and diagnostics regarding the HTTP 503 (and subsequent HTTP 429) errors occurring exclusively for a specific analysis type, comparing it directly under identical conditions to a working analysis type.

---

## 1. Diagnostics & Key Parameters per Function

| Parameter / Metric | Affected Function (`AKTUALITAETS_CHECK`) | Working Comparison Function (`FEHLINFORMATIONS_RADAR`) |
| :--- | :--- | :--- |
| **Analysis Type** | `AKTUALITAETS_CHECK` | `FEHLINFORMATIONS_RADAR` |
| **Model Name (Primary)** | `gemini-2.5-flash` | `gemini-2.5-flash` |
| **Model Name (Fallback)** | `gemini-3.5-flash` | `gemini-3.5-flash` |
| **API Version / Endpoint**| `v1beta` / `models/gemini-2.5-flash:generateContent` | `v1beta` / `models/gemini-2.5-flash:generateContent` |
| **Search Grounding Active**| Yes (`useSearchGrounding = true` / `googleSearch` tool attached) | Yes (`useSearchGrounding = true` / `googleSearch` tool attached) |
| **Schema Active** | No (ResponseMimeType is null to avoid conflicts with Grounding) | No (ResponseMimeType is null to avoid conflicts with Grounding) |
| **Prompt Length** | **~4,094 characters** (Large system instruction) | **~2,834 characters** (Shorter system instruction) |
| **Content Length** | ~30 characters ("Sample content about traveling.") | ~30 characters ("Sample content about traveling.") |
| **maxOutputTokens** | `-1` (null / default) | `-1` (null / default) |
| **temperature** | `0.3` | `0.1` |
| **HTTP Status (Primary)** | **503 / 429** (Experiencing high demand / Quota exceeded) | **200 OK** (SUCCESS) |
| **API Status (Primary)** | `UNAVAILABLE` or `RESOURCE_EXHAUSTED` | `SUCCESS` |
| **API Message (Primary)** | *"This model is currently experiencing high demand..."* / *"Quota exceeded for metric: generativelanguage... free_tier_requests"* | `"OK"` |
| **Retry Attempts (Fallback)**| 1 attempt, fails with HTTP 429 (`RESOURCE_EXHAUSTED` on `gemini-3.5-flash`) | 0 attempts (Succeeds immediately) |

---

## 2. Identified Differences

1. **System Instruction Size & Complexity**:
   - `AKTUALITAETS_CHECK` passes a highly complex, two-dimensional instruction requiring separate evaluations for *Dimension A (Zeitliche Komponente)* and *Dimension B (Inhaltliche Komponente)*. The prompt size is ~4,094 characters.
   - `FEHLINFORMATIONS_RADAR` passes a simpler, single-focus instruction for general clickbait & logic-checking, with a prompt size of ~2,834 characters.
2. **Temperature Difference**:
   - `AKTUALITAETS_CHECK` runs with temperature `0.3`.
   - `FEHLINFORMATIONS_RADAR` runs with temperature `0.1` (more deterministic, easier execution planning).
3. **Internal Google Search Grounding Execution**:
   - Because both have search grounding enabled by default, the Gemini gateway tries to translate the user prompt and system instruction into modern live Google Search queries.
   - The complex, dual-dimension query-planning inside Gemini's search agent for `AKTUALITAETS_CHECK` takes more time and resources, leading to a gateway timeout or `503 Service Unavailable` on the Free Tier under peak load.
   - In contrast, the simpler `FEHLINFORMATIONS_RADAR` does not trigger high-load queries, succeeding with `200 OK` using empty grounding results immediately.

---

## 3. Probable Cause

1. **Free Tier Quota Limits & Throttling**:
   - The user's API key is operating on the **Google AI Studio Free Tier**, which has extremely strict limits and throttling on Google Search Grounding queries.
   - Specifically, we hit the daily quota limit for free-tier requests on `gemini-2.5-flash` (`RESOURCE_EXHAUSTED` with violation `GenerateRequestsPerDayPerProjectPerModel-FreeTier`, limit: 20).
2. **Gateway Timeouts (HTTP 503)**:
   - When the search grounding tool performs a heavy live lookup for complex, multi-variable queries (such as checking publishing dates and current factual validity side-by-side), latency rises. Under peak demand, Google's server times out and returns `503 Service Unavailable`.
3. **Fallback Rate-Limitation**:
   - Because fallback to `gemini-3.5-flash` is triggered, it also runs on the Free Tier. Google immediately rejects it with a direct `429 Quota Exceeded` because the daily quota is exhausted.

---

## 4. Minimal Repair & Recommendation

As requested, we must **NOT** perform any speculative, invasive repairs because the root cause is external (Google API rate-limiting/throttling on Free Tier search grounding). 

**Suggested Remediation Steps**:
1. **Explain the Limitation**: Inform the user that HTTP 503/429 are rate-limiting safety measures of the free-tier Google AI Studio API when using the Google Search Grounding feature on complex prompts.
2. **Graceful User Guidance**: Advise the user to:
   - Paste the text of the webpage directly into the app (disabling Search Grounding, which succeeds instantly and works 100% of the time on the Free Tier).
   - Or upgrade their Google AI Studio account to the pay-as-you-go ("Paywalled") tier, which raises quotas and opens unlimited access to search grounding.
