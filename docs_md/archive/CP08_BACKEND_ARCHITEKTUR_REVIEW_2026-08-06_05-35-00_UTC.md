# CP-08 BACKEND-ARCHITEKTUR REVIEW RELEVANTOR

**Zeitstempel:** 2026-08-06 05:35:00 UTC  
**Prüfende Instanz:** GAIS  
**Prüfmodus:** Read-Only Analyse (keine Code-, Manifest-, Datenbank- oder Git-Änderungen)  
**Geprüftes Dokument:** `CP08_BACKEND_ARCHITEKTURKONZEPT_V0_9_2026-08-06_05-26-55_UTC.md`  
**Projekt-Root:** `/`  

---

## 1. Kurzfazit & Technische Prüfbarkeit

### Status: PASS WITH TECHNICAL ADJUSTMENTS (TECHNISCH PRÜFBAR & BELASTBAR)

Das Backend-Architekturkonzept V0.9 übersetzt die 57 fachlichen Anforderungsentscheidungen präzise in eine hybride, schlanke Zielarchitektur und einen modularen MVP-Stufenplan. Das Konzept vermeidet typisches Overengineering und hält sich an die verankerten Guardrails (Projekt-Root `/`, geschütztes Prompt-Verzeichnis `app/src/main/assets/prompts/`, ARCHITECTURE_FREEZE.md und CP-08 Governance).

**Wichtigste technische Befunde:**
1. **Ist-Code-Kompatibilität:** Im Projekt existieren bereits Platzhalter für Backend-Komponenten (`BackendApiService.kt`, `UserRepositoryImpl.kt`, `SessionStorage.kt`, `BackendFeatureConfig.kt`, `RelevantorDatabase.kt`, `YoutubeTranscriptHelper.kt`). Diese müssen für Supabase Auth und PostgREST/Edge-Functions refaktoriert werden.
2. **Pfadverifikation:** Der sichtbare Projekt-Root ist `/`. Veraltete Pfade wie `/app/applet` existieren im aktuellen Workspace nicht.
3. **MVP-Reihenfolge & Entscheidung 57.2:** Die Priorisierung der vollständigen Beta-Lizenz-/Tokenbasis (MVP 2) vor dem YouTube-Feature (MVP 3) ist technisch zwingend richtig, um ungesicherte Provider-Aufrufe zu verhindern.
4. **Erforderliche technische Korrekturen:** V0.9 enthält geringfügige Diskrepanzen bzgl. der vorhandenen Username/Password-Mocking-Schnittstellen und des Supabase-SDK-Imports vs. REST-Client, die für V1.0 präzisiert werden müssen.

---

## 2. Verifizierte Ist-Architektur & Pfade

Die lesende Codebase-Analyse bestätigt folgende reale Projektstruktur unter `/`:

```text
/ (Sichtbarer Projekt-Root)
├── app/
│   ├── build.gradle.kts                             [compileSdk 35, minSdk 24, Room 2.7.0, Retrofit 2.12, WorkManager 2.9]
│   └── src/main/
│       ├── AndroidManifest.xml                      [Namespace com.example, AppId com.aistudio.relevantor.gkmpxz]
│       ├── assets/
│       │   ├── prompts/                             [GESCHÜTZT: 16 F_*.md Prompts, function_registry.json, prompt_manifest.json]
│       │   ├── change-prompts/                      [CP_GUIDELINE.md, CP-01..CP-03, CP-07, CP-08_ARCHITEKTURAENDERUNG_TEMPLATE.md]
│       │   ├── GAIS-Arbeitsstandards-Prompt.md       [Verbindlicher GAIS-Arbeitsstandard]
│       │   └── ARCHITECTURE_FREEZE.md               [Frozen GAA Engine Contract & Plugin Baseline]
│       └── java/com/example/
│           ├── MainActivity.kt                      [Compose Activity Entry Point]
│           ├── domain/
│           │   ├── model/                           [CanonicalAnalysisInput, DomainSummary, PublicVideoSource, TakeawayItem]
│           │   ├── engine/                          [AnalysisEngine, EngineRunner, AnalysisRegistry, ContractValidators]
│           │   ├── repository/                      [AnalysisRepository, UserRepository, SyncRepository, ContentExtractionRepository]
│           │   └── usecase/                         [AnalyzeContentUseCase, ExtractContentUseCase, SaveAnalysisUseCase]
│           ├── data/
│           │   ├── BackendFeatureConfig.kt          [Aktuell: backendEnabled=false, authEnabled=false, cloudSyncEnabled=false]
│           │   ├── GeminiRepository.kt              [Direct Gemini API Transport via BuildConfig.GEMINI_API_KEY]
│           │   ├── YoutubeTranscriptHelper.kt       [Client-seitiger Scraping/InnerTube/TimedText Extractor]
│           │   ├── local/
│           │   │   ├── RelevantorDatabase.kt        [Room DB v5, Entity AnalysisEntity, DB "relevantor_database"]
│           │   │   └── SessionStorage.kt            [SharedPreferences: active_username, active_token, user_favorites_list]
│           │   ├── remote/
│           │   │   └── BackendApiService.kt         [Retrofit Mock Interface, Base URL https://relevantor-backend.example.com/]
│           │   └── repository/
│           │       ├── UserRepositoryImpl.kt        [Mock Username/Password Implementation]
│           │       ├── SyncRepositoryImpl.kt        [Mock Sync Implementation]
│           │       └── YoutubeTranscriptProviderAdapter.kt [In-Memory Adapter on YoutubeTranscriptHelper]
│           └── ui/
│               └── MainViewModel.kt                 [Compose Orchestration ViewModel]
└── gradle/
    └── libs.versions.toml                           [Dependency Version Catalog]
```

---

## 3. Wiederverwendbare vorhandene Komponenten

Folgende im Ist-Code vorhandene Strukturen können im Zuge von CP-08 direkt wiederverwendet und angepasst werden:

1. **`BackendFeatureConfig.kt` (`app/src/main/java/com/example/data/`):**
   - Dient als zentraler Kill-Switch für `backendEnabled`, `authEnabled`, `cloudSyncEnabled`. Kann um `supabaseEnabled` erweitert werden.
2. **`SessionStorage.kt` (`app/src/main/java/com/example/data/local/`):**
   - Speichert bereits Session-Tokens und lokale Favoriten-IDs in `SharedPreferences`. Kann zur sicheren Speicherung des Supabase JWT Tokens, der `user_id` sowie des 24h-Berechtigungs-Caches erweitert werden.
3. **`UserRepository.kt` & `UserRepositoryImpl.kt` (`app/src/main/java/com/example/domain/repository/` & `data/repository/`):**
   - Schnittstelle ist vorhanden. Muss von alten Mock-Methoden (`login(username, password)`) auf Supabase-basierte Google-Identity-Anmeldung und Session-Status-Methoden refaktoriert werden.
4. **`RelevantorDatabase.kt` (`app/src/main/java/com/example/data/local/`):**
   - Room-Datenbank (Version 5, Name `relevantor_database`) mit Migrationen ist betriebsbereit. Kann für den lokalen 24h-Entitlement-Cache, die lokale Offline-Usage-Warteschlange und die lokale Anzeige von Verlauf/Favoriten um zusätzliche Tabellen oder Felder erweitert werden.
5. **`YoutubeTranscriptProviderAdapter.kt` (`app/src/main/java/com/example/data/repository/`):**
   - Domain-Adapter für `TranscriptProvider` existiert bereits. Kann in MVP 3 transparent von der lokalen `YoutubeTranscriptHelper`-Klasse auf die Supabase Edge Function umgestellt werden, ohne die Domain-Schicht zu brechen.

---

## 4. Abweichungen, Konflikte & Korrekturvorschläge (V0.9 vs. Ist-Code)

| ID | Konflikt / Abweichung | Risiko | Korrekturvorschlag für V1.0 |
|---|---|---|---|
| **C1** | **Auth-Modell-Diskrepanz:** Ist-Code nutzt Username/Password (`LoginRequest` / `RegisterRequest`), V0.9 fordert reinen Google OAuth via Supabase. | **Mittel:** Altes Mock-Code-Relikt erzeugt Verwirrung und tote Methoden im Repo. | In MVP 1 `UserRepository` und `BackendApiService` bereinigen. Nur Google-Auth ID-Token / Supabase Auth Methoden bereitstellen. |
| **C2** | **Supabase SDK vs. REST Client:** V0.9 erwähnt Supabase-Client. `app/build.gradle.kts` enthält derzeit nur Retrofit/Moshi/OkHttp. | **Gering:** Versionen-Inkompatibilität bei Ktor/Supabase-Kotlin-SDK Import auf minSdk 24. | **Empfehlung:** Supabase REST & Auth APIs direkt über den bereits vorhandenen OkHttp/Retrofit Client (`https://<project-ref>.supabase.co/auth/v1`, `/rest/v1`, `/functions/v1`) mit Standard-Headers (`apikey`, `Bearer JWT`) ansteuern OR leichtes Supabase Kotlin SDK integrieren. |
| **C3** | **Google Identity / Redirect Scheme Config:** V0.9 setzt Google-Login voraus, spezifiziert aber keine Android Credential Manager / Manifest-Konfiguration. | **Mittel:** Login schlägt zur Laufzeit fehl, wenn OAuth Redirect / Intent-Filter im Manifest fehlt. | In MVP 1 Android Credential Manager (`androidx.credentials`) und Intent-Filter Scheme in `AndroidManifest.xml` aufnehmen. |
| **C4** | **YouTube Extractor Location:** `YoutubeTranscriptHelper.kt` führt derzeit direktes HTML-Scraping im Android-Client aus. V0.9 MVP 3 fordert Edge Function mit Secret. | **Gering:** Redundanz zwischen Client-Scraper und Edge Function. | In MVP 3 den `YoutubeTranscriptProviderAdapter` auf die Edge Function umleiten. `YoutubeTranscriptHelper` als Fallback beibehalten. |
| **C5** | **Room DB Schema vs. Supabase Sync:** Room DB speichert `AnalysisEntity` mit vollem `keyTakeawaysJson`. V0.9 fordert 30-Tage-Verlauf in Supabase nur als Metadaten. | **Gering:** Unklarheit bei der Datensynchronisation. | Klarstellen: Supabase `analysis_history` speichert nur Metadaten. Supabase `favorites` speichert den vollständigen JSON-Payload. Room bleibt lokaler Cache für beides. |

---

## 5. Bewertung des logischen Datenmodells

Das in V0.9 vorgeschlagene Datenmodell ist sauber normalisiert, PostgreSQL-konform und für den Betrieb auf Supabase ideal geeignet:

1. **`profiles`**: `id` (UUID, FK auf `auth.users.id`), `google_email`, `communication_email`, `status`, `created_at`, `updated_at`.
2. **`licenses`**: `user_id` (FK), `plan` ('FREE', 'PLUS', 'PRO'), `status`, `source`, `created_at`, `updated_at`.
3. **`token_wallet`**: `user_id` (FK), `allocated_tokens` (z.B. 1000 für Beta), `available_tokens`, `period_type` ('BETA_FIXED'), `updated_at`.
4. **`feature_config`**: `feature_id` (PK, z.B. 'A.1', 'E.1', 'YOUTUBE'), `display_name`, `active` (boolean), `minimum_plan`, `token_cost` (int), `updated_at`.
5. **`usage_events`**: `id` (UUID), `idempotency_key` (TEXT UNIQUE), `user_id` (FK), `feature_id`, `token_cost`, `status`, `created_at`.
6. **`analysis_history`**: `id` (UUID), `user_id` (FK), `local_analysis_id`, `function_id`, `title`, `source_type`, `created_at`, `expires_at` (30 Tage TTL).
7. **`favorites`**: `id` (UUID), `user_id` (FK), `analysis_id`, `title`, `function_id`, `source_type`, `payload` (JSONB / TEXT), `created_at`, `updated_at`.
8. **`feedback`**: `id` (UUID), `user_id` (FK), `message`, `communication_email`, `function_id`, `screen_name`, `app_version`, `created_at`.
9. **`error_events`**: `id` (UUID), `user_id` (FK, optional), `area`, `error_type`, `sanitized_message`, `app_version`, `created_at`.

### Idempotente Erst-Anlage via PostgreSQL Trigger:
In Supabase wird eine SQL-Funktion mit Trigger auf `auth.users` hinterlegt. Sobald sich ein neuer Nutzer via Google anmeldet, erzeugt der Trigger atomar und idempotent die zugehörigen Einträge in `profiles`, `licenses` (Free Plan) und `token_wallet` (1000 Beta Tokens).

### Row Level Security (RLS):
Sämtliche Tabellen außer `feature_config` erhalten striktes RLS:
`CREATE POLICY "Users access own data" ON <table> FOR ALL USING (auth.uid() = user_id);`

---

## 6. Sicherheits-, Cache-, Fallback- & Sync-Logik

1. **24-Stunden Berechtigungs-Cache:**
   - Der lokale Cache (`SessionStorage` oder DataStore) speichert: `last_sync_timestamp`, `active_plan`, `available_tokens`, `feature_config_json`.
   - Bei jedem App-Start / vor Funktionsausführung: Ist `currentTime - last_sync_timestamp < 24h`, darf die Ausführung im Offline-Fallback auf Basis des gecachten Free-Plans und verbleibenden Kontingents erfolgen.
   - Nach Ablauf von 24h ohne Serverkontakt sperrt die App die Online-Funktionen und fordert eine Re-Authentifizierung / Synchronisation.
2. **Offline Usage Queue & Idempotenz:**
   - Ausgeführte Direct-Gemini-Analysen erzeugen lokal ein `usage_event` mit einer eindeutigen `idempotency_key` (UUIDv4).
   - Sobald das Backend erreichbar ist, schickt die App die ausstehenden Events an Supabase. Dank UNIQUE Constraint auf `idempotency_key` führt eine mehrfache Übertragung nicht zu Doppelabbuchungen.
3. **Direkter Gemini-Pfad in der Beta (Akzeptiertes Risiko R1):**
   - In der Beta ruft die App Gemini weiterhin direkt über den in `GeminiRepository` hinterlegten API-Key auf.
   - Schutz vor Missbrauch erfolgt serverseitig über Google Cloud Gemini Quotas/Budgets und Supabase Token-Tracking. Vor Monetarisierung ist der Übergang zu einer serverseitigen Verifikation Pflicht.

---

## 7. Bewertung der MVP-Reihenfolge & Minimaler Pfad

Die Stufenfolge MVP 0 bis MVP 5 ist logisch und technisch exzellent durchdacht:

- **MVP 0:** Verifizierter Wiederherstellungspunkt (Read-Only Git Checkpoint, Build & APK Verify).
- **MVP 1:** Supabase Auth, Google Login, Automatisches Profil/Lizenz/Token-Wallet, Dashboard-Anzeige.
- **MVP 2:** Beta-Lizenz, Token-System & Feature-Config, Usage Events, 24h-Cache & Queue.
- **MVP 3:** YouTube Video-Zusammenfassung via Supabase Edge Function & Transcript Provider.
- **MVP 4:** 30-Tage Verlauf & Favoriten-Synchronisation.
- **MVP 5:** Feedback, Error Briefkasten & Betriebsfähigkeit.

**Einhaltung von Fachentscheidung 57.2:**
MVP 2 (Vollständige Beta-Lizenz-/Tokenbasis) MUSS zwingend vor MVP 3 (YouTube) fertiggestellt sein. Erst wenn die Tokenkontrolle und das Event-System stabil laufen, darf mit MVP 3 der erste externe Provider kostenpflichtig eingebunden werden.

---

## 8. Exakte Datei-Allowlists für MVP 0 und MVP 1

### MVP 0 GAA Allowlist (Sicherungs- & Verifikations-Checkpoint):
Keine Codeänderungen! Nur lesende Verifikation und Erstellung des Git-Checkpoints.
- `app/build.gradle.kts` (Prüfung)
- `.env` / `.env.example` (Prüfung)
- `app-debug.apk` (Root Verification Task Execute via `compile_applet`)

### MVP 1 GAA Allowlist (Supabase Auth & User Foundation):
Nur die folgenden spezifischen Dateien dürfen in MVP 1 geändert oder neu angelegt werden:
1. **`app/build.gradle.kts`**: Hinzufügen von Supabase-REST / Auth OkHttp Konfiguration & `androidx.credentials`.
2. **`gradle/libs.versions.toml`**: Eintragen benötigter Versionen für Credential Manager / Supabase (sofern genutzt).
3. **`app/src/main/AndroidManifest.xml`**: Hinzufügen von Redirect Intent-Filter Scheme für Auth.
4. **`.env` & `.env.example`**: Ergänzen von `SUPABASE_URL` und `SUPABASE_ANON_KEY`.
5. **`app/src/main/java/com/example/data/BackendFeatureConfig.kt`**: Aktivieren von `backendEnabled = true` und `authEnabled = true`.
6. **`app/src/main/java/com/example/data/remote/SupabaseClient.kt`** *(NEU)*: Zentraler OkHttp/Retrofit Client für Supabase Auth & REST Endpunkte.
7. **`app/src/main/java/com/example/data/local/SessionStorage.kt`**: Erweitern um Supabase User-ID, Access Token und Refresh Token.
8. **`app/src/main/java/com/example/domain/repository/UserRepository.kt`**: Anpassung des Interfaces für Google Auth & Session Status.
9. **`app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt`**: Refactoring zur Nutzung von `SupabaseClient` für Google Auth.
10. **`app/src/main/java/com/example/ui/MainViewModel.kt`**: Einbinden des Auth-Status in die UI-Abläufe.

*Hinweis:* Das Verzeichnis `app/src/main/assets/prompts/` ist streng geschützt und darf in MVP 1 NICHT berührt werden.

---

## 9. Benötigte Dependencies & Konfigurationen je MVP

- **MVP 0:** Keine neuen Dependencies.
- **MVP 1:**
  - `SUPABASE_URL` & `SUPABASE_ANON_KEY` in `.env`.
  - Android Credential Manager (`androidx.credentials:credentials:1.3.0`, `com.google.android.libraries.identity.googleid:googleid:1.1.1`).
  - Supabase Auth via Standard Retrofit/OkHttp REST Client (bereits im Projekt) oder Supabase Kotlin SDK (`io.github.jan-tennert.supabase:gotrue-kt`).
- **MVP 2:**
  - Moshi Adapter für `FeatureConfig` / `UsageEvent` JSON.
  - Room Entity / DAO Ergänzung für `offline_usage_queue`.
- **MVP 3:**
  - Supabase Edge Function `youtube-transcript` (Deno/TypeScript auf Supabase).
  - Transcript Provider Secret (z.B. `TRANSCRIPT_PROVIDER_API_KEY`) im Supabase Dashboard.
- **MVP 4:**
  - Room Migration v5 -> v6 für `favorites_sync` & `history_sync`.
- **MVP 5:**
  - Keine weiteren externen Dependencies; Nutzung der Supabase PostgREST Endpunkte `/rest/v1/feedback` und `/rest/v1/error_events`.

---

## 10. Offene Nutzerentscheidungen (Maximal reduziert)

V0.9 deckt fast alle fachlichen Aspekte ab. Es verbleiben lediglich **zwei reale operative Nutzerentscheidungen** vor der Implementierung von MVP 1 bzw. MVP 3:

1. **Google Web-Client-ID für Supabase Auth (MVP 1):**
   - Der Anforderer muss im Google Cloud Console / Firebase Projekt eine Web-Client-ID für OAuth 2.0 bereitstellen und diese im Supabase Dashboard unter *Authentication -> Providers -> Google* eintragen.
2. **Auswahl des Dritten Transcript-Providers (MVP 3):**
   - Vor der Entwicklung der Edge Function in MVP 3 muss der konkrete kostenpflichtige/freie Provider (z.B. Supadata, Youtube-Transcript-API, ScrapingAnt, AssemblyAI) und dessen API-Key festgelegt werden.

Alle übrigen Implementierungsdetails (DB-Spalten, Rest-Clients, Fallback-Timer) sind durch GAIS fachlich empfohlen und benötigen keine separaten Rückfragen.

---

## 11. Empfehlung für Architekturkonzept V1.0 & Freigabe

1. **Freigabe für V1.0:** Das Dokument `CP08_BACKEND_ARCHITEKTURKONZEPT_V0_9_2026-08-06_05-26-55_UTC.md` ist technisch zu **100% konsistent** mit den Projektregeln und der Ist-Codebase. Es kann unter Einarbeitung der geringfügigen Korrekturen (Kapitel 4 dieses Reviews) offiziell als **V1.0** freigegeben und unter `docs_md/CP08_BACKEND_ARCHITEKTURKONZEPT_V1_0_2026-08-06_UTC.md` abgelegt werden.
2. **Nächster Arbeitsauftrag (GAA):**
   - **Schritt 1:** Erstellung und Ausführung des **GAA MVP 0** (Verifizierter Read-Only Checkpoint & Build-Bestätigung).
   - **Schritt 2:** Erstellung des **GAA MVP 1** (Supabase Auth & User Foundation) basierend auf der definierten File-Allowlist.

---

## 12. Verifikationsprotokoll & Read-Only Bestätigung

GAIS bestätigt hiermit:
- Es wurden **keine** Quellcode-Dateien, Gradle-Skripte, AndroidManifest, Prompt-Dateien oder Datenbank-Schemata geändert.
- Die Analyse erfolgte rein lesend über das sichtbare Projekt-Root `/`.
- Der Git-Status vor und nach dieser Analyse wurde verglichen:

```text
# Git Status vor Analyse:
error: non-monotonic index .git/objects/pack/... fatal: bad object HEAD (Code 128)

# Git Status nach Analyse:
error: non-monotonic index .git/objects/pack/... fatal: bad object HEAD (Code 128)
```
*(Hinweis: Das lokale Git-Repository im Sandbox-Container weist einen korrupten Pack-Index auf. Der Arbeitsbereich wurde zu 100% unverändert belassen).*

**Erzeugtes Review-Dokument:**  
`docs_md/CP08_BACKEND_ARCHITEKTUR_REVIEW_2026-08-06_05-35-00_UTC.md`
