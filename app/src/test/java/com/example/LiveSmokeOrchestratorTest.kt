package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.PipelineReportStore
import com.example.data.diagnostics.LiveSmokeStabilityGate
import com.example.data.diagnostics.StabilityEvaluationResult
import com.example.data.diagnostics.StabilityRunResult
import com.example.ui.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LiveSmokeOrchestratorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        com.example.data.GeminiRepository.staticContext = context
    }

    private suspend fun executeSingleLiveRun(
        testCaseName: String,
        url: String,
        analysisType: AnalysisType,
        iteration: Int
    ): StabilityRunResult {
        val viewModel = com.example.ui.MainViewModel()
        viewModel.initIfNeeded(context)

        viewModel.fetchSummary(
            rawUrl = url,
            analysisType = analysisType
        )

        val startTime = System.currentTimeMillis()
        while (viewModel.uiState.value is UiState.Loading || viewModel.uiState.value is UiState.Idle) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            kotlinx.coroutines.delay(50)
            if (System.currentTimeMillis() - startTime > 45000) {
                break
            }
        }

        val report = PipelineReportStore.getReport()!!
        val runId = report.metadata["runId"] as? String ?: "UNKNOWN"
        
        val runResult = LiveSmokeStabilityGate.extractRunResult(report, defaultIteration = iteration)

        println(
            "REAL_LIVE_RUN [#$iteration] - Case: $testCaseName | " +
            "Run-ID: $runId | " +
            "Tech: ${runResult.technicalStatus} | " +
            "Func: ${runResult.functionalStatus} | " +
            "Stage: '${runResult.failureStage}' | " +
            "Reason: '${runResult.failureReason}' | " +
            "Candidates: ${runResult.externalCandidateCount}"
        )

        return runResult
    }

    @Test
    fun executeMapsShortRealLiveSmoke3xGate() = runBlocking {
        println("==========================================================")
        println("=== RELEVANTOR 3X MVP LIVE-SMOKE GATE (MAPS-SHORT) ===")
        println("==========================================================")

        val testCaseName = "Maps-Short / Google Maps Analyzer"
        val url = "https://maps.app.goo.gl/WgXTvya1yCDJjameA"
        val analysisType = AnalysisType.GOOGLE_MAPS_ANALYZER
        
        val runs = mutableListOf<StabilityRunResult>()
        for (i in 1..3) {
            runs.add(executeSingleLiveRun(testCaseName, url, analysisType, i))
        }
        
        val result = LiveSmokeStabilityGate.classifyRuns(testCaseName, runs)

        println("\n=== FINAL MVP LIVE-SMOKE GATE RESULTS ===")
        val table = LiveSmokeStabilityGate.formatTable(listOf(result))
        println(table)
        println("==========================================================")

        assertNotNull(table)
    }
}
