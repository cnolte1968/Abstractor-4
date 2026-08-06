# RELEVANTOR WORKSPACE MASTER V1.3

**Erstellt am:** 2026-08-03 04:10:00 UTC  
**Gültig ab:** 2026-08-03 04:10:00 UTC  
**Herausgeber:** GAIS  
**Projekt-Root:** `/app/applet`  
**Repository:** `cnolte1968/Abstractor-4`  
**Status:** `WORKSPACE MASTER V1.3 PASS`

---

## 1. Übersicht & Zweck

Der **RELEVANTOR WORKSPACE MASTER V1.3** definiert die verbindlichen Governance- und Arbeitsregeln für Entwicklungs-, Test-, Audit- und Deployment-Aktivitäten im RELEVANTOR-Projekt. Er erweitert die Version 1.2 um konkrete Erkenntnisse ("Lessons Learned") und Schutzmechanismen aus dem Meilenstein **CP-03 (GOOGLE_MAPS_LOCATION_QUERY)**.

---

## 2. Änderungen gegenüber V1.2

1. **Pfadschutz & Geisterverzeichnis-Prävention (Abschnitt 3.1):** Absolute Verbot von doppelt verschachtelten Pfadstrukturen (`/app/applet/app/applet/...`).
2. **Post-Creation Verifikationspflicht (Abschnitt 3.2):** Verbindliche Nachprüfung aller neu erzeugten Dateien auf ihren realen physischen Ablageort und ihre Integrität.
3. **Exklusivität von Löschoperationen (Abschnitt 3.3):** Striktes Verbot der parallelen oder doppelten Verwendung von Shell-Löschbefehlen (`rm`) und Plattform-Tools (`delete_file`).
4. **Audit-Dokumenten-Schutz (Abschnitt 3.4):** Verbot des automatischen Löschens von Audit-Dateien ohne explizite SHA-256-Prüfung und Freigabe.
5. **CP-03 Integrations-Architektur (Abschnitt 4):** Verankerung des `LocationQuestionEngine`-Designmusters für ortsbezogene Kontextabfragen.

---

## 3. Neue Verbindliche Governance-Regeln

### 3.1 Pfadschutz & Verzeichnisintegrität
- **Projekt-Root:** Der einzig valide Root-Pfad im System ist `/app/applet`.
- **Pfad-Validierung:** Vor jeder Erstellung von Dateien oder Ordnern muss der Zielpfad auf Korrektheit und Eindeutigkeit geprüft werden.
- **Verbot von Pfad-Verschachtelungen:** Pfadkombinationen wie `/app/applet/app/applet/...` sind ausdrücklich verboten. Führen Plattform-Werkzeuge zu doppelten Pfad-Präfixen, muss das erzeugte Artefakt unverzüglich an den kanonischen Ort `/app/applet/docs_md/` verschoben und das Geisterverzeichnis gereinigt werden.

### 3.2 Dateierzeugung & Nachprüfung
- Nach der Erstellung jeder Datei muss umgehend überprüft werden:
  1. Tatsächlicher physikalischer Speicherort.
  2. Korrektheit des Dateinamens und des UTC-Zeitstempels.
  3. SHA-256 Prüfsumme bei allen Sicherheits-, Audit- und Architekturberichten.

### 3.3 Exklusiver Löschmechanismus (Race Condition Schutz)
- **Verbot von redundanten Löschbefehlen:** Es darf niemals gleichzeitig oder direkt aufeinanderfolgend ein Shell-Befehl (`rm`) und das Plattform-Tool `delete_file` für dieselbe Datei verwendet werden.
- **Vorgehensweise:** Löschoperationen sind bevorzugt über einen einzigen, kontrollierten Shell-Aufruf durchzuführen, um `CORTEX_STEP_TYPE_FILE_CHANGE`-Fehler durch nicht mehr existierende Dateien zu vermeiden.

### 3.4 Audit-Dokumenten-Schutz
- Audit- und Testberichte (`docs_md/*AUDIT*.md`, `docs_md/*TEST*.md`) dürfen **niemals automatisch gelöscht** werden.
- Eine Bereinigung ist nur zulässig nach:
  1. Bestätigung der Datei-Identität.
  2. SHA-256 Abgleich mit dem kanonischen Zielort.
  3. Expliziter Anweisung oder Audit-Freigabe.

---

## 4. CP-03 Lessons Learned & Architektur-Standards

### 4.1 Location Question Engine Pattern
- Die neue Engine `LocationQuestionEngine` ist fest im `AnalysisRegistryImpl` unter der Funktions-ID `GOOGLE_MAPS_LOCATION_QUERY` registriert.
- **Planner & Coordinator:**
  - `LocationQuestionPlanner` ermittelt die Fragekategorie (`STOSSZEITEN`, `ZUGANG_MOBILITAET`, `PREISE_OEFFNUNGSZEITEN`, etc.) und setzt bei zeitkritischen Anfragen `requiresGrounding = true`.
  - `LocationQuestionCoordinator` aggregiert Places API, Reviews, Wikipedia/Wikivoyage und steuert das Web-Search-Grounding.
- **Testbarkeits-Standard:** Koordinatoren und Hilfsklassen müssen für Unit-Tests mit Mocks/Subclasses geöffnet sein (`open class`), ohne Produktionseigenschaften zu verfälschen.

### 4.2 Workspace Integrity & Green Build Pipeline
- Sämtliche Testläufe (`gradle :app:testDebugUnitTest`) müssen kontinuierlich grün bleiben. Spröde Tests mit statischen Größenannahmen (z. B. Enum-Count assertions) sind proaktiv auf dynamische Vergleiche umzustellen.

---

## 5. Status & Verifikation

Mit diesem Update ist die Workspace-Governance V1.3 für alle künftigen Change-Prompts und Audit-Läufe vollumfänglich wirksam.

**STATUS:** `WORKSPACE MASTER V1.3 PASS`
