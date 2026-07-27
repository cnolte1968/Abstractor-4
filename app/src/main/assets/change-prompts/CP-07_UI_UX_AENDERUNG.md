# RELEVANTOR – CHANGE-PROMPT CP-07
# Änderung an CD / UI / UX

Version: 1.2 (GOVERNANCE-HARDENED)
Status: ACTIVE

---

# 1. Zweck

Dieser Change-Prompt definiert den verbindlichen Prozess für Änderungen an der Benutzeroberfläche von Relevantor.

CP-07 schützt die Trennung zwischen:

- UI-Darstellung
- Funktionslogik
- Datenverarbeitung
- KI-Verarbeitung
- API-Integration

Ziele:

- sichere Weiterentwicklung der Benutzeroberfläche
- Vermeidung unbeabsichtigter funktionaler Änderungen
- Schutz bestehender Analysefunktionen
- kontrollierte Anpassung von Compose-Komponenten
- minimale und nachvollziehbare Änderungen

Grundregel:

Eine UI-Änderung bleibt ausschließlich eine UI-Änderung.

---

# 2. Anwendung

CP-07 ist verpflichtend bei:

- Layoutänderungen
- Compose-Anpassungen
- Iconänderungen
- Abständen
- Größenänderungen
- Farben
- Themes
- Kartenlayouts
- Navigationselementen
- visueller Benutzerführung

Beispiele:

- Icon-Ausrichtung
- Startseitenoptimierung
- Button-Anpassungen
- Dialog-Optimierungen
- Card-Layouts

---

# 3. Abgrenzung

## CP-01 – Promptänderung

Nicht CP-07 bei:

- Änderungen an Prompt-Dateien
- Analyseanweisungen
- KI-Verarbeitungsregeln

---

## CP-02 – Funktionsverhalten

Nicht CP-07 bei:

- Änderung von Datenflüssen
- Änderung von Logik
- Änderung von Verarbeitungsschritten

---

## CP-03 – Neue Funktion

Nicht CP-07 bei:

- neuer Funktion
- neuer Registrierung
- neuer Analysefähigkeit

---

## CP-05 – Verarbeitung

Nicht CP-07 bei:

- URL-Verarbeitung
- Dokument-Extraktion
- HTML-Cleaning
- Token-Verarbeitung

---

## CP-06 – Output Contract

Nicht CP-07 bei:

- JSON-Änderungen
- Parseränderungen
- Validatoränderungen

---

Wenn eine UI-Änderung technische Logik beeinflusst:

STOPP.

Neuen Change-Typ prüfen.

---

# 4. Rollenmodell

## ChatGPT

Verantwortlich:

- fachliche Beschreibung
- Änderungsziel
- Erstellung GAA
- Scope-Prüfung

---

## GAIS

Verantwortlich:

- technische Analyse
- UI-Dry-Run
- Umsetzung
- Build
- Tests
- Abschlussbericht

GAIS darf keine zusätzlichen Optimierungen außerhalb des Auftrags durchführen.

---

# 5. Phase 0 – Fachliche Klärung

Vor Erstellung eines GAA müssen geklärt sein:

## Ziel

- Welcher UI-Bereich soll geändert werden?
- Welches Problem besteht?
- Welches Zielverhalten wird erwartet?

---

## Umfang

Dokumentieren:

- betroffener Screen
- betroffene Komponente
- gewünschte Änderung

---

## Ausschluss

Nicht Bestandteil:

- neue Funktionen
- neue Daten
- neue Logik
- neue Verarbeitung

---

# 6. Phase 1 – UI Baseline

Vor jeder Änderung analysiert GAIS:

Keine Dateien ändern.

Zu dokumentieren:

- betroffene Screens
- Compose-Komponenten
- Dateien
- Abhängigkeiten
- aktuelles Verhalten

Ausgabe:

UI BASELINE REPORT

---

# 7. Phase 2 – UI Dry Run

Vor Implementierung verpflichtend.

Keine Änderungen.

Der Dry Run enthält:

## Betroffene Dateien

Liste:

- vollständiger Pfad
- Zweck
- geplante Änderung

---

## UI-Komponenten

