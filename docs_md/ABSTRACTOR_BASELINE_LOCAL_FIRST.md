# ABSTRACTOR – TECHNICAL BASELINE FREEZE & PROJECT HANDOVER

Dieses Dokument friert den aktuellen Entwicklungsstand der **Abstractor-App** nach Abschluss der Local-First-Härtung ein. Es dient als technisches Übergabedokument, grenzt zukünftige Backend-Integrationen klar ab und definiert einen sicheren, stabilen Wiederaufsetzpunkt für weitere Entwicklungsschritte.

---

## 1. ARCHITEKTURSTATUS (LOCAL-FIRST DESIGN)

Der Abstractor ist in dieser Version als **vollständig funktionsfähige Local-First Offline-App** konzipiert und implementiert. Alle Datenflüsse, Zwischenspeicherungen und Benutzeroberflächen arbeiten lokal, stabil und ohne die Notwendigkeit einer aktiven Internetverbindung für administrative Zwecke.

### 1.1 Aktueller Produktmodus
*   **Modus:** 100% Local-First / Gastmodus.
*   **Backend-Bereitschaft (Backend-Readiness):** Alle Netzwerkschnittstellen, lokale Warteschlangen (Sync Queue) und Synchronisations-Worker sind im Code vollständig vorbereitet erhalten, jedoch im aktuellen Release-Zustand durch globale Feature-Flags funktional deaktiviert.

### 1.2 Feature-Flags (`BackendFeatureConfig`)
Die Steuerung erfolgt zentral über das Objekt `BackendFeatureConfig` außerhalb des Domain-Layers:
*   `backendEnabled = false`: Verhindert jegliche direkte Verbindung zur Backend-URL.
*   `authEnabled = false`: Deaktiviert Login-, Registrierungs- und Account-Auswahlflüsse.
*   `cloudSyncEnabled = false`: Deaktiviert den `SyncWorker`, den `SyncScheduler` sowie die automatische Übertragung geänderter lokaler Datensätze in die Cloud.

### 1.3 Hauptdatenfluss (Standard-Verarbeitungspfad)
```
[Benutzereingabe: URL/Text] 
         │
         ▼
[AnalyzeContentUseCase]
         │ (Gemini API Call für Inhaltsanalyse)
         ▼
[Gemini API Service / Response]
         │ (Liefert Roh-JSON)
         ▼
[SummaryResponseParser]
         │ (Valide Strict-Checking via Moshi -> Erzeugt DomainSummary)
         ▼
[DomainSummary Model]
         │
         ├──► [MainActivity / UI]:Sofortige visuelle Darstellung
         │
         └──► [AnalysisRepositoryImpl / App Database (Room)]
                   │
                   ▼ (Speicherung als Offline-Entität in SQLite)
             [Local Cache / Room DB]
                   │
                   ▼ (Auslesen beim App-Start oder History-Klick)
             [Lokaler Verlauf (UI)]
```

---

## 2. SYSTEMARCHITEKTUR & SCHICHTEN (LAYERS)

Das Projekt folgt einer strikten Implementierung der **Clean Architecture** zur Trennung von Belangen, was eine spätere Aktivierung oder Anpassung des Backends ohne Nebeneffekte ermöglicht.

### 2.1 UI Layer (MainActivity)
*   **Verhalten:** Ist als reiner *Render-Only-Layer* realisiert. Sie empfängt Zustände (UI States) und zeichnet die Komponenten (Jetpack Compose).
*   **Eigenschaften:** Enthält keine Parsing-Logik, keine direkten DAO-Aufrufe, keine API-Routing-Details und keine raw Network-Mechaniken. Alle Interaktionen werden an das ViewModel delegiert.

