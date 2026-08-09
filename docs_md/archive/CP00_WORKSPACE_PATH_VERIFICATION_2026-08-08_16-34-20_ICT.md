# CP-00 Workspace & Pfadkonvention Verifikation (GAIS Governance)

**Datum / Zeit:** 2026-08-08 16:34:20 ICT  
**Durchgeführt von:** GAIS (Google AI Studio)  
**Projekt:** Relevantor  

---

## 1. Tatsächlicher physischer Workspace

- **Arbeitsverzeichnis in der Linux-Shell:** `/app/applet` (`pwd -P` = `/app/applet`).
- **Container Root:** `/` (Das Betriebssystem-Root des Linux-Build-Containers).
- **Physischer Projekt-Speicherort:** `/app/applet` ist der physische Mount-Point des Repositories auf der Festplatte des Android-Build-Containers.

---

## 2. Tatsächlicher logischer Workspace

- **GAIS Platform Tool Root:** `/` (Workspace Root aus Sicht der AI Studio Tool-Schnittstelle und des File-Explorers).
- **Pfad-Resolution der GAIS Tools:** Die AI Studio Tool-Schnittstelle (`create_file`, `view_file`, `edit_file`) verarbeitet Pfadangaben relativ zum Workspace Root (`/app/applet`).
  - Eine Eingabe wie `TargetFile: "/docs_md/DATEI.md"` oder `docs_md/DATEI.md` wird von der Plattform automatisch physisch zu `/app/applet/docs_md/DATEI.md` aufgelöst.
  - Eine Eingabe wie `TargetFile: "/app/applet/docs_md/DATEI.md"` führt bei manchen Plattform-Tools zu einer unbeabsichtigten Pfadverkettung (`/app/applet` + `/app/applet/docs_md/...` = `/app/applet/app/applet/docs_md/...`).

---

## 3. Bedeutung von `/app/applet/` und `/docs_md/`

- **Hypothese 1 (Prüfung):** `/app/applet/` ist der physische Container-Mount des Projektes.
  - **Ergebnis:** **BESTÄTIGT**. `/app/applet` ist der physische Arbeitsordner.
- **Hypothese 2 (Prüfung):** Ist `/app/applet` ein zweiter separater Ordner innerhalb des Projekts?
  - **Ergebnis:** **NEIN**. Es existiert kein echter zweiter Projektbaum. Ordnerstrukturen wie `/app/applet/app/applet/` waren ausschließlich flüchtige Artefakte früherer Tool-Aufrufe mit expliziter Pfadverkettung.
- **Bedeutung von `/docs_md/`:**
  - Der logische Pfad `/docs_md/` bezeichnet das primäre Dokumentationsverzeichnis im Workspace.
  - Physisch befindet sich dieser Ordner auf Container-Ebene unter `/app/applet/docs_md/`.
  - Auf der Linux-Systemroot-Ebene (`/docs_md`) existiert kein eigenständiger Ordner.

---

## 4. Empfohlene zukünftige Pfadkonvention

- **Welche Pfadangabe ist korrekt für neue Berichte, CP-Dokumente und Reports?**
  - **Empfohlene Variante:** **`/docs_md/DATEI.md`** (bzw. `docs_md/DATEI.md` relativ zum Workspace Root).
  - **Begründung:** Gemäß den AI Studio System-Instruktionen erwarten GAIS-Datei-Tools eine absolute Pfadangabe ab dem Workspace Root mit führendem Slash (`/docs_md/DATEI.md`). Die Plattform löst dies fehlerfrei zum physischen Ziel `/app/applet/docs_md/DATEI.md` auf.

---

## 5. Verbindliche Beantwortung der Prüffragen

1. **"GAIS verwendet ausschließlich `/app/applet/...` als Pfadangabe."**
   - **Antwort:** **NEIN**
   - **Technische Begründung:** Die explizite Mitgabe von `/app/applet/` in Tool-Parametern verursacht in der Plattform-Schnittstelle doppelte Pfadverkettungen (`/app/applet/app/applet/...`) und führt zu Verwirrung.

2. **"GAIS verwendet niemals mehr `/docs_md/...` als Ablageziel."**
   - **Antwort:** **NEIN**
   - **Technische Begründung:** `/docs_md/...` ist genau der korrekte logische Zielpfad für GAIS Tools, da er von der Plattform sauber in den physischen Ordner `/app/applet/docs_md/...` übersetzt wird.

---

## 6. Bestehende Dokumentationsbestände

- **Aktueller Bestand in `docs_md/` (physisch `/app/applet/docs_md/`):**
  - 23 aktuelle Dokumente (inkl. aller MVP-1C-Abnahmeberichte und Checkpoints).
  - Unterordner `archive/` mit 53 historischen Berichten.
- **Dokumente außerhalb von `docs_md/`:** Keine relevanten Fachdokumente außerhalb.
- **Ablageziel für das Archiv:** `docs_md/archive/` (physisch `/app/applet/docs_md/archive/`).

---

## 7. Migration & Risiken

- **Migration erforderlich:** **NEIN** (Der physikalische Dateistand unter `/app/applet/docs_md/` ist 100% vollständig und korrekt).
- **Risiken:** **Keine**. Es wurden keine schreibenden Datei- oder Git-Operationen an Produktivcode durchgeführt.
