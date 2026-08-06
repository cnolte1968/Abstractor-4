import datetime
import os

now_utc = datetime.datetime.now(datetime.timezone.utc)
timestamp = now_utc.strftime("%Y-%m-%d_%H-%M-%S")
filename = f"/app/applet/docs_md/CP03_LOCATION_QUESTION_PHASE1_PLANNER_{timestamp}_UTC.md"

header = f"""# Abschlussbericht: CP-03 Phase 1 (LocationQuestionPlanner)

Status: CP-03 PHASE 1 PASS
Dokumentversion: 1.0.0
CP-Version: 03.1
Erstellt: {now_utc.strftime("%Y-%m-%dT%H:%M:%SZ")}
Datum: {now_utc.strftime("%Y-%m-%d")}
Uhrzeit: {now_utc.strftime("%H:%M:%S")}
Zeitzone: UTC
Autor: GAIS
Projekt-Root: /app/applet
App-Modul: /app/applet/app
Git-Repository-Status: NICHT VORHANDEN
Git-Branch: NICHT VERFÜGBAR
Parent Document: RELEVANTOR_WORKSPACE_MASTER_V1_2_2026-08-02_15-15-00_UTC.md
Task-ID: 
Quell- oder Bezugsdokumente: RELEVANTOR_WORKSPACE_MASTER_V1_2_2026-08-02_15-15-00_UTC.md
absoluter Dateipfad: {filename}

## 1. Gelesener Workspace Master
`/app/applet/docs_md/RELEVANTOR_WORKSPACE_MASTER_V1_2_2026-08-02_15-15-00_UTC.md`

## 2. Verifizierter Root
`/app/applet`

## 3. Gewählter Planner-Pfad und Architekturbegründung
**Pfad:** `/app/applet/app/src/main/java/com/example/domain/engine/location/LocationQuestionPlanner.kt`
**Begründung:** Der `LocationQuestionPlanner` ist zu 100% frei von Android-, Netzwerk-, Repository- und Data-Layer-Abhängigkeiten (Pure Domain Logic). Gemäß der Regel "Ist der Planner vollständig frei von Android-, Netzwerk-, Repository- und Data-Layer-Abhängigkeiten, bevorzuge einen passenden Pfad unter domain/." wurde die Datei im `domain`-Layer abgelegt. Es existierten keine gegenteiligen Projektkonventionen.

## 4. Implementierte Kategorien
- STOSSZEITEN
- ZUGANG_MOBILITAET
- BARRIEREFREIHEIT
- PARKEN
- ATMOSPHAERE_AUSSTATTUNG
- FAMILIEN_KINDER
- HISTORIE_KULTUR
- PREISE_OEFFNUNGSZEITEN
- SAISON_EVENTS
- SONSTIGE

## 5. Source-Selection-Regeln
- **Pflicht (Immer):** PLACES
- **Optional (Immer):** REVIEWS
- **Zusätzlich je nach Kategorie:**
  - STOSSZEITEN -> `LOCATION_CONTEXT` (Pflicht)
  - BARRIEREFREIHEIT, PARKEN -> `LOCATION_CONTEXT` (Pflicht)
  - ZUGANG_MOBILITAET -> `LOCATION_CONTEXT` (Pflicht), `WIKIVOYAGE` (Optional)
  - HISTORIE_KULTUR -> `WIKIPEDIA` (Pflicht), `WIKIVOYAGE` (Optional)
- SEARCH_GROUNDING wird als Pflichtquelle hinzugefügt, falls die Grounding-Regel zutrifft.

## 6. Grounding-Regeln
**Grounding = TRUE bei:**
- Kategorie `PREISE_OEFFNUNGSZEITEN`
- Kategorie `SAISON_EVENTS`
- Eindeutigen Zeitbegriffen: "heute", "aktuell", "jetzt", "feiertag", "derzeit", "momentan", "aktuelle ausstellung"
- Jahreszahlen (allgemeiner Zeitbezug, Regex-gestützt)

**Grounding = FALSE bei:**
- Rein statischen Fragen wie Historie, Barrierefreiheit, Parkstruktur, allgemeiner Zugang, Ausstattung (sofern kein Zeitbezug enthalten ist).

## 7. Fragevalidierung
- **Leere Frage / Blank:** Returns `null`
- **Reiner Smalltalk** (hallo, wie geht es, tschüss etc.): Returns `null`
- **Offensichtlich ortsfremde Frage** (wer ist der bundeskanzler, rezept für, wie groß ist der mond etc.): Returns `null`
- **Indirekte, gültige Ortsfragen** (Ist das für ältere Menschen geeignet?, Lohnt es sich bei Regen?): Werden über Keywords korrekten Kategorien (ZUGANG_MOBILITAET, ATMOSPHAERE_AUSSTATTUNG) zugeordnet.

## 8. Ausgeführte Tests und Ergebnisse
Die Testdatei `/app/applet/app/src/test/java/com/example/LocationQuestionPlannerTest.kt` beinhaltet 18 Testmethoden für die 20 geforderten Prüfszenarien. Alle Tests wurden erfolgreich (`PASS`) ausgeführt.

## 9. Geplante neue Dateien
1. `/app/applet/app/src/main/java/com/example/domain/engine/location/LocationQuestionPlanner.kt`
2. `/app/applet/app/src/test/java/com/example/LocationQuestionPlannerTest.kt`
3. `/app/applet/docs_md/CP03_LOCATION_QUESTION_PHASE1_PLANNER_..._UTC.md` (Dieser Bericht)

## 10. Tatsächlich neue Dateien
- `/app/applet/app/src/main/java/com/example/domain/engine/location/LocationQuestionPlanner.kt`
- `/app/applet/app/src/test/java/com/example/LocationQuestionPlannerTest.kt`
- {filename}

## 11. Tatsächlich geänderte Dateien
- KEINE

## 12. Abweichungen vom Change Budget
- KEINE

## 13. Buildstatus
PASS (`compile_applet` erfolgreich)

## 14. Unit-Teststatus
PASS (`gradle :app:testDebugUnitTest` erfolgreich)

## 15. Abschlussstatus
**CP-03 PHASE 1 PASS**
"""

with open(filename, "w") as f:
    f.write(header)
