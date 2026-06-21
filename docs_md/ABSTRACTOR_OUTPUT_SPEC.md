# ABSTRACTOR OUTPUT SPECIFICATION (AUSGABE-SPEZIFIKATION)

Dieses Dokument definiert die spezifischen Ausgabe-Vorgaben für alle 10 Abstractor-Funktionen der Abstractor-App. Es dient als technische und fachliche Richtlinie (Schnittstellen- und Design-Schnitt) zwischen der UI-Ergebnisdarstellung, dem Copy/Share-Text und der zukünftigen Prompt-Optimierung (Zielblock 6).

---

## 1. GEMEINSAME AUSGABE-BASIS

Jede Analyse-Funktion liefert Daten, die einheitlich verarbeitet und im UI sowie im Copy/Share-Format dargestellt werden.

### A. Kopfbereich Ergebnisseite
* **Titel:** Der extrahierte oder generierte Artikel- bzw. Quellentitel.
* **Unterzeile (Metadaten):** 
  * Wenn `summary.owner` geliefert wird, unauffällig als `von <owner>` anzeigen.
  * Wenn kein Owner geliefert wird (unbekannt/leer), unauffällig die `originalUrl` oder deren Domain anzeigen.
  * Stil: Kleinere Schrift, sekundär gedämpfte Textfarbe (`color = MaterialTheme.colorScheme.secondary`), oberhalb von Primär-Aktionen platziert.

### B. Begriffe & Wording
* Der Begriff **„Ergebnisse“** wird konsequent durch **„Inhalte“** ersetzt (sowohl im UI-Bereich der Takeaways als auch im generierten Copy-Text). 
* Ein allgemeiner Hinweis wie `Funktion: X` wird im Copy-Text entfernt, um den reinen Nutzwert des Inhalts hervorzuheben.

### C. Standard-Bulletpoints (Takeaway-Darstellung)
* **Standard:** Kein Nummerierungsmuster (kein `1., 2., 3.`). Stattdessen werden die Key-Takeaways im UI als eigenständige Info-Karten gerendert.
* **Semantische Struktur:** Das UI splittet jedes Takeaway mittels `parseTakeaway()` in einen fetten Titel (`**Titel**`) und eine Beschreibung auf.
* **Aufzählungszeichen:** Anstelle generischer Striche verwendet das UI Material-Design-3-Icons, die zum Charakter der jeweiligen Funktion passen.

---

## 2. FUNKTIONSSPEZIFISCHE ERGEBNISVORGABEN

Hier werden für jeden der 10 `AnalysisType`s die individuellen Ergebnis-Charaktere, Takeaway-Strukturen, Nummerierungsentscheidungen, Icons und künftige UI-Ausbauten definiert.

### 1. STANDARD_WEBSEITE (Standard-Webseite zusammenfassen)
* **Ergebnis-Charakter:** Allgemeine, strukturierte Inhaltszusammenfassung für typische Web-Artikel, Blogs und Berichte.
* **Takeaway-Struktur:** Wesentliche Teilaspekte der Webseite, aufgeteilt in logische Abschnitte (z. B. These, Hintergrund, Fazit).
* **Nummerierung:** **NEIN** (rein darstellend, hierarchisch flach).
* **Icon:** `Icons.Default.CheckCircle` (allgemeines Erledigt-/Inhaltssymbol).
* **Copy-Text-Besonderheit:** Standard-Gemeinschaftskontext. Keine Sondertexte.
* **Geplante UI-Besonderheit:** Einfache Themen-Tags.

---

### 2. MULTIMEDIA (Multimedia-Inhalt zusammenfassen)
* **Ergebnis-Charakter:** Fokus auf transkribierte Audio- und Videodaten (z. B. YouTube-Inhalte, Podcasts) und visuelle Metadaten.
* **Takeaway-Struktur:** Zeitliche Meilensteine, Kernaussagen des Redners, multimediale Verweise.
* **Nummerierung:** **NEIN** (fortlaufende inhaltliche Punkte).
* **Icon:** `Icons.Default.CheckCircle` (oder künftig ein Play-/Video-Symbol).
* **Copy-Text-Besonderheit:** Erwähnung von Sprechern/Kanälen im Owner-Bereich falls vorhanden.
* **Geplante UI-Besonderheit:** Timeline-ähnliche Anordnung der Bulletpoints.

---

### 3. DOKUMENTE (Dokumente zusammenfassen)
* **Ergebnis-Charakter:** Analyse hochgeladener PDF-, Word-, Excel- oder reiner Textdokumente.
* **Takeaway-Struktur:** Formelle Schlüsselthesen, Tabellenstrukturen, Referenzen oder formale Abschnitte.
* **Nummerierung:** **NEIN** (flache Struktur).
* **Icon:** `Icons.Default.CheckCircle` (oder künftig ein Dokument-Icon wie `Icons.Default.Description`).
* **Copy-Text-Besonderheit:** Anzeige des ursprünglichen Dateinamens im Owner/Titel-Bereich.
* **Geplante UI-Besonderheit:** Datei-Meta-Card (Dateigröße, Seitenanzahl).

---

### 4. TOP_3_KERNAUSSAGEN (Die Top 3 Kernaussagen ermitteln)
* **Ergebnis-Charakter:** Radikale Priorisierung auf exakt drei Kernpunkte zwecks zeitsparender Chef-Zusammenfassung.
* **Takeaway-Struktur:** Exakt drei prägnante, voneinander abgegrenzte Erkenntnisse.
* **Nummerierung:** **JA** (Zwingend `01`, `02`, `03` zur Visualisierung der Top-3-Relation).
* **Icon:** Entfällt temporär im UI durch große Nummerierungs-Badges.
* **Copy-Text-Besonderheit:** „Inhalte:“ listet nummerierte Einträge anstelle von Spiegelstrichen.
* **Geplante UI-Besonderheit:** Nummerierte Priority-Badges im Ribbon-Stil.

---

### 5. AKTUALITAETS_CHECK (Aktualität prüfen)
* **Ergebnis-Charakter:** Abgleich des Alters und der Relevanz der Informationen einer Website gegenüber aktuellem Zeitgeschehen (Zwang zur Grounding-Engine).
* **Takeaway-Struktur:** Zeitstempel-Analyse, Aktualitätsbewertung, Abweichungen zu neueren Erkenntnissen.
* **Nummerierung:** **NEIN**.
* **Icon:** `Icons.Default.Search` (Symbolisiert die Prüfung/Suche).
* **Copy-Text-Besonderheit:** Letztes Modifikationsdatum im Kopfbereich vermerken.
* **Geplante UI-Besonderheit:** Rote/Gelbe/Grüne Alters-Ampel.

---

### 6. FEHLINFORMATIONS_RADAR (Fehlinformations-Radar aktivieren)
* **Ergebnis-Charakter:** Kritisches Fact-Checking, Erkennung von Clickbait, Fake News, Halbwahrheiten oder verzerrten Darstellungen.
* **Takeaway-Struktur:** Behauptung vs. Belegte Faktenlage, Logische Fehlschlüsse, Vertrauenswürdigkeitsindex.
* **Nummerierung:** **NEIN**.
* **Icon:** `Icons.Default.Warning` (Signalisiert Skepsis und Warnung).
* **Copy-Text-Besonderheit:** Expliziter Vertrauenswürdigkeits-Score als Prozentwert oder Ampel-Text.
* **Geplante UI-Besonderheit:** Detektor-Warnungsbanner im Kopfbereich mit prozentualer Faktencheck-Wahrscheinlichkeit.

---

### 7. RISIKO_ANALYSE (Risiko-Analyse durchführen)
* **Ergebnis-Charakter:** Identifikation von rechtlichen, finanziellen, reputationsbezogenen oder operativen Gefahren einer beschriebenen Idee, News oder Vertragsseite.
* **Takeaway-Struktur:** Risiko, Eintrittswahrscheinlichkeit, Impact-Level und anwendbare Gegenmaßnahmen.
* **Nummerierung:** **JA** (Die Risiken werden nach Schweregrad sortiert aufgelistet).
* **Icon:** `Icons.Default.Warning` (bzw. Nummern-Badges im UI).
* **Copy-Text-Besonderheit:** Gliederung nach Risikostufen im Text.
* **Geplante UI-Besonderheit:** Farbcodierte Warning-Cards (Rot = Kritisches Risiko, Gelb = Mittleres Risiko, Blau = Geringes Risiko/Hinweis).

---

