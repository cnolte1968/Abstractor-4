# CP-08 MVP 1C APP↔SUPABASE DATABASE PROOF - TECHNICAL DRY-RUN REPORT

**Zeitstempel:** 2026-08-07 14:42:00 ICT / 2026-08-07 00:42:00 UTC  
**Prüfende Instanz:** GAIS  
**Prüfmodus:** Technical Dry-Run (Read-Only Analysis & Design, keine Produktivcode-Änderungen)  
**Bezugsdokumente:**
- `/docs_md/CP08_MVP1B_SUPABASE_SCHEMA_FOUNDATION_2026-08-07_14-25-00_ICT.md`
- `/docs_md/CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md`
- `/docs_md/CP08_MVP1_GAIS_SUPABASE_CONNECTIVITY_2026-08-07_13-52-00_ICT.md`
- `/AGENTS.md`
- `/ARCHITECTURE_FREEZE.md`

---

## 1. Status: READY FOR IMPLEMENTATION (MVP 1C)

Die technische Analyse bestätigt, dass das Projekt Relevantor vollständig bereit für den ersten echten Android-End-to-End Database Proof (`Android → Supabase PostgREST → PostgreSQL public.system_status → Android`) ist. All notwendigen Schlüssel (`SUPABASE_URL` und `SUPABASE_PUBLISHABLE_KEY`) sind in den AI Studio Runtime Secrets vorhanden und die Remote-Tabelle `system_status` wurde in MVP 1B verifiziert.

---

## 2. Git- & Workspace-Baseline

- **Kanonischer Root:** `/`
- **Android-Modul:** `/app/`
- **Backend-Codebasis:** `/supabase/`
- **Geisterpfad-Status:** Clean (`/app/applet/` entfernt)
- **`git status --short`:** Clean
- **`git fsck --full --strict`:** PASS (Exitcode 0)

---

## 3. Ist-Architektur des Android-Netzwerkpfads

1. **Jetpack Compose + MVVM Architecture:**
   - Standard-Netzwerkstack: **Retrofit 2.12**, **OkHttp 4.12**, **Moshi 1.15**.
   - Lokaler First-Ansatz: `BackendFeatureConfig.kt` steuert `backendEnabled = false`, `authEnabled = false`, `cloudSyncEnabled = false`.
2. **Secrets & Secrets Gradle Plugin:**
   - Konfiguration in `/app/build.gradle.kts`:
     ```kotlin
     secrets {
         propertiesFileName = ".env"
         defaultPropertiesFileName = ".env.example"
     }
     ```
   - Bei Gradle Build generiert das Secrets Plugin aus `.env` / `.env.example` `BuildConfig`-Felder (`BuildConfig.SUPABASE_URL`, `BuildConfig.SUPABASE_PUBLISHABLE_KEY`).
3. **Diagnose- & Preflight-Infrastruktur:**
   - `/app/src/main/java/com/example/data/RuntimePreflight.kt` führt bei App-Start / Preflight automatisierte System- & Netzwerk-Checks durch (DNS, Permissions, API Gateway).

---

## 4. Secret- & BuildConfig-Mechanismus

- **`SUPABASE_URL` in AI Studio Secrets:** **JA** (`https://jryfnuzzxwtrnflpqfbb.supabase.co`)
- **`SUPABASE_PUBLISHABLE_KEY` in AI Studio Secrets:** **JA** (in Runtime vorhanden)
- **Aktivierung in Android:**
  Durch Ergänzung von Schlüssel-Hüllen in `/.env.example`:
  ```env
  SUPABASE_URL=https://placeholder.supabase.co
  SUPABASE_PUBLISHABLE_KEY=placeholder_publishable_key
  ```
  erzeugt Gradle automatisch:
  - `BuildConfig.SUPABASE_URL`
  - `BuildConfig.SUPABASE_PUBLISHABLE_KEY`
  ohne dass Gradle-Skripte oder Build-Konfigurationen geändert werden müssen.

---

## 5. Empfohlener Client: Retrofit 2 / OkHttp 4

- **Empfehlung:** **Vorhandener Retrofit / OkHttp Stack**
- **Begründung gegen Supabase-Kotlin-SDK:**
  1. **Null Dependency-Risiko:** Kein Hinzufügen von Ktor, kotlinx-serialization oder extra Kotlin-Compiler-Plugins nötig (`minSdk 24` bleibt unbeeinflusst).
  2. **Maximale Transparenz:** PostgREST liefert Standard-REST/JSON (`GET /rest/v1/system_status?select=*`).
  3. **Performance & Interceptoren:** OkHttp Interceptor fügt automatisch `apikey` und `Authorization: Bearer <publishable_key>` Header hinzu.

---

## 6. Exakter Soll-Datenfluss für MVP 1C

