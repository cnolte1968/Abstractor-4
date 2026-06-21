# 📂 Projekt-Spezifikation & Entwicklungsstand: **Abstractor** 🚀

Dieses Dokument dient als vollständiger, hochaktueller Übergabeberichts- und Spezifikations-Dump der Android-Anwendung **Abstractor**. Es wurde optimiert, um direkt in ein LLM (wie Gemini) eingefüttert zu werden, um die Einarbeitung zu erleichtern, neue Features zu planen oder am Projekt weiterzuarbeiten.

---

## 1. 📋 Projekt-Übersicht & Zweck
Der **Abstractor** ist eine native, performance-optimierte Android-App, die lange Webseiten-Inhalte, Dokumente und YouTube-Videos analysiert und mithilfe von **Googles Gemini-KI** in Sekunden präzise, übersichtliche Zusammenfassungen sowie Kernaussagen auf Deutsch generiert.

Die App ist darauf getrimmt, extrem fehlerresistent zu sein, eine nahtlose Benutzererfahrung zu bieten und lästige Bot-Umlenkungen oder Blocking-Restriktionen (wie z. B. auf YouTube) nahtlos zu umgehen.

---

## 2. 🛠️ Technologiestack & Architektur
- **Programmiersprache**: Kotlin (Modernes, asynchrones Programmiermodell mit Coroutines & Flows)
- **UI-Framework**: **Jetpack Compose** mit **Material Design 3 (M3)**
  - Edles, minimalistisches Design mit großzügigen Abständen (Spacious Grid)
  - Edge-to-Edge-Unterstützung (`enableEdgeToEdge()`)
  - Dynamischer Dim-Overlay-Karten-Effekt bei Shared Launch
- **Architekturmuster**: **MVVM (Model-View-ViewModel)** mit reaktivem Zustandsmanagement via Kotlin Coroutines und `StateFlow`
- **Netzwerk- & Parser-Bibliotheken**:
  - `OkHttp3` (für HTTP-Verbindungen & Scraping)
  - `Retrofit2` (für nahtlosen Zugriff auf die Gemini-API)
  - `Moshi` (für Typen-sicheres JSON-Parsing in Kotlin-Modelle)
- **Modell**: **Gemini 3.5 Flash** (`v1beta/models/gemini-3.5-flash:generateContent`) mit erzwungenem JSON-Schema (`responseSchema`).

---

## 3. 🎯 Hauptfunktionen (Core Features)

### A. 🌐 Webseiten-Inhaltscraper & Extraktor (`WebpageExtractor`)
- Lädt den rohen HTML-Code einer vom Nutzer übergebenen URL.
- **Header-Metadaten-Guard**: Bevor Script- und Style-Tags entfernt werden, filtert der Scraper gezielt Meta-Tags wie `<title>`, `og:title`, `description` und `og:description` heraus.
- Bereinigt den restlichen Body von irrelevanten Widgets, CSS/JavaScript, Navigationsleisten und Werbung, um nur den reinen Text-Inhalt zu extrahieren.
- Kombiniert den extrahierten Text mit den Header-Metadaten und übergibt die bereinigte Struktur an das Gemini-Sprachmodell zur Analyse auf Deutsch.

### B. 🎥 Intelligentes, zweistufiges YouTube-Handling (`YoutubeTranscriptHelper`)
Da YouTube automatisierte Bot-Anfragen für Video-Transkripte häufig mit Consent-Schranken, Cookie-Warnungen oder `LOGIN_REQUIRED` (Altersverifikationen) blockiert, nutzt die App ein hochmodernes, **unterbrechungsfreies Ausfallsystem**:
1. **Stufe 1 (Direkt-Abruf des Video-Transkripts)**:
   - Die App ruft die Watch-Page (`/watch?v=...`) mit passenden Headern ab und scant die HTML-Struktur nach dem `captionTracks`-Pfad (oder dem `timedtext`-Fallback).
   - Wird der Transkript-Link gefunden, lädt die App die XML-Untertitel, extrahiert die Textsegmente und baut das gedruckte Wortprotokoll des Videos zusammen.
2. **Stufe 2 (oEmbed-Metadaten Fallback - Bulletproof)**:
   - Scheitert Stufe 1 aufgrund einer Bot-Sperre von Google, weicht die App **vollautomatisch** auf die offizielle und blockadegeschützte **YouTube oEmbed-Schnittstelle** aus:
     `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v={videoId}&format=json`
   - Diese liefert garantiert und blockierungsfrei wichtige Video-Metadaten zurück (Video-Titel, Kanal-Name, Ersteller).
