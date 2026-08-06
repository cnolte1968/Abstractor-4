# WORKSPACE INVENTORY REPORT

**Timestamp:** 2026-08-03 08:54:42 UTC  
**Environment:** Google AI Studio (GAIS) Build Environment  
**Status:** `INVENTORY PASS`

---

## 1. Verzeichnisinventur (Physisch vorhandene Struktur)

Die physische Analyse der Ordnerstruktur auf Workspace-Ebene ergibt folgende vorhandene Haupt- und Systemverzeichnisse:

| Verzeichnis / Pfad | Status / Beschreibung |
|---|---|
| `/app` | Haupt-Modulordner des Android-Projekts (Kotlin source, resources, build definitions) |
| `/assets` | Bild- und Icon-Ressourcen (z. B. App-Icons V1, V2, V3) |
| `/build` | Gradle-Build-Verzeichnis der Root-Ebene (enthält `./build/reports/`) |
| `/build_metadata` | Platform Build Metadata (`EXPORT_VERIFICATION.txt`, `metadata.json`) |
| `/.build-outputs` | GAIS-Plattform-Ausgabeordner (enthält `app-debug.apk`) |
| `/debug_archive` | Archiv historischer Debug- und Diagnoseberichte |
| `/docs_md` | Zentrale Dokumentationsablage (55 Markdown- / Textdateien) |
| `/.git` & `/.github` | Git-Repository-Metadaten und CI/CD Workflows (`build-apk.yml`) |
| `/.gradle` & `/gradle` | Gradle-Cache und Gradle Version Catalog / Properties |
| `/.kotlin` | Kotlin-Compiler-Cache |
| `/recovery_backup_2026-07-20` | Sicherungsverzeichnis (Backup Ghost Directory / Manifest) |
| `/recovery_backup_2026-08-02` | Sicherungsverzeichnis (Recovery Backups Icon & MainActivity) |

---

## 2. Build- und APK-Inventur

Exakte Prüfung der Build-Ordner und APK-Standorte im Workspace:

1. **Existenz von `build/` im Projektroot:**
   - **Ja**, das Verzeichnis `/build/` ist physisch im Projektroot vorhanden (`./build/reports/`).

2. **Existenz von `app/build/` im App-Modul:**
   - **Ja**, das Modul-Build-Verzeichnis `./app/build/` ist physisch vorhanden (`./app/build/outputs/apk/debug/app-debug.apk`).

3. **Physisch vorhandene APK-Ausgabepfade (`app-debug.apk`):**
   - `./app/build/outputs/apk/debug/app-debug.apk` (Physisch vorhanden, Standard-Gradle-Build-Pfad)
   - `./.build-outputs/app-debug.apk` (Physisch vorhanden, GAIS-Plattform-Exportpfad)
   - `./app-debug.apk` (Physisch vorhanden im Projektroot)

---

## 3. Klassifikation aller Dateien unter `docs_md/`

Insgesamt wurden **55 Dateien** in `docs_md/` analysiert und klassifiziert:

### Kategorie A: AKTIVE PRIMÄRQUELLE (11 Dateien)
*Architektur, Projektkontext, Governance und aktive Master-Statusdokumente*

1. `RELEVANTOR_VERIFIED_PROJECT_BASE_CORRECTED_2026-08-03_08-07-20_UTC.md` - Gültige, korrigierte Projektbasis
2. `GAIS-Architektur_2026-08-02.md` - Zentrale GAIS-Systemarchitektur
3. `GAIS-Verzeichnisstruktur_2026-08-03.md` - Aktuelle Master-Verzeichnisstruktur
4. `RELEVANTOR_WORKSPACE_MASTER_V1_3_2026-08-03_04-10-00_UTC.md` - Master-Workspace-Status v1.3
5. `PROJECT_CONTEXT_RELEVANTOR.md` - Globaler Projektkontext
6. `RELEVANTOR_ARCHITECTURE.md` - Relevantor App-Architektur
7. `RELEVANTOR_BASELINE_LOCAL_FIRST.md` - Governance & Local-First Prinzipien
8. `RELEVANTOR_DEVELOPMENT_STATUS.md` - Aktueller Modul-Entwicklungsstand
9. `RELEVANTOR_FUNCTION_EXECUTION_MODEL.md` - Funktions-Ausführungsmodell
10. `RELEVANTOR_OUTPUT_SPEC.md` - Output-Formate & UI Specifications
11. `TEST_COVERAGE_MATRIX.md` - Aktuelle Testabdeckungsmatrix

---

### Kategorie B: AKTIVER NACHWEIS (14 Dateien)
*Aktuelle Validierungs- und Testberichte sowie abgeschlossene Change-Dokumentation*

