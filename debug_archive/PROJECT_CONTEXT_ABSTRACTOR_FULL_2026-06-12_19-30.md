# PROJECT CONTEXT: ABSTRACTOR (FULL SYSTEM BLUEPRINT)
**Dokumenten-Version:** 2.0 (System-Freeze State)  
**Sicherungsdatum:** 2026-06-12 (Snapshot-Capture 19:30)  
**System-Status:** STABLE (Produktiv lauffähig, ready für Prompt-Externalisierung)

---

## 1. Projektbeschreibung (HIGH LEVEL)

### Was ist Abstractor?
**Abstractor** ist eine hochgradig spezialisierte Android-Sicherheits- und Informations-Analyse-App, die für anspruchsvolle Wissensarbeiter, Analysten und Entscheider entwickelt wurde. Anstatt klassische, inhaltsleere Textzusammenfassungen zu generieren, seziert die App digitale Inhalte (Webseiten, YouTube-Videos/Transkripte, hochgeladene PDFs, Word-Dokumente und Bilder) mit chirurgischer Präzision auf Substanz, Glaubwürdigkeit und strategischen Nutzen.

### Ziel der App
Die App löst das Problem des Information Overload und der bewussten medialen Verzerrung. Sie filtert Marketingphrasen, irrelevantes Füllmaterial und werbliche Übertreibungen rigoros aus digitalen Quellen heraus und verdichtet die verbleibenden Informationen in strukturierte, sachlich tiefe Analysen.

### Hauptnutzen für den User
* **Sofortige Orientierung:** Schnelle, intellektuell anspruchsvolle Erfassung komplexer Quellentexte oder mehrstündiger Medien.
* **Erkennung blinder Flecken:** Aufdeckung von verborgenen Risiken, logischen Fehlschlüssen, Fehlinformationen und einseitiger Berichterstattung.
* **Unternehmerischer Hebel:** Extraktion von konkreten, marktgängigen Geschäftsideen und strategischen Handlungspfaden direkt aus rohen Datenquellen.
* **Executive-ready Briefings:** Ausgabe von glasklaren Bulletpoint-Listen mit fettgedruckten Leitbegriffen zur sofortigen Weiterleitung oder Archivierung.

---

## 2. Funktionale Architektur (Analysefunktionen)

Das System verfügt über **10 eigenständige Analysemodi** (`AnalysisType`), die über das interaktive Oberflächen-Cockpit gesteuert werden. 

### Übersicht der Analysefunktionen

| Analysefunktion | System-Enum (`AnalysisType`) | Hauptzweck | Core-Vorgaben & Format | Grounding (Google Search) |
| :--- | :--- | :--- | :--- | :--- |
| **Standard Webseite** | `STANDARD_WEBSEITE` | Substantielle, ungeschönte Zusammenfassung von Webinhalten ohne Phrasen. | Bulletpoints mit fetten Leitbegriffen (z.B. **[Thema]**: Detail). | Conditional (von User wählbar) |
| **Multimedia** | `MULTIMEDIA` | Video-/Audio-Transkriptionen (z.B. YouTube) analysieren. Filtert Füllwörter und Sponsoren-Werbeblöcke heraus. | Bereinigte Kernaussagen mit fetten Leitbegriffen. | Conditional (von User wählbar) |
| **Dokumente** | `DOKUMENTE` | Detailgetreue Analyse von lokal hochgeladenen Dokumenten (PDFs, TXT, Bilder). | Erschließung tragender Thesen, Tabellendaten und statistischer Fakten. | Deaktiviert |
| **3 Kernpunkte** | `TOP_3_KERNAUSSAGEN` | Schnelles Screening einer Quelle zur Relevanzprüfung vor dem Lesen. | Maximal 3 Bulletpoints, jeder Bulletpoint besteht aus **genau einem Satz**. Keine Ziffern. | Conditional (von User wählbar) |
| **Aktualitätsprüfung** | `AKTUALITAETS_CHECK`| Getrennte Prüfung der Gültigkeit: Wann veröffentlicht? (A) und ist Inhalt fachlich überholt? (B). | Streng fokussiert auf Zeitangaben. Takeaways müssen mit **Veröffentlichung (Dimension A)** und **Inhaltliche Gültigkeit (Dimension B)** beginnen. | **Strikte Pflicht (Immer True)** |
| **Fehlinformationsradar** | `FEHLINFORMATIONS_RADAR`| Erkennung zweifelhafter Informationen, Clickbait, manipulativer Rhetorik und unbelegter Statements. | Analysiert Konsistenz und Quellenangaben. Bullet-Highlights für logische Fehlschlüsse. | **Strikte Pflicht (Immer True)**|
| **Risikoanalyse** | `RISIKO_ANALYSE` | Identifikation verdeckter Gefahren, Schwachstellen, rechtlicher/finanzieller Hürden. | Stark fokussierter systemischer Risiko-Katalog mit fetten Kategorien. | Conditional (von User wählbar) |
| **Business Inkubator** | `BUSINESS_INKUBATOR` | Generierung von bis zu 3 innovativen, tragfähigen Geschäftsmodellen (SaaS/Nische) basierend auf dem Inhalt. | Extrahiert Marktpotenziale, Ineffizienzen und Mehrwertversprechen. | Conditional (von User wählbar) |
| **Fakt vs. Meinung** | `FACTS_VS_OPINIONS_ANALYZER`| Neutrale Klassifizierung von Aussagen in Fakten `[F]`, Meinungen `[M]`, Vermutungen `[V]`, Werbung `[W]` und Spekulationen `[S]`. | Erste Zeile: Gesamt-Einschätzung. Zweite Zeile: Legende. Folgende Zeilen: Kategorisierte Aussagen mit exklusivem Suffix-Tag. Keine geschachtelten Strukturen. | Conditional (von User wählbar) |
| **Perspektivenanalyse** | `PERSPECTIVES_AND_COUNTERPOSITIONS`| Aufbrechen von Informationsblasen (Confirmation Bias) durch das Hinzufügen von validen Gegenpositionen. | Maximal 7 begründete, sachliche alternative Thesen. Reale Quell-URLs werden unzensiert beibehalten, erfundene URLs sind scharf verboten. | Conditional (von User wählbar) |

