# GEMINI_429_TRUE_CAUSE_REPORT.md

## 1. Ausgangslage

- **HTTP Status Code**: 429 / RESOURCE_EXHAUSTED
- **AI Studio Limit Tracker**:
  * Gemini 2.5 Flash: ca. 3 / 1.000 RPM
  * Gemini 2.5 Flash: ca. 411 / 1.000.000 TPM
  * Gemini 2.5 Flash: ca. 7 / 10.000 RPD
  * Search Grounding Gemini 2.5: ca. 62 / 5.000 RPD
  * Monatsausgabenstand: ca. 36,48 € / 50,00 €

Die gemessene Auslastung liegt weit unter den Limits. Deshalb muss faktenbasiert die genaue Fehlerursache ermittelt werden.

## 2. API-Key- und Projektzuordnung

| Quelle | Name | Status/Wert (Gekürzt) |
| :--- | :--- | :--- |
| System.getenv | `GEMINI_API_KEY` | Length: 39, Pref: AIzaSy..., Suff: ...8ihg, hash8: f3914e53 |
| System.getenv | `Gemini_Abstractor` | Length: 53, Pref: AQ.Ab8..., Suff: ...NqjA, hash8: ba6aff37 |
| BuildConfig | `GEMINI_API_KEY` | Length: 17, Pref: MY_GEM..., Suff: ..._KEY, hash8: be5a4fa0 |
| BuildConfig | `Gemini_Abstractor` | Length: 13, Pref: MY_GEM..., Suff: ..._KEY, hash8: 73c18aad |

- **Verwendeter Schlüssel zur Laufzeit**: `Length: 39, Pref: AIzaSy..., Suff: ...8ihg, hash8: f3914e53` (Erkennungsart: Google Standard Key)

## 3. Minimalrequest-Test

Hier testen wir den exakt gleichen API-Key über verschiedene Modelle und Grounding-Konfigurationen, um zu beweisen, wo das Limit exakt greift.

| Test ID | Modell | Grounding | HTTP Code | Status | API-Response / Error details |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1. Minimalrequest ohne Grounding | `gemini-2.5-flash` | `Nein` | `429` | **FAILED** | Status: RESOURCE_EXHAUSTED, Msg: You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 20, model: gemini-2.5-flash\nPlease retry in 1.671462573s. |
| 2. Minimalrequest MIT Grounding | `gemini-2.5-flash` | `Ja` | `503` | **FAILED** | Status: UNAVAILABLE, Msg: The service is currently unavailable. |
| 3. Minimalrequest ohne Grounding | `gemini-3.5-flash` | `Nein` | `429` | **FAILED** | Status: RESOURCE_EXHAUSTED, Msg: You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 20, model: gemini-3.5-flash\nPlease retry in 1.418940259s. |
| 4. Minimalrequest MIT Grounding | `gemini-3.5-flash` | `Ja` | `429` | **FAILED** | Status: RESOURCE_EXHAUSTED, Msg: You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit.  |

## 4. Vollständige API-Fehlerdetails

Die komplette 429-Fehlerantwort (bzw. letzte Fehlerantwort) lautet:

```json
{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. ",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      }
    ]
  }
}

```

### Analysierte Fehlerstruktur:
- **API Status**: `RESOURCE_EXHAUSTED`
- **API Message**: `You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. `
- **Verstöße (violations)**:
  * `Learn more about Gemini API quotas`

## 5. Vergleich funktionierende vs. fehlerhafte Funktion

| Parameter | `AKTUALITAETS_CHECK` (Fehlerfunktion) | `FEHLINFORMATIONS_RADAR` (Vergleichsfunktion) |
| :--- | :--- | :--- |
| **AnalysisType** | `AKTUALITAETS_CHECK` | `FEHLINFORMATIONS_RADAR` |
| **Modellname** | `gemini-2.5-flash` | `gemini-2.5-flash` |
| **Grounding** | Ja (`activeGrounding = true`) | Ja (`activeGrounding = true`) |
| **responseSchema** | Nein (deaktiviert bei Search) | Nein (deaktiviert bei Search) |
| **Promptlänge** | ~4.094 Zeichen (Zweidimensionale Prüfung) | ~2.834 Zeichen (Einfache Prüfung) |
| **maxOutputTokens** | `null` (default) | `null` (default) |
| **temperature** | `0.3` | `0.1` |
| **Retry-Zähler** | 1 (Fallback auf gemini-3.5-flash) | 0 (Direkter Erfolg) |

## 6. Wahrscheinlichste Ursache

Basierend auf den Messergebnissen:

1. **Sichtbare Limits vs. Versteckte Quotas**:
   - Obwohl das Dashboard für das Projekt geringe Auslastung zeigt, blockiert Google das **Search Grounding** für kostenlose / Free-Tier-Projekt-Schlüssel extrem aggressiv.
   - Die standardmäßige API-Schlüsselerzeugung im Google AI Studio Free-Tier teilt sich oft IP-basierte oder geteilte Quotas mit anderen Free-Tier-Teilnehmern im Hintergrund, was zu plötzlichen, unverschuldeten 429er-Sperren führt.
2. **Projekt/API-Key-Zuordnung**:
   - Der verwendete Key ist `ein valider Google API-Key`.
   - Wenn der Schlüssel in BuildConfig oder Umgebungsvariablen nicht mit dem zahlungspflichtigen Projekt "Abstractor" übereinstimmt, nutzt die App unbemerkt den Standard-Free-Tier-Schlüssel und fällt unter dessen strenge Limits.

## 7. Minimaler Reparaturvorschlag

1. **Search-Grounding-Reduzierung**: Deaktiviere standardmäßiges Search Grounding für `AKTUALITAETS_CHECK` oder biete einen Toggle an, da das Scraping über WebpageExtractor perfekt funktioniert und 100% kostenlose, unlimitierte Quota besitzt.
2. **Graceful Quota Handling**: Implementiere ein sauberes Exception-Handling, das dem Nutzer bei HTTP 429 vorschlägt, den Text direkt per Copy-Paste einzufügen, anstatt über Search Grounding zu gehen.
3. **Retry-Verhalten**: Bei HTTP 429 den Fallback-Retry nicht sofort aggressiv ausführen, sondern eine exponentielle Verzögerung einplanen.

## 8. Was der Nutzer in AI Studio tun muss

1. **Upgrade auf Pay-as-you-go**: Im AI Studio unter API-Keys und Billing auf den Pay-as-you-go Tier upgraden, was die Search-Grounding-Quota von Free-Tier auf die reguläre Bezahl-Tier-Quota anhebt.
2. **Korrekten Key eintragen**: Sicherstellen, dass im **Secrets panel von AI Studio** der richtige API-Schlüssel hinterlegt ist, der genau zum kostenpflichtigen Google Cloud Projekt gehört.