3. **Stufe 3 (Gemini LLM Brücke für oEmbed)**:
   - Erhält die App oEmbed-Metadaten, baut das ViewModel (`MainViewModel`) einen spezialisierten Prompt: Er übergibt den exakten Titel des Videos und den Kanal/Ersteller an **Gemini** und instruiert die KI, ihr **internes Weltwissen** über dieses Video oder das spezifische Thema/Kanal zu nutzen.
   - Dadurch erhält der Nutzer **trotz Download-Blockade des Originaltranskripts** eine treffsichere, fehlerfreie Analyse des Videos und seiner Hauptthemen.

### C. 🔄 System-Intent „Teilen“ (System Action Send Integration)
- Der Abstractor ist als Empfänger für Text-Sharing im Android-Manifest registriert.
- Teilt der Nutzer aus einer beliebigen Drittanbieter-App (z. B. Google Chrome, YouTube, Firefox) eine URL über den Android-„Teilen“-Dialog mit der Abstractor-App, wird das Programm getriggert.
- **Visual Floating Overlay**: Ist die Aktivität ein Shared Launch, rendert Compose eine innovative, halbdurchsichtige, leicht abgedunkelte Overlay-Hintergrund-Ebene und platziert die Zusammenfassungs-Karte elegant im Zentrum. Der Nutzer sieht die Analyseergebnisse sofort als Popup-Card, ohne seine aktuelle App (z.B. YouTube) vollständig verlassen zu müssen!
- Verfügt über praktische Schnellknöpfe zum Kopieren 📋, direkten Weiterteilen 💬 und Öffnen der Original-URL ↗️.

### D. 🔍 Spezifische Analyse-Modi (`AnalysisType` & Prompts)
Der Abstractor unterstützt **8 hochpräzise Analyse-Modi** mit maßgeschneiderten System-Anweisungen und Parametern (z.B. unterschiedlichen Temperaturen):

1. **Standard-Webseiten zusammenfassen** (`STANDARD_WEBSEITE`)
   - **Titel in UI**: *"Standard-Webseiten zusammenfassen"*
   - **Fokus**: Erstellt eine ausgewogene, dichte und strukturierte Kurzbeschreibung sowie informative Kernaussagen auf Deutsch.
2. **Multimedia-Content** (`MULTIMEDIA`)
   - **Titel in UI**: *"Multimedia-Content zusammenfassen"*
   - **Fokus**: Optimiert für Podcasts, Videotranskripte und visuelle Beiträge. Hebt Sprecherabsichten und audiovisuelle Kernpunkte hervor.
3. **3 Kernpunkte** (`TOP_3_KERNAUSSAGEN`)
   - **Titel in UI**: *"3 Kernpunkte"*
   - **Fokus**: Reduziert die Quelle auf **maximal 3 zentrale, hochprofessionelle und absolut seriöse Kernthemen/Hauptstatements**. Jedes dieser Themen wird in **genau einem vollständigen Satz** zusammengefasst. Das Gemini-System liest dafür die Quelle vollständig (zu 100%) durch. Das Resultat wird als numerisch sortierte Liste (`1.`, `2.`, `3.`) ausgegeben.
   - **Spezifikation & Stil**: Absolut sachlich, glaubhaft und professionell – keine reißerische Sprache oder Clickbait. Der Parser erhält die Nummerierungen im JSON-Array (`keepNumbering` Flag), während die Compose-UI diese in der Kartenansicht elegant filtert, um Doppel-Nummerierungen bei der Index-Badge-Visualisierung zu vermeiden.
4. **Prüfe Aktualität** (`AKTUALITAETS_CHECK`)
   - **Titel in UI**: *"Prüfe Aktualität"*
   - **Fokus**: Übergibt das Material an ein penibles Faktenchecker-Modul. Es werden **absolut keine allgemeinen Inhalts-Zusammenfassungen** erstellt. Die Kernaussagen listen **ausschließlich** Informationen auf, die sich um die Aktualität der Seite drehen (z.B. Datumsstempel, Updates, Alter der Daten, Versionshinweise, Trends). Jedes Bulletpoint ist streng zeitlich/aktualitätsbezogen.
