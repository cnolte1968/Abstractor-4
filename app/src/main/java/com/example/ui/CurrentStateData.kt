package com.example.ui

object CurrentStateData {
    val MARKDOWN_TEXT = """
# 📂 Projekt-Spezifikation & Entwicklungsstand: **Abstractor** 🚀

Dieses Dokument dient als vollständiger Übergabeberichts- und Spezifikations-Dump der Android-Anwendung **Abstractor**, der direkt in ein LLM (wie Gemini) eingefüttert werden kann, um neue Features zu planen oder am Projekt weiterzuarbeiten.

---

## 1. 📋 Projekt-Übersicht & Zweck
Der **Abstractor** ist eine native, performance-optimierte Android-App, die lange Webseiten-Inhalte und YouTube-Videos analysiert und mithilfe von **Googles Gemini-KI** in Sekunden präzise, übersichtliche Zusammenfassungen sowie Kernaussagen auf Deutsch generiert.

Die App ist darauf getrimmt, extrem fehlerresistent zu sein und lästige Bot-Umlenkungen oder Blocking-Restriktionen (wie z. B. auf YouTube) nahtlos zu umgehen.

---

## 2. 🛠️ Technologiestack & Architektur
- **Programmiersprache**: Kotlin (Modernes, asynchrones Programmiermodell)
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

---

## 4. 🗂️ Quellcode-Architektur

### `MainActivity.kt`
Der Einstiegspunkt der Anwendung. Verwaltet die Intent-Datenübernahme (Shared Launch). Rendert die Compose-Bildschirme in Abhängigkeit des ViewModels:
- **`LauncherIntroSection`**: Eine aufgeräumte Startseite mit schönem Infoblock, URL-Eingabefeld (OutlinedTextField) und Such-Button.
- **`LoadingSpinner`**: Ein robuster, rotierender Fortschrittsanzeiger mit informativem Status-Hilfstext auf Deutsch.
- **`SummaryView`**: Zeigt den Titel der Quelle, die Original-URL, eine zweisätzige Kurzbeschreibung und die wichtigsten Kernaussagen als übersichtliche Bulletpoints an. Enthält Copy/Share-Aktionen.
- **`ErrorSection`**: Zeigt ansprechende Fehlerzustände (z. B. wenn Seiten nicht geladen werden können oder durch Bezahlschranken blockiert sind).

### `MainViewModel.kt`
Der Controller zwischen Netzwerk/Extraktor und dem UI-Zustand. Modelliert den Status der Anwendung als reaktiven `StateFlow<UiState>`:
```kotlin
sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val summary: DomainSummary) : UiState
    data class Error(val isPaywallOrBlocked: Boolean, val message: String, val detail: String?) : UiState
}
```
Regelt die Scrape-Reihenfolge: Erkennt Domain-Typen (YouTube oder Web-URL) und stößt die Scraper asynchron im Hintergrund des `viewModelScope` an.

### `GeminiNetwork.kt`
Konfiguriert Retrofit und führt die Inferenz-Anfragen an Googles Server durch. Enthaltene Konzepte:
- **Enforced JSON Response Schema**:
  ```kotlin
  private val abstractorSummarySchema = ResponseSchema(
      type = "OBJECT",
      properties = mapOf(
          "title" to SchemaProperty(type = "STRING", description = "Titel der Quelle"),
          "original_url" to SchemaProperty(type = "STRING", description = "Die unveränderte Original-URL"),
          "short_description" to SchemaProperty(type = "STRING", description = "Prägnante Kurzbeschreibung (max. zwei Sätze)"),
          "key_takeaways" to SchemaProperty(
              type = "ARRAY",
              description = "Kernaussagen als übersichtliche Bulletpoints",
              items = SchemaProperty(type = "STRING")
          )
      ),
      required = listOf("title", "original_url", "short_description", "key_takeaways")
  )
  ```
- **Hocheffektiver Regex-Parser Fallback**: Falls das Schema aufgrund unerwarteter KI-Formatänderungen fehlschlagen sollte, greift ein regulärer Ausdrucksschutz ein. Dieser filtert die Keys `title`, `short_description` und die Bulletpoint-Arrays direkt aus dem Text und garantiert, dass die App niemals abstürzt.
- **Sicherer Key-Abruf**: Holt die API-Schlüssel dynamisch aus den BuildConfig-Umgebungsvariablen (`Gemini_Abstractor`/`GEMINI_API_KEY`).

### `YoutubeTranscriptHelper.kt` & `WebpageExtractor.kt`
Klassen zur Ausführung von rohen Netzwerk-Scraps, XML-Subtitle-Parsing und JSON-Metadaten-Bereinigung.

---

## 5. ⚡ Robustheits-Entscheidungen & Best Practices
1. **Kein Google Search Grounding bei YouTube**: Für YouTube-URLs wird Search Grounding explizit deaktiviert, da das Grounding-Tool Webseiten-Suchen anstößt, welche bei Video-Plattformen häufig Timeouts oder Status-503-Fehler generieren.
2. **Schnelle Unit-Tests & Robolectric**: Es existieren Gradle-Unittest-Bedingungen. Der App-Name in der Testing-Umgebung spiegelt exakt den echten App-Namen „Abstractor“ wider.
3. **Moshi & Unicode-Filterung**: Für oEmbed-Antworten und JSON-Strings gibt es einen robusten Unicode-Wandler (`\\uXXXX`), der asiatische und europäische Sonderzeichen perfekt in native Char-Strings auflöst.

---

## 📈 Aktueller Status & Nächste Schritte
Die Anwendung kompiliert **fehlerfrei**, sämtliche lokalen JVM- und Unit-Tests laufen fehlerfrei durch. Die App ist voll einsatzbereit für:
- Erstellung von Verlaufstabellen (Local Room Database für vergangene Zusammenfassungen).
- Erstellung von Favoriten- und Bookmark-Listen für wichtige Offline-Zusammenfassungen.
- Optionale Personalisierung des KI-Zusammenfassungsstils (z.B. lustig, wissenschaftlich, extrem kurz).
    """.trimIndent()
}
