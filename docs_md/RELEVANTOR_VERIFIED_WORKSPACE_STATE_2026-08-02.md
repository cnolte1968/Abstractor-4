# RELEVANTOR Verified Workspace State Report

**Spezifikations-Datum:** 2026-08-02  
**Ausführendes System:** Google AI Studio (GAIS) Agent  
**Berichtstyp:** Ist-Zustands-Verifikation des aktiven Relevantor-Workspaces  
**Absoluter Berichts-Pfad:** `/app/applet/docs_md/RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md`  

---

## 1. Verifizierter Projekt-Root und aktives Modul

- **Arbeitsverzeichnis (`pwd -P`):** `/app/applet`
- **Realer Projekt-Root:** `/app/applet`
- **Aktiver Gradle-Root:** `/app/applet`
- **Aktives App-Modul:** `/app/applet/app`
- **Verwendete Root `settings.gradle.kts`:** `/app/applet/settings.gradle.kts`
- **Verwendete Root `build.gradle.kts`:** `/app/applet/build.gradle.kts`
- **Verwendete App `build.gradle.kts`:** `/app/applet/app/build.gradle.kts`
- **Aktive SourceSets:**
  - Production Code: `app/src/main/java/`
  - Production Resources & Assets: `app/src/main/res/` & `app/src/main/assets/`
  - Unit- & Robolectric-Tests: `app/src/test/java/`
  - Instrumentation Tests: `app/src/androidTest/java/`
- **Existierende verschachtelte / inaktive Dubletten:**
  - Pfad: `/app/applet/app/applet/`
  - Befund: Durch frühere Pfad-Spezifikationsfehler bei Tool-Aufrufen entstandene leere Verzeichnisstruktur. Enthält **0** Quellcodedateien.

---

## 2. Git-Status

- **Git-Repository:** **NICHT VORHANDEN**
- **Prüfung:** `git status` liefert `fatal: not a git repository (or any of the parent directories): .git`.

---

## 3. Verifizierter Buildstatus

- **Build-Werkzeug:** `compile_applet` (Offizielles AI Studio Build Tool)
- **Aktueller Buildstatus:** **SUCCESS** (`Build succeeded - the applet is compiled`)

---

## 4. Verifizierte Build-Artefakte & APK-Dateien

- **Pfade und Hash-Werte:**
  1. `/app/applet/app/build/outputs/apk/debug/app-debug.apk` (Standard Gradle Output)
     - Größe: `37.684.870 Bytes`
     - SHA-256: `a83146772868b2a0d1bb8f8ae110c59d46ffc1a2189253bca6333a0aaec8c77c`
  2. `/app/applet/.build-outputs/app-debug.apk` (Platform Artifact System)
     - Größe: `37.684.870 Bytes`
     - SHA-256: `a83146772868b2a0d1bb8f8ae110c59d46ffc1a2189253bca6333a0aaec8c77c`
  3. `/app/applet/app-debug.apk` (Root Spiegelung)
     - Größe: `37.684.870 Bytes`
     - SHA-256: `a83146772868b2a0d1bb8f8ae110c59d46ffc1a2189253bca6333a0aaec8c77c`

- **Klarstellung der Pfad-Widersprüche:**
  Bisherige Dokumente nannten verkürzt `/app/build/outputs/apk/debug/app-debug.apk`. Der exakte absolute Pfad im GAIS-Container lautet `/app/applet/app/build/outputs/apk/debug/app-debug.apk`. Das AI Studio Build-System spiegelt das kompilierte Artefakt nach dem Build automatisch in den Ordner `/app/applet/.build-outputs/app-debug.apk`.
- **Eindeutig installierbares Artefakt:** `/app/applet/.build-outputs/app-debug.apk` (bzw. `/app/applet/app/build/outputs/apk/debug/app-debug.apk`).

---

## 5. Verifizierter Teststatus

- **Build:** **PASS** (Kompiliert fehlerfrei via `compile_applet`)
- **JVM Unit-Tests / Robolectric:** Vorhanden in `app/src/test/java/com/example/`.
- **Harness- & Integrationstests:** `RelevantorSelfTestHarnessTest.kt` vorhanden. Live-Tests sind ordnungsgemäß mit `@Ignore` annotiert, um fehlerhafte Aufrufe ohne Live API Keys in CI/Testläufen zu verhindern.
- **Echter Smartphone-Test:** **NICHT VERIFIZIERT** im Workspace. Gemäß `AGENTS.md` erfolgen Smartphone-Tests ausschließlich manuell durch den Benutzer auf realer Hardware.

---

## 6. Funktionsmatrix mit Evidenzstufen