### 2.2 Presentation Layer (MainViewModel)
*   **Verhalten:** Orchestriert UI-Zustände aus Flows und delegiert Geschäftslogik an die übergeordneten *UseCases*.
*   **Eigenschaften:** Kapselt Datenströme, verwaltet UI-Zustände (z.B. Analyse-Fortschritt, Dialog-Schnittstellen) und transformiert DB-Entitäten für die Präsentation. Er ruft UseCases auf, niemals direkt die Datenquellen (DAOs/Retrofit).

### 2.3 Domain Layer (Core Logic)
*   **Verhalten:** Ist die reinste Schicht und enthält ausschließlich plattformunabhängigen, geschäftsrelevanten Kotlin-Code.
*   **Eigenschaften:** Enthält das Kernmodell `DomainSummary` (samt `TakeawayItem`), die Schnittstellen-Definitionen (Interfaces) der Repositories (z.B. `AnalysisRepository`) sowie die fachlichen UseCases (z.B. `AnalyzeContentUseCase`).
*   **Freiheitsgrad:** **Keine Abhändigkeiten** zu Android-Frameworks, Room-Annotationen, oder Retrofit-Bibliotheken.

### 2.4 Data Layer (Schnittstellen & Frameworks)
*   **Verhalten:** Kapselt die konkrete Datenakquise und -haltung. Er vereinigt lokale SQLite-Dateien mit Cloud-Ressourcen.
*   **Eigenschaften:**
    *   **Gemini Client:** Empfängt Prompts und liefert strukturierte Textantworten.
    *   **Room Database (`AbstractorDatabase`):** Verwaltet persistente lokale Tabellen für Analyseergebnisse.
    *   **BackendApiService (Retrofit):** Definiert REST-Endpunkte für Cloud-Speicherung und Authentifizierung (nicht aktiv).
    *   **WorkManager Integration:** Handhabt Background-Sync-Dienste, gesteuert über den `SyncScheduler` (nicht aktiv).

### 2.5 Prompt Layer (AIGC & Quality Rules)
*   **Verhalten:** Definiert, wie strukturierte Antworten vom KI-Modell angefordert werden.
*   **Eigenschaften:** Gesteuert durch zentrale Prompts (z.B. `_global_quality_rules.md`, `prompt_manifest.json`), welche das Gemini-Modell auf ein exakt definiertes, standardisiertes JSON-Ausgabeformat festlegen (Validierung via `SummaryResponseParser`).

---

## 3. FUNKTIONALER SNAPSHOT

### 3.1 Aktive Features (Local-First Release)
1.  **Stabile Inhaltsanalyse:** Verarbeitung von Links (Webseiten, Medium, etc.), Texten und YouTube-Transkripten.
2.  **Strict JSON Parsing:** Zuverlässiges Parsing des Gemini-Antwortobjekts direkt in das typsichere `DomainSummary`.
3.  **Lokales Auto-Save:** Jedes erfolgreich generierte Ergebnis wird vollautomatisch ohne Nutzerinteraktion in der lokalen Room-Datenbank gespeichert.
4.  **Lokaler Verlauf (Local History):**
    *   Verlaufseinträge überdauern App-Neustarts.
    *   Unterstützt das Laden alter Berichte aus der Historie in die interaktive Hauptansicht.
    *   Verfügt über einen sauberen Empty-State bei leerer Datenbank.
5.  **Copy & Share UI Clutter-Free:** Teilen und Kopieren von Analysen als textuelle Zusammenfassung, perfekt aufbereitet ohne Backend-Artefakte.
6.  **Sichere Local-First UI:** Die Oberfläche wurde von Cloud-Hinweisen befreit. Anstelle ausbaufähiger Sync-Buttons wird transparent die Meldung *"Ihre generierten Analysen werden sicher und privat ausschließlich lokal auf Ihrem Gerät gespeichert."* präsentiert.

### 3.2 Deaktivierte / Vorbereitete Features
*(Vorbereitet, aber im aktuellen Release deaktiviert)*
*   **Cloud Backend-API:** Endpunkte für Analyse-Upload und Session-Handling.
*   **Auth Module:** Registrierung, Login, Benutzer-Validierung und Token-Speicherung.
*   **Synchronisations-Engine:** Der `SyncWorker` bricht seine Verarbeitung augenblicklich mit `Result.success()` ab, sobald er im Guest-First-Modus gestartet wird, und führt keine Netzwerkzugriffe aus.
*   **Sync Queue:** Lokale Zwischenspeicherung von Löschungen/Erstellungen für Offline-Verfügbarkeit.
*   **Background Scheduler:** Verhindert das Registrieren geplanter Jobs im Android `WorkManager` im lokalen Standardbetrieb.

---

## 4. BEKANNTE EINSCHRÄNKUNGEN & HINWEISE

1.  **Robolectric / Unit Tests in GAIS:** Die vollständige Ausführung komplexer Unit- und Screenshot-Tests mithilfe von Robolectric wird in manchen cloudbasierten Test-Umgebungen durch Deadlines oder Sandbox-Sicherheitsrichtlinien eingeschränkt. Eine lokale Ausführung der bereitgestellten Testklassen (z.B. `ExampleRobolectricTest` via `./gradlew test`) auf der Entwicklungs-Workstation wird empfohlen.
2.  **Release-Signing / Keystore:** Signaturen für den Google Play Store (`release.keystore`) sind umgebungsspezifisch. Für die lokale Erzeugung ist `assembleDebug` oder ein Unsigned-Release-Build zu verwenden.
3.  **Echte Backend-Bereitstellung:** Backend-Endpunkte für die Synchronisierung existieren auf Code-Ebene als REST-Schnittstellen, verlangen aber vor Aktivierung eine konkrete Backend-Serverinstanz sowie eine verifizierbare API-Spezifikation.

---

## 5. TECHNISCHES DATEI-INVENTAR (KEY FILES)

| Datei | Pfad | Zweck |
| :--- | :--- | :--- |
| **BackendFeatureConfig.kt** | `/app/src/main/java/com/example/data/BackendFeatureConfig.kt` | Zentraler Schalter für alle Backend-, Authentifizierungs- und Sync-Prozesse. |
| **MainActivity.kt** | `/app/src/main/java/com/example/MainActivity.kt` | Haupteintritts-UI und Render-Schicht der Compose-Oberfläche. |
| **MainViewModel.kt** | `/app/src/main/java/com/example/ui/MainViewModel.kt` | Bindeglied, State-Flow-Halter und Orchestrator für UseCases. |
| **DomainSummary.kt** | `/app/src/main/java/com/example/domain/model/DomainSummary.kt` | Das zentrale fachliche Datenmodell der strukturierten Zusammenfassung. |
| **AnalyzeContentUseCase.kt** | `/app/src/main/java/com/example/domain/usecase/AnalyzeContentUseCase.kt` | UseCase zur Steuerung der Gemini Core-Analyse. |
| **AnalysisRepository.kt** | `/app/src/main/java/com/example/domain/repository/AnalysisRepository.kt` | Domänen-Schnittstelle für Lese- und Schreibzugriffe auf Ergebnisse. |
| **AnalysisRepositoryImpl.kt** | `/app/src/main/java/com/example/data/repository/AnalysisRepositoryImpl.kt` | Implementierung, die Daten in Room speichert und Sync-Checks prüft. |
| **SummaryResponseParser.kt** | `/app/src/main/java/com/example/data/SummaryResponseParser.kt` | Strict-Parser für die JSON-Validierung. |
| **AbstractorDatabase.kt** | `/app/src/main/java/com/example/data/local/AbstractorDatabase.kt` | Hauptklasse der Room-Datenbank (lokale SQLite). |
| **LocalEntities.kt** | `/app/src/main/java/com/example/data/local/entities/LocalEntities.kt` | Definition aller SQL-Tabellenstrukturen (Entities) für Room. |
| **LocalDaos.kt** | `/app/src/main/java/com/example/data/local/daos/LocalDaos.kt` | Datenzugriffsobjekte (DAOs) für SQLite-Operationen. |
| **SyncWorker.kt** | `/app/src/main/java/com/example/data/sync/SyncWorker.kt` | WorkManager-Worker für Hintergrundabgleiche (deaktiviert). |
| **SyncScheduler.kt** | `/app/src/main/java/com/example/data/sync/SyncScheduler.kt` | Verplanung der Hintergrundjobs über Android OS (deaktiviert). |
| **_global_quality_rules.md** | `/assets/prompts/_global_quality_rules.md` | Globale System-Eingaben für die Inhaltsanalyse durch die KI. |
| **prompt_manifest.json** | `/assets/prompts/prompt_manifest.json` | Manifest zur Registrierung strukturierter Gemini Analyse-Prompts. |

