# Project Context: Abstractor

## 1. Projektziel & Zielgruppe

**Abstractor** ist eine hochgradig spezialisierte Sicherheits- und Informations-Analyse-App für Android. Sie richtet sich an anspruchsvolle Wissensarbeiter, Analysten, Unternehmer und Entscheider, die täglich mit einer massiven Informationsflut konfrontiert sind.

Anstatt klassische, inhaltsleere und generische Textzusammenfassungen zu erzeugen, seziert die App digitale Inhalte (Webseiten, YouTube-Videos, hochgeladene PDFs, Office-Dokumente und Bilder) mit chirurgischer Präzision auf Substanz, Glaubwürdigkeit und strategischen Nutzen.

---

## 2. Kernnutzen für den Anwender

* **Rigoroser Rauschfilter:** Banalitäten, Marketingfloskeln, werbliche Übertreibungen und irrelevantes Füllmaterial werden systematisch ausgefiltert.
* **Expliziter Substanz-Fokus:** Konzentration auf harte Fakten, wissenschaftliche Daten, statistische Werte, tragende Argumente und unkonventionelle Perspektiven.
* **Erkennung blinder Flecken:** Integrierter Schredder für logische Fehlschlüsse, manipulative Rhetorik, Clickbait sowie einseitige Quellenberichterstattung.
* **Unternehmerischer Hebel:** Direkte Ableitung von echten, marktfähigen Nischen-Geschäftsmodellen, Wertversprechen und strategischen Handlungspfaden.
* **Executive-Ready Briefings:** Bereitstellung von hochgradig strukturierten und visuell scannbaren Analysen, die sofort geteilt, exportiert oder archiviert werden können.

---

## 3. Übersicht der 10 Analysefunktionen

Das System bietet über sein modulares Cockpit genau zehn eigenständige Analysemodi (`AnalysisType`):

| Analysefunktion | System-Enum (`AnalysisType`) | Hauptziel & Mehrwert | Grounding (Google Search) |
| :--- | :--- | :--- | :--- |
| **Standard Webseite** | `STANDARD_WEBSEITE` | Substantielle, ungeschönte Zusammenfassung von Webinhalten mit prägnanten Stichpunkten. | Optional (User-gesteuert) |
| **Multimedia** | `MULTIMEDIA` | Video-/Audio-Transkripte analysieren. Filtert Sponsorenblöcke, Intros und Füllsel heraus. | Optional (User-gesteuert) |
| **Dokumente** | `DOKUMENTE` | Detailgetreue Analyse von lokal hochgeladenen Dokumenten (PDFs, TXT) und Bildern. | Deaktiviert (Lokaler Fokus) |
| **3 Kernpunkte** | `TOP_3_KERNAUSSAGEN` | Schnelles Screening zur Relevanzprüfung. Präzise 3 Stichpunkte mit jeweils genau einem Satz. | Optional (User-gesteuert) |
| **Aktualitätsprüfung** | `AKTUALITAETS_CHECK` | Analysiert unabhängig das Alter der Quelle (Dimension A) und die inhaltliche Aktualität (Dimension B). | **Erzwungen Generisch (Immer Aktiv)** |
| **Fehlinformationsradar** | `FEHLINFORMATIONS_RADAR` | Aufdeckung von voreingenommenen Darstellungen, unbelegten Thesen und rhetorischen Verzerrungen. | **Erzwungen Generisch (Immer Aktiv)** |
| **Risikoanalyse** | `RISIKO_ANALYSE` | Systemischer Risikokatalog zur Identifikation versteckter Gefahren und finanzieller/rechtlicher Hürden. | Optional (User-gesteuert) |
| **Business Inkubator** | `BUSINESS_INKUBATOR` | Generierung von bis zu drei tragfähigen, unkonventionellen Geschäftsideen (SaaS/Nische) aus der Quelle. | Optional (User-gesteuert) |
| **Fakt vs. Meinung** | `FACTS_VS_OPINIONS_ANALYZER` | Streng neutrale Zuordnung von Aussagen in Fakten `[F]`, Meinungen `[M]`, Vermutungen `[V]`, etc. | Optional (User-gesteuert) |
| **Perspektivenanalyse** | `PERSPECTIVES_AND_COUNTERPOSITIONS` | Aufbrechen von Confirmation Bias durch Aufzeigen fundierter, valider Gegen- und Alternativthesen. | Optional (User-gesteuert) |

