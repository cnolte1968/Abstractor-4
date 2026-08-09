# CP-08 MVP 1A.2 GAIS↔SUPABASE CONNECTIVITY CHECK REPORT

**Zeitstempel:** 2026-08-07 13:52:00 ICT / 2026-08-06 23:52:00 UTC  
**Prüfende Instanz:** GAIS  
**Prüfmodus:** Technical Connectivity Check (read-only, keine Code-, Manifest- oder Supabase-Projektänderungen)  
**Bezugsdokumente:**
- `/docs_md/CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md`
- `/AGENTS.md`
- `/ARCHITECTURE_FREEZE.md`

---

## 1. Ergebnisse der technischen Connectivity-Prüfung

| Prüfpunkt | Status | Details |
| :--- | :--- | :--- |
| **Supabase CLI direkt im PATH** | **NEIN** | `supabase` ist nicht global im Shell-PATH installiert. |
| **`npx supabase` nutzbar** | **JA** | `npx -y supabase --version` liefert erfolgreich Version **2.111.0**. |
| **Sichere CLI-Authentifizierung** | **JA** | Über die Umgebungsvariable `SUPABASE_ACCESS_TOKEN` kann `npx supabase` im CI/Agent-Container authentifiziert werden. |
| **MCP-Unterstützung im Workspace** | **NEIN** | Das aktuelle GAIS-Build-Environment (Android-Container) besitzt keine MCP-Server-Schnittstelle. |

---

## 2. Bewertung der Entwicklungs-Varianten

### Variante A: GAIS → Supabase MCP
- **Machbarkeit:** **NICHT VERFÜGBAR**
- **Begründung:** Im aktuellen GAIS Build-Mode existiert keine Konfigurations- oder Registrierungsschnittstelle für MCP-Server.

### Variante B: GAIS → Supabase CLI (via `npx -y supabase`)
- **Machbarkeit:** **VOLLKOMMEN MACHBAR**
- **Voraussetzung:** Einmalige Bereitstellung von `SUPABASE_ACCESS_TOKEN` und Supabase Project Reference (`<project-ref>`).
- **Anwendungsfall:** Automatisiertes Ausführen von `npx -y supabase db push` und `npx -y supabase functions deploy`.

### Variante C: Code-Driven Repository (GAIS erzeugt `/supabase/` → Dashboard-Execution)
- **Machbarkeit:** **VOLLKOMMEN MACHBAR & RISIKOFREI**
- **Voraussetzung:** Keine Zugangsdaten in der Runtime erforderlich.
- **Anwendungsfall:** GAIS schreibt alle SQL-Migrationen unter `/supabase/migrations/` und Edge-Functions unter `/supabase/functions/`. Der Entwickler führt die SQL-Skripte im Supabase Dashboard (Projekt *Relevantor*, Org *thinktory*) aus.

---

## 3. Empfohlener Entwicklungsweg

**Empfehlungs-Kombination: Variante C (Repository-First) als Baseline mit optionaler Variante B (CLI)**

1. **Git-Single-Source-of-Truth:** Alle Datenbank-Schemas, RLS-Policies und Edge-Functions werden von GAIS strikt unter `/supabase/` im Repositorygehalten.
2. **Schema-Anwendung (Prozessual):**
   - **Ohne Token:** GAIS erstellt `/supabase/migrations/*.sql` -> Entwickler kopiert das SQL einmalig in das Supabase Dashboard.
   - **Mit Token:** Sobald `SUPABASE_ACCESS_TOKEN` in den Secrets hinterlegt ist, kann GAIS via `npx -y supabase db push` automatisiert synchronisieren.

---

## 4. Einmalige Einrichtungsschritte für Variante B (optional)

Falls eine direkte CLI-Synchronisation durch GAIS gewünscht ist:
1. Erzeugen eines Personal Access Tokens (PAT) im Supabase Dashboard (`Account -> Access Tokens`).
2. Eintragen des Tokens als `SUPABASE_ACCESS_TOKEN` in den AI Studio Secrets.
3. Ausführen von `npx -y supabase link --project-ref <project-ref>` im Workspace.

---

## 5. Sicherheitsgrenzen

- **Keine Service Role Keys:** Der Supabase Service Role Key wird weder im Code, noch in Gradle, noch in der Android-App hinterlegt.
- **Grenzziehung Android App:** In der App werden ausschließlich die öffentlichen Variablen `SUPABASE_URL` und `SUPABASE_ANON_KEY` genutzt.

---

## 6. Verbleibende Blocker & Empfehlung für MVP 1B

- **Blocker:** Keine Blocker vorhanden.
- **Empfehlung:** Unmittelbare Freigabe für **CP-08 MVP 1B (Backend Foundation & Database Proof)** mit der vorbereiteten File-Allowlist.