---

## 3. Technische Architektur (IST-ZUSTAND)

### Framework-Stack
* **Plattform:** Native Android Applikation (Kotlin, minSdk 26, targetSdk 34)
* **UI-Framework:** Jetpack Compose mit Material Design 3 (M3)
* **Netzwerkschicht:** Retrofit 2 & OkHttpClient (Verbindungs- und Lese-Timeouts robust auf 120 Sekunden konfiguriert)
* **JSON-SerDe:** Moshi mit Kotlin-Reflect-Adapter für strikt typisierte JSON-Bindings

---

### Zentrale API-Schicht: `GeminiNetwork.kt`
Der gesamte API-Zustand, das Payload-Assembly und die System-Ausfall-Brücken sind im Kotlin-Singleton `GeminiRepository` beheimatet.

#### Modell-Kacheln (`GeminiModels.kt / GeminiModelConfig`)
* **Primary Model:** `gemini-2.5-flash` (Die primäre Wahl für extrem schnelle, kosteneffiziente und schema-treue JSON-Generierung).
* **Fallback Model:** `gemini-3.5-flash` (Sprung-Ersatz bei HTTP-Fehlern, Serverüberlastung oder Ratenlimitierung/429 auf dem primären Modell).
* *Hinweis zur Modellgeschichte:* `gemini-1.5-flash` läuft auf dem Google API-Server auf einen Fehler (HTTP 404 NOT_FOUND), da das Modell veraltet ist, weshalb `gemini-2.5-flash` als stabiler Anker fungiert.

```kotlin
object GeminiModelConfig {
    const val TEXT_MODEL = "gemini-2.5-flash"
    const val FALLBACK_MODEL = "gemini-3.5-flash"
}
```

---

### Temperatur-Steuerung pro Funktion
Das LLM besitzt modulare Kreativitäts- und Präzisionsgrenzen:
* **Strikter logischer Fokus (0.1):** `FEHLINFORMATIONS_RADAR`, `FACTS_VS_OPINIONS_ANALYZER`
* **Analytisches Standardmaß (0.2 - 0.3):** `STANDARD_WEBSEITE`, `MULTIMEDIA`, `DOKUMENTE`, `TOP_3_KERNAUSSAGEN`, `PERSPECTIVES_AND_COUNTERPOSITIONS`, `AKTUALITAETS_CHECK`
* **Strategische Bewertung (0.4):** `RISIKO_ANALYSE`
* **Kreative Synthese / Neuschaffung (0.8):** `BUSINESS_INKUBATOR`

---

