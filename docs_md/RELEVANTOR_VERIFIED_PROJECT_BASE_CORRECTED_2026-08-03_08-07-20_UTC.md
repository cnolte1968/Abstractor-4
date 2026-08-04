# RELEVANTOR VERIFIED PROJECT BASE (CORRECTED)

**Erstellt am:** 2026-08-03 08:07:20 UTC  
**Dokument-ID:** `RELEVANTOR_VERIFIED_PROJECT_BASE_CORRECTED_2026-08-03_08-07-20_UTC`  
**Durchgeführt von:** GAIS  
**Arbeitsmodus:** Reiner Analyse-, Konsolidierungs- & Dokumentationsmodus (Korrektur des Pfadmodells, keine Code- oder Git-Änderungen)  
**Repository:** `cnolte1968/Abstractor-4`  

---

## 1. GESICHERTE FAKTEN

1. **Gültiger Projekt-Root:**  
   Der Projekt-Root gemäß der bestätigten GAIS-Verzeichnisstruktur ist die oberste sichtbare Projektebene. Es gibt keine Annahme eines zusätzlichen Zwischenordners.
2. **Bestätigte relative Verzeichnisstruktur:**  
   Die Auswertung der primären Verzeichnisquelle (`GAIS-Verzeichnisstruktur_2026-08-03.md`) belegt die Existenz folgender relativer Hauptpfade:
   - `app/` (Android-Anwendungsmodul, Quellcode `app/src/main/java/com/example/`, Tests `app/src/test/java/com/example/`, Ressourcen `app/src/main/res/`)
   - `assets/` (Prompts, Qualitätsregeln, `function_registry.json`)
   - `docs_md/` (Dokumentation und Audit-Protokolle)
   - `gradle/` & Build-Konfigurationsskripte (`build.gradle.kts`, `settings.gradle.kts`)
3. **Architekturmodell & Schichten:**  
   Gemäß `GAIS-Architektur_2026-08-02.md` folgt das System strikt der Clean Architecture + MVVM unter `com.example`:
   - **UI Layer (`app/src/main/java/com/example/ui/`)**: Jetpack Compose, `MainViewModel`, `FeatureCatalog`, `OutputPresentationPolicy`.
   - **Domain Layer (`app/src/main/java/com/example/domain/`)**: Use Cases (`AnalyzeContentUseCase`, `ExtractContentUseCase`), Engine-Schnittstellen (`AnalysisRegistry`, `AnalysisEngine`), `LocationQuestionCoordinator`.
   - **Data Layer (`app/src/main/java/com/example/data/`)**: Extraktoren (`data/extraction`), KI-Engines (`data/engine`), Local Database (`data/local` / Room), Diagnostic Store (`PipelineReportStore`).
4. **CP-03 Implementierungsstand:**  
   Gemäß `RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` und Quellbaum ist die Kategorie `GOOGLE_MAPS_LOCATION_QUERY` mit `LocationQuestionPlanner`, `LocationQuestionCoordinator` und `LocationQuestionEngine` im Quellcode vorhanden.

---

## 2. NICHT VERIFIZIERTE AUSSAGEN

1. **Host-spezifische absolute Pfade:**  
   Alle absoluten Host-Pfade außerhalb der sichtbaren Projektstruktur sind nicht verifiziert.
2. **Ursachenhypothesen zum lokalen Git-Status:**  
   Spekulative Annahmen über Ursachen von Git-Index-Fehlern ohne Beleg in den Primärquellen.
3. **Subjektive Preview- / Smoke-Test-Freigaben:**  
   Unbelegte Aussagen aus früheren Chat-Verläufen ohne protokollierte Testnachweise.

---

## 3. WIDERSPRÜCHE AUS VORHERIGEN LÄUFEN

1. **Inkonsistente absolute Pfadannahmen:**  
   In vorherigen Läufen wurden variierende absolute Host-Pfade und unbeabsichtigte Pfadverschachtelungen angenommen. Die korrigierte Datenbasis verwendet ausschließlich die sichtbare relative Projektstruktur.
2. **Widersprüchliche Aussagen zum Repository-Zustand:**  
   Variierende Angaben in früheren Berichten bezüglich der lokalen Git-Objektdatenbank vs. Remote-Repository.

---

## 4. GÜLTIGE ARBEITSREGELN

1. **Exklusive Verwendung relativer Pfade:**  
   Sämtliche Pfadreferenzen beziehen sich ausschließlich auf die sichtbare relative Projektstruktur (`app/`, `assets/`, `docs_md/`, `gradle/`).
2. **Keine Annahme von Zwischenordnern:**  
   Es werden keine zusätzlichen absoluten Pfadpräfixe oder verschachtelte Ordnerstrukturen vorausgesetzt.
3. **Keine technischen Eingriffe:**  
   Keine Modifikation von produktivem Quellcode unter `app/src/`, `assets/` oder `gradle/`.

---

## 5. AKTUELLER PROJEKTSTATUS

- **Quellcode & Business Logic:** CP-03 (Google Maps Location Context Analysis) im Quellbaum unter `app/src/main/java/com/example/` vorhanden.
- **Projektstruktur:** Konsolidiert auf Basis der relativen Pfadstruktur (`app/`, `assets/`, `docs_md/`, `gradle/`).
- **Dokumentationsstand:** Aktualisiert und korrigiert in `docs_md/`.

---

## 6. RISIKEN

- **Risiko historischer Fehldiagnosen:** Die frühere Verwendung falscher absoluter Pfade kann zu Fehldiagnosen und Pfadverschachtelungen geführt haben. Das vorliegende Modell behebt dieses Risiko durch strikte Relativierung auf die sichtbare Projektebene.

---

## 7. NÄCHSTER ZULÄSSIGER SCHRITT

- Keine weitere Handlung empfehlen.

---

**ABSCHLUSSSTATUS:**  
`PATH MODEL CORRECTED`
