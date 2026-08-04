# RELEVANTOR VERIFIED PROJECT BASE

**Erstellt am:** 2026-08-03 08:01:26 UTC  
**Dokument-ID:** `RELEVANTOR_VERIFIED_PROJECT_BASE_2026-08-03_08-01-26_UTC`  
**Durchgeführt von:** GAIS  
**Arbeitsmodus:** Reiner Analyse-, Konsolidierungs- & Dokumentationsmodus (Keine Code-, Git- oder Systemänderungen)  
**Projekt-Root:** `/app/applet`  
**Repository:** `cnolte1968/Abstractor-4`  

---

## 1. GESICHERTE FAKTEN

1. **Kanonsicherer Projekt-Root:**  
   Der Arbeits- und Projektbereich ist eindeutig als `/app/applet` verifiziert.
2. **Physikalische Verzeichnisstruktur:**  
   Die Auswertung der primären Verzeichnisstruktur (`GAIS-Verzeichnisstruktur_2026-08-03.md`) belegt die Existenz folgender Hauptpfade unter `/app/applet`:
   - `app/src/main/java/com/example/` (Clean Architecture Source)
   - `app/src/test/java/com/example/` (Unit- & Robolectric-Tests)
   - `app/src/main/res/` (Android-Ressourcen & Adaptive Icons)
   - `assets/` (Prompts, Quality Rules, `function_registry.json`)
   - `docs_md/` (Dokumentation und Audit-Protokolle)
   - `gradle/` & Build-Skripte (`build.gradle.kts`, `settings.gradle.kts`)
3. **Architekturmodell & Schichten:**  
   Gemäß `GAIS-Architektur_2026-08-02.md` folgt das System strikt der Clean Architecture + MVVM:
   - **UI Layer (`com.example.ui`)**: Jetpack Compose, `MainViewModel`, `FeatureCatalog`, `OutputPresentationPolicy`.
   - **Domain Layer (`com.example.domain`)**: Use Cases (`AnalyzeContentUseCase`, `ExtractContentUseCase`), Engine-Schnittstellen (`AnalysisRegistry`, `AnalysisEngine`), `LocationQuestionCoordinator`.
   - **Data Layer (`com.example.data`)**: Extraktoren (`data.extraction`), KI-Engines (`data.engine`), Local Database (`data.local` / Room), Diagnostic Store (`PipelineReportStore`).
4. **CP-03 Implementierungsstand:**  
   Gemäß `RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` und Quellbaum ist die Kategorie `GOOGLE_MAPS_LOCATION_QUERY` mit `LocationQuestionPlanner`, `LocationQuestionCoordinator` und `LocationQuestionEngine` im Quellcode vorhanden.
5. **Externe Notfallsicherung:**  
   Der unberührte Zustand des Workspace wurde unter `/app/relevantor_emergency_backup_2026-08-03_07-13-19_UTC` extern archiviert und per SHA-256 verifiziert.

---

## 2. NICHT VERIFIZIERTE AUSSAGEN

1. **Ursache des lokalen Git-Schadens:**  
   Hypothesen aus früheren Logs, dass automatische Browser-Refreshes (F5) oder parallele Build-Daemons den `.git/index` physikalisch zerstört haben, sind spekulativ und durch keine Primärquelle belegt.
2. **Subjektive Preview- / Smoke-Test-Freigaben:**  
   Mündliche oder narrative Bestätigungen ("Preview-Test erfolgreich") ohne nachweisbare Emulator-Interaktionsprotokolle.
3. **Ungepushte Commit-Existenzen:**  
   Der lokale Commit-Hash `51d60df...` existierte ausschließlich im beschädigten lokalen Git-Index und war nicht im Remote-Repository vorhanden.

---

## 3. WIDERSPRÜCHE AUS VORHERIGEN LÄUFEN

1. **Widerspruch bezüglich Git-Status:**  
   Frühere GAIS-Läufe vermeldeten abwechselnd "Git-Datenbank vollkommen intakt" und "Git-Objektdatenbank massiv beschädigt". Die unabhängige Rohdatenanalyse zeigte: Der lokale `.git/index` ist beschädigt, während das Remote-Repository auf GitHub (`cnolte1968/Abstractor-4.git`, HEAD `c4c4065...`) vollständig intakt ist.
2. **Widerspruch bezüglich Pfadverschachtelung:**  
   In einzelnen Läufen entstanden durch relative Pfadbefehle doppelt verschachtelte Verzeichnisse (`app/applet/app/applet/...`). Diese Geisterstrukturen wurden identifiziert und bereinigt. `/app/applet` ist der einzig valide Root-Pfad.

---

## 4. GÜLTIGE ARBEITSREGELN

1. **Zero-Risk Deployment & Pfadschutz:**  
   - Sämtliche Pfade beziehen sich auf `/app/applet`.
   - Verschachtelungen wie `/app/applet/app/applet` sind verboten.
   - Keine direkten Git-Schreibaktionen (`reset`, `checkout`, `commit`, `push`) im beschädigten Workspace.
2. **Lösch- & Modifikationsschutz:**  
   - Niemals Shell-Löschbefehle (`rm`) und Plattform-Tools (`delete_file`) parallel auf dieselbe Datei anwenden.
   - Keine produktiven Quellcode-Dateien unter `app/src/`, `assets/` oder `gradle/` ohne gesonderte Freigabe ändern.
3. **Qualitäts- & Formatvorgaben:**  
   - Alle Berichte in `docs_md/` erfordern einen synchronen UTC-Zeitstempel im Dateinamen und im Dokumentkopf.

---

## 5. AKTUELLER PROJEKTSTATUS

- **Quellcode & Business Logic:** Vollständig integriert auf Stand CP-03 (Google Maps Location Context Analysis).
- **Lokaler Workspace (`/app/applet`):** Produktive Arbeitsdateien vorhanden, lokaler Git-Index beschädigt.
- **Remote-Repository:** Intakt und klonbar unter `https://github.com/cnolte1968/Abstractor-4.git` (HEAD `c4c4065...`).
- **Sicherungsstand:** Notfallsicherung + Migrations-Manifest vollständig erzeugt unter `/app/relevantor_emergency_backup_2026-08-03_07-13-19_UTC`.

---

## 6. RISIKEN

1. **Workspace-Instabilität bei unbedachten Git-Aktionen:** Direkte Git-Befehle im beschädigten lokalen Repository können ungesicherte lokale Anpassungen unwiederbringlich löschen.
2. **Tool-Schreibfehler bei relativen Pfaden:** Unachtsame relative Pfadübertragungen können erneut verschachtelte Ordnerstrukturen auslösen.

---

## 7. NÄCHSTER ZULÄSSIGER SCHRITT

**Empfehlung:**  
Manuelle Durchsicht und Bestätigung des vorliegenden Berichts sowie des Migrations-Manifests (`CP00_WORKSPACE_MIGRATION_MANIFEST_2026-08-03_07-13-19_UTC.md`). Nach Benutzerfreigabe kann die Übertragung der verifizierten lokalen Arbeitsdateien in ein sauberes Arbeitsverzeichnis erfolgen.

---

**ABSCHLUSSSTATUS:**  
`VERIFIED PROJECT BASE PASS`
