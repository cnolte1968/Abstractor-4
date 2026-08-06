# NEW CHAT BOOTSTRAP ANALYSE

**Erstellt am:** 2026-08-03 08:23:30 UTC  
**Dokument-ID:** `NEW_CHAT_BOOTSTRAP_ANALYSIS_2026-08-03_08-23-30_UTC`  
**Durchgeführt von:** GAIS  
**Arbeitsmodus:** Reiner Analyse- & Dokumentationsmodus (Keine Code-, Git- oder Systemänderungen)  
**Repository:** `cnolte1968/Abstractor-4`  

---

## 1. Minimal notwendige Kontextbasis

Für den fehlerfreien Neustart eines Entwicklungs-Chats mit GAIS werden ausschließlich verifizierte Primärquellen aus der bestehenden Projektstruktur verwendet. Unbelegte Vermutungen oder widersprüchliche Angaben aus vergangenen Unterhaltungen werden strikt ignoriert.

---

## 2. Klassifikation der Kontextquellen

### A) MUSS übernommen werden
1. **`docs_md/RELEVANTOR_VERIFIED_PROJECT_BASE_CORRECTED_2026-08-03_08-07-20_UTC.md`**
   - **Zweck:** Konsolidierter, korrigierter Ist-Stand des Projekts auf Basis relativer Pfade.
   - **Warum notwendig:** Definiert das gültige Pfadmodell (`app/`, `assets/`, `docs_md/`, `gradle/`) ohne ungesicherte Host-Annahmen und verbindet die gesicherten Fakten aus CP-03.
2. **`docs_md/GAIS-Architektur_2026-08-02.md`**
   - **Zweck:** Technische Architektur- und Systembeschreibung (Clean Architecture + MVVM).
   - **Warum notwendig:** Beschreibt die Schichtentrennung (`com.example.ui`, `com.example.domain`, `com.example.data`), die Modulverträge (`AnalysisEngine`, `AnalysisRegistry`) und das Pipeline-System.
3. **`docs_md/GAIS-Verzeichnisstruktur_2026-08-03.md`**
   - **Zweck:** Vollständiger Verzeichnis- und Dateibaum der sichtbaren Projektebene.
   - **Warum notwendig:** Dient als physikalische Nachweisquelle für alle existierenden Verzeichnisse und verhindert Halluzinationen oder doppelt verschachtelte Ordnerstrukturen.
4. **`AGENTS.md` / `docs_md/RELEVANTOR_WORKSPACE_MASTER_V1_3_2026-08-03_04-10-00_UTC.md`**
   - **Zweck:** Persistent verankerte Arbeits- und Governance-Regeln.
   - **Warum notwendig:** Garantiert den Schutz der App-Identität (`applicationId`), Signierung, Pfadsicherheit und Zero-Risk-Deployment.

---

### B) SOLLTE übernommen werden
1. **`docs_md/RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md`**
   - **Zweck:** Fachlicher Projektkontext, Feature-Katalog und funktionale Anforderungen.
   - **Sinnvoller Nutzen:** Bietet Überblick über alle Analysekategorien (inkl. CP-03 `GOOGLE_MAPS_LOCATION_QUERY`) und geplante Fachfunktionen.
2. **`docs_md/CP00_WORKSPACE_MIGRATION_MANIFEST_2026-08-03_07-13-19_UTC.md`**
   - **Zweck:** Übersicht über bestehende Abweichungen zwischen lokalem Stand und Remote-Repository.
   - **Sinnvoller Nutzen:** Ermöglicht die geplante, kontrollierte Übernahme lokaler Anpassungen bei Bedarf.

---

### C) NICHT übernehmen
1. **Informationen aus alten Chat-Verläufen / Ausführungs-Summaries:**
   - Narrativ behauptete Test- oder Freigabestände ohne protokollierten Emulator-Beweis.
2. **Unbelegte Ursachenhypothesen:**
   - Annahmen über F5-Refreshes, System-Crashs oder Beschädigungsursachen der lokalen Git-Objektdatenbank.
3. **Veraltete / Widersprüchliche Pfadmodelle:**
   - Absolute Host-Pfade (z. B. `/app/applet`) oder verschachtelte Geisterpfade (`app/applet/app/applet`), da die sichtbare Projektebene die direkte relative Referenz ist.
4. **Nicht mehr existierende Commit-Hashes:**
   - Lokale Commit-Hashes (wie `51d60df...`), die remote nicht vorhanden sind und im beschädigten lokalen Index lagen.

---

## 3. Startanweisung für einen neuen GAIS-Chat

```markdown
# GAIS NEW CHAT BOOTSTRAP PROMPT

## Projektziel
Entwicklung, Pflege und Erweiterung der Android-Anwendung RELEVANTOR (Package `com.example`), einer nativen Kotlin/Jetpack Compose App zur KI-gestützten Inhaltsanalyse und Qualitätsbewertung (Gemini API, Google Search Grounding).

## Arbeitsregeln & Pfadmodell
- Verwendung ausschließlich der sichtbaren relativen Projektstruktur (`app/`, `assets/`, `docs_md/`, `gradle/`).
- Verbot der Erzeugung oder Verwendung absoluter Host-Pfade oder verschachtelter Ordnerstrukturen.
- Schutz aller Audit- und Dokumentationsdateien in `docs_md/`.
- Verbot von parallelen/doppelten Löschbefehlen (`rm` und `delete_file` gleichzeitig).

## Wichtigste Architekturregeln
- Strikte Einhaltung von Clean Architecture + MVVM:
  - UI Layer: `app/src/main/java/com/example/ui/` (Compose UI, ViewModels)
  - Domain Layer: `app/src/main/java/com/example/domain/` (Use Cases, Engine Interfaces, Coordinator)
  - Data Layer: `app/src/main/java/com/example/data/` (Extraktoren, Engines, Local DB, Repositories)
- Erweiterungen von Analysekategorien folgen dem bestehenden `AnalysisEngine`- und `AnalysisRegistryImpl`-Muster (z.B. CP-03 `LocationQuestionEngine`).

## Wichtigste Verbote
- Keine Änderung der `applicationId` oder der Keystore-Signierungskonfigurationsdateien.
- Keine ungeprüften Git-Aktionen (wie `git reset --hard` oder `git checkout`) im beschädigten lokalen Repository.
- Keine Erzeugungen von Quellcode ohne vorherige Sichtung der betroffenen Dateien.
```

---

## 4. Abschlussstatus

**NEW CHAT BOOTSTRAP READY**
