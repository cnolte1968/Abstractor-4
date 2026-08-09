# CP-08 MVP 1C APP↔SUPABASE DATABASE PROOF - REVISED TECHNICAL DRY-RUN REPORT (V2)

**Zeitstempel:** 2026-08-07 14:50:00 ICT / 2026-08-07 00:50:00 UTC  
**Prüfende Instanz:** GAIS  
**Prüfmodus:** Technical Dry-Run V2 (Korrigerter Read-Only Entwurf, keine Produktivcode-Änderungen)  
**Bezugsdokumente:**
- `/docs_md/CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_2026-08-07_14-42-00_ICT.md`
- `/docs_md/CP08_MVP1B_SUPABASE_SCHEMA_FOUNDATION_2026-08-07_14-25-00_ICT.md`
- `/docs_md/CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md`
- `/docs_md/CP08_MVP1_GAIS_SUPABASE_CONNECTIVITY_2026-08-07_13-52-00_ICT.md`
- `/AGENTS.md`
- `/ARCHITECTURE_FREEZE.md`

---

## 1. Status: READY FOR IMPLEMENTATION (MVP 1C V2)

Die revidierte technische Analyse bestätigt, dass das Projekt Relevantor vollständig bereit für den ersten echten Android-End-to-End Database Proof (`Android → Supabase PostgREST → PostgreSQL public.system_status → Android`) ist. All notwendigen Schlüssel (`SUPABASE_URL` und `SUPABASE_PUBLISHABLE_KEY`) sind in den AI Studio Runtime Secrets vorhanden und die Remote-Tabelle `system_status` wurde in MVP 1B verifiziert.

---

## 2. Git- & Workspace-Baseline & Pfadklarstellung

- **Kanonischer Root:** `/`
- **Android-Modul:** `/app/`
- **Backend-Codebasis:** `/supabase/`
- **Workspace-Pfadstruktur:** Der interne Container-Mount `/app/applet` ist die reguläre Sandbox-Mount-Struktur des Arbeitsbereichs. Es wurden keine unzulässigen verschachtelten Pfaddubletten (wie `/app/applet/app/applet/...`) festgestellt.
- **`git status --short`:** PASS (Keine unbefugten Modifikationen)
- **`git fsck --full --strict`:** PASS (Exitcode 0)

---

## 3. Korrigierte Supabase-Header-Logik & REST-Client

- **Header-Spezifikation:**
  Der unauthentifizierte PostgREST-Zugriff für anonyme/öffentliche Lesezugriffe (RLS `anon` Policy) erfordert ausschließlich den HTTP-Header:
  `apikey: <publishable-key>`
- **Entfernung von Authorization Bearer:**
  Der Header `Authorization: Bearer <publishable-key>` wurde explizit aus dem Entwurf entfernt. Der Header `Authorization: Bearer <JWT>` wird erst in zukünftigen Phasen bei Implementierung einer echten Supabase-Nutzerauthentifizierung verwendet. MVP 1C arbeitet ohne Nutzer-Login.

---

## 4. Exakter Soll-Datenfluss für MVP 1C

```text
Android Boot / Diagnostik (RuntimePreflight)
  ↓
SupabaseSystemStatusChecker.checkStatus()
  ↓
SupabaseApiService (Retrofit GET /rest/v1/system_status?select=*)
  ↓ OkHttp Interceptor (Header "apikey: BuildConfig.SUPABASE_PUBLISHABLE_KEY")
Supabase PostgREST Gateway (https://jryfnuzzxwtrnflpqfbb.supabase.co)
  ↓ RLS SELECT Policy Evaluation (public.system_status)
PostgreSQL Table: public.system_status
  ↓ Returns 200 OK [{"id":1,"status":"online","backend_version":"1","updated_at":"..."}]
Moshi JSON Adapter (SystemStatusDto)
  ↓
SystemStatusResult (status == "online" && backend_version == "1")
  ↓
Preflight / Logcat Diagnostic PASS
```

---

## 5. Verbindliche Datei-Allowlist (Maximal 5 Dateien)

### Neue Produktionsdateien (2)
1. `/app/src/main/java/com/example/data/remote/SupabaseApiService.kt`
   - *Zweck:* Retrofit-Interface für PostgREST mit `SystemStatusDto` und OkHttp Header-Interceptor (`apikey`).
2. `/app/src/main/java/com/example/data/remote/SupabaseSystemStatusChecker.kt`
   - *Zweck:* Kapselung des DB-Proof-Aufrufs, Rückgabe eines strukturierten `SystemStatusResult`.

### Neue Testdatei (1)
3. `/app/src/test/java/com/example/data/SupabaseSystemStatusTest.kt`
   - *Zweck:* Isolierter JVM / Robolectric Unit-Test für DTO-Deserialisierung und Supabase-Client-Aufruf.

### Bestehende Dateien zu ändern (2)
4. `/.env.example`
   - *Zweck & Begründung:* **JA, erforderlich.** Das Secrets Gradle Plugin verwendet `.env` / `.env.example`, um daraus Gradle-`BuildConfig`-Felder zu erzeugen. Die Deklaration von `SUPABASE_URL` und `SUPABASE_PUBLISHABLE_KEY` in `/.env.example` stellt sicher, dass `BuildConfig.SUPABASE_URL` und `BuildConfig.SUPABASE_PUBLISHABLE_KEY` typ- und bautauglich im Android-Projekt generiert werden.
5. `/app/src/main/java/com/example/data/RuntimePreflight.kt`
   - *Zweck & Begründung:* **JA, erforderlich.** Dient als kleinstmögliche, entkoppelte technische Trigger-Stelle bei App-Start. Führt den Supabase Status-Check rein diagnostisch aus, ohne `MainActivity`, `MainViewModel` oder UI-Komponenten zu berühren.

---

## 6. Sperrliste & Unveränderte Komponenten

- `/app/build.gradle.kts` (NICHT ÄNDERN)
- `/gradle/libs.versions.toml` (NICHT ÄNDERN)
- `/app/src/main/java/com/example/data/BackendFeatureConfig.kt` (**Bleibt `backendEnabled = false`**)
- UI-Schicht (`MainActivity.kt`, Compose-Screens) (NICHT ÄNDERN - 0 UI-Änderungen)
- Supabase Remote-Ressourcen / Schemas (NICHT ÄNDERN)
- Room-Datenbank, Local-First Engine & Prompts (NICHT ÄNDERN)

---

## 7. Sachliche Regressionsrisiko-Bewertung

- **Regressionsrisiko:** **niedrig**
- **Betroffene Bereiche:**
  `RuntimePreflight` bei App-Start (mögliche Netzwerklatenz oder Timeouts, falls die Netzwerkverbindung blockiert ist).
- **Konkrete Gegenmaßnahmen:**
  1. Absicherung aller Supabase-Aufrufe mit strikten Socket-/HTTP-Timeouts (3 Sekunden Timeout) und vollständigem `try-catch`.
  2. Ausführung des Status-Checks nur dann, wenn `BuildConfig.SUPABASE_URL` konfiguriert und nicht leer ist.
  3. Preflight-Ergebnisse dienen rein der Diagnose; Fehler beim Supabase-Connect blockieren niemals den Start der lokalen App oder der Analyse-Engines.

---

## 8. Testdesign & Smartphone-E2E-Nachweis

1. **JVM Unit- & Integrationstest:**
   - Testausführung via `SupabaseSystemStatusTest.kt` im Workspace.
2. **Smartphone-E2E-Proof:**
   - APK Build (`compile_applet`) und Test auf physischem Gerät.
   - Verifikation im Logcat über Tag `RUNTIME_SMOKE` -> `Supabase-Database (system_status) -> PASS`.
   - Keine sichtbare UI-Anpassung erforderlich (CP-07 bleibt unangetastet).

---

## 9. Konkrete Empfehlung für den produktiven MVP-1C-GAA

Baufreigabe für den produktiven **MVP 1C App↔Supabase Database Proof** auf Basis der verbindlichen 5-Datei-Allowlist.