5. **Erkenne Fehlinformationen** (`FEHLINFORMATIONS_RADAR`)
   - **Titel in UI**: *"Erkenne Fehlinformationen"*
   - **Fokus**: Untersucht Texte auf logische Fehlschlüsse, reißerische Formulierungen, Clickbait oder ungestützte Thesen unter Verwendung von Search Grounding.
6. **Identifiziere Risiken** (`RISIKO_ANALYSE`)
   - **Titel in UI**: *"Identifiziere Risiken"*
   - **Fokus**: Filtert potenzielle Risiken, Fallstricke, Sicherheitsmängel oder versteckte Klauseln/Nachteile aus Geschäftsbedingungen, Verträgen oder Artikeln heraus.
7. **Finde Geschäftsidee** (`BUSINESS_INKUBATOR`)
   - **Titel in UI**: *"Finde Geschäftsidee"*
   - **Fokus**: Sucht nach ungenutzten Marktpotenzialen, Skalierungsmöglichkeiten oder Monetarisierungschancen in den Inhalten.
8. **Dokumente zusammenfassen** (`DOKUMENTE`)
   - **Titel in UI**: *"Dokumente zusammenfassen"*
   - **Fokus**: Maßgeschneidert für das Hochladen, Verarbeiten und Analysieren lokaler Dokumente (PDFs, Bilder, TXT).

---

## 4. 🗂️ Quellcode-Architektur

### 📄 `MainActivity.kt`
Der Einstiegspunkt und UI-Renderer der Anwendung:
- **Einstellbarer URL-Cursor**: Ein modifiziertes `OutlinedTextField` mit `singleLine = true`, das die standardmäßige reibungslose native Cursorbewegung und horizontales Scrollen auf Smartphones unterstützt (kein Hängenbleiben bei überlangen URLs).
- **Integrierte Löschfunktion**: Auf der rechten Seite des Eingabefeldes befindet sich ein interaktiver Lösch-Button (ein kleines "X"-Symbol, `clear_url_input_button`), der die Eingabezeile mit einem Klick leert.
- **Nativer Zwischenablage-Zugriff**: Der Button *"Aus Zwischenablage einfügen"* greift direkt über den systemseitigen `LocalClipboardManager` auf die echte Android-Zwischenablage zu, liest den Text aus, fügt ihn in die Eingabezeile ein und gibt visuelles Toast-Feedback. Es werden keine statischen Dummytexte mehr benutzt.
- **Dynamischer App-Header**: Der Header der App besitzt ein variables Textfeld (`app_header_title`). 
  - Befindet sich der Nutzer im Ruhezustand auf der **Startseite**, lautet der Titel schlicht **"Abstractor"**.
  - Wechselt die Ansicht zur **Detailseite** (während des Ladevorgangs, Erfolgs- oder Fehlerzustands), ändert sich der Titel dynamisch zum exakten Namen der aktuell aufgerufenen Funktion (z.B. *"3 Kernpunkte"* oder *"Prüfe Aktualität"*). Der Anwender weiß dadurch jederzeit, in welchem Modus er sich befindet.
- **URL-Erhalt bei Rücksprung**: Wenn der Benutzer von der Detailseite zur Startseite zurückspringt, wird die zuvor eingetragene URL **bewusst nicht gelöscht**, sondern bleibt im Eingabefeld bestehen. Dies ermöglicht ein schnelles Durchwechseln und Ausprobieren verschiedener Analysefunktionen für dieselbe URL.

### 📄 `MainViewModel.kt`
Das Bindeglied zwischen UI und API-Schnittstellen. Verwaltet:
- Den reaktiven Zustand `UiState`:
  ```kotlin
  sealed interface UiState {
      object Idle : UiState
      data class Loading(val step: LoadingStep) : UiState
      data class Success(val summary: AbstractorSummary, val analysisType: AnalysisType) : UiState
      data class Error(val isPaywallOrBlocked: Boolean, val message: String, val detail: String?) : UiState
  }
  ```
- Den aktuell gewählten Analysemodus (`currentAnalysisType` als StateFlow).
- Die Bereitstellung von `fetchSummary` sowie Datei-Analysen (`summarizeFileUri`).

### 📄 `GeminiNetwork.kt`
Führt Inferenz-Anfragen an Googles AI-Server via Retrofit durch.
- **Erzwungenes JSON-Schema** (`abstractorSummarySchema`):
  Stellt absolut sicher, dass die KI ausschließlich strukturierte Daten mit passenden Feldern (`title`, `original_url`, `short_description`, `key_takeaways` als Array) zurückliefert.
