# ABSTRACTOR FUNCTION EXECUTION MODEL (FUNCTION COGNITIVE ARCHITECTURE)

## 1. ZWECK DIESES DOKUMENTS
Dieses Dokument definiert das herrschende und verbindliche **Function Execution Model** für alle 10 Analysefunktionen des Abstractors. Es ergänzt die globale Prompt-Basis (`_global_quality_rules.md`) und das Funktionsprompt-Template (`_function_prompt_template.md`).

Dieses Modell stellt sicher, dass jede einzelne Funktion eine präzise fachliche Seele und Kognitionsstruktur besitzt:
* **Eingangsdaten (Input Interpretation):** Was wird wie gefiltert?
* **Denkprozess (Cognitive Processing):** Wie arbeitet die jeweilige Experten-Rolle?
* **Ausgabeformatierung (Output Construction):** Wie werden die JSON-Daten gefüllt?
* **Qualitätskontrolle (Quality Guards):** Welche typspezifischen Fehler werden abgewehrt?

---

## 2. ZUSAMMENHANG IN DER SYSTEMARCHITEKTUR
Das Zusammenwirken der einzelnen Komponenten erfolgt nach diesem Schichtenmodell:

```
+-------------------------------------------------------------+
|               GLOBAL QUALITY RULES (Datei A)                |
|           (_global_quality_rules.md - System-Vorsatz)        |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|             FUNCTION EXECUTION MODEL (Dieses Doc)           |
|         (Kognitiver Leitfaden & Entwurfslandkarte)          |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|              FUNKTIONSSPEZIFISCHER PROMPT (Datei B)          |
|          (F_*.md - Konkrete Ausgestaltung per Funktion)     |
+-------------------------------------------------------------+
```

---

## 3. DAS 4-LAYER COGNITIVE MODEL
Jede Funktion durchläuft in ihrer internen Strukturierung vier Phasen:

1. **INPUT_INTERPRETATION_LAYER:** Bestimmt die Art der Quelle (Text, Transkript, HTML-Rumpf). Filtert Rauschen (z.B. Cookie-Banner, Systemmeldungen) heraus, identifiziert Sprecherrollen und zeitliche Bezüge der Quelle.
2. **COGNITIVE_PROCESSING_LAYER:** Legt fest, welche geistigen Operationen (Synthese, Dekonstruktion, zeitlicher Abgleich, Risikogewichtung, kreativer Transfer, Kontrastierung) das Large Language Model (LLM) durchführen soll.
3. **OUTPUT_CONSTRUCTION_LAYER:** Übersetzt die interne Repräsentation in die exakten JSON-Felder (`title`, `original_url`, `short_description`, `key_takeaways`, `owner`) und stellt sicher, dass Aufbau und Inhalt perfekt zur Android Jetpack Compose UI kompatibel sind.
4. **QUALITY_GUARDS:** Weist explizite Fehlerklassen ab (z. B. generische Begriffseinleitungen, Halluzination von Quellen, redundante Detailwiedergabe).

---

## 4. COGNITIVE SPECIFICATIONS FOR THE 10 FUNCTIONS

### 4.1 STANDARD_WEBSEITE (Allgemeine Webanalyse)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Strukturierter oder unstrukturierter Textkörper einer klassischen HTML-Seite (bereinigt von Navigation und Werbung).
  * **Fokus:** Identifikation des Hauptgegenstands, der Kernthese der Autoren und des primären Informationsziels der Webseite.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Leitartikler / Executive Assistant.
  * **Methode:** Synthese der Kernaussagen. Komprimiere weitschweifige Absätze auf ihre tragenden informationellen Säulen. Extrahiere die Intention des Urhebers.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** 1-2 dichte Sätze, die den thematischen Rahmen und den Nutzen der Seite beschreiben.
  * **Key Takeaways:** 3-5 hochinformative Punkte im Muster `**[Konkretes Schlagwort]:** [Präziser Erläuterungssatz]`.
  * **Nummerierung:** Keine Nummerierung (flache Material-Design Kartendarstellung).
* **QUALITY_GUARDS:**
  * ❌ Keine generischen Einleitungen wie „Inhalt“, „Erkenntnis“, „Zusammenfassung“.
  * ❌ Keine Meta-Statements über die Funktion.
  * ❌ Keine führenden Hochkommas oder Bulletzeichen innerhalb der JSON-Werte.

---

### 4.2 MULTIMEDIA (Video-, Podcast- und Audioanalysen)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Meist maschinell erzeugte Video- oder Podcast-Transkripte (oft ohne Interpunktion, mit Sprecherzuordnungen wie `Speaker 1:`).
  * **Fokus:** Identifikation der Haupt-Protagonisten, deren expliziter Sprechabsichten und die chronologische Strukturierung längerer Audio-/Videobeiträge.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Professioneller Medien-Analyst & Protokollführer.
  * **Methode:** Narrative Strukturrekonstruktion. Trenne spontane Plauderei, Einleitungs-Floskeln und Sponsoring-Einblendungen von den echten sachlichen Thesen. Rekonstruiere den inhaltlichen Faden auch bei ungeordneter Sprache.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Beschreibt das Kern-Thema der Folge, den Host/Sprecher und die übergeordnete Diskussions-Atmosphäre.
  * **Key Takeaways:** 3-5 prägnante inhaltliche Blöcke, die thematisch getrennte Abschnitte des Beitrags zusammenfassen. Beginnend mit `**[Thematisches Segment]:** [Fließtext]`.
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Werbe- oder Sponsoringblöcke (z. B. „VPN-Anbieter XY“) dürfen unter keinen Umständen in die Takeaways einfließen.
  * ❌ Keine Übernahme von unstrukturiertem "Sprechdurchfall" (Füllwörter, Halbsätze). Der Output muss grammatikalisch perfekt und fließend sein.

---

### 4.3 DOKUMENTE (PDFs, wissenschaftliche Paper, Berichte)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Strukturierte, seitenbasierte PDF-Extrakte, behördliche Berichte, Whitepaper, wissenschaftliche Dokumente.
  * **Fokus:** Identifikation der Methodik, der Datenbasis, zentraler Thesen und der formulierten Schlussfolgerungen.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Wissenschaftlicher Analyst & Gutachter.
  * **Methode:** Strukturierte Text-Dekonstruktion. Filter die methodische Vorgehensweise, empirische Befunde und finale Handlungsempfehlungen. Ignoriere rein akademische Abgrenzungen, fokussiere den anwendungsbereiten Kern.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Benennt präzise das Forschungs- oder Berichtsziel sowie die Primärtheorie bzw. den Herausgebungszweck.
  * **Key Takeaways:** 4-5 tiefschürfende Punkte. Inhaltliche Schwerpunkte: (i) Forschungsfrage/Anlass, (ii) Methodik/Datenbasis, (iii) Hauptbefund, (iv) Empfehlungen.
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Niemals das Wort „Dimension“ oder „Kapitel 1“ am Zeilenanfang verwenden.
  * ❌ Keine Erfindung von statistischen Werten. Prozentzahlen oder Stichprobengrößen nur nennen, wenn sie absolut exakt im Grounding-Text verankert sind.

---

### 4.4 TOP_3_KERNAUSSAGEN (Radikale Essenz)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Jeglicher Text oder Quellcode einer Seite/Dokument.
  * **Fokus:** Die drei absolut signifikantesten und weitreichendesten Hauptpunkte des Inhalts.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Chefredakteur / Executive Decision Maker.
  * **Methode:** Radikale Priorisierung. Bewerte alle enthaltenen Aussagen nach ihrer Tragweite und sortiere die Top 3 heraus. Ignoriere alles Sekundäre, Detailerklärungen und Beispiele.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Ultrakurzer Satz zur Relevanz der Top 3.
  * **Key Takeaways:** Exakt 3 hochverdichtete Takeaways im Muster: `**[Kernaussage-Schlagwort]:** [Detaillierte, in sich geschlossene logische Erläuterung]`.
  * **Nummerierung:** Keine manuelle Nummerierung im Code generieren (wird vom UI erzwungen!).
* **QUALITY_GUARDS:**
  * ❌ Es dürfen nicht mehr und nicht weniger als exakt 3 Takeaways im JSON-Array enthalten sein.
  * ❌ Die Schlüsselwörter dürfen keine generischen Phrasen wie „Erste Kernaussage“, „Takeaway 1“ oder „Thema A“ sein, sondern müssen die inhaltliche Essenz konkret benennen.

---

### 4.5 AKTUALITAETS_CHECK (Zeitliche Validierung)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Beliebige Textquellen, oft Nachrichtenartikel, rechtliche Vorgaben oder Branchenberichte.
  * **Fokus:** Explizite Datumsangaben in der Quelle, zeitkritische Faktenlage (z.B. angekündigte Gesetze, ablaufende Fristen, historische Referenzen).
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Zeitkritischer Informations-Revisor & Zeithistoriker.
  * **Methode:** Zeitliche Diskrepanzanalyse. Vergleiche das Erstellungsdatum des Artikels bzw. den Stand der Fakten mit der aktuellen Realität (UTC-Bezugspunkt). Identifiziere, ob Behauptungen überholt, übertragbar oder hinfällig sind.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Bewertet die zeitliche Relevanz im Jetzt-Zustand (aktuell, überholt, historisch informativ).
  * **Key Takeaways:** 3-4 Punkte im Schema `**[Zeitlicher Faktenaspekt]:** [Erläuterung der aktuellen Gültigkeit und etwaiger Änderungen]`.
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Jedes Takeaway MUSS eine konkrete zeitliche, inhaltliche Aussage zur Gültigkeit treffen.
  * ❌ Verbote für generische Einleitungen wie „Gültigkeit“, „Aktualität“, „Inhaltliche Gültigkeit“ oder „Zeitbezug“.

---

### 4.6 FEHLINFORMATIONS_RADAR (Faktencheck & Bias-Identifikation)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Meinungslastige Texte, polemische Artikel, ungeprüfte Web-Behauptungen oder virale Transkripte.
  * **Fokus:** Reißerische Adjektive, logische Fehlschlüsse, unbewiesene Behauptungen, einseitiges Framing und manipulative Sprachtricks.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Unbestechlicher Faktenchecker & kognitiver Analyst.
  * **Methode:** Logische Fehleranalyse & Manipulationstracking. Dekonstruiere Schein-Argumente (wie z.B. Strohmann-Argumente, ad-hominem Angriffe, falsche Kausalitäten). Zeige sachlich auf, wo die Quelle Behauptungen als Fakt darstellt, obwohl Belege fehlen.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Gibt an, wie stark die Quelle manipulativ, biased oder fehlerbehaftet formuliert ist (Neutralitäts-Score-Einordnung).
  * **Key Takeaways:** 3-5 hochpräzise Analysen. Jedes Takeaway benennt die konkrete Manipulation oder Ungenauigkeit: `**[Identifizierter Bias/Fehlschluss]:** [Präzise Aufdeckung, warum und wo diese Argumentation fehlerhaft ist]`.
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Keine Einwort-Takeaways.
  * ❌ Keine generischen Phrasen wie „Erkenntnis“, „Keyword“, „Begründung“, „Subjektivität“.
  * ❌ Keine polemische Sprache im Output. Die Analyse muss absolut sachlich, distanziert und unparteiisch formuliert sein.

---

### 4.7 RISIKO_ANALYSE (Szenariotechnische Bewertung)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Projektberichte, Wirtschafts-News, politische Entwicklungen oder Technologie-Whitepaper.
  * **Fokus:** Risikofaktoren, potenzielle Engpässe, versteckte Kosten, systemische Abhängigkeiten und negative Folgeeffekte.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Erfahrener Risiko-Manager & Krisen-Koordinator.
  * **Methode:** Systemische Bedrohungsanalyse. Identifiziere direkte und indirekte (Zweit- und Drittrundeneffekte) Risiken. Gewichte diese gedanklich nach Eintrittswahrscheinlichkeit und Schadensausmaß.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Das übergeordnete Bedrohungsszenario und die generelle Verwundbarkeit des verhandelten Systems.
  * **Key Takeaways:** 3-4 strukturierte Risikobewertungen. Inhaltliches Schema: `**[Konkreter Risikovektor]:** [Präziser Wirkungszusammenhang und Bedrohungsszenario]`.
  * **Nummerierung:** Keine manuelle Nummerierung im Code generieren (UI-Rendering-Konfig!).
* **QUALITY_GUARDS:**
  * ❌ Die Risiken dürfen nicht oberflächlich sein (z.B. nicht bloß *"Finanzrisiko: Es könnte teurer werden"* sondern *"Wechselkursinduziertes Finanzierungsrisiko: Da die Importgüter vorrangig in USD..."*).
  * ❌ Keine Labels wie „Risiko 1“, „Gefahr 1“ oder „Eintritt“.

---

### 4.8 BUSINESS_INKUBATOR (Innovations- & Gründungsideen)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Beliebige fachliche Abhandlungen, ökonomische Publikationen oder gesellschaftliche Trends.
  * **Fokus:** Identifizierte Probleme, ungelöste Pain-Points der Konsumenten, neue Gesetzgebungen oder disruptive Technologiedurchbrüche.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Venture Builder & kreativer Startup-Inkubator-Coach.
  * **Methode:** Kreativer Transfer & Value-Proposition Design. Übersetze die Probleme der Quelle in konkrete, marktfähige Geschäftsmodelle. Entwickle neue Value Propositions (Dienstleistungen, Softwarelösungen, dichte Nischenprodukte).
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Eine Zusammenfassung der Marktchancen bzw. des ökonomischen Handlungsspielraums, den diese Quelle offenbart.
  * **Key Takeaways:** 3-4 konkrete, unterschiedliche Gründungsideen im Format: `**[Startup-Modell-Name]:** [Beschreibung der Zielgruppe, des gelösten Pain Points und des Monetarisierungskonzepts]`.
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Die Key Takeaways dürfen unter keinen Umständen eine Zusammenfassung des Artikels sein! Sie müssen visionäre, anwendbare Geschäftsideen sein, die auf der Quelle basieren.
  * ❌ Keine unvollständigen Konzepte (z.B. bloß *"Eine App für Steuern"*). Jede Idee muss ein klares, separates Produkt- oder Dienstleistungskonzept skizzieren (i) Zielgruppe, (ii) USP, (iii) Monetarisierung.

---

### 4.9 FACTS_VS_OPINIONS_ANALYZER (Epistemische Trennung)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Gemischte Textgattungen (Leitartikel, Studienberichte, Blogposts, Debattenbeiträge).
  * **Fokus:** Epistemische Qualifikatoren (z. B. *"wissenschaftlich bewiesen"* vs. *"ich glaube"*, *"viele meinen"*). Empirische Belege versus emotionale Bewertungen.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Kritischer Wissenschaftsjournalist & Epistemologe.
  * **Methode:** Beleg- und Überzeugungs-Klassifikation. Isoliere empirisch fundierte Fakten von subjektiven Meinungsäußerungen, Framings oder Interpretationen des Autors.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Das prozentuale Verhältnis (neutral schätzen) zwischen objektiven Daten und meinungsstarken Interpretationslinien der Quelle.
  * **Key Takeaways:** 4-5 analysierte Thesen. Jede Zeile MUSS zwingend mit einem eindeutigen epistemischen Präfix eingeleitet werden:
    * `**[Fakt]:**` oder `**[Meinung]:**` gefolgt von der präzisen Einordnung der Aussage.
    * *Beispiel:* `**[Fakt] Überprüfbare Absatzzahlen:** Der Autobauer verzeichnete im Kernquartal einen realen Umsatzrückgang von 12 Prozent.`
    * *Beispiel:* `**[Meinung] Unbewiesene Marktdominanz-These:** Der Autor behauptet, dass traditionelle Antriebe innerhalb von drei Jahren vollständig vom Weltmarkt verschwinden werden.`
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Absolute Pflicht zur Verwendung von `**[Fakt] ...**` bzw. `**[Meinung] ...**` im Prefix!
  * ❌ Keine falsche Eingruppierung emotionaler Thesen unter das Prädikat "Fakt", nur weil der Verfasser der Quelle eine autoritative Haltung einnimmt.

---

### 4.10 PERSPECTIVES_AND_COUNTERPOSITIONS (Diskursive Reife)

* **INPUT_INTERPRETATION_LAYER:**
  * **Eingabe:** Argumentative Schriften, politische Dossiers, ideologische Publikationen oder thesenstarke Artikel.
  * **Fokus:** Vorgebrachte Hauptthesen der Quelle und die immanente Argumentationsstrategie.
* **COGNITIVE_PROCESSING_LAYER:**
  * **Denkweise:** Philosophischer Debattenleiter & neutraler Schiedsrichter.
  * **Methode:** Dialektische Kontrastierung. Erschließe die Hauptthesen der Vorlage. Konstruiere oder herleite fundierte, etablierte Gegenpositionen, um eine pluralistische und ausgewogene Meinungsbildung zu ermöglichen.
* **OUTPUT_CONSTRUCTION_LAYER:**
  * **Short Description:** Einordnung des behandelten Diskurses und Benennung der primären Kontroverse.
  * **Key Takeaways:** 3-4 dialektisch kontrastierte Blöcke im präzisen Muster: `**[Kontroverses Thema]:** Die Quelle behauptet [A], etablierte Gegenpositionen verweisen jedoch auf [B] oder betonen [C].`
  * **Nummerierung:** Keine Nummerierung.
* **QUALITY_GUARDS:**
  * ❌ Es dürfen keine beliebigen, losen Perspektivensammlungen entstehen. Jeder Punkt muss hart mit der These der Quelle kontraktiert sein (These vs. Antithese/Gegenperspektive).
  * ❌ Keine Generierung von Schein-Konsens (z.B. *"Beide Seiten haben recht"*). Die echten Kontroversen und Bruchlinien müssen messerscharf herausgearbeitet werden.

---

## 5. OPTIMIERUNGS-LANDKARTE (AM BEISPIEL F_STANDARD_WEBSEITE.md)
Dieses Kognitionsmodell dient im nächsten Schritt (Arbeitsauftrag 31F) als Master-Schablone, um die 10 bestehenden Promptdateien `F_*.md` Schritt für Schritt strukturell auf den exakt gleichen Qualitätsstandard zu heben.

### Ablaufplan zur Prompt-Überarbeitung:
1. **Lade** die bestehende Datei (z. B. `F_STANDARD_WEBSEITE.md`).
2. **Kopiere** die Struktur aus `_function_prompt_template.md`.
3. **Ergänze** die fachspezifischen Inhalte aus diesem Execution Model in die Platzhalter-Bereiche.
4. **Validierte** den Output-Vertrag und die UI-Kompatibilität.
5. **Ersetze** die alte Promptdatei vollständig mit dem neuen, strukturierten und hochfunktionalen Inhalt.
