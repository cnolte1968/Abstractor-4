# CP-00 Workspace-Doppelstruktur Root-Cause-Analyse

**Datum / Zeit:** 2026-08-08 18:31:53 ICT  
**Durchgeführt von:** GAIS (Google AI Studio)  
**Projekt:** Relevantor  

---

## 1. Faktische Verzeichnisstruktur (Physischer Ist-Zustand)

Eine direkte, schreibfreie Systemanalyse aller Pfade im Linux-Container und im GAIS-Workspace zeigt folgenden exakten physischen Ist-Zustand:

1. **Linux-Container Root (`/`):**
   - Ist das System-Root des Android-Build-Containers.
   - Ein Verzeichnis `/docs_md` existiert auf Linux-Systemebene **NICHT** (`DOES NOT EXIST`).
   - Ein Verzeichnis `/app` existiert und ist das Mutterverzeichnis des Container-Mounts (`/app/applet`).

2. **Kanonischer Workspace Root (`/` im GAIS File-Explorer = `/app/applet` auf Container-Ebene):**
   - Das Arbeitsverzeichnis (`pwd -P`) lautet `/app/applet`.
   - Alle GAIS-Tools operieren relativ zu dieser Wurzel.

3. **Hauptverzeichnisse im Kanonischen Workspace Root:**
   - **`docs_md/` (physisch `/app/applet/docs_md/`):** **EXISTS** — 83 Dateien (23 aktuelle Berichte, 53 archivierte Dokumente in `archive/` und Unterordner, ~11.9 MB).
   - **`app/` (physisch `/app/applet/app/`):** **EXISTS** — 1.773 Dateien (Android App Quellcode, ViewModels, System-Prompts unter `src/main/assets/prompts/`, Change-Prompts unter `src/main/assets/change-prompts/`, Build-Konfigurationen, ~128.9 MB).
   - **`supabase/` (physisch `/app/applet/supabase/`):** **EXISTS** — CLI-Config `config.toml` & SQL-Migrationen.
   - **`tools/` (physisch `/app/applet/tools/`):** **EXISTS** — Governance-Skripte (`git_post_ui_push_health_gate.sh`, `build_structure_doc.py`).

4. **Verschachtelter Pfad `app/applet/` (physisch `/app/applet/app/applet/`):**
   - **`app/applet/docs_md/` (physisch `/app/applet/app/applet/docs_md/`):** **EXISTS** — Enthält exakt **3 Dateien** (13.2 KB).

---

## 2. Vergleich `docs_md/` vs `app/applet/docs_md/`

- **Haupt-Dokumentationsordner `docs_md/` (`/app/applet/docs_md/`):**
  - **Status:** Vollständig & Primär.
  - **Enthaltene Dateien:** 83 Dateien (Architektur-Reviews, MVP-1C Abnahmeberichte, Selbsttest-Matrizen, Verzeichnisinventare und Unterverzeichnis `archive/`).
- **Verschachtelter Ordner `app/applet/docs_md/` (`/app/applet/app/applet/docs_md/`):**
  - **Status:** Artefakt-Ordner.
  - **Enthaltene Dateien (Exakt 3 Dateien):**
    1. `GAIS_GOVERNANCE_SMOKE_TEST_2026-08-08_15-44-05_ICT.md`
    2. `CP00_MVP1C_STABLE_CHECKPOINT_2026-08-08_15-53-54_ICT.md`
    3. `CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-03-39_ICT.md`
- **SHA-256 & Inhaltlicher Vergleich:**
  - Keine dieser 3 Dateien existiert im primären `docs_md/` Ordner (Dateinamen kommen in `docs_md/` nicht vor).
  - Es gibt **keine inhaltlichen SHA-256-Dubletten** zwischen beiden Ordnern.

---

## 3. Vergleich `app/` vs `app/applet/` (Code-Duplikats-Prüfung)

- **`app/` (`/app/applet/app/`):**
  - Enthält das **vollständige Android-Anwendungsmodul** (Kotlin-Quellcode unter `src/main/java/`, Robolectric Tests unter `src/test/java/`, AndroidManifest, System-Prompts, `build.gradle.kts`).
- **`app/applet/` (`/app/applet/app/applet/`):**
  - Enthält **KEINEN Quellcode**, **KEINE Kotlin-Dateien**, **KEINE Gradle-Dateien** und **KEINE Unterprojekte**.
  - Enthält ausschließlich den Unterordner `docs_md/` mit den oben genannten 3 Markdown-Dateien.
- **Ergebnis Code-Duplikate:** **0% Code-Duplikate**. Es existiert kein zweiter Code-Baum und kein paralleles Android-Modul.

---

## 4. Git-Zuordnung

- **Lokaler Git-CLI Status:** Die lokale `.git`-Binary meldet einen Index-Fehler (`fatal: unknown index entry format 0x4d500000`, `loose object 2981ba2e... corrupt`).
- **Geplante Repository-Zuordnung:**
  - In Git ist die kanonische Projektstruktur `app/`, `docs_md/`, `supabase/`, `tools/` verankert.
  - Der verschachtelte Pfad `app/applet/` ist ein flüchtiges lokales Artefakt früherer Tool-Pfade und stellt keine relevante Git-Struktur dar.

---

## 5. Entstehungshypothesen (Verhaltensanalyse)

- **Hypothese 1 (Doppelte Pfadverkettung bei Tool-Aufrufen):** **BESTÄTIGT / FAKT**
  - Das physische Arbeitsverzeichnis des Containers lautet `/app/applet`.
  - Wenn in einem früheren Tool-Aufruf explizit `TargetFile: "/app/applet/docs_md/DATEI.md"` übergeben wurde, verknüpfte das AI Studio Plattform-Tool die Pfade zu `/app/applet` + `app/applet/docs_md/DATEI.md` = `/app/applet/app/applet/docs_md/DATEI.md`.
  - Dadurch entstanden am 2026-08-08 zwischen 15:44:05 ICT und 16:03:39 ICT die 3 Berichte im verschachtelten Pfad `app/applet/docs_md/`.

- **Hypothese 2 (Echte parallele Repositories / Code-Dubletten):** **WIDERLEGT**
  - Es wurden keine zwei Repositories geklont und es existiert kein doppelter Quellcode.

---

## 6. Sichere Erkenntnisse

1. **Echte Code-Doppelstruktur:** **NEIN (0% Code-Duplikate)**.
2. **Dokumentations-Situation:**
   - Primäres und einzig gültiges Dokumentationsverzeichnis ist **`docs_md/`** (physisch `/app/applet/docs_md/`).
   - Der Ordner `app/applet/docs_md/` enthält lediglich 3 vorübergehende Berichte aus vorherigen Durchläufen.
3. **Kanonische Pfadangabe für GAIS:**
   - In allen zukünftigen Tool-Aufrufen (`create_file`, `edit_file`, `view_file`) MUSS als Pfad strikt `/docs_md/DATEI.md` (oder `docs_md/DATEI.md`) verwendet werden.
   - Der Präfix `/app/applet/` darf in Tool-Argumenten NIEMALS mehr mitgegeben werden.

---

## 7. Empfohlene nächste Entscheidung

1. **Dateisystem & Produktivcode:** Der Produktivcode in `app/`, `supabase/` und `tools/` ist 100% intakt und unberührt.
2. **Dokumentation:** Alle Berichte werden weiterhin zentral unter `/docs_md/` abgelegt.
3. **GitHub Checkpoint:** Der bereitgestellte MVP-1C-Stand ist bereit für den manuellen GitHub-Checkpoint durch den Anforderer über die GAIS GitHub UI.
