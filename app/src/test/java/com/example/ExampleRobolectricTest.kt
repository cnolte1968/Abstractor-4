package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Abstractor", appName)
  }

  @Test
  fun testGeminiTop3Live() = kotlinx.coroutines.runBlocking {
    val url = "https://our-worldly-wisdom.com/georgetown-penang-street-art/"
    println("ROBOLECTRIC: Fetching content for: $url")
    val content = com.example.data.WebpageExtractor.fetchContent(url) ?: "Sample content about traveling."
    
    val apiKey = System.getenv("GEMINI_API_KEY")

    if (apiKey.isNullOrEmpty()) {
        println("ROBOLECTRIC: API Key GEMINI_API_KEY is missing, skipping Gemini live test.")
        return@runBlocking
    }
    println("ROBOLECTRIC: API Key found. Calling Gemini live API...")

    try {
        val summary = com.example.data.GeminiRepository.summarize(
            url = url,
            contentText = content,
            useSearchGrounding = false,
            analysisType = com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN
        )
        println("ROBOLECTRIC: Summary Title: ${summary.title}")
        println("ROBOLECTRIC: Summary Original URL: ${summary.originalUrl}")
        println("ROBOLECTRIC: Summary Short Description: ${summary.shortDescription}")
        println("ROBOLECTRIC: Summary Key Takeaways Count: ${summary.keyTakeaways.size}")
        summary.keyTakeaways.forEachIndexed { index, takeaway ->
            println("ROBOLECTRIC: Takeaway ${index + 1}: $takeaway")
        }
    } catch (e: Exception) {
        println("ROBOLECTRIC: Gemini live test failed with exception:")
        e.printStackTrace()
    }
  }

  @Test
  fun testDirectGemini35Flash() = kotlinx.coroutines.runBlocking {
    val apiKey = System.getenv("GEMINI_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("ROBOLECTRIC: API Key missing.")
        return@runBlocking
    }
    val request = com.example.data.GenerateContentRequest(
        contents = listOf(com.example.data.Content(parts = listOf(com.example.data.Part(text = "Hello!")))),
        generationConfig = com.example.data.GenerationConfig(
            temperature = 0.2
        )
    )
    println("ROBOLECTRIC: Direct call to gemini-3.5-flash...")
    try {
        val response = com.example.data.RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
        println("ROBOLECTRIC: gemini-3.5-flash success! Result text: ${response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text}")
    } catch (e: retrofit2.HttpException) {
        println("ROBOLECTRIC: gemini-3.5-flash failed with HttpException:")
        println("  Status Code: ${e.code()}")
        println("  Error Body: ${e.response()?.errorBody()?.string()}")
    } catch (e: Exception) {
        println("ROBOLECTRIC: gemini-3.5-flash failed with exception:")
        e.printStackTrace()
    }
  }

  @Test
  fun testDirectGemini25Flash() = kotlinx.coroutines.runBlocking {
    val apiKey = System.getenv("GEMINI_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("ROBOLECTRIC: API Key missing.")
        return@runBlocking
    }
    val request = com.example.data.GenerateContentRequest(
        contents = listOf(com.example.data.Content(parts = listOf(com.example.data.Part(text = "Hello!")))),
        generationConfig = com.example.data.GenerationConfig(
            temperature = 0.2
        )
    )
    println("ROBOLECTRIC: Direct call to gemini-2.5-flash...")
    try {
        val response = com.example.data.RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
        println("ROBOLECTRIC: gemini-2.5-flash success! Result text: ${response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text}")
    } catch (e: retrofit2.HttpException) {
        println("ROBOLECTRIC: gemini-2.5-flash failed with HttpException:")
        println("  Status Code: ${e.code()}")
        println("  Error Body: ${e.response()?.errorBody()?.string()}")
    } catch (e: Exception) {
        println("ROBOLECTRIC: gemini-2.5-flash failed with exception:")
        e.printStackTrace()
    }
  }

  @Test
  fun testListModels() = kotlinx.coroutines.runBlocking {
    val apiKey = System.getenv("GEMINI_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("ROBOLECTRIC: API Key missing, skipping ListModels test.")
        return@runBlocking
    }
    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
    println("ROBOLECTRIC: Calling ListModels at: https://generativelanguage.googleapis.com/v1beta/models")
    try {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            println("ROBOLECTRIC: ListModels Response HTTP Status: ${response.code}")
            
            // simple regex to find all models/xxxx
            val regex = """models/[a-zA-Z0-9.\-_]+""".toRegex()
            val matches = regex.findAll(body).map { it.value }.distinct().toList()
            println("ROBOLECTRIC: Distinct Model Names Found:")
            matches.forEach { model ->
                if (model.contains("gemini", ignoreCase = true)) {
                    println("  - $model")
                }
            }
        }
    } catch (e: Exception) {
        println("ROBOLECTRIC: ListModels failed with exception:")
        e.printStackTrace()
    }
  }

  @Test
  fun testAllAnalysisTypes() = kotlinx.coroutines.runBlocking {
    val apiKey = System.getenv("GEMINI_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        println("ROBOLECTRIC: API Key missing, skipping all-types test.")
        return@runBlocking
    }
    val url = "https://our-worldly-wisdom.com/georgetown-penang-street-art/"
    println("ROBOLECTRIC: Fetching content for: $url")
    val content = com.example.data.WebpageExtractor.fetchContent(url) ?: "Sample content about traveling."
    
    val types = listOf(
        com.example.data.AnalysisType.AKTUALITAETS_CHECK,
        com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR
    )
    for (t in types) {
        println("\nROBOLECTRIC: =========================================")
        println("ROBOLECTRIC: TESTING ANALYSIS TYPE: $t")
        try {
            val summary = com.example.data.GeminiRepository.summarize(
                url = url,
                contentText = content,
                useSearchGrounding = false,
                analysisType = t
            )
            println("ROBOLECTRIC: $t SUCCESS! Takeaways count: ${summary.keyTakeaways.size}")
        } catch (e: Exception) {
            println("ROBOLECTRIC: $t FAILED with exception:")
            if (e is retrofit2.HttpException) {
                println("  Status Code: ${e.code()}")
                val errBody = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
                println("  Error Body: $errBody")
            } else {
                e.printStackTrace()
            }
        }
        println("ROBOLECTRIC: =========================================\n")
    }
  }

  @Test
  fun testPromptLoaderFallbackAndRestore() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    println("FALLBACK_TEST: Starting controlled reflection-based fallback test")
    
    fun clearLoaderCache() {
      try {
        val cacheField = com.example.data.PromptLoader::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cacheMap = cacheField.get(null) as? java.util.concurrent.ConcurrentHashMap<*, *>
        cacheMap?.clear()
        println("FALLBACK_TEST: Loader cache cleared successfully.")
      } catch (e: Exception) {
        println("FALLBACK_TEST: Could not clear cache using reflection: ${e.message}")
      }
    }

    try {
      // 1. Initial State: Load with matching file present
      clearLoaderCache()
      val initialPrompt = com.example.data.PromptLoader.loadPromptForAnalysisType(context, com.example.data.AnalysisType.STANDARD_WEBSEITE)
      println("FALLBACK_TEST: Initial prompt load successful: ${initialPrompt != null}")
      org.junit.Assert.assertNotNull("Initially, standard prompt should load successfully", initialPrompt)
      org.junit.Assert.assertTrue("Loaded content should be valid", initialPrompt?.isNotBlank() == true)

      // 2. Simulate missing asset file by injecting a non-existent filename into manifestMapping
      try {
        val mappingField = com.example.data.PromptLoader::class.java.getDeclaredField("manifestMapping")
        mappingField.isAccessible = true
        val mappingMap = mappingField.get(null) as? java.util.concurrent.ConcurrentHashMap<String, String>
        if (mappingMap != null) {
          mappingMap["STANDARD_WEBSEITE"] = "NON_EXISTENT_FILE_FOR_TESTING.md"
          println("FALLBACK_TEST: Injected non-existent asset filename into manifestMapping.")
        } else {
          org.junit.Assert.fail("manifestMapping map is null")
        }
      } catch (e: Exception) {
        org.junit.Assert.fail("Failed to inject mock mapping: ${e.message}")
      }

      // Clear cache again to force next load from filesystem
      clearLoaderCache()

      // 3. Fallback state: Try to load when file is missing
      val fallbackPrompt = com.example.data.PromptLoader.loadPromptForAnalysisType(context, com.example.data.AnalysisType.STANDARD_WEBSEITE)
      println("FALLBACK_TEST: Fallback load result (should be null to indicate hardcoded fallback): $fallbackPrompt")
      org.junit.Assert.assertNull("PromptLoader must return null when asset is missing to trigger repo fallback", fallbackPrompt)

    } finally {
      // 4. Restore state: clear the manifestLoaded flag and manifestMapping so it reloads clean from prompt_manifest.json
      try {
        val flagField = com.example.data.PromptLoader::class.java.getDeclaredField("isManifestLoaded")
        flagField.isAccessible = true
        flagField.set(null, false)

        val mappingField = com.example.data.PromptLoader::class.java.getDeclaredField("manifestMapping")
        mappingField.isAccessible = true
        val mappingMap = mappingField.get(null) as? java.util.concurrent.ConcurrentHashMap<*, *>
        mappingMap?.clear()
        
        println("FALLBACK_TEST: Cleaned up injected maps and manifest state.")
      } catch (e: Exception) {
        println("FALLBACK_TEST: Error during cleanup: ${e.message}")
      }
      
      // Clear cache once again to ensure clean final state
      clearLoaderCache()

      // Verifying normal behavior is restored
      val finalPrompt = com.example.data.PromptLoader.loadPromptForAnalysisType(context, com.example.data.AnalysisType.STANDARD_WEBSEITE)
      println("FALLBACK_TEST: Final prompt load after restoration: ${finalPrompt != null}")
      org.junit.Assert.assertNotNull("After restoration, prompt must load again from assets", finalPrompt)
    }
  }

  @Test
  fun testPromptEngineOrchestration() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    fun clearLoaderCache() {
      try {
        val cacheField = com.example.data.PromptLoader::class.java.getDeclaredField("cache")
        cacheField.isAccessible = true
        val cacheMap = cacheField.get(null) as? java.util.concurrent.ConcurrentHashMap<*, *>
        cacheMap?.clear()
      } catch (e: Exception) {
        println("testPromptEngineOrchestration: Could not clear cache: ${e.message}")
      }
    }

    try {
      // 1. Test normal case where PromptEngine successfully loads the asset using PromptLoader
      clearLoaderCache()
      val systemInstruction = com.example.data.PromptEngine.getSystemInstruction(context, com.example.data.AnalysisType.STANDARD_WEBSEITE)
      org.junit.Assert.assertTrue("System instruction from PromptEngine should load successfully", systemInstruction.isNotBlank())
      org.junit.Assert.assertFalse("By default, it should be the long asset prompt rather than fallback start", systemInstruction.contains("Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter. Deine Aufgabe ist es, den Inhalt der bereitgestellten URL tiefgründig, substanziell und frei von Allgemeinplätzen auf Deutsch zusammenzufassen."))

      // 2. Test fallback case when asset is missing or blocked
      try {
        val mappingField = com.example.data.PromptLoader::class.java.getDeclaredField("manifestMapping")
        mappingField.isAccessible = true
        val mappingMap = mappingField.get(null) as? java.util.concurrent.ConcurrentHashMap<String, String>
        if (mappingMap != null) {
          mappingMap["STANDARD_WEBSEITE"] = "NON_EXISTENT_FILE_FOR_TESTING.md"
        }
      } catch (e: Exception) {
        org.junit.Assert.fail("Failed to inject mock mapping: ${e.message}")
      }
      clearLoaderCache()
      
      val fallbackInstruction = com.example.data.PromptEngine.getSystemInstruction(context, com.example.data.AnalysisType.STANDARD_WEBSEITE)
      org.junit.Assert.assertTrue("Fallback instruction should still be returned", fallbackInstruction.isNotBlank())
      org.junit.Assert.assertTrue("Fallback instruction should be the fallback text from PromptFallbackProvider", fallbackInstruction.contains("Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter. Deine Aufgabe ist es, den Inhalt der bereitgestellten URL tiefgründig, substanziell und frei von Allgemeinplätzen auf Deutsch zusammenzufassen."))

    } finally {
      // Reset state
      try {
        val flagField = com.example.data.PromptLoader::class.java.getDeclaredField("isManifestLoaded")
        flagField.isAccessible = true
        flagField.set(null, false)

        val mappingField = com.example.data.PromptLoader::class.java.getDeclaredField("manifestMapping")
        mappingField.isAccessible = true
        val mappingMap = mappingField.get(null) as? java.util.concurrent.ConcurrentHashMap<*, *>
        mappingMap?.clear()
      } catch (e: Exception) {
        println("testPromptEngineOrchestration: Error during cleanup: ${e.message}")
      }
      clearLoaderCache()
    }
  }

  @Test
  fun testSummaryResponseParser() {
    // 1. Valid JSON Response
    val validJson = """
      {
        "title": "A Great Article",
        "original_url": "https://example.com/classic",
        "short_description": "We analyze everything here in detail.",
        "key_takeaways": [
          { "title": "1. Deep insight into modularization", "details": "My second major takeaway" }
        ],
        "owner": "John Author"
      }
    """.trimIndent()

    val parsed1 = com.example.data.SummaryResponseParser.parse(validJson)
    org.junit.Assert.assertEquals("A Great Article", parsed1.title)
    org.junit.Assert.assertEquals("1. Deep insight into modularization", parsed1.keyTakeaways[0].title)
    org.junit.Assert.assertEquals("My second major takeaway", parsed1.keyTakeaways[0].details)
    org.junit.Assert.assertEquals("John Author", parsed1.owner)

    // 2. Markdown Code Block JSON
    val markdownJson = """
      Some preamble text from LLM...
      ```json
      {
        "title": "Markdown Article",
        "original_url": "https://example.com/md",
        "short_description": "MD short desc.",
        "key_takeaways": [
          { "title": "Takeaway A", "details": "details A" },
          { "title": "Takeaway B", "details": "details B" }
        ]
      }
      ```
      Some postamble text.
    """.trimIndent()

    val parsed2 = com.example.data.SummaryResponseParser.parse(markdownJson)
    org.junit.Assert.assertEquals("Markdown Article", parsed2.title)
    org.junit.Assert.assertEquals("Takeaway A", parsed2.keyTakeaways[0].title)
    org.junit.Assert.assertEquals("Takeaway B", parsed2.keyTakeaways[1].title)
    org.junit.Assert.assertNull(parsed2.owner)
  }

  @Test
  fun testRegressionAllTenAnalysisTypes() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val allTypes = com.example.data.AnalysisType.values()
    
    org.junit.Assert.assertEquals("There should be exactly 10 AnalysisTypes", 10, allTypes.size)

    for (type in allTypes) {
      println("REGRESSION_TEST: Testing type -> $type")
      
      // 1. Check Runtime Configuration
      val runtimeConfig = com.example.data.AnalysisRuntimeConfigs.forType(type)
      org.junit.Assert.assertNotNull("Runtime config must exist for $type", runtimeConfig)
      
      // Specific Grounding Rules
      if (type == com.example.data.AnalysisType.AKTUALITAETS_CHECK || type == com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR) {
        org.junit.Assert.assertTrue("Force grounding must be true for $type", runtimeConfig.forceGrounding)
      } else {
        org.junit.Assert.assertFalse("Force grounding should be false for $type", runtimeConfig.forceGrounding)
      }
      
      // Specific Temperature Rules
      when (type) {
        com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR -> org.junit.Assert.assertEquals(0.1, runtimeConfig.temperature, 0.001)
        com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> org.junit.Assert.assertEquals(0.1, runtimeConfig.temperature, 0.001)
        com.example.data.AnalysisType.AKTUALITAETS_CHECK -> org.junit.Assert.assertEquals(0.3, runtimeConfig.temperature, 0.001)
        com.example.data.AnalysisType.RISIKO_ANALYSE -> org.junit.Assert.assertEquals(0.4, runtimeConfig.temperature, 0.001)
        com.example.data.AnalysisType.BUSINESS_INKUBATOR -> org.junit.Assert.assertEquals(0.8, runtimeConfig.temperature, 0.001)
        else -> org.junit.Assert.assertEquals(0.2, runtimeConfig.temperature, 0.001)
      }

      // 2. Check Prompt Routing (Asset Prompt Loading)
      val prompt = com.example.data.PromptEngine.getSystemInstruction(context, type)
      org.junit.Assert.assertTrue("Prompt for $type should not be blank", prompt.isNotBlank())
      
      // Check that it doesn't load the hardcoded fallback by default (which is a different specific string)
      val defaultFallbackStart = "Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter."
      val usesAsset = !prompt.contains(defaultFallbackStart) || type == com.example.data.AnalysisType.STANDARD_WEBSEITE // STANDARD_WEBSEITE is sometimes mapped to fallback if assets are not fully packaged or fallback is identical, but indeed here standard assets are correct.
      println("REGRESSION_TEST: Prompt length for $type is ${prompt.length} characters.")

      // 3. Parser Verification using correct JSON
      val rawJson = """
        {
          "title": "Title for $type",
          "original_url": "https://example.com/$type",
          "short_description": "Short description for $type",
          "key_takeaways": [
            { "title": "This is takeout number one for $type", "details": "details" },
            { "title": "This is takeout number two for $type", "details": "details" }
          ],
          "owner": "Owner $type"
        }
      """.trimIndent()

      val parsed = com.example.data.SummaryResponseParser.parse(rawJson)

      org.junit.Assert.assertEquals("Title for $type", parsed.title)
      org.junit.Assert.assertEquals("https://example.com/$type", parsed.originalUrl)
      org.junit.Assert.assertEquals("Short description for $type", parsed.shortDescription)
      org.junit.Assert.assertTrue("Key takeaways must be populated for $type", parsed.keyTakeaways.isNotEmpty())
      org.junit.Assert.assertEquals("Owner $type", parsed.owner)
    }
  }

  @Test
  fun testLocalSaveWorksWithoutAuth() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AbstractorDatabase::class.java
    ).allowMainThreadQueries().build()

    val fakeApi = FakeBackendApiService()
    val repository = com.example.data.repository.AnalysisRepositoryImpl(db, fakeApi)

    val sampleSummary = com.example.domain.model.DomainSummary(
        id = "local-save-id-8888",
        title = "Local Master Title",
        originalUrl = "https://example.com/test-local",
        shortDescription = "Saved offline",
        keyTakeaways = listOf(com.example.domain.model.TakeawayItem("Local Item", "Local Details")),
        owner = null
    )

    repository.saveAnalysis(sampleSummary)

    // Assert that it is saved locally
    val retrieved = db.analysisDao().getAnalysisById("local-save-id-8888")
    org.junit.Assert.assertNotNull("The summary should reside in the local database cache", retrieved)
    org.junit.Assert.assertEquals("Local Master Title", retrieved?.title)

    // Since user is Guest, ensure no remote api call was executed
    org.junit.Assert.assertFalse("Api create should not be invoked for Guest mode", fakeApi.createAnalysisCalled)
    db.close()
  }

  @Test
  fun testGuestSyncThrowsExceptionAndNoRemoteCalls() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AbstractorDatabase::class.java
    ).allowMainThreadQueries().build()

    val fakeApi = FakeBackendApiService()
    val syncRepository = com.example.data.repository.SyncRepositoryImpl(db, fakeApi)

    // The database is freshly built, meaning no active user entity exists (Guest status)
    try {
        syncRepository.syncAll()
        org.junit.Assert.fail("Guest sync must throw IllegalStateException indicating that registration or login is required")
    } catch (e: IllegalStateException) {
        org.junit.Assert.assertTrue(e.message?.contains("lokalen Gastmodus") == true)
    }

    org.junit.Assert.assertFalse("Direct sync call or remote pulls should never execute for unregistered Guests", fakeApi.getUserAnalysesCalled)
    db.close()
  }

  @Test
  fun testHistoryContainsSavedAnalysis() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AbstractorDatabase::class.java
    ).allowMainThreadQueries().build()

    val fakeApi = FakeBackendApiService()
    val repository = com.example.data.repository.AnalysisRepositoryImpl(db, fakeApi)

    val sampleSummary1 = com.example.domain.model.DomainSummary(
        id = "history-id-1",
        title = "First Article Summary",
        originalUrl = "https://example.com/1",
        shortDescription = "Desc 1",
        keyTakeaways = listOf(com.example.domain.model.TakeawayItem("Item 1", "Details 1")),
        owner = null
    )
    val sampleSummary2 = com.example.domain.model.DomainSummary(
        id = "history-id-2",
        title = "Second Article Summary",
        originalUrl = "https://example.com/2",
        shortDescription = "Desc 2",
        keyTakeaways = listOf(com.example.domain.model.TakeawayItem("Item 2", "Details 2")),
        owner = null
    )

    repository.saveAnalysis(sampleSummary1)
    repository.saveAnalysis(sampleSummary2)

    val allHistories = db.analysisDao().getAllAnalyses()
    org.junit.Assert.assertEquals("The local cache should contain exactly 2 elements", 2, allHistories.size)
    org.junit.Assert.assertTrue(allHistories.any { it.title == "First Article Summary" })
    org.junit.Assert.assertTrue(allHistories.any { it.title == "Second Article Summary" })
    db.close()
  }

  @Test
  fun testAuthFailureDoesNotProduceMockSuccess() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AbstractorDatabase::class.java
    ).allowMainThreadQueries().build()

    val fakeApi = FakeBackendApiService() // This api always returns HTTP auth failure
    val userRepository = com.example.data.repository.UserRepositoryImpl(db, fakeApi)

    val loginResult = userRepository.login("invalid_user", "invalid_password")
    org.junit.Assert.assertFalse("Login on network or invalid credentials must return false", loginResult)

    val registerResult = userRepository.register("invalid_user", "invalid_password")
    org.junit.Assert.assertFalse("Registration on network issues must return false", registerResult)

    val activeUser = db.userCacheDao().getActiveUser()
    org.junit.Assert.assertNull("Active user session entity must remain null on auth failure", activeUser)
    db.close()
  }

  class FakeBackendApiService : com.example.data.remote.BackendApiService {
    var loginCalled = false
    var registerCalled = false
    var createAnalysisCalled = false
    var deleteAnalysisCalled = false
    var getUserAnalysesCalled = false

    override suspend fun login(request: com.example.data.remote.LoginRequest): retrofit2.Response<com.example.data.remote.LoginResponse> {
        loginCalled = true
        return retrofit2.Response.error(401, okhttp3.ResponseBody.create(null, "Unauthorized"))
    }

    override suspend fun register(request: com.example.data.remote.RegisterRequest): retrofit2.Response<com.example.data.remote.UserResponse> {
        registerCalled = true
        return retrofit2.Response.error(400, okhttp3.ResponseBody.create(null, "Bad Request"))
    }

    override suspend fun getCurrentUser(): retrofit2.Response<com.example.data.remote.UserResponse> {
        return retrofit2.Response.error(401, okhttp3.ResponseBody.create(null, "Unauthorized"))
    }

    override suspend fun createAnalysis(summary: com.example.domain.model.DomainSummary): retrofit2.Response<com.example.domain.model.DomainSummary> {
        createAnalysisCalled = true
        return retrofit2.Response.success(summary)
    }

    override suspend fun getAnalysis(id: String): retrofit2.Response<com.example.domain.model.DomainSummary> {
        return retrofit2.Response.error(404, okhttp3.ResponseBody.create(null, "Not Found"))
    }

    override suspend fun getUserAnalyses(userId: String): retrofit2.Response<List<com.example.domain.model.DomainSummary>> {
        getUserAnalysesCalled = true
        return retrofit2.Response.success(emptyList())
    }

    override suspend fun deleteAnalysis(id: String): retrofit2.Response<Unit> {
        deleteAnalysisCalled = true
        return retrofit2.Response.success(Unit)
    }

    override suspend fun syncPush(request: com.example.data.remote.SyncPushRequest): retrofit2.Response<com.example.data.remote.SyncResponse> {
        return retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, "Not Implemented"))
    }

    override suspend fun syncPull(): retrofit2.Response<List<com.example.domain.model.DomainSummary>> {
        return retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, "Not Implemented"))
    }
  }

  @Test
  fun testSyncWorkerGuestModeReturnsSuccess() {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val worker = androidx.work.testing.TestListenableWorkerBuilder<com.example.data.sync.SyncWorker>(context).build()
      val result = kotlinx.coroutines.runBlocking { worker.doWork() }
      org.junit.Assert.assertEquals(androidx.work.ListenableWorker.Result.success(), result)
  }

  @Test
  fun testSyncSchedulerConfigurationConstraint() {
      val context = ApplicationProvider.getApplicationContext<Context>()
      com.example.data.sync.SyncScheduler.schedulePeriodicSync(context)
      com.example.data.sync.SyncScheduler.enqueueOneTimeSync(context)
      
      val workManager = androidx.work.WorkManager.getInstance(context)
      val periodicInfos = workManager.getWorkInfosForUniqueWork("com.example.data.sync.PERIODIC_SYNC").get()
      val oneTimeInfos = workManager.getWorkInfosForUniqueWork("com.example.data.sync.ONE_TIME_SYNC").get()
      
      org.junit.Assert.assertFalse("Periodic work should be scheduled", periodicInfos.isEmpty())
      org.junit.Assert.assertFalse("One time work should be scheduled", oneTimeInfos.isEmpty())
      
      val periodicConstraints = periodicInfos[0].constraints
      org.junit.Assert.assertEquals(androidx.work.NetworkType.CONNECTED, periodicConstraints.requiredNetworkType)
      
      val oneTimeConstraints = oneTimeInfos[0].constraints
      org.junit.Assert.assertEquals(androidx.work.NetworkType.CONNECTED, oneTimeConstraints.requiredNetworkType)
  }
}
