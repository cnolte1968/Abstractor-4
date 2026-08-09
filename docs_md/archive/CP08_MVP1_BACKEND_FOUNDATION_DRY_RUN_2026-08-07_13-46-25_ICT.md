# CP-08 MVP 1A BACKEND-FOUNDATION DRY-RUN REPORT

**Zeitstempel:** 2026-08-07 13:46:25 ICT / 2026-08-06 23:46:25 UTC  
**Prüfende Instanz:** GAIS  
**Prüfmodus:** Technical Dry-Run (keine Code-, Manifest-, Datenbank- oder Git-Änderungen)  
**Bezugsdokumente:**
- `/docs_md/CP08_BACKEND_ARCHITEKTUR_REVIEW_2026-08-06_05-35-00_UTC.md`
- `/AGENTS.md`
- `/ARCHITECTURE_FREEZE.md`
- `/app/src/main/assets/GAIS-Arbeitsstandards-Prompt.md`
- `/app/src/main/assets/change-prompts/CP-08_ARCHITEKTURAENDERUNG_TEMPLATE.md`

---

## 1. Status: READY FOR MVP 1B (MIT AUFTEILUNG 1B/1C)

Die technische Analyse bestätigt, dass das Projekt Relevantor und die verankerte Architektur vollständig bereit für den Start der Backend-Foundation (MVP 1) sind. Es wurden keine Blocker im Android-Code oder in den Arbeitsregeln festgestellt.

---

## 2. Git- & Workspace-Status

- **Git-Health-Gate:** `POST-PUSH-GIT-HEALTH PASS / ALREADY SYNCHRONIZED`
- **Branch / HEAD:** `main` @ `2981ba2e36074d45ecff3b0d5cb7cdf6f1de6f61`
- **git status --short:**
  ```text
  ?? GAIS-Architektur_2026-08-07.md
  ?? GAIS-System-State_07-08-2026.md
  ?? app/build/
  ?? docs_md/GAIS-Verzeichnisstruktur_2026-08-07.md
  ```
- **git fsck --full --strict:** Clean (Exitcode 0)
- **Unerwartete Änderungen:** NEIN

---

## 3. Aktueller Backend-Iststand

1. **Ordnerstruktur:**
   - Kanonischer Root: `/`
   - Android-Modul: `/app/`
   - Zukünftige Backend-Codebasis: `/supabase/` (**existiert aktuell noch nicht**).
2. **Vorhandene Platzhalter-Komponenten im Android-Code:**
   - `/app/src/main/java/com/example/data/BackendFeatureConfig.kt` (`backendEnabled = false`, `authEnabled = false`, `cloudSyncEnabled = false`)
   - `/app/src/main/java/com/example/data/local/SessionStorage.kt` (`SharedPreferences` für Token/Username)
   - `/app/src/main/java/com/example/data/remote/BackendApiService.kt` (Retrofit Interface mit Mock-Endpunkten `https://relevantor-backend.example.com/`)
   - `/app/src/main/java/com/example/domain/repository/UserRepository.kt` & `/app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt` (Mock Username/Password Methoden)
   - `/app/src/main/java/com/example/data/repository/SyncRepositoryImpl.kt`
   - `/app/src/main/java/com/example/data/sync/SyncScheduler.kt` & `SyncWorker.kt` (WorkManager Setup)
3. **Dependencies (`app/build.gradle.kts` & `libs.versions.toml`):**
   - Retrofit 2.12, OkHttp 4.12, Moshi 1.15 sind installiert und einsatzbereit.
   - Kein Supabase SDK eingebunden.

---

## 4. GAIS ↔ Supabase Zugriff

- **Aktueller Status:** **NICHT VORHANDEN**
  - Es ist keine Supabase CLI im Container installiert (`which supabase` -> not found).
  - Es sind keine Supabase MCP-Server oder schreibenden API-Tokens in der GAIS Runtime konfiguriert.
- **Konkrete technische Variante für Schema-Anwendung:**
  - **Variante A (Manuell via Dashboard / Empfohlen für MVP 1B):** GAIS generiert die SQL-Migrationsdateien unter `/supabase/migrations/`. Der Entwickler führt das SQL einmalig im Supabase SQL-Editor (Projekt `Relevantor`, Org `thinktory`) aus.
  - **Variante B (CLI / Automation):** Erfordert die Bereitstellung des `SUPABASE_ACCESS_TOKEN` / `DB_URL` im AI Studio Runtime Environment, falls GAIS automatische Migrationen ausführen soll.

---

## 5. Empfohlener Supabase-Zugriffsweg der Android-App

- **Empfohlener Client:** **Retrofit / OkHttp REST-Client (PostgREST & Auth REST API)**
- **Begründung:**
  1. `minSdk 24` Kompatibilität ohne Konflikte mit Ktor- / Kotlin-Stdlib-Versionen.
  2. Keine neuen schwere Drittanbieter-Bibliotheken erforderlich -> **Null Dependency-Risiko**.
  3. Vollständige Transparenz und Einbindung in bestehende OkHttp Interceptors (`apikey` Header und `Authorization: Bearer <JWT>` Header).