1. `CP00_POST_ICON_RESTORE_VALIDATION_2026-08-03_04-39-16_UTC.md` - Validierungsbericht Icon-Restore
2. `CP00_RUNTIME_REGRESSION_AUDIT_2026-08-03_04-25-00_UTC.md` - Laufzeit-Regressionsaudit
3. `CP03_PHASE6_E2E_TEST_2026-08-03_03-25-00_UTC.md` - E2E Testnachweis CP-03
4. `GITHUB_BASELINE_CP03_V13_2026-08-03_04-12-00_UTC.md` - Baseline V1.3 Audit
5. `GITHUB_CHECKPOINT_CP03_FINAL_2026-08-03_03-55-00_UTC.md` - Final Checkpoint CP-03
6. `GITHUB_ICON_RESTORE_CHECKPOINT_2026-08-03_04-41-46_UTC.md` - Icon Restore Checkpoint
7. `GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md` - Qualitätsverifizierung Output
8. `GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md` - Verifizierte Plattformfähigkeiten
9. `LOCAL_BUILD_HANDOFF.md` - Lokaler Übergabebericht
10. `NEW_CHAT_BOOTSTRAP_ANALYSIS_2026-08-03_08-23-30_UTC.md` - Chat-Bootstrap-Analyse
11. `README_INSTALL.txt` - Installationshinweise
12. `RELEVANTOR_CONTEXT_RECOVERY_2026-08-03_07-52-56_UTC.md` - Wiederherstellungsnachweis Kontext
13. `RELEVANTOR_SELF_TEST_MATRIX.md` - Selbsttest-Ergebnisse
14. `RELEVANTOR_VERIFIED_PROJECT_BASE_2026-08-03_08-01-26_UTC.md` - Baseline-Schnittstellenbericht

---

### Kategorie C: HISTORISCH / ARCHIVIERBAR (30 Dateien)
*Historische Phasen-Dokumente, gelöste Fehlerdiagnosen, alte Verzeichnislistings und Zwischenstände*

1. `ABSTRACTOR_SYSTEM_STATE.md` - Veralteter Systemzustand vor Umstellung
2. `CP00_GAIS_ERROR_FORENSIC_AUDIT_2026-08-03_04-00-00_UTC.md` - Behobenes Fehler-Audit
3. `CP00_GHOST_DIRECTORY_AUDIT_2026-08-03_03-48-00_UTC.md` - Erledigter Ghost-Directory Audit
4. `CP00_GHOST_DIRECTORY_CLEANUP_2026-08-03_03-52-00_UTC.md` - Erledigter Cleanup-Bericht
5. `CP00_ICON_FORENSIC_ANALYSIS_2026-08-03_04-30-00_UTC.md` - Abgeschlossene Icon-Forensik
6. `CP00_ICON_RESTORE_2026-08-03_04-35-00_UTC.md` - Historischer Restore-Bericht
7. `CP00_POST_PHASE6_INTEGRITY_AUDIT_2026-08-03_03-30-00_UTC.md` - Veraltetes Integrity Audit
8. `CP00_REPOSITORY_INTEGRITY_AUDIT_2026-08-02_14-55.md` - Historisches Repository-Audit
9. `CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-16-24_UTC.md` - Dry-Run Versuch 1
10. `CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-26-03_UTC.md` - Dry-Run Versuch 2
11. `CP03_LOCATION_QUESTION_PHASE1_PLANNER_2026-08-02_15-32-14_UTC.md` - Phasenprotokoll Phase 1
12. `CP03_PHASE2_REGISTRATION_2026-08-03_02-28-20_UTC.md` - Phasenprotokoll Phase 2
13. `CP03_PHASE3_PRECHECK_2026-08-03_02-33-15_UTC.md` - Phasenprotokoll Phase 3
14. `CP03_PHASE4_COORDINATOR_2026-08-03_02-48-00_UTC.md` - Phasenprotokoll Phase 4
15. `CP03_PHASE5_CHANGE_AUDIT_2026-08-03_03-15-00_UTC.md` - Phasenprotokoll Phase 5 Audit
16. `CP03_PHASE5_ENGINE_2026-08-03_03-05-00_UTC.md` - Phasenprotokoll Phase 5 Engine
17. `GAIS-Verzeichnisstruktur_2026-08-02.md` - Veraltete Struktur vom Vortag
18. `GITHUB_BACKUP_CP03_PHASE5_2026-08-03_03-18-00_UTC.md` - Historisches Phase-5 Backup
19. `GITHUB_FINAL_STAGING_AUDIT_CP03_2026-08-03_03-41-00_UTC.md` - Staging Audit
20. `GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md` - Frühe GAIS-Analyse
21. `GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md` - Erledigte Architektur-Korrektur
22. `GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md` - Veraltetes Agent-Verifikationsprotokoll
23. `RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md` - Veralteter Export
24. `RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md` - Behobene Icon-Diagnose
25. `RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` - Veralteter Projekt-Export
26. `RELEVANTOR_RESOURCE_FORENSIC_AUDIT_2026-08-02.md` - Erledigtes Ressourcen-Audit
27. `RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md` - Historischer Zustand Juli 2026
28. `RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md` - Veralteter State 2026-08-02
29. `ZUSAMMENFASSUNG_ARBEITEN.md` - Allgemeine/generische Zusammenfassung
30. `verzeichnisstruktur-und-dateien.md` - Redundantes Verzeichnislisting

---

### Kategorie D: NICHT EINDEUTIG (0 Dateien)
*Keine unklaren Dateien vorhanden; alle 55 Dokumente konnten eindeutig zugewiesen werden.*

---

## 4. Vorschlag für zukünftige Archivstruktur

Um die Übersichtlichkeit in `docs_md/` dauerhaft hochzuhalten, wird folgende Unterstruktur zur optionalen späteren Bereinigung vorgeschlagen (ohne in dieser Phase Änderungen durchzuführen):

```text
docs_md/
├── active/              # Primärquellen (Kategorie A)
├── evidence/            # Aktive Nachweise & Testberichte (Kategorie B)
└── archive/             # Historische Berichte & Diagnosen (Kategorie C)
    ├── phase_audits/    # CP00 / CP03 Zwischenstände & Audits
    ├── forensics/       # Icon-, Ghost- & Error-Analysen
    └── legacy_exports/  # Alte Statusberichte & Exporte
```

---

## Status
**`INVENTORY PASS`**
