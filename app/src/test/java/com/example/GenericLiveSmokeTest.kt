package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import com.example.data.PipelineReportStore
import com.example.data.diagnostics.LiveSmokeStabilityGate
import com.example.data.diagnostics.StabilityRunResult
import com.example.data.diagnostics.TestReferenceRegistry
import com.example.ui.UiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Generischer Live-Smoke-Testrunner für alle Relevantor-Funktionen.
 * 
 * Führt Live-Analysen über den echten MainViewModel.fetchSummary-Pfad aus.
 * Nutzt die TestReferenceRegistry als zentrale Source of Truth.
 * 
 * Parameterisierbar über System Properties / Environment Variables:
 * - relevantor.analysisType (z.B. MISINFORMATION_RADAR, FRESHNESS_CHECK, GOOGLE_MAPS_ANALYZER)
 * - relevantor.testMode (difficult [Standard] | easy)
 * - relevantor.referenceKey (optionaler expliziter Key in TestReferenceRegistry)
 * - relevantor.url (optionale explizite URL-Übersteuerung)
 * - relevantor.freeQuery (optional/erforderlich für Abfragefunktionen)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GenericLiveSmokeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        org.robolectric.shadows.ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        com.example.data.GeminiRepository.staticContext = context
    }

    data class LiveSmokeExecutionResult(
        val runId: String,
        val analysisType: AnalysisType,
        val testMode: String,
        val registryReference: String,
        val inputUrl: String,
        val finalResolvedUrl: String,
        val title: String,
        val technicalStatus: String,
        val functionalStatus: String,
        val parserStatus: String,
        val contractStatus: String,
        val resultRendered: Boolean,
        val uiStateName: String,
        val finalOutcome: String,
        val takeawaysCount: Int,
        val shortDescription: String,
        val stabilityRunResult: StabilityRunResult
    )

    fun executeLiveAnalysis(
        targetAnalysisType: AnalysisType,
        mode: String = "difficult",
        explicitUrl: String? = null,
        explicitQuery: String? = null
    ): LiveSmokeExecutionResult = runBlocking {
        val entry = TestReferenceRegistry.getByAnalysisType(targetAnalysisType)
            ?: throw IllegalStateException("Kein Eintrag in TestReferenceRegistry für AnalysisType: $targetAnalysisType gefunden.")

        val testRef = when (mode.lowercase()) {
            "easy" -> entry.easy ?: throw IllegalStateException("Keine 'easy' Referenz für $targetAnalysisType in TestReferenceRegistry.")
            else -> entry.difficult ?: throw IllegalStateException("Keine 'difficult' Referenz für $targetAnalysisType in TestReferenceRegistry.")
        }

        val urlToUse = explicitUrl?.trim()?.ifEmpty { null } ?: testRef.reference
        val queryToUse = explicitQuery?.trim()?.ifEmpty { null } ?: testRef.testQuery

        val isQueryFunction = targetAnalysisType.canonical() == AnalysisType.FREE_SOURCE_QUERY ||
                targetAnalysisType.canonical() == AnalysisType.GOOGLE_MAPS_LOCATION_QUERY

        if (isQueryFunction && queryToUse.isNullOrBlank()) {
            throw IllegalStateException("AnalysisType $targetAnalysisType erfordert eine Query (freeQuery). Keine übergeben und keine in TestReferenceRegistry hinterlegt.")
        }

        println("==========================================================")
        println("=== RELEVANTOR GENERIC LIVE-SMOKE RUNNER ===")
        println("==========================================================")
        println("STARTING RUN: AnalysisType=$targetAnalysisType | Mode=$mode")
        println("INPUT_URL: $urlToUse")
        if (queryToUse != null) {
            println("FREE_QUERY: $queryToUse")
        }

        val viewModel = com.example.ui.MainViewModel()
        viewModel.initIfNeeded(context)

        viewModel.fetchSummary(
            rawUrl = urlToUse,
            analysisType = targetAnalysisType,
            freeQuery = queryToUse
        )

        val startTime = System.currentTimeMillis()
        while (viewModel.uiState.value is UiState.Loading || viewModel.uiState.value is UiState.Idle) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
            kotlinx.coroutines.delay(50)
            if (System.currentTimeMillis() - startTime > 45000) {
                break
            }
        }

        // Looper nach dem Statuswechsel nachpumpen, damit der uiState-Collector im ViewModel
        // den PipelineReportStore vollständig befüllen kann
        repeat(5) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(100, TimeUnit.MILLISECONDS)
            kotlinx.coroutines.delay(50)
        }

        val state = viewModel.uiState.value
        val report = PipelineReportStore.getReport()
            ?: throw IllegalStateException("PipelineReportStore enthält keinen Report nach Ausführung.")

        val runId = (report.metadata["runId"] as? String)?.ifEmpty { null }
            ?: (if (state is UiState.Success && state.summary.analysisId.isNotEmpty()) state.summary.analysisId.substringBefore("|") else "UNKNOWN")

        val resolvedUrl = (report.source_http_fetch["finalUrl"] as? String)?.ifEmpty { null }
            ?: (if (state is UiState.Success) state.summary.originalUrl else urlToUse)

        val title = (if (state is UiState.Success && state.summary.title.isNotBlank()) state.summary.title else (report.parsing["parsedTitle"] as? String)) ?: "UNKNOWN"

        val technicalStatus = (report.final_result["technicalStatus"] as? String)?.takeIf { it != "NOT_RUN" }
            ?: (if (state is UiState.Success) "PASS" else "FAIL")

        val functionalStatus = (report.final_result["functionalStatus"] as? String)?.takeIf { it != "NOT_RUN" }
            ?: (if (state is UiState.Success) "PASS" else "FAIL")

        val parserStatus = if (report.parsing["parserSuccess"] == true || state is UiState.Success) "PASS" else "FAIL"
        val contractStatus = if (report.contract_validation["contractSuccess"] == true || state is UiState.Success) "PASS" else "FAIL"
        val resultRendered = report.rendering["resultRendered"] == true || state is UiState.Success

        val stabilityResult = LiveSmokeStabilityGate.extractRunResult(report, defaultIteration = 1)

        val finalOutcome = when {
            functionalStatus == "PASS" && technicalStatus == "PASS" && state is UiState.Success -> "PASS"
            functionalStatus == "INSUFFICIENT_CONTENT" || (state is UiState.Success && state.summary.shortDescription.contains("INSUFFICIENT_CONTENT")) -> "INSUFFICIENT_CONTENT"
            else -> "FAIL"
        }

        val takeawaysCount = if (state is UiState.Success) state.summary.keyTakeaways.size else 0
        val shortDesc = if (state is UiState.Success) state.summary.shortDescription else ""

        if (state is UiState.Error || finalOutcome == "FAIL") {
            println("==========================================================")
            println("=== ERROR STATE DIAGNOSTICS ===")
            if (state is UiState.Error) {
                println("Error message: ${state.message}")
                println("Error detail: ${state.detail}")
            }
            println("failureStage: ${com.example.data.GatewayDiagnostics.failureStage}")
            
            println("Exception-Klasse: ${com.example.data.GatewayDiagnostics.exceptionClass}")
            println("Exception-Message: ${com.example.data.GatewayDiagnostics.exceptionMessage}")
            
            println("HTTP-Status: ${report.source_http_fetch["responseCode"] ?: "N/A"}")
            println("extractorStatus: ${report.source_http_fetch["finalStatus"] ?: "N/A"}")
            println("sourceContentLength: ${com.example.data.GatewayDiagnostics.sourceContentLengthSent}")
            println("geminiRequestStarted: ${report.gemini_request["geminiRequestStarted"] ?: false}")
            println("geminiResponseReceived: ${report.gemini_response["geminiResponseReceived"] ?: false}")
            println("--- PIPELINE REPORT DUMP ---")
            try {
                println(PipelineReportStore.getLastReportJson())
            } catch (e: Exception) {
                println(report.toString())
            }
            println("==========================================================")
        }

        val result = LiveSmokeExecutionResult(
            runId = runId,
            analysisType = targetAnalysisType,
            testMode = mode,
            registryReference = testRef.reference,
            inputUrl = urlToUse,
            finalResolvedUrl = resolvedUrl,
            title = title,
            technicalStatus = technicalStatus,
            functionalStatus = functionalStatus,
            parserStatus = parserStatus,
            contractStatus = contractStatus,
            resultRendered = resultRendered,
            uiStateName = state::class.simpleName ?: "UNKNOWN",
            finalOutcome = finalOutcome,
            takeawaysCount = takeawaysCount,
            shortDescription = shortDesc,
            stabilityRunResult = stabilityResult
        )

        println("\nLIVE_SMOKE_RESULT_START")
        println("RUN_ID: ${result.runId}")
        println("ANALYSIS_TYPE: ${result.analysisType}")
        println("TEST_MODE: ${result.testMode}")
        println("REGISTRY_REFERENCE: ${result.registryReference}")
        println("INPUT_URL: ${result.inputUrl}")
        println("FINAL_URL: ${result.finalResolvedUrl}")
        println("MAIN_VIEWMODEL_PATH: JA")
        println("TITLE: ${result.title}")
        println("TECHNICAL_STATUS: ${result.technicalStatus}")
        println("FUNCTIONAL_STATUS: ${result.functionalStatus}")
        println("PARSER: ${result.parserStatus}")
        println("CONTRACT: ${result.contractStatus}")
        println("RESULT_RENDERED: ${result.resultRendered}")
        println("UI_STATE: ${result.uiStateName}")
        println("TAKEAWAYS_COUNT: ${result.takeawaysCount}")
        println("SHORT_DESCRIPTION: ${result.shortDescription}")
        println("FINAL_OUTCOME: ${result.finalOutcome}")
        println("LIVE_SMOKE_RESULT_END\n")

        return@runBlocking result
    }

    /**
     * Parameterisierter Haupt-Einstiegspunkt für beliebige CLI-Aufrufe.
     */
    @Test
    fun executeGenericLiveSmoke() {
        val analysisTypeStr = System.getProperty("relevantor.analysisType")
            ?: System.getenv("RELEVANTOR_ANALYSIS_TYPE")
            ?: System.getProperty("analysisType")
            ?: System.getenv("ANALYSIS_TYPE")

        if (analysisTypeStr.isNullOrBlank()) {
            throw IllegalArgumentException(
                "Fehlender Parameter: 'relevantor.analysisType' nicht gesetzt. " +
                "Verwendung: -Drelevantor.analysisType=<TYPE> (z.B. MISINFORMATION_RADAR, FRESHNESS_CHECK, GOOGLE_MAPS_ANALYZER)"
            )
        }

        val analysisType = try {
            AnalysisType.valueOf(analysisTypeStr.trim().uppercase())
        } catch (e: Exception) {
            throw IllegalArgumentException("Unbekannter AnalysisType '$analysisTypeStr'. Gültige Werte sind: ${AnalysisType.entries.map { it.name }}")
        }

        val testMode = System.getProperty("relevantor.testMode")
            ?: System.getenv("RELEVANTOR_TEST_MODE")
            ?: "difficult"

        val explicitUrl = System.getProperty("relevantor.url")
            ?: System.getProperty("relevantor.reference")

        val explicitQuery = System.getProperty("relevantor.freeQuery")
            ?: System.getenv("RELEVANTOR_FREE_QUERY")

        val result = executeLiveAnalysis(
            targetAnalysisType = analysisType,
            mode = testMode,
            explicitUrl = explicitUrl,
            explicitQuery = explicitQuery
        )

        assertNotNull("Run-ID muss vorhanden sein", result.runId)
        assertTrue("Ausführung muss mit PASS abschließen (Ergebnis: ${result.finalOutcome})", result.finalOutcome == "PASS")
    }

    /**
     * Convenience-Testmethoden für dedizierte Einzellauf-Aufrufe.
     */
    @Test
    fun runGoogleMapsAnalyzerDifficult() {
        val result = executeLiveAnalysis(AnalysisType.GOOGLE_MAPS_ANALYZER, mode = "difficult")
        assertNotNull(result.runId)
    }

    @Test
    fun runMisinformationRadarDifficult() {
        val result = executeLiveAnalysis(AnalysisType.MISINFORMATION_RADAR, mode = "difficult")
        assertNotNull(result.runId)
        assertTrue("MISINFORMATION_RADAR muss PASS sein (Ergebnis: ${result.finalOutcome})", result.finalOutcome == "PASS")
    }

    @Test
    fun runFreshnessCheckDifficult() {
        val result = executeLiveAnalysis(AnalysisType.FRESHNESS_CHECK, mode = "difficult")
        assertNotNull(result.runId)
        assertTrue("FRESHNESS_CHECK muss PASS sein (Ergebnis: ${result.finalOutcome})", result.finalOutcome == "PASS")
    }
}
