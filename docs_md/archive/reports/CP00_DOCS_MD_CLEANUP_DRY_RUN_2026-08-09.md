# CP-00 Docs_MD Bereinigung Dry-Run (Klassifizierung & Archivierungs-Vorschlag)

**Datum / Zeit:** 2026-08-09 17:56:40 ICT  
**Durchgeführt von:** GAIS (Google AI Studio)  
**Projekt:** Relevantor  
**Status:** Rein lesende Trockenübung (Dry-Run) – 0 Dateiänderungen / 0 Verschiebeaktionen  

---

## 1. Ausgangslage & Analysebereich

- **Haupt-Dokumentationspfad:** `docs_md/`
- **Vorhandenes Archivverzeichnis:** `docs_md/archive/` (enthält derzeit 56 archivierte historische Dokumente)
- **Gesamtzahl analysierter Dateien in `docs_md/`:** 38 Dateien

---

## 2. Klassifizierungsergebnisse

### Kategorie A: Aktive Projektdokumente (Bleiben in `/docs_md/`) — 14 Dateien
Diese Dokumente bilden das aktive Wissens- und Regel-Fundament des Projekts. Sie beschreiben den aktuellen Systemstand, Architektur, Spezifikationen und Governance-Regeln.

| Nr. | Dateiname | Zweck / Begründung |
|---|---|---|
| 1 | `ABSTRACTOR_SYSTEM_STATE.md` | Aktueller Systemstatus-Deskriptor & Abstractor-State |
| 2 | `GAIS-Architektur_2026-08-09.md` | Aktuelle technische Gesamtarchitektur (Stand 09.08.2026) |
| 3 | `GAIS-Verzeichnisstruktur_2026-08-09.md` | Aktuellste verifizierte Verzeichnisstruktur (377 Dateien) |
| 4 | `GAIS_GOVERNANCE_SMOKE_TEST_2026-08-09_17-20-40_ICT.md` | Aktuellster Governance- & Smoke-Test-Bericht |
| 5 | `PROJECT_CONTEXT_RELEVANTOR.md` | Aktueller Projektkontext, Kernfunktionen & Roadmaps |
| 6 | `README_INSTALL.txt` | Aktive Installationsanleitung für manuelle APK-Transfers |
| 7 | `RELEVANTOR_ARCHITECTURE.md` | Grundlegende Architekturspezifikation |
| 8 | `RELEVANTOR_BASELINE_LOCAL_FIRST.md` | Spezifikation der Local-First-Architektur |
| 9 | `RELEVANTOR_DEVELOPMENT_STATUS.md` | Aktueller Entwicklungsstand & Statusbericht |
| 10 | `RELEVANTOR_FUNCTION_EXECUTION_MODEL.md` | Ausführungsmodell der 11 Analysefunktionen |
| 11 | `RELEVANTOR_GAIS_WORKSPACE_RULES.md` | Verbindliche Pfad- & Workspace-Governance-Regeln |
| 12 | `RELEVANTOR_OUTPUT_SPEC.md` | Ausgabe- & JSON-Vertragsspezifikation der LLM-Engines |
| 13 | `RELEVANTOR_SELF_TEST_MATRIX.md` | Matrix für Selbsttests und Funktionsprüfungen |
| 14 | `TEST_COVERAGE_MATRIX.md` | Matrix der Testabdeckung |

---

### Kategorie B: Archiv-Kandidaten (Vorschlag: Verschieben nach `/docs_md/archive/`) — 24 Dateien
Diese Dateien repräsentieren historische Abnahmeberichte, gelöste Troubleshooting-Szenarien, ältere Versionen von Architektur-/Strukturberichten sowie abgeschlossene Checkpoint-Vorbereitungen.

