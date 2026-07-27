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

> [!IMPORTANT]
> **Specification Mandatory:** Every new analytical function must be fully specified and approved using the official [FUNCTION_SPEC_TEMPLATE.md](/FUNCTION_SPEC_TEMPLATE.md) baseline template before any code changes are made.

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

---

## 4. Known Technical Debt (Post-Freeze Backlog)

These existing compiler warnings and non-blocking elements are documented as technical debt and are allowed to persist for now, to be checked and addressed in a future task:

* **Kotlin Annotation Warns (`GeminiModels.kt`):** Compiler warnings concerning annotation default targets (`-Xannotation-default-target=param-property`) on serialization parameters.
* **MainViewModel `always-true` Warning:** Line 846 instance check warning in compilation output.
* **Test Deprecations (`ExampleRobolectricTest.kt`):** Warning on OkHttp `ResponseBody.create(contentType, content)` usage in unit mock setups.

---

## 5. Architectural Verification Check

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
