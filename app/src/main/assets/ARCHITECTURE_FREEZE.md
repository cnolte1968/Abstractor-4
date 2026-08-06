# ARCHITECTURE FREEZE – GLOBAL ANALYTICAL ENGINE (GAA)
**Date:** July 4, 2026  
**Status:** APPROVED & LOCKED  

This document serves as the official, frozen baseline definition of the **Global Analytical Engine (GAA)** plugin architecture. From this point forward, no changes to the structural pattern of the GAA core are permitted without strict architectural review. All future feature expansions must implement the plugin interfaces outlined herein.

---

## 1. Frozen Target Architecture Baseline

The architecture is locked into a decoupled, contract-first design that separates transport, orchestration, validation, and plugin execution.

```
[UI / ViewModels]
       │
       ▼ (invokes)
[AnalyzeContentUseCase]
       │
       ├─► [AnalysisRegistry] (resolves engine & functionId mapping)
       │
       └─► [EngineRunner] (the single isolated Execution Boundary)
                 │
                 ├─► [EngineContract Validation] (pre & post execution)
                 │
                 └─► [AnalysisEngine Plugin] (actual Gemini API execution)
                           │
                           └─► [GeminiRepository Gateway] (pure network transport)
```

### 1.1 AnalysisRegistry (Pure Mapping)
* **Rule:** The registry is a lightweight catalog. It contains only mapping from `AnalysisType` or `functionId` to the corresponding `AnalysisEngine` plugin instance.
* **Prohibitions:** No runtime routing decisions, no feature-specific capability flags, no formatting, and no fallback handling.

### 1.2 EngineRunner (Isolated Execution Boundary)
* **Rule:** The `EngineRunner` is the **only** entry point for executing an `AnalysisEngine`. Direct calls to `engine.analyze(...)` from outside the `EngineRunner` are strictly forbidden.
* **Responsibilities:**
  1. Validates the `CanonicalAnalysisInput` against the contract's `inputSchema` before execution.
  2. Runs the engine's processing block.
  3. Validates the resulting `DomainSummary` against the contract's `outputSchema` and versioning constraints post-execution.

### 1.3 EngineContract (Strict Runtime Validation)
* **Rule:** Every registered engine must export a declared `EngineContract` containing:
  * `functionId` (e.g., `A.1`, `E.1`)
  * `version` (Must comply strictly with SemVer regex: `^\d+\.\d+\.\d+$`)
  * `inputSchema` (Specification of mandatory fields, e.g., `CanonicalAnalysisInput(enrichedText!=null)`)
  * `outputSchema` (Specification of mandatory structures, e.g., `DomainSummary(title, original_url, short_description, key_takeaways)`)
* **Behavior:** Any deviation in input schemas, missing required output fields (blank title, empty takeaways), or invalid SemVer strings causes an immediate `IllegalStateException` (Hard Fail).

### 1.4 AnalyzeContentUseCase (Zero Business Logic)
* **Rule:** The orchestration use case is extremely lean. It performs three steps and nothing else:
  1. `resolve()` (Queries `AnalysisRegistry` for the matching function ID and plugin).
  2. `execute()` (Hands over input and engine to `EngineRunner.runEngine(...)`).
  3. `persist/return()` (Saves the verified output to the database repository and returns).
* **Prohibitions:** No special conditional branching per function ID, no direct parameter hacking, no direct network calls.

### 1.5 GeminiRepository (Pure Transport)
* **Rule:** The `GeminiRepository` is a clean gateway implementing the `GeminiGateway` port.
* **Prohibitions:** Legacy endpoints (such as `summarize(...)` with inline engine instantiations) have been entirely removed. It must never contain processing logic, local state, or prompt configurations.

### 1.6 Parser & Failure Handling (No Fallback Masking)
* **Rule:** For all structured functions, a failure to extract valid JSON must throw an exception. Returning a degraded single-takeaway "fallback summary" as a success is prohibited. Structured functions must either fail completely or satisfy the contract.

---

## 2. Checklist: Adding a New Analytical Function

Follow these exact steps when adding support for a new analysis feature (e.g., a new prompt context or category):

1. **Add Prompt Asset:** Place the new system instruction markdown file under the `app/src/main/assets/prompts/` directory (e.g., `prompts/F_MY_NEW_PROMPT.md`).
2. **Define the Engine Contract:** Determine the `functionId`, SemVer version, and input/output schema constraints for the new function.
3. **Register/Instantiate Engine:**
   * If the function uses standard webpage analysis with a custom prompt, add its configuration to the `webFunctions` list in `AnalysisRegistryImpl` to let it auto-register.
   * If it requires customized pre- or post-processing, subclass `BaseGeminiEngine` (similar to `DocumentAnalysisEngine`) and register it manually in `AnalysisRegistryImpl.init`.
4. **Map AnalysisType:** If a new `AnalysisType` enum is introduced, ensure it is mapped to its `functionId` inside `AnalysisRegistryImpl.getFunctionIdForType`.
5. **Add Contract Unit Tests:** Add a regression test case inside `BaseArchitectureRegressionTest` to execute the engine, verify validation, and test invalid states (violating input/output contract fields) to confirm the boundary behaves deterministically.

---

## 3. Strict No-Go Rules

1. **❌ Direct Engine Calls:** Do not invoke `.analyze(...)` on an engine from any ViewModel, Activity, or UseCase. You must use `AnalyzeContentUseCase` or directly go through `EngineRunner`.
2. **❌ Masked Fallbacks:** Do not capture JSON parsing errors to return a mock `DomainSummary` with `fallbackUsed = true` when structured output was expected. Let the engine runner fail so the application can retry or display an explicit error state to the user.
3. **❌ Non-SemVer Versioning:** Do not use simple integers or custom strings for engine versions (e.g., `1`, `v1.2`). The engine contract validator will throw an exception if the version does not match `Major.Minor.Patch`.
4. **❌ UUID State Drift:** Ensure all pipeline steps preserve the caller's initial `analysisId`. Generating random UUIDs inside parsers or sub-steps ruins traceability logs.
5. **❌ No Absolute Statements or Superlatives:** In prompt instructions and resulting analysis outputs, do not use commercialized, exaggerated, or absolute phrasing such as "risikofrei" (risk-free), "perfekt" (perfect), "unbegrenzt" (unlimited), "100% gesichert" (100% secured), or "absolut fehlerfrei" (absolutely error-free). All assessments must be objective, nüchtern, and contextual.

---

## 4. Hardened Rules for New Functions

All future functional expansions must adhere to these strict modularity rules:

1. **Engine Selection & Reuse Policy:**
   * **Prefer Existing Engines:** You must always prefer reusing existing engine plugins.
   * **Standard Web Engine:** For all URL and webpage analysis functions, the `WebpageAnalysisEngine` is the absolute standard and must be used with a custom prompt.
   * **New Engine Restriction:** You may only implement a new specialized engine (subclassing `BaseGeminiEngine`) if there is a zwingende (mandatory) technical need for specific preprocessing (e.g., unique text scraping or custom formatting) or postprocessing. Creating a new engine purely due to a new functional topic or rating logic is strictly forbidden. Any core-change or engine addition requires explicit approval.
2. **Prompt File Standards:**
   * **Path:** Prompts must reside exclusively in `app/src/main/assets/prompts/`.
   * **Format:** Must be standard Markdown (`.md`).
   * **Strict Naming Scheme:** File names must strictly follow the pattern `F_[FUNCTION_NAME_IN_UPPERCASE].md`. Examples: `F_EMPFEHLUNGS_VALIDATOR.md`, `F_AKTUALITAETS_CHECK.md`.
3. **DomainSummary Field Schema Conformity:**
   * **Mandatory Fields:** In JSON and Kotlin data mappings, you must exclusively use the four fields: `title`, `original_url`, `short_description`, and `key_takeaways` (each takeaway containing `title` and `details`).
   * **Strict Prohibitions:** Alternative field names or variants (such as `summary`, `keyPoints`, `key_points`, `takeaways`, `bulletpoints`) are strictly forbidden.
   * **Non-Empty key_takeaways Rule:** The `key_takeaways` list must never be empty in a `Success` state. If the analysis result indicates a "null-finding" or no negative features are detected, the LLM must generate at least one takeaway describing the objective null-finding (e.g., `title: "Keine veralteten Empfehlungen"`, `details: "Die untersuchten Handlungsempfehlungen stützen sich auf zeitlose Grundprinzipien..."`).
4. **Grounding & Insufficient Content Policy:**
   * **Grounding is an Enhancement, Not a Source Substitute:** Grounding is intended to augment research, verify temporal facts, or check external context—it must never be used to mask or replace missing or extremely weak source contents. If a function requires deep webpage content analysis, the page source must be substantial.
   * **A. Extraction & Summarization Functions:**
     * *Examples:* `A.1 Standard-Webseite`, `A.2 Top 3 Kernaussagen`.
     * *Policy:* These functions operate purely on the provided page content. If the content is too weak, short, or empty, they must strictly return `INSUFFICIENT_CONTENT`. Under no circumstances should they invoke künstlicher Success (artificially synthesized answers) or use external Grounding to fake a source-level finding.
   * **B. Research, Verification & Inquiry Functions:**
     * *Examples:* `A.3 Freie Quellenanfrage`, `B.1 Aktualitätscheck`.
     * *Policy:* These functions may utilize Google Search Grounding to research external context, provided that `supportsSearchGrounding = true` is explicitly configured in their `EngineCapabilities` and `EngineContract`. However, any uncertainty, lack of confirmation, or conflicting external facts must be declared transparently. Grounding must never pretend to find a source-level fact if that fact is not present in the primary page source.
   * **Content Sufficiency Check:** Before an API call is made, if the content is too thin to fulfill the functional analysis goal and the function does not support search-grounded external inquiry, the pipeline must fail fast with `INSUFFICIENT_CONTENT`.