### Grounding & Search-Integration
Wenn Google Search Grounding (`activeGrounding = true`) genutzt wird (erzwungen bei `AKTUALITAETS_CHECK` und `FEHLINFORMATIONS_RADAR` oder manuell vom User aktiviert):
1. Das Tool-Objekt `Tool(googleSearch = emptyMap())` wird in den API Request injiziert.
2. **Kritische Einschränkung:** Sobald Grounding aktiv ist, weigert sich die Gemini-API, ein striktes JSON-Formatschema zu erzwingen (`responseMimeType` muss von `application/json` auf `null` zurückgesetzt werden). Das System wechselt in diesem Fall auf unstrukturierten Text-Rückgabetyp und verlässt sich auf die Regex-Kompensation zum Strukturaufbau.

---

### Response Handling: Parser & Fallbacks
Das System verarbeitet Antworten mehrstufig, um Abstürze bei strukturellen Abweichungen des LLMs unbemerkt im Hintergrund zu reparieren:

#### Stufe 1: Moshi JSON schema parsing
Versucht, das vom API-Server gelieferte JSON-Objekt streng in die `AbstractorSummary`-Datenklasse zu überführen. Nach erfolgreichem Parsing werden die Bulletpoints durch Bereinigungsmethoden (`cleanTakeawayItem`, `cleanFactsVsOpinionsTakeaway`) von fehlerhaften LLM-Artefakten (wie doppelten Aufzählungstrichen oder Anführungszeichen) befreit.

#### Stufe 2: Syntaktische JSON-Korrektur (Vor-Verarbeitung)
* Lokalisierung des JSON-Kerns durch Extraktion aller Zeichen zwischen den ersten `{` und letzten `}` geschweiften Klammern im Antworttext.
* Entfernung illegaler, abschließender Kommata (`,\s*\}` oder `,\s*\]`), die bei LLM-Generierungen häufig vorkommen.
* Automatisches Mapping von CamelCase-Tasten (`originalUrl`, `shortDescription`, `keyTakeaways`) auf die geforderten SnakeCase-Attribute des Moshi-Adapters.

#### Stufe 3: Robuster Regex-Fallback-Parser
Sollte die JSON-Kette komplett unvollständig, abgeschnitten oder durch Freitext-Zusätze beschädigt sein, greift die Regex-Ebene:
* **Feldextraktion:** Extraktion von `title`, `short_description` und `owner` über fokussierte reguläre Ausdrücke (z. B. `\"title\"\\s*:\\s*\"([^\"]*)\"`).
* **Array-Extraktion:** Isolierung der eckigen Klammern `[...]` bei `key_takeaways` und schrittweises Auslesen aller String-Elemente.
* **Zeilen-Parser:** Versagt selbst die Array-Isolierung, spaltet der Parser die Antwort in Zeilen auf und wandelt jede Zeile, die mit Standard-Aufzählungszeichen (`-`, `*`, `•`) oder Ziffern (`1.`, `2)`) beginnt, eigenständig in einen sauberen Text-Stichpunkt um.

---

### Fehlerbehandlung & Ausfallsicherheit
1. **HTTP 429 & Quota Limit (RESOURCE_EXHAUSTED):**
   Führt den API-Call sofort mit denselben Parametern auf dem redundanten Ausfallmodell (`gemini-3.5-flash`) aus. Ein detaillierter JSON-Body-Scraper liest Metriken wie `quota_metric`, `quota_limit_value` und `retryDelay` für die diagnostischen ADB Logs aus.
2. **System-Fallbacks bei Totalausfällen:**
   Sollten beide Modelle ausfallen, wird die Exception kontrolliert bis zum UI-Status der ViewModels emporgehoben und dem User verständlich als `Fehler beim Laden` präsentiert (keine "NullPointerExceptions" im UI-Thread).

---

## 4. Prompt-System (Architekturen im Vergleich)

### IST-Zustand (Hardcoded Kotlin Schicht)
* **Ablage:** Alle Systemanweisungen und Usertemplates liegen im Code-Singleton `GeminiRepository` (in `GeminiNetwork.kt`) als statische, unveränderliche Kotlin-Strings.
* **Dynamische Zusammensetzung:** Der User-Prompt wird zur Laufzeit über einen speichereffizienten `StringBuilder` zusammengesetzt. Dabei wird die Ziel-URL, der über den `WebpageExtractor` bzw. `YoutubeTranscriptHelper` extrahierte Quelltext sowie ein hardcodiertes JSON-Formatbeispiel zusammengeschmolzen.
* **Problem:** Um einen Prompt anzupassen, Rechtschreibfehler im Instruktionstext zu korrigieren oder ein Ausgabe-Detail zu schärfen, muss die App neu kompiliert und als neues APK-Paket verteilt werden.

