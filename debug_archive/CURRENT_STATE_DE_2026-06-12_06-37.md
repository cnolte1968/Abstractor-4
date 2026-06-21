# CURRENT_STATE: "Abstractor" Android Application (Stand: 2026-06-12)

Dieses Dokument dient als aktueller, vollständiger Projekt- und Entwicklungsstand der Android-Anwendung **Abstractor** nach der erfolgreichen Integration der neuen Perspektiven- & Gegenpositionen-Funktion.

---

## 1. 📋 Aktuelle AnalysisTypes & UI-Funktionsnamen

Die App verfügt über insgesamt **10 Analyse- und Fokustypen** (9 über das Startseiten-Cockpit, 1 über Dateiupload):

| ID / technischer `AnalysisType` | UI-Name der Funktion | UI-Karten-Überschrift (Detailseite) | Kurze Funktions-Beschreibung |
| :--- | :--- | :--- | :--- |
| `STANDARD_WEBSEITE` | Standard-Webseiten zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | Präzise Inhalts-Zusammenfassung aus Text & Grafiken |
| `MULTIMEDIA` | Multimedia-Content zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | Videos, Podcasts und Transkripte analysieren |
| `DOKUMENTE` | Dokumente zusammenfassen | `WICHTIGSTE KERNAUSSAGEN` | Wissenschaftliche Auswertung hochgeladener PDF- und Textdateien |
| `TOP_3_KERNAUSSAGEN` | 3 Kernpunkte | `3 ZENTRALE KERNAUSSAGEN` | Die drei wichtigsten, absolut fluff-freien Hauptthemen |
| `AKTUALITAETS_CHECK` | Prüfe Aktualität | `AKTUALITÄTS-DETAILS (ZWEIDIMENSIONAL)` | Analysiert Release-Zeitpunkt (Dim A) vs. inhaltliche Relevanz (Dim B) |
| `FEHLINFORMATIONS_RADAR` | Erkenne Fehlinformationen | `ZWEIFELHAFTE INFORMATIONEN` | Medienkompetenz-Sweep nach Framing, Clickbait und Logikfehlern |
| `RISIKO_ANALYSE` | Identifiziere Risiken | `SPEZIFISCHE RISIKEN` | Deckt strategische, finanzielle und regulatorische Risiken auf |
| `BUSINESS_INKUBATOR` | Finde Geschäftsidee | `WICHTIGSTE KERNAUSSAGEN` | Leitet profitable SaaS-Ideen und Marktchancen aus dem Text ab |
| **`FACTS_VS_OPINIONS_ANALYZER`** | Fakt oder Meinung!? | `FAKT ODER MEINUNG!?` | Klassifiziert Aussagen feingranular als Fakt `[F]`, Meinung `[M]` etc. |
| **`PERSPECTIVES_AND_COUNTERPOSITIONS`** | Perspektiven- & Gegenpositionen-Finder | `PERSPEKTIVEN & GEGENPOSITIONEN` | (*Neu*) Findet Gegenargumente, alternative Sichten und Kritiken zum Inhalt |

---

## 2. 🤖 Modellkonfiguration

Die gesamte Text- und Inhaltsanalyse wird über zentrale Konstanten in `GeminiModelConfig` gesteuert:

* **Aktuelles Primärmodell (`TEXT_MODEL`)**: `"gemini-2.5-flash"`
  * *Begründung*: Äußerst stabil, geringe Latenzzeit, hervorragende Einhaltung von Response-Schemas.
* **Aktuelles Fallbackmodell (`FALLBACK_MODEL`)**: `"gemini-3.5-flash"`
  * *Begründung*: Neues, mächtiges Modell, welches bei zeitweiligen Überlastungen des Primärmodells (z. B. durch 503-Fehler) als automatische Rückfallebene einspringt.
* **Kommt `gemini-1.5-flash` noch produktiv vor?**
  * **Nein.** Alle operativen Code-Pfade wurden von dem veralteten Modell bereinigt. Es existieren lediglich historische Erwähnungen/Fehlerprotokolle in archivierten Markdown-Dokumenten.

---

## 3. 🔑 API-Key-Ladelogik & Secret-Umgang

Die API-Schlüssel-Ermittlung erfolgt dynamisch und fehlertolerant über die Methode `getApiKey()` in `GeminiNetwork.kt`:

1. **Gelesene Secret-Namen**:
   * `GEMINI_API_KEY`
   * `Gemini_Abstractor`
2. **Reihenfolge der Schlüssel-Abfrage**:
   * Zuerst Umgebungsvariablen über `System.getenv("GEMINI_API_KEY")`
   * Danach `System.getenv("Gemini_Abstractor")`
   * Anschließend BuildConfig-Felder über `BuildConfig.GEMINI_API_KEY`
   * Zuletzt `BuildConfig.Gemini_Abstractor`
3. **Filterung & Priorisierung**:
   * Ein gefundener Key wird bevorzugt verwendet, wenn er mit `"AIzaSy"` beginnt (Google Key-Präfix).
   * Sollte kein Key dieses Präfix besitzen, wird der erste nicht-leere String gewählt, der sich von den Platzhaltern `MY_GEMINI_KEY` und `MY_GEMINI_API_KEY` unterscheidet.
4. **Verwenden `GEMINI_API_KEY` und `Gemini_Abstractor` denselben Key?**
   * Laut System-Diagnosen im Environment ist `GEMINI_API_KEY` ein authentischer Google-API-Key (Länge: 39 Zeichen, z. b. `AIzaSy...8ihg`).
   * `Gemini_Abstractor` hält eine alternative, längere Entwickler-Zertifizierung/Token (53 Zeichen, z. B. `AQ.Ab8...NqjA`). Beide Werte bleiben strikt getrennt und werden in dieser Reihenfolge sicher evaluiert.

---

## 4. 📂 Grounding- & Schema-Status je Funktion

Aufgrund von REST-API-Restriktionen (Search Grounding und JSON Response-Schema schließen sich bei Google-API-Requests gegenseitig aus) steuert die App das Verhalten dynamisch über den Zustand `activeGrounding`:

| Funktion / Typ | Search-Grounding-Status | responseSchema / responseMimeType Status |
| :--- | :--- | :--- |
| `AKTUALITAETS_CHECK` | **Immer aktiv (true)** | Keine Schema-Enforcement (`null`), da Grounding aktiv |
| `FEHLINFORMATIONS_RADAR` | **Immer aktiv (true)** | Keine Schema-Enforcement (`null`), da Grounding aktiv |
| `TOP_3_KERNAUSSAGEN` | Standardmäßig inaktiv; wechselt auf `true` bei Scraping-Ausfall | `application/json` (erzwingt `AbstractorSummary` Schema) |
| *Alle anderen Typen* | Standardmäßig inaktiv (`useSearchGrounding = false`) | `application/json` (erzwingt `AbstractorSummary` Schema) |

---

## 5. ⚙️ Parsing- & Cleaning-Sonderregeln

Beim Empfang und Einlesen der JSON-Daten werden modusspezifische Bereinigungen durchgeführt:
* **`FACTS_VS_OPINIONS_ANALYZER`**: 
  * Nutzt `keepNumbering = true`, damit die strukturierten Format-Kürzel wie `[F]`, `[M]` oder `[V]` an den Kernaussagen unter keinen Umständen vom Regex-Sanitizer de-nummeriert oder weggeschnitten werden.
  * Verwendet die maßgeschneiderte `cleanFactsVsOpinionsTakeaway()` Filterlogik.
* **`PERSPECTIVES_AND_COUNTERPOSITIONS`**:
  * Setzt ebenfalls `keepNumbering = true`, um strukturierte oder nummerierte Argumentationspfade sachlich und ungekürzt im Originalwortlaut inklusive Quellenlink-Strukturen wiedergeben zu können.
* **`parseSummaryRobustly()`**:
  * Ein hochperformanter Regex-Fallback-Parser fängt unvollständiges JSON, Backslash-Escapes oder störende Markdown-Zusatztexte der Gemini API vollautomatisch ab.

---

## 6. ⚠️ Bekannte Risiken & Build-Status

* **Risikobewertung**: Keine unmittelbaren Risiken. Der Code ist sauber typisiert, besitzt lückenlose Kompatibilität für alle AnalysisTypes und löst nicht auswertbare Inhalte (Länge < 500 Zeichen) über das VM-Fehlerhandling sauber auf, um Halluzinationen zu verhindern.
* **Klare Build-Empfehlung**: **GRÜN (GREEN)**
  * Der Applet-Build wurde lokal erfolgreich verifiziert. Das Projekt kompiliert fehlerfrei.
