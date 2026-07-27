# RELEVANTOR – CHANGE-PROMPT CP-03
# Neuanlage einer Funktion

Version: 1.2 (GOVERNANCE-HARDENED)
Status: ACTIVE

---

# 1. Zweck

Dieser Change-Prompt definiert den verbindlichen Prozess zur sicheren Einführung einer vollständig neuen Relevantor-Funktion.

CP-03 wird angewendet, wenn eine neue Analysefunktion, KI-Funktion, API-basierte Funktion oder hybride Verarbeitungskette eingeführt wird.

Ziele:

- keine Regression bestehender Funktionen
- vollständige Integration in die Relevantor-Architektur
- kontrollierte Einführung neuer Fähigkeiten
- reproduzierbare technische Verifikation
- minimale Änderungen am bestehenden System

CP-03 ist kein Refactoring-Auftrag.

---

# 2. Anwendung

CP-03 ist verpflichtend bei:

- neuer Analysefunktion
- neuer KI-Funktion
- neuer API-Funktion
- neuer Hybrid-Funktion
- neuer Verarbeitungskette
- neuer Prompt-basierter Funktion

Beispiele:

- Google Maps Analyzer
- Dokumentanalyse
- neue Recherchefunktion
- neue Content-Verarbeitung

---

# 3. Abgrenzung zu anderen Change-Prompts

## CP-01 – Optimierung Funktions-Prompt

Verwenden bei:

- Änderung ausschließlich einer bestehenden Prompt-Datei
- Verbesserung von Analyseanweisungen
- Anpassung von Qualitätsregeln

---

## CP-02 – Veränderung Arbeitsweise bestehender Funktion

Verwenden bei:

- Änderung einer vorhandenen Funktion
- Änderung bestehender Verarbeitung
- Änderung bestehender Datenflüsse

---

## CP-03 – Neuanlage Funktion

Verwenden bei:

- neuer Function-ID
- neuer Registrierung
- neuer Analysefähigkeit
- neuer Verarbeitungskette

---

## CP-04 – Löschung Funktion

Für kontrollierte Entfernung bestehender Funktionen.

---

## CP-05 – Verarbeitung

Für:

- URL-Verarbeitung
- HTML-Cleaning
- Dokument-Extraktion
- Token-Limits

---

## CP-06 – Ausgabe-Contract

Für:

- JSON-Änderungen
- neue Felder
- geänderte Contracts
- Parser-/Validatoränderungen

---

## CP-07 – UI/UX

Für:

- Layout
- Farben
- Icons
- Navigation
- reine Darstellung

---

# 4. Rollenmodell

## ChatGPT

Verantwortlich:

- fachliche Definition der neuen Funktion
- Funktionsumfang
- Erstellung des GAA
- Prüfung der Anforderungen

---

## GAIS

Verantwortlich:

- technische Analyse
- Baseline
- Dry Run
- Implementierung
- Tests
- Abschlussbericht

GAIS entscheidet nicht eigenständig über Erweiterungen des Funktionsumfangs.

---

# 5. Phase 0 – Fachliche Klärung

Vor Erstellung eines GAA müssen folgende Punkte geklärt sein:

## Funktion

- Name
- Zweck
- Benutzerproblem
- Zielgruppe
- erwarteter Nutzen

---

## Input

Dokumentieren:

- URL
- Text
- Datei
- Medieninhalt
- externe Datenquelle

---

## Verarbeitung

Definieren:

- Prompt-basierte Verarbeitung
- API-Verarbeitung
- lokale Verarbeitung
- Hybrid-Verarbeitung

---

## Output

Definieren:

- erwartetes Ergebnis
- Darstellung
- notwendige Informationen

---

## Abgrenzung

Nicht Bestandteil:

- spätere Erweiterungen
- Optimierungen bestehender Funktionen
- Architekturumbauten ohne Notwendigkeit

---

# 6. Phase 1 – Baseline Analyse

Vor jeder Änderung erstellt GAIS eine technische Baseline.

Keine Dateien verändern.

Zu analysieren:

## Architektur

- bestehende Funktionsstruktur
- vergleichbare Funktionen
- Registrierungsmechanismen
- Testmechanismen

---

## Betroffene Bereiche

Prüfen:

- AnalysisType
- FeatureCatalog
- Function Registry
- Prompt Assets
- Engine
- Repository/API
- UI
- Tests

---

Ausgabe:

BASELINE REPORT

mit:

- aktuelle Architektur
- betroffene Komponenten
- Risiken
- geplante Integrationspunkte

---

# 7. Phase 2 – Funktions-Dry-Run

Vor Implementierung verpflichtend.

