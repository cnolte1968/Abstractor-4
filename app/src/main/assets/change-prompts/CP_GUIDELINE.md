# CHANGE PROMPT GOVERNANCE GUIDELINE

## Document Metadata

- Document Version: 1.1
- Status: ACTIVE
- Created: 2026-07-15
- Last Modified: 2026-07-15
- Change Process: GOVERNANCE
- Change ID: GOVERNANCE-002

---

## 1. Zweck der Change-Prompt-Systematik

Change Prompts (CP) definieren standardisierte Verfahren zur kontrollierten Weiterentwicklung von Relevantor.

Ziel:
- keine spontanen Änderungen
- keine Architekturdrifts
- reproduzierbare Verbesserungen
- klare Trennung zwischen fachlicher Definition und technischer Umsetzung


# 2. Grundprinzipien für alle CPs

## 2.1 Fachlichkeit vor Technik

Jeder Change startet mit:
- Nutzerproblem
- gewünschtem Nutzen
- fachlichem Zielbild

Keine technische Lösung darf das fachliche Problem ersetzen.


## 2.2 ChatGPT und GAIS haben unterschiedliche Rollen

ChatGPT:
- fachliche Analyse
- Anforderungsaufnahme
- Funktionsdesign
- Erstellung des GAA

GAIS:
- technische Selbstermittlung
- Analyse der Codebasis
- Umsetzung
- Tests
- technische Dokumentation


## 2.3 Keine Umsetzung ohne vollständige Klärung

Vor jedem GAA müssen geklärt sein:

- Problem
- Ziel
- gewünschter Nutzen
- Input
- Output
- Qualitätskriterien
- No-Gos


# 3. Standardstruktur einer Funktionsoptimierung

Jede Funktionsoptimierung analysiert:

## 3.1 Funktionsidentität

- Welche Funktion?
- Welche technische ID?
- Welche Engine?
- Welcher Contract?
- Welcher Prompt?


## 3.2 Nutzerproblem

Was funktioniert aktuell nicht?

Beispiele:
- falsche Inhalte
- fehlende Perspektive
- zu viel Redundanz
- falsche Gewichtung


## 3.3 Nutzer-Nutzen

Welche Erkenntnis soll der Nutzer erhalten?


## 3.4 KI-Rolle

Jeder Funktionsprompt definiert:

- Fachrolle
- Denkmodell
- Analyseperspektive


## 3.5 Input-Spezifikation

Definieren:

- verfügbare Daten
- fehlende Daten
- Grenzen
- verbotene Quellen


## 3.6 Relevanzlogik

Jede Funktion braucht:

- Was ist wichtig?
- Was ist unwichtig?
- Was wird verworfen?


## 3.7 Output-Spezifikation

Definieren:

- Felder
- Struktur
- Länge
- Sprache
- Format


## 3.8 Negativregeln

Jede Funktion benötigt:

- was darf nicht passieren?
- welche Halluzinationen vermeiden?
- welche Fehlinterpretationen vermeiden?


# 4. Architekturregeln

## 4.1 Keine unnötigen Core-Änderungen

Vor Änderungen prüfen:

- Prompt ausreichend?
- Presentation Layer ausreichend?
- Registry ausreichend?
- Engine-Erweiterung notwendig?


## 4.2 Schichten sauber trennen

Prompt:
fachliche KI-Anweisung

Engine:
technische Verarbeitung

Parser:
Strukturierung

Domain:
Datenmodell

Presentation:
Darstellung


## 4.3 UI-Probleme nicht über Prompts lösen


# 5. Prompt-Versionierung

Jeder Funktionsprompt benötigt Metadaten:

- Function Key
- Prompt Version
- Status
- Created
- Last Modified
- Change Process
- Change ID
- Previous Version


Keine Versionierung im Dateinamen.


# 6. Test- und Freigabestandards

Jeder Change benötigt:

## Technisch

- Build
- Unit Tests
- Regression Tests


## Fachlich

- realistischer Testinput
- erwartetes Verhalten
- PASS/FAIL Bewertung


## Runtime

Bei relevanten Änderungen:

- echter App-Test
- Screenshotprüfung
- Ergebnisqualität


# 7. Change-Größen

## Klein

Beispiel:
UI-Abstand ändern

## Mittel

Beispiel:
PresentationPolicy erweitern

## Stark

Beispiel:
Prompt-Logik komplett renovieren


# 8. Freeze-Regeln

Nach erfolgreichem Change:

- Version dokumentieren
- Status setzen
- keine parallelen Änderungen


# 9. GAA-Standard

Jeder Umsetzungsvorschlag endet mit einem GAA.

Der GAA enthält:

- Ziel
- Kontext
- betroffene Dateien
- Grenzen
- Aufgaben
- Tests
- erwartete Ausgabe


# 10. Zusätzliche Governance-Regeln

## 10.1 Read-Before-Write
Vor jeder Änderung an bestehenden Dateien ist die Datei zwingend zu lesen, um den aktuellen Zustand exakt zu prüfen. Es dürfen keine Annahmen oder veraltete Informationen aus früherem Kontext verwendet werden.

## 10.2 Prompt Hash Governance
Bei jeder Änderung an einem Prompt muss im Änderungsbericht der SHA-256-Hashwert der Prompt-Datei dokumentiert werden:
- Alter SHA-256 Hashwert
- Neuer SHA-256 Hashwert

## 10.3 Testpflicht nach Änderungstyp
Die Verifikationsmethode richtet sich nach dem Typ des vorgenommenen Changes:
- **Codeänderung (Core/Engine/Parser/Domain/etc.):** Erfolgreicher Build und das fehlerfreie Bestehen aller Unit Tests.
- **Promptänderung:** Erfassung der Prompt-Hashes, erfolgreiches Durchführen der relevanten Funktionstests sowie ein fehlerfreier Build.
- **UI-Änderung (MainActivity/Components/Themes):** Erfolgreicher Build sowie anschließende visuelle Screenshot-Verifikation und Runtime-Tests.

## 10.4 Schutz der CP_GUIDELINE
Die Datei `CP_GUIDELINE.md` selbst ist ein geschütztes Governance-Dokument. Jegliche Änderungen an ihr bedürfen eines eigenständigen Governance-GAAs, eines formellen Reviews sowie einer anschließenden manuellen Erhöhung der Dokument-Version im Metadatenblock.

## 10.5 Quality Gap First
Vor jeder Prompt-Optimierung ist zwingend zuerst eine Qualitätsanalyse durchzuführen:
- Eine konkrete Qualitätslücke muss identifiziert werden.
- Ein reales oder repräsentatives Beispiel muss dokumentiert werden.
- Ein ungenügendes Ergebnis (Schlechtes Ergebnis) muss präzise beschrieben werden.
- Das gewünschte Zielergebnis (Gutes Ergebnis) muss im Vorfeld definiert werden.

## 10.6 Evidenzpflicht
Jede Prompt-Optimierung erfordert eine nachweisbare Verbesserung der Qualität basierend auf:
- Dem bereitgestellten Testinput.
- Dem aktuellen (fehlerhaften/ungenügenden) Ergebnis.
- Der detaillierten Beschreibung des Qualitätsproblems.
- Dem konkret gewünschten Ergebnis.

## 10.7 PASS/FAIL Kriterien
Vor der tatsächlichen Umsetzung müssen klare Abnahmebedingungen definiert werden:
- **Fachliche Qualitätskriterien:** Inhaltliche Präzision, Informationsdichte, logische Kohärenz, Redundanzfreiheit.
- **Technische Prüfkriterien:** Korrektes Format, Einhaltung von Schemata/Contracs, fehlerfreier Parser-Durchlauf.
- **Abnahmebedingungen:** Was muss exakt erfüllt sein, damit der Change als "erfolgreich" gilt.

## 10.8 Klassifikation der Änderungsintensität
Jede Prompt-Änderung muss im Vorfeld nach ihrer Intensität klassifiziert werden, um den Verifikationsaufwand festzulegen:
- **KLEIN:** Reine Anpassungen von Formulierungen, Sprache, Tonalität oder Beispielen im Prompt.
- **MITTEL:** Anpassung der Relevanzlogik, Hinzufügen/Ändern von Filtern oder Verfeinerung der Output-Regeln.
- **STARK:** Einführung einer neuen Analyse-Methode, grundlegend geänderter Nutzer-Nutzen oder eine komplette, umfassende Prompt-Renovierung. Starke Änderungen erfordern eine besonders intensive Validierung auf realen Endgeräten und eine umfassende manuelle Abnahme.
