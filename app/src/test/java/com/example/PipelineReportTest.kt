package com.example

import com.example.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PipelineReportTest {

    @Test
    fun testPipelineReportCreatedOnAnalyseBegin() {
        // Reset report store first (simulating clean state)
        // We can't access private field lastReport directly, but we can start a new report
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")
        assertNotNull(report)

        // Verify metadata fields are initialized
        assertEquals("MANUAL_URL", report.metadata["sourceTrigger"])
        assertEquals(true, report.metadata["isManualRun"])
        assertFalse(report.metadata["runId"].toString().isEmpty())

        // Verify pre-defined steps exist and are in status "NOT_RUN"
        val expectedStepIds = listOf(
            "input_intake", "notation_and_id_resolution", "feature_routing",
            "extractor_selection", "url_normalization", "source_network_preflight",
            "source_http_fetch", "html_extraction", "content_cleaning",
            "engine_routing", "prompt_loading", "gemini_request",
            "gemini_response", "response_normalization", "parsing",
            "contract_validation", "rendering", "final_result"
        )

        assertEquals(expectedStepIds.size, report.steps.size)
        for (stepId in expectedStepIds) {
            val step = report.steps.find { it.stepId == stepId }
            assertNotNull("Step $stepId should exist", step)
            assertEquals("Step $stepId should be NOT_RUN", "NOT_RUN", step?.status)
        }
    }

    @Test
    fun testPipelineReportContainsMetadataAndResolution() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")

        // Update notation diagnostics
        PipelineReportStore.updateSection("notation_and_id_resolution") { map ->
            map["originalAnalysisType"] = "STANDARD_WEBSEITE"
            map["canonicalAnalysisType"] = "WEB_SUMMARY"
            map["functionId"] = "WEB_SUMMARY"
            map["engineId"] = "WebpageAnalysisEngine"
            map["legacyTypeDetected"] = true
            map["legacyTypeValue"] = "STANDARD_WEBSEITE"
        }

        // Validate that they are correctly reflected
        assertEquals("STANDARD_WEBSEITE", report.notation_and_id_resolution["originalAnalysisType"])
        assertEquals("WEB_SUMMARY", report.notation_and_id_resolution["canonicalAnalysisType"])
        assertEquals(true, report.notation_and_id_resolution["legacyTypeDetected"])

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val notationJson = json.getJSONObject("notation_and_id_resolution")
        assertEquals("STANDARD_WEBSEITE", notationJson.getString("originalAnalysisType"))
        assertEquals("WEB_SUMMARY", notationJson.getString("canonicalAnalysisType"))
        assertEquals(true, notationJson.getBoolean("legacyTypeDetected"))
    }

    @Test
    fun testPipelineReportFailureHandling() {
        PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")

        PipelineReportStore.startStep("source_http_fetch", "Source HTTP Fetch", "Requesting URL")
        val ioException = java.io.IOException("HTTP_ERROR_404")
        PipelineReportStore.endStepFail("source_http_fetch", ioException, "Failed to reach server")

        // Finalize report status with failure
        PipelineReportStore.updateSection("final_result") { map ->
            map["finalStatus"] = "FAIL"
            map["failureStage"] = "CONTENT_EXTRACTION"
            map["failureStepId"] = "source_http_fetch"
            map["userVisibleErrorTitle"] = "Fehler beim Laden"
            map["pipelineCompleted"] = false
        }

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val finalResult = json.getJSONObject("final_result")
        assertEquals("FAIL", finalResult.getString("finalStatus"))
        assertEquals("CONTENT_EXTRACTION", finalResult.getString("failureStage"))
        assertEquals("source_http_fetch", finalResult.getString("failureStepId"))
        assertFalse(finalResult.getBoolean("pipelineCompleted"))

        val steps = json.getJSONArray("pipeline_steps")
        var fetchStepFound = false
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            if (step.getString("stepId") == "source_http_fetch") {
                fetchStepFound = true
                assertEquals("FAIL", step.getString("status"))
                assertEquals("java.io.IOException", step.getString("exceptionClass"))
                assertEquals("HTTP_ERROR_404", step.getString("exceptionMessage"))
                assertEquals("Failed to reach server", step.getString("notes"))
            }
        }
        assertTrue("HTTP fetch step should be present in JSON", fetchStepFound)
    }

    @Test
    fun testPipelineReportSafety() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")

        // Put some dummy key or long string to verify it is not present or is truncated
        val longString = "A".repeat(5000)
        GatewayDiagnostics.reset()
        GatewayDiagnostics.first1000CharsAfterCleaning = longString
        
        // Context setup is required for populateFromDiagnostics, using Robolectric context
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        PipelineReportStore.populateFromDiagnostics(context)

        val jsonStr = PipelineReportStore.getLastReportJson()
        
        // Assert no API key is present in report
        assertFalse("Should not contain API key pattern", jsonStr.contains("AIzaSy"))
        
        // Assert truncated string is exactly/less than 1000 characters
        val json = JSONObject(jsonStr)
        val cleanedText = json.getJSONObject("content_cleaning").getString("first1000CharsAfterCleaning")
        assertTrue("Should be truncated to 1000 chars", cleanedText.length <= 1000)
    }

    @Test
    fun testMissingReportReturnsDefaultJson() {
        try {
            val field = PipelineReportStore::class.java.getDeclaredField("lastReport")
            field.isAccessible = true
            field.set(null, null)
        } catch (e: Exception) {
            // Ignore if field not found
        }

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)
        assertEquals("NO_PIPELINE_REPORT_AVAILABLE", json.getString("status"))
    }

    @Test
    fun testPipelineReportConsistencyOnContentExtractionFailure() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")
        
        GatewayDiagnostics.reset()
        GatewayDiagnostics.sourceUrl = "https://example.com/missing-page"
        GatewayDiagnostics.sourceHost = "example.com"
        GatewayDiagnostics.sourceDnsOutcome = "FAIL"
        
        // Run some early steps as PASS
        PipelineReportStore.startStep("input_intake", "Input Intake")
        PipelineReportStore.endStepPass("input_intake", "Passed")
        
        PipelineReportStore.startStep("notation_and_id_resolution", "Notation")
        PipelineReportStore.endStepPass("notation_and_id_resolution", "Passed")
        
        PipelineReportStore.startStep("feature_routing", "Routing")
        PipelineReportStore.endStepPass("feature_routing", "Passed")
        
        PipelineReportStore.startStep("extractor_selection", "Extractor")
        PipelineReportStore.endStepPass("extractor_selection", "Passed")
        
        PipelineReportStore.startStep("url_normalization", "Normalization")
        PipelineReportStore.endStepPass("url_normalization", "Passed")
        
        // Preflight fails
        PipelineReportStore.startStep("source_network_preflight", "Source Network Preflight")
        val dnsException = java.io.IOException("DNS resolution failed for example.com")
        PipelineReportStore.endStepFail("source_network_preflight", dnsException, "Preflight failed")
        
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        PipelineReportStore.populateFromDiagnostics(context)
        
        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)
        
        // Assert top level section and step status match
        val preflightSection = json.getJSONObject("source_network_preflight")
        assertEquals("example.com", preflightSection.getString("sourceHost"))
        
        // Check final_result consistency
        val finalResult = json.getJSONObject("final_result")
        assertEquals("FAIL", finalResult.getString("finalStatus"))
        assertEquals("CONTENT_EXTRACTION", finalResult.getString("failureStage"))
        assertEquals("source_network_preflight", finalResult.getString("failureStepId"))
        assertFalse(finalResult.getBoolean("pipelineCompleted"))
        
        // Verify synchronous fail in pipeline_steps
        val steps = json.getJSONArray("pipeline_steps")
        var preflightStepFound = false
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            if (step.getString("stepId") == "source_network_preflight") {
                preflightStepFound = true
                assertEquals("FAIL", step.getString("status"))
                assertEquals("java.io.IOException", step.getString("exceptionClass"))
                assertEquals("DNS resolution failed for example.com", step.getString("exceptionMessage"))
            }
        }
        assertTrue(preflightStepFound)
    }

    @Test
    fun testPipelineReportConsistencyOnNormalSuccessfulRun() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")
        
        GatewayDiagnostics.reset()
        GatewayDiagnostics.sourceUrl = "https://wischnewski-unlimited.com/info"
        GatewayDiagnostics.normalizedSourceUrl = "https://wischnewski-unlimited.com/info"
        GatewayDiagnostics.finalUrl = "https://wischnewski-unlimited.com/info"
        GatewayDiagnostics.sourceHost = "wischnewski-unlimited.com"
        GatewayDiagnostics.sourceDnsOutcome = "SUCCESS"
        GatewayDiagnostics.sourceHttpStatus = 200
        GatewayDiagnostics.rawHtmlLength = 15000
        GatewayDiagnostics.selectedContentContainer = "article"
        GatewayDiagnostics.textBeforeCleaningLength = 10000
        GatewayDiagnostics.textAfterCleaningLength = 4000
        GatewayDiagnostics.loadedAnalysisType = "STANDARD_WEBSEITE"
        GatewayDiagnostics.loadedCanonicalAnalysisType = "WEB_SUMMARY"
        GatewayDiagnostics.loadedFunctionId = "WEB_SUMMARY"
        GatewayDiagnostics.loadedEngineName = "WebpageAnalysisEngine"
        GatewayDiagnostics.loadedPromptAssetFile = "prompts/web_summary.txt"
        GatewayDiagnostics.loadedPromptLength = 1500
        GatewayDiagnostics.rawGeminiResponseLength = 1200
        GatewayDiagnostics.normalizedResponseLength = 1100
        
        val stepIds = listOf(
            "input_intake", "notation_and_id_resolution", "feature_routing",
            "extractor_selection", "url_normalization", "source_network_preflight",
            "source_http_fetch", "html_extraction", "content_cleaning",
            "engine_routing", "prompt_loading", "gemini_request",
            "gemini_response", "response_normalization", "parsing",
            "contract_validation", "rendering", "final_result"
        )
        
        for (stepId in stepIds) {
            PipelineReportStore.startStep(stepId, stepId)
            PipelineReportStore.endStepPass(stepId, "Successful execution of $stepId")
        }
        
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        PipelineReportStore.populateFromDiagnostics(context)
        
        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)
        
        // Assert final status is PASS
        val finalResult = json.getJSONObject("final_result")
        assertEquals("PASS", finalResult.getString("finalStatus"))
        assertTrue(finalResult.getBoolean("pipelineCompleted"))
        assertEquals("NONE", finalResult.getString("failureStage"))
        
        // Assert all steps are PASS
        val steps = json.getJSONArray("pipeline_steps")
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            assertEquals("PASS", step.getString("status"))
        }
        
        // Verify source_network_preflight doesn't report FAIL for the active domains
        val preflightSection = json.getJSONObject("source_network_preflight")
        assertNotEquals("FAIL", preflightSection.getString("wischnewski_unlimited_com_dns"))
        assertNotEquals("FAIL", preflightSection.getString("generativelanguage_googleapis_com_dns"))
    }

    @Test
    fun testPipelineReportConsistencyNewFixes() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")
        
        GatewayDiagnostics.reset()
        GatewayDiagnostics.sourceUrl = "https://example.com/test-article"
        GatewayDiagnostics.normalizedSourceUrl = "https://example.com/test-article"
        GatewayDiagnostics.finalUrl = "https://example.com/test-article"
        GatewayDiagnostics.sourceHost = "example.com"
        GatewayDiagnostics.rawHtmlLength = 25000
        GatewayDiagnostics.bodyReadLength = 25000
        GatewayDiagnostics.selectedContentContainer = "entry-content"
        GatewayDiagnostics.selectedContainerHtmlLength = 12000
        GatewayDiagnostics.selectedContainerTextLength = 5000
        GatewayDiagnostics.textBeforeCleaningLength = 10000
        GatewayDiagnostics.textAfterCleaningLength = 5000
        GatewayDiagnostics.loadedAnalysisType = "KEY_TAKEAWAYS"
        GatewayDiagnostics.loadedCanonicalAnalysisType = "KEY_TAKEAWAYS"
        GatewayDiagnostics.loadedFunctionId = "KEY_TAKEAWAYS"
        GatewayDiagnostics.loadedEngineName = "Top3KeyPointsEngine"
        GatewayDiagnostics.loadedPromptAssetFile = "prompts/key_takeaways.txt"
        GatewayDiagnostics.loadedPromptLength = 1800
        GatewayDiagnostics.rawGeminiResponseLength = 1500
        GatewayDiagnostics.normalizedResponseLength = 1400

        // Start steps
        val stepIds = listOf(
            "input_intake", "notation_and_id_resolution", "feature_routing",
            "extractor_selection", "url_normalization", "source_network_preflight",
            "source_http_fetch", "html_extraction", "content_cleaning",
            "engine_routing", "prompt_loading", "gemini_request",
            "gemini_response", "response_normalization", "parsing",
            "contract_validation", "rendering", "final_result"
        )
        
        for (stepId in stepIds) {
            PipelineReportStore.startStep(stepId, stepId)
            PipelineReportStore.endStepPass(stepId, "Successful execution of $stepId")
        }
        
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        PipelineReportStore.populateFromDiagnostics(context)
        
        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        // 1. Verify bodyReadLength is correctly set and not zero/empty
        val httpFetch = json.getJSONObject("source_http_fetch")
        assertEquals(25000, httpFetch.getInt("bodyReadLength"))

        // 2. Verify selectedContainerHtmlLength and selectedContainerTextLength are correctly set
        val htmlExtraction = json.getJSONObject("html_extraction")
        assertEquals(12000, htmlExtraction.getInt("selectedContainerHtmlLength"))
        assertEquals(5000, htmlExtraction.getInt("selectedContainerTextLength"))

        // 3. Verify featureId and acceptedInputs are populated
        val notationSection = json.getJSONObject("notation_and_id_resolution")
        assertEquals("KEY_TAKEAWAYS", notationSection.getString("featureId"))

        val featureRouting = json.getJSONObject("feature_routing")
        assertEquals("WEB", featureRouting.getString("acceptedInputs"))

        // 4. Verify engine_routing step startedAt/endedAt are not empty
        var engineRoutingStepFound = false
        val steps = json.getJSONArray("pipeline_steps")
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            if (step.getString("stepId") == "engine_routing") {
                engineRoutingStepFound = true
                assertEquals("PASS", step.getString("status"))
                assertFalse("startedAt should not be empty", step.getString("startedAt").isEmpty())
                assertFalse("endedAt should not be empty", step.getString("endedAt").isEmpty())
            }
        }
        assertTrue(engineRoutingStepFound)
    }

    @Test
    fun testPipelineReportCategoryAndEngineRoutingFixes() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_URL")
        
        GatewayDiagnostics.reset()
        GatewayDiagnostics.sourceUrl = "https://example.com/test-article"
        GatewayDiagnostics.normalizedSourceUrl = "https://example.com/test-article"
        GatewayDiagnostics.finalUrl = "https://example.com/test-article"
        GatewayDiagnostics.sourceHost = "example.com"
        GatewayDiagnostics.rawHtmlLength = 25000
        GatewayDiagnostics.bodyReadLength = 25000
        GatewayDiagnostics.selectedContentContainer = "entry-content"
        GatewayDiagnostics.selectedContainerHtmlLength = 12000
        GatewayDiagnostics.selectedContainerTextLength = 5000
        GatewayDiagnostics.textBeforeCleaningLength = 10000
        GatewayDiagnostics.textAfterCleaningLength = 5000
        GatewayDiagnostics.loadedAnalysisType = "WEB_SUMMARY"
        GatewayDiagnostics.loadedCanonicalAnalysisType = "WEB_SUMMARY"
        GatewayDiagnostics.loadedFunctionId = "WEB_SUMMARY"
        GatewayDiagnostics.loadedEngineName = "WebpageAnalysisEngine"
        GatewayDiagnostics.loadedPromptAssetFile = "prompts/web_summary.txt"
        GatewayDiagnostics.loadedPromptLength = 1800
        GatewayDiagnostics.rawGeminiResponseLength = 1500
        GatewayDiagnostics.normalizedResponseLength = 1400

        // Start steps in natural sequence
        val stepIds = listOf(
            "input_intake", "notation_and_id_resolution", "feature_routing",
            "extractor_selection", "url_normalization", "source_network_preflight",
            "source_http_fetch", "html_extraction", "content_cleaning",
            "engine_routing", "prompt_loading", "gemini_request",
            "gemini_response", "response_normalization", "parsing",
            "contract_validation", "rendering", "final_result"
        )
        
        // Simulating sequence timing
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
        var currentTime = System.currentTimeMillis() - 10000
        
        for (stepId in stepIds) {
            val step = report.steps.find { it.stepId == stepId }!!
            step.status = "RUNNING"
            step.startedAt = dateFormat.format(java.util.Date(currentTime))
            currentTime += 100 // increment by 100ms
            step.status = "PASS"
            step.endedAt = dateFormat.format(java.util.Date(currentTime))
            currentTime += 10 // increment for delay between steps
        }
        
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        PipelineReportStore.populateFromDiagnostics(context)
        
        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        // 1. Verify selectedFeatureCategory is mapped to a professional category name and not "A"
        val featureRouting = json.getJSONObject("feature_routing")
        val category = featureRouting.getString("selectedFeatureCategory")
        assertNotEquals("A", category)
        assertEquals("Verstehen & Verdichten", category)

        // 2. Verify engine_routing starts before prompt_loading
        val stepsList = json.getJSONArray("pipeline_steps")
        var engineRoutingStartedAt: java.util.Date? = null
        var promptLoadingStartedAt: java.util.Date? = null
        var engineRoutingDecision: String? = null
        var engineRoutingNextStep: String? = null

        for (i in 0 until stepsList.length()) {
            val step = stepsList.getJSONObject(i)
            if (step.getString("stepId") == "engine_routing") {
                engineRoutingStartedAt = dateFormat.parse(step.getString("startedAt"))
                engineRoutingDecision = step.getString("decision")
                engineRoutingNextStep = step.getString("nextStep")
            } else if (step.getString("stepId") == "prompt_loading") {
                promptLoadingStartedAt = dateFormat.parse(step.getString("startedAt"))
            }
        }

        assertNotNull(engineRoutingStartedAt)
        assertNotNull(promptLoadingStartedAt)
        assertTrue("engine_routing should start before prompt_loading", engineRoutingStartedAt!!.before(promptLoadingStartedAt))
        assertEquals("Load prompt for selected engine", engineRoutingDecision)
        assertEquals("prompt_loading", engineRoutingNextStep)
    }
}
