# Implementierungsbericht: Perspektiven- & Gegenpositionen-Finder

Dieses Dokument dokumentiert die erfolgreiche Implementierung der neuen Analysefunktion **„Perspektiven- & Gegenpositionen-Finder“** in der Android-App **Abstractor**.

---

## 1. Übersicht & Zielsetzung

Die Funktion unterstützt Wissensarbeiter dabei, zu einem gelesenen Inhalt gezielt Gegenargumente, alternative Sichtweisen, differenzierte Expertenmeinungen und blinde Flecken aufzudecken. Sie dient primär der Vermeidung von **Informationsblasen**, **einseitigen Argumentationen** und dem sogenannten **Bestätigungsfehler (Confirmation Bias)**. Der Nutzer erhält dadurch eine strukturierte Übersicht darüber, welche relevanten Gegenpositionen, Interessengruppen oder alternative Denkrichtungen in der ursprünglichen Quelle nicht oder nur unzureichend abgebildet wurden.

---

## 2. Technische Architektur & Datenfluss

Der Datenfluss fügt sich nahtlos in die bestehende, robuste MVVM-Architektur der App ein:

1. **Auswahl im UI (`MainActivity.kt`)**: 
   - Ein neuer Eintrag wurde in das `CockpitLayout` integriert: **„Perspektiven- & Gegenpositionen-Finder“** mit der Kurzbeschreibung *„Findet Gegenargumente, alternative Sichten und kritische Perspektiven zum Inhalt.“* (Icon: `🔄`, Farbe: Emerald Green).
   
2. **Zustand & Verarbeitung (`MainViewModel.kt`)**:
   - Nach der Auswahl wird der neue Analysetyp `PERSPECTIVES_AND_COUNTERPOSITIONS` getriggert.
   - Es wird direkt geprüft, ob ausreichender Text vorliegt (entweder durch direkt übergebenen Text oder ein YouTube-Transkript). Wenn die Quelldaten nicht auswertbar sind (Länge < 500 Zeichen), bricht das System gemäß den Vorgaben sauber ab und zeigt den vordefinierten Fehler-Status an (siehe Abschnitt *Fehlerbehandlung*).
   
3. **Schnittstelle zur Gemini API (`GeminiNetwork.kt` & `GeminiModels.kt`)**:
   - Die `AnalysisType`-Enumeration wurde um den Wert `PERSPECTIVES_AND_COUNTERPOSITIONS` erweitert.
   - In `GeminiNetwork.kt` wurde ein hochkarätiger, systemischer Prompt entworfen, der das KI-Modell instruiert, als unbestechlicher Perspektiven-Analyst zu agieren.
   - Die Ausgabestruktur wird über das native Jetpack JSON-Response-Schema (Moshi-basiert) validiert und verarbeitet.

---

## 3. Prompting & Quellen-Integrität (Anti-Halluzinations-Schutz)

Der integrierte Prompt folgt strengsten Richtlinien zur Vermeidung von „KI-Slop“ und Halluzinationen:

* **Strikte Datenbasis**: Das KI-Modell arbeitet ausschließlich auf Basis des real übergebenen Inhalts der Quell-URL.
* **Keine Fiktion**: Es ist dem Modell unter Androhung von Verarbeitungsfehlern verboten, Scheinquellen (z. B. `example.com`), Platzhalter-Links oder nicht verifizierte externe URLs zu erfinden.
* **URL-Erhalt**: Identifizierte echte Quellen-URLs aus dem Verarbeitungskontext werden zeichengenau, vollständig und unverschlüsselt beibehalten (z. B. im Format `Quelle: [vollständige_unveränderte_url]`), damit diese im UI anklickbar/kopierbar gerendert werden können.
* **Ehrlichkeit bei fehlenden Quellen**: Gibt es keine direkten externen Belege, formuliert das Modell die logische Gegenposition rein analytisch auf Basis des Textes, verzichtet aber vollständig auf erfundene Links.

---

## 4. Strukturierte Ausgabe & UI-Darstellung

Das zurückgegebene JSON-Format gliedert sich in:
* `title`: Der authentische Titel des Ausgangsdokuments.
* `owner`: Name der Autoren/Organisation, falls erkennbar.
* `original_url`: Die Original-URL der analysierten Quelle.
* `short_description`: Eine prägnante, zweisätzige Einordnung über die gefundenen Gegenargumente (z. B. ein Hinweis darauf, welche Aspekte im Artikel unzureichend dargestellt sind).
* `key_takeaways`: Eine präzise, flache Liste aus bis zu 7 zentralen Gegenpositionen (ohne verschachtelte Aufzählungspunkte, Bindestriche oder Zeilenumbrüche), die einleitend ein fettgedrucktes Thema als Leitbegriff enthalten.

---

## 5. Fehlerbehandlung & Robustheit (Inhalt nicht auswertbar)

Falls eine URL (z. B. durch Paywalls, vorgeschaltete Logins oder technische Blockaden) keinen auslesbaren Text liefert, greift das robuste Fallback-System:

* **Titel**: *„Inhalt nicht auswertbar“*
* **Kurzbeschreibung**: *„Für diese Quelle konnte kein ausreichender Inhalt geladen werden, um die angeforderte Analyse zuverlässig durchzuführen.“*
* **Kernaussagen (keyTakeaways)**:
  - *„Die Funktion benötigt tatsächlich auslesbaren Inhalt der Quelle.“*
  - *„Aus URL, Titel oder Metadaten werden bewusst keine fachlichen Ergebnisse erzeugt.“*
  - *„Bitte prüfe die URL oder versuche eine andere Quelle.“*

Dadurch werden "Schmuck-Antworten" oder frei erfundene Statements basierend auf URLs oder Snippets vollständig unterbunden.

---

## 6. Modifizierte Dateien in diesem Zyklus

1. `app/src/main/java/com/example/data/GeminiModels.kt`
   - Hinzufügen des neuen Enums `AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS`.
2. `app/src/main/java/com/example/data/GeminiNetwork.kt`
   - Entwurf des umfassenden System-Prompts für Gegenpositionen.
   - Integration der robusten Extraktion und Bereinigung der Markdown/Text-Outputs.
   - Festlegung der optimalen Generierungs-Temperatur (0.2).
3. `app/src/main/java/com/example/ui/MainViewModel.kt`
   - Einbettung der inhaltlichen Längenprüfungen (`hasEnoughRealContent`) für YouTube-Transkripte und Webinhalte.
   - Zuweisung des spezifischen Fehler-Zustands bei leeren Crawling-Ergebnissen.
4. `app/src/main/java/com/example/MainActivity.kt`
   - Integration des Analysetyps im Cockpit-Bildschirm inklusive Icon-Gestaltung (`🔄`) und farblicher Kennzeichnung.
   - Implementierung der dynamischen UI-Karten-Überschriften.

---
*Bericht erstellt am 11. Juni 2026 für das Projekt „Abstractor“.*