---

### Ziel-Architektur (Externalisiertes Asset-System)
* **Ablage:** Alle Prompts sind bereits als saubere Markdown-Dateien (`.md`) im Verzeichnis `/app/src/main/assets/prompts/` vorkonfiguriert.
* **Zentrale Registrierung:** Die Datei `prompt_manifest.json` dient als "Registry". Sie steuert pro `AnalysisType` die Dateizuordnung, Versionsstände, Grounding-Anforderungen, JSON-Schema-Vorschriften und das spezifische Qualitätsprofil.
* **Globale Qualitätsschranke:** In `_global_quality_rules.md` sind globale Grundregeln festgehalten (z. B. "DYNAMISCHER UMFANG", "SUBSTANZ STATT BLABLA", "STRUKTUR-JSON-GEBOT"), die für alle Typen gleichermaßen gelten.

#### Status der Ziel-Architektur: **ENTWURF (Inaktiv im Code)**
* Die Asset-Markdown-Dateien und das Manifest sind vorhanden, werden aber **zur Laufzeit noch nicht vom Android AssetManager ausgelesen**.
* In `prompt_manifest.json` ist für alle Einträge das Attribut `"runtime_active"` auf `false` gesetzt, um anzuzeigen, dass der Code aktuell noch exklusiv die hardcodeten Strings nutzt.

---

## 5. Qualitätsmodell (Vorgaben für den „Wow-Effekt“)

Um dem User eine exzellente Leseerfahrung zu bieten, die sich von gewöhnlicher "KI-Massenware" abhebt, folgt die Prompt-Generierung strengen redaktionellen Qualitätsmaßstäben:

1. **Intellektuelle Tiefe (Substanz über Marketing):**
   Das System filtert einleitendes Geplänkel, werbliche Selbstbeweihräucherung und repetitive Zusammenfassungen sofort aus. Nur harte, belegbare Daten, Strategien und Erkenntnisse überleben die Analyse.
2. **Dynamische Inhaltsanpassung (Kein "One-Size-Fits-All"):**
   Der KI-Ausgabeumfang verhält sich elastisch zur Komplexität der Quelle. Ein langes, 30-seitiges PDF-Dokument liefert eine detailtiefe Analyse mit vielen Takeaways, während ein trivialer News-Artikel prägnant auf 3 präzise Sätze verdichtet wird.
3. **Optimale visuelle Scanbarkeit:**
   Fast alle Analysemodi erzwingen die Ausgaben-Einleitung über **fettgedruckte Leitbegriffe** (z. B. `**Wirtschaftliches Risiko**: ...`), um dem menschlichen Auge das gezielte Überfliegen der Liste im "Scangrid"-Modus zu erleichtern.
4. **Schutz vor Text-Verschachtelungen:**
   Strikte Verbote von Sub-Mündungen, Bindestrich-Treppen, Sternchen-Listen oder Tabs innerhalb einzelner Bulletpoints stellen sicher, dass das UI-Modell ein flaches, sauberes und perfekt lesbares Zeilenbild zeichnet.

---

## 6. UI / UX Status (Design & Interaktion)

### Startbildschirm (Cockpit-Zentrale)
* **URL-Eingabefeld:** Zentrales, einzeiliges Textfeld auf der Startseite zur Eingabe der zu analysierenden Web-Adresse. Sie unterstützt vollautomatisches URL-Cleaning und Redirect-Auflösung.
* **Analyse-Cockpit:** Ein zweispaltiges Grid aus interaktiven Funktionskarten. Jede Karte repräsentiert einen eigenen `AnalysisType` (z. B. Risikoanalyse, Fakt vs. Meinung).
* **Direkt-Trigger (Arbeitspaket 1 implementiert!):** Sobald der Nutzer auf eine der Funktionskarten tippt, startet das ViewModel **sofort** die entsprechende Analyse auf Basis des aktuellen URL-Inhalts. Der alte, redundante und sperrige Button "Analyse starten" am unteren Bildschirmrand wurde restlos entfernt, was zu einer ungemein schnellen und intuitiven UX führt.
* **Datei- und Dokumentenimport:** Über ein Floating Action Button (FAB) oder ein Büroklammer-Icon kann der User lokale Medien (Bilder mit Text, PDF-Dokumente) einspielen. Das System leitet diese automatisch in den `DOKUMENTE`-Analysemodus weiter.

