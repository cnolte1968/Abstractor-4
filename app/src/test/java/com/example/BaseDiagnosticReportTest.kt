package com.example

import com.example.data.PipelineReport
import com.example.data.PipelineReportStore
import com.example.data.diagnostics.LocationContextDiagnosticContributor
import com.example.data.diagnostics.ReportSanitizer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BaseDiagnosticReportTest {

    @Test
    fun testCompleteReportSchemaPresent() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MANUAL_TEST")
        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val mandatoryFields = listOf(
            "report_schema_version",
            "generated_at",
            "metadata",
            "ui_state",
            "current_function",
            "routing",
            "extraction",
            "enrichment",
            "gemini",
            "parsing",
            "contract",
            "rendering",
            "fallbacks",
            "errors",
            "timeline",
            "function_specific_context",
            "redaction_summary",
            "truncation_summary"
        )

        for (field in mandatoryFields) {
            assertTrue("Expected schema field $field in JSON report", json.has(field))
        }

        assertEquals("1.0.0", json.getString("report_schema_version"))
    }

    @Test
    fun testLocationContextContributorActiveOnlyForGoogleMapsLocationContext() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "MAPS_TEST")

        // 1. Function set to WEB_SUMMARY -> Contributor should not apply
        PipelineReportStore.updateSection("notation_and_id_resolution") { map ->
            map["canonicalAnalysisType"] = "WEB_SUMMARY"
            map["functionId"] = "WEB_SUMMARY"
        }

        val jsonStrOther = PipelineReportStore.getLastReportJson()
        val jsonOther = JSONObject(jsonStrOther)
        val funcCtxOther = jsonOther.getJSONObject("function_specific_context")
        assertFalse("location_context should not be present for WEB_SUMMARY", funcCtxOther.has("location_context"))

        // 2. Function set to GOOGLE_MAPS_LOCATION_CONTEXT -> Contributor should apply
        PipelineReportStore.updateSection("notation_and_id_resolution") { map ->
            map["canonicalAnalysisType"] = "GOOGLE_MAPS_LOCATION_CONTEXT"
            map["functionId"] = "GOOGLE_MAPS_LOCATION_CONTEXT"
        }
        PipelineReportStore.updateSection("location_context") { map ->
            map["originalUrl"] = "https://maps.app.goo.gl/sample"
            map["incomingPlaceName"] = "Brandenburger Tor"
            map["parserStatus"] = "SUCCESS"
        }

        val jsonStrMaps = PipelineReportStore.getLastReportJson()
        val jsonMaps = JSONObject(jsonStrMaps)
        val funcCtxMaps = jsonMaps.getJSONObject("function_specific_context")
        assertTrue("location_context should be present for GOOGLE_MAPS_LOCATION_CONTEXT", funcCtxMaps.has("location_context"))

        val locObj = funcCtxMaps.getJSONObject("location_context")
        assertEquals("https://maps.app.goo.gl/sample", locObj.getString("original_url"))
        assertEquals("Brandenburger Tor", locObj.getString("incoming_place_name"))
        assertEquals("SUCCESS", locObj.getString("parser_status"))
    }

    @Test
    fun testSensitiveDataRedaction() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "SECRET_TEST")
        PipelineReportStore.updateSection("gemini_request") { map ->
            map["apiKeyPresent"] = true
            map["apiKey"] = "AIzaSySecretApiKey123456"
            map["authHeader"] = "Bearer secret_token_xyz"
            map["cookieHeader"] = "session_id=12345"
        }

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val geminiReq = json.getJSONObject("gemini").getJSONObject("gemini_request")
        if (geminiReq.has("apiKey")) {
            assertEquals("[REDACTED]", geminiReq.getString("apiKey"))
        }
        if (geminiReq.has("authHeader")) {
            assertEquals("[REDACTED]", geminiReq.getString("authHeader"))
        }

        val redactionSummary = json.getJSONObject("redaction_summary")
        assertTrue(redactionSummary.getInt("redacted_keys_count") > 0)
    }

    @Test
    fun testRawTextSanitizationAndMetadata() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "TEXT_TEST")
        val sampleHtml = "<html><body><h1>Test Content</h1></body></html>"
        PipelineReportStore.updateSection("html_extraction") { map ->
            map["rawHtmlFirst500SafeChars"] = sampleHtml
        }

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val htmlExt = json.getJSONObject("extraction").getJSONObject("html_extraction")
        assertTrue("Should have raw_html_metadata instead of raw html preview", htmlExt.has("raw_html_metadata"))
        val meta = htmlExt.getJSONObject("raw_html_metadata")
        assertEquals(sampleHtml.length, meta.getInt("char_count"))
        assertTrue(meta.getString("sha256").isNotBlank())
        assertEquals("text/html", meta.getString("content_type"))
        assertEquals("LOADED", meta.getString("status"))
    }

    @Test
    fun testReportSizeCapAndMandatoryFields() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "LARGE_TEST")
        // Add huge notes to multiple steps to inflate report size
        for (i in 0 until 50) {
            PipelineReportStore.startStep("step_$i", "Step $i", "Input $i")
            PipelineReportStore.endStepPass("step_$i", "Output $i " + "A".repeat(2000), "Decision $i")
        }

        val jsonStr = PipelineReportStore.getLastReportJson()
        val json = JSONObject(jsonStr)

        val byteSize = jsonStr.toByteArray(Charsets.UTF_8).size
        assertTrue("Report size ($byteSize bytes) must not exceed 65,536 bytes", byteSize <= 65536)

        val truncationSummary = json.getJSONObject("truncation_summary")
        assertNotNull(truncationSummary)
        assertTrue(truncationSummary.getInt("final_size_bytes") <= 65536)

        // Verify mandatory schema fields remain intact
        assertTrue(json.has("report_schema_version"))
        assertTrue(json.has("metadata"))
        assertTrue(json.has("timeline"))
        assertTrue(json.has("errors"))
    }
}
