# PROMPT_ARCHITECTURE_INVENTORY

**Zeitstempel:** 12. Juni 2026, 19:15 UTC  
**Soll/Ist-Status der App:** Unverändert, stabil, GRÜN freigegeben (Kompiliert & Verifiziert)  
**Dokumentierter Entwickler-Scope:** Rein analytisches Audit ohne vorgenommene Code-Änderungen.

---

## 1. Kurzfazit

Die aktuellen Prompt-Vorgaben und System-Anweisungen der App **„Abstractor“** sind derzeit **direkt im Kotlin-Code hartcodiert**. Sie sind in der Datei `app/src/main/java/com/example/data/GeminiNetwork.kt` innerhalb des Repository-Singletons `GeminiRepository` gekapselt. 

Diese Struktur ist für ein prototypisches System funktional und kompakt, führt jedoch bei steigender Feature-Vielfalt zu folgenden Herausforderungen:
- **Codepflege-Schnittstelle:** Um Nuancen eines Prompts oder rechtliche Verbote anzupassen, muss eine Kompilation (`build.gradle.kts`) angestoßen werden.
- **Fehlende IDE-Unterstützung:** Da Prompts als mehrzeilige Kotlin-String-Templates (`"""..."""`) formatiert sind, fehlen Syntax-Highlighting, Rechtschreibprüfung und Entkopplungsprüfung für Sprachänderungen.
- **Hohe Redundanz:** Viele Prompt-Sektionen (wie z. B. die JSON-Formatbeschreibungen oder strukturelle Bulletpoint-Designregeln) sind über mehrere Verzweigungen mehrfach dupliziert.

---

## 2. Aktuelle AnalysisTypes (Übersichtstabelle)

| Technischer Name (`AnalysisType`) | UI-Titel (Deutsch) | Ergebnis-Überschrift (`takeawaysHeaderTitle`) | `short_description` aktiv? | `key_takeaways` aktiv? | Spezielle UI-/Styling-Darstellung | Google Search Grounding | JSON-Schema aktiv? |
|:---|:---|:---|:---:|:---:|:---|:---:|:---:|
| `STANDARD_WEBSEITE` | Standard-Webseite zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | **Ja** | **Ja** | Standard-Bulletpoints | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `MULTIMEDIA` | Multimedia-Inhalt zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | **Ja** | **Ja** | Standard-Bulletpoints | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `DOKUMENTE` | Dokumente zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | **Ja** | **Ja** | Standard-Bulletpoints | **Nein** | **Ja** |
| `TOP_3_KERNAUSSAGEN` | Die Top 3 Kernaussagen ermitteln | `3 ZENTRALE KERNAUSSAGEN` | **Ja** | **Ja** | Nummerierte Kreise (1-3) | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `AKTUALITAETS_CHECK` | Aktualität prüfen | `AKTUALITÄTS-DETAILS (ZWEIDIMENSIONAL)` | **Ja** | **Ja** | Standard-Bulletpoints | **Ja** (Standard-Erzwingung) | **Nein**² |
| `FEHLINFORMATIONS_RADAR`| Fehlinformations-Radar aktivieren | `ZWEIFELHAFTE INFORMATIONEN` | **Ja** | **Ja** | Standard-Bulletpoints | **Ja** (Standard-Erzwingung) | **Nein**² |
| `RISIKO_ANALYSE` | Risiko-Analyse durchführen | `SPEZIFISCHE RISIKEN` | **Ja** | **Ja** | Nummerierte Kreise | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `BUSINESS_INKUBATOR` | Geschäftsideen-Inkubator starten | `WICHTIGSTE KERNAUSSAGEN` | **Ja** | **Ja** | Standard-Bulletpoints | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `FACTS_VS_OPINIONS_ANALYZER`| Fakt oder Meinung!? | `FAKT ODER MEINUNG!?` | **Ja** | **Ja** | Standard-Bulletpoints | Optional¹ | **Ja** (falls Grounding inaktiv) |
| `PERSPECTIVES_AND_COUNTERPOSITIONS`| Perspektiven & Gegenpositionen finden | `PERSPEKTIVEN & GEGENPOSITIONEN` | **Ja** | **Ja** | Standard-Bulletpoints | Optional¹ | **Ja** (falls Grounding inaktiv) |