### Ergebnisseite
Die Ergebnisseite zeichnet sich durch einen hochglanzpolierten Material-3-Look aus:
* **Kontext-Box:** Eine elegante, abgerundete Status-Karte oben zeigt das Dokumentensymbol, die Quelladresse und den erkannten Autor/Herausgeber (`owner`).
* **Ergebnis-Stapel:** Ein scrollbares, großzügig schattiertes Card-Element visualisiert die extrahierte `short_description` und den Bulletpoint-Stapel aus `key_takeaways`.
* **Utility-Aktionen:** Integrierte Schnelltasten für "In Zwischenablage kopieren" und "Teilen" (System Share Intent) erlauben die schnelle Weitergabe des Ergebnis-Briefings.

### Aktuelle UI/UX-Probleme (Schulden)
* **Kopier-Formate:** Die Text-Ausgabebelegung für das Kopieren konvertiert HTML-Fettdrucke (`**`) manchmal unschön in reinen Plain-Text ohne ansprechende Absätze.
* **Inkonsistentes Briefing-Auge:** Manche Analysetypen (wie `3 Kernpunkte` oder `Fakt vs Meinung`) weichen strukturell stark im Layoutvorgabebild ab, was auf dem UI-Ergebnisscreen gelegentlich zu unruhiger Typografie führt.

---

## 7. Aktueller Systemstatus

* **API-Infrastruktur:** Hervorragend. Der in der App integrierte API-Key nutzt den stabilen Google AI Paid/Pro-Billing-Bereich, weshalb keine Ratenlimitierungs-Drosselungen im normalen Betrieb auftreten.
* **Netzwerkstabilität:** Sehr hoch. Die automatische URL-Redirect-Entschlüsselung löst verdeckte Adressen (z. B. Kurz-URLs von YouTube oder Twitter) vor der Gemini-Übergabe lokal im HttpClient auf, um verfälschte LLM-Datenabfragen zu verhindern.
* **Kompilierung:** Das Gesamtprojekt baut absolut fehlerfrei (`compile_applet` liefert konstante Erfolgsmeldungen) und ist im Streaming-Emulator sofort start- und einsatzbereit.

---

## 8. Bekannte Architekturprobleme & technische Schulden

1. **Schema-Redundanz:**
   Die Struktur des Antwort-JSONs ist doppelt deklariert: Einmal als statisches Kotlin-Schema-Payload-Objekt (`abstractorSummarySchema`) zur Übermittlung an die API und parallel als formulierter Freitext-Leitfaden in jedem einzelnen String-Prompt.
2. **Koppelungs-Hürde:**
   Der Code verlässt sich auf scharfkantige String-Trimming-Operationen wie das Abschneiden von führenden Bindestrichen oder Ziffern (`cleanTakeawayItem`). Ändern sich die Prompts (oder weicht das API-Modell durch Updates leicht ab), können diese Filter wichtige Textbestandteile zerstören oder fehlerhafte Suffixe hinterlassen.
3. **Inaktive Prompt-Schätze:**
   Das Verzeichnis `/assets/prompts` und die zugehörige `prompt_manifest.json` sind hochgradig optimiert und gepflegt, bleiben im produktiven Runtime-Code der App zurzeit jedoch ungenutzt.

---

## 9. Zielvision (Das finale System)

* **Die Prompt Engine:** Vollständige Entkopplung. Das `GeminiRepository` liest zur Laufzeit über den `AssetManager` das `prompt_manifest.json` ein, lädt die gewünschte `.md`-Dokumentdatei und fügt sie dynamisch mit den globalen Qualitätsregeln (`_global_quality_rules.md`) zusammen.
* **Das Insight Dashboard:** Evolution der UI zu einem echten Datenbüro. Ergebnisse werden nicht mehr nur linear untereinander gereiht, sondern über visualisierte Metrik-Badges (z. B. ein farbiger "Glaubwürdigkeits-Index" für Fehlinformationen) intuitiv darstellbar gemacht.
* **Das Executive Briefing System:** Standardisiertes Sharing-Layout, welches Berichte beim Teilen automatisch in saubere Markdown-Dateien oder PDF-Protokolle verpackt.

---
**ENDE DES BLUEPRINTS**  
*Dieses Dokument dient als verbindliche Lektüre- und Einstiegsgrundlage für alle anschließenden Entwicklungssitzungen und Folge-Chats.*
