# RELEVANTOR - Zentrale Test-Referenz-Registry (CP-02)

Dieses Dokument dient als verbindliche Source of Truth für alle Testreferenzen der aktiven Relevantor-Funktionen sowie der sauberen Abgrenzung gegenüber UI-/Navigations-Platzhaltern.
Fachprompts (`app/src/main/assets/prompts/F_*.md`) bleiben dabei vollständig unverändert.

---

## 1. Testreferenz-Matrix (Aktive Quality-Gate-Funktionen: 15)

| Funktion (ID) | Easy Reference | Test Mode | Difficult Reference | Test Mode | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **WEB_SUMMARY** | `https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/` | **LIVE** | `https://share.google/OLQ1vkrSTzTWyCwoM` (Focus Online) | **LIVE** | **VERIFIED** |
| **KEY_TAKEAWAYS** | `https://en.wikipedia.org/wiki/Main_Page` | **LIVE** | `https://studienportal.de/homeoffice-produktivitaet-studie` | **SYNTHETIC** | **VERIFIED** |
| **FREE_SOURCE_QUERY** | `https://freie-anfrage.de` | **SYNTHETIC** | `https://share.google/OLQ1vkrSTzTWyCwoM` (Focus Online) | **LIVE** | **VERIFIED** |
| **MULTIMEDIA_ANALYSIS** | `https://www.youtube.com/watch?v=hJP5GqnTrNo` | **LIVE** | `https://www.youtube.com/watch?v=5qap5aO4i9A` | **LIVE** | **VERIFIED** |
| **FRESHNESS_CHECK** | `https://techportal.de/trends-2026` | **SYNTHETIC** | `https://share.google/OLQ1vkrSTzTWyCwoM` (Focus Online) | **LIVE** | **VERIFIED** |
| **MISINFORMATION_RADAR** | `https://klimabericht.de/co2-diskussion` | **SYNTHETIC** | `https://share.google/OLQ1vkrSTzTWyCwoM` (Focus Online) | **LIVE** | **VERIFIED** |
| **FACTS_VS_OPINIONS** | `https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/` | **LIVE** | `https://immo-news.de/preisentwicklung` | **SYNTHETIC** | **VERIFIED** |
| **RISK_ANALYSIS** | `https://www.who.int/news-room/fact-sheets/detail/radon-and-health` | **LIVE** | `https://finanz-blog.de/krypto-vorsorge` | **SYNTHETIC** | **VERIFIED** |
| **PERSPECTIVES_COUNTERPOSITIONS** | `https://www.pewresearch.org/2025/04/03/views-of-risks-opportunities-and-regulation-of-ai/` | **LIVE** | `https://debattenportal.de/tempolimit` | **SYNTHETIC** | **VERIFIED** |
| **RELEVANT_ASPECTS** | `https://www.gov.uk/government/publications/agentic-ai-and-consumers/agentic-ai-and-consumers` | **LIVE** | `https://recht-portal.de/homeoffice-gesetz` | **SYNTHETIC** | **VERIFIED** |
| **GOOGLE_MAPS_ANALYZER** | `https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep` | **LIVE** | `https://maps.app.goo.gl/WgXTvya1yCDJjameA` | **LIVE** | **VERIFIED** |
| **GOOGLE_MAPS_LOCATION_CONTEXT** | `https://www.google.com/maps/place/Brandenburger+Tor/` | **LIVE** | `https://maps.app.goo.gl/WgXTvya1yCDJjameA` | **LIVE** | **VERIFIED** |
| **GOOGLE_MAPS_LOCATION_QUERY** | `https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep` | **LIVE** | `https://maps.app.goo.gl/WgXTvya1yCDJjameA` | **LIVE** | **VERIFIED** |
| **DOCUMENT_SUMMARY** | `https://www.gov.uk/government/publications/ai-cyber-threats-open-letter-to-business-leaders` | **LIVE** | `https://cdn.websitebuilder.service.justice.gov.uk/uploads/sites/54/2025/07/AI-paper-PDF.pdf` | **LIVE** | **UNSURE** |
| **PHOTO_SCREENSHOT_ANALYSIS** | `https://www.jpl.nasa.gov/images/pia00123-earth-pacific-ocean/` | **LIVE** | `https://science.nasa.gov/resource/earth-poster-version-d/` | **LIVE** | **UNSURE** |

---

## 2. Platzhalter / Noch nicht entwickelt (Aktuell außerhalb des Quality Gates: 7)

Diese Funktionen sind reine Navigations-/UI-Platzhalter und erzeugen **keine** Testpflicht oder fehlende Testfälle:

| Funktion (ID) | Kategorie | Name | Status |
| :--- | :--- | :--- | :--- |
| **`AI_IMAGE_DETECTOR`** | E (Arbeiten mit Dateien) | Bild mit KI erzeugt? | **OUT_OF_SCOPE** |
| **`INFOGRAPHIC_GENERATOR`** | C (Visualisierung) | Infografik-Generator | **OUT_OF_SCOPE** |
| **`STRUCTURE_VISUALIZER`** | C (Visualisierung) | Struktur-Visualisierer | **OUT_OF_SCOPE** |
| **`IMAGE_IDEA_GENERATOR`** | C (Visualisierung) | Bildideen-Generator | **OUT_OF_SCOPE** |
| **`SOCIAL_MEDIA_GENERATOR`** | D (Inhalte verarbeiten) | Social-Media-Generator | **OUT_OF_SCOPE** |
| **`COMMUNICATION_GENERATOR`** | D (Inhalte verarbeiten) | Kommunikations-Generator | **OUT_OF_SCOPE** |
| **`MULTI_URL_SUMMARY`** | D (Inhalte verarbeiten) | Zusammenfassung aus mehreren URL | **OUT_OF_SCOPE** |

---

## 3. Detailbeschreibung der aktiven Referenzen

### WEB_SUMMARY
*   **Easy:** `https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/` (LIVE)
    *   *Zweck:* Standard Webseiten-Analyse eines strukturierten Reiseberichts mit Text und HTML.
    *   *Erwartung:* Erfolgreiche Zusammenfassung mit strukturierten Kernaussagen und Kontext.
*   **Difficult:** `https://share.google/OLQ1vkrSTzTWyCwoM` (LIVE - Focus Online Sam Altman Panne)
    *   *Zweck:* Realer Android News share.google Redirect auf Focus Online Artikel.
    *   *Erwartung:* Redirect-Auflösung auf www.focus.de, saubere Content-Extraktion und strukturierte Zusammenfassung PASS.

### FREE_SOURCE_QUERY
*   **Easy:** `https://freie-anfrage.de` (SYNTHETIC)
    *   *Zweck:* Synthetischer Golden-Case für freie Q&A-Quellenabfrage.
    *   *Erwartung:* Präzise faktenbasierte Beantwortung der Nutzerfrage aus dem Quellentext.
*   **Difficult:** `https://share.google/OLQ1vkrSTzTWyCwoM` (LIVE - Focus Online Sam Altman Panne)
    *   *Zweck:* Reale freie Quellenfrage über Android-News-Redirect.
    *   *Verbindliche Testfrage:* `Was genau ist laut Artikel bei Google passiert und wie wurde der Fehler erklärt?`
    *   *Erwartung:* share.google Redirect aufgelöst, Frage ausschließlich aus Quellentext beantwortet, funktionaler PASS.

### RISK_ANALYSIS
*   **Easy:** `https://www.who.int/news-room/fact-sheets/detail/radon-and-health` (LIVE)
    *   *Zweck:* Reale WHO-Information mit konkreten Gesundheitsrisiken, Synergie-Effekten beim Rauchen und Folgen für Gebäude.
    *   *Erwartung:* Quelle vollständig extrahiert, konkrete Risiken strukturiert erkannt, Risikoarten und mögliche Folgen sauber getrennt, funktionaler PASS.
*   **Difficult:** `https://finanz-blog.de/krypto-vorsorge` (SYNTHETIC)

### PERSPECTIVES_COUNTERPOSITIONS
*   **Easy:** `https://www.pewresearch.org/2025/04/03/views-of-risks-opportunities-and-regulation-of-ai/` (LIVE)
    *   *Zweck:* Reale Studie mit unterschiedlichen Perspektiven von Öffentlichkeit und KI-Experten auf Chancen, Risiken und Regulierung.
    *   *Erwartung:* Unterschiedliche Positionen klar getrennt, Gemeinsamkeiten und Gegensätze herausgearbeitet, keine künstliche Gleichsetzung, funktionaler PASS.