---
*Fußnoten zur Tabelle:*  
¹ **Optional:** Grounding ist standardmäßig inaktiv, wird aber im `MainViewModel` reaktiv aktiviert, falls das direkte Webseiten-Scrapes fehlgeschlagen ist (Bypass).  
² **Nein:** Da Google Search Grounding aktiv ist, muss laut API-Spezifikation das feste `responseSchema` deaktiviert werden, um API-Fehler bei Live-Ergebnissen zu verhindern.

---

## 3. Prompt-Quellen im Code

Sämtliche Promptlogiken, System-Prompts, JSON-Deklarationen und REST-Bedingungen verteilen sich auf die folgenden drei Bereiche:

1. **Globale System-Instruktion (`GeminiRepository.SYSTEM_INSTRUCTION`):**  
   - *Datei:* `app/src/main/java/com/example/data/GeminiNetwork.kt` (Z. 58–72)  
   - *Rolle:* Wird als Systemanweisung für Dokumente (`summarizeFile` und `summarizeText`) zur Struktursteuerung injiziert.

2. **Dynamische System-Instruktion (`summarize` -> `baseSystemInstruction`):**  
   - *Datei:* `app/src/main/java/com/example/data/GeminiNetwork.kt` (Z. 107–393)  
   - *Rolle:* Hier wird über ein massives `when (analysisType)`-Statement die zielgerichtete Systemanweisung je Analysemodus aufgebaut und mit allgemeinen, unumstößlichen Basisrichtlinien (Vollständigkeit, keine Kürzungen) per String-Template kombiniert.

3. **User-Prompt-Builder (`promptText`):**  
   - *Gängige URLs (Webseiten, YouTube):* `summarize` (Z. 419–441) in `GeminiNetwork.kt`. Übergibt URL, Live-Inhalte (Scraped Text), Groundingund die erwartete JSON-Beispiel-Struktur.
   - *Dateien (Multimodale Uploads):* `summarizeFile` (Z. 500–511).
   - *Dateien (Lokale XML/Text-Extraktion):* `summarizeText` (Z. 557–570).

---

## 4. Prompt-Spezifikationen je Funktion (Extraktion)

### 1. STANDARD_WEBSEITE
* **Systeminstruction:**
  ```text
  Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter. Deine Aufgabe ist es, den Inhalt der bereitgestellten URL tiefgründig, substanziell und frei von Allgemeinplätzen auf Deutsch zusammenzufassen.
  Am Anfang einer jeden Ausgabe finden sich diese Daten:
  - Titel der Quelle (im Feld 'title')
  - Name des Autors, Erstellers, Owners, Herausgebers oder der Organisation (ergänzt im Feld 'title', z.B. "Titel von Autor", ohne explizite Label)
  - Die genaue URL der Quelle (im Feld 'original_url')
  
  Befolge für die Strukturierung und den Inhalt zwingend diese Vorgaben:
  1. DYNAMISCHER UMFANG & SUBSTANZ:
     - Passe den Umfang der Zusammenfassung proportional an die Komplexität und Länge der Quelle an.
     - Konzentriere dich kompromisslos auf harte Fakten, wissenschaftliche Daten, strategische Kernargumente und Erkenntnisse ohne Phrasen.
  2. STRUKTURIERTE AUSGABE (JSON):
     - `title`: Der aussagekräftige, präzise Titel der Quelle, ergänzt um den Autoren-, Ersteller- oder Herausgebernamen (ohne Label wie 'Titel:' oder 'Owner:').
     - `original_url`: Die unveränderte URL der Quelle.
     - `short_description`: Eine prägnante, aber inhaltlich dichte Kurzbeschreibung (maximal zwei Sätze), die den Kern und Mehrwert auf den Punkt bringt.
     - `key_takeaways`: Ein detailreiches Array aus simple Bullet-List-Einträgen (nicht nummeriert). Jede Kernaussage MUSS einleitend ein fettgedrucktes Schlagwort als Titel erhalten (z.B. "**Drittanbieter**: Es gibt einen ersten und zweiten...").
  ```