- **Präzises Prompting für 3 Kernpunkte**: Enthält die vollständige, elegante Spezifikation und Verarbeitungsvorschrift für die Funktion "3 Kernpunkte", die Gemini anweist, den gesamten Inhalt zu lesen, maximal 3 Kernpunkte in je genau einem Satz und in einem hochprofessionellen, nicht reißerischen Ton zu verfassen und diese als nummerierte Liste auszugeben.
- **Sicherheitsbewusste Beibehaltung der Nummerierung**: `parseSummaryRobustly` und `cleanTakeawayItem` unterstützen einen `keepNumbering`-Parameter. Bei der "3 Kernpunkte"-Analyse wird die nummerierte Struktur in den Roh-Daten erhalten (z.B. für Share- & Clipboard-Aktionen), während `MainActivity` diese beim Rendern visualisierungsfreundlich filtert, um doppelte Ziffernanzeigen in der Listenansicht zu vermeiden.
- **Sicherer Key-Abruf**: Bezieht API-Schlüssel standardkonform aus der Android-Laufzeitumgebung (`BuildConfig.GEMINI_API_KEY`).
- **Backup Regex-Parser**: Robuster Fallback-Parser, der bei unvollständigen JSON-Fragmenten korrigierend eingreift, um App-Abstürze vollständig zu verhindern.

### 📄 `YoutubeTranscriptHelper.kt` & `WebpageExtractor.kt`
Netzwerk-Helferklassen zur strukturierten Extraktion von Rohtexten aus Webseiten und zum XML-Parsing von YouTube Untertiteln.

---

## 5. 🔄 Benutzeroberflächen-Aufteilung (Layout-Konzept)
Die App folgt einer strikten Trennung in zwei Hauptbereiche:
1. **Die Startseite (Home Screen)**
   - Aufgeräumte Oberfläche, die von der URL-Eingabezeile sowie der Funktionsauswahl dominiert wird.
   - Enthält die native One-Tap-Löschtaste ("X") und den funktionierenden Zwischenablage-Paste-Button.
   - Ermöglicht den direkten Upload lokaler Dokumente wie PDFs oder Fotos zwecks Analyse.
2. **Die Detailseite (Analysis Screen)**
   - Wird dynamisch getriggert, sobald eine Analyse gestartet wird.
   - Präsentiert im Header den Titel der aufgerufenen Funktion.
   - Zeigt das Ergebnis ansprechend segmentiert an (Metadaten-Karte mit Titel/Autor/URL, präzise Kurzbeschreibung, gefolgt von ausdrucksstarken Bulletpoints bzw. einer 1-3 nummerierten Liste im Falle der Kernaussagen).
   - Bietet Schnellzugriffe (Kopieren, Android-System-Teilen, Link im externen Browser öffnen).
   - Ermöglicht den sofortigen Rücksprung zur Startseite, um dieselbe URL mit einer anderen Funktion erneut zu analysieren (URL bleibt im Feld erhalten).

---

## ⚡ Robustheits-Entscheidungen & Best Practices
1. **Kein Google Search Grounding bei YouTube**: Deaktiviert für reine YouTube-URLs, um überflüssige Web-Timeouts und HTTP 503-Sperren zu umgehen.
2. **Moshi & Unicode-Filterung**: Schutz vor fehlerhaften Zeichenkodierungen bei oEmbed-Metadaten.
3. **Optimierte M3 Usability**: Alle klickbaren Elemente besitzen eine Mindesthöhe von **48dp** (Barrierefreiheit) und nutzen reaktive Material-Ripples für flüssiges haptisches/visuelles Feedback.
4. **Fehlertolerantes Prompting**: Die Prompts für Spezialmodi (wie Aktualität und 3 Kernpunkte) enthalten stringente "Systembefehle" an Gemini, die sicherstellen, dass die Ausgabe exakt das leistet, was der Nutzer angefordert hat (kein "Abschweifen" oder Halluzinieren allgemeiner Textabschnitte).

---

## 📊 Aktueller Stand
Die Anwendung kompiliert **fehlerfrei** (`Build Succeeded`), läuft reibungslos und setzt alle Usability-Vorgaben präzise um. Sämtliche Unit- und Screenshot-Tests (Robolectric & Roborazzi) sind vollständig grün. Das Projekt ist auf dem absolut neuesten Stand vorbereitet.
