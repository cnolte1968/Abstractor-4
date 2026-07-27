# RELEVANTOR – CHANGE-PROMPT-REPOSITORY
Version: 1.0 (PROD-LOCKED)

Dieses Repository dient als zentrales Archiv und Prozesshandbuch für alle strukturierten Änderungen (Change-Prompts) an Relevantor. 

---

## 1. Zweck der Change-Prompts

Ein **Change-Prompt (CP)** ist keine einmalige Codeänderung (GAA), sondern eine **wiederverwendbare Prozess- und Sicherheitsvorlage** für eine bestimmte Kategorie von Änderungen. 

Da Relevantor auf einer hochgradig entkoppelten, vertragsbasierten Plugin-Architektur basiert, führt jede unstrukturierte Änderung am System zu erheblichen Regressionsrisiken (z. B. Absturz des JSON-Parsers, Verletzung des Ausgabe-Vertrags, fehlerhafte Zuordnung in der Pipeline oder Wiedereinführung von Legacy-Notationen). 

Die Change-Prompts stellen sicher, dass GAIS:
1. Den aktuellen Systemzustand vor einer Änderung exakt analysiert (Trockenlauf).
2. Nur die für den spezifischen Change-Typ erlaubten Dateien modifiziert.
3. Die vertraglichen Schnittstellen und Validatoren unberührt lässt.
4. Jede Änderung lückenlos über automatisierte Tests und den Pipeline-Report (Copy PR) absichert.

---

## 2. Unterschied: GAA vs. Change-Prompt

| Merkmal | Global Analytical Action (GAA) | Change-Prompt (CP) |
| :--- | :--- | :--- |
| **Natur** | Eine konkrete, vom Benutzer angeforderte funktionale oder visuelle Aufgabe. | Eine standardisierte Verhaltens- und Sicherheitsrichtlinie für GAIS. |
| **Gültigkeit** | Einmalig (pro Task). | Permanent und wiederverwendbar (im System verankert). |
| **Ziel** | Implementierung einer Funktionalität. | Absicherung des Prozesses, Risikominimierung und Regressionsschutz. |

---

## 3. Die Change-Prompt-Typen (CP-1 bis CP-7)

Sämtliche Änderungen an Relevantor müssen genau einer der folgenden Kategorien zugeordnet und nach deren jeweiligem CP-Protokoll durchgeführt werden:

*   **CP-1. Optimierung Funktions-Prompt**
    *   *Fokus:* Reine inhaltliche/sprachliche Justierung eines bestehenden Markdown-Prompts in `assets/prompts/`.
    *   *Einschränkung:* Strengstens verboten sind Änderungen an Programmlogik, Parsern, Validatoren oder dem JSON-Ausgabe-Schema.
*   **CP-2. Veränderung der Arbeitsweise einer Funktion**
    *   *Fokus:* Inhaltliche oder logische Ablaufänderungen einer bestehenden Funktion, die über reine Prompt-Justierung hinausgehen.
*   **CP-3. Neuanlage einer Funktion**
    *   *Fokus:* Erstellung einer komplett neuen Analysefunktion inklusive Prompt-Datei, Registrierung und Integration.
*   **CP-4. Löschung einer Funktion**
    *   *Fokus:* Sichere und rückstandsfreie Entfernung einer Funktion aus Registries, Menüs und dem Code.
*   **CP-5. Änderung des Verarbeitungs-Fensters**
    *   *Fokus:* Anpassung der Input-Verarbeitung, Daten-Extraktion (Preflight, HTML-Cleaning, Token-Limits).
*   **CP-6. Änderung am Ausgabe-Fenster**
    *   *Fokus:* Modifikation von Output-Verträgen (Contracts), JSON-Parsern, Render-Profilen oder Validatoren.
*   **CP-7. Änderung an CD / UI / UX**
    *   *Fokus:* Anpassungen an Compose-Views, Menüstrukturen, Farbthemen oder Dialogen.

---

## 4. Das Grundprinzip der risikofreien Entwicklung und das Rollenmodell (ChatGPT vs. GAIS)

Die Bearbeitung von Change-Prompts basiert auf einem strikten Rollenmodell:
* **ChatGPT (Dialogführer):** Führt den strukturierten Dialog mit dem Benutzer und generiert den konkreten, maschinenlesbaren **GAA (Global Analytical Action)**.
* **GAIS (Ausführer):** Führt den generierten GAA im Projekt aus, ermittelt technische Parameter (z. B. SHA-Hashes, Kotlin-Pfade) selbstständig und meldet die Ergebnisse (Unit-Tests, Copy PR) zurück.
* **Benutzer (Kopierer):** Überträgt Daten und Ergebnisse als Brücke zwischen beiden Systemen.

Jeder Change-Prompt erzwingt eine strikte, phasenbasierte Ausführung:

```
[1. Baseline ermitteln] ──► [2. Trockenlauf (Dry Run)] ──► [3. Minimalumsetzung]
                                                                    │
[6. Freigabe / Release] ◄── [5. Runtime (Copy PR)]    ◄── [4. Build & Test]
```

1.  **Baseline-Validierung:** Feststellung des aktuellen Quelltext- und Prompt-Zustands sowie des SHA-256-Hashes der Baseline.
2.  **Trockenlauf (Dry Run):** Analyse der betroffenen Dateien, Bestimmung des betroffenen Parsers/Validators, Identifikation aller Risiken und Entwurf einer minimalinvasiven Änderung.
3.  **Minimalumsetzung:** Modifikation von *ausschließlich* den im Change-Prompt erlaubten Dateien. Keine "Mitnahme-Refactorings".
4.  **Build & Test:** Lokale Kompilierung via `compile_applet` und Unit-Test-Ausführung via `gradle :app:testDebugUnitTest`.
5.  **Runtime-Verifikation:** Validierung des gesamten Durchlaufs anhand des **Copy PR (Pipeline-Report)**.
6.  **Freigabeentscheidung:** Übergabe des vollständigen Berichts an den Benutzer zur finalen Abnahme. Bei Fehlern erfolgt ein sofortiger Rollback.

---

## 5. Unumstößliche Kernregeln für GAIS

*   **Copy PR bleibt die zentrale Wahrheit:** Der Pipeline-Report (Copy PR) ist das einzige, unbestechliche Werkzeug zur Prüfung der gesamten Pipeline-Funktion zur Laufzeit. Er darf niemals beschädigt, vereinfacht oder in seiner Struktur manipuliert werden.
*   **Keine Sammeländerungen:** Es wird immer nur ein einziger Change-Prompt zurzeit bearbeitet. Die Vermischung von Prompt-Optimierung (CP-1) und Schema-Änderung (CP-6) ist strengstens untersagt.
*   **Keine Legacy-Strukturen:** Die historische A/B/E-Notation oder veraltete `legacyFunctionId`-Ausgaben dürfen unter keinen Umständen wieder eingeführt werden. Alle Funktions-IDs, Kategorien und Registrierungen müssen ausschließlich moderne, kanonische und professionell benannte IDs nutzen.
*   **Erklärungs- und Risikopflicht:** GAIS muss vor jeder Änderung die betroffenen Dateien und Risiken im Rahmen des Trockenlaufs explizit auflisten.
