# Relevantor GAIS Workspace Rules

**Stand / Zeitstempel:** 2026-08-08 22:53:00 ICT  
**Projekt:** Relevantor  
**Status:** Verbindliche Governance-Richtlinie für GAIS & Anforderungsformulierung  

---

## 1. Zweck

Dieses Dokument definiert die verbindlichen Regeln für den Umgang mit Verzeichnisstrukturen, Dateipfaden und Workspace-Ablagen im Projekt Relevantor. 

**Ziel:**
- Verhindern von unabsichtlich erzeugten Artefakt-Ordnern (wie z. B. `app/applet/docs_md/`) durch ungenaue Pfadangaben in Arbeitsaufträgen (GAAs).
- Unterbinden von Spekulationen über container-interne Verzeichnis-Mounts.
- Sicherstellen einer sauberen, konsistenten und jederzeit nachvollziehbaren Repository-Struktur für Dokumentation, Quellcode, Backend-Konfigurationsdateien und Automation-Skripte.

---

## 2. Grundprinzipien

1. **Keine Vermutung interner GAIS-Pfade:**
   Es dürfen niemals Annahmen über interne Container-Host-Pfade, Sandbox-Mounts oder plattformspezifische Pfadumleitungen getroffen werden.
2. **Pfade müssen aus bestätigten Projektinformationen stammen:**
   Jede Pfadangabe in Arbeitsaufträgen muss auf einer direkt verifizierten Dateisystem-Analyse basieren.
3. **Bei Unsicherheit zuerst Diagnose:**
   Ist die physische Lage eines Ordners unklar, wird vor jeglicher schreibenden Dateioperation (Erstellen, Verschieben, Löschen) eine reine Read-Only-Diagnose durchgeführt.
4. **Sichtbare Fakten haben Vorrang vor Interpretationen:**
   Reale Messergebnisse des Dateisystems (z. B. `pwd -P`, `os.walk`, SHA-256 Hashes) sind die einzig verbindliche Wahrheit.

---

## 3. Pfadverwendung in GAAs (Arbeitsaufträgen)

- **Erlaubte Pfadangaben:**
  - Ausschließliche Verwendung von relativen oder ab dem Workspace Root `/` aufgelösten Pfaden:
    - Dokumentation: `docs_md/DATEINAME.md` bzw. `/docs_md/DATEINAME.md`
    - Android-Quellcode: `app/src/main/java/...`
    - System-Prompts: `app/src/main/assets/prompts/...`
    - Change-Prompts: `app/src/main/assets/change-prompts/...`
    - Supabase Backend: `supabase/...`
    - Automation & Skripte: `tools/...`
- **Nicht erlaubte Pfadangaben:**
  - Erfundene oder vermutete Container-Präfixe wie `/app/applet/docs_md/...` oder `/app/applet/app/...`.
  - Ungeprüfte Pfad-Migrationen oder unüberlegte Struktur-Änderungen ohne vorherigen Analyse-Auftrag.

---

## 4. Vorgehen bei Pfadproblemen

Tritt eine Pfaddiskrepanz oder der Verdacht einer Doppelstruktur auf, gilt folgende verbindliche Schrittfolge:

1. **Read-only Workspace-Analyse:**
   Prüfen des tatsächlichen Arbeitsverzeichnisses (`pwd -P`) und aller relevanten Verzeichnisse auf Festplatte.
2. **Tatsächliche Verzeichnisstruktur erfassen:**
   Messen von Dateianzahl, Byte-Größen, Speicherdaten und absoluten Systempfaden.
3. **Vergleich möglicher Doppelstrukturen:**
   Inhaltlicher Vergleich der beteiligten Ordner inklusive SHA-256 Checksummen-Vergleich, um echte Code-Dubletten von flüchtigen Artefakten zu unterscheiden.
4. **Ursache analysieren:**
   Lückenloses Nachvollziehen der Entstehung (z. B. doppelte Pfadverkettung bei Tool-Aufrufen).
5. **Erst danach gezielter Änderungsauftrag:**
   Durchführen einer gezielten Bereinigung oder Übertragung ohne unbeteiligten Quellcode anzurühren.

---

## 5. Rollenverteilung

- **ChatGPT / Prompting-Ebene:**
  - Analysiert Problemstellungen, erstellt strukturierte GAAs und prüft Pfade auf Konformität.
  - Erfindet keine GAIS-internen Containerpfade und hält sich strikt an die bestätigten Projektfakten.
- **GAIS (Google AI Studio Agent):**
  - Führt die geforderten technischen Checks und Dateisystem-Analysen aus.
  - Meldet Fakten und Messergebnisse transparent zurück.
  - Verändert Quellcode und Struktur nur bei explizitem, autorisiertem Auftrag.
- **Nutzer:**
  - Hält die finale Entscheidungskompetenz für externe GitHub-Aktionen, Submissions und Freigaben.

---

## 6. Aktuell bestätigte Relevantor-Fakten

- **Workspace Root:** `/` (Physisch im Container unter `/app/applet` gemountet).
- **Kanonischer Dokumentationspfad:** `docs_md/` (Beinhaltet alle Abnahmeberichte, Selbsttest-Matrizen, Architekturberichte sowie das Unterverzeichnis `archive/`).
- **Kanonischer Android-Code-Pfad:** `app/` (Beinhaltet Kotlin-Sourcecode unter `src/main/java/`, Robolectric Tests unter `src/test/java/`, System-Prompts unter `src/main/assets/prompts/` und Change-Prompts unter `src/main/assets/change-prompts/`).
- **Kanonischer Supabase-Pfad:** `supabase/` (Beinhaltet `config.toml` und SQL-Migrationen).
- **Kanonischer Tools-Pfad:** `tools/` (Beinhaltet Skripte wie `git_post_ui_push_health_gate.sh` und `build_structure_doc.py`).
- **Artefaktordner-Status:** Der verschachtelte Ordner `app/applet/docs_md/` wurde am 2026-08-08 nachweislich und vollständig bereinigt. Es existieren **keine Code-Duplikate** und keine verschachtelten Projektbäume mehr.

---

## 7. Lessons Learned

1. **Explizite Containerpfade meiden:** Werden Container-Mount-Präfixe in Plattform-Tools mitgegeben, führt dies zu doppelter Pfadverkettung und ungewollten Unterordnern.
2. **Dokumentationsablage und Code-Struktur trennen:** Pfadabweichungen bei Markdown-Dateien bedeuten keinesfalls ein Problem im Quellcode oder im Build-System.
3. **Analyse vor Intervention:** Systematische Read-Only-Diagnosen verhindern unüberlegte Lösch- oder Verschiebungsaktionen.