Dokumentieren:

- Screen
- Composable
- Row
- Column
- LazyColumn
- Card
- Button
- Dialog

---

## Vorher/Nachher

Darstellung:

Vorher:

...

Nachher:

...

---

## Risikoanalyse

Prüfen:

- nur Darstellung betroffen
- kein State geändert
- keine Navigation geändert
- keine Funktionsauswahl geändert
- kein Datenfluss betroffen

---

# 8. Baseline-Sicherung

Vor Änderung dokumentieren:

- Git-Status
- betroffene Dateien
- SHA-256 Hash der UI-Dateien
- aktueller Buildstatus

---

# 9. Strikter Datei-Lock

CP-07 erlaubt ausschließlich UI-nahe Änderungen.

Erlaubt:

- Compose Screen Dateien
- UI-Komponenten
- Theme-Dateien
- Darstellungsressourcen

Optional:

- strings.xml
- colors.xml

nur minimal und zwingend notwendig.

---

# 10. Verbotene Änderungen

Unter CP-07 strikt verboten:

- ViewModels
- StateFlows
- Event-Handler
- Datenmodelle
- Repository-Schichten
- Analyse-Engine
- API-Services
- FeatureCatalog
- AnalysisType
- Registry
- Prompt-Dateien

Besonders verboten:

- app/src/main/java/.../data/engine/
- app/src/main/assets/prompts/

Bei Notwendigkeit:

STOPP.

Neuen Change-Prompt verwenden.

---

# 11. UI-State-Regel

Keine Änderungen an:

- Business-State
- ViewModel-State
- Datenflüssen

Wenn zusätzliche Daten für UI benötigt werden:

→ CP-02 oder CP-05 prüfen.

---

# 12. Minimalimplementierung

Erlaubt:

- Modifier
- Alignment
- Padding
- Größe
- Farbe
- Darstellung
- Layoutstruktur

Nicht erlaubt:

- Refactoring
- Codebereinigung
- Architekturänderung
- Sammeländerungen

---

# 13. Build und Tests

Nach Umsetzung:

Pflicht:

Build:

compile_applet

Tests:

gradle :app:testDebugUnitTest

Zusätzlich:

- bestehende Regressionstests prüfen

---

# 14. Visuelle Runtime-Verifikation

Jede UI-Änderung benötigt eine reale Prüfung.

Prüfen:

- Smartphone
- Tablet, falls betroffen
- Textumbrüche
- Icons
- Abstände
- Bedienbarkeit

Falls Screenshot-Tests vorhanden:

- ausführen
- Unterschiede dokumentieren

---

# 15. Accessibility

Bei UI-Anpassungen prüfen:

- ausreichende Touch-Flächen
- sinnvolle contentDescription
- keine Verschlechterung der Bedienbarkeit

---

# 16. Copy PR Runtime-Nachweis

Der Copy PR ist die zentrale Runtime-Wahrheit.

Nachweisen:

- App startet
- betroffener Screen funktioniert
- Navigation funktioniert
- bestehende Funktionen bleiben erreichbar

---

# 17. Rollback

Bei:

- Buildfehler
- Testfehler
- unerwartetem Verhalten

gilt:

1. Änderung stoppen
2. keine weiteren Reparaturversuche
3. Baseline wiederherstellen
4. Fehlerbericht erstellen

---

# 18. Abschlussbericht

Format:

CP-07 ABSCHLUSSBERICHT

## Änderung

- Ziel:
- Screen:
- UI-Komponente:

## Dateien

Geändert:

- ...

Nicht geändert:

- ...

## Scope-Lock

Eingehalten:

JA / NEIN

## Build

PASS / FAILED

## Tests

PASS / FAILED

## Visuelle Prüfung

Ergebnis:

## Copy PR

Status:

## Risiken

## Empfehlung

PASS / FAILED

---

# 19. Definition abgeschlossen

CP-07 ist abgeschlossen wenn:

- Dry Run bestätigt
- Scope eingehalten
- ausschließlich erlaubte Dateien geändert
- Build erfolgreich
- Tests erfolgreich
- visuelle Prüfung erfolgt
- Abschlussbericht erstellt