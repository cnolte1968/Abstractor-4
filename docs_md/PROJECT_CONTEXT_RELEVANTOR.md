# Project Context: Relevantor

**Stand:** 2026-08-09 17:20:40 ICT  
**Status:** **100% KOMPILIERT, PROJEKTPFADE BEREINIGT & BACKEND FOUNDATION (MVP 1C) VERIFIZIERT**  

---

## 1. Projektziel & Zielgruppe

**Relevantor** ist eine hochgradig spezialisierte Sicherheits- und Informations-Analyse-App für Android. Sie richtet sich an anspruchsvolle Wissensarbeiter, Analysten, Unternehmer und Entscheider, die täglich mit einer massiven Informationsflut konfrontiert sind.

Anstatt klassische, inhaltsleere und generische Textzusammenfassungen zu erzeugen, seziert die App digitale Inhalte (Webseiten, YouTube-Videos, hochgeladene PDFs, Office-Dokumente und Bilder) mit chirurgischer Präzision auf Substanz, Glaubwürdigkeit und strategischen Nutzen.

---

## 2. Kernnutzen für den Anwender

* **Rigoroser Rauschfilter:** Banalitäten, Marketingfloskeln, werbliche Übertreibungen und irrelevantes Füllmaterial werden systematisch ausgefiltert.
* **Expliziter Substanz-Fokus:** Konzentration auf harte Fakten, wissenschaftliche Daten, strategische Kernargumente und Erkenntnisse ohne Phrasen.
* **Erkennung blinder Flecken:** Integrierter Schredder für logische Fehlschlüsse, manipulative Rhetorik, Clickbait sowie einseitige Quellenberichterstattung.
* **Unternehmerischer Hebel:** Direkte Ableitung von echten, marktfähigen Nischen-Geschäftsmodellen, Wertversprechen und strategischen Handlungspfaden.
* **Executive-Ready Briefings:** Bereitstellung von hochgradig strukturierten und visuell scannbaren Analysen, die sofort geteilt, exportiert oder archiviert werden können.

---

## 3. Übersicht der 11 Analysefunktionen

Das System bietet über sein modulares Cockpit genau elf eigenständige Analysemodi (`AnalysisType`):

| Analysefunktion | System-Enum (`AnalysisType`) | Hauptziel & Mehrwert | Grounding (Google Search) |
| :--- | :--- | :--- | :--- |
| **Standard Webseite** | `STANDARD_WEBSEITE` | Substantielle, ungeschönte Zusammenfassung von Webinhalten mit prägnanten Stichpunkten. | Optional (User-gesteuert) |
| **Multimedia** | `MULTIMEDIA` | Video-/Audio-Transkripte analysieren. Filtert Sponsorenblöcke, Intros und Füllsel heraus. | Optional (User-gesteuert) |
| **Dokumente** | `DOKUMENTE` | Detailgetreue Analyse von lokal hochgeladenen Dokumenten (PDFs, TXT) und Bildern. | Deaktiviert (Lokaler Fokus) |
| **3 Kernpunkte** | `TOP_3_KERNAUSSAGEN` | Schnelles Screening zur Relevanzprüfung. Präzise 3 Stichpunkte mit jeweils genau einem Satz. | Optional (User-gesteuert) |
| **Aktualitätsprüfung** | `AKTUALITAETS_CHECK` | Analysiert unabhängig das Alter der Quelle (Dimension A) und die inhaltliche Aktualität (Dimension B). | **Erzwungen Generisch (Immer Aktiv)** |
| **Fehlinformationsradar** | `FEHLINFORMATIONS_RADAR` | Aufdeckung von voreingenommenen Darstellungen, unbelegten Thesen und rhetorischen Verzerrungen. | **Erzwungen Aktiv** |
| **Risikoanalyse** | `RISIKO_ANALYSE` | Systemischer Risikokatalog zur Identifikation versteckter Gefahren und finanzieller/rechtlicher Hürden. | Optional (User-gesteuert) |
| **Business Inkubator** | `BUSINESS_INKUBATOR` | Generierung von bis zu drei tragfähigen, unkonventionellen Geschäftsideen (SaaS/Nische) aus der Quelle. | Optional (User-gesteuert) |
| **Fakt vs. Meinung** | `FACTS_VS_OPINIONS_ANALYZER` | Streng neutrale Zuordnung von Aussagen in Fakten `[F]`, Meinungen `[M]`, Vermutungen `[V]`, etc. | Optional (User-gesteuert) |
| **Perspektivenanalyse** | `PERSPECTIVES_AND_COUNTERPOSITIONS` | Aufbrechen von Confirmation Bias durch Aufzeigen fundierter, valider Gegen- und Alternativthesen. | Optional (User-gesteuert) |
| **Frage an die Quelle** | `FREIE_QUELLENANFRAGE` | Ermöglicht die gezielte Beantwortung einer spezifischen Anwender-Frage direkt basierend auf der Inhaltsquelle. | Optional (User-gesteuert) |

---

## 4. Aktueller Entwicklungsstand & Abgeschlossene Meilensteine

1. **Clean Architecture & Android App Core:**
   - Single-Activity (`MainActivity`) mit Jetpack Compose Material 3 UI.
   - Entkoppelte Layer (UI, Domain, Data, Engine, Assets).
   - 11 Analysefunktionen voll funktionsfähig und über Asset-Prompts gesteuert.
2. **Supabase Backend Foundation (MVP 1C):**
   - Direct REST API Anbindung via `SupabaseApiService` & Retrofit 2.
   - Status-Preflight Checker (`SupabaseSystemStatusChecker`, `RuntimePreflight`).
   - Supabase Migration `20260807000000_mvp1_system_status.sql` für `system_status` Singleton Tabelle.
   - Version-Mapping von Int4 und SemVer-Strings (`"1.0"` -> `1`) verifiziert in `SupabaseSystemStatusTest`.
3. **Workspace Path & Governance Clean-Up:**
   - Vollständige Eliminierung des temporären Verschachtelungs-Artefakts `app/applet/docs_md/`.
   - Festlegung der `RELEVANTOR_GAIS_WORKSPACE_RULES.md` (Kanonischer Root `/`, Pfadangabe ab `/docs_md/`).
   - Dateisystem 100% sauber und frei von Code- oder Dokumentations-Dubletten.

---

## 5. Verzeichnisstruktur der Hauptkomponenten

```text
/
├── app/                                  # Android Anwendungs-Modul
│   ├── src/main/java/com/example/        # Kotlin Quellcode (UI, Domain, Data, Engine)
│   ├── src/main/assets/prompts/          # Asset-Prompts & Manifeste
│   └── src/test/java/com/example/        # Robolectric & JUnit Tests
├── supabase/                             # Supabase Backend Configuration & Migrationen
│   ├── config.toml                       # Supabase CLI Konfiguration
│   └── migrations/                       # SQL Migrationen (system_status)
├── tools/                                # Automation & Health Gate Skripte
│   ├── git_post_ui_push_health_gate.sh   # Post-Push Health Check
│   └── build_structure_doc.py            # Verzeichnisstruktur-Generator
├── docs_md/                              # Projektdokumentation & Berichte
├── AGENTS.md                             # Bindende System Governance
└── ARCHITECTURE_FREEZE.md                # Architektur Gefriervereinbarung
```

---

## 6. Offene Aufgaben & Nächste Schritte

1. **GitHub Checkpoint (Anforderer-Aktion):**
   - Ausführen des manuellen Pushs in der GAIS GitHub UI (`CP-08 MVP1 Backend Foundation + App-Supabase DB Proof`).
2. **Nächster Entwicklungsmeilenstein (MVP 1D):**
   - Edge-Function Health Proof (`health-check` Edge Function in Supabase deployment).
3. **Langfristige Roadmap:**
   - Supabase Auth Integration & sichere Session-Synchronisation.