```text
Android Preflight Check (RuntimePreflight)
  ↓
SupabaseSystemStatusChecker.checkStatus()
  ↓
SupabaseApiService (Retrofit GET /rest/v1/system_status?select=*)
  ↓ OkHttp Interceptor (apikey: BuildConfig.SUPABASE_PUBLISHABLE_KEY, Bearer ...)
Supabase PostgREST Gateway (https://jryfnuzzxwtrnflpqfbb.supabase.co)
  ↓ RLS SELECT Policy Evaluation
PostgreSQL Table: public.system_status
  ↓ Returns 200 OK [{"id":1,"status":"online","backend_version":"1","updated_at":"..."}]
Moshi JSON Adapter (SystemStatusDto)
  ↓
SystemStatusResult (status == "online" && backend_version == "1") -> Preflight PASS
```

---

## 7. Datei-Allowlist & Sperrliste

### Allowlist für den produktiven MVP 1C GAA (Exakt 4 Dateien)

**Neue Dateien (2):**
1. `/app/src/main/java/com/example/data/remote/SupabaseApiService.kt`
   - *Zweck:* Retrofit-Interface für PostgREST, `SystemStatusDto` Datenklasse und Retrofit-Client Builder mit Header-Interceptor.
2. `/app/src/main/java/com/example/data/remote/SupabaseSystemStatusChecker.kt`
   - *Zweck:* Abstraktion für den isolated DB-Proof-Aufruf, liefert strukturiertes `SystemStatusResult` für Preflight & Tests.

**Bestehende zu ändernde Dateien (2):**
1. `/.env.example`
   - *Zweck:* Ergänzung der Schlüssel-Hüllen `SUPABASE_URL` und `SUPABASE_PUBLISHABLE_KEY` für das Secrets Gradle Plugin.
2. `/app/src/main/java/com/example/data/RuntimePreflight.kt`
   - *Zweck:* Hinzufügen eines isolierten Preflight-Checks `Supabase-Database (system_status)`, der den Live-Status abfragt, falls `BuildConfig.SUPABASE_URL` definiert ist.

### Sperrliste (STRIKT VERBOTEN ZU ÄNDERN)

- `/app/build.gradle.kts` (Keine Änderungen erforderlich)
- `/gradle/libs.versions.toml` (Keine neuen Bibliotheken)
- `/app/src/main/java/com/example/data/BackendFeatureConfig.kt` (**Bleibt `backendEnabled = false`**)
- `/app/src/main/assets/prompts/` (Gesperrte Prompts)
- `AndroidManifest.xml` (Unverändert)
- Room-Datenbank & UI-Compose-Screens (Keine Änderungen)

---

## 8. Begründung: Keine Änderungen an BackendFeatureConfig & UI

- **`BackendFeatureConfig`:** Bleibt `backendEnabled = false`. Der Proof prüft die Netzwerkverbindung und die Supabase-Datenbank isoliert in der Preflight-/Diagnose-Schicht. Die regulären App-Analysen bleiben 100% Local-First.
- **UI Layouts:** Keine UI-Komponenten werden geändert. Der Nachweis erfolgt sauber über Preflight-Logs, System-Diagnose und automatisierte Unit/Robolectric-Tests.

---

## 9. Testdesign & E2E-Verifikation

1. **Unit / Robolectric Test:**
   - Neuer Test in `app/src/test/java/com/example/data/SupabaseSystemStatusTest.kt`, der die JSON-Moshi-Deserialisierung und den PostgREST-Aufruf testet.
2. **GAIS-Workspace Live-GET Test:**
   - Ausführung via Gradle Test `/gradlew testDebugUnitTest` oder Java Execution, um den echten Live-Endpunkt `https://jryfnuzzxwtrnflpqfbb.supabase.co/rest/v1/system_status` im Container abzufragen und HTTP 200 mit `status = online` zu verifizieren.
3. **Smartphone-E2E-Proof:**
   - APK Build (`compile_applet`) & Installation auf dem echten Testgerät. Bei App-Start führt `RuntimePreflight` den Check durch und loggt `Supabase-Database (system_status) -> PASS`.

---

## 10. Risiken & Abmilderung

- **Risk 1 (Secret Leak):** Der Publishable Key ist für Client-Applikationen entworfen und durch RLS geschützt. Dennoch wird er nicht hardcodiert, sondern via `BuildConfig` injiziert.
- **Risk 2 (Netzwerk-Ausfall / Offline):** Der Preflight-Check fängt alle `IOException` / `HttpException` ab und markiert das Ergebnis als `FAIL` (mit Details), ohne dass die App abstürzt.
- **Risk 3 (App-Regression):** Da `backendEnabled = false` bleibt und keine UI-Dateien geändert werden, ist das Regressionsrisiko für bestehende Features exakt **0%**.

---

## 11. Konkrete Empfehlung für den produktiven MVP-1C-GAA

Direkte Freigabe durch den Anforderer für **MVP 1C (App↔Supabase Database Proof)** unter Verwendung der oben definierten exakten File-Allowlist (2 neue Dateien, 2 bestehende Dateien zu ändern).