---

## 5. Dry-Run Checklist for New Functions

Before implementing any new analytical function, run a simulated dry run and verify each item below:

* [ ] **Is a new engine truly necessary?** If yes, is there a technical preprocessing/postprocessing justification? If it is a URL analysis, did you reuse `WebpageAnalysisEngine`?
* [ ] **Is the prompt file path and name correct?** Is it placed in `app/src/main/assets/prompts/` and named exactly as `F_[FUNCTION_NAME_IN_UPPERCASE].md`?
* [ ] **Are the output field names strictly conformed?** Are you using exclusively `title`, `original_url`, `short_description`, and `key_takeaways`? No alternative names allowed!
* [ ] **Is the non-empty takeaways rule handled?** In case of a null-finding, is a structured takeaway generated instead of an empty list?
* [ ] **Are absolute terms and superlatives avoided?** Have you ensured that no absolute statements like "perfekt", "risikofrei", "unbegrenzt" are present in the prompt or expected outcomes?
* [ ] **Is the Grounding & Insufficient Content Policy satisfied?** Does your function fall under Category A (strictly require webpage content, throw `INSUFFICIENT_CONTENT` on weak source) or Category B (support Search Grounding, explicitly enabled in `EngineCapabilities`)? Ensure Grounding is never used to fake missing source data!
* [ ] **Is the registry mapping defined?** Are both `AnalysisType` and `AnalysisRegistryImpl` mappings configured without touching the Orchestration Core?

---

## 6. Known Technical Debt (Post-Freeze Backlog)

These existing compiler warnings and non-blocking elements are documented as technical debt and are allowed to persist for now, to be checked and addressed in a future task:

* **Kotlin Annotation Warns (`GeminiModels.kt`):** Compiler warnings concerning annotation default targets (`-Xannotation-default-target=param-property`) on serialization parameters.
* **MainViewModel `always-true` Warning:** Line 846 instance check warning in compilation output.
* **Test Deprecations (`ExampleRobolectricTest.kt`):** Warning on OkHttp `ResponseBody.create(contentType, content)` usage in unit mock setups.

---

## 7. Architectural Verification Check

| Architecture Audit Dimension | Status | Notes / Verification |
|---|---|---|
| **1. Direct Engine Calls Outside Runner** | 🟢 **PASS (0 Calls)** | All legacy direct calls removed. Unit tests and repository completely cleaned up. |
| **2. GeminiRepository Gateway Purity** | 🟢 **PASS** | Repository is now a pure transport client. All legacy `summarize` logic is deleted. |
| **3. Orchestration Lean State** | 🟢 **PASS** | `AnalyzeContentUseCase` only contains `resolve -> run -> return` flow. |
| **4. Registry Purity** | 🟢 **PASS** | `AnalysisRegistry` contains only mappings and no functional logic or capability flags. |
| **5. EngineContract Enforceability** | 🟢 **PASS** | Every execution path in `EngineRunnerImpl` asserts contract bounds (Input/Output Schemas and SemVer checks). |
| **6. Hard-Fail on Contract Violation** | 🟢 **PASS** | Validation rules throw explicit runtime exceptions on schema mismatch or version invalidity. |
| **7. Build Stability & Regression Tests** | 🟢 **PASS** | All JVM architecture validation tests and live integration suites compile and pass green. |

### Final Conclusion:
> **ARCHITECTURE FREEZE ACCEPTED:** Yes, the Global Analytical Engine (GAA) plugin system is fully hardened, completely decoupled, and officially frozen.

---

## 8. GAIS Workspace & Repository Governance Integration

All architectural modifications, engine plugin additions, and prompt updates are subject to the following mandatory workspace governance rules:

1. **Canonical Root & Path Mapping**:
   * The visible workspace root is strictly `/`. All relative paths in contracts, tasks, and documentation must resolve from `/`.
   * Internal container paths (e.g., `/app/applet` or concatenated variants) are strictly forbidden in file operations and tooling.
2. **Git Health Gate**:
   * Prior to any architectural modification or prompt change, `git fsck --full` and `git status --short` must be executed.
   * If any repository corruption is detected, all file operations must STOP immediately.
3. **Allowlist Enforcement & Asset Protection**:
   * Code, prompt, or configuration changes must operate strictly within an authorized file allowlist for the given task.
   * Prompts (`app/src/main/assets/prompts/`), icon assets, drawables, manifests, build scripts, and database schema files are strictly protected.
   * Binary assets must never be modified or re-encoded without explicit authorization and SHA-256 verification.