---

## 4. UI/UX & Interaktions-Design

Die App verwendet ein fokussiertes, barrierefreies Ein-Screen-Cockpit, das auf Material Design 3 (M3) aufbaut:
* **Dynamische Farbanpassung:** Nutzung des M3-Farbsystems für ein konsistentes, blendfreies Gesamtbild.
* **Scanbarkeit:** Ergebnisse zeichnen sich durch strukturierte Stichpunkte mit stets **fettgedruckten Leitbegriffen** aus, die das schnelle Überfliegen erleichtern.
* **Direkt-Aktionen:** Integrierte Schnellaktionen zum Kopieren, Teilen (Share-Intent) und Neuladen der Analysen sowie ein flexibler Dateipicker für lokale Dokumente.

---

## 5. Stand der Prompt-Externalisierung

Die Prompt-Architektur von Abstractor wurde in ein hochgradig modulares und anpassbares asset-basiertes System überführt:
* **Produktions-Manifest:** Die Datei `app/src/main/assets/prompts/prompt_manifest.json` dient als zentraler Router und ordnet jedem `AnalysisType` seine jeweilige Prompt-Datei zu.
* **Externalisierte Prompts:** Alle Systemanweisungen und Qualitätsprofile sind als eigenständige Markdown-Dateien (`F_*.md`) organisiert.
* **Globale Qualitätsschranke:** In `_global_quality_rules.md` sind übergeordnete Qualitätsmaßstäbe (z. B. Umfangsadaption, Verbot von verschachtelten Bulletpoints) hinterlegt, die systemweit geladen und injiziert werden.
* **Laufzeit-Aktivierung:** Der `PromptLoader` liest die Konfiguration vollständig dynamisch aus den Assets. Hardcodete Fallbacks im Kotlin-Code dienen nur noch als robustes Sicherheitsnetz für den unwahrscheinlichen Fall von Asset-Lesefehlern.

---

## 6. Verzeichnisstruktur der produktiven Assets

Sämtliche produktiven Steuerungs- und Sprachkompetenzen liegen sauber getrennt im Asset-Ordner:

```
app/src/main/assets/prompts/
├── prompt_manifest.json                       # Produktive Root-Zuweisungstabelle
├── _global_quality_rules.md                   # Systemweite Qualitätsmaßstäbe und Formatverbote
├── F_STANDARD_WEBSEITE.md                     # Prompt für Standard Webseiten
├── F_MULTIMEDIA.md                            # Prompt für Video- und Podcast-Transkripte
├── F_DOKUMENTE.md                             # Prompt für PDF- und Textdateien
├── F_TOP_3_KERNAUSSAGEN.md                    # Prompt für die 3-Kernpunkte-Verdichtung
├── F_AKTUALITAETS_CHECK.md                    # Prompt für Aktualitäts- und Relevanzanalysen
├── F_FEHLINFORMATIONS_RADAR.md                # Prompt für Faktentreue und Rhetorikprüfung
├── F_RISIKO_ANALYSE.md                        # Prompt für systematische Risikoaufdeckungen
├── F_BUSINESS_INKUBATOR.md                    # Prompt für das Ableiten von Innovations-Modellen
├── F_FACTS_VS_OPINIONS_ANALYZER.md            # Prompt für die neutrale Klassifizierungs-Engine
└── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md     # Prompt für das Hinzufügen von Gegenpositionen
```
