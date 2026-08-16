package com.example.data.diagnostics

import com.example.data.PipelineReport

/**
 * Data structure representing the result of a single run iteration in a smoke/stability test.
 */
data class StabilityRunResult(
    val runIteration: Int,
    val technicalStatus: String, // "PASS", "FAIL"
    val functionalStatus: String, // "PASS", "INSUFFICIENT_CONTENT", "DEGRADED", "FAIL"
    val failureStage: String = "",
    val failureReason: String = "",
    val payloadInputHash: String = "",
    val externalCandidateCount: Int = 0
)

/**
 * Data structure representing the aggregated 3x stability evaluation of a test case.
 */
data class StabilityEvaluationResult(
    val testCaseName: String,
    val runs: List<StabilityRunResult>,
    val stabilityStatus: String, // "STABLE_PASS", "STABLE_FAIL", "FLAKY", "UNKNOWN"
    val firstDivergence: String
)

/**
 * Live-Smoke Stability Gate evaluator for CP-02.
 * Executes classification over 3x iterations of critical test cases.
 */
object LiveSmokeStabilityGate {

    /**
     * Extracts a StabilityRunResult from a finished PipelineReport.
     */
    fun extractRunResult(report: PipelineReport, defaultIteration: Int = 1): StabilityRunResult {
        val iteration = (report.metadata["runIteration"] as? Int)
            ?: (report.final_result["runIteration"] as? Int)
            ?: defaultIteration
        val techStatus = (report.final_result["technicalStatus"] as? String) ?: "FAIL"
        val funcStatus = (report.final_result["functionalStatus"] as? String)
            ?: (report.final_result["finalStatus"] as? String)
            ?: "FAIL"
        val stage = (report.final_result["failureStage"] as? String) ?: ""
        val reason = (report.final_result["semanticOutcomeReason"] as? String)
            ?: (report.final_result["userVisibleErrorMessage"] as? String)
            ?: ""
        val hash = (report.final_result["payloadInputHash"] as? String) ?: ""
        val candidateCount = (report.final_result["externalCandidateCount"] as? Int) ?: 0

        return StabilityRunResult(
            runIteration = iteration,
            technicalStatus = techStatus,
            functionalStatus = funcStatus,
            failureStage = stage,
            failureReason = reason,
            payloadInputHash = hash,
            externalCandidateCount = candidateCount
        )
    }

    /**
     * Classifies a series of runs (expecting N=3) into STABLE_PASS, STABLE_FAIL, or FLAKY.
     *
     * Rules:
     * - 3/3 functional PASS and technical PASS -> STABLE_PASS
     * - 3/3 same functional status (non-PASS) and same failure stage and same technical status -> STABLE_FAIL
     * - Any difference across runs -> FLAKY
     */
    fun classifyRuns(testCaseName: String, runs: List<StabilityRunResult>): StabilityEvaluationResult {
        if (runs.isEmpty()) {
            return StabilityEvaluationResult(testCaseName, runs, "UNKNOWN", "Keine Läufe vorhanden")
        }
        if (runs.size < 3) {
            return StabilityEvaluationResult(testCaseName, runs, "UNKNOWN", "Unvollständige Testserie (${runs.size}/3 Läufe)")
        }

        val funcStatuses = runs.map { it.functionalStatus }
        val techStatuses = runs.map { it.technicalStatus }
        val failureStages = runs.map { it.failureStage }

        // Rule 1: 3/3 functional PASS + technical PASS
        val allPass = funcStatuses.all { it == "PASS" } && techStatuses.all { it == "PASS" }
        if (allPass) {
            return StabilityEvaluationResult(testCaseName, runs, "STABLE_PASS", "Keine (Alle 3 Läufe PASS)")
        }

        // Rule 2: 3/3 identical functional non-PASS status, same stage, same technical status
        val allSameFunc = funcStatuses.distinct().size == 1
        val allSameStage = failureStages.distinct().size == 1
        val allSameTech = techStatuses.distinct().size == 1

        if (allSameFunc && allSameStage && allSameTech && funcStatuses.first() != "PASS") {
            val statusName = funcStatuses.first()
            val stageDesc = if (failureStages.first().isNotEmpty()) " in Stufe '${failureStages.first()}'" else ""
            return StabilityEvaluationResult(
                testCaseName = testCaseName,
                runs = runs,
                stabilityStatus = "STABLE_FAIL",
                firstDivergence = "Keine (Alle 3 Läufe konsistent $statusName$stageDesc)"
            )
        }

        // Rule 3: Divergence found -> FLAKY
        var divergence = "Ergebnisse divergieren zwischen den Läufen"
        for (i in 1 until runs.size) {
            val prev = runs[i - 1]
            val curr = runs[i]
            if (prev.functionalStatus != curr.functionalStatus) {
                divergence = "Run ${curr.runIteration} functionalStatus '${curr.functionalStatus}' weicht von Run ${prev.runIteration} '${prev.functionalStatus}' ab"
                break
            } else if (prev.technicalStatus != curr.technicalStatus) {
                divergence = "Run ${curr.runIteration} technicalStatus '${curr.technicalStatus}' weicht von Run ${prev.runIteration} '${prev.technicalStatus}' ab"
                break
            } else if (prev.failureStage != curr.failureStage) {
                divergence = "Run ${curr.runIteration} failureStage '${curr.failureStage}' weicht von Run ${prev.runIteration} '${prev.failureStage}' ab"
                break
            }
        }

        return StabilityEvaluationResult(
            testCaseName = testCaseName,
            runs = runs,
            stabilityStatus = "FLAKY",
            firstDivergence = divergence
        )
    }

    /**
     * Formats a list of test results into a compact table string according to the specification:
     * Testfall | Run1 | Run2 | Run3 | StabilityStatus | erste Abweichung
     */
    fun formatTable(results: List<StabilityEvaluationResult>): String {
        val sb = StringBuilder()
        sb.appendLine("Testfall | Run1 | Run2 | Run3 | StabilityStatus | erste Abweichung")
        sb.appendLine("---|---|---|---|---|---")
        for (res in results) {
            val r1 = formatRunShort(res.runs.getOrNull(0))
            val r2 = formatRunShort(res.runs.getOrNull(1))
            val r3 = formatRunShort(res.runs.getOrNull(2))
            sb.appendLine("${res.testCaseName} | $r1 | $r2 | $r3 | ${res.stabilityStatus} | ${res.firstDivergence}")
        }
        return sb.toString().trimEnd()
    }

    private fun formatRunShort(run: StabilityRunResult?): String {
        if (run == null) return "N/A"
        val stageSuffix = if (run.failureStage.isNotEmpty() && run.failureStage != "NONE") "@${run.failureStage}" else ""
        return "${run.functionalStatus}$stageSuffix"
    }
}