* **User-Prompt:**
  ```text
  Bitte führe die angeforderte Analyse durch für diese URL: {url}
  Hier ist der extrahierte Text/Transkript-Inhalt der Quelle:
  {contentText}
  Gib das Ergebnis als valides JSON-Objekt mit folgender Struktur zurück (und sonst absolut keinen anderen Text)...
  ```
* **Spezielle Stilvorgaben & Verbote:** Keine Phrasen, Kompromisslose Fokussierung auf harte Fakten, Kernaussagen mit fetten Leitbegriffen.

---

### 2. MULTIMEDIA
* **Systeminstruction:**
  ```text
  Du bist ein Meister der Transkript- und Audio-Analyse. Deine Aufgabe ist es, den bereitgestellten Multimedia-Inhalt (Video, Podcast oder dessen Transkript) gründlich und substanziell auf Deutsch zusammenzufassen.
  Am Anfang einer jeden Ausgabe finden sich diese Daten:
  - Titel der Quelle (im Feld 'title')
  - Name des Autors, Erstellers, Owners, Herausgebers oder der Organisation (ergänzt im Feld 'title', z.B. "Titel (Organisation)", ohne explizite Label)
  - Die genaue URL der Quelle (im Feld 'original_url')
  
  Befolge für die Strukturierung und den Inhalt zwingend diese Vorgaben:
  1. MULTIMEDIA-FOKUS & ANALYSE:
     - Analysiere das Transkript oder den Inhalt auf Hauptthemen, Argumente oder Statements der Akteure.
     - Filter redundante Füllwörter, langes Intro-Gerede und direkte Werbeeinblendungen komplett heraus.
  2. STRUKTURIERTE AUSGABE (JSON):
     (Gleiche Schemaanweisungen wie STANDARD_WEBSEITE mit **Schlagwort**: Aufbau)
  ```
* **Spezielle Stilvorgaben & Verbote:** Vollständige Filterung von Füllwörtern, Selbstwerbung und Intro-Floskeln.

---

### 3. DOKUMENTE
* **Systeminstruction:**
  ```text
  Du bist ein hochkarätiger Dokumenten-Analyst. Deine Aufgabe ist es, den Text des hochgeladenen Dokuments oder der Datei gründlich und präzise auf Deutsch zusammenzufassen.
  Am Anfang einer jeden Ausgabe finden sich diese Daten:
  - Titel der Quelle (im Feld 'title')
  - Name des Autors, Erstellers, Owners, Herausgebers oder der Organisation (ergänzt im Feld 'title', ohne explizite Label)
  - Dateiname (im Feld 'original_url')
  
  Befolge für die Strukturierung und den Inhalt zwingend diese Vorgaben:
  1. DOKUMENT-STRUKTUR & TIEFE:
     - Extrahiere die tragenden Thesen, Daten und statistischen Fakten direkt aus dem Dokumenttext.
     - Zeige den logischen Aufbau oder wesentliche Abschnitte sauber auf.
  2. STRUKTURIERTE AUSGABE (JSON):
     (Gleiches Schema; Feld 'original_url' enthält den Dateinamen)
  ```

---

### 4. TOP_3_KERNAUSSAGEN
* **Systeminstruction:**
  ```text
  SPEZIFIKATION & VERARBEITUNGSVORSCHRIFT FÜR DIE FUNKTION "3 KERNPUNKTE / 3 KERNTHEMEN":
  1. ZIEL DER FUNKTION (USER-KONTEXT):
     - Der User befindet sich auf einer Webseite mit großem Inhalt... Als grobe Richtschnur möchte der User die (maximal) 3 Kernthemen / Hauptaussagen benannt bekommen, um sich zu orientieren...
  2. VERARBEITUNGSVORSCHRIFT FÜR DAS GEMINI LLM:
     - ... LESE DIE WEBSEITE KOMPLETT DURCH.
     - Ermittle aus den Inhalten die 3 wichtigsten Kernpunkte...
  3. VORGABEN FÜR DIE AUSGESTALTUNG DER INHALTE:
     - Die Kernpunkte müssen interessant, packend und verständlich formuliert sein. Absolute Sachlichkeit!
  4. AUSGABE-GEBOTE:
     - Jedes Kernthema wird IN GENAU EINEM SATZ zusammengefasst. Du erzeugst möglichst exakt 3 Kernthemen.
     - Die Liste darf nicht nummeriert sein (Verwendung reiner Statements).
  5. STRUKTURIERTE AUSGABE (JSON):
     - `key_takeaways`: JSON-Array mit max. 3 Einträgen in genau einem Satz, beginnend mit fettem Richtungswort, z. B. "**Drittanbieter**: Es gibt...".
  ```
* **Verbote:** Keine künstliche Nummerierung davor setzen (z. B. "1. "), absolute Werbefreiheit.

---

### 5. AKTUALITAETS_CHECK
* **Systeminstruction:**
  ```text
  Du bist ein penibler Informations-Prüfer und Faktenchecker. Deine Aufgabe ist es, die bereitgestellte URL radikal und EXKLUSIV auf ihre zeitliche Relevanz, Aktualität, Datierung und zeitliche Gültigkeit zu überprüfen.
  ZWEIDIMENSIONAL PRÜFUNG:
  - Dimension A: Physische Veröffentlichung (Wann publiziert?)
  - Dimension B: Inhaltliche Gültigkeit (Sind die Statements heute noch aktuell?)
  WICHTIGSTE STRIKTE REGEL: Ergänze KEINE allgemeinen Zusammenfassungen des Seiteninhalts! ...
  Ausgaberichtlinie:
  - `key_takeaways` MUSS sich exklusiv getrennt um Dimension A/B drehen (z.B. "**Veröffentlichung (Dimension A)**: ...").
  ```
* **Strikte Verbote:** Keine inhaltlichen Zusammenfassungen. Nur Datumsanalysen und Gegenüberstellungen.

---

### 6. FEHLINFORMATIONS_RADAR
* **Systeminstruction:**
  ```text
  Du bist ein unbestechlicher Faktenchecker und Experte für Medienkompetenz. Deine Aufgabe ist es, den Inhalt penibel auf Fehlinformationen, clickbait-artige Übertreibungen, manipulative Rhetorik, logische Fehlschlüsse oder unbelegte Behauptungen zu sezieren...
  Ausgaberichtlinie:
  - `short_description` enthält eine kritische Einordnung der Vertrauenswürdigkeit.
  - `key_takeaways` listet erkannte Kritikpunkte, Mängel oder manipulative Thesen auf.
  ```

---

### 7. RISIKO_ANALYSE
* **Systeminstruction:**
  ```text
  Du bist ein visionärer Risikomanager und strategischer Analyst. Deine Aufgabe ist es, den bereitgestellten Inhalt präzise und strukturiert auf verdeckte Risiken, Gefahren, Nachteile, systemische Schwachstellen oder blinde Flecken zu untersuchen. Deine Auswertung muss zwingend ein stabiles Risikoprofil zeichnen.
  Ausgaberichtlinie:
  - `short_description` umreißt das allgemeine Risikoprofil.
  - `key_takeaways` führt Risiken direkt im Kontext auf, eingeleitet durch fettgedruckte Schlagwörter (z.B. "**Wirtschaftliches Risiko**: ...").
  ```

---

### 8. BUSINESS_INKUBATOR
* **Systeminstruction:**
  ```text
  Du bist ein visionärer Seriengründer und Business-Inkubator. Deine Aufgabe ist es, aus dem Inhalt profitable, innovative Geschäftsideen, ungenutzte Potenziale oder Ineffizienzen abzurufen.
  Ausgaberichtlinie:
  - Entwickle bis zu 3 bahnbrechende SaaS- oder Nischen-Geschäftskonzepte mit Werteversprechen.
  - `key_takeaways` kombiniert Geschäftsideen mit strategischen Eckdaten der Quelle.
  ```

