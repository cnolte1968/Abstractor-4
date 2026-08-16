# RELEVANTOR - Manuelle Smartphone-Testfälle

Dieses Dokument hält die final festgelegten manuellen Testfälle für die Überprüfung auf physischen Geräten fest.
Die verbindliche Source of Truth im Code ist `com.example.data.diagnostics.TestReferenceRegistry`.

---

## 1. Web-Testfälle (WEB_SUMMARY, KEY_TAKEAWAYS, FACTS_VS_OPINIONS)

### WEB-01 Standard Webseiten-Analyse (Reisebericht)
*   **Titel:** Wischnewski in Guinea-Bissau
*   **URL:** `https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/`
*   **Erwartung:** Vollständige strukturierte Zusammenfassung und Kernaussagen.

### WEB-02 Wikipedia Hauptseite (Heterogener Content)
*   **Titel:** Wikipedia Main Page
*   **URL:** `https://en.wikipedia.org/wiki/Main_Page`
*   **Erwartung:** Stabile Extraktion und präzise Takeaways.

### WEB-03 Android News Redirect / Focus Online (Sam Altman Falschmeldung)
*   **Titel:** Panne bei Google: Suchmaschine zeigt kurzzeitig Tod von OpenAI-Chef Sam Altman an
*   **URL (Share):** `https://share.google/OLQ1vkrSTzTWyCwoM`
*   **Ziel-URL:** `https://www.focus.de/panorama/welt/panne-bei-google-suchmaschine-zeigt-kurzzeitig-tod-von-openai-chef-sam-altman-an_25885e2a-8b33-443e-8e2f-333dffa6ea1f.html`
*   **Funktionen:** `WEB_SUMMARY`, `FRESHNESS_CHECK`, `MISINFORMATION_RADAR`
*   **Erwartung:**
    *   `share.google` Redirect wird sauber aufgelöst.
    *   Focus Online Artikelinhalt wird extrahiert.
    *   WEB_SUMMARY liefert strukturierte Zusammenfassung PASS.
    *   FRESHNESS_CHECK ordnet Vorfall zeitlich ein.
    *   MISINFORMATION_RADAR bewertet Fehlinformationsrisiko der Suchmaschinen-Panne sachlich.

---

## 2. YouTube-Testfälle (MULTIMEDIA_ANALYSIS)

### YT-01 Transcript verfügbar
*   **Titel:** How AI Could Save (Not Destroy) Education | Sal Khan | TED
*   **URL:** `https://www.youtube.com/watch?v=hJP5GqnTrNo`
*   **Erwartung:** 
    *   Profil besitzt Transkript.
    *   Multimedia-Analyse ist aktiv und liefert ein Ergebnis OHNE Degraded-Warnung.

### YT-02 Degraded-Fall (Ohne Transkript)
*   **Titel:** Lofi Girl: lofi hip hop radio – beats to relax/study to
*   **URL:** `https://www.youtube.com/watch?v=5qap5aO4i9A`
*   **Erwartung:** 
    *   Profil besitzt KEIN Transkript.
    *   Multimedia-Analyse ist als POTENTIAL / DEGRADED gekennzeichnet.
    *   Die Ausführung erfolgt erfolgreich, gibt aber einen Degraded-Hinweis aus und basiert rein auf Titel, Beschreibung und Metadaten.

---

## 3. Google Maps-Testfälle (MAPS_ANALYZER, LOCATION_CONTEXT, LOCATION_QUERY)

### MAPS-01 Eindeutiger POI (Doi Suthep / Brandenburger Tor)
*   **Titel:** Wat Phra That Doi Suthep
*   **URL:** `https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep`
*   **Erwartung:** Sofortige eindeutige Places-Auflösung ohne Disambiguierungs-Rückfrage.

### MAPS-02 Komplexer Shortlink mit Plus-Code & CID
*   **Titel:** โจ๊กหลังมอ ประตูวิศวะ (Congee street food Chiang Mai)
*   **URL:** `https://maps.app.goo.gl/WgXTvya1yCDJjameA`
*   **Erwartung:**
    *   Parser extrahiert Plus-Code und CID `0xbc6488380210c370:0xbc698c3e02104370`.
    *   Automatische Disambiguierung wählt `ChIJJ7Q_mWE72jARcEMQAjiMjLw` aus.
    *   Erfolgreiche Analyse ohne Abbruch.