---

## 6. SICHERHEITSREGELN FÜR DIE WEITERARBEIT

Damit die Stabilität und die Offline-Sicherheit des Abstractors auch bei zukünftigen Erweiterungen gewahrt bleiben, müssen Entwickler folgende Designrichtlinien einhalten:

1.  **Backend-Aktivierung nur über Flags:**
    *   Aktivieren Sie das Backend niemals hartcodiert in einzelnen Klassen. Verwenden Sie stets die Abfragen von `BackendFeatureConfig`.
2.  **Keine unfertige Cloud-UI ausliefern:**
    *   Erweitern Sie die Benutzeroberfläche erst dann um Login-Felder, Account-Verzeichnisse oder Cloud-Status-Symbole, wenn das dafür nötige Backend stabil steht und getestet wurde.
3.  **Parser-Integrität wahren:**
    *   Erweitern Sie das Schema von `DomainSummary` nur abwärtskompatibel. Der JSON-Vertrag im Prompt-Verzeichnis muss exakt mit den Kotlin-Klassenschnittstellen übereinstimmen.
4.  **UI Render-Only belassen:**
    *   Vermeiden Sie das Injizieren von DAOs oder API-Services in Composable-Funktionen. Alle Aktionen gehen durch das `MainViewModel`.
5.  **Domain Android-frei halten:**
    *   Fügen Sie keine Android-spezifischen Imports (wie `android.content.Context`) oder Jetpack Component Pakete in den Domain-Layer ein.

---

## 7. EMPFOHLENE NÄCHSTE ENTWICKLUNGSPHASEN

Nachdem die Local-First-Härtung abgeschlossen und eingefroren ist, können folgende Phasen getrennt voneinander als eigener Entwicklungsumfang gestartet werden:

### Phase A: Backend-Schnittstellenspezifikation (API Contract)
*   **Ziel:** Definition des genauen REST-API-Vertrags zwischen App und Server.
*   **Inhalt:** Festlegung der JSON-Strukturen für `/api/auth/login`, `/api/auth/register`, `/api/analyses` und das Konfliktlösungsmodell bei der Synchronisierung.

### Phase B: Authentifizierung & User Cache Aktivierung
*   **Ziel:** Bereitstellung einer sicheren Benutzerverwaltung lokal und remote.
*   **Inhalt:** Aktivierung von `authEnabled = true`, Erstellen der Login-/Registrierungsansicht, lokale Absicherung des Secret-Tokens im EncryptedSharedPreference-Keystore.

### Phase C: Datenabgleich & Synchronisations-Härtung
*   **Ziel:** Synchronisation des lokalen SQLite-Verlaufs mit der Cloud-Datenbank.
*   **Inhalt:** Aktivierung von `cloudSyncEnabled = true`. Testen von Edge Cases wie Netzausfall während der Analyse, Offline-Queue-Abarbeitung nach Verbindungswiederkehr und Conflict-Resolution (z.B. Client-Wins vs. Server-Wins).

---

**Freigegeben als stabile Local-First Basis für weitere Releases.** 🚀
