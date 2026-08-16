package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GatewayDiagnostics
import com.example.data.PipelineReportStore
import com.example.data.diagnostics.LiveSmokeStabilityGate
import com.example.data.diagnostics.StabilityEvaluationResult
import com.example.data.diagnostics.StabilityRunResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveSmokeStabilityGateTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GatewayDiagnostics.reset()
    }

    @Test
    fun testStabilityClassification_StablePass() {
        val runs = listOf(
            StabilityRunResult(runIteration = 1, technicalStatus = "PASS", functionalStatus = "PASS", failureStage = "NONE", payloadInputHash = "hash1"),
            StabilityRunResult(runIteration = 2, technicalStatus = "PASS", functionalStatus = "PASS", failureStage = "NONE", payloadInputHash = "hash1"),
            StabilityRunResult(runIteration = 3, technicalStatus = "PASS", functionalStatus = "PASS", failureStage = "NONE", payloadInputHash = "hash1")
        )

        val result = LiveSmokeStabilityGate.classifyRuns("Web-Summary / General", runs)

        assertEquals("STABLE_PASS", result.stabilityStatus)
        assertEquals(3, result.runs.size)
        assertTrue(result.firstDivergence.contains("Alle 3 Läufe PASS") || result.firstDivergence.contains("Keine"))
    }

    @Test
    fun testStabilityClassification_StableFail_InsufficientContent() {
        val runs = listOf(
            StabilityRunResult(runIteration = 1, technicalStatus = "PASS", functionalStatus = "INSUFFICIENT_CONTENT", failureStage = "NONE", failureReason = "Text too short"),
            StabilityRunResult(runIteration = 2, technicalStatus = "PASS", functionalStatus = "INSUFFICIENT_CONTENT", failureStage = "NONE", failureReason = "Text too short"),
            StabilityRunResult(runIteration = 3, technicalStatus = "PASS", functionalStatus = "INSUFFICIENT_CONTENT", failureStage = "NONE", failureReason = "Text too short")
        )

        val result = LiveSmokeStabilityGate.classifyRuns("Minimal-Page / Top3", runs)

        assertEquals("STABLE_FAIL", result.stabilityStatus)
        assertTrue(result.firstDivergence.contains("konsistent INSUFFICIENT_CONTENT"))
    }

    @Test
    fun testStabilityClassification_StableFail_DegradedMetadata() {
        val runs = listOf(
            StabilityRunResult(runIteration = 1, technicalStatus = "PASS", functionalStatus = "DEGRADED", failureStage = "NONE", failureReason = "Transcript unavailable"),
            StabilityRunResult(runIteration = 2, technicalStatus = "PASS", functionalStatus = "DEGRADED", failureStage = "NONE", failureReason = "Transcript unavailable"),
            StabilityRunResult(runIteration = 3, technicalStatus = "PASS", functionalStatus = "DEGRADED", failureStage = "NONE", failureReason = "Transcript unavailable")
        )

        val result = LiveSmokeStabilityGate.classifyRuns("YT-NoCaption / Summary", runs)

        assertEquals("STABLE_FAIL", result.stabilityStatus)
        assertTrue(result.firstDivergence.contains("konsistent DEGRADED"))
    }

    @Test
    fun testStabilityClassification_Flaky_StatusMismatch() {
        val runs = listOf(
            StabilityRunResult(runIteration = 1, technicalStatus = "PASS", functionalStatus = "PASS", failureStage = "NONE"),
            StabilityRunResult(runIteration = 2, technicalStatus = "PASS", functionalStatus = "FAIL", failureStage = "parsing", failureReason = "STRUCTURED_EXTRACTION_FAILED"),
            StabilityRunResult(runIteration = 3, technicalStatus = "PASS", functionalStatus = "PASS", failureStage = "NONE")
        )

        val result = LiveSmokeStabilityGate.classifyRuns("YouTube-Degraded / Frage an Quelle", runs)

        assertEquals("FLAKY", result.stabilityStatus)
        assertTrue(result.firstDivergence.contains("Run 2 functionalStatus 'FAIL' weicht von Run 1 'PASS' ab"))
    }

    @Test
    fun testStabilityClassification_Flaky_StageMismatch() {
        val runs = listOf(
            StabilityRunResult(runIteration = 1, technicalStatus = "FAIL", functionalStatus = "FAIL", failureStage = "disambiguation", externalCandidateCount = 3),
            StabilityRunResult(runIteration = 2, technicalStatus = "FAIL", functionalStatus = "FAIL", failureStage = "places_fetch", externalCandidateCount = 1),
            StabilityRunResult(runIteration = 3, technicalStatus = "FAIL", functionalStatus = "FAIL", failureStage = "disambiguation", externalCandidateCount = 3)
        )

        val result = LiveSmokeStabilityGate.classifyRuns("Maps-Short / Google Maps Analyzer", runs)

        assertEquals("FLAKY", result.stabilityStatus)
        assertTrue(result.firstDivergence.contains("Run 2 failureStage 'places_fetch' weicht von Run 1 'disambiguation' ab"))
    }

    @Test
    fun testStabilityClassification_FourReferenceCases() {
        // Reference Case 1: Maps-Short / Google Maps Analyzer (Flaky disambiguation scoring)
        val case1Runs = listOf(
            StabilityRunResult(1, "PASS", "PASS", "NONE", externalCandidateCount = 1),
            StabilityRunResult(2, "FAIL", "FAIL", "disambiguation", "TEXT_SEARCH_AMBIGUOUS", externalCandidateCount = 3),
            StabilityRunResult(3, "PASS", "PASS", "NONE", externalCandidateCount = 1)
        )
        val res1 = LiveSmokeStabilityGate.classifyRuns("Maps-Short / Google Maps Analyzer", case1Runs)
        assertEquals("FLAKY", res1.stabilityStatus)

        // Reference Case 2: Maps-Q / Google Maps Analyzer (LocationBias missing -> consistently ambiguous)
        val case2Runs = listOf(
            StabilityRunResult(1, "FAIL", "FAIL", "disambiguation", "TEXT_SEARCH_AMBIGUOUS", externalCandidateCount = 4),
            StabilityRunResult(2, "FAIL", "FAIL", "disambiguation", "TEXT_SEARCH_AMBIGUOUS", externalCandidateCount = 4),
            StabilityRunResult(3, "FAIL", "FAIL", "disambiguation", "TEXT_SEARCH_AMBIGUOUS", externalCandidateCount = 4)
        )
        val res2 = LiveSmokeStabilityGate.classifyRuns("Maps-Q / Google Maps Analyzer", case2Runs)
        assertEquals("STABLE_FAIL", res2.stabilityStatus)

        // Reference Case 3: Maps-Q / Kontext zum Ort (Wikipedia empty context -> groundings vary)
        val case3Runs = listOf(
            StabilityRunResult(1, "PASS", "PASS", "NONE", payloadInputHash = "hashA"),
            StabilityRunResult(2, "PASS", "INSUFFICIENT_CONTENT", "NONE", payloadInputHash = "hashA"),
            StabilityRunResult(3, "PASS", "PASS", "NONE", payloadInputHash = "hashA")
        )
        val res3 = LiveSmokeStabilityGate.classifyRuns("Maps-Q / Kontext zum Ort", case3Runs)
        assertEquals("FLAKY", res3.stabilityStatus)

        // Reference Case 4: YouTube-Degraded / Frage an Quelle (Empty takeaways parsing rejection)
        val case4Runs = listOf(
            StabilityRunResult(1, "PASS", "FAIL", "parsing", "STRUCTURED_EXTRACTION_FAILED"),
            StabilityRunResult(2, "PASS", "FAIL", "parsing", "STRUCTURED_EXTRACTION_FAILED"),
            StabilityRunResult(3, "PASS", "DEGRADED", "NONE", "Metadata fallback")
        )
        val res4 = LiveSmokeStabilityGate.classifyRuns("YouTube-Degraded / Frage an Quelle", case4Runs)
        assertEquals("FLAKY", res4.stabilityStatus)

        val table = LiveSmokeStabilityGate.formatTable(listOf(res1, res2, res3, res4))
        assertTrue(table.contains("Maps-Short / Google Maps Analyzer"))
        assertTrue(table.contains("Maps-Q / Google Maps Analyzer"))
        assertTrue(table.contains("Maps-Q / Kontext zum Ort"))
        assertTrue(table.contains("YouTube-Degraded / Frage an Quelle"))
        println("=== 3X STABILITY GATE TABLE ===")
        println(table)
    }

    @Test
    fun testExtractionFromPipelineReport() {
        val report = PipelineReportStore.startNewReport(sourceTrigger = "SMOKE_RUN", runIteration = 2)
        report.final_result["technicalStatus"] = "PASS"
        report.final_result["functionalStatus"] = "INSUFFICIENT_CONTENT"
        report.final_result["failureStage"] = "source_http_fetch"
        report.final_result["semanticOutcomeReason"] = "No text found"
        report.final_result["payloadInputHash"] = "sha256abc"
        report.final_result["externalCandidateCount"] = 2

        val extracted = LiveSmokeStabilityGate.extractRunResult(report)

        assertEquals(2, extracted.runIteration)
        assertEquals("PASS", extracted.technicalStatus)
        assertEquals("INSUFFICIENT_CONTENT", extracted.functionalStatus)
        assertEquals("source_http_fetch", extracted.failureStage)
        assertEquals("No text found", extracted.failureReason)
        assertEquals("sha256abc", extracted.payloadInputHash)
        assertEquals(2, extracted.externalCandidateCount)
    }
}