### 8. BUSINESS_INKUBATOR (Geschäftsideen-Inkubator starten)
* **Ergebnis-Charakter:** Kreativ-analytische Auswertung einer Technologie oder URL zur Generierung innovativer Geschäftsmodelle.
* **Takeaway-Struktur:** USP (Alleinstellungsmerkmal), Zielgruppe, Monetarisierungsstrategien und konkreter nächster Schritt.
* **Nummerierung:** **NEIN** (Kreative Ideenentwicklung).
* **Icon:** `Icons.Default.Lightbulb` (Repräsentiert die Ideenfindung und Innovation).
* **Copy-Text-Besonderheit:** Fokus auf monetäre Hebel und Wachstumspotenziale.
* **Geplante UI-Besonderheit:** Strukturierte Grid-Cards (z.B. One-Page Business Canvas Style).

---

### 9. FACTS_VS_OPINIONS_ANALYZER (Fakten vs. Meinungen-Analysator)
* **Ergebnis-Charakter:** Trennung von rein objektiven, verifizierbaren Fakten und subjektiven Einordnungen (Kolumnen, PR-Materialien).
* **Takeaway-Struktur:** Das JSON-Schema erzwingt die Kennzeichnung jedes Eintrags. Fakt-Einträge beginnen mit `[F]`, Meinungen mit `[M]`. Beide Bereiche werden getrennt gegenübergestellt.
* **Nummerierung:** **NEIN**.
* **Icon:** `Icons.Default.Verified` (Fakt-Verifizierung).
* **Copy-Text-Besonderheit:** Saubere Trennung der Liste im Clipboard unter „Fakten“ und „Meinungen“.
* **Geplante UI-Besonderheit:** Zukünftig zweigeteilte Tab-Spalten-Anordnung im UI (Tab 1: Fakten, Tab 2: Meinungen) zur visuellen Trennung.

---

### 10. PERSPECTIVES_AND_COUNTERPOSITIONS (Perspektiven & Gegenpositionen)
* **Ergebnis-Charakter:** Ausgewogene Darstellung verschiedener Interessengruppen und kontroverser Sichtweisen aus einem Dokument oder Artikel.
* **Takeaway-Struktur:** Gegenpositionen, Konsensbereiche, Dissensfelder, beteiligte Interessengruppen.
* **Nummerierung:** **NEIN** (ausgewogene Darstellung).
* **Icon:** `Icons.Default.CheckCircle` (Sinnvoll ergänzbar durch Dialektik-Icons wie Justitia/Balance).
* **Copy-Text-Besonderheit:** Symmetrische Aufzählung von Pro & Contra.
* **Geplante UI-Besonderheit:** Duales Pro-Contra-Splitter-Layout (Nebeneinander-Anordnung auf breiten Bildschirmen/Tablets).

---

## 3. ÜBERGREIFENDE SPEZIFISCHE LAYOUT- UND STRUKTURVORGABEN

### A. Formatierung der Aufzählungspunkte (Takeaways)
* **Ein-Ebenen-Design (Strikte Flachheit):** Alle Spiegelstriche/Inhalte dürfen nur *auf einer einzigen Ebene* dargestellt werden. Hierarchische Unterpunkte (verschachtelte Tab-Bullets) sind unzulässig, da sie das Layout stören und beim Text-Copying Artefakte erzeugen. Wenn Unterpunkte nötig sind, müssen sie direkt als Fließtext im selben Absatz integriert werden, optional unter Verwendung von Inline-Nummerierungen wie `(i)`, `(ii)` oder `(iii)`.
* **Keine generischen Label-Platzhalter:** Die Takeaway-Karten dürfen den Textfluss nicht mit redundanten, uninformativen Präfixen wie *„Erkenntnis:“*, *„Keyword:“*, *„Begründung:“*, *„Subjektivität:“* etc. beginnen. 
* **Fettgedruckte Schlagwort-Einleitung:** Jeder Punkt muss stattdessen mit einem konkreten, inhaltlich sprechenden und individuellen Begriff/Thema fettgedruckt eingeleitet werden, gefolgt von der Erläuterung.
  * *Soll-Format:* `**Konkretes Schlagwort:** Erläuternder, fließender Folgetext direkt danach.`
* **Keine führenden Hochkommas:** Alle Takeaways sind direkt ohne führende einfache oder doppelte Anführungszeichen/Hochkommas (z. B. `'` oder `"`) am Zeilenanfang auszugeben.