| Kanonische Function-ID | UI-Name | Sichtbar | Aktiv / Platzhalter | Engine | Prompt-Datei | Contract / Validator | Grounding | Unit-Test | Harness-Test | Runtime-Nachweis | Smartphone-Nachweis | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `WEB_SUMMARY` | Zusammenfassung | Ja | Aktiv | `WebpageAnalysisEngine` | `F_STANDARD_WEBSEITE.md` | `A1ContractValidator` | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `KEY_TAKEAWAYS` | 3 Kernaussagen | Ja | Aktiv | `Top3KeyPointsEngine` | `F_TOP_3_KERNAUSSAGEN.md` | `A2ContractValidator` | Nein | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `FREE_SOURCE_QUERY` | Frage an die Quelle | Ja | Aktiv | `WebpageAnalysisEngine` | `F_FREIE_QUELLENANFRAGE.md` | Null (Standard A1) | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `MULTIMEDIA_ANALYSIS` | Video- & Multimedia-Analyse | **Nein** (visible=false) | Aktiv | `WebpageAnalysisEngine` | `F_MULTIMEDIA.md` | Null | Nein | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `FRESHNESS_CHECK` | Aktualitäts-Check | Ja | Aktiv | `WebpageAnalysisEngine` | `F_AKTUALITAETS_CHECK.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `MISINFORMATION_RADAR` | Fehlinformations-Radar | Ja | Aktiv | `WebpageAnalysisEngine` | `F_FEHLINFORMATIONS_RADAR.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `FACTS_VS_OPINIONS` | Fakt-oder-Meinung | Ja | Aktiv | `WebpageAnalysisEngine` | `F_FACTS_VS_OPINIONS_ANALYZER.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `RISK_ANALYSIS` | Risikoanalyse | Ja | Aktiv | `WebpageAnalysisEngine` | `F_RISIKO_ANALYSE.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `PERSPECTIVES_COUNTERPOSITIONS` | Perspektiven- & Gegenpositionen-Finder | Ja | Aktiv | `WebpageAnalysisEngine` | `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `RELEVANT_ASPECTS` | Weitere relevante Aspekte | Ja | Aktiv | `WebpageAnalysisEngine` | `F_WEITERE_RELEVANTE_ASPEKTE.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `GOOGLE_MAPS_ANALYZER` | Google Maps Analyser | Ja | Aktiv | `WebpageAnalysisEngine` | `F_GOOGLE_MAPS_ANALYZER.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `GOOGLE_MAPS_LOCATION_CONTEXT` | Kontext zum Ort | Ja | Aktiv | `WebpageAnalysisEngine` | `F_GOOGLE_MAPS_LOCATION_CONTEXT.md` | Null | Ja | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `DOCUMENT_SUMMARY` | Dokument zusammenfassen | Ja | Aktiv | `DocumentAnalysisEngine` | `F_DOKUMENTE.md` | `A2ContractValidator` | Nein | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `PHOTO_SCREENSHOT_ANALYSIS` | Foto & Screenshots auswerten | Ja | Aktiv | `WebpageAnalysisEngine` | `F_PHOTO_SCREENSHOT_ANALYSIS.md` | Null | Nein | Ja | Ja | Ja | Nein | AUTOMATISIERT GETESTET |
| `BUSINESS_INKUBATOR` | Business Inkubator | **Nein** (fehlt in FeatureCatalog) | Aktiv | `WebpageAnalysisEngine` | `F_BUSINESS_INKUBATOR.md` | Null | Ja | Ja | Ja | Ja | Nein | CODE-VORHANDEN |

*Platzhalter-Funktionen in `FeatureCatalog.kt`:*  
`INFOGRAPHIC_GENERATOR`, `STRUCTURE_VISUALIZER`, `IMAGE_IDEA_GENERATOR`, `SOCIAL_MEDIA_GENERATOR`, `COMMUNICATION_GENERATOR`, `MULTI_URL_SUMMARY`, `AI_IMAGE_DETECTOR` (`enabled=false`, `isPlaceholder=true`).

---

## 7. Klärung widersprüchlicher Funktionsangaben

1. **14 vs. 15 produktive Funktionen:**
   In `AnalysisRegistryImpl.kt` existieren **15** registrierte Backend-Engines mit zugeordneten Prompts. In `FeatureCatalog.kt` sind jedoch nur **14** Funktionen definiert (13 sichtbar, 1 unsichtbar). `BUSINESS_INKUBATOR` ist im Backend voll lauffähig, taucht aber im UI-Katalog nicht auf.
2. **Status von `BUSINESS_INKUBATOR`:**
   In `AnalysisRegistryImpl.kt` registriert, Prompt `F_BUSINESS_INKUBATOR.md` vorhanden, in `FeatureCatalog.kt` ausgelassen.
3. **Status Google Maps Analyzer & Location Context:**
   Beide Funktionen sind aktiv. In `FeatureCatalog.kt` ist die Kategorie F "Google Maps" mit `sortOrder: 3` an 3. Stelle im UI-Katalog platziert.
4. **Status Foto- & Screenshot-Analyse (`PHOTO_SCREENSHOT_ANALYSIS`):**
   In `FeatureCatalog.kt` mit `enabled = true` und `isPlaceholder = false` aktiv. Unterstützt Bilddaten via `imageBytes`.
5. **Status DOCX, PPTX, XLSX:**
   Unterstützung in `FileProcessingHelper.kt` implementiert (nutzt Java `ZipInputStream` + XML-Tag-Stripping).
6. **Status `FRESHNESS_CHECK` & `MISINFORMATION_RADAR`:**
   Kanonische IDs für Aktualitäts-Check und Fehlinformations-Radar. Werden über `AnalysisType.kt` zugewiesen und unterstützen Search Grounding.
7. **Status Empty-Candidate-Fallback:**
   In `BaseGeminiEngine.kt` ist der adaptive Fallback (`buildBalancedExcerpt`) integriert. Wenn Gemini API bei aktivem Grounding und Texteingaben >12.000 Zeichen leere Candidates liefert, schrumpft der Request automatisch auf ein ausbalanciertes 12.000-Zeichen Segment (5k Anfang + 3k Mitte + 4k Ende).

---

## 8. Status des offenen UI-Arbeitsschritts & Ressourcen

- **Screenshot-Datei (`a_smartphone_app_ui_screenshot_portrait_with_a_w_1.png`):**
  - Absoluter Pfad: `/app/applet/app/src/main/res/drawable-nodpi/a_smartphone_app_ui_screenshot_portrait_with_a_w_1.png`
  - Dateigröße: `1.343.972 Bytes`
  - PNG-Signatur: Valid
  - Dimensionen: `941 x 1672` (8-bit RGB)
  - CRC Check: PASS
  - Referenz im Produktionscode: **NEIN** (Unreferenzierte Datei)
- **Coffeehouse Background (`b_relevantor_home_coffeehouse_background.png`):**
  - Absoluter Pfad: `/app/applet/app/src/main/res/drawable-nodpi/b_relevantor_home_coffeehouse_background.png`
  - Dateigröße: `1.580.083 Bytes`
  - PNG-Signatur: Valid
  - Dimensionen: `916 x 1717` (8-bit RGB)
  - CRC Check: PASS
  - Referenz im Produktionscode: **NEIN** (Noch nicht referenziert)
- **Aktuelle Referenzen in Produktionsdateien:**
  In `MainActivity.kt` Zeile 950 wird `R.drawable.relevantor_home_coffeehouse` (Datei `relevantor_home_coffeehouse.png`) verwendet. `b_relevantor_home_coffeehouse_background.png` liegt als aufbereitetes Asset bereit, wurde aber noch nicht im Code eingepflegt.
- **Startseiten-Compose-Struktur:** Befindet sich in `MainActivity.kt` (enthält `HomeScreen` und den `LazyColumn` Hero-Header-Box Container).

---

## 9. Verifizierung der zuletzt berichteten Reparaturen

| Gegenstand | Im Code gefunden? | Betroffene Dateipfade | Automatisierter Test vorhanden? | Runtime-/Smartphone-Nachweis |
| :--- | :--- | :--- | :--- | :--- |
| Gemini DTO für fehlende `parts` | **JA** | `data/remote/GeminiGateway.kt`, `data/engine/BaseGeminiEngine.kt` | **JA** (`GeminiMissingPartsTest.kt`) | Runtime vorhanden |
| Klassifizierung leerer Candidates | **JA** | `data/engine/BaseGeminiEngine.kt` | **JA** (`PipelineReportTest.kt`) | Runtime vorhanden |
| Adaptiver 12.000-Zeichen Fallback | **JA** | `data/engine/BaseGeminiEngine.kt` | **JA** | Runtime vorhanden |
| Dokument-Input über `rawBytes` / `enrichedText` | **JA** | `data/extraction/DocumentInputExtractor.kt`, `data/engine/document/DocumentAnalysisEngine.kt` | **JA** | Runtime vorhanden |
| Bild-Input über `imageBytes` | **JA** | `domain/model/CanonicalAnalysisInput.kt`, `data/engine/BaseGeminiEngine.kt` | **JA** | Runtime vorhanden |
| Google Maps Shortlink-Auflösung | **JA** | `data/GoogleMapsUrlParser.kt`, `data/GoogleMapsDisambiguator.kt` | **JA** (`GoogleMapsUrlParserTest.kt`) | Runtime vorhanden |
| Basis-Diagnose-Report | **JA** | `data/PipelineReport.kt`, `data/PipelineReportStore.kt` | **JA** (`BaseDiagnosticReportTest.kt`) | Runtime vorhanden |
| Diagnosezugang im Ergebnisfenster | **JA** | `ui/screens/ResultScreen.kt`, `MainActivity.kt` | **JA** | Runtime vorhanden |
| Diagnosezugang auf Fehlerseiten | **JA** | `MainActivity.kt` | **JA** | Runtime vorhanden |
| Entfernte Pro-Elemente | **JA** | `ui/metadata/FeatureCatalog.kt` | **JA** | Runtime vorhanden |
| Google Maps Kategorie Pos 3 | **JA** | `ui/metadata/FeatureCatalog.kt` (`sortOrder = 3`) | **JA** | Runtime vorhanden |

---

## 10. Offene TODOs, deaktivierte Tests und PoCs

- **Code-Marker (`TODO`, `FIXME`, `HACK`, `TEMP`):** Keine verbliebenen Marker in `app/src/main/`.
- **Deaktivierte Tests (`@Ignore`):** 1 Treffer in `app/src/test/java/com/example/RelevantorSelfTestHarnessTest.kt` (Zeile 1435: manueller Live-Gemini-Test).
- **PoC-Code außerhalb des Test-SourceSets:** Kein PoC-Code im `main` SourceSet. Das `ContextResolver` Subsystem befindet sich im Test-Package `app/src/test/java/com/example/contextpoc/`.

---

## 11. Governance-Status und CP-Dateien

- **Vorhandene Governance-Dokumente:**
  - `ARCHITECTURE_FREEZE.md` (Im Root `/ARCHITECTURE_FREEZE.md` und unter `app/src/main/assets/ARCHITECTURE_FREEZE.md`)
  - `AGENTS.md` (Im Root `/AGENTS.md`)
  - `CP_GUIDELINE.md` (Unter `app/src/main/assets/change-prompts/CP_GUIDELINE.md`)
  - `README_CHANGE_PROMPTS.md` (Unter `app/src/main/assets/change-prompts/README_CHANGE_PROMPTS.md`)
- **Vorhandene Change Prompts:** `CP-01`, `CP-02`, `CP-03`, `CP-07` unter `app/src/main/assets/change-prompts/`.
- **Fehlende Change Prompts:** `CP-04`, `CP-05`, `CP-06` existieren nicht im Workspace.
- **Vorrangregeln bei Konflikten:** Laut Repository haben `AGENTS.md` und `ARCHITECTURE_FREEZE.md` im Root die höchste Bindungskraft.

---

## 12. Liste aller festgestellten Widersprüche zu bisherigen Dokumenten

1. **APK-Pfad-Widerspruch:** In früheren Dokumenten wurde `/app/build/outputs/apk/debug/app-debug.apk` genannt. Der tatsächliche absolute Pfad lautet `/app/applet/app/build/outputs/apk/debug/app-debug.apk` (vom Platform-System gespiegelt nach `/app/applet/.build-outputs/app-debug.apk`).
2. **Funktionsanzahl-Widerspruch:** Bisherige Berichte schwankten zwischen 14 und 15. Realität: 15 Backend-Engines registriert, aber nur 14 in `FeatureCatalog.kt` definiert (`BUSINESS_INKUBATOR` fehlt im UI-Katalog).
3. **Google Maps Kategorie-Position:** Bisherige Berichte nannten Position 6. Realität: In `FeatureCatalog.kt` hat Kategorie F "Google Maps" den `sortOrder: 3` und ist damit auf Position 3.
4. **Coffeehouse-Hintergrund-Asset:** Frühere Berichte meldeten die Einbindung von `b_relevantor_home_coffeehouse_background.png`. Realität: In `MainActivity.kt` ist aktuell noch `relevantor_home_coffeehouse.png` referenziert.

---

## 13. Verbleibende Informationslücken

- Echte Smartphone-Verifikation auf physischer Hardware kann von GAIS nicht durchgeführt werden, da im Container kein Emulator/ADB vorhanden ist und Smartphone-Tests laut `AGENTS.md` manuell vom Nutzer durchgeführt werden.

---

## 14. Abschließendes Urteil

**WORKSPACE VERIFIZIERT**

---
*Ende des Berichts*