- **REST Endpunkte:**
  - Auth: `POST https://<project-ref>.supabase.co/auth/v1/token?grant_type=id_token`
  - Database: `GET/POST https://<project-ref>.supabase.co/rest/v1/profiles`
  - Functions: `POST https://<project-ref>.supabase.co/functions/v1/health`

---

## 6. Notwendige öffentliche Konfiguration & Secrets-Grenzen

- **Benötigte öffentliche Konfigurationsvariablen in `.env`:**
  ```env
  SUPABASE_URL=https://<project-ref>.supabase.co
  SUPABASE_ANON_KEY=<public-anon-key>
  ```
- **Injizierung:** Erfolgt über das Secrets Gradle Plugin in `BuildConfig.SUPABASE_URL` und `BuildConfig.SUPABASE_ANON_KEY`.
- **Sicherheits-Grenze:** Der `SUPABASE_SERVICE_ROLE_KEY` darf **NIEMALS** in der Android-App hinterlegt oder im Git committet werden!

---

## 7. Minimaler `/supabase/`-Aufbau (für MVP 1)

```text
/supabase/
├── config.toml                              [Supabase CLI Projekt-Config]
├── migrations/
│   └── 20260807000000_mvp1_schema_foundation.sql [SQL: profiles, licenses, token_wallet, RLS, Trigger]
└── functions/
    └── health/                              [Minimal Edge Function]
        └── index.ts
```

---

## 8. DB-Proof-Design (Proof A)

- **Ablauf:**
  `Android App (UserRepositoryImpl)`  
  ↓ (Retrofit HTTP GET /rest/v1/profiles?select=id,google_email)  
  `Supabase PostgREST Gateway`  
  ↓ (RLS + JWT Check)  
  `PostgreSQL Database`  
  ↓ (Returns 200 OK + Profile JSON)  
  `Android App (Ergebnis im Health-Status gelb/grün im Settings-Screen)`

---

## 9. Edge-Health-Proof-Design (Proof B)

- **Ablauf:**
  `Android App (BackendApiService)`  
  ↓ (Retrofit HTTP POST /functions/v1/health)  
  `Supabase Edge Runtime (Deno)`  
  ↓ (Evaluates request)  
  `Returns { "status": "ok", "service": "relevantor-edge", "timestamp": "2026-08-07T..." }`  
  ↓  
  `Android App (Verifiziert Edge Function Erreichbarkeit)`

---

## 10. Strategische Entscheidung: Aufteilung in MVP 1B und MVP 1C

- **Empfehlung: JA, AUFTEILEN IN 1B (DB-Proof) UND 1C (Edge-Proof).**
- **Begründung:**
  Eine simultane Implementierung von Datenbankschema, Auth-Integration, Retrofit-Header-Interceptors UND Edge-Function-Deployment führt zu einer unübersichtlichen Vielzahl gleichzeitiger Änderungen.
  - **MVP 1B:** Fokus auf `/supabase/migrations/`, `BackendApiService.kt`, `UserRepositoryImpl.kt`, `SessionStorage.kt` und DB-Proof (`/rest/v1/profiles`).
  - **MVP 1C:** Fokus auf `/supabase/functions/health/index.ts`, `BackendApiService.kt` Erweiterung und Edge-Health-Proof (`/functions/v1/health`).

---

## 11. Exakte File-Allowlist für den ersten produktiven Schritt (MVP 1B)

Folgende Dateien dürfen im nächsten produktiven Schritt angepasst / neu angelegt werden:

**Neue Dateien:**
1. `/supabase/config.toml`
2. `/supabase/migrations/20260807000000_mvp1_schema_foundation.sql`

**Bestehende Dateien:**
1. `/.env.example`
2. `/app/src/main/java/com/example/data/BackendFeatureConfig.kt`
3. `/app/src/main/java/com/example/data/remote/BackendApiService.kt`
4. `/app/src/main/java/com/example/domain/repository/UserRepository.kt`
5. `/app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt`
6. `/app/src/main/java/com/example/data/local/SessionStorage.kt`

**Strikt verboten:**
- Keine Änderungen an Prompts, Manifesten, UI Layouts oder AndroidManifest.xml in MVP 1B.

---

## 12. Risiken & Blocker

1. **Fehlende `.env` Secrets:** Ohne echten `SUPABASE_URL` und `SUPABASE_ANON_KEY` liefert der REST-Client 401/404 Fehler -> Graceful Fallback auf `backendEnabled = false` ist Pflicht.
2. **Schema-Anwendung:** Da GAIS aktuell keine CLI-Schreibverbindung hat, muss der Entwickler das generierte SQL aus `20260807000000_mvp1_schema_foundation.sql` einmalig im Supabase-Dashboard ausführen.

---

## 13. Empfehlung für den unmittelbar nächsten Schritt

Erteilung der Freigabe durch den User für **MVP 1B (Schema Foundation & Database Proof)** mit der oben definierten exakten File-Allowlist.