### B. Visualisierung „Ganz kurz“ (Visual Polish)
* **UI-Gestaltung:** Nur das obere Label „GANZ KURZ“ wird gefärbt (z. B. mit der Primärfarbe `MaterialTheme.colorScheme.primary` als visueller Akzent).
* **Text-Gestaltung:** Der eigentliche Fließtext (Beschreibung) innerhalb der Karte wird mit einem unaufdringlichen, tiefen neutralen Schwarzton/Dunkelgrau (`Color.Black` oder `onSurface`) gerendert.
* **Text-Größe:** Die Schriftgröße des Fließtexts wird gegenüber den restlichen Elementen leicht herabgesetzt (`13.sp` statt `14.sp`, Zeilenhöhe `20.sp`), um eine harmonische Lesbarkeits-Hierarchie zu etablieren.

---

## 4. GEMEINSAME NUMMERIERUNGSREGELN

Um Brüche im UI zu vermeiden, wird die Entscheidung über Nummerierungen zentral gesteuert:

| AnalysisType | Nummerierung im UI | Begründung |
| :--- | :--- | :--- |
| **TOP_3_KERNAUSSAGEN** | **JA** | Fachliche Exklusivität und Relevanz-Reihenfolge (01 bis 03). |
| **RISIKO_ANALYSE** | **JA** | Priorisierung der Risikostufen (Schwerste zuerst). |
| **Alle anderen 8 Typen** | **NEIN** | Gleichrangige, flache Bulletpoints sorgen für ein moderneres, luftigeres Layout. |

---

## 5. COPY/SHARE-PLAINTEXT-SCHEMASTRECKUNG

Der unter `buildPlainTextShareOrCopyText()` erzeugte Text folgt diesem festen Raster:

```text
Titel der Quelle: <Titel-Inhalt>
Owner: <Owner-Inhalt oder Domain-Ersatz>

URL:
<original_url>

Ganz kurz:
<short_description_text>

Inhalte:
- [Icon/Prefix falls zwingend] <Takeaway 1>
- [Icon/Prefix falls zwingend] <Takeaway 2>
- ...
```

*Vorteile:* Kein unschönes "Funktion:"-Metadatenrauschen, kein unübersetztes "Results:", direkte Verwertbarkeit im geschäftlichen Alltag und saubere Lesbarkeit durch doppelte Zeilenumbrüche zwischen Sektionen.

---

## 6. PROMPT-OPTIMIERUNGSPLAN & FUNKTIONSSPEZIFISCHE STEIGERUNG (Auszug Testnotizen)

Dieser Plan steuert gezielt die zukünftige fachliche Überarbeitung der `.md`-Prompts in Zielblock 6.

### 1. STANDARD_WEBSEITE (`F_STANDARD_WEBSEITE.md`)
* **Beobachtetes Problem:** Takeaways beginnen oft stereotyp und redundant mit *„Erkenntnis:“* oder einem einleitenden Hochkomma/Anführungszeichen.
* **Soll-Optimierung:** Der Prompt muss anweisen, dass jedes Takeaway mit einem aussagekräftigen Schlagwort beginnt: `**Sprechendes Schlagwort:** Inhaltlicher Satz.` Keine Anführungszeichen am Anfang.
* **Prompt-Änderung nötig:** **YES (in Zielblock 6)**

### 2. AKTUALITAETS_CHECK (`F_AKTUALITAETS_CHECK.md`)
* **Beobachtetes Problem:** Ausgabe wirft unnötige, starre technische Dimensionstitel (*„Dimension A: Inhaltliche Gültigkeit“*) aus, die die Lesbarkeit erschweren.
* **Soll-Optimierung:** Technische Kategorien verbergen. Stattdessen sind direkt inhaltliche, zeitbezogene Kernaussagen der Aktualität herauszuarbeiten.
  * *Soll-Format:* `**Veröffentlichungszeitpunkt & Veralterung:** Der Artikel datiert von...`
* **Prompt-Änderung nötig:** **YES (in Zielblock 6)**

### 3. FEHLINFORMATIONS_RADAR (`F_FEHLINFORMATIONS_RADAR.md`)
* **Beobachtetes Problem:** Generiert oft nutzlose, zu kurze Einzelbegriffe (z. B. *„keyword“*, *„Subjektivität“*, *„Begründung“*) oder leitet alles faul mit dem Wort *„Erkenntnis“* ein.
* **Soll-Optimierung:** Strikter Ausschluss von Einzelwort-Takeaways. Jede erkannte Fehlinformation oder manipulative Technik muss präzise betitelt und im Folgetext sachlich widerlegt werden.
  * *Soll-Format:* `**Manipulative Überschrift (Clickbait):** Der Text nutzt reißerische Adjektive im Titel...`
