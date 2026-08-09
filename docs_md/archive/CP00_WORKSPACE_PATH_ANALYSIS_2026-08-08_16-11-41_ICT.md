# CP-00 Workspace Pfadanalyse & Kanonische Pfadkonvention

**Datum / Zeit:** 2026-08-08 16:11:41 ICT  
**Durchgeführt von:** GAIS (Google AI Studio)  
**Projekt:** Relevantor  

---

## 1. Pfadbestand & Systemerkenntnisse

- **Physischer GAIS Workspace:** `/app/applet` (Aktuelles Arbeitsverzeichnis `pwd -P` im Linux-Container).
- **Logischer Projektpfad:** `/` (Workspace Root aus Sicht der AI Studio Werkzeuge und File-Explorer).
- **`/docs_md/` auf Linux-Root-Ebene (`/docs_md`):** **NEIN** (Gibt es nicht auf Linux-Systemebene `/`).
- **`/app/applet/docs_md/` vorhanden:** **JA** (Das kanonische Dokumentationsverzeichnis im Workspace Root).
- **Dateien doppelt / verschachtelter Pfad (`/app/applet/app/applet/docs_md/`):**
  - **Ursache:** Bei Verwendung von werkzeugübergreifenden absoluten Container-Pfaden wie `/app/applet/docs_md/...` in Tool-Aufrufen verkettet das AI Studio Platform Tool den Arbeitsordner `/app/applet` mit dem übergebenen Pfad. Dadurch entsteht der verschachtelte Pfad `/app/applet` + `/app/applet/docs_md/...` = `/app/applet/app/applet/docs_md/...`.
- **Echte parallele Projektbäume:** **NEIN** (Es existiert kein zweites eigenständiges Code-Repository, sondern lediglich leere/selektive Ordnerstrukturen, die durch die Pfadverkettung absoluter Container-Pfade in vorherigen Tool-Aufrufen erzeugt wurden).

---

## 2. Vergleich der Pfadbereiche

| Pfad | Existenz | Inhalt / Funktion |
|---|---|---|
| `/docs_md` (Linux-Systemroot) | **NEIN** | Nicht vorhanden. |
| `docs_md/` (`/app/applet/docs_md/`) | **JA** | Primary Kanonischer Dokumentationsordner (23 Dateien + `archive/`). |
| `app/` (`/app/applet/app/`) | **JA** | Android-App-Modul (Quellcode `src/`, Build-Dateien `build.gradle.kts`). |
| `app/applet/docs_md/` | **JA (Artefakt)** | Verschachtelter Ordner mit den 3 Berichten dieser Session, entstanden durch Verkettung bei vorangestelltem `/app/applet/`. |

---

## 3. Beschluss & Verbindliche Pfadregel

1. **Variante A akzeptiert:** `/app/applet` ist der physische Container-Mount. Keine Migration erforderlich.
2. **Kanonische Pfadkonvention:**
   - **Regel:** Alle Pfadangaben in Aufgabenstellungen, Tool-Aufrufen (`view_file`, `create_file`, `edit_file`) und Shell-Befehlen MÜSSEN strikt relativ zum Workspace Root (oder mit führendem Slash im Sinne des Workspace Roots, z. B. `docs_md/` oder `/docs_md/`) angegeben werden.
   - **Verbot:** Die Angabe von vorangestellten Container-Pfaden wie `/app/applet/` in Tool-Argumenten ist strikt VERBOTEN, da dies zu ungewollten Pfadverkettungen führt.

---

*Bericht erstellt unter `/docs_md/CP00_WORKSPACE_PATH_ANALYSIS_2026-08-08_16-11-41_ICT.md`.*
