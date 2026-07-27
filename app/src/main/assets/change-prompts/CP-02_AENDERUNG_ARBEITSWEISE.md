# RELEVANTOR – CHANGE-PROMPT CP-02
# Veränderung der Arbeitsweise einer bestehenden Funktion

Version: 1.2 (GOVERNANCE-HARDENED)
Status: ACTIVE

---

# 1. Zweck

Dieser Change-Prompt definiert den verbindlichen Prozess für Änderungen an der Arbeitsweise einer bereits bestehenden Relevantor-Funktion.

CP-02 wird angewendet, wenn eine bestehende Funktion fachlich oder technisch verändert werden soll, ohne dass eine neue Funktion angelegt wird.

Ziele:

- Fehlerursachen sauber isolieren
- bestehende Funktionen kontrolliert verbessern
- Regressionen vermeiden
- Architektur und Contracts schützen
- Änderungen minimalinvasiv durchführen
- Runtime-Verhalten nachweisen

CP-02 ist kein Refactoring-Auftrag und kein Architekturumbau.

---

# 2. Abgrenzung zu anderen Change-Prompts

Vor Beginn muss geprüft werden, ob CP-02 der richtige Change-Typ ist.

## CP-01 – Optimierung Funktions-Prompt

Verwenden bei:

- Änderungen ausschließlich innerhalb einer Prompt-Datei
- Verbesserung von Rollenbeschreibung
- Anpassung von Analyseanweisungen
- Anpassung von Qualitätsregeln

Nicht CP-02.

---

## CP-02 – Veränderung Arbeitsweise bestehende Funktion

Verwenden bei:

- Änderung interner Verarbeitungsschritte
- Änderung von Datenflüssen
- Änderung von Entscheidungslogiken
- Änderung von API-Abläufen
- Änderung von Fehlerbehandlung
- Änderung des funktionalen Verhaltens bei unverändertem Output-Contract

---

## CP-03 – Neuanlage Funktion

Nicht CP-02 verwenden bei:

- neuer Analysefunktion
- neuer Function-ID
- neuer Registrierung
- neuer UI-Funktion

---

## CP-05 – Verarbeitung

Nicht CP-02 verwenden bei:

- URL-Extraktion
- HTML-Cleaning
- Dokument-Parsing
- Token-Limits
- Input-Aufbereitung

---

## CP-06 – Ausgabe-Contract

Nicht CP-02 verwenden bei:

- neuen JSON-Feldern
- geänderten JSON-Strukturen
- geänderten Datentypen
- Parser-/Validatoränderungen

---

## CP-07 – UI/UX

Nicht CP-02 verwenden bei:

- Layout
- Farben
- Icons
- Abstände
- reine Navigation
- visuelle Anpassungen

---

# 3. Rollenmodell

## ChatGPT

Verantwortlich für:

- fachliche Klärung
- Einordnung des Change-Typs
- Definition des Zielverhaltens
- Erstellung des GAA

---

## GAIS

Verantwortlich für:

- technische Analyse
- Dry Run
- Ermittlung betroffener Dateien
- Implementierung nach Freigabe
- Build
- Tests
- Runtime-Verifikation

GAIS verändert keine Dateien ohne vorherige Analyse.

---

# 4. Pflicht-Fachklärung vor GAA-Erstellung

Vor Erstellung eines GAA müssen folgende Punkte geklärt sein:

## Funktionsidentität

- Funktionsname
- Function-ID
- AnalysisType
- Zweck der Funktion
- aktuelle Nutzung

---

## Fehlerbild

Dokumentieren:

- Was funktioniert nicht?
- Wie äußert sich der Fehler?
- Wie reproduzierbar ist der Fehler?
- Welche Nutzeraktion löst ihn aus?

---

## Zielbild

Dokumentieren:

- gewünschtes Verhalten
- erwarteter Nutzerablauf
- erwartetes Ergebnis

---

## Abgrenzung

Festlegen:

Erlaubt:

- notwendige Änderungen zur Korrektur der bestehenden Funktion

Nicht erlaubt:

- neue Funktionen
- Architekturumbau
- Änderungen anderer Funktionen
- Contractänderungen

---

# 5. Phase 0 – Technische Diagnose

Vor jeder Implementierung führt GAIS eine vollständige Analyse durch.

Keine Dateien verändern.

Ausgabe:

1. Aktueller technischer Ablauf
2. Fehlerursache(n)
3. Betroffene Dateien
4. Risiken
5. Minimaler Änderungsumfang
6. Erwartetes Testergebnis

Danach wartet GAIS auf Freigabe.

---

# 6. Phase 1 – Baseline sichern

Vor Änderungen dokumentieren:

## Funktion

- Function-ID
- AnalysisType
- FeatureCatalog-Eintrag
- Prompt-Datei
- Engine-Zuordnung
- UI-Einstiegspunkt

---

## Dateien

Für jede betroffene Datei:

- vollständiger Dateipfad
- Zweck
- SHA-256 Hash

---

## Testbaseline

Dokumentieren:

- Buildstatus
- Unit-Teststatus
- bestehende Fehler

---

# 7. Phase 2 – Dry Run

Keine Änderungen.

GAIS dokumentiert:

## Technischer Datenfluss

Darstellung:

UI

↓

ViewModel

↓

Input-Verarbeitung

↓

Parser / Extractor

↓

Repository / Engine

↓

API / Gemini

↓

Contract

↓

Validator

↓

UI Rendering

---

## Vorher/Nachher-Vergleich

| Bereich | Aktuell | Ziel |
|---|---|---|
| Input | | |
| Verarbeitung | | |
| API | | |
| Prompt | | |
| Ergebnis | | |

---

# 8. Regeln für API- und Hybrid-Funktionen

Bei Funktionen mit externen APIs gelten zusätzliche Regeln.

Pflicht:

- API-Aufrufe niemals direkt aus UI-Komponenten
- Netzwerkzugriffe nur über Service-/Repository-Schichten
- asynchrone Verarbeitung verwenden
- Fehlerfälle definieren
- Timeout-Verhalten definieren
- Secrets niemals im Code speichern

---

# 9. Contract-Lock

Bei CP-02 bleibt der bestehende Output-Contract unverändert.

Verboten:

- neue JSON-Felder
- Entfernen bestehender Felder
- Änderung von Datentypen
- Änderung von Validatoren

Falls erforderlich:

→ CP-06 verwenden.

---

# 10. Scope-Lock

GAIS darf ausschließlich die im Dry Run freigegebenen Dateien ändern.

Verboten:

- Neben-Refactoring
- Architekturänderungen
- Codebereinigung außerhalb des Scopes
- Änderungen anderer Funktionen
- globale Optimierungen

Bei Erweiterung des Scopes:

STOPP.

Neuen Change-Typ prüfen.

---

# 11. Phase 3 – Minimalimplementierung

Nach Freigabe:

- ausschließlich geplante Änderungen durchführen
- keine zusätzlichen Verbesserungen
- keine Sammeländerungen

---

# 12. Phase 4 – Build und Tests

Pflicht:

Build:

compile_applet

Tests:

gradle :app:testDebugUnitTest

Zusätzlich prüfen:

- Registry-Tests
- Integrationstests
- relevante Regressionstests

---

# 13. Phase 5 – Runtime-Verifikation Copy PR

Der Copy PR ist die verbindliche Runtime-Wahrheit.

Prüfen:

- richtige Function-ID
- richtige Engine
- korrekter Datenfluss
- Contract PASS
- erwartetes Verhalten sichtbar

---

# 14. Rollback

Bei:

- Buildfehler
- Testfehler
- Pipelinefehler
- unerwartetem Verhalten

gilt:

1. Änderung stoppen
2. keine weiteren Reparaturversuche
3. stabile Baseline wiederherstellen
4. Fehlerbericht erstellen

---

# 15. Abschlussbericht

Format:

CP-02 ABSCHLUSSBERICHT

Baseline:
- Funktion:
- Dateien:
- Hashes:

Änderung:
- Ziel:
- Geänderte Dateien:
- Umfang:

Scope-Lock:
- eingehalten JA/NEIN

Contract-Lock:
- eingehalten JA/NEIN

Build:
- PASS/FAIL

Tests:
- PASS/FAIL

Copy PR:
- Routing:
- Verarbeitung:
- Validator:

Risiken:

Empfehlung:

Freigabe / Nachbesserung