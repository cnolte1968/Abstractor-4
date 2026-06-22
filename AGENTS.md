# Project-Specific Agent Instructions for ABSTRACTOR

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
- 