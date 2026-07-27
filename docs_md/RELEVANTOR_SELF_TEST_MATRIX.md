# RELEVANTOR SELF-TEST MATRIX

Diese Testmatrix dokumentiert den automatisierten Teststatus aller im `FeatureCatalog` deklarierten Funktionen. 

## Definition der Qualitätsstufen (Status)

*   **GRÜN**: Die Funktion ist im `FeatureCatalog` aktiv, im `AnalysisRegistry` korrekt registriert, verfügt über ein vorhandenes Prompt-Asset, vollständige Golden-Path-Test-Fixtures (Input, Gemini Response, Expected Output) und läuft erfolgreich im automatisierten Pipeline-Akzeptanztest durch.
*   **GELB**: Die Funktion ist im Code aktiv und registriert, aber es fehlen noch die Golden-Path-Test-Fixtures oder der automatisierte Pipeline-Test.
*   **ROT**: Die Funktion ist inaktiv, ein reiner Platzhalter oder nicht funktionstüchtig.

---

## Status-Tabelle

| Funktion | Kategorie | AnalysisType | Prompt-Datei | Golden Input vorhanden | Fake Gemini Response vorhanden | Expected Output vorhanden | Full Pipeline Test vorhanden | Contract Test vorhanden | History Test vorhanden | UI/Menu Test vorhanden | Status |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **A.1** Zusammenfassung | Verstehen & Verdichten (A) | `STANDARD_WEBSEITE` | `F_STANDARD_WEBSEITE.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **A.2** 3 Kernaussagen | Verstehen & Verdichten (A) | `TOP_3_KERNAUSSAGEN` | `F_TOP_3_KERNAUSSAGEN.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **A.3** Frage an die Quelle | Verstehen & Verdichten (A) | `FREIE_QUELLENANFRAGE` | `F_FREIE_QUELLENANFRAGE.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **A.4** Video- & Multimedia-Analyse | Verstehen & Verdichten (A) | `MULTIMEDIA` | `F_MULTIMEDIA.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.1** Aktualitäts-Check | Qualität, Kritik & Einordnung (B) | `AKTUALITAETS_CHECK` | `F_AKTUALITAETS_CHECK.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.2** Fehlinformations-Radar | Qualität, Kritik & Einordnung (B) | `FEHLINFORMATIONS_RADAR` | `F_FEHLINFORMATIONS_RADAR.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.3** Fakt-oder-Meinung | Qualität, Kritik & Einordnung (B) | `FACTS_VS_OPINIONS_ANALYZER` | `F_FACTS_VS_OPINIONS_ANALYZER.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.4** Risikoanalyse | Qualität, Kritik & Einordnung (B) | `RISIKO_ANALYSE` | `F_RISIKO_ANALYSE.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.5** Perspektiven- & Gegenpositionen-Finder | Qualität, Kritik & Einordnung (B) | `PERSPECTIVES_AND_COUNTERPOSITIONS` | `F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **B.6** Weitere relevante Aspekte | Qualität, Kritik & Einordnung (B) | `WEITERE_RELEVANTE_ASPEKTE` | `F_WEITERE_RELEVANTE_ASPEKTE.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **E.1** Dokument zusammenfassen | Arbeiten mit Dateien (E) | `DOKUMENTE` | `F_DOKUMENTE.md` | Ja | Ja | Ja | Ja | Ja | Ja | Ja | **GRÜN** |
| **C.1** Infografik-Generator | Visualisierung (C) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **C.2** Struktur-Visualisierer | Visualisierung (C) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **C.3** Bildideen-Generator | Visualisierung (C) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **D.1** Social-Media-Generator | Inhalte verarbeiten (D) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **D.2** Kommunikations-Generator | Inhalte verarbeiten (D) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **D.3** Zusammenfassung aus mehreren URL | Inhalte verarbeiten (D) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **E.2** Foto & Screenshots auswerten | Arbeiten mit Dateien (E) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |
| **E.3** Bild mit KI erzeugt? | Arbeiten mit Dateien (E) | *Keiner (Placeholder)* | *Keiner* | Nein | Nein | Nein | Nein | Nein | Nein | Nein | **ROT** (Inaktiv) |

---

## Dokumentierte Lücken (Gaps)

Alle aktiven Analyse-Funktionen (**A.1, A.2, A.3, A.4, B.1, B.2, B.3, B.4, B.5, B.6, E.1**) wurden vollständig gehärtet:
1. **Golden-Path-Assets**: Alle erforderlichen Testdaten unter `app/src/test/assets/golden/<Fid>/` (z.B. `input.html`, `gemini_response.json`, `expected_domain_summary.json` etc.) sind vorhanden.
2. **Pipelines**: Vollständig integrierte Akzeptanz- und Contract-Tests im Harness `RelevantorSelfTestHarnessTest.kt` laufen erfolgreich durch.

*Hinweis: Inaktive Funktionen (ROT) verbleiben als Platzhalter, bis sie implementiert werden.*