Keine Änderungen.

Der Dry Run muss enthalten:

## Funktionsbeschreibung

- Name
- Zweck
- Benutzerproblem
- Input
- Verarbeitung
- Output

---

## Architekturpfad

Darstellung:

Input

↓

Verarbeitung

↓

Prompt / API / Engine

↓

Output Contract

↓

Validator

↓

UI Rendering

---

## Betroffene Dateien

Auflisten:

- neue Dateien
- bestehende Dateien

jeweils:

- vollständiger Pfad
- Zweck
- geplante Änderung

---

## Risiken

Bewerten:

- Regression bestehender Funktionen
- Parser-Risiken
- Contract-Risiken
- API-Risiken
- UI-Risiken
- Test-Risiken

---

# 8. Architektur-Integration

Eine neue Funktion muss vollständig geprüft werden:

## Registrierung

- AnalysisType
- FeatureCatalog
- Function Registry

---

## Verarbeitung

- Engine-Zuordnung
- Repository
- API-Service
- Prompt-Datei

---

## UI

- Menü
- Funktionseintrag
- Navigation
- Ergebnisdarstellung

---

## Tests

- Unit Tests
- Integration Tests
- Runtime Tests

Keine Ebene darf übersprungen werden.

---

# 9. Regel für AnalysisType

Bei jedem neuen AnalysisType:

GAIS prüft alle betroffenen Stellen:

- AnalysisRegistryImpl
- MainActivity
- Runtime Verification
- weitere when-Auswertungen

Pflicht:

- vollständige Registrierung
- keine nicht vollständigen when-Ausdrücke
- Build erfolgreich

---

# 10. Hybrid- und API-Funktionen

Bei Funktionen mit externen APIs gilt:

Dry Run muss dokumentieren:

- verwendete API
- Authentifizierung
- Datenfluss
- asynchroner Ablauf
- Fehlerbehandlung
- Timeout-Verhalten

Regeln:

- keine API-Aufrufe direkt aus UI
- Netzwerkzugriffe nur über Service/Repository
- Secrets niemals im Code
- sichere Konfiguration über Environment/Secrets

---

# 11. Prompt-Dateien

Neue Prompt-Dateien müssen:

- korrekten Speicherort besitzen
- Namenskonvention einhalten
- Markdown-validiert sein
- korrekt registriert werden

---

# 12. PoC-Isolation

Neue Funktionen werden initial:

- isoliert
- kontrolliert
- möglichst im Debug-/PoC-Modus

eingeführt.

Keine Beeinträchtigung bestehender Produktionspfade.

---

# 13. Existing Feature Protection Lock

Bei CP-03 dürfen bestehende Funktionen nicht verändert werden.

Nicht erlaubt ohne separaten Change:

- bestehende Prompts ändern
- bestehende Parser ändern
- bestehende Contracts ändern
- bestehende Funktionen refactoren

---

# 14. Implementierung

Nach Freigabe des Dry Runs:

- nur geplante Dateien ändern
- minimale Umsetzung
- keine Neben-Refactorings
- keine zusätzlichen Funktionen

---

# 15. Build und Tests

Pflicht:

Build:

compile_applet

Tests:

gradle :app:testDebugUnitTest

Zusätzlich:

- Integration Tests
- Registry Tests
- Smoke Tests

---

# 16. Runtime-Verifikation Copy PR

Der Copy PR ist die verbindliche Wahrheit.

Nachweisen:

- Funktion registriert
- richtige Pipeline
- Prompt geladen
- Verarbeitung erfolgreich
- Output erzeugt
- UI Rendering funktioniert

---

# 17. Rollback

Bei Fehlern:

1. Änderung stoppen
2. keine weiteren Reparaturversuche
3. Baseline wiederherstellen
4. Fehlerbericht erstellen

---

# 18. Abschlussbericht

Format:

CP-03 ABSCHLUSSBERICHT

Neue Funktion:

- Name:
- Function-ID:
- Zweck:

Architektur:

- AnalysisType:
- FeatureCatalog:
- Registry:
- Prompt:
- Engine:

Dateien:

- neu:
- geändert:

Tests:

- Build:
- Unit Tests:
- Integration Tests:
- Runtime:

Copy PR:

- Status:
- Ergebnis:

Risiken:

Empfehlung:

PASS / FAILED

---

# 19. Abschlussaktion GAIS

Nach Abschluss bestätigen:

1. Dateiänderungen vollständig dokumentiert
2. keine ungeplanten Dateien verändert
3. Scope eingehalten
4. Tests durchgeführt
5. Runtime bestätigt
6. Abschlussbericht erstellt