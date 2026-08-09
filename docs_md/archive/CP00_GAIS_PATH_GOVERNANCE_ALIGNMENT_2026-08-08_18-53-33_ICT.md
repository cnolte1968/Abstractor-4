# CP-00 GAIS Path Governance & Workspace Alignment

**Datum / Zeit:** 2026-08-08 18:53:33 ICT  
**Durchgeführt von:** GAIS (Google AI Studio)  
**Projekt:** Relevantor  

---

## 1. Verifiziertes Problemverständnis

Im Arbeitsverlauf des Relevantor-Projekts kam es vereinzelt zu Unklarheiten bezüglich der Ablagestruktur von Dokumenten und Berichten. 

- **Symptom:** Es entstand temporär eine verschachtelte Ordnerstruktur unter `app/applet/docs_md/` mit genau drei Markdown-Dateien, während der eigentliche Projektcode und der primäre Dokumentationsordner unter `/` (`docs_md/` bzw. `app/`) verblieben.
- **Klärung:** Die technische Forensik hat eindeutig gezeigt, dass es sich um ein rein externes Pfad-Adressierungsthema bei einzelnen Tool-Aufrufen handelte. Der produktive Android-Anwendungs-Code, die Prompts, die Supabase-Konfigurationen und die Governance-Skripte waren zu keinem Zeitpunkt von Duplikaten oder Verfälschungen betroffen.

---

## 2. Verifizierte Ursachen

### Ursache A: Falsche / doppelte Pfadangaben in Tool-Aufrufen (BESTÄTIGT)
- Der physische Mount-Point des Repositories im Linux-Build-Container lautet `/app/applet`.
- Aus Sicht der GAIS-Plattform-Schnittstelle ist die Root-Ebene des Workspace jedoch als `/` definiert.
- Wurde in Tool-Parametern explizit der volle Containerpfad angegeben (z. B. `TargetFile: "/app/applet/docs_md/BERICHT.md"`), verknüpfte die Plattform-Schnittstelle diesen relativen Pfad erneut mit dem Working Directory:
  $$\text{Working Dir } (/app/applet) + \text{Target} (/app/applet/docs_md/...) = /app/applet/app/applet/docs_md/...$$
- **Ergebnis:** Dadurch wurden die drei betroffenen Berichte am 2026-08-08 in das verschachtelte Artefaktverzeichnis geschrieben.

### Ursache B: Trennung von Pfadablage und Quellcode (BESTÄTIGT)
- Es existierte **zu keinem Zeitpunkt ein zweiter Android-Codebaum**, kein paralleles Backend und keine Quellcode-Doppelstruktur.
- Die Artefaktstruktur beschränkte sich ausnahmslos auf genau drei flüchtige Markdown-Berichte, die inzwischen sicher in das kanonische Verzeichnis `docs_md/` konsolidiert wurden.

### Ursache C: Git-Metadaten-Verhalten ist ein separates Phänomen (BESTÄTIGT)
- Beschädigte lokale `.git`-Indexe im Container (`fatal: unknown index entry format`) resultieren aus unvollständigen lokalen Git-Metadaten im Container environment und haben keine ursächliche Verbindung zur Erstellung von Workspace-Dokumenten oder der Code-Qualität.
- Die Synchronisation und der Release-Stand erfolgen sicher über die GAIS GitHub UI.

---

## 3. Nicht-Ursachen (Widerlegt)

- **WIDERLEGT:** Es gibt kein Architektur- oder Code-Duplikat-Problem im Projekt.
- **WIDERLEGT:** Es gibt keine Fehlfunktion im Android-Build-System oder in den Jetpack Compose / ViewModel Komponenten.
- **WIDERLEGT:** Es gibt keine fehlerhafte Multi-Repository-Struktur.

---

## 4. Zukünftige Pfadregeln (Verbindliche Governance)

Um jegliche Wiederholung von Pfadverschachtelungen auszuschließen, gelten ab sofort folgende verbindliche Arbeitsregeln für GAIS:

### Regel 1: Verbot interner Container-Präfixe
- In Tool-Parametern (`create_file`, `edit_file`, `view_file`, `delete_file`) darf der Präfix `/app/applet/` **NIEMALS** verwendet werden.

### Regel 2: Kanonische Workspace-Relative Pfadangabe
- Alle Datei-Pfade werden absolut ab der logischen Workspace-Wurzel `/` angegeben:
  - Dokumentation: `/docs_md/DATEINAME.md`
  - Android-Code: `/app/src/main/java/...`
  - Prompts: `/app/src/main/assets/prompts/...`
  - Change Prompts: `/app/src/main/assets/change-prompts/...`
  - Backend: `/supabase/...`
  - Tools: `/tools/...`

### Regel 3: Read-Only Diagnose bei Unklarheiten
- Bei jeglicher Unsicherheit über die Existenz oder Lage eines Ordners wird vor schreibenden Dateioperationen eine rein lesende Pfadverifikation durchgeführt.

### Regel 4: Nachlaufende Verifikation
- Nach dem Erstellen neuer Dokumente wird sichergestellt, dass die Datei exakt an der beabsichtigten Stelle im kanonischen Ordner `docs_md/` liegt.

---

## 5. Offene Punkte & Status

- **Status Workspace:** 100% sauber, saubere Ordnerstruktur `docs_md/`, `app/`, `supabase/`, `tools/`.
- **Artefaktordner `app/applet/`:** Vollständig und rückstandslos entfernt.
- **Produktiv-Code:** Intakt und kompilierbar (`compile_applet` PASS).

---

## 6. Empfehlung

Die erarbeiteten Pfadregeln bieten maximale Sicherheit. Das Relevantor-Projekt ist fachlich, technisch und strukturell bereit für den manuellen GitHub Checkpoint durch den Anforderer via GAIS GitHub UI.
