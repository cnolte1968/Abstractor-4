# Project-Specific Agent Instructions for RELEVANTOR

This file contains persistent constraints and instructions that the AI Studio Agent must adhere to during all code editing, building, and deployment operations.

## 1. Zero-Risk Deployment & Build Constraints
- **GAIS Role Limit**: Use Google AI Studio (GAIS) exclusively as a code analysis, code writing, and build compilation tool.
- **Single Installable Source**: The only valid, compiled, and installable build artifact is located at:
  `/app/build/outputs/apk/debug/app-debug.apk`
- **Strict WebUSB / WebInstall Prohibition**: Do NOT attempt to run WebUSB installs, browser-based flash operations, or GAIS install bridge actions.
- **External Distribution Only**: Installs must be performed externally via manual transfer (e.g., USB transfer, Google Drive, or ADB local CLI wrapper).
- **Physical Device Testing Only**: App behavior, lifecycle, and native capabilities must be tested solely on real hardware, as there is no active emulator in the agent sandbox.

## 2. Signing Stability
- Do NOT regenerate, overwrite, or modify `/debug.keystore.base64` or the signing configuration within `/app/build.gradle.kts` unless directly asked. 
- Maintain certificate fingerprint consistency. 

## 3. Prompt Governance & Protection Rules
- **Protected Directory**: `app/src/main/assets/prompts/` is a production system directory.
- **Prohibited Automated Actions**: Prompts in this directory must NEVER be automatically archived, moved, deleted, renamed, or merged.
- **Change Authorizations**: Changes to prompt files occur EXCLUSIVELY via CP-01 (Prompt Optimization) or explicit user change requests with clear authorization.
- **Mandatory Pre-Check**: Before any prompt modification:
  1. Verify references in `prompt_manifest.json`, `function_registry.json`, `AnalysisRegistry`, engines, and coordinators.
  2. Verify active usage and downstream dependencies.
  3. Classify change type (CP-01 Optimization, CP-03 New Feature, or Technical CP).
- **Quality Verification**: Verify output format, JSON validity, and ensure no regressions on existing capabilities. 

## 4. Canonical Project Paths & Mapping
- **Canonical Root**: The visible project root is strictly `/`. All file operations, task descriptions, and allowlists must specify paths relative to `/`.
- **App Module Path**: The Android application module is located at `/app/`.
- **Prohibited Container Paths**: Internal container paths such as `/app/applet` or concatenated artifacts like `/app/applet/app/applet` must NEVER be used in file tools, task descriptions, or prompt instructions.

## 5. Git Health Gate & Operation Rules
- **Mandatory Pre-Check**: At the start of every new session and prior to any critical write or Git action, run `git fsck --full` and `git status --short`. If corruption or repository error is detected, STOP immediately without modifying any files.
- **No Automated Git Operations**: GAIS is strictly prohibited from running `git add`, `git commit`, `git push`, `git pull`, or automated Git closures. Staging, committing, and pushing occur exclusively via the user in the AI Studio GitHub UI.

## 6. Strict File Allowlist & Protected Assets
- **Task Allowlist Constraint**: Every change task must operate strictly within an explicit, pre-authorized file allowlist. Unapproved file modifications are strictly prohibited and classified as task failures.
- **Protected File Categories**: Prompts (`app/src/main/assets/prompts/`), launcher/icon resources, background image assets, AndroidManifest.xml, build.gradle.kts files, and database schema files are strictly protected and require explicit change authorization.
- **Binary Asset Protection**: Binary assets (PNG, WEBP, ZIP, Keystore) must NEVER be automatically re-encoded, optimized, or overwritten. Unmodified binary files must remain byte-for-byte and SHA-256 identical before and after tasks.

## 7. Task Completion & Integrity Gate
- Verify all modified files against the authorized task allowlist.
- Perform a final `git fsck --full` health check.
- Execute standard compilation build (`compile_applet`) to verify technical stability.

## 8. GAIS Working Standards
- **Binding Reference**: The file `app/src/main/assets/GAIS-Arbeitsstandards-Prompt.md` defines the binding working standards and governance process for all daily GAIS operations.
