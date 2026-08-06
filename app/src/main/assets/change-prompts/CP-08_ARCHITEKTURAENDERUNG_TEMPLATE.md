# CP-08 – ARCHITEKTURÄNDERUNG & SYSTEMERWEITERUNG

Version 1.0 (05.08.2026)

## 1. Zweck und Einsatzbereich

CP-08 steuert alle Änderungen, die die technische Gesamtarchitektur, zentrale Plattformkomponenten, Datenhaltungsstrategien oder systemübergreifende Datenflüsse betreffen.

Anwendungsfälle:
- Backend-Einführung
- Supabase-Integration
- Datenbank-Erweiterungen
- Authentifizierungssysteme
- Lizenz-/Token-Systeme
- Analytics-Plattform
- externe Provider
- Cloud-Funktionen
- Architektur-Migrationen

---

## 2. Abgrenzung zu anderen CPs

- **CP-01**: Prompt-Optimierung ohne Architekturänderung.
- **CP-02**: Änderung bestehender Funktionslogik innerhalb der bestehenden Architektur.
- **CP-03**: Neue Funktion innerhalb des bestehenden Architekturrahmens.
- **CP-07**: UI/UX-Anpassungen.
- **CP-08**: Wenn sich die technische Gesamtstruktur, Datenflüsse, zentrale Services oder Plattformkomponenten verändern.

---

## 3. Minimalitätsprüfung vor CP-08

Vor Einsatz von CP-08 prüfen:
- Ist wirklich eine Architekturänderung notwendig?
- Kann das Ziel innerhalb der bestehenden Architektur erreicht werden?
- Welche kleinere Alternative wurde geprüft?
- Welche Komponenten können bewusst später entstehen?

Ziel: Vermeidung von Overengineering.

---

## 4. Phase 1: Fachliche Klärung

Pflichtfragen:
- Welches Problem wird gelöst?
- Welcher Nutzer- oder Geschäftsnutzen entsteht?
- Was passiert ohne diese Änderung?
- Welche Alternativen wurden geprüft?

---

## 5. Phase 2: Ist-Architektur Analyse

Prüfen:
- aktuelle Architektur
- betroffene Module
- Datenflüsse
- Schnittstellen
- Abhängigkeiten
- Sicherheitsaspekte
- Kostenfolgen
- Skalierungsrisiken

---

## 6. Phase 3: Zielarchitektur

Dokumentieren:
- Zielbild
- Komponenten
- Verantwortlichkeiten
- Datenmodell
- Schnittstellen
- Sicherheitskonzept
- Migrationsstrategie
- Fallback-Strategie

---

## 7. Phase 4: Freigabe-GAA

Standard-Format:

```markdown
GAA – CP-08 ARCHITEKTURÄNDERUNG: <Titel> – durchzuführen von: GAIS

Ziel:
<Beschreibung>

Analyseumfang:
<Komponenten / Systeme>

Betroffene Dateien und Systeme:
<Liste>

Schnittstellen:
<Datenflüsse / Verträge>

Grenzen:
<Klare Ausschlüsse>

Verifikation:
<Tests / Build / Runtime / Architekturprüfung>

Ausgabe:
<Ergebnisbericht>
```

---

## 8. Phase 5: Kontrollierte Umsetzung

Regeln:
- Umsetzung nur nach Freigabe.
- Keine parallelen Architekturänderungen.
- Minimale Änderung.
- Klare Datei- und Systemgrenzen.

---

## 9. Phase 6: Verifikation und Architektur-Freeze

Prüfen:
- Build
- Tests
- Runtime/E2E
- Datenflüsse
- Sicherheit
- Kosten
- Wartbarkeit

Danach:
- Architektur dokumentieren.
- Schnittstellen aktualisieren.
- Neue Architektur als Basis einfrieren.
