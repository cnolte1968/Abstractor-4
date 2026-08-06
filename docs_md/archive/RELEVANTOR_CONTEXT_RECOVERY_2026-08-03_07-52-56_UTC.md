# RELEVANTOR KONTEXT-RECOVERY UND IST-STAND-KONSOLIDIERUNG

**Erstellt am:** 2026-08-03 07:52:56 UTC  
**Dokument-ID:** `RELEVANTOR_CONTEXT_RECOVERY_2026-08-03_07-52-56_UTC`  
**Durchgeführt von:** GAIS  
**Arbeitsmodus:** Reine Analyse & Konsolidierung (Keine Code- oder Git-Änderungen)  
**Projekt-Root:** `/app/applet`  
**Repository:** `cnolte1968/Abstractor-4`  

---

## 1. Ziel & Einleitung

Aufgrund vorangegangener Kontext- und Pfadverwirrungen sowie widersprüchlicher Aussagen in früheren Ausführungsberichten dient dieses Dokument der Erstellung eines **vollständig verifizierten Projekt-Ist-Standes**.

Der vorliegende Bericht basiert **ausschließlich auf nachweisbaren Primärquellen** im Workspace. Es wurden keinerlei technische Modifikationen an Produktdateien (`app/src/`, `assets/`, `gradle/`, `.git/`) vorgenommen.

---

## 2. Ausgewertete Primärquellen

Folgende lokale Systemdateien wurden als verbindliche Referenzen herangezogen:

1. **`docs_md/RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md`**  
   *Inhalt:* Projektdefinition, funktionale Kernanforderungen, Feature-Katalog und Entwicklungs-Roadmap.
2. **`docs_md/GAIS-Architektur_2026-08-02.md`**  
   *Inhalt:* Technische Clean-Architecture-Spezifikation, Schichtenmodelle (UI, Domain, Data), Modulinteraktionen und Engine-Verträge.
3. **`docs_md/GAIS-Verzeichnisstruktur_2026-08-03.md`**  
   *Inhalt:* Vollständige physische Verzeichnis- und Dateibaumanalyse des Workspace `/app/applet`.
4. **`docs_md/RELEVANTOR_WORKSPACE_MASTER_V1_3_2026-08-03_04-10-00_UTC.md` & `AGENTS.md`**  
   *Inhalt:* Governance-Regeln, Zero-Risk Deployment Vorgaben, Pfadschutz und Sicherheitsbeschränkungen.

---

## 3. Verzeichnisstruktur & Pfadprüfung

### 3.1 Kanonischer Projekt-Root
- **Gültiger Root-Pfad:** `/app/applet`
- **Sicherheitsvorgabe:** Sämtliche Arbeitsdateien und Build-Konfigurationsdateien befinden sich direkt unterhalb dieses Root-Pfades.

### 3.2 Prüfung des Geisterverzeichnisses (`applet/app/applet`)
- **Status:** **NICHT VORHANDEN (`DOES_NOT_EXIST`)**
- **Befund:** Eine physikalische Prüfung ergab, dass das verschachtelte Verzeichnis `/app/applet/app/applet` im aktuellen Arbeitsstand nicht existiert. Das Verzeichnis wurde in früheren Bereinigungsläufen entfernt und die Verzeichnisintegrität wiederhergestellt.

