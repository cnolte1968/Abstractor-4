# RELEVANTOR Project Context Export

**Export-Datum:** 2026-08-02  
**System / Applet:** Relevantor (Package: `com.example`)  
**Ziel:** Vollständige Projektexport-Dokumentation für die nahtlose Übergabe und Weiterentwicklung in neuen Chat-Sitzungen.

---

## Querverweise zu bestehenden Systemdokumenten

Um Redundanzen zu vermeiden, verweist dieses Dokument auf folgende bestehende, geprüfte Spezifikationen in `docs_md/` und im Projektstamm:

1. **Systemarchitektur & Schichtenmodell:** `docs_md/GAIS-Architektur_2026-08-02.md` & `docs_md/RELEVANTOR_ARCHITECTURE.md`
2. **Verzeichnis- & Dateistruktur:** `docs_md/GAIS-Verzeichnisstruktur_2026-08-02.md`
3. **Architektur-Schutzregeln:** `ARCHITECTURE_FREEZE.md`
4. **Agenten- & Deployment-Vorgaben:** `AGENTS.md`
5. **Funktionsmanifest:** `app/src/main/assets/prompts/function_registry.json`

---

## 1. Aktueller Entwicklungsstand

Der aktuelle Entwicklungsstand repräsentiert eine vollständig funktionale, lokale Android-Anwendung ("Local-First") mit 15 produktiven Qualitäts- und Analysefunktionen, die über das Gemini API Gateway (`gemini-2.5-flash`) mit und ohne Google Search Grounding betrieben werden.

### Produktive Funktionen (15/15)
- **Top-3 Kernaussagen (`TOP_3_KERNAUSSAGEN`)**: Extraktion der 3 bis 5 wichtigsten Thesen.
- **Standard Webseite (`STANDARD_WEBSEITE`)**: Universelle Inhaltszusammenfassung von Webseiten.
- **Aktualitäts-Check (`AKTUALITAETS_CHECK`)**: Verifizierung von zeitkritischen Fakten via Google Search Grounding.
- **Fehlinformations-Radar (`FEHLINFORMATIONS_RADAR`)**: Erkennung potenzieller Falschinformationen und Clickbait mit Grounding.
- **Fakten vs. Meinungen (`FACTS_VS_OPINIONS`)**: Analyse der subjektiven vs. objektiven Anteile eines Textes.
- **Risiko-Analyse (`RISIKO_ANALYSE`)**: Identifikation von Risiken, Warnungen und Implikationen.
- **Perspektiven & Gegenpositionen (`PERSPECTIVES_COUNTERPOSITIONS`)**: Beleuchtung verschiedener Blickwinkel via Grounding.
- **Business-Inkubator (`BUSINESS_INKUBATOR`)**: Extraktion von Marktchancen, Geschäftsmodellen und Monetarisierungsansätzen.
- **Weitere relevante Aspekte (`WEITERE_RELEVANTE_ASPEKTE`)**: Ergänzende Kontextaspekte und Hintergrundinformationen.
- **Freie Quellenanfrage (`FREIE_QUELLENANFRAGE`)**: Benutzerdefinierte Recherchenfragen mit Such-Grounding.
- **Dokumente (`DOKUMENTE`)**: Strukturierte PDF- und Fließtextanalyse (A2 Contract).
- **Multimedia (`MULTIMEDIA`)**: Analyse von YouTube-Videos und Audiotranskripten.
- **Fotos & Screenshots (`PHOTO_SCREENSHOT`)**: Multimodale Bild- und OCR-Analyse.
- **Google Maps Ort (`GOOGLE_MAPS`)**: Analyse von Standorten und Orten via Maps-URLs.
- **Google Maps Standortkontext (`GOOGLE_MAPS_LOCATION_CONTEXT`)**: Tiefenanalyse geografischer Umfelder und POIs.

### In Entwicklung / Proof-of-Concept (POC)
- **Context Resolver Subsystem (`com.example.contextpoc`)**: Im Test-Package (`app/src/test/java/com/example/contextpoc/`) existiert ein funktionierendes POC zur Anreicherung von Standortkontexten über externe Open-Data-Schnittstellen (Wikipedia API, Wikivoyage API, Google Places).

