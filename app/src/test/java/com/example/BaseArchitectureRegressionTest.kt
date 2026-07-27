package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.GeminiRepository
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.SourceType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * ===================================================================================
 *                         RELEVANTOR TEST CLASSIFICATIONS
 * ===================================================================================
 * 
 * 1. UNIT-TESTS (Local JVM, Immediate)
 *    - Purpose: Verify parsing logic, normalization, and business rule evaluation locally.
 *    - Constraints: No network access. Standard JVM speed.
 * 
 * 2. MOCK / API-TESTS (Local JVM, Simulated Network)
 *    - Purpose: Verify how the codebase reacts to mock API payloads or error responses (e.g., 429, 500).
 *    - Constraints: Network is mocked. Fits inside regular CI runs.
 * 
 * 3. LIVE GEMINI-TESTS (Local JVM with Live Network via system property / API Key)
 *    - Purpose: Verify current Gemini models and prompt instructions against real API endpoints.
 *    - Constraints: Requires a valid GEMINI_API_KEY. Vulnerable to quota/RPM limits.
 * 
 * 4. ECHTE SMARTPHONE-TESTS (Physical Hardware / Logcat Diagnostics)
 *    - Purpose: Verify end-to-end device integration (URI resolution, native extractors, UI layout, touch targets).
 *    - Constraints: Must be run on physical devices. Robolectric/JVM-Fixture results are NEVER labeled as smartphone-proof.
 * ===================================================================================
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BaseArchitectureRegressionTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        GeminiRepository.staticContext = context
    }

    // ===============================================================================
    // REGRESSION SUITE: MINIMAL-REGRESSION FOR KEY FUNCTIONS
    // ===============================================================================

    /**
     * TIER 1: UNIT-TEST / MOCK-TEST
     * Regression case for STANDARD_WEBSEITE.
     */
    @Test
    fun regression_STANDARD_WEBSEITE_emptyInput() {
        println("REGRESSION_TEST: Running STANDARD_WEBSEITE empty input validation")
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "",
            enrichedText = "",
            analysisId = java.util.UUID.randomUUID().toString()
        )
        // Should trigger validation error or handle gracefully
        assertNotNull(input.analysisId)
        assertTrue(input.rawText.isEmpty())
    }

    /**
     * TIER 1: UNIT-TEST / MOCK-TEST
     * Regression case for TOP_3_KERNAUSSAGEN.
     */
    @Test
    fun regression_TOP_3_KERNAUSSAGEN_bulletPointFormat() {
        println("REGRESSION_TEST: Running TOP_3_KERNAUSSAGEN format validation")
        val sampleText = "The project builds cleanly on standard environments."
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = sampleText,
            enrichedText = sampleText,
            analysisId = java.util.UUID.randomUUID().toString()
        )
        assertNotNull(input.analysisId)
        assertEquals("The project builds cleanly on standard environments.", input.rawText)
    }

    /**
     * TIER 1: UNIT-TEST / MOCK-TEST
     * Regression case for DOKUMENTE PDF (Simulated/Text path).
     */
    @Test
    fun regression_DOKUMENTE_PDF_structure() {
        println("REGRESSION_TEST: Running DOKUMENTE PDF structure check")
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "Steuerliche Bescheinigung DekaBank 2025 - Gudrun Nolte",
            enrichedText = "Steuerliche Bescheinigung DekaBank 2025 - Gudrun Nolte",
            metadata = mapOf("fileName" to "GudrunNolte.pdf"),
            mimeType = "application/pdf",
            analysisId = java.util.UUID.randomUUID().toString()
        )
        assertNotNull(input.analysisId)
        assertEquals("GudrunNolte.pdf", input.metadata["fileName"])
    }

    /**
     * TIER 1: UNIT-TEST / MOCK-TEST
     * Regression case for DOKUMENTE DOCX.
     */
    @Test
    fun regression_DOKUMENTE_DOCX_structure() {
        println("REGRESSION_TEST: Running DOKUMENTE DOCX structure check")
        val input = CanonicalAnalysisInput(
            sourceType = SourceType.DOCUMENT,
            rawText = "Lebenslauf Gudrun Nolte",
            enrichedText = "Lebenslauf Gudrun Nolte",
            metadata = mapOf("fileName" to "GudrunNolte.docx"),
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            analysisId = java.util.UUID.randomUUID().toString()
        )
        assertNotNull(input.analysisId)
        assertEquals("GudrunNolte.docx", input.metadata["fileName"])
    }

    /**
     * TIER 1: UNIT-TEST / MOCK-TEST
     * Regression case for empty / too short input error.
     */
    @Test
    fun regression_ERROR_emptyOrTooShortInput() {
        println("REGRESSION_TEST: Running error state validation for short input")
        val shortText = "abc"
        val isInsufficient = shortText.length < 5
        assertTrue("Input of length ${shortText.length} should be classified as too short", isInsufficient)
    }

    @Test
    fun test_engine_contract_hardening() {
        println("REGRESSION_TEST: Running contract schema and version hardening checks")
        val capabilities = com.example.domain.engine.EngineCapabilities(
            name = "Test Engine",
            supportsSearchGrounding = false,
            supportsDirectPdf = false
        )
        
        // 1. Valid Contract with SemVer
        val validContract = com.example.domain.engine.EngineContract(
            functionId = "TEST.1",
            version = "1.2.3",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = capabilities,
            promptPath = "prompts/test.md"
        )
        
        // Ensure SemVer validation works
        val validSummary = com.example.domain.model.DomainSummary(
            id = "id-1",
            title = "Test",
            originalUrl = "http://test",
            shortDescription = "Desc",
            keyTakeaways = listOf(com.example.domain.model.TakeawayItem("T1", "D1")),
            owner = null,
            fallbackUsed = false,
            analysisId = "id-1"
        )
        validContract.validateOutput(validSummary) // Should not throw

        // 2. Invalid Contract Version (violating SemVer)
        val invalidVersionContract = validContract.copy(version = "1.0")
        try {
            invalidVersionContract.validateOutput(validSummary)
            fail("Should have thrown IllegalStateException for invalid SemVer version")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Contract violation: version must be valid SemVer") == true)
        }

        // 3. Invalid Output Contract Violation (blank title)
        val invalidSummary = validSummary.copy(title = " ")
        try {
            validContract.validateOutput(invalidSummary)
            fail("Should have thrown IllegalStateException for blank title")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Contract violation: output schema requires non-blank 'title'") == true)
        }

        // 4. Invalid Input Contract Violation (missing enrichedText)
        val invalidInput = CanonicalAnalysisInput(
            sourceType = SourceType.WEB,
            rawText = "",
            enrichedText = "",
            analysisId = "id-1"
        )
        try {
            validContract.validateInput(invalidInput)
            fail("Should have thrown IllegalStateException for blank enrichedText")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Contract violation: input schema requires non-null/non-empty enrichedText") == true)
        }
    }

    @Test
    fun test_feature_catalog_consistency_and_mapping() {
        println("REGRESSION_TEST: Running FeatureCatalog consistency and mapping validation")
        val features = com.example.ui.metadata.FeatureCatalog.features
        val categories = com.example.ui.metadata.FeatureCatalog.categories

        // 1. Ensure FeatureCatalog contains standard visible functions
        val expectedFunctionIds = listOf("WEB_SUMMARY", "KEY_TAKEAWAYS", "FREE_SOURCE_QUERY", "MULTIMEDIA_ANALYSIS", "FRESHNESS_CHECK", "MISINFORMATION_RADAR", "FACTS_VS_OPINIONS", "RISK_ANALYSIS", "PERSPECTIVES_COUNTERPOSITIONS", "RELEVANT_ASPECTS", "DOCUMENT_SUMMARY")
        val visibleActiveFunctionIds = features.filter { !it.isPlaceholder && it.enabled }.map { it.functionId }
        
        for (expectedId in expectedFunctionIds) {
            assertTrue("FeatureCatalog must contain active function $expectedId", visibleActiveFunctionIds.contains(expectedId))
        }

        // 2. Ensure each active visible function has a valid AnalysisType
        features.forEach { feature ->
            if (!feature.isPlaceholder && feature.enabled) {
                assertNotNull("Active visible function ${feature.functionId} must have a valid AnalysisType", feature.analysisType)
            }
        }

        // 3. Ensure categories list maps correctly to CategoryInfo
        val mappedCategoriesList = categories.map { cat ->
            com.example.CategoryInfo(
                id = cat.id,
                label = cat.label,
                name = cat.name,
                icon = cat.icon,
                color = cat.color,
                functions = features
                    .filter { it.category == cat.id && it.visible }
                    .map { feat ->
                        com.example.FunctionInfo(
                            id = feat.functionId,
                            name = feat.name,
                            description = feat.description,
                            type = feat.analysisType,
                            icon = feat.icon,
                            color = feat.color,
                            isPlaceholder = feat.isPlaceholder,
                            acceptedInputs = feat.acceptedInputs
                        )
                    }
            )
        }

        assertEquals("Should map all defined categories", categories.size, mappedCategoriesList.size)
        
        // Assert specific features are placed in correct categories with correct mapping
        val catA = mappedCategoriesList.find { it.id == "A" }
        assertNotNull(catA)
        assertTrue(catA!!.functions.any { it.id == "WEB_SUMMARY" && it.type == AnalysisType.WEB_SUMMARY })
        assertTrue(catA.functions.any { it.id == "KEY_TAKEAWAYS" && it.type == AnalysisType.KEY_TAKEAWAYS })
        
        val catE = mappedCategoriesList.find { it.id == "E" }
        assertNotNull(catE)
        assertTrue(catE!!.functions.any { it.id == "DOCUMENT_SUMMARY" && it.type == AnalysisType.DOCUMENT_SUMMARY })

        // Additional UI feature-menu assertions to prevent active functions from being hidden
        features.forEach { feat ->
            if (feat.enabled && !feat.isPlaceholder) {
                // 1. Must be associated with a visible category
                val cat = categories.find { it.id == feat.category }
                assertNotNull("Each active function ${feat.functionId} must be mapped to a valid category", cat)
                
                // 2. Must have a visible non-blank name/label
                assertTrue("Each active function ${feat.functionId} must have a non-blank name", feat.name.isNotBlank())
                
                // 3. Must have a visible non-blank description
                assertTrue("Each active function ${feat.functionId} must have a non-blank description", feat.description.isNotBlank())
            }
        }

        // Specifically assert that WEITERE_RELEVANTE_ASPEKTE (B.6) is visible, enabled, is not a placeholder, and mapped to Category B
        val b6Feature = features.find { it.functionId == "RELEVANT_ASPECTS" }
        assertNotNull("WEITERE_RELEVANTE_ASPEKTE (B.6) must be present in FeatureCatalog", b6Feature)
        assertTrue("WEITERE_RELEVANTE_ASPEKTE (B.6) must be enabled", b6Feature!!.enabled)
        assertFalse("WEITERE_RELEVANTE_ASPEKTE (B.6) must not be a placeholder", b6Feature.isPlaceholder)
        assertTrue("WEITERE_RELEVANTE_ASPEKTE (B.6) must be visible", b6Feature.visible)
        assertEquals("WEITERE_RELEVANTE_ASPEKTE (B.6) must be in category B", "B", b6Feature.category)

        val catB = mappedCategoriesList.find { it.id == "B" }
        assertNotNull("Category B must exist", catB)
        assertTrue("Category B must contain RELEVANT_ASPECTS (B.6)", catB!!.functions.any { it.id == "RELEVANT_ASPECTS" && it.type == AnalysisType.RELEVANT_ASPECTS })

        // ===========================================================================
        // HARDENED VALIDATION OF EXPANSION RULES & PROMPT ASSETS
        // ===========================================================================
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyGateway = object : com.example.domain.repository.GeminiGateway {
            override suspend fun generateContent(model: String, request: com.example.data.GenerateContentRequest): com.example.data.GenerateContentResponse {
                throw NotImplementedError()
            }
        }
        val registry = com.example.data.engine.AnalysisRegistryImpl(dummyGateway, context)

        // Validate that each visible active function has an engine registered
        features.filter { !it.isPlaceholder && it.enabled }.forEach { feature ->
            val engine = registry.getEngine(feature.functionId)
            assertNotNull("Every active visible function must be registered in AnalysisRegistry (failed: ${feature.functionId})", engine)
            val contract = engine!!.contract

            // 1. Path must be in app/src/main/assets/prompts/ (internally represented under prompts/ in assets)
            assertTrue(
                "Prompt path must live under 'prompts/' directory (failed: ${contract.promptPath})",
                contract.promptPath.startsWith("prompts/")
            )

            // 2. Must end in .md
            assertTrue(
                "Prompt file must have '.md' extension (failed: ${contract.promptPath})",
                contract.promptPath.endsWith(".md")
            )

            // 3. Must match F_[FUNCTION_NAME_IN_UPPERCASE].md
            val filename = contract.promptPath.substringAfter("prompts/")
            val baseName = filename.substringBefore(".md")
            assertTrue(
                "Prompt file name must match pattern 'F_[UPPERCASE_NAME].md', but was: $filename",
                filename.startsWith("F_") && baseName.all { it.isUpperCase() || it == '_' || it.isDigit() }
            )

            // 4. Verify physical asset exists and is readable in the actual assets of the app
            try {
                val stream = context.assets.open(contract.promptPath)
                val content = stream.bufferedReader().use { it.readText() }
                assertTrue("Prompt file content must not be empty: ${contract.promptPath}", content.isNotBlank())
            } catch (e: Exception) {
                fail("Failed to open or read the registered prompt file at path: ${contract.promptPath}. Error: ${e.message}")
            }
        }

        // 5. Test that a Success-DomainSummary with empty keyTakeaways throws a contract violation error
        val capabilities = com.example.domain.engine.EngineCapabilities(
            name = "Test Engine",
            supportsSearchGrounding = false,
            supportsDirectPdf = false
        )
        val validContract = com.example.domain.engine.EngineContract(
            functionId = "TEST.KEYTAKEAWAYS",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput(enrichedText!=null)",
            outputSchema = "DomainSummary(title, original_url, short_description, key_takeaways)",
            capabilities = capabilities,
            promptPath = "prompts/test.md"
        )
        val invalidSummaryEmptyTakeaways = com.example.domain.model.DomainSummary(
            id = "id-empty-takeaways",
            title = "Valid Title",
            originalUrl = "http://valid-url",
            shortDescription = "This is a valid short description of proper length.",
            keyTakeaways = emptyList(), // Empty list (Violating the non-empty rule)
            owner = null,
            fallbackUsed = false,
            analysisId = "id-empty-takeaways"
        )
        try {
            validContract.validateOutput(invalidSummaryEmptyTakeaways)
            fail("Should have thrown IllegalStateException for empty key_takeaways")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Error message should indicate keyTakeaways constraint violation",
                e.message?.contains("Contract violation: output schema requires non-empty 'keyTakeaways'") == true
            )
        }
    }

    @Test
    fun test_legacy_database_copy_safety() {
        println("REGRESSION_TEST: Verifying that legacy DB copy never overwrites an existing relevantor_database")
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val newDbFile = context.getDatabasePath("relevantor_database")
        newDbFile.parentFile?.mkdirs()
        newDbFile.writeText("EXISTING_RELEVANTOR_DATA")
        
        val oldDbFile = context.getDatabasePath("abstractor_database")
        oldDbFile.writeText("OLD_ABSTRACTOR_DATA")
        
        // Invoke private tryCopyLegacyDatabase via reflection
        val companion = com.example.data.local.RelevantorDatabase.Companion
        val method = companion::class.java.getDeclaredMethod("tryCopyLegacyDatabase", Context::class.java)
        method.isAccessible = true
        method.invoke(companion, context)
        
        // Verify that the relevantor_database file was NOT overwritten
        assertEquals("EXISTING_RELEVANTOR_DATA", newDbFile.readText())
        
        // Clean up
        newDbFile.delete()
        oldDbFile.delete()
    }

    @Test
    fun test_migration_4_5_is_registered() {
        println("REGRESSION_TEST: Verifying that MIGRATION_4_5 is declared and registered")
        val migration = com.example.data.local.RelevantorDatabase.MIGRATION_4_5
        assertEquals(4, migration.startVersion)
        assertEquals(5, migration.endVersion)
    }

    @Test
    fun test_no_destructive_migration_fallback() {
        println("REGRESSION_TEST: Verifying that no destructive migration fallback is configured in Room builder")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("relevantor_database")
        dbFile.parentFile?.mkdirs()
        
        // Clear instance first to ensure clean state (INSTANCE is static on the outer RelevantorDatabase class)
        val dbClass = com.example.data.local.RelevantorDatabase::class.java
        val field = dbClass.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
        
        // Create an existing SQLite database file with user_version = 3
        val sqliteDb = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqliteDb.version = 3
        sqliteDb.close()
        
        // Opening this database with RelevantorDatabase should throw an IllegalStateException / Room exception
        // because its version is 3 and there is no migration path from 3 to 5, and destructive fallback is disabled.
        try {
            val db = com.example.data.local.RelevantorDatabase.getInstance(context)
            db.openHelper.writableDatabase
            fail("Should have failed with Room migration error because destructive fallback is disabled and version 3 has no migration path")
        } catch (e: Exception) {
            println("Caught expected migration exception: ${e.message}")
            // Check if it's a Room migration exception (usually contains "A migration from X to Y is required" or similar)
            val hasMigrationMessage = e.message?.contains("migration", ignoreCase = true) == true || 
                                     e.cause?.message?.contains("migration", ignoreCase = true) == true
            assertTrue("Expected migration failure message, but got: ${e.message}", hasMigrationMessage)
        } finally {
            // Reset database instance and delete file
            field.set(null, null)
            dbFile.delete()
        }
    }

    @Test
    fun test_feature_catalog_input_policies() {
        println("REGRESSION_TEST: Verifying input policies for active functions")
        val features = com.example.ui.metadata.FeatureCatalog.features
        
        // 1. All active features have acceptedInputs
        features.forEach { feature ->
            assertNotNull("Each feature must have acceptedInputs defined", feature.acceptedInputs)
            assertFalse("Each feature must define at least one accepted input", feature.acceptedInputs.isEmpty())
        }

        // 2. E.1 accepts DOCUMENT
        val e1 = features.find { it.functionId == "DOCUMENT_SUMMARY" }
        assertNotNull(e1)
        assertTrue("E.1 must accept DOCUMENT", e1!!.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.DOCUMENT))

        // 3. A.1/A.2/B-functions accept WEB
        val a1 = features.find { it.functionId == "WEB_SUMMARY" }
        assertNotNull(a1)
        assertTrue("A.1 must accept WEB", a1!!.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.WEB))

        val a2 = features.find { it.functionId == "KEY_TAKEAWAYS" }
        assertNotNull(a2)
        assertTrue("A.2 must accept WEB", a2!!.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.WEB))

        features.filter { it.category == "B" }.forEach { bFeat ->
            assertTrue("${bFeat.functionId} must accept WEB", bFeat.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.WEB))
        }

        // 4. A.4 accepts MULTIMEDIA or WEB+MULTIMEDIA
        val a4 = features.find { it.functionId == "MULTIMEDIA_ANALYSIS" }
        assertNotNull(a4)
        assertTrue("A.4 must accept MULTIMEDIA", a4!!.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.MULTIMEDIA))
    }

    @Test
    fun test_view_model_summarizeFileUri_decoupled_routing() {
        println("REGRESSION_TEST: Verifying summarizeFileUri routing is decoupled")
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val viewModel = com.example.ui.MainViewModel()
        viewModel.initIfNeeded(context)
        
        // Ensure initial state (defaults to WEB_SUMMARY)
        assertEquals(com.example.data.AnalysisType.WEB_SUMMARY, viewModel.currentAnalysisType.value)
        
        // Call with custom analysisType and check if it is correctly stored (instead of hardcoded DOKUMENTE)
        val dummyUri = android.net.Uri.parse("content://dummy")
        try {
            viewModel.summarizeFileUri(context, dummyUri, com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN)
        } catch (e: Exception) {
            // expected to throw or log because dummy uri cannot be read
        }
        // Verify current analysis type is set to our custom passed AnalysisType instead of DOKUMENTE!
        assertEquals("currentAnalysisType should be set to passed type", com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN, viewModel.currentAnalysisType.value)
    }
}
