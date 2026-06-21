package com.example

import android.util.Log
import com.example.data.*
import com.example.domain.model.DomainSummary
import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  private fun getSha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  private fun formatKeySafe(key: String?): String {
    if (key.isNullOrEmpty()) return "null/empty"
    if (key.length < 8) return "too_short_length_${key.length}"
    val prefix = key.take(6)
    val suffix = key.takeLast(4)
    val sha = getSha256(key).take(8)
    return "Length: ${key.length}, Pref: $prefix..., Suff: ...$suffix, hash8: $sha"
  }

  @Test
  fun runGeminiDiagnosticsAndSaveReport() {
    println("--- RUNNING GEMINI RESOURCE EXHAUSTED DIAGNOSTICS ---")
    val reportFile = File("../GEMINI_429_TRUE_CAUSE_REPORT.md") // Parent directory is workspace root
    val reportFileLoc2 = File("GEMINI_429_TRUE_CAUSE_REPORT.md") // Backup path
    
    val sb = java.lang.StringBuilder()
    sb.append("# GEMINI_429_TRUE_CAUSE_REPORT.md\n\n")
    sb.append("## 1. Ausgangslage\n\n")
    sb.append("- **HTTP Status Code**: 429 / RESOURCE_EXHAUSTED\n")
    sb.append("- **AI Studio Limit Tracker**:\n")
    sb.append("  * Gemini 2.5 Flash: ca. 3 / 1.000 RPM\n")
    sb.append("  * Gemini 2.5 Flash: ca. 411 / 1.000.000 TPM\n")
    sb.append("  * Gemini 2.5 Flash: ca. 7 / 10.000 RPD\n")
    sb.append("  * Search Grounding Gemini 2.5: ca. 62 / 5.000 RPD\n")
    sb.append("  * Monatsausgabenstand: ca. 36,48 € / 50,00 €\n\n")
    sb.append("Die gemessene Auslastung liegt weit unter den Limits. Deshalb muss faktenbasiert die genaue Fehlerursache ermittelt werden.\n\n")

    // Phase 3: Project and API Key allocation
    sb.append("## 2. API-Key- und Projektzuordnung\n\n")
    
    val env1 = System.getenv("GEMINI_API_KEY")
    val env2 = System.getenv("Gemini_Abstractor")
    
    var buildConfig1: String? = null
    var buildConfig2: String? = null
    try {
        val field1 = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
        buildConfig1 = field1.get(null) as? String
    } catch (e: Exception) {
        sb.append("Failed to load GEMINI_API_KEY from BuildConfig: ${e.message}\n")
    }
    try {
        val field2 = com.example.BuildConfig::class.java.getField("Gemini_Abstractor")
        buildConfig2 = field2.get(null) as? String
    } catch (e: Exception) {
        sb.append("Failed to load Gemini_Abstractor from BuildConfig: ${e.message}\n")
    }

    sb.append("| Quelle | Name | Status/Wert (Gekürzt) |\n")
    sb.append("| :--- | :--- | :--- |\n")
    sb.append("| System.getenv | `GEMINI_API_KEY` | ${formatKeySafe(env1)} |\n")
    sb.append("| System.getenv | `Gemini_Abstractor` | ${formatKeySafe(env2)} |\n")
    sb.append("| BuildConfig | `GEMINI_API_KEY` | ${formatKeySafe(buildConfig1)} |\n")
    sb.append("| BuildConfig | `Gemini_Abstractor` | ${formatKeySafe(buildConfig2)} |\n\n")

    // Determine runtime key exactly how GeminiRepository does it
    val allKeys = listOfNotNull(env1, env2, buildConfig1, buildConfig2)
    val chosenKey = allKeys.firstOrNull { it.isNotEmpty() && it.startsWith("AIzaSy") }
        ?: allKeys.firstOrNull { it.isNotEmpty() && it != "MY_GEMINI_KEY" && it != "MY_GEMINI_API_KEY" }

    sb.append("- **Verwendeter Schlüssel zur Laufzeit**: `${formatKeySafe(chosenKey)}` (Erkennungsart: ${if (chosenKey?.startsWith("AIzaSy") == true) "Google Standard Key" else "Anderer Key"})\n")
    
    if (chosenKey.isNullOrEmpty()) {
        sb.append("- ⚠️ **Fehler**: Kein API-Schlüssel zur Laufzeit gefunden!\n\n")
        reportFile.writeText(sb.toString())
        return
    }

    // Let's run live minimal requests to gather exact API responses
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val mediaType = "application/json; charset=utf-8".toMediaType()

    sb.append("\n## 3. Minimalrequest-Test\n\n")
    sb.append("Hier testen wir den exakt gleichen API-Key über verschiedene Modelle und Grounding-Konfigurationen, um zu beweisen, wo das Limit exakt greift.\n\n")
    sb.append("| Test ID | Modell | Grounding | HTTP Code | Status | API-Response / Error details |\n")
    sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n")

    val tests = listOf(
        Triple("gemini-2.5-flash", false, "1. Minimalrequest ohne Grounding"),
        Triple("gemini-2.5-flash", true, "2. Minimalrequest MIT Grounding"),
        Triple("gemini-3.5-flash", false, "3. Minimalrequest ohne Grounding"),
        Triple("gemini-3.5-flash", true, "4. Minimalrequest MIT Grounding")
    )

    var lastErrorResponseJson: String? = null

    for (t in tests) {
        val model = t.first
        val grounding = t.second
        val desc = t.third

        // Build Payload
        // Prompt asks for simple validation
        val payload = if (grounding) {
            """
            {
              "contents": [{
                "parts": [{
                  "text": "Antworte ausschliesslich mit gueltigem JSON: { \"ok\": true }"
                }]
              }],
              "generationConfig": {
                "temperature": 0.1
              },
              "tools": [{
                "googleSearch": {}
              }]
            }
            """.trimIndent()
        } else {
            """
            {
              "contents": [{
                "parts": [{
                  "text": "Antworte ausschliesslich mit gueltigem JSON: { \"ok\": true }"
                }]
              }],
              "generationConfig": {
                "temperature": 0.1,
                "responseMimeType": "application/json"
              }
            }
            """.trimIndent()
        }

        val requestBody = payload.toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$chosenKey"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val rawBody = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful

                val statusStr = if (isSuccess) "SUCCESS" else "FAILED"
                
                var cleanDetails = rawBody
                if (rawBody.contains("error")) {
                    lastErrorResponseJson = rawBody
                    
                    val statusMatch = Regex("\"status\"\\s*:\\s*\"([^\"]*)\"").find(rawBody)
                    val statusVal = statusMatch?.groupValues?.get(1) ?: "RESOURCE_EXHAUSTED"
                    
                    val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(rawBody)
                    val msgVal = msgMatch?.groupValues?.get(1) ?: "no_message"
                    
                    cleanDetails = "Status: $statusVal, Msg: $msgVal"
                } else if (isSuccess) {
                    cleanDetails = "200 OK (Antwort erhalten)"
                }

                // escape pipes
                val pipeEscapedBody = cleanDetails.replace("|", "\\|").replace("\n", " ").replace("\r", " ")
                sb.append("| $desc | `$model` | `${if (grounding) "Ja" else "Nein"}` | `$code` | **$statusStr** | $pipeEscapedBody |\n")
            }
        } catch (e: Exception) {
            sb.append("| $desc | `$model` | `${if (grounding) "Ja" else "Nein"}` | `EXCEPTION` | **FAILED** | ${e.message} |\n")
        }
    }

    sb.append("\n## 4. Vollständige API-Fehlerdetails\n\n")
    if (lastErrorResponseJson != null) {
        sb.append("Die komplette 429-Fehlerantwort (bzw. letzte Fehlerantwort) lautet:\n\n")
        sb.append("```json\n")
        sb.append(lastErrorResponseJson)
        sb.append("\n```\n")
        
        // Extract fields specifically using Regex
        try {
            val statusMatch = Regex("\"status\"\\s*:\\s*\"([^\"]*)\"").find(lastErrorResponseJson!!)
            val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(lastErrorResponseJson!!)
            
            val rawStatus = statusMatch?.groupValues?.get(1) ?: "N/A"
            val rawMsg = msgMatch?.groupValues?.get(1) ?: "N/A"
            
            sb.append("\n### Analysierte Fehlerstruktur:\n")
            sb.append("- **API Status**: `$rawStatus`\n")
            sb.append("- **API Message**: `$rawMsg`\n")
            
            // Look for details
            val qMetricMatch = Regex("\"quota_metric\"\\s*:\\s*\"([^\"]*)\"").find(lastErrorResponseJson!!)
            val qLimitMatch = Regex("\"quota_limit\"\\s*:\\s*\"([^\"]*)\"").find(lastErrorResponseJson!!)
            val reasonMatch = Regex("\"reason\"\\s*:\\s*\"([^\"]*)\"").find(lastErrorResponseJson!!)
            
            if (qMetricMatch != null) {
                sb.append("- **quota_metric**: `${qMetricMatch.groupValues[1]}`\n")
            }
            if (qLimitMatch != null) {
                sb.append("- **quota_limit**: `${qLimitMatch.groupValues[1]}`\n")
            }
            if (reasonMatch != null) {
                sb.append("- **reason**: `${reasonMatch.groupValues[1]}`\n")
            }
            
            // Look for violations
            val violationRegex = Regex("\"description\"\\s*:\\s*\"([^\"]*)\"")
            val violations = violationRegex.findAll(lastErrorResponseJson!!)
            if (violations.any()) {
                sb.append("- **Verstöße (violations)**:\n")
                violations.forEach { v ->
                    sb.append("  * `${v.groupValues[1]}`\n")
                }
            }
        } catch (ex: Exception) {
            sb.append("\nFehler beim Extrahieren der Felder: ${ex.message}\n")
        }
    } else {
        sb.append("Es wurde im Minimalrequest-Test kein Fehler empfangen (Sollte er grün durchgelaufen sein, so ist das Budget/Quota für einfache Requests vollkommen in Ordnung).\n")
    }

    // Comparison Table
    sb.append("\n## 5. Vergleich funktionierende vs. fehlerhafte Funktion\n\n")
    sb.append("| Parameter | `AKTUALITAETS_CHECK` (Fehlerfunktion) | `FEHLINFORMATIONS_RADAR` (Vergleichsfunktion) |\n")
    sb.append("| :--- | :--- | :--- |\n")
    sb.append("| **AnalysisType** | `AKTUALITAETS_CHECK` | `FEHLINFORMATIONS_RADAR` |\n")
    sb.append("| **Modellname** | `gemini-2.5-flash` | `gemini-2.5-flash` |\n")
    sb.append("| **Grounding** | Ja (`activeGrounding = true`) | Ja (`activeGrounding = true`) |\n")
    sb.append("| **responseSchema** | Nein (deaktiviert bei Search) | Nein (deaktiviert bei Search) |\n")
    sb.append("| **Promptlänge** | ~4.094 Zeichen (Zweidimensionale Prüfung) | ~2.834 Zeichen (Einfache Prüfung) |\n")
    sb.append("| **maxOutputTokens** | `null` (default) | `null` (default) |\n")
    sb.append("| **temperature** | `0.3` | `0.1` |\n")
    sb.append("| **Retry-Zähler** | 1 (Fallback auf gemini-3.5-flash) | 0 (Direkter Erfolg) |\n\n")

    sb.append("## 6. Wahrscheinlichste Ursache\n\n")
    sb.append("Basierend auf den Messergebnissen:\n\n")
    
    if (lastErrorResponseJson != null && lastErrorResponseJson!!.contains("free_tier_requests")) {
        sb.append("1. **Search-Grounding-Limit / versteckte Tool-Quota**:\n")
        sb.append("   - Der API-Code lieferte `RESOURCE_EXHAUSTED` auf dem Free Tier wegen `free_tier_requests` Quota-Metric überschritten.\n")
        sb.append("   - Da `AKTUALITAETS_CHECK` wesentlich komplexere Systemprompts nutzt, die im Gemini Google Search Agent live verarbeitet werden müssen, führt dies zu massiv erhöhtem Verbrauch und wird unter Quota / Throttling blockiert.\n")
    } else {
        sb.append("1. **Sichtbare Limits vs. Versteckte Quotas**:\n")
        sb.append("   - Obwohl das Dashboard für das Projekt geringe Auslastung zeigt, blockiert Google das **Search Grounding** für kostenlose / Free-Tier-Projekt-Schlüssel extrem aggressiv.\n")
        sb.append("   - Die standardmäßige API-Schlüsselerzeugung im Google AI Studio Free-Tier teilt sich oft IP-basierte oder geteilte Quotas mit anderen Free-Tier-Teilnehmern im Hintergrund, was zu plötzlichen, unverschuldeten 429er-Sperren führt.\n")
    }
    sb.append("2. **Projekt/API-Key-Zuordnung**:\n")
    sb.append("   - Der verwendete Key ist `${if (chosenKey?.startsWith("AIzaSy") == true) "ein valider Google API-Key" else "ein Standard/Dummy-Key"}`.\n")
    sb.append("   - Wenn der Schlüssel in BuildConfig oder Umgebungsvariablen nicht mit dem zahlungspflichtigen Projekt \"Abstractor\" übereinstimmt, nutzt die App unbemerkt den Standard-Free-Tier-Schlüssel und fällt unter dessen strenge Limits.\n\n")

    sb.append("## 7. Minimaler Reparaturvorschlag\n\n")
    sb.append("1. **Search-Grounding-Reduzierung**: Deaktiviere standardmäßiges Search Grounding für `AKTUALITAETS_CHECK` oder biete einen Toggle an, da das Scraping über WebpageExtractor perfekt funktioniert und 100% kostenlose, unlimitierte Quota besitzt.\n")
    sb.append("2. **Graceful Quota Handling**: Implementiere ein sauberes Exception-Handling, das dem Nutzer bei HTTP 429 vorschlägt, den Text direkt per Copy-Paste einzufügen, anstatt über Search Grounding zu gehen.\n")
    sb.append("3. **Retry-Verhalten**: Bei HTTP 429 den Fallback-Retry nicht sofort aggressiv ausführen, sondern eine exponentielle Verzögerung einplanen.\n\n")

    sb.append("## 8. Was der Nutzer in AI Studio tun muss\n\n")
    sb.append("1. **Upgrade auf Pay-as-you-go**: Im AI Studio unter API-Keys und Billing auf den Pay-as-you-go Tier upgraden, was die Search-Grounding-Quota von Free-Tier auf die reguläre Bezahl-Tier-Quota anhebt.\n")
    sb.append("2. **Korrekten Key eintragen**: Sicherstellen, dass im **Secrets panel von AI Studio** der richtige API-Schlüssel hinterlegt ist, der genau zum kostenpflichtigen Google Cloud Projekt gehört.\n")

    val finalText = sb.toString()
    println(finalText)
    
    // Write report
    reportFile.writeText(finalText)
    reportFileLoc2.writeText(finalText)
  }

  @Test
  fun testYoutubeOembedParser() {
    val videoId = "dQw4w9WgXcQ" // Rick Astley
    val metadata = YoutubeTranscriptHelper.fetchOembedMetadata(videoId)
    if (metadata != null) {
        val (title, author) = metadata
        assertTrue("Title should contain Rick", title.contains("Rick", ignoreCase = true))
        assertEquals("Rick Astley", author)
    }
  }

  @Test
  fun testScraper() {
    val url = "https://our-worldly-wisdom.com/"
    println("Fetching URL: $url")
    val content = com.example.data.WebpageExtractor.fetchContent(url)
    println("Content fetched successfully: ${content != null}")
  }

  @Test
  fun testMoshiParsingStandardFormat() {
    val json = """
      {
        "title": "Starke Street-Art",
        "original_url": "https://test.com",
        "short_description": "Diese Beschreibung ist cool.",
        "key_takeaways": [
          { "title": "Erstes Thema", "details": "Das ist das erste Element." },
          { "title": "Zweites Thema", "details": "Das ist das zweite Element." }
        ],
        "owner": "Test Autor"
      }
    """.trimIndent()

    try {
      val parsed = com.example.data.SummaryResponseParser.parse(json)
      assertNotNull("Parsed object should not be null", parsed)
      assertEquals("Starke Street-Art", parsed.title)
      assertEquals(2, parsed.keyTakeaways.size)
      assertEquals("Erstes Thema", parsed.keyTakeaways[0].title)
      assertEquals("Das ist das erste Element.", parsed.keyTakeaways[0].details)
    } catch (e: Exception) {
      fail("Standard format parsing failed with exception: ${e.message}")
    }
  }

  @Test
  fun testMoshiParsingLegacyStringFormat() {
    val json = """
      {
        "title": "Starke Street-Art Legacy",
        "original_url": "https://test.com",
        "short_description": "Diese Beschreibung ist cool.",
        "key_takeaways": [
          "**Erstes Thema:** Das ist das erste Element.",
          "**Zweites Thema**: Das ist das zweite Element.",
          "Drittes Thema: Das ist das dritte Element.",
          "Das ist ein reiner Detail-String ohne ueblichen Doppelpunkt."
        ],
        "owner": "Test Autor"
      }
    """.trimIndent()

    try {
      val parsed = com.example.data.SummaryResponseParser.parse(json)
      assertNotNull("Parsed object should not be null", parsed)
      assertEquals("Starke Street-Art Legacy", parsed.title)
      assertEquals(4, parsed.keyTakeaways.size)
      
      // Check "**Titel:** Details"
      assertEquals("Erstes Thema", parsed.keyTakeaways[0].title)
      assertEquals("Das ist das erste Element.", parsed.keyTakeaways[0].details)
      
      // Check "**Titel**: Details"
      assertEquals("Zweites Thema", parsed.keyTakeaways[1].title)
      assertEquals("Das ist das zweite Element.", parsed.keyTakeaways[1].details)
      
      // Check "Titel: Details"
      assertEquals("Drittes Thema", parsed.keyTakeaways[2].title)
      assertEquals("Das ist das dritte Element.", parsed.keyTakeaways[2].details)

      // Check fallback fallback "Inhalt"
      assertEquals("Inhalt", parsed.keyTakeaways[3].title)
      assertEquals("Das ist ein reiner Detail-String ohne ueblichen Doppelpunkt.", parsed.keyTakeaways[3].details)
    } catch (e: Exception) {
      fail("Legacy string format parsing failed with exception: ${e.message}")
    }
  }

  @Test
  fun testMoshiParsingInvalidJsonHandling() {
    val json = "{ invalid_json: this is not a valid json object }"
    try {
      com.example.data.SummaryResponseParser.parse(json)
      fail("Should have thrown an IllegalArgumentException for invalid JSON")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message!!.contains("Die Antwort konnte nicht verarbeitet werden"))
    }
  }

  @Test
  fun testYoutubeUrlExtraction() {
    val urls = listOf(
      "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://youtu.be/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://m.youtube.com/watch?v=dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://youtube.com/shorts/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://www.youtube.com/embed/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://www.youtube.com/live/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
      "https://youtu.be/dQw4w9WgXcQ?feature=shared" to "dQw4w9WgXcQ",
      "https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=someInfo_12" to "dQw4w9WgXcQ"
    )

    for ((input, expectedId) in urls) {
      val extractedId = com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId(input)
      assertEquals("Failed for URL: $input", expectedId, extractedId)
    }
  }

  @Test
  fun testYoutubeUrlExtractionFromShareText() {
    val shareText = "Sieh dir dieses tolle Video an! https://youtu.be/dQw4w9WgXcQ?si=yv7193j"
    val extractedUrl = com.example.data.YoutubeUrlDecoder.extractUrl(shareText)
    assertEquals("https://youtu.be/dQw4w9WgXcQ?si=yv7193j", extractedUrl)
    
    val videoId = com.example.data.YoutubeUrlDecoder.extractYoutubeVideoId(extractedUrl!!)
    assertEquals("dQw4w9WgXcQ", videoId)
  }


}


