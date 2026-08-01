package com.example.data.diagnostics

import com.example.data.PipelineReport

interface DiagnosticContributor {
    val contributorId: String
    fun appliesTo(report: PipelineReport): Boolean
    fun contribute(report: PipelineReport): Map<String, Any?>
}