---

## 4. Analyse-Testfälle (RISK_ANALYSIS, PERSPECTIVES, RELEVANT_ASPECTS, FREE_SOURCE_QUERY)

### ANA-01 Freie Quellenabfrage (FREE_SOURCE_QUERY)
*   **URL (Share):** `https://share.google/OLQ1vkrSTzTWyCwoM`
*   **Frage:** `Was genau ist laut Artikel bei Google passiert und wie wurde der Fehler erklärt?`
*   **Erwartung:**
    *   share.google Redirect wird aufgelöst.
    *   Artikeltext wird analysiert.
    *   Frage wird präzise aus dem Quellentext beantwortet.

### ANA-02 Gesundheitsrisiken (RISK_ANALYSIS)
*   **Titel:** WHO - Radon and Health
*   **URL:** `https://www.who.int/news-room/fact-sheets/detail/radon-and-health`
*   **Erwartung:**
    *   Quelle wird vollständig extrahiert.
    *   Konkrete Gesundheitsrisiken und Synergie-Effekte (z.B. beim Rauchen) werden strukturiert erkannt und kategorisiert.

### ANA-03 Perspektiven & Gegenpositionen (PERSPECTIVES_COUNTERPOSITIONS)
*   **Titel:** Pew Research Center - Views of risks, opportunities and regulation of AI
*   **URL:** `https://www.pewresearch.org/2025/04/03/views-of-risks-opportunities-and-regulation-of-ai/`
*   **Erwartung:**
    *   Öffentlichkeits- und Expertenperspektiven sauber differenziert.
    *   Gemeinsamkeiten und Gegensätze herausgearbeitet.

### ANA-04 Relevante Nebenaspekte & Dimensionen (RELEVANT_ASPECTS)
*   **Titel:** GOV.UK - Agentic AI and Consumers
*   **URL:** `https://www.gov.uk/government/publications/agentic-ai-and-consumers/agentic-ai-and-consumers`
*   **Erwartung:**
    *   Relevante Dimensionen (Recht, Verbraucherschutz, Risiken) strukturiert aufgeführt.

---

## 5. Dokument- & Bild-Testfälle (DOCUMENT_SUMMARY, PHOTO_SCREENSHOT_ANALYSIS)

### DOC-01 Dokument-Zusammenfassung (DOCUMENT_SUMMARY)
*   **Easy URL:** `https://www.gov.uk/government/publications/ai-cyber-threats-open-letter-to-business-leaders`
*   **Difficult URL:** `https://cdn.websitebuilder.service.justice.gov.uk/uploads/sites/54/2025/07/AI-paper-PDF.pdf`
*   **Erwartung:** Dokument/PDF wird verarbeitet und strukturiert zusammengefasst.

### IMG-01 Bild- & Screenshot-Analyse (PHOTO_SCREENSHOT_ANALYSIS)
*   **Easy Source:** `https://www.jpl.nasa.gov/images/pia00123-earth-pacific-ocean/`
*   **Difficult Source:** `https://science.nasa.gov/resource/earth-poster-version-d/`
*   **Erwartung:** Hauptmotive und Text-/Grafikelemente der Bildquelle korrekt erkannt.

---

## 6. Platzhalter / Noch nicht entwickelt – aktuell außerhalb des Quality Gates

Folgende Funktionen sind reine Navigations- bzw. UI-Platzhalter und besitzen aktuell keine Testfälle und kein aktives Test-Soll:
*   **Bild mit KI erzeugt?** (`AI_IMAGE_DETECTOR`, Kategorie E - Arbeiten mit Dateien)
*   **Social-Media-Generator** (`SOCIAL_MEDIA_GENERATOR`, Kategorie D - Inhalte verarbeiten)
*   **Kommunikations-Generator** (`COMMUNICATION_GENERATOR`, Kategorie D - Inhalte verarbeiten)
*   **Zusammenfassung aus mehreren URL** (`MULTI_URL_SUMMARY`, Kategorie D - Inhalte verarbeiten)
*   **Infografik-Generator** (`INFOGRAPHIC_GENERATOR`, Kategorie C - Visualisierung)
*   **Struktur-Visualisierer** (`STRUCTURE_VISUALIZER`, Kategorie C - Visualisierung)
*   **Bildideen-Generator** (`IMAGE_IDEA_GENERATOR`, Kategorie C - Visualisierung)