*   **Difficult:** `https://debattenportal.de/tempolimit` (SYNTHETIC)

### RELEVANT_ASPECTS
*   **Easy:** `https://www.gov.uk/government/publications/agentic-ai-and-consumers/agentic-ai-and-consumers` (LIVE)
    *   *Zweck:* Reale Analyse zu Agentic AI mit Verbraucher-, Rechts-, Unternehmens- und Risikodimensionen.
    *   *Erwartung:* Über Basisinhalt hinaus relevante Dimensionen identifiziert (Recht, Verbraucherschutz, Risiken), funktionaler PASS.
*   **Difficult:** `https://recht-portal.de/homeoffice-gesetz` (SYNTHETIC)

### DOCUMENT_SUMMARY
*   **Easy:** `https://www.gov.uk/government/publications/ai-cyber-threats-open-letter-to-business-leaders` (LIVE)
    *   *Zweck:* Kurzes offizielles Dokument / verlinkte PDF mit überschaubarer Struktur.
    *   *Erwartung:* Dokument/PDF erfolgreich verarbeitet, zentrale Handlungsempfehlungen zusammengefasst, funktionaler PASS.
*   **Difficult:** `https://cdn.websitebuilder.service.justice.gov.uk/uploads/sites/54/2025/07/AI-paper-PDF.pdf` (LIVE)
    *   *Zweck:* Komplexeres mehrseitiges juristisches Diskussionspapier zu AI and the Law.
    *   *Erwartung:* PDF vollständig verarbeitet, zentrale Themen und Argumentationsblöcke korrekt strukturiert, funktionaler PASS.

### PHOTO_SCREENSHOT_ANALYSIS
*   **Easy:** `https://www.jpl.nasa.gov/images/pia00123-earth-pacific-ocean/` (LIVE)
    *   *Zweck:* Einfaches reales NASA-Bild mit eindeutigem Hauptmotiv (Erde/Pazifik).
    *   *Erwartung:* Hauptmotiv Erde/Pazifik korrekt erkannt, keine erfundenen Details, funktionaler PASS.
*   **Difficult:** `https://science.nasa.gov/resource/earth-poster-version-d/` (LIVE)
    *   *Zweck:* Komplexer Poster-/Infografik-Fall mit Bild-, Text- und Layoutinformationen.
    *   *Erwartung:* Visuelle Hauptelemente erkannt, Text-/Grafikbestandteile zusammengeführt, funktionaler PASS.

### MULTIMEDIA_ANALYSIS
*   **Easy:** `https://www.youtube.com/watch?v=hJP5GqnTrNo` (LIVE - YT-01)
    *   *Zweck:* Video mit vollständigem Transkript (Sal Khan TED Talk).
    *   *Erwartung:* Volle Multimedia-Analyse aktiv, keine Degraded-Warnung, Timestamp-Zusammenfassung.
*   **Difficult:** `https://www.youtube.com/watch?v=5qap5aO4i9A` (LIVE - YT-02)
    *   *Zweck:* Video/Stream ohne Transkript (Lofi Girl) als kontrollierter Degraded-Fall.
    *   *Erwartung:* Wechselt auf DEGRADED, Zusammenfassung rein aus Metadaten.

### GOOGLE_MAPS_ANALYZER / LOCATION_CONTEXT / LOCATION_QUERY
*   **Easy:** `https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep` bzw. `https://www.google.com/maps/place/Brandenburger+Tor/` (LIVE)
    *   *Zweck:* Eindeutiger POI mit klaren Ortskoordinaten / weltbekanntes Wahrzeichen.
    *   *Erwartung:* Eindeutige Places-Auflösung ohne Disambiguierungs-Konflikt.
*   **Difficult:** `https://maps.app.goo.gl/WgXTvya1yCDJjameA` (LIVE)
    *   *Zweck:* Kurz-URL mit Plus-Code `QXV3+893`, thailändischer Beschriftung und dezimaler CID-Auflösung.
    *   *Erwartung:* Deterministische Disambiguierung auf `ChIJJ7Q_mWE72jARcEMQAjiMjLw` und vollständige Analyse.

---

## 3. Technische Umsetzung
Die Kotlin-Registry ist implementiert unter:
`/app/src/main/java/com/example/data/diagnostics/TestReferenceRegistry.kt`