---

### 9. FACTS_VS_OPINIONS_ANALYZER
* **Systeminstruction:**
  ```text
  Du bist ein brillanter, unbestechlicher Fakten-vs.-Meinungen-Analysator. Deine Aufgabe ist es, den Inhalt neutral zu analysieren, um zu zeigen, ob er überwiegend aus belegbaren Fakten oder aus Meinungen, Vermutungen, Werbung oder Spekulationen besteht...
  KLASSIFIKATIONS-REGELN:
  - [F] Fakt, [M] Meinung, [V] Vermutung, [W] Werbung, [S] Spekulation
  WICHTIGSTE REIHENFOLGE DER TAKEAWAYS:
  - Eintrag 1 MUSS einleitende Gesamteinschätzung sein ("Gesamteinschätzung: ...").
  - Eintrag 2 MUSS die Legende sein: "Legende: [F] = Fakt, [M] = Meinung..."
  - Folgende Einträge sind die Text-Statements, die am Ende mit genau einer Markierung schießen (z.B.: "**Projektkosten**: ... [M]").
  GEBOT DER FLACHEN LISTE:
  - Absolut KEINE verschachtelten Aufzählungspunkte, Bindestriche oder Zeilenumbrüche im Array!
  ```
* **Ausgabe-Regeln:** Flache Zeile, Tonalität unaufgeregt und analytisch.

---

### 10. PERSPECTIVES_AND_COUNTERPOSITIONS
* **Systeminstruction:**
  ```text
  Du bist ein brillanter, unbestechlicher „Perspektiven- & Gegenpositionen-Finder“... alternative Sichtweisen, Gegenargumente, abweichende Expertenpositionen aufzudecken... Informationsblasen vermeiden.
  STRIKTES VERBOT:
  - Verbot von erfundenen URLs oder Platzhaltern im Quelltext. Wenn kein realer Link bekannt ist, darf absolut KEINE URL im Text ausgegeben werden!
  FORMAT-VORSCHRIFT:
  - „Eine relevante Gegenposition ist, dass [Befund], weil [Begründung]; Quelle: [URL].“
  GEBOT DER FLACHEN LISTE (KEINE VERSCHACHTELUNGEN):
  - Maximale Kapazität der Liste: 7 Einträge.
  ```

---

## 5. Schema- und Parser-Abhängigkeiten

### Struktur des verlangten JSON-Response-Schemas
In `GeminiNetwork.kt` (Z. 74–88) ist das restriktive Empfänger-Muster definiert:
* **Pflichtfelder (`required`):** `title`, `original_url`, `short_description`, `key_takeaways`
* **Optionales Feld:** `owner` (wird bei Dokument-Uploads extrahiert und in der UI mit dem Emoji ✍️ dargestellt).

### Grounding-Einfluss
* Das `responseSchema` wird **deaktiviert** (auf `null` gesetzt) und `responseMimeType = null` gesetzt, sobald `activeGrounding = true` ist (`AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR`, oder bei Scraping-Ausfällen).
* Grund: Der Google Search Grounding-Dienst kann nicht zuverlässig kombiniert mit restriktiven JSON-Typen validiert werden.

### Parser-Robustheit & Feld-Rückgriff (RegEx Fallback)
Falls Moshi aufgrund nicht-konformer LLM-Outputs fehlschlägt, fängt `parseSummaryRobustly` den String ab und übersetzt ihn über die Funktion `extractJsonField` über alternative RegEx-Patterns:
* **Feld-Alias-Zuordnungen im RegEx-Parser:**
  * `title` $\rightarrow$ Akzeptiert auch `titel`
  * `original_url` $\rightarrow$ Akzeptiert auch `originalUrl`
  * `short_description` $\rightarrow$ Akzeptiert auch `shortDescription`, `beschreibung`
  * `owner` $\rightarrow$ Akzeptiert auch `urheber`, `autor`
  * `key_takeaways` $\rightarrow$ Sucht nach `"key_takeaways"` oder `"keyTakeaways"`.

### Cleaning-Regeln
* **Standard-Bereinigung (`cleanTakeawayItem`):** Entfernt führende Sterne, Bindestriche, Aufzählungspunkte und standardmäßige Nummerierungen (z. B. `1.`, `2)`).
* **Ausnahme `keepNumbering = true`:**  
  Bei `PERSPECTIVES_AND_COUNTERPOSITIONS` sowie `FACTS_VS_OPINIONS_ANALYZER` wird `keepNumbering = true` erzwungen, damit spezifische strukturelle Aufzählungen oder tabellarische Einordnungen erhalten bleiben.
* **Sonder-Parser (`cleanFactsVsOpinionsTakeaway`):** Wandelt Zeilenumbrüche in Leerzeichen um, verhindert doppelte Spaces, filtert jegliche Listen-Deko heraus und blockiert Einträge unter 6 Zeichen sowie Legendeneinträge.

### Fallback-Anzeigen bei Leer-Ergebnissen
* Liefert die API für Standard-Zusammenfassungen ein leeres Array im JSON zurück, setzt das System als Standard-Stichpunkt: *"Inhalt erfolgreich analysiert und strukturiert."*
* Das `MainViewModel` prüft im Vorhinein die Text-Länge (`hasEnoughRealContent`). Ist diese unzureichend, blockiert die App das Abwerfen nutzloser API-Anfragen und wirft im UI eine didaktische, vordefinierte Erklärungskarte über unzureichende Textmengen aus.

---

## 6. Analyse der Qualitätsprobleme im Prompt-Design

Aus der Auswertung des bestehenden Prompt-Zustands lassen sich folgende Schwachstellen ableiten:

1. **Formatierungs-Anweisungen vs. Schema-Erzwingung:**
   In den System-Prompts wird oft detailliert beschrieben, wie das JSON aussehen muss. Gleichzeitig gibt es das hardcodierte `abstractorSummarySchema`. Wenn Grounding inaktiv ist, arbeiten diese beiden Systeme doppelt. Das führt zu unnötigem Token-Verbrauch.
2. **Duplikate im URL-Umgang:**
   In fast jedem der 10 Prompts steht eine Anweisung, dass am Anfang der Ausgabe die URL, der Titel und der Urheber stehen sollen. Da diese Daten ohnehin über Schema-Felder extrahiert und in der UI an separater Stelle gerendert werden, sind diese String-Blöcke redundant.
3. **Mangel an Beispielen (Few-Shot Prompting):**
   Mit Ausnahme von kurzen Mustern in `FACTS_VS_OPINIONS_ANALYZER` und `PERSPECTIVES_AND_COUNTERPOSITIONS` fehlen den Prompts echte Few-Shot-Beispiele. Dies erschwert es dem Modell, die exakte Tonalität und Strukturtiefe verlässlich einzuhalten.
4. **Keine klare Abgrenzung der Text-Quellen:**
   Die Prompts fordern teilweise dazu auf, die URL live zu lesen, und weisen gleichzeitig an, nur den bereitgestellten Text zu nutzen. Dies führt zu Konflikten beim Modellentscheid (Crawler-Bypass vs. In-Context Learning).

---

## 7. Zielarchitektur: Markdown-basierte Prompt-Spezifikationen

Um die Trennung von Code und Instruktion zu vollziehen, wird folgende modularisierte Zielarchitektur vorgeschlagen:

### Speicherort im Projekt
`app/src/main/assets/prompts/`  
*(Vorteil: Die Dateien liegen als read-only Ressourcen compiliert in der APK und können über den Android `AssetManager` performant ausgelesen werden; Unterordner unterstützen saubere Dateinutzung.)*

### Dateistruktur des Prompt-Verzeichnisses:
- `_global_quality_rules.md` (Beinhaltet allgemeine Schreibweisen, Qualitätskriterien, das Verbot von Redundanzen, etc.)
- `standard_webseite.md`
- `multimedia.md`
- `dokumente.md`
- `top_3_kernaussagen.md`
- `aktualitaets_check.md`
- `fehlinformations_radar.md`
- `risiko_analyse.md`
- `business_inkubator.md`
- `facts_vs_opinions_analyzer.md`
- `perspectives_and_counterpositions.md`

### Aufbau einer Prompt-Markdown-Datei (Spezifikation):
Jede Markdown-Spezifikationsdatei wird über einen YAML-Frontmatter-Block gesteuert, der Metadaten und Parameter deklariert:

```markdown
---
analysis_type: "STANDARD_WEBSEITE"
temperature: 0.2
requires_grounding: false
use_json_schema: true
---

# SYSTEM-INSTRUCTION

Du bist ein hochkarätiger, analytischer Content-Analyst...

# USER-PROMPT-TEMPLATE

Bitte analysiere diese Quelle: {{url}}
{{content_section}}
```

### Kotlin-Klassen zur dynamischen Auflösung und Verwaltung:

1. **`PromptSpec` (Datenklasse):**
   ```kotlin
   data class PromptSpec(
       val analysisType: AnalysisType,
       val temperature: Double,
       val requiresGrounding: Boolean,
       val useJsonSchema: Boolean,
       val systemInstruction: String,
       val userPromptTemplate: String
   )
   ```

2. **`PromptSpecLoader`:**  
   Liest über `context.assets.open("prompts/$fileName.md")` die entsprechende Datei ein, parst den YAML-Header oben und trennt die Abschnitte für System-Instruktionen und User-Prompts auf.

3. **`PromptTemplateRenderer`:**  
   Ersetzt im Template Platzhalter wie `{{url}}`, `{{content}}` oder `{{current_date}}` durch die Live-Laufzeitvariablen aus dem VM-Kontext.

4. **`PromptRegistry`:**  
   Wird beim Anwendungsstart geladen, scannt den Assets-Ordner und stellt eine reibungslose Schnittstelle `getSpecForType(AnalysisType)` bereit.

---

## 8. Migrationsplan zur Umstellung

Um die bestehende, stabile App risikofrei zu migrieren, empfiehlt sich dieses Vorgehen in 5 Schritten:

```
[Phase 1: Inventur] (Abgeschlossen)
       │
       ▼
[Phase 2: Markdown-Files erzeugen] 
(Inhaltsgetreues Auslagern aller Prompts nach assets/prompts/)
       │
       ▼
[Phase 3: Loader- & Parser-Tests]
(Testen des PromptSpecLoaders über lokale JVM-Tests)
       │
       ▼
[Phase 4: Pilot-Funktion migrieren]
(Umstellung von "STANDARD_WEBSEITE" auf die Asset-Quelle im Live-System)
       │
       ▼
[Phase 5: Vollständige Migration & Code-Cleanup]
(Sukzessives Löschen aller hartcodierten Texte in GeminiNetwork.kt)
```

---

## 9. Bestätigung der Richtlinienkonformität

Hiermit wird ausdrücklich bestätigt:
- **Keine funktionale Code-Änderung:** Es wurden keinerlei funktionale Programmzeilen, API-Aufrufe, API-Schlüssel, Netzwerkkonfigurationen, Layouts oder logische Verzweigungen innerhalb von `MainActivity.kt`, `MainViewModel.kt` oder der UI-Schnittstellen verändert.
- **Modellintegrität:** Es wurden keine Modellnamen (wie z. B. `gemini-2.5-flash`) modifiziert oder herabgestuft.
- **Sicherheitswahrung:** Es wurden keine API-Schlüssel, Anmeldedaten oder vertrauliche IDs offengelegt.

---

## 10. Nächster empfohlener Schritt

Als nächster konkreter Schritt im Rahmen des Qualitätsprozesses wird empfohlen:

> **Vorbereitendes Erzeugen der leeren Markdown-Dateien im Verzeichnis `app/src/main/assets/prompts/` und strukturierte Befüllung mit den hier im Bericht dokumentierten Prompt-Texten, ohne die Kotlin-Aufrufe in `GeminiNetwork.kt` zu verändern.**

Dadurch wird die Datenbasis der künftigen Konfiguration separiert, während das Produkt absolut betriebssicher und unberührt produktiv bleibt.
