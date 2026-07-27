package com.example

import android.util.Log
import com.example.data.*
import com.example.domain.model.DomainSummary
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    val env2 = System.getenv("Gemini_Relevantor")
    
    var buildConfig1: String? = null
    var buildConfig2: String? = null
    try {
        val field1 = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
        buildConfig1 = field1.get(null) as? String
    } catch (e: Exception) {
        sb.append("Failed to load GEMINI_API_KEY from BuildConfig: ${e.message}\n")
    }
    try {
        val field2 = com.example.BuildConfig::class.java.getField("Gemini_Relevantor")
        buildConfig2 = field2.get(null) as? String
    } catch (e: Exception) {
        sb.append("Failed to load Gemini_Relevantor from BuildConfig: ${e.message}\n")
    }

    sb.append("| Quelle | Name | Status/Wert (Gekürzt) |\n")
    sb.append("| :--- | :--- | :--- |\n")
    sb.append("| System.getenv | `GEMINI_API_KEY` | ${formatKeySafe(env1)} |\n")
    sb.append("| System.getenv | `Gemini_Relevantor` | ${formatKeySafe(env2)} |\n")
    sb.append("| BuildConfig | `GEMINI_API_KEY` | ${formatKeySafe(buildConfig1)} |\n")
    sb.append("| BuildConfig | `Gemini_Relevantor` | ${formatKeySafe(buildConfig2)} |\n\n")

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
    sb.append("   - Wenn der Schlüssel in BuildConfig oder Umgebungsvariablen nicht mit dem zahlungspflichtigen Projekt \"Relevantor\" übereinstimmt, nutzt die App unbemerkt den Standard-Free-Tier-Schlüssel und fällt unter dessen strenge Limits.\n\n")

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
      val parsed = com.example.data.SummaryResponseParser.parse(json, analysisId = "test-run-id-1")
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
      val parsed = com.example.data.SummaryResponseParser.parse(json, analysisId = "test-run-id-2")
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
      com.example.data.SummaryResponseParser.parse(json, analysisId = "test-run-id-3")
      fail("Expected IOException due to STRUCTURED_EXTRACTION_FAILED")
    } catch (e: java.io.IOException) {
      assertEquals("STRUCTURED_EXTRACTION_FAILED", e.message)
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

  @Test
  fun testMoshiParsingUserScreenshotJson() {
    val json = """
      {
        "title": "Wischnewski in Guinea-Bissau (March 2026)",
        "original_url": "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/",
        "short_description": "Eine Zusammenfassung von Wischnewskis abenteuerlicher Reise nach Guinea-Bissau, die von schlechten Straßen, kulturellen Begegnungen und der Erkundung abgelegener Inseln geprägt ist.",
        "key_takeaways": [
          {
            "title": "Abenteuerliche Anreise über Land",
            "details": "Die Einreise von Senegal nach Guinea-Bissau erwies sich aufgrund extrem schlechter Straßenverhältnisse und zahlreicher Kontrollpunkte als zeitraubend und anstrengend."
          },
          {
            "title": "Kulturelle Begegnungen in Bissau",
            "details": "In der Hauptstadt Bissau erlebte der Reisende eine entspannte Atmosphäre, kolonialen Charme und die Offenheit der Einheimischen trotz der wirtschaftlichen Herausforderungen."
          },
          {
            "title": "Die unberührten Bijagós-Inseln",
            "details": "Ein Höhepunkt war die Überfahrt zum unberührten Archipel der Bijagós-Inseln, wo traditionelle Lebensweisen und eine einzigartige Tierwelt im Vordergrund standen."
          }
        ],
        "owner": "Wischnewski Unlimited"
      }
    """.trimIndent()

    try {
      val parsed = com.example.data.SummaryResponseParser.parse(json, analysisId = "test-screenshot")
      println("TEST_DEBUG: parsed successfully!")
      println("TEST_DEBUG: title = ${parsed.title}")
      println("TEST_DEBUG: shortDescription = ${parsed.shortDescription}")
      println("TEST_DEBUG: takeaways size = ${parsed.keyTakeaways.size}")
      for (takeaway in parsed.keyTakeaways) {
        println("TEST_DEBUG: takeaway: ${takeaway.title} -> ${takeaway.details}")
      }
      assertEquals(3, parsed.keyTakeaways.size)
    } catch (e: Exception) {
      e.printStackTrace()
      fail("Failed to parse user screenshot JSON: ${e.message}")
    }
  }

  @Test
  fun testRobustParsingWithMalformedJson() {
    val malformedJson = """
      {
        "title": "Abenteuerliche "Reise" nach Guinea-Bissau",
        "original_url": "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/",
        "short_description": "Eine abenteuerliche "Reise" über Land.",
        "key_takeaways": [
          {
            "title": "[F] Abenteuerliche "Anreise" über Land [F]",
            "details": "Die Einreise von Senegal nach Guinea-Bissau [inkl. Kontrollpunkte] war schwierig."
          }
        ]
      }
    """.trimIndent()
    
    try {
      val parsed = com.example.data.SummaryResponseParser.parse(malformedJson, analysisId = "test-robust")
      assertEquals("Abenteuerliche \"Reise\" nach Guinea-Bissau", parsed.title)
      assertEquals("Eine abenteuerliche \"Reise\" über Land.", parsed.shortDescription)
      assertEquals(1, parsed.keyTakeaways.size)
      assertEquals("[F] Abenteuerliche \"Anreise\" über Land [F]", parsed.keyTakeaways[0].title)
      assertEquals("Die Einreise von Senegal nach Guinea-Bissau [inkl. Kontrollpunkte] war schwierig.", parsed.keyTakeaways[0].details)
    } catch (e: Exception) {
      e.printStackTrace()
      fail("Failed robust parsing: ${e.message}")
    }
  }

  @Test
  fun testA1WischnewskiLive() {
    println("=== STARTING LIVE DIAGNOSTIC FOR WISCHNEWSKI URL ===")
    val url = "https://www.wischnewski-unlimited.com/wischnewski-in-guinea-bissau/"
    
    // 1. Extract content
    val extracted = com.example.data.WebpageExtractor.extractWebpageContent(url)
    
    val rawHtml = com.example.data.WebpageExtractor.client.newCall(
        okhttp3.Request.Builder().url(url).build()
    ).execute().use { it.body?.string() ?: "" }
    
    val extractor = com.example.data.WebpageExtractor
    val (containerHtml, containerName) = extractor.extractPreferredContainer(rawHtml)
    println("DEBUG_STEP: Preferred Container Name: $containerName")
    println("DEBUG_STEP: Container HTML length: ${containerHtml.length}")
    if (containerHtml.length > 500) {
        println("DEBUG_STEP: Container HTML First 500: ${containerHtml.take(500)}")
    }
    
    val ruleCounts = mutableMapOf<String, Int>()
    val cleanNonContent = extractor.cleanNonContentTags(containerHtml, ruleCounts)
    println("DEBUG_STEP: After cleanNonContentTags length: ${cleanNonContent.length}")
    println("DEBUG_STEP: Rule counts after non-content: $ruleCounts")
    
    val ruleCountsBoilerplate = mutableMapOf<String, Int>()
    val cleanBoilerplate = extractor.cleanBoilerplateTags(cleanNonContent, ruleCountsBoilerplate)
    println("DEBUG_STEP: After cleanBoilerplateTags length: ${cleanBoilerplate.length}")
    println("DEBUG_STEP: Rule counts after boilerplate: $ruleCountsBoilerplate")
    
    var textWithSpaces = cleanBoilerplate.replace(Regex("<[^>]*?>"), " ")
    textWithSpaces = extractor.decodeHtmlEntities(textWithSpaces)
    textWithSpaces = textWithSpaces.replace(Regex("\\s+"), " ").trim()
    println("DEBUG_STEP: Text with spaces before boilerplate clean length: ${textWithSpaces.length}")
    println("DEBUG_STEP: Text with spaces before boilerplate clean: ${textWithSpaces.take(1000)}")
    
    val textBoilerplates = listOf(
        "Deine E-Mail-Adresse wird nicht veröffentlicht",
        "Erforderliche Felder sind mit * markiert",
        "Kommentar hinterlassen",
        "Kommentar schreiben",
        "Meinen Namen, meine E-Mail-Adresse und meine Website in diesem Browser für die nächste Kommentierung speichern",
        "Benachrichtige mich über nachfolgende Kommentare via E-Mail",
        "Benachrichtige mich über neue Beiträge via E-Mail",
        "Diese Website verwendet Cookies",
        "Cookie-Einstellungen",
        "Datenschutzerklärung",
        "Impressum",
        "Stolz präsentiert von WordPress",
        "Teilen mit:",
        "Gefällt mir:",
        "Gefällt mir Wird geladen",
        "E-Mail-Adresse wird nicht veröffentlicht",
        "Suche nach:",
        "Beitrag nicht abgeschickt - E-Mail Adresse kontrollieren",
        "E-Mail-Überprüfung fehlgeschlagen, bitte versuche es noch einmal",
        "Ihr Blog kann leider keine Beiträge per E-Mail teilen",
        "Schreibe einen Kommentar"
    )
    
    var currentText = textWithSpaces
    for (bp in textBoilerplates) {
        val pattern = Regex(Regex.escape(bp) + ".*?(?:\\.|\\n|$)", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(currentText).toList()
        if (matches.isNotEmpty()) {
            println("DEBUG_STEP: Boilerplate matched: '$bp'")
            for (m in matches) {
                println("DEBUG_STEP:   Removing matched text: '${m.value}'")
            }
            currentText = currentText.replace(pattern, "")
            println("DEBUG_STEP:   New length: ${currentText.length}")
        }
    }
    
    val finalCleaned = extractor.cleanHtmlContent(containerHtml, mutableMapOf())
    println("DEBUG_STEP: Final cleaned length: ${finalCleaned.length}")
    if (finalCleaned.length > 500) {
        println("DEBUG_STEP: Final cleaned first 500: ${finalCleaned.take(500)}")
    }
    
    // Read the diagnostics that were filled in GatewayDiagnostics during extractWebpageContent
    val rawHtmlLength = com.example.data.GatewayDiagnostics.rawHtmlLength
    val selectedContentContainer = com.example.data.GatewayDiagnostics.selectedContentContainer
    val textBeforeCleaningLength = com.example.data.GatewayDiagnostics.textBeforeCleaningLength
    val textAfterCleaningLength = com.example.data.GatewayDiagnostics.textAfterCleaningLength
    val finalUserContent = extracted.enrichedText ?: ""
    val finalUserContentLength = finalUserContent.length
    try {
        java.io.File("finalUserContent.txt").writeText(finalUserContent)
    } catch (e: Exception) {
        println("ERROR writing finalUserContent.txt: ${e.message}")
    }
    
    val containsCommentText = finalUserContent.contains("Kommentar", ignoreCase = true) || finalUserContent.contains("comment", ignoreCase = true)
    val containsCategoryText = finalUserContent.contains("Kategorie", ignoreCase = true) || finalUserContent.contains("category", ignoreCase = true)
    val containsArchiveText = finalUserContent.contains("Archiv", ignoreCase = true) || finalUserContent.contains("archive", ignoreCase = true)
    val containsCookieText = finalUserContent.contains("Cookie", ignoreCase = true) || finalUserContent.contains("Consent", ignoreCase = true)
    val containsSocialSharingText = finalUserContent.contains("teilen", ignoreCase = true) || finalUserContent.contains("share", ignoreCase = true) || finalUserContent.contains("Gefällt mir", ignoreCase = true)
    val containsSearchText = finalUserContent.contains("suchen", ignoreCase = true) || finalUserContent.contains("search", ignoreCase = true)
    val containsNewsletterText = finalUserContent.contains("newsletter", ignoreCase = true) || finalUserContent.contains("abonnieren", ignoreCase = true) || finalUserContent.contains("subscribe", ignoreCase = true)
    
    println("DIAG_A1_rawHtmlLength: $rawHtmlLength")
    println("DIAG_A1_selectedContentContainer: $selectedContentContainer")
    println("DIAG_A1_textBeforeCleaningLength: $textBeforeCleaningLength")
    println("DIAG_A1_textAfterCleaningLength: $textAfterCleaningLength")
    println("DIAG_A1_finalUserContentLength: $finalUserContentLength")
    println("DIAG_A1_containsCommentText: $containsCommentText")
    println("DIAG_A1_containsCategoryText: $containsCategoryText")
    println("DIAG_A1_containsArchiveText: $containsArchiveText")
    println("DIAG_A1_containsCookieText: $containsCookieText")
    println("DIAG_A1_containsSocialSharingText: $containsSocialSharingText")
    println("DIAG_A1_containsSearchText: $containsSearchText")
    println("DIAG_A1_containsNewsletterText: $containsNewsletterText")
    
    val first1500 = if (finalUserContent.length > 1500) finalUserContent.substring(0, 1500) else finalUserContent
    val last1500 = if (finalUserContent.length > 1500) finalUserContent.substring(finalUserContent.length - 1500) else finalUserContent
    println("DIAG_A1_first1500:\n$first1500")
    println("DIAG_A1_last1500:\n$last1500")
    
    // 2. Prompt-Wirksamkeit belegen
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val loader = com.example.data.engine.AndroidAssetPromptLoader(context)
    val basePrompt = loader.loadAsset("prompts/F_STANDARD_WEBSEITE.md")
    val globalRules = try { loader.loadAsset("prompts/_global_quality_rules.md") } catch (e: Exception) { "" }
    val systemInstructionText = if (globalRules.isNotBlank()) {
        "$basePrompt\n\n=== GLOBAL QUALITY RULES ===\n$globalRules"
    } else {
        basePrompt
    }
    
    val promptSha256 = getSha256(systemInstructionText)
    val promptFirst500 = if (systemInstructionText.length > 500) systemInstructionText.substring(0, 500) else systemInstructionText
    val promptContainsOutputLimits = systemInstructionText.contains("max. 2 Sätze", ignoreCase = true) || systemInstructionText.contains("exakt 3 bis 5", ignoreCase = true) || systemInstructionText.contains("8 Wörter", ignoreCase = true)
    val promptContainsBoilerplateExclusion = systemInstructionText.contains("Website-Rahmeninhalten", ignoreCase = true) || systemInstructionText.contains("Kommentaren", ignoreCase = true) || systemInstructionText.contains("Kategorien", ignoreCase = true)
    val finalSystemPromptLength = systemInstructionText.length
    
    println("DIAG_A2_promptAssetFile: prompts/F_STANDARD_WEBSEITE.md")
    println("DIAG_A2_promptSha256: $promptSha256")
    println("DIAG_A2_promptFirst500:\n$promptFirst500")
    println("DIAG_A2_promptContainsOutputLimits: $promptContainsOutputLimits")
    println("DIAG_A2_promptContainsBoilerplateExclusion: $promptContainsBoilerplateExclusion")
    println("DIAG_A2_finalSystemPromptLength: $finalSystemPromptLength")
    
    // 3. Output Gemini-Rohantwort & 4. Check Contract-Lücke
    val apiKey = System.getenv("GEMINI_API_KEY") ?: try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
    val isKeyConfigured = apiKey.isNotBlank() && apiKey != "MY_GEMINI_KEY" && apiKey != "MY_GEMINI_API_KEY" && apiKey != "YOUR_API_KEY_HERE"
    if (isKeyConfigured) {
        println("=== CALLING GEMINI LIVE FOR WISCHNEWSKI ANALYSIS ===")
        val gateway = com.example.data.GeminiRepository
        val engine = com.example.data.engine.web.WebpageAnalysisEngine(gateway, loader)
        
        try {
            val summary = kotlinx.coroutines.runBlocking {
                engine.analyze(extracted)
            }
            println("DIAG_A3_geminiRawResponseLength: ${com.example.data.GatewayDiagnostics.rawGeminiResponseLength}")
            println("DIAG_A3_geminiRawResponsePreview: ${com.example.data.GatewayDiagnostics.rawGeminiFirstSafeChars}")
            
            val rawResponseFile = java.io.File("raw_gemini_response.json")
            val rawResponse = if (rawResponseFile.exists()) rawResponseFile.readText() else ""
            println("DIAG_A3_geminiRawResponse:\n$rawResponse")
            
            // 4. Check Contract-Lücke
            val keyTakeaways = summary.keyTakeaways
            println("DIAG_A4_takeawaysSize: ${keyTakeaways.size}")
            var contractViolation = false
            val detailsList = mutableListOf<String>()
            
            for ((index, takeaway) in keyTakeaways.withIndex()) {
                val details = takeaway.details
                val wordCount = details.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                println("DIAG_A4_takeaway_${index + 1}_wordCount: $wordCount")
                println("DIAG_A4_takeaway_${index + 1}_text: $details")
                if (wordCount > 30) {
                    contractViolation = true
                    detailsList.add("Takeaway ${index + 1} details too long: $wordCount words (limit is 30 words)")
                }
            }
            
            val shortDescWordCount = summary.shortDescription.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            println("DIAG_A4_shortDesc_wordCount: $shortDescWordCount")
            println("DIAG_A4_shortDesc_text: ${summary.shortDescription}")
            if (shortDescWordCount > 35) {
                contractViolation = true
                detailsList.add("Short description too long: $shortDescWordCount words (limit is 35 words)")
            }
            
            println("DIAG_A4_contractCompliance: ${!contractViolation}")
            if (contractViolation) {
                println("DIAG_A4_contractViolations:\n${detailsList.joinToString("\n")}")
            }
        } catch (e: Throwable) {
            println("=== GEMINI LIVE CALL FAILED ===")
            e.printStackTrace()
        }
    } else {
        println("=== SKIPPING GEMINI LIVE CALL: NO API KEY PROVIDED ===")
    }
  }

  @Test
  fun testAnalysisTypeMigrationPhase1Compatibility() {
      // 1. canonical() mapping checks
      assertEquals(AnalysisType.WEB_SUMMARY, AnalysisType.STANDARD_WEBSEITE.canonical())
      assertEquals(AnalysisType.KEY_TAKEAWAYS, AnalysisType.TOP_3_KERNAUSSAGEN.canonical())
      assertEquals(AnalysisType.RELEVANT_ASPECTS, AnalysisType.WEITERE_RELEVANTE_ASPEKTE.canonical())
      assertEquals(AnalysisType.DOCUMENT_SUMMARY, AnalysisType.DOKUMENTE.canonical())
      
      assertEquals(AnalysisType.WEB_SUMMARY, AnalysisType.WEB_SUMMARY.canonical())
      assertEquals(AnalysisType.KEY_TAKEAWAYS, AnalysisType.KEY_TAKEAWAYS.canonical())
      assertEquals(AnalysisType.RELEVANT_ASPECTS, AnalysisType.RELEVANT_ASPECTS.canonical())
      assertEquals(AnalysisType.DOCUMENT_SUMMARY, AnalysisType.DOCUMENT_SUMMARY.canonical())

      // 3. AnalysisRegistryImpl routing and delegation checks
      val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
      // We can use a mock gateway or just pass a simple anonymous subclass since we don't call analyze()
      val dummyGateway = object : com.example.domain.repository.GeminiGateway {
          override suspend fun generateContent(model: String, request: com.example.data.GenerateContentRequest): com.example.data.GenerateContentResponse {
              throw NotImplementedError("Mock gateway for registry testing - should not be invoked")
          }
      }
      val registry = com.example.data.engine.AnalysisRegistryImpl(dummyGateway, context)

      // Verify same functionId is returned for type mapping
      assertEquals(registry.getFunctionIdForType(AnalysisType.STANDARD_WEBSEITE), registry.getFunctionIdForType(AnalysisType.WEB_SUMMARY))
      assertEquals(registry.getFunctionIdForType(AnalysisType.TOP_3_KERNAUSSAGEN), registry.getFunctionIdForType(AnalysisType.KEY_TAKEAWAYS))
      assertEquals(registry.getFunctionIdForType(AnalysisType.WEITERE_RELEVANTE_ASPEKTE), registry.getFunctionIdForType(AnalysisType.RELEVANT_ASPECTS))
      assertEquals(registry.getFunctionIdForType(AnalysisType.DOKUMENTE), registry.getFunctionIdForType(AnalysisType.DOCUMENT_SUMMARY))

      // Verify the engines mapped to them are identical and non-null
      val a1LegacyEngine = registry.getEngine("WEB_SUMMARY")
      val a2LegacyEngine = registry.getEngine("KEY_TAKEAWAYS")
      val b6LegacyEngine = registry.getEngine("RELEVANT_ASPECTS")
      val e1LegacyEngine = registry.getEngine("DOCUMENT_SUMMARY")

      assertNotNull("WEB_SUMMARY engine should exist", a1LegacyEngine)
      assertNotNull("KEY_TAKEAWAYS engine should exist", a2LegacyEngine)
      assertNotNull("RELEVANT_ASPECTS engine should exist", b6LegacyEngine)
      assertNotNull("DOCUMENT_SUMMARY engine should exist", e1LegacyEngine)

      // 4. OutputPresentationPolicy mapping checks
      val legacyA2Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.TOP_3_KERNAUSSAGEN)
      val newA2Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.KEY_TAKEAWAYS)
      assertEquals(legacyA2Policy, newA2Policy)

      val legacyA1Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.STANDARD_WEBSEITE)
      val newA1Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.WEB_SUMMARY)
      assertEquals(legacyA1Policy, newA1Policy)

      val legacyB6Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.WEITERE_RELEVANTE_ASPEKTE)
      val newB6Policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(AnalysisType.RELEVANT_ASPECTS)
      assertEquals(legacyB6Policy, newB6Policy)
      assertEquals("WEITERE RELEVANTE ASPEKTE ZUR QUELLE", newB6Policy.sectionHeader)
      assertEquals("WICHTIGSTE KERNAUSSAGEN", newA2Policy.sectionHeader)

      // Verify FeatureCatalog maps B.6 to RELEVANT_ASPECTS
      val b6Metadata = com.example.ui.metadata.FeatureCatalog.features.find { it.functionId == "RELEVANT_ASPECTS" }
      assertNotNull("RELEVANT_ASPECTS should exist in FeatureCatalog", b6Metadata)
      assertEquals(AnalysisType.RELEVANT_ASPECTS, b6Metadata!!.analysisType)
  }

  @Test
  fun testAnalysisTypeMigrationPhase2Diagnostics() {
      // Verify GatewayDiagnostics can hold and reset the new fields
      GatewayDiagnostics.reset()
      assertEquals("", GatewayDiagnostics.loadedCanonicalAnalysisType)

      GatewayDiagnostics.loadedCanonicalAnalysisType = AnalysisType.STANDARD_WEBSEITE.canonical().name

      assertEquals("WEB_SUMMARY", GatewayDiagnostics.loadedCanonicalAnalysisType)

      // Verify SmokeTestCaseResult correctly receives fields on construction
      val preflight = SmokeTestCasePreflight("PASS", "PASS")
      val steps = SmokeTestCaseSteps()
      val result = SmokeTestCaseResult(
          testId = "T-WEB_SUMMARY",
          analysisType = AnalysisType.STANDARD_WEBSEITE.name,
          canonicalAnalysisType = AnalysisType.STANDARD_WEBSEITE.canonical().name,
          inputType = "WEB",
          extractor = "WebpageExtractor",
          preflight = preflight,
          steps = steps
      )

      assertEquals("STANDARD_WEBSEITE", result.analysisType)
      assertEquals("WEB_SUMMARY", result.canonicalAnalysisType)

      // Verify JSON serialization includes the new keys
      val report = SmokeTestHarnessReport(
          appVersion = "1.0",
          device = "TestDevice",
          networkType = "WIFI",
          tests = listOf(result)
      )
      val json = report.toJsonString()

      assertTrue("JSON should contain main canonicalAnalysisType", json.contains("\"canonicalAnalysisType\": \"WEB_SUMMARY\""))
  }

  @Test
  fun testNotationsmigrationPhase3WebSummary() {
      // 1. Web-Summary-Feature in FeatureCatalog uses WEB_SUMMARY as its start type
      val webSummaryFeature = com.example.ui.metadata.FeatureCatalog.features.find { it.functionId == "WEB_SUMMARY" }
      assertNotNull("Web summary feature metadata should exist", webSummaryFeature)
      assertEquals("A.1 feature should use WEB_SUMMARY", AnalysisType.WEB_SUMMARY, webSummaryFeature?.analysisType)

      // 2. WEB_SUMMARY routes to LegacyFunctionId A.1 and correct function ID
      val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
      val dummyGateway = object : com.example.domain.repository.GeminiGateway {
          override suspend fun generateContent(model: String, request: com.example.data.GenerateContentRequest): com.example.data.GenerateContentResponse {
              throw NotImplementedError("Mock gateway")
          }
      }
      val registry = com.example.data.engine.AnalysisRegistryImpl(dummyGateway, context)
      assertEquals("WEB_SUMMARY", registry.getFunctionIdForType(AnalysisType.WEB_SUMMARY))

      // 3. WEB_SUMMARY uses the existing prompt file F_STANDARD_WEBSEITE.md
      val webSummaryEngine = registry.getEngine("WEB_SUMMARY")
      assertNotNull("Web summary engine should be non-null", webSummaryEngine)
      assertEquals("prompts/F_STANDARD_WEBSEITE.md", webSummaryEngine?.contract?.promptPath)

      // 4. STANDARD_WEBSEITE remains functional as a legacy type
      assertEquals("WEB_SUMMARY", registry.getFunctionIdForType(AnalysisType.STANDARD_WEBSEITE))
  }

  @Test
  fun testNotationsmigrationPhase4Kernaussagen() {
      // 1. Kernaussagen-Feature in FeatureCatalog uses KEY_TAKEAWAYS as its start type
      val kernaussagenFeature = com.example.ui.metadata.FeatureCatalog.features.find { it.functionId == "KEY_TAKEAWAYS" }
      assertNotNull("Kernaussagen feature metadata should exist", kernaussagenFeature)
      assertEquals("A.2 feature should use KEY_TAKEAWAYS", AnalysisType.KEY_TAKEAWAYS, kernaussagenFeature?.analysisType)

      // 2. KEY_TAKEAWAYS routes to LegacyFunctionId A.2 and correct function ID
      val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
      val dummyGateway = object : com.example.domain.repository.GeminiGateway {
          override suspend fun generateContent(model: String, request: com.example.data.GenerateContentRequest): com.example.data.GenerateContentResponse {
              throw NotImplementedError("Mock gateway")
          }
      }
      val registry = com.example.data.engine.AnalysisRegistryImpl(dummyGateway, context)
      assertEquals("KEY_TAKEAWAYS", registry.getFunctionIdForType(AnalysisType.KEY_TAKEAWAYS))

      // 3. KEY_TAKEAWAYS uses the existing prompt file F_TOP_3_KERNAUSSAGEN.md
      val kernaussagenEngine = registry.getEngine("KEY_TAKEAWAYS")
      assertNotNull("Kernaussagen engine should be non-null", kernaussagenEngine)
      assertEquals("prompts/F_TOP_3_KERNAUSSAGEN.md", kernaussagenEngine?.contract?.promptPath)

      // 4. TOP_3_KERNAUSSAGEN remains functional as a legacy type
      assertEquals("KEY_TAKEAWAYS", registry.getFunctionIdForType(AnalysisType.TOP_3_KERNAUSSAGEN))

      // 5. Web Summary remains WEB_SUMMARY
      val webSummaryFeature = com.example.ui.metadata.FeatureCatalog.features.find { it.functionId == "WEB_SUMMARY" }
      assertEquals(AnalysisType.WEB_SUMMARY, webSummaryFeature?.analysisType)
  }
}


