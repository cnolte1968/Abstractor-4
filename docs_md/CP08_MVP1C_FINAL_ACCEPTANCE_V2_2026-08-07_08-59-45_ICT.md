# CP08 MVP 1C Final Acceptance (V2 Korrektur)

## Ziel MVP 1C
Nachweis der App↔Supabase Database Konnektivität. Der Proof soll belegen, dass die Android App per REST-Client auf die in Supabase angelegte Tabelle `system_status` zugreifen kann.

## Architekturpfad
- Supabase REST API (v1)
- Android Retrofit + Moshi + OkHttp
- Read-Only Test-Tabelle `system_status`
- Authentication per `apikey` Header (Publishable Key), kein Bearer Token.

## Geänderte Dateien
- `app/src/main/java/com/example/data/remote/SupabaseApiService.kt`
- `app/src/main/java/com/example/data/remote/SupabaseSystemStatusChecker.kt`
- `app/src/test/java/com/example/data/SupabaseSystemStatusTest.kt`
- `app/src/main/java/com/example/data/RuntimePreflight.kt`
- `.env.example`

## Backend-Schema (Supabase)
Tabelle `system_status`:
- `id`: int8 (Primary Key)
- `status`: text
- `backend_version`: text
- `updated_at`: timestamptz

**RLS-Sicherheit (KORREKTUR):**
- RLS ist für `system_status` **aktiviert**.
- `SELECT` für `anon` / `authenticated` über Policy "Allow public read access to system_status" explizit erlaubt.
- `INSERT`, `UPDATE`, `DELETE` für `anon` implizit blockiert (da keine Schreib-Policies existieren).

## Unit-/Integrationtests
- `SupabaseSystemStatusTest.kt` mit Robolectric, testet JSON Parsing, Status-Auswertung, Header (apikey), Path, Error Handling und Netzwerkfehler.
- Alle Tests: PASS
- `online / 1` Parsing: PASS

## Finaler Smartphone-E2E-Nachweis
- Gerät: Samsung SM-S938B
- Android: 16 / API 36
- App-Version: 2.1
- Netzwerk: Cellular
- Zeitpunkt: 2026-08-07 ca. 15:52 ICT
- Preflight:
  - Android-Permission INTERNET: PASS
  - DNS Gemini Host: PASS
  - HTTPS Port 443: PASS
  - Supabase-Database (system_status): PASS
  - Detail: `PASS (status=online, version=1)`

## Bekannte Korrektur backend_version
- Das Moshi-Mapping für `backend_version` schlug anfänglich auf dem Gerät fehl, da `@field:Json` in Kombination mit `KotlinJsonAdapterFactory` den Konstruktorparameter nicht korrekt mappte.
- Korrigiert durch Verwendung von `@Json(name = "backend_version")`.
- Test korrigiert auf reale Payload `{"backend_version":"1"}` statt `backendVersion`.

## Ghost-Path-Klärung & Bereinigung
- Es gab inkonsistente Pfadangaben in vorherigen Berichten (z. B. absolute Container-Pfade `/app/applet/app/...`), was zu der fälschlichen Annahme führte, ein "Ghost-Path" sei entstanden.
- **Aktueller Stand:** Der Projektbaum ist kanonisch absolut korrekt. 
- Das Android-Modul liegt sauber in `/app/`.
- Es existiert kein unzulässiger verschachtelter Pfad wie `/app/applet/app/...` im Workspace.
- Etwaige temporäre Geisterdateien in anderen Pfaden wurden bereits in Aktion CP00 spurlos entfernt.
- **Fazit:** Kanonischer Workspace ist sauber.

## Verbleibende Risiken
- Zukünftige Moshi/Kotlin-Versionen könnten das Verhalten der `@Json` Annotationen weiter ändern (siehe Compiler Warnings bezüglich `KT-73255`).

## Status
MVP 1C FINAL FREEZE PASS

## Nächste Stufe
MVP 1D Edge-Function Health Proof
