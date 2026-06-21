package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.DomainSummary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = com.example.data.SummaryResponseParser.moshiInstance

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiRepository {
    private const val SYSTEM_INSTRUCTION = """
        Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter. Deine Aufgabe ist es, den bereitgestellten Quelltext (Webseite, Dokument, Bild oder Transkript) tiefgründig, substanziell und frei von Allgemeinplätzen oder Flachheiten zu analysieren.

        Befolge strikt diese architektonischen Vorgaben für deine Ausgabe:

        1. DYNAMISCHER UMFANG: Die Länge deiner Zusammenfassung darf NICHT standardisiert kurz sein. Passe den Umfang proportional an die Komplexität und Länge der Quelle an. Ein 2-stündiges Video oder ein 20-seitiger Fachaufsatz erfordert eine detailreiche, umfassende Ausarbeitung; ein kurzer News-Beitrag wird prägnant verdichtet.
        2. SUBSTANZ STATT BLABLA: Ignoriere Einleitungen, Smalltalk, Marketing-Phrasen und Redundanzen. Konzentriere dich kompromisslos auf die harten Fakten, wissenschaftlichen Daten, strategischen Kernargumente und unkonventionellen Erkenntnisse der Quelle.
        3. VOLLE QUELLE ANALYSIEREN / KEINE KÜRZUNGEN: Du MUSST zwingend die GANZE Seite / den vollständigen bereitgestellten Text analysieren und vollständig berücksichtigen, nicht nur den Anfang oder beliebige Ausschnitte. Abkürzungen oder oberflächliche Überflüge sind strengstens verboten. Vollständigkeit hat oberste Priorität: Kernbegrifflichkeiten, Statements und harte Fakten müssen mit maximaler sachlicher Tiefe hervorgebracht werden.
        4. STRUKTUR: Halte dich zwingend an das geforderte JSON-Ausgabe-Schema, aber fülle die Felder mit maximaler intellektueller Tiefe:
           - `title`: Aussagekräftiger, präziser Titel der Quelle.
           - `original_url`: Unveränderte Original-URL.
           - `short_description`: Eine prägnante, aber dichte Einführung (maximal zwei Sätze), die den exakten Kern und den Mehrwert der Quelle auf den Punkt bringt.
           - `key_takeaways`: Ein detailreiches Array aus Bulletpoints. Jeder Bulletpoint muss eine eigenständige, tiefgründige Erkenntnis transportieren (keine Ein-Wort-Sätze, sondern ausformulierte, wertvolle Wissenshäppchen mit Kontext).
           - `owner`: Der Autor, Urheber, Ersteller oder die Organisation (Herausgeber, Medienanstalt, etc.) der Quelle, falls vorhanden, sonst null.
    """

    private val abstractorSummarySchema = ResponseSchema(
        type = "OBJECT",
        properties = mapOf(
            "title" to SchemaProperty(type = "STRING", description = "Titel der Quelle"),
            "original_url" to SchemaProperty(type = "STRING", description = "Die unveränderte Original-URL (wichtig: behalte die URL exakt so bei, wie sie übergeben wurde, ohne Zeichen zu verändern oder zu kürzen)"),
            "short_description" to SchemaProperty(type = "STRING", description = "Eine prägnante Kurzbeschreibung (maximal zwei Sätze)"),
            "key_takeaways" to SchemaProperty(
                type = "ARRAY",
                description = "Die wichtigsten Kernaussagen als übersichtliche Bulletpoints",
                items = SchemaProperty(type = "STRING")
            ),
            "owner" to SchemaProperty(type = "STRING", description = "Der Autor, Urheber, Ersteller oder die Organisation (Herausgeber, Medienanstalt, etc.) der Quelle, falls vorhanden, sonst null")
        ),
        required = listOf("title", "original_url", "short_description", "key_takeaways")
    )

    private fun getApiKey(): String? {
        val keyEnv1 = System.getenv("GEMINI_API_KEY")
        val keyEnv2 = System.getenv("Gemini_Abstractor")
        val key1 = try { BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String } catch (e: Exception) { null }
        val key2 = try { BuildConfig::class.java.getField("Gemini_Abstractor").get(null) as? String } catch (e: Exception) { null }
        
        val allKeys = listOfNotNull(keyEnv1, keyEnv2, key1, key2)
        return allKeys.firstOrNull { it.isNotEmpty() && it.startsWith("AIzaSy") }
            ?: allKeys.firstOrNull { it.isNotEmpty() && it != "MY_GEMINI_KEY" && it != "MY_GEMINI_API_KEY" }
    }

    suspend fun summarize(
        url: String,
        contentText: String?,
        useSearchGrounding: Boolean,
        analysisType: AnalysisType = AnalysisType.STANDARD_WEBSEITE,
        context: android.content.Context? = null
    ): DomainSummary {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw IllegalArgumentException("API_KEY_MISSING")
        }

        val baseSystemInstruction = PromptEngine.getSystemInstruction(
            context = context,
            analysisType = analysisType
        )

        val runtimeConfig = AnalysisRuntimeConfigs.forType(analysisType)
        val temp = runtimeConfig.temperature
        val maxTokens: Int? = null
        val activeGrounding = runtimeConfig.forceGrounding || (runtimeConfig.allowUserGrounding && useSearchGrounding)

        // Bündeln von User-Prompt: Übergabe der URL und des Inhalts (falls vorhanden)
        val promptText = StringBuilder().apply {
            append("Bitte führe die angeforderte Analyse durch für diese URL: ")
            append(url)
            append("\n\n")
            if (!contentText.isNullOrBlank()) {
                append("Hier ist der extrahierte Text/Transkript-Inhalt der Quelle:\n")
                append(contentText)
            } else if (activeGrounding) {
                append("Nutze das Google Search Grounding Tool, um den Inhalt dieser URL live abzurufen und detailliert zu analysieren.")
            } else {
                append("Hinweis: Es konnte kein direkter Text der Webseite extrahiert werden. Bitte analysiere diese URL und ihren Aufbau und nutze dein internes Wissen über diese Quelle, um das geforderte JSON-Ergebnis zu generieren.")
            }
            
            append("\n\nGib das Ergebnis als valides JSON-Objekt mit folgender Struktur zurück (und sonst absolut keinen anderen Text):\n")
            append("{\n")
            append("  \"title\": \"Titel der Quelle\",\n")
            append("  \"original_url\": \"$url\",\n")
            append("  \"short_description\": \"Eine dem Analysetyp entsprechende prägnante Beschreibung (maximal zwei Sätze)\",\n")
            append("  \"key_takeaways\": [\n")
            append("    \"Die Analyseergebnisse als übersichtliche Bulletpoints\"\n")
            append("  ]\n")
            append("}\n")
        }.toString()

        val contents = listOf(Content(parts = listOf(Part(text = promptText))))
        val systemInstruction = Content(parts = listOf(Part(text = baseSystemInstruction)))

        // Search grounding is set up using the googleSearch tool field
        val tools = if (activeGrounding) {
            listOf(Tool(googleSearch = emptyMap()))
        } else {
            null
        }

        val config = if (activeGrounding) {
            GenerationConfig(
                responseMimeType = null,
                temperature = temp,
                maxOutputTokens = maxTokens
            )
        } else {
            GenerationConfig(
                responseMimeType = if (runtimeConfig.useJsonSchemaWhenUngrounded) "application/json" else null,
                responseSchema = if (runtimeConfig.useJsonSchemaWhenUngrounded) abstractorSummarySchema else null,
                temperature = temp,
                maxOutputTokens = maxTokens
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = config,
            tools = tools,
            systemInstruction = systemInstruction
        )

        val response = generateContentWithFallback(
            apiKey = apiKey,
            request = request,
            analysisType = analysisType,
            useSearchGrounding = useSearchGrounding,
            responseSchemaActive = (config.responseSchema != null),
            contextInfo = "Webpage Summarization",
            contentTextLength = contentText?.length ?: 0
        )
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Keine Antwort von Gemini erhalten.")

        val summary = SummaryResponseParser.parse(
            input = rawText
        )
        if (analysisType == AnalysisType.TOP_3_KERNAUSSAGEN && summary.keyTakeaways.isEmpty()) {
            throw IllegalStateException("Keine Kernaussagen extrahiert.")
        }
        return summary
    }

    suspend fun summarizeFile(fileBytes: ByteArray, mimeType: String, fileName: String): DomainSummary {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw IllegalArgumentException("API_KEY_MISSING")
        }

        val promptText = StringBuilder().apply {
            append("Bitte analysiere das angehängte Dokument oder Bild ($fileName) gründlich und fasse es präzise auf Deutsch zusammen.")
            append("\n\nGib das Ergebnis als valides JSON-Objekt mit folgender Struktur zurück (und sonst absolut keinen anderen Text):\n")
            append("{\n")
            append("  \"title\": \"Titel des hochgeladenen Inhalts\",\n")
            append("  \"original_url\": \"Dateiname: $fileName\",\n")
            append("  \"short_description\": \"Eine prägnante Kurzbeschreibung des Inhalts (maximal zwei Sätze)\",\n")
            append("  \"key_takeaways\": [\n")
            append("    \"Die wichtigsten Details, Erkenntnisse oder Kernaussagen aus diesem Dokument oder Bild als übersichtliche Bulletpoints\"\n")
            append("  ]\n")
            append("}\n")
        }.toString()

        val imageBase64 = FileProcessingHelper.toBase64(fileBytes)
        val contents = listOf(
            Content(
                parts = listOf(
                    Part(text = promptText),
                    Part(inlineData = InlineData(mimeType = mimeType, data = imageBase64))
                )
            )
        )
        val systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION)))

        val config = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = abstractorSummarySchema,
            temperature = 0.2
        )

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = config,
            systemInstruction = systemInstruction
        )

        val response = generateContentWithFallback(
            apiKey = apiKey,
            request = request,
            analysisType = null,
            useSearchGrounding = false,
            responseSchemaActive = (config.responseSchema != null),
            contextInfo = "File Processing: $fileName",
            contentTextLength = imageBase64.length
        )
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Keine Antwort von Gemini erhalten.")

        return SummaryResponseParser.parse(
            input = rawText
        )
    }

    suspend fun summarizeText(text: String, fileName: String): DomainSummary {
        val apiKey = getApiKey()
        if (apiKey.isNullOrEmpty()) {
            throw IllegalArgumentException("API_KEY_MISSING")
        }

        val promptText = StringBuilder().apply {
            append("Bitte analysiere den folgenden extrahierten Text aus dem Dokument oder der Datei ($fileName) gründlich und fasse ihn präzise auf Deutsch zusammen.\n\n")
            append("Inhalt der Datei:\n")
            append(text)
            append("\n\nGib das Ergebnis als valides JSON-Objekt mit folgender Struktur zurück (und sonst absolut keinen anderen Text):\n")
            append("{\n")
            append("  \"title\": \"Titel des Dokuments\",\n")
            append("  \"original_url\": \"Dateiname: $fileName\",\n")
            append("  \"short_description\": \"Eine prägnante Kurzbeschreibung des Inhalts (maximal zwei Sätze)\",\n")
            append("  \"key_takeaways\": [\n")
            append("    \"Die wichtigsten Details, Erkenntnisse oder Kernaussagen aus diesem Dokument als übersichtliche Bulletpoints\"\n")
            append("  ]\n")
            append("}\n")
        }.toString()

        val contents = listOf(Content(parts = listOf(Part(text = promptText))))
        val systemInstruction = Content(parts = listOf(Part(text = SYSTEM_INSTRUCTION)))

        val config = GenerationConfig(
            responseMimeType = "application/json",
            responseSchema = abstractorSummarySchema,
            temperature = 0.2
        )

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = config,
            systemInstruction = systemInstruction
        )

        val response = generateContentWithFallback(
            apiKey = apiKey,
            request = request,
            analysisType = null,
            useSearchGrounding = false,
            responseSchemaActive = (config.responseSchema != null),
            contextInfo = "Text Processing: $fileName",
            contentTextLength = text.length
        )
        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Keine Antwort von Gemini erhalten.")

        return SummaryResponseParser.parse(
            input = rawText
        )
    }

    private suspend fun generateContentWithFallback(
        apiKey: String,
        request: GenerateContentRequest,
        analysisType: AnalysisType?,
        useSearchGrounding: Boolean,
        responseSchemaActive: Boolean,
        contextInfo: String,
        contentTextLength: Int = 0,
        initiatedByShareIntent: Boolean = false
    ): GenerateContentResponse {
        val apiEndpointBase = "https://generativelanguage.googleapis.com/"
        val apiVersion = "v1beta"
        
        val promptLen = request.contents.firstOrNull()?.parts?.firstOrNull()?.text?.length ?: 0
        val maxTokens = request.generationConfig?.maxOutputTokens ?: -1
        val temp = request.generationConfig?.temperature ?: -1.0
        val trackingGrounding = request.tools?.any { it.googleSearch != null } == true
        
        val keyIdentifier = if (apiKey.length >= 8) {
            "Length: ${apiKey.length}, Prefix: ${apiKey.take(6)}...${apiKey.takeLast(4)}"
        } else {
            "Length: ${apiKey.length}, Value: $apiKey"
        }
        val googleProjectId = System.getenv("GOOGLE_CLOUD_PROJECT") ?: "Abstractor (Default/BuildConfig)"
        val estimatedTokens = (promptLen + contentTextLength) / 4
        
        Log.i("GeminiRepository", "=== [BEFORE CALL DIAGNOSTICS] ===")
        Log.i("GeminiRepository", "- 1. API-Key-Name oder Identifier: $keyIdentifier")
        Log.i("GeminiRepository", "- 2. Google-Projekt-ID: $googleProjectId")
        Log.i("GeminiRepository", "- 3. Modellname: ${GeminiModelConfig.TEXT_MODEL}")
        Log.i("GeminiRepository", "- 4. API-Version / Endpoint: $apiVersion / models/${GeminiModelConfig.TEXT_MODEL}:generateContent")
        Log.i("GeminiRepository", "- 5. AnalysisType: ${analysisType ?: "null"}")
        Log.i("GeminiRepository", "- 6. useSearchGrounding: $trackingGrounding")
        Log.i("GeminiRepository", "- 7. responseSchema aktiv: $responseSchemaActive")
        Log.i("GeminiRepository", "- 8. Promptlaenge in Zeichen: $promptLen")
        Log.i("GeminiRepository", "- 9. Contentlaenge in Zeichen: $contentTextLength")
        Log.i("GeminiRepository", "- 10. geschaetzte Tokenzahl: $estimatedTokens")
        Log.i("GeminiRepository", "- 11. maxOutputTokens: $maxTokens")
        Log.i("GeminiRepository", "- 12. temperature: $temp")
        Log.i("GeminiRepository", "- 13. Retry-Zaehler: 0 (Initial Call)")
        Log.i("GeminiRepository", "- 15. Gestartet durch Share-Intent oder URL: ${if (initiatedByShareIntent) "Share-Intent" else "URL-Eingabe (oder Direktverarbeitung $contextInfo)"}")
        Log.i("GeminiRepository", "==============================")

        return try {
            val res = RetrofitClient.service.generateContent(GeminiModelConfig.TEXT_MODEL, apiKey, request)
            Log.i("GeminiRepository", "=== [AFTER CALL DIAGNOSTICS - SUCCESS] ===")
            Log.i("GeminiRepository", "- 10. HTTP-Status: 200")
            Log.i("GeminiRepository", "- 11. API-Status: SUCCESS")
            Log.i("GeminiRepository", "- 12. API-Message: OK")
            Log.i("GeminiRepository", "- 13. Anzahl Retry-Versuche: 0")
            Log.i("GeminiRepository", "==============================")
            res
        } catch (e: Exception) {
            var httpCode = -1
            var errorBodyMsg = ""
            if (e is retrofit2.HttpException) {
                httpCode = e.code()
                errorBodyMsg = try { e.response()?.errorBody()?.string() ?: "" } catch (ex: Exception) { "" }
                
                // Detailed 429 reporting
                if (httpCode == 429 || errorBodyMsg.contains("RESOURCE_EXHAUSTED") || errorBodyMsg.contains("quota")) {
                    val apiStatus = extractJsonField(errorBodyMsg, "status") ?: "RESOURCE_EXHAUSTED"
                    val apiMsg = extractJsonField(errorBodyMsg, "message") ?: "Quota exceeded / resource exhausted"
                    val qMetric = extractJsonField(errorBodyMsg, "quota_metric") ?: "unknown_metric"
                    val qId = extractJsonField(errorBodyMsg, "quota_limit") ?: "unknown_limit"
                    val qVal = extractJsonField(errorBodyMsg, "quota_limit_value") ?: "unknown_value"
                    val rDelay = extractJsonField(errorBodyMsg, "retryDelay") ?: "N/A"
                    
                    val violations = mutableListOf<String>()
                    Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").findAll(errorBodyMsg).forEach { match ->
                        violations.add(match.groupValues[1])
                    }
                    
                    val sanitizedBody = errorBodyMsg.replace(apiKey, "REDACTED_API_KEY")
                    
                    Log.e("GeminiRepository", "=== [HTTP 429 / RESOURCE_EXHAUSTED DETECTED (PRIMARY)] ===")
                    Log.e("GeminiRepository", "- HTTP Code: $httpCode")
                    Log.e("GeminiRepository", "- API Status: $apiStatus")
                    Log.e("GeminiRepository", "- API Message: $apiMsg")
                    Log.e("GeminiRepository", "- quotaMetric: $qMetric")
                    Log.e("GeminiRepository", "- quotaId: $qId")
                    Log.e("GeminiRepository", "- quotaValue: $qVal")
                    Log.e("GeminiRepository", "- retryDelay: $rDelay")
                    Log.e("GeminiRepository", "- violations: $violations")
                    Log.e("GeminiRepository", "- Full Response Body: $sanitizedBody")
                    Log.e("GeminiRepository", "==============================")
                }
            }
            Log.i("GeminiRepository", "=== [AFTER CALL DIAGNOSTICS - PRIMARY FAILED] ===")
            Log.i("GeminiRepository", "- 10. HTTP-Status: $httpCode")
            Log.i("GeminiRepository", "- 11. API-Status: FAILED")
            Log.i("GeminiRepository", "- 12. API-Message: $errorBodyMsg")
            Log.i("GeminiRepository", "- 13. Anzahl Retry-Versuche: 1 (Initiating Fallback)")
            Log.i("GeminiRepository", "==============================")
            
            Log.i("GeminiRepository", "=== [BEFORE FALLBACK CALL DIAGNOSTICS] ===")
            Log.i("GeminiRepository", "- 1. API-Key-Name oder Identifier: $keyIdentifier")
            Log.i("GeminiRepository", "- 2. Google-Projekt-ID: $googleProjectId")
            Log.i("GeminiRepository", "- 3. Modellname: ${GeminiModelConfig.FALLBACK_MODEL}")
            Log.i("GeminiRepository", "- 4. API-Version / Endpoint: $apiVersion / models/${GeminiModelConfig.FALLBACK_MODEL}:generateContent")
            Log.i("GeminiRepository", "- 5. AnalysisType: ${analysisType ?: "null"}")
            Log.i("GeminiRepository", "- 6. useSearchGrounding: $trackingGrounding")
            Log.i("GeminiRepository", "- 7. responseSchema aktiv: $responseSchemaActive")
            Log.i("GeminiRepository", "- 8. Promptlaenge in Zeichen: $promptLen")
            Log.i("GeminiRepository", "- 9. Contentlaenge in Zeichen: $contentTextLength")
            Log.i("GeminiRepository", "- 10. geschaetzte Tokenzahl: $estimatedTokens")
            Log.i("GeminiRepository", "- 11. maxOutputTokens: $maxTokens")
            Log.i("GeminiRepository", "- 12. temperature: $temp")
            Log.i("GeminiRepository", "- 13. Retry-Zaehler: 1 (Fallback Call)")
            Log.i("GeminiRepository", "- 15. Gestartet durch Share-Intent oder URL: ${if (initiatedByShareIntent) "Share-Intent" else "URL-Eingabe (oder Direktverarbeitung $contextInfo)"}")
            Log.i("GeminiRepository", "==============================")
            
            val fallbackRes = try {
                val r = RetrofitClient.service.generateContent(GeminiModelConfig.FALLBACK_MODEL, apiKey, request)
                Log.i("GeminiRepository", "=== [AFTER FALLBACK CALL DIAGNOSTICS - SUCCESS] ===")
                Log.i("GeminiRepository", "- 10. HTTP-Status: 200")
                Log.i("GeminiRepository", "- 11. API-Status: SUCCESS")
                Log.i("GeminiRepository", "- 12. API-Message: OK")
                Log.i("GeminiRepository", "- 13. Anzahl Retry-Versuche: 1 (Success on retry 1)")
                Log.i("GeminiRepository", "==============================")
                r
            } catch (fallbackEx: Exception) {
                var fHttpCode = -1
                var fErrorBody = ""
                if (fallbackEx is retrofit2.HttpException) {
                    fHttpCode = fallbackEx.code()
                    fErrorBody = try { fallbackEx.response()?.errorBody()?.string() ?: "" } catch (ex: Exception) { "" }
                    
                    // Detailed 429 reporting on fallback
                    if (fHttpCode == 429 || fErrorBody.contains("RESOURCE_EXHAUSTED") || fErrorBody.contains("quota")) {
                        val apiStatus = extractJsonField(fErrorBody, "status") ?: "RESOURCE_EXHAUSTED"
                        val apiMsg = extractJsonField(fErrorBody, "message") ?: "Quota exceeded / resource exhausted"
                        val qMetric = extractJsonField(fErrorBody, "quota_metric") ?: "unknown_metric"
                        val qId = extractJsonField(fErrorBody, "quota_limit") ?: "unknown_limit"
                        val qVal = extractJsonField(fErrorBody, "quota_limit_value") ?: "unknown_value"
                        val rDelay = extractJsonField(fErrorBody, "retryDelay") ?: "N/A"
                        
                        val violations = mutableListOf<String>()
                        Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").findAll(fErrorBody).forEach { match ->
                            violations.add(match.groupValues[1])
                        }
                        
                        val sanitizedBody = fErrorBody.replace(apiKey, "REDACTED_API_KEY")
                        
                        Log.e("GeminiRepository", "=== [HTTP 429 / RESOURCE_EXHAUSTED DETECTED (FALLBACK)] ===")
                        Log.e("GeminiRepository", "- HTTP Code: $fHttpCode")
                        Log.e("GeminiRepository", "- API Status: $apiStatus")
                        Log.e("GeminiRepository", "- API Message: $apiMsg")
                        Log.e("GeminiRepository", "- quotaMetric: $qMetric")
                        Log.e("GeminiRepository", "- quotaId: $qId")
                        Log.e("GeminiRepository", "- quotaValue: $qVal")
                        Log.e("GeminiRepository", "- retryDelay: $rDelay")
                        Log.e("GeminiRepository", "- violations: $violations")
                        Log.e("GeminiRepository", "- Full Response Body: $sanitizedBody")
                        Log.e("GeminiRepository", "==============================")
                    }
                }
                Log.i("GeminiRepository", "=== [AFTER FALLBACK CALL DIAGNOSTICS - FAILURE] ===")
                Log.i("GeminiRepository", "- 10. HTTP-Status: $fHttpCode")
                Log.i("GeminiRepository", "- 11. API-Status: FAILED")
                Log.i("GeminiRepository", "- 12. API-Message: $fErrorBody")
                Log.i("GeminiRepository", "- 13. Anzahl Retry-Versuche: 1 (Both models failed)")
                Log.i("GeminiRepository", "==============================")
                throw fallbackEx
            }
            fallbackRes
        }
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\"([^\"]*)\"")
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.trim()
    }
}