| Nr. | Dateiname | Typ / Grund für Archivierung |
|---|---|---|
| 1 | `CP00_GAIS_PATH_GOVERNANCE_ALIGNMENT_2026-08-08_18-53-33_ICT.md` | Historischer Ausrichtungsbericht Pfad-Governance (08.08.) |
| 2 | `CP00_GHOST_DOCUMENTATION_CLEANUP_2026-08-08_18-38-12_ICT.md` | Historischer Bericht zur Ghost-Ordner-Bereinigung |
| 3 | `CP00_GHOST_PATH_RECOVERY_MVP1C_2026-08-07_08-49-42_ICT.md` | Historischer Pfad-Wiederherstellungsbericht (07.08.) |
| 4 | `CP00_GITHUB_CHECKPOINT_PREPARATION_2026-08-08_16-21-31_ICT.md` | Älterer Checkpoint-Vorbereitungsbericht (08.08.) |
| 5 | `CP00_GITHUB_CHECKPOINT_READY_2026-08-08_18-56-14_ICT.md` | Historischer Checkpoint-Bereitschaftsbericht (08.08.) |
| 6 | `CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-03-39_ICT.md` | Historische Git-Wiederherstellungsstrategie v1 |
| 7 | `CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-17-13_ICT.md` | Historische Git-Wiederherstellungsstrategie v2 |
| 8 | `CP00_MVP1C_STABLE_CHECKPOINT_2026-08-08_15-53-54_ICT.md` | Älterer Checkpoint-Bericht MVP 1C |
| 9 | `CP00_WORKSPACE_DUPLICATE_ROOT_CAUSE_ANALYSIS_2026-08-08_18-31-53_ICT.md` | Ursachenanalyse historisches Pfadproblem |
| 10 | `CP00_WORKSPACE_PATH_ANALYSIS_2026-08-08_16-11-41_ICT.md` | Historische Pfadanalyse |
| 11 | `CP00_WORKSPACE_PATH_VERIFICATION_2026-08-08_16-34-20_ICT.md` | Historische Pfadverifikation |
| 12 | `CP08_BACKEND_ARCHITEKTUR_REVIEW_2026-08-06_05-35-00_UTC.md` | Älteres Backend-Review (06.08.) |
| 13 | `CP08_MVP1B_SUPABASE_SCHEMA_FOUNDATION_2026-08-07_14-25-00_ICT.md` | Historischer Schema-Foundation Bericht |
| 14 | `CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_2026-08-07_14-42-00_ICT.md` | Historischer Dry-Run Bericht MVP 1C v1 |
| 15 | `CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_V2_2026-08-07_14-50-00_ICT.md` | Historischer Dry-Run Bericht MVP 1C v2 |
| 16 | `CP08_MVP1C_APP_SUPABASE_DB_PROOF_IMPLEMENTATION_2026-08-07_14-58-00_ICT.md` | Historischer Implementierungsbericht MVP 1C |
| 17 | `CP08_MVP1C_BACKEND_VERSION_MAPPING_FIX_2026-08-07_15-41-00_ICT.md` | Historischer Fix-Bericht Version-Mapping |
| 18 | `CP08_MVP1C_FINAL_ACCEPTANCE_V2_2026-08-07_08-59-45_ICT.md` | Historischer Final-Acceptance Bericht |
| 19 | `CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md` | Historischer Foundation Dry-Run |
| 20 | `CP08_MVP1_GAIS_SUPABASE_CONNECTIVITY_2026-08-07_13-52-00_ICT.md` | Historischer Konnektivitätsbericht |
| 21 | `GAIS-Architektur_2026-08-07.md` | Vorherige Architektur-Version (ersetzt durch 09.08.) |
| 22 | `GAIS-Verzeichnisstruktur_2026-08-07.md` | Vorherige Verzeichnisstruktur-Version (07.08.) |
| 23 | `GAIS-Verzeichnisstruktur_2026-08-08.md` | Vorherige Verzeichnisstruktur-Version (08.08.) |
| 24 | `GAIS_GOVERNANCE_SMOKE_TEST_2026-08-08_15-44-05_ICT.md` | Vorheriger Smoke-Test-Bericht (ersetzt durch 09.08.) |

---

### Kategorie C: Unklare Dokumente (Manuelle Prüfung erforderlich) — 0 Dateien
- **Keine**. Alle 38 Dateien im Ordner `docs_md/` konnten anhand von Datum, Inhalt und Relevanz eindeutig zugeordnet werden.

---

## 3. Zusammenfassung & Statistik

| Kategorie | Beschreibung | Anzahl Dateien | Prozentsatz |
|---|---|---|---|
| **Kategorie A** | Aktive Projektdokumente (in `docs_md/` behalten) | **14** | 36.8% |
| **Kategorie B** | Archiv-Kandidaten (nach `docs_md/archive/` verschieben) | **24** | 63.2% |
| **Kategorie C** | Unklare Dokumente (Manuelle Entscheidung) | **0** | 0.0% |
| **Gesamt** | Analysierte Dateien im Dokumentationsbereich | **38** | **100.0%** |

---

## 4. Empfehlungen & Nächste Schritte

1. **Aktivierung der Archivierung:** Nach Bestätigung durch den Anforderer kann GAIS in einem separaten Schritt die 24 Dateien der Kategorie B sauber und ohne Nebenwirkungen in das bestehende Archiv `docs_md/archive/` verschieben.
2. **Effekt der Bereinigung:** Reduzierung des aktiven Dokumentationsbereichs von 38 auf 14 schlanke, hochrelevante Kern-Dokumente. Dadurch erhöht sich die Übersichtlichkeit für künftige ChatGPT-Prompts und Kontext-Uploads drastisch.
3. **Sicherheit:** Keinerlei Quellcode-, Backend- oder Build-Dateien sind von dieser Aktion betroffen.