* **Prompt-Änderung nötig:** **YES (in Zielblock 6)**

### 4. BUSINESS_INKUBATOR (`F_BUSINESS_INKUBATOR.md`)
* **Beobachtetes Problem:** Liefert oft nur eine Standardzusammenfassung des Inhalts, statt kreativ-analytisch echte Geschäftsideen aus den beschriebenen Problemen zu extrahieren.
* **Soll-Optimierung:** Der Prompt muss den Fokus radikal von "Nacherzählung" auf "Ideen-Inkubation" verlagern. Erzwungene Ableitung von praxistauglichen Ideen (aus Marktlücken, Defiziten oder Potenzialen der Quelle).
  * *Soll-Format:* `**Abo-Dienst für Nischen-X:** Weil der Artikel zeigt, dass Zielgruppe Y keinen Zugang zu...`
* **Prompt-Änderung nötig:** **YES (in Zielblock 6)**

### 5. PERSPECTIVES_AND_COUNTERPOSITIONS (`F_PERSPECTIVES_AND_COUNTERPOSITIONS.md`)
* **Beobachtetes Problem:** Sammelt oft beliebige Meinungsfetzen statt eine dialektische, kritische Gegenüberstellung zu den Hauptthesen der Quelle vorzunehmen.
* **Soll-Optimierung:** Prompt so trimmen, dass er erst die zentrale These der Quelle erfasst und ihr dann eine handfeste, wissenschaftlich oder gesellschaftlich vertretene Gegenposition gezielt gegenüberstellt.
  * *Soll-Format:* `**These des unbegrenzten Wachstums (Quelle):** Der Autor plädiert für X, während kritische Ökonomen das Gegenmodell Y anführen...`
* **Prompt-Änderung nötig:** **YES (in Zielblock 6)**

---

## 7. PROMPT_OPTIMIZATION_IMPACT MATRIX

| AnalysisType | Betroffene Datei | Beobachtetes Fehlverhalten | Neue Output-Regel im Prompt | Spätere Prompt-Änderung nötig |
| :--- | :--- | :--- | :--- | :--- |
| **STANDARD_WEBSEITE** | `F_STANDARD_WEBSEITE.md` | Startet oft mit stereotypem "Erkenntnis / " oder Hochkommas. | Strikte Vorgabe für Schlagwort-Einleitung; Anführungszeichen am Anfang verboten. | **YES** |
| **AKTUALITAETS_CHECK** | `F_AKTUALITAETS_CHECK.md` | Nutzt starre technische Bezeichner ("Dimension A/B..."). | Entfernung metabezogener Dimensionen; Fokus auf echtzeitliche Inhaltsbewertung. | **YES** |
| **FEHLINFORMATIONS_RADAR** | `F_FEHLINFORMATIONS_RADAR.md` | Zu kurze oder bedeutungslose One-Word-Takeaways. | Mindestbeschreibung von 2 Sätzen; Einzelfehlinformation konkret im Titel benennen. | **YES** |
| **BUSINESS_INKUBATOR** | `F_BUSINESS_INKUBATOR.md` | Bloße repetitive Zusammenfassungen der Webseiten-Inhalte. | Zwingende Formulierung von innovativen, praxisnahen Geschäftsideen inkl. Begründung. | **YES** |
| **PERSPECTIVES_AND_COUNTERPOSITIONS** | `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | Beliebige Aufzählung von Akteuren ohne dialektischen Kontrast. | Gegenüberstellung: Kernthese der Quelle vs. etablierte Gegenmeinung im Weltwissen. | **YES** |

---

## 8. EMPFEHLUNG FÜR DEN NÄCHSTEN SCHRITT (ZIELBLOCK 6)

Wir empfehlen dringend, mit **`F_STANDARD_WEBSEITE.md`** und **`F_BUSINESS_INKUBATOR.md`** als erste Meilensteine in Zielblock 6 zu starten. Diese beiden Funktionen decken das breiteste Spektrum ab. Durch die Etablierung des Schlagwort-Musters bei der Standard-Webseite und die Erhöhung der kreativen Schöpfungstiefe beim Business-Inkubator erzielen wir die größte qualitative Hebelwirkung für den Nutzer. Kaskadierend können danach die restlichen Spezialdaten-Prompts optimiert werden. Und da die UI das Split-Parsing bereits unterstützt, greifen diese kosmetischen Prompt-Updates nahtlos ohne Code-Änderungen in `MainActivity.kt`.