### Eingefrorene / Geschützte Bereiche
- **Core Clean Architecture Layering**: Definiert in `ARCHITECTURE_FREEZE.md`. Modulgrenzen zwischen `ui`, `domain` und `data` dürfen ohne explizite Freigabe nicht geändert werden.

### Bekannte Baustellen
- **UI-Visuelle Anreicherung von Multi-Source Groundings**: Darstellung von externen Suchreferenzen und Links in der UI kann noch optisch verfeinert werden.
- **Persistierung des In-Memory Pipeline Reports**: Der `PipelineReportStore` zeichnet aktuell alle Diagnoseschritte im Arbeitsspeicher auf und geht bei App-Neustart verloren.

---

## 2. Bekannte Bugs

| Bug ID | Beschreibung | Ursache | Workaround | Priorität | Betroffene Dateien |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **BUG-01** | `EMPTY_CANDIDATE_CONTENT` bei großen Eingaben mit Search Grounding | Das Gemini API Modell liefert bei übergroßen HTML-Payloads (>12k Zeichen) in Kombination mit Google Search Grounding gelegentlich HTTP 200 mit 0 Text-Parts. | Implementierter **Adaptiver 3-Segment Fallback** (`buildBalancedExcerpt` in `BaseGeminiEngine`): Automatische Kürzung auf 12k Zeichen (5k Anfang + 3k Mitte + 4k Ende) bei Retry. | High (gelöst via Engine-Fallback) | `data/engine/BaseGeminiEngine.kt` |
| **BUG-02** | HTTP 429 Rate Limiting bei rapiden Testläufen | Gemini API Key Rate-Limits bei unmittelbarer aufeinanderfolgender Ausführung mehrerer Live-API-Tests. | Verwendung des isolierten Test-Harness (`RelevantorSelfTestHarnessTest`) mit Mocks sowie Backoff-Delays. | Medium | `data/engine/BaseGeminiEngine.kt`, `GEMINI_429_TRUE_CAUSE_REPORT.md` |
| **BUG-03** | Speicherbedarf bei riesigen PDF-Inline-Payloads | Konvertierung von vielseitigen PDFs (>20MB) in Base64-Inline-Bytes belastet den JVM Heap Space. | Maximale Dateigrößenprüfung vor Base64-Konvertierung in `DocumentInputExtractor`. | Low | `data/extraction/DocumentInputExtractor.kt` |

---

## 3. Technische Schulden

1. **Direkter DTO-Aufbau in `BaseGeminiEngine`**:
   - *Beschreibung:* `BaseGeminiEngine` erstellt den JSON-Payload für den Gemini-REST-Request direkt als HashMap/DTO.
   - *Risiko:* Niedrig. Bei zukünftigen API-Versionswechseln müssen DTO-Strukturen in der Engine angepasst werden.
   - *Empfehlung:* Bei einer künftigen Überarbeitung einen dedizierten `GeminiRequestBuilder` auslagern.

2. **Verbliebene Legacy-Assets in `res/drawable/`**:
   - *Beschreibung:* Im Ordner `app/src/main/res/drawable/` existieren neben den Haupt-Icons vereinzelt ältere Bildressourcen (`relevantor_app_icon_v2.png`, `v3.png`).
   - *Risiko:* Sehr niedrig (kein Einfluss auf Funktion oder Build-Größe).
   - *Empfehlung:* Bei einem Bereinigungslauf unbenutzte Entwicklungs-Drawables entfernen.

3. **In-Memory-Diagnostikspeicher (`PipelineReportStore`)**:
   - *Beschreibung:* Laufzeit-Diagnosen werden in einer Thread-sicheren In-Memory-Liste vorgehalten.
   - *Risiko:* Niedrig. Diagnosedaten stehen nach einem App-Neustart nicht mehr zur Verfügung.
   - *Empfehlung:* Falls langfristige Tracing-Historien gewünscht sind, Speicherung in Room-Datenbank erweitern.

---

## 4. Aktuelle Roadmap

1. **Schritt 1: Transfer des Context Resolver POCs in das Produktionspaket**:
   - Überführung von `ContextResolver` aus `app/src/test/java/com/example/contextpoc/` in den produktiven `domain`/`data`-Bereich zur mehrstufigen Standortdisambiguation.
2. **Schritt 2: Export- & Share-Funktionalität für Analyseergebnisse**:
   - Implementierung einer Export-Schaltfläche (PDF / Markdown) für erzeugte Analysen aus dem Verlauf.
3. **Schritt 3: Erhöhte Benutzer-Feedback-Präzision bei Netzwerk-Timeouts**:
   - Verfeinerte UI-Fehlermeldungen im `MainViewModel` bei unterbrochenen Internetverbindungen oder API-Drosselungen.

---

## 5. Architekturentscheidungen

*(Ergänzende Beschlüsse, die nicht bereits in `GAIS-Architektur_2026-08-02.md` oder `ARCHITECTURE_FREEZE.md` enthalten sind)*

- **Local-First Transaktionssicherheit**: Jedes Analyseergebnis wird vor der UI-Präsentation synchron über `SaveAnalysisUseCase` in der Room-Datenbank (`RelevantorDatabase`) abgelegt. Dies garantiert eine 100%ige Wiederherstellbarkeit bei Prozessbeendigung durch Android OOM.
- **Entkopplung von Prompts via Asset-Verzeichnis**: Prompt-Dateien werden nicht als Kotlin-Strings kompiliert, sondern als externe Markdown-Assets geladen. Dies ermöglicht Anpassungen von Instruktionen ohne Rekompilierung der Anwendung.
- **In-Memory Pipeline Report Harness**: Für automatisierte Self-Tests und Diagnosen wurde der `PipelineReportStore` etabliert, der alle HTTP-Metriken, Fallbacks und Validation Traces zentral sammelt.

---

## 6. Promptstatus

Alle 15 Qualitätsfunktionen greifen auf typisierte Prompt-Templates im Asset-Verzeichnis `app/src/main/assets/prompts/` zu.

| Funktion / AnalysisType | Prompt-Datei | Reifegrad | Status | Bekannte Probleme |
| :--- | :--- | :--- | :--- | :--- |
| `TOP_3_KERNAUSSAGEN` | `F_TOP_3_KERNAUSSAGEN.md` | Hoch | Produktiv | Keine |
| `STANDARD_WEBSEITE` | `F_STANDARD_WEBSEITE.md` | Hoch | Produktiv | Keine |
| `AKTUALITAETS_CHECK` | `F_AKTUALITAETS_CHECK.md` | Hoch | Produktiv | Benötigt 3-Segment Fallback bei >12k Zeichen HTML |
| `FEHLINFORMATIONS_RADAR` | `F_FEHLINFORMATIONS_RADAR.md` | Hoch | Produktiv | Benötigt 3-Segment Fallback bei >12k Zeichen HTML |
| `FACTS_VS_OPINIONS` | `F_FACTS_VS_OPINIONS_ANALYZER.md` | Hoch | Produktiv | Keine |
| `RISIKO_ANALYSE` | `F_RISIKO_ANALYSE.md` | Hoch | Produktiv | Keine |
| `PERSPECTIVES_COUNTERPOSITIONS` | `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | Hoch | Produktiv | Such-Grounding erfordert saubere Quelltext-Bereinigung |
| `BUSINESS_INKUBATOR` | `F_BUSINESS_INKUBATOR.md` | Hoch | Produktiv | Keine |
| `WEITERE_RELEVANTE_ASPEKTE` | `F_WEITERE_RELEVANTE_ASPEKTE.md` | Hoch | Produktiv | Keine |
| `FREIE_QUELLENANFRAGE` | `F_FREIE_QUELLENANFRAGE.md` | Hoch | Produktiv | Grounding-Ergebnisse variieren je nach Suchbegriff |
| `DOKUMENTE` | `F_DOKUMENTE.md` | Hoch | Produktiv | Benötigt A2-Vertragsvalidierung |
| `MULTIMEDIA` | `F_MULTIMEDIA.md` | Hoch | Produktiv | Bei fehlendem YouTube-Transkript Fallback auf Metadaten |
| `PHOTO_SCREENSHOT` | `F_PHOTO_SCREENSHOT_ANALYSIS.md` | Hoch | Produktiv | Hoher Token-Bedarf bei hochauflösenden Bildern |
| `GOOGLE_MAPS` | `F_GOOGLE_MAPS_ANALYZER.md` | Hoch | Produktiv | Maps-Short-URLs erfordern vorherige HTTP-Header-Auflösung |
| `GOOGLE_MAPS_LOCATION_CONTEXT` | `F_GOOGLE_MAPS_LOCATION_CONTEXT.md` | Hoch | Produktiv | Keine |

---

## 7. Teststatus

Die Testabdeckung ist in der automatisierten Test-Suite unter `app/src/test/java/com/example/` organisiert.

| Funktion / AnalysisType | Unit-Test | Integrationstest | Runtime-Test | Smartphone-Test |
| :--- | :--- | :--- | :--- | :--- |
| `TOP_3_KERNAUSSAGEN` | PASS | PASS | PASS | PARTIAL |
| `STANDARD_WEBSEITE` | PASS | PASS | PASS | PARTIAL |
| `AKTUALITAETS_CHECK` | PASS | PASS | PASS | PARTIAL |
| `FEHLINFORMATIONS_RADAR` | PASS | PASS | PASS | PARTIAL |
| `FACTS_VS_OPINIONS` | PASS | PASS | PASS | PARTIAL |
| `RISIKO_ANALYSE` | PASS | PASS | PASS | PARTIAL |
| `PERSPECTIVES_COUNTERPOSITIONS` | PASS | PASS | PASS | PARTIAL |
| `BUSINESS_INKUBATOR` | PASS | PASS | PASS | PARTIAL |
| `WEITERE_RELEVANTE_ASPEKTE` | PASS | PASS | PASS | PARTIAL |
| `FREIE_QUELLENANFRAGE` | PASS | PASS | PASS | PARTIAL |
| `DOKUMENTE` | PASS | PASS | PASS | PARTIAL |
| `MULTIMEDIA` | PASS | PASS | PASS | PARTIAL |
| `PHOTO_SCREENSHOT` | PASS | PASS | PASS | PARTIAL |
| `GOOGLE_MAPS` | PASS | PASS | PASS | PARTIAL |
| `GOOGLE_MAPS_LOCATION_CONTEXT` | PASS | PASS | PASS | PARTIAL |

*Hinweis zu Smartphone-Tests:* Gemäß den Regeln in `AGENTS.md` erfolgen Smartphone-Tests ausschließlich manuell durch den Benutzer auf realer Hardware mit der kompilierten APK unter `/app/build/outputs/apk/debug/app-debug.apk`.

---

## 8. Bekannte Risiken

- **Architektur:** Sehr geringes Risiko. Geschützt durch `ARCHITECTURE_FREEZE.md`.
- **Laufzeit (Runtime):** Hoher Speicherverbrauch bei der Verarbeitung übergroßer Bild- oder PDF-Dateien im Arbeitsspeicher vor der Übertragung.
- **Gemini API:** Rate-Limiting (HTTP 429) bei zu vielen Anfragen pro Minute sowie gelegentliche leere Responses bei aktivem Search Grounding (abgefangen durch den Adaptiven 3-Segment Fallback).
- **Google AI Studio / Container:** Im Build-Container existiert kein Android Emulator und kein ADB. Verifizierungen erfolgen über JVM-Robolectric-Tests (`gradle :app:testDebugUnitTest`) und Kompilierungsprüfungen (`compile_applet`).
- **Android OS:** Target SDK 35 erfordert strikte Einhaltung von Edge-to-Edge `WindowInsets` im Compose Scaffold Layout.
- **Build-System:** KSP/Room Code-Generierung erfordert exakte Abstimmung der Kotlin- und Gradle-Plugin-Versionen (`2.0.21`).
- **Performance:** JSoup-Parsing im `WebInputExtractor` hängt von der Antwortzeit externer Zielwebseiten ab.

---

## 9. Wichtige Projektregeln

1. **Zero-Risk Deployment (`AGENTS.md`)**: Google AI Studio dient ausschließlich als Code-Analyse-, Schreib- und Kompilierwerkzeug. Es dürfen keine Browser-basierten Flash- oder WebUSB-Installationen ausgeführt werden.
2. **Kompilierter APK-Pfad**: Der einzige gültige Installations-Artifact-Pfad lautet:
   `/app/build/outputs/apk/debug/app-debug.apk`
3. **Schutz der Signierung**: Dateiinhalte von `/debug.keystore.base64` und Signierungskonfigurationen in `/app/build.gradle.kts` dürfen niemals modifiziert werden.
4. **Keine Code-Veränderung in Freeze-Phasen**: Strukturveränderungen an Kernschichten (`ui`, `domain`, `data`) sind strikt untersagt.
5. **Asset-Prompt-Pflicht**: Prompts dürfen nicht als Hardcoded-Strings im Kotlin-Code abgelegt werden, sondern müssen im Asset-Ordner `prompts/` liegen und in `function_registry.json` eingetragen sein.

---

## 10. Offene Entscheidungen

1. **Produktions-Integration des Context Resolvers**:
   - *Fragestellung:* Soll das in `app/src/test/java/com/example/contextpoc/` entwickelte Wikipedia/Wikivoyage-Subsystem als Standard-Inhaltsanreicherung in die Haupt-Domain integriert oder als optionales Modul belassen werden?
2. **Dauerhafte Tracing-Persistenz**:
   - *Fragestellung:* Sollen Traces aus dem `PipelineReportStore` optional in einer neuen Room-Tabelle gespeichert werden, um Verlaufsdiagnosen über App-Restarts hinweg zu ermöglichen?

---

## 11. Empfehlungen

1. **Integration des Context Resolvers aus dem Test-Package in die Produktion**:
   - *Begründung:* Das Anreichern von Standortdaten um Open-Data-Quellen erhöht die Qualität der Ortsanalysen spürbar.
2. **Beibehaltung der automatisierten Regressionstests vor jedem Release**:
   - *Begründung:* Die Test-Suite `RelevantorSelfTestHarnessTest` stellt die Einhaltung aller A1/A2-Schnittstellenverträge zuverlässig sicher.
3. **Periodische Bereinigung unbenutzter Bild-Assets**:
   - *Begründung:* Ein sauberer Resource-Ordner erleichtert die Wartung und verhindert Verwechslungen bei UI-Anpassungen.

---

## 12. Projektübergabe (Executive Summary für neue Chat-Sitzungen)

### System-Steckbrief
- **Projekt Name:** Relevantor (Android App)
- **Technologie-Stack:** Kotlin, Jetpack Compose, Material Design 3, Room DB, Retrofit2, Moshi, Kotlin Coroutines/Flow, Google Gemini API (`gemini-2.5-flash`), Google Search Grounding.
- **Architektur:** Clean Architecture + MVVM + Prompt-Driven Design.

### Was ein neuer Chat wissen MUSS:
1. **Projektstatus:** Das Projekt ist zu 100 % funktionsfähig, baut fehlerfrei über `compile_applet` und besteht alle Unit- und Integrationstests (`gradle :app:testDebugUnitTest`).
2. **Architektur-Schutz:** Die Verzeichnisstruktur und Schichtentrennung unterliegen dem `ARCHITECTURE_FREEZE.md`. Er erstelle keine neuen Schichten und ändere keine Paketstrukturen.
3. **Analyse-Funktionen:** Alle 15 Qualitätsfunktionen sind produktiv einsatzbereit. Ihre Instruktionen liegen als Markdown-Dateien unter `app/src/main/assets/prompts/` und sind in `function_registry.json` deklariert.
4. **Build & APK:** Build-Prüfungen erfolgen über `compile_applet`. Das fertige APK liegt unter `/app/build/outputs/apk/debug/app-debug.apk`.
5. **Laufzeit-Robuste Features:** Bei übergroßen Webinhalten mit Search Grounding greift in `BaseGeminiEngine` automatisch ein adaptiver 3-Segment-Excerpt-Fallback, um leere Antworten zu verhindern.

---
*Ende des RELEVANTOR Project Context Export*