### 3.3 Gültige Pfadkonventionen
- **Anwendungscode:** `/app/applet/app/src/main/java/com/example/...`
- **Testcode:** `/app/applet/app/src/test/java/com/example/...`
- **Ressourcen:** `/app/applet/app/src/main/res/...`
- **Asset-Prompts:** `/app/applet/assets/prompts/...`
- **Dokumentation:** `/app/applet/docs_md/...`
- **Build-Artefakte:** `/app/applet/.build-outputs/app-debug.apk` bzw. `/app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Architektur- & Systemzustand

### 4.1 Systemarchitektur
Relevantor ist eine native Android-Anwendung (`com.example`) in Kotlin und Jetpack Compose zur Inhaltsanalyse und Qualitätsbewertung mittels Gemini API und Google Search Grounding.

Das System gliedert sich nach **Clean Architecture + MVVM**:
1. **UI Layer (`com.example.ui`)**: Jetpack Compose Screens, `MainViewModel`, UI-Dekoratoren (`OutputPresentationPolicy`) und `FeatureCatalog`.
2. **Domain Layer (`com.example.domain`)**: Use Cases (`AnalyzeContentUseCase`, `ExtractContentUseCase` etc.), Engine-Interfaces (`AnalysisRegistry`, `AnalysisEngine`), `LocationQuestionCoordinator` und Domänenmodelle.
3. **Data Layer (`com.example.data`)**: Extraktoren (`com.example.data.extraction`), Engines (`com.example.data.engine`), Prompt Loader (`AndroidAssetPromptLoader`), SQLite/Room Datenbank (`RelevantorDatabase`), Verträge/Parser (`SummaryResponseParser`, `A1ContractValidator`, `A2ContractValidator`) und `PipelineReportStore`.

### 4.2 Stand der Feature-Implementierung (CP-03)
- Der Meilenstein **CP-03 (`GOOGLE_MAPS_LOCATION_QUERY`)** ist im Quellcode vollständig integriert.
- `LocationQuestionPlanner`, `LocationQuestionCoordinator` und `LocationQuestionEngine` sowie zugehörige Registrierungs- und UI-Anpassungen sind in `app/src/main` vorhanden.
- Das adaptive Launcher-Icon ist in `app/src/main/res/drawable-nodpi/relevantor_app_icon.png` und `ic_launcher_foreground.xml` definiert.

---

## 5. Strukturierter Ist-Stand

### GESICHERTE FAKTEN (Direkt aus Primärquellen belegt)
1. **Projekt-Root:** `/app/applet` ist der einzig gültige Arbeitsbereich.
2. **Keine verschachtelte Geisterstruktur:** `/app/applet/app/applet` existiert nicht im aktuellen Baum.
3. **Clean Architecture:** Der Produktcode hält sich strikt an die Schichtentrennung zwischen UI, Domain und Data.
4. **App-Identität & Signierung:** `applicationId` und Keystore-Konfigurationen sind geschützt und dürfen nicht geändert werden.
5. **Remote-Repository Status:** Das Remote-Repository `https://github.com/cnolte1968/Abstractor-4.git` auf Branch `main` ist intakt und funktionsfähig (HEAD: `c4c4065ce28df448a32b4a6033777add4b74e26b`).
6. **Lokale Sicherung:** Eine vollständige Notfallsicherung wurde unter `/app/relevantor_emergency_backup_2026-08-03_07-13-19_UTC` extern abgelegt.

---

### NICHT VERIFIZIERT (Unbelegte Aussagen aus früheren Chat-Verläufen)
1. **Ursache der Git-Index-Beschädigung:** Hypothesen, dass Browser-Refreshes (F5) oder Gradle-Hintergrundprozesse den lokalen `.git/index` physikalisch überschrieben haben, sind spekulativ und nicht durch System-Logs belegt.
2. **Subjektive Preview-Freigaben:** Behauptungen wie "CP03 PREVIEW SMOKE TEST PASS" ohne aufgezeichnete visuelle Bestätigung im Emulator sind unbelegt.
3. **Commit-Historie:** Der lokale Hash `51d60df...` war ein ungepushtes lokales Artefakt im beschädigten Git-Index und existierte nicht auf dem Remote-Server.

---

### RISIKEN (Vor technischen Änderungen zu beachten)
1. **Lokale Git-Datenbank-Integrität:** Die lokale `.git`-Datenbank unter `/app/applet` ist beschädigt (`fatal: unknown index entry format`). Ein `git status` oder `git commit` schlägt im aktuellen Workspace fehl.
2. **Ungepushte Arbeitsstände:** Lokale Arbeitsdateien (wie z. B. Forensikberichte in `docs_md/` und Anpassungen an `AnalysisRegistryImpl.kt` / `LocationQuestionCoordinator.kt`) müssen vor jeder Workspace-Bereinigung oder Neu-Klonung gesichert bleiben.
3. **Race Conditions bei Datei-Löschungen:** Wie in `RELEVANTOR_WORKSPACE_MASTER_V1_3` beschrieben, darf niemals gleichzeitig ein Shell-Löschbefehl und ein Plattform-Tool auf dieselbe Datei angewendet werden.

---

## 6. Nächster zulässiger Arbeitsschritt

**Empfehlung:**  
Prüfung und Freigabe des erstellten Migrations-Manifests (`CP00_WORKSPACE_MIGRATION_MANIFEST_2026-08-03_07-13-19_UTC.md`) durch den Anwender/Betreiber. Nach expliziter Freigabe kann eine kontrollierte Übertragung der differierenden Arbeitsdateien aus der Notfallsicherung in ein sauberes Remote-Klon-Verzeichnis erfolgen, ohne den beschädigten Workspace direkt zu manipulieren.

---

## 7. Abschlussstatus

**CONTEXT RECOVERY PASS**
