Status: AUDIT COMPLETE - REPOSITORY WARNING
Erstellt: 2026-08-02T14:55:00Z
Datum: 2026-08-02
Uhrzeit: 14:55:00
Zeitzone: UTC
Autor: GAIS (Google AI Studio Build Agent)
Projekt: RELEVANTOR
Projekt-Root: /app/applet
App-Modul: /app/applet/app
Git-Branch: main
Workspace: /app/applet
SHA-256: [WIRD_IN_AUDIT_TABELLE_ERFASST]
Dokumentversion: 1.0.0

# CP-00 Repository Integrity & Workspace Audit
## Sicherheits-Audit für den RELEVANTOR-Workspace

- **Status**: **REPOSITORY WARNING**
- **Datum**: 2026-08-02
- **Uhrzeit**: 14:55:00 UTC
- **Autor**: GAIS
- **Ziel-Datei**: `/app/applet/docs_md/CP00_REPOSITORY_INTEGRITY_AUDIT_2026-08-02_14-55.md`

---

## Executive Summary

Im Rahmen der Sicherheitsmaßnahme **CP-00 (Repository Integrity & Workspace Audit)** wurde der gesamte Arbeitsbereich der RELEVANTOR-Anwendung forensisch analysiert. 

### Haupt-Erkenntnisse:
1. **Eindeutiger Projekt-Root**: Der gültige, primäre Projekt-Root ist unmissverständlich `/app/applet`.
2. **Geisterverzeichnis identifiziert**: Es existiert ein Geisterverzeichnis unter `/app/applet/app/applet` sowie ein tief verschachteltes Geister-Dokumentenverzeichnis unter `/app/applet/app/applet/docs_md`.
3. **Deplatzierte Dokumente**: Die Vorarbeiten zum Change Prompt 03 (`CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_2026-08-02.md`, `CP03_FUNCTION_DESIGN_GOOGLE_MAPS_LOCATION_QUERY_2026-08-02.md` und `CP03_GOOGLE_MAPS_LOCATION_QUERY_BASELINE_2026-08-02.md`) wurden versehentlich im Geisterpfad `/app/applet/app/applet/docs_md` abgelegt, da ein früheren Tool-Aufruf aus dem CWD `/app/applet/app` heraus mit dem relativen Pfad `applet/docs_md` erfolgte.
4. **Keine Produktionscode-Beschädigung**: Der Quellcode unter `/app/applet/app/src` ist absolut sauber, unbeschädigt und voll funktionsfähig.
5. **Gesamtbewertung**: **`REPOSITORY WARNING`** aufgrund von Pfadverwirrungen und Geisterordner-Existenz.

---

## Teil 1 – Projekt-Root eindeutig bestimmen

Eine umfassende Prüfung der Systemumgebung und Git-Konfiguration ergab folgende Messwerte:

| Parameter | Ermittelter Wert | Status / Bewertung |
| :--- | :--- | :--- |
| **Physical Working Directory (`pwd -P`)** | `/app/applet` | **KORREKT** |
| **Logical Working Directory (`pwd`)** | `/app/applet` | **KORREKT** |
| **Git Repository Root** | `/app/applet` | **KORREKT** |
| **Android-Projektroot** | `/app/applet` | **KORREKT** (Enthält `settings.gradle.kts`, `build.gradle.kts`) |
| **App-Modul** | `/app/applet/app` | **KORREKT** (Enthält Modul-`build.gradle.kts` und `src/`) |
| **SourceSet Main** | `/app/applet/app/src/main` | **KORREKT** |
| **Gradle Root** | `/app/applet` | **KORREKT** |
| **Aktueller Git Branch** | `main` | **STABIL** |

**Prüfung auf mehrere Projekt-Roots**:
Es existiert nur ein einziges valides Gradle- / Git-Root-Verzeichnis unter `/app/applet`. 

---

## Teil 2 – Verzeichnisstruktur vollständig prüfen

Die Prüfung der vordefinierten Schlüsselpfade zeigt folgendes Ergebnis:

| Pfad | Existiert | Ist Verzeichnis | Anmerkung / Status |
| :--- | :---: | :---: | :--- |
| `/app/applet` | **Ja** | **Ja** | Primärer Projekt-Root |
| `/app/applet/app` | **Ja** | **Ja** | Haupt-App-Modul |
| `/app/applet/docs_md` | **Ja** | **Ja** | Primärer Dokumentations-Ordner |
| `/app/applet/assets` | **Ja** | **Ja** | Statische Assets |
| `/app/applet/app/src` | **Ja** | **Ja** | Source Root |
| `/app/applet/app/src/main` | **Ja** | **Ja** | Main SourceSet |
| `/app/applet/app/src/main/java` | **Ja** | **Ja** | Kotlin/Java Quellcode |
| `/app/applet/app/src/main/assets` | **Ja** | **Ja** | Prompts & Registry Assets |
| `/app/applet/app/src/main/res` | **Ja** | **Ja** | Android Resoucen |
| `/app/applet/app/src/main/res/drawable` | **Ja** | **Ja** | Vector Drawables |
| `/app/applet/app/src/main/res/drawable-nodpi` | **Ja** | **Ja** | App Icon Vector Assets |

---

## Teil 3 – Suche nach Geisterverzeichnissen

Eine rekursive Suche über das gesamte Dateisystem `/app` deckte folgende Geisterpfade und Verschachtelungsfehler auf:

| Pfad-Muster | Befund | Gefundener Pfad | Beschreibung & Auswirkung |
| :--- | :---: | :--- | :--- |
| `*/app/applet/app/applet*` | **GEFUNDEN** | `/app/applet/app/applet` | **Geisterverzeichnis!** Erzeugt durch relativen Aufruf `applet/` aus `/app/applet/app`. |
| `*/docs_md/*` | **GEFUNDEN** | `/app/applet/app/applet/docs_md` | **Geister-Doku-Ordner!** Enthält die korrigierte CP03 Dry-Run Fassung. |
| `/app/applet/app/app` | Nicht vorhanden | - | Kein doppelter App-Ordner. |
| `/app/applet/docs_md/docs_md` | Nicht vorhanden | - | Kein doppelter docs_md Ordner. |

---

## Teil 4 – Forensische Dokumentationspfad-Prüfung

Sämtliche im Repository auffindbaren `.md`-Dateien (ausgenommen temporäre Gradle-Build-Artefakte) wurden erfasst, gehasht und auf Duplikate untersucht:

### 4.1 Haupt-Dokumentation (`/app/applet/docs_md/`)

| Dateiname | Relative Pfadangabe | Größe (Bytes) | Geändert (UTC) | SHA-256 Hash |
| :--- | :--- | :---: | :---: | :--- |
| `ABSTRACTOR_SYSTEM_STATE.md` | `docs_md/ABSTRACTOR_SYSTEM_STATE.md` | 21.886 | 2026-08-02 14:40:24 | `e0b3990dbf36aa4e4ff9c304da622b5bdf805232aec798bbf4330fa6455fcd68` |
| `GAIS-Architektur_2026-08-02.md` | `docs_md/GAIS-Architektur_2026-08-02.md` | 33.635 | 2026-08-02 14:40:24 | `6e345d5154dcaaf25fb72c2e32ace4e72786be6d60128f4147df41a511a18919` |
| `GAIS-Verzeichnisstruktur_2026-08-02.md` | `docs_md/GAIS-Verzeichnisstruktur_2026-08-02.md` | 23.423 | 2026-08-02 14:40:24 | `718bba214b4e50046c1facfb239488f6f3dd984d54cad24b0ee7e7eb08e6a6e3` |
| `GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md` | `docs_md/GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md` | 12.607 | 2026-08-02 14:40:24 | `95bafed932ad6f952cffd56e6cc716aa6dcf632bc12411972a62a3fb3618fdce` |
| `GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md` | `docs_md/GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md` | 8.667 | 2026-08-02 14:40:24 | `b927b0535cea9e15dcd2cdcee96eac169fd0c866c06d36d553d83d7e89db1a79` |
| `GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md` | `docs_md/GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md` | 5.747 | 2026-08-02 14:40:24 | `fd9499f1433016c1b997dc972df0763e606ab3e248197aa701b3dd13a7585208` |
| `GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md` | `docs_md/GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md` | 2.207 | 2026-08-02 14:40:24 | `3875d290842e696d7d7ce54c701b048cd4c5e287f1915704516ad70f011b395f` |
| `GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md` | `docs_md/GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md` | 5.983 | 2026-08-02 14:40:24 | `4d24435ce287e9bba1b4867cc1f2d1457f023cbfe123c5271759ec1a69aa98ef` |
| `LOCAL_BUILD_HANDOFF.md` | `docs_md/LOCAL_BUILD_HANDOFF.md` | 5.271 | 2026-08-02 14:40:25 | `91af3df84bff495d87e283be3f9e2ab7749c234d3f3e0699a757410fbad72f82` |
| `PROJECT_CONTEXT_RELEVANTOR.md` | `docs_md/PROJECT_CONTEXT_RELEVANTOR.md` | 6.972 | 2026-08-02 14:40:25 | `098a72ca56c59068b9eefa3fec3cd3feec1b4453e3e121065f8c96415589b805` |
| `RELEVANTOR_ARCHITECTURE.md` | `docs_md/RELEVANTOR_ARCHITECTURE.md` | 8.726 | 2026-08-02 14:40:25 | `6343ec67a0ec8d8239ed589cd45126e494e534be549d98881d141d164bccca85` |
| `RELEVANTOR_BASELINE_LOCAL_FIRST.md` | `docs_md/RELEVANTOR_BASELINE_LOCAL_FIRST.md` | 12.663 | 2026-08-02 14:40:25 | `e07449bbf67a8ab88ca9c49cc00fc0297d369c97de6a19fb1b2beb48a1369849` |
| `RELEVANTOR_DEVELOPMENT_STATUS.md` | `docs_md/RELEVANTOR_DEVELOPMENT_STATUS.md` | 3.446 | 2026-08-02 14:40:25 | `559d8a8db5bd04a1607c7e020b9a8446c2d02ea59d33924b5d1d910dc23bb8a0` |
| `RELEVANTOR_FUNCTION_EXECUTION_MODEL.md` | `docs_md/RELEVANTOR_FUNCTION_EXECUTION_MODEL.md` | 17.584 | 2026-08-02 14:40:25 | `9f4c1ca60bcd6240bfdd46a04a22012bd18cf2f5c4786309b11367c029cc0b5d` |
| `RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md` | `docs_md/RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md` | 32.844 | 2026-08-02 14:40:24 | `4d98af6b82597bdd8f7ec1c5f9b3d602dcfdcc5fd2e09797da10105ee829c1de` |
| `RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md` | `docs_md/RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md` | 9.311 | 2026-08-02 14:40:25 | `9588716f10dcfb80c32bfcc3073dd33355feff6437a4289b6afb9e66c03fde9a` |
| `RELEVANTOR_OUTPUT_SPEC.md` | `docs_md/RELEVANTOR_OUTPUT_SPEC.md` | 16.632 | 2026-08-02 14:40:24 | `3cf1e09bb0db26b7f7bf34065459705805dd983185a024a92f023cd93b0a225a` |
| `RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` | `docs_md/RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` | 15.485 | 2026-08-02 14:40:25 | `1d5cb7d56510a20022e3b37812d491ec7feb2578d9db80be8f3c00165d4f08ee` |
| `RELEVANTOR_RESOURCE_FORENSIC_AUDIT_2026-08-02.md` | `docs_md/RELEVANTOR_RESOURCE_FORENSIC_AUDIT_2026-08-02.md` | 10.695 | 2026-08-02 14:40:24 | `16f51d8fcb8bb44905f09482f2bdae1c18fa64e69a010471b3b6757d5a7a5749` |
| `RELEVANTOR_SELF_TEST_MATRIX.md` | `docs_md/RELEVANTOR_SELF_TEST_MATRIX.md` | 4.879 | 2026-08-02 14:40:24 | `250e6108c560fb5400451bbab5359f1e0f60a354999f3164dfb7f2c4886789cc` |
| `RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md` | `docs_md/RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md` | 25.356 | 2026-08-02 14:40:24 | `f8a03b0fc7e1b52de5124dbe4e95a368d38c7631f8239e1d2cf189d2090cce26` |
| `RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md` | `docs_md/RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md` | 14.114 | 2026-08-02 14:40:24 | `58d19b06b5f694273cf1a5f3aec8bd414387c1e68d7aedb030b6e65e3f9f1628` |
| `TEST_COVERAGE_MATRIX.md` | `docs_md/TEST_COVERAGE_MATRIX.md` | 1.483 | 2026-08-02 14:40:25 | `bbd8bac6d6754339c4398678f2a7a277b42568be9e1efa7b774a69cea4ea4671` |
| `ZUSAMMENFASSUNG_ARBEITEN.md` | `docs_md/ZUSAMMENFASSUNG_ARBEITEN.md` | 9.808 | 2026-08-02 14:40:24 | `646069610d8d604f0468b6afacd59e75dcaa2cfc19c93d5b01b0c89405ebf4d4` |
| `verzeichnisstruktur-und-dateien.md` | `docs_md/verzeichnisstruktur-und-dateien.md` | 17.116 | 2026-08-02 14:40:24 | `79d0c5c4c2a764673a98a37f47de4a4e4367ee8ed3ef2b3837f0057fa2531dc9` |

### 4.2 Geisterpfad-Dokumente (`/app/applet/app/applet/docs_md/`)

| Dateiname | Voller Pfad | Größe (Bytes) | Geändert (UTC) | SHA-256 Hash |
| :--- | :--- | :---: | :---: | :--- |
| `CP03_GOOGLE_MAPS_LOCATION_QUERY_BASELINE_2026-08-02.md` | `/app/applet/app/applet/docs_md/...` | 2.391 | 2026-08-02 14:40:24 | `67a57848570475d79980128428fc493158ae9b70645b6c8a5530bf6734cd2f9d` |
| `CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_2026-08-02.md` | `/app/applet/app/applet/docs_md/...` | 17.445 | 2026-08-02 14:40:25 | `50814c675857e51b70fb2c0846d4c76606d5239b9e774d197a22cc58544dd15a` |
| `CP03_FUNCTION_DESIGN_GOOGLE_MAPS_LOCATION_QUERY_2026-08-02.md` | `/app/applet/app/applet/docs_md/...` | 20.639 | 2026-08-02 14:40:25 | `e2426a45f7044e67b0e9d59d67fd05ff1f2a2292bd18fddb9a308f76c5a2c0ca` |

### 4.3 Deplatzierte Einzel-Dokumente

| Dateiname | Absoluter Pfad | Anmerkung |
| :--- | :--- | :--- |
| `GEMINI_429_TRUE_CAUSE_REPORT.md` | `/app/applet/app/GEMINI_429_TRUE_CAUSE_REPORT.md` | Deplatziert im Modulordner `app/`. Identisch mit `/app/applet/GEMINI_429_TRUE_CAUSE_REPORT.md`. |

---

## Teil 5 – Geschützte Arbeitsbereiche definieren

Zur Verhinderung künftiger Pfadverwirrungen werden folgende verbindliche Arbeitsbereichs-Grenzen festgelegt:

| Arbeitsbereich | Zuverlässiger Pfad (relativ zu PROJECT_ROOT) | Absoluter Pfad | Zweck & Schutzregeln |
| :--- | :--- | :--- | :--- |
| **Produktionscode** | `app/src/main/java` | `/app/applet/app/src/main/java` | Ausschließlicher Ablageort für Kotlin/Java-Sourcecode. **Keine Doku/Skripte**. |
| **Prompts** | `app/src/main/assets/prompts` | `/app/applet/app/src/main/assets/prompts` | System- & Funktionsprompts sowie Manifests. Nur valides Markdown & JSON. |
| **Ressourcen** | `app/src/main/res` | `/app/applet/app/src/main/res` | Layouts, Values, Drawables, Strings, XML. |
| **Dokumentation** | `docs_md` | `/app/applet/docs_md` | Technische Berichte, FDS, Dry-Run Reports, Audits. |
| **Assets** | `assets` | `/app/applet/assets` | Statische Projekt-Assets. |
| **Tests** | `app/src/test` / `app/src/androidTest` | `/app/applet/app/src/test` | Unit- & Robolectric-Tests. |

---

## Teil 6 – Pfadregeln definieren

Verbindliche Projektregeln für GAIS:

1. **PROJECT_ROOT Verbindlichkeit**: `PROJECT_ROOT` ist strikt als `/app/applet` definiert.
2. **Verbot manueller Pfadketten-Zusammensetzung**: GAIS darf künftig **niemals** Pfadstrings frei zusammenschneiden oder relative Sprünge (`../../applet/...`) nutzen.
3. **Absolutpfad-Gebot**: Alle Zielpfade bei Tool-Aufrufen (`create_file`, `edit_file`, `view_file`) müssen mit `/app/applet/` beginnen.
4. **CWD-Gebot bei Tool-Aufrufen**: Befehle via `run_command` müssen stets mit `Cwd: "/app/applet"` ausgeführt werden.
5. **Ausschluss von Verschachtelungen**: Pfade wie `app/applet/...` oder `applet/docs_md/...` sind **STRIKT VERBOTEN**.

---

## Teil 7 – Standardheader für alle MD-Dateien

Künftig erzeugte Markdown-Dateien MÜSSEN zwingend folgenden Header am Dateianfang enthalten:

```markdown
Status: [PASS / WARNING / DRAFT / IN_PROGRESS]
Erstellt: [YYYY-MM-DDTHH:MM:SSZ]
Datum: [YYYY-MM-DD]
Uhrzeit: [HH:MM:SS]
Zeitzone: UTC
Autor: GAIS (Google AI Studio Build Agent)
Projekt: RELEVANTOR
Projekt-Root: /app/applet
App-Modul: /app/applet/app
Git-Branch: main
Workspace: /app/applet
SHA-256: [AUTOMATISCH_ERMITTELT]
Dokumentversion: 1.0.0
```

---

## Teil 8 – Repository Health Check

Vor jedem künftigen Change Prompt (CP-01, CP-03, CP-07) muss folgende automatische Prüf-Matrix bestanden werden:

- [x] **Projektroot korrekt**: `/app/applet` ist vorhanden und enthält `settings.gradle.kts`.
- [x] **App-Modul korrekt**: `/app/applet/app` ist vorhanden und enthält `build.gradle.kts`.
- [x] **Promptpfade korrekt**: `app/src/main/assets/prompts` existiert und ist belegt.
- [x] **Ressourcenpfade korrekt**: `app/src/main/res` ist vorhanden.
- [x] **docs_md korrekt**: `/app/applet/docs_md` ist primärer Ablageort.
- [x] **Assets korrekt**: `/app/applet/assets` ist vorhanden.
- [ ] **Keine Geisterpfade**: **Achtung!** `/app/applet/app/applet` existiert aktuell noch!
- [x] **Keine doppelten Projekt-Roots**: Kein zweites Git/Gradle Root im Tree.
- [x] **Arbeitsverzeichnis korrekt**: CWD ist `/app/applet`.

---

## Teil 9 – Empfehlungen & Risikomatrix

| Risiko | Ursache | Auswirkung | Wahrscheinlichkeit | Empfohlene dauerhafte Lösung |
| :--- | :--- | :--- | :---: | :--- |
| **R-01: Geisterordner `/app/applet/app/applet`** | Werkzeug-Aufruf mit relativem Pfad `applet/docs_md` aus `/app/applet/app` heraus. | Dokumente landen im Geisterordner und werden im Haupt-Ordner `/app/applet/docs_md` nicht gefunden. | **Hoch** (bereits eingetreten) | **Empfehlung**: Nach Freigabe Verschiebung aller CP03-Dateien von `/app/applet/app/applet/docs_md/` nach `/app/applet/docs_md/` und anschließende Löschung von `/app/applet/app/applet`. |
| **R-02: Deplatzierte Datei in `app/`** | Manuelle Erstellung im Modulordner. | Unordnung im App-Modulordner. | **Mittel** | Verschieben der `app/GEMINI_429_TRUE_CAUSE_REPORT.md` nach `debug_archive`. |
| **R-03: Pfadsynthese durch KI-Agenten** | Zusammenbau von relativen Pfaden im Agenten-Prompting. | Wiederkehr von Geisterpfaden. | **Mittel** | Striktes Einhalten der Pfadregeln aus Teil 6 (stets `/app/applet/...`). |

---

## Abschlussstatus

**`REPOSITORY WARNING`**

*(Begründung: Das Repository weist eine saubere Codebase auf, enthält jedoch durch frühere relative Pfadaufrufe ein aktives Geisterverzeichnis `/app/applet/app/applet/docs_md` mit den CP03-Dokumenten sowie eine deplatzierte Datei unter `/app/applet/app/GEMINI_429_TRUE_CAUSE_REPORT.md`.)*
