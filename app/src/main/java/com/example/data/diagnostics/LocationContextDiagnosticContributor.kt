package com.example.data.diagnostics

import com.example.data.PipelineReport

class LocationContextDiagnosticContributor : DiagnosticContributor {
    override val contributorId: String = "location_context"

    override fun appliesTo(report: PipelineReport): Boolean {
        val canonicalType = report.notation_and_id_resolution["canonicalAnalysisType"] as? String ?: ""
        val functionId = report.notation_and_id_resolution["functionId"] as? String ?: ""
        val originalType = report.notation_and_id_resolution["originalAnalysisType"] as? String ?: ""
        val featureId = report.notation_and_id_resolution["featureId"] as? String ?: ""
        val routeTarget = report.feature_routing["routeTargetAnalysisType"] as? String ?: ""

        return canonicalType == "GOOGLE_MAPS_LOCATION_CONTEXT" ||
                functionId == "GOOGLE_MAPS_LOCATION_CONTEXT" ||
                originalType == "GOOGLE_MAPS_LOCATION_CONTEXT" ||
                featureId == "GOOGLE_MAPS_LOCATION_CONTEXT" ||
                routeTarget == "GOOGLE_MAPS_LOCATION_CONTEXT" ||
                canonicalType == "LOCATION_CONTEXT" ||
                functionId == "LOCATION_CONTEXT"
    }

    override fun contribute(report: PipelineReport): Map<String, Any?> {
        val loc = report.location_context
        val originalUrl = (loc["originalUrl"] as? String)?.ifEmpty { null }
            ?: (report.url_normalization["rawUrl"] as? String)?.ifEmpty { null }
            ?: (report.input_intake["receivedUrl"] as? String)
            ?: ""
        val normalizedUrl = (loc["normalizedUrl"] as? String)?.ifEmpty { null }
            ?: (report.url_normalization["normalizedSourceUrl"] as? String)
            ?: ""
        val incomingPlaceName = (loc["incomingPlaceName"] as? String)?.ifEmpty { null }
            ?: (report.input_intake["rawInput"] as? String)
            ?: ""
        val resolvedPlaceName = (loc["resolvedPlaceName"] as? String)?.ifEmpty { null }
            ?: (report.parsing["parsedTitle"] as? String)
            ?: ""
        val parserStatus = loc["parserStatus"] as? String ?: "NOT_RUN"
        val wikipediaStatus = loc["wikipediaStatus"] as? String ?: "NOT_RUN"
        val wikivoyageStatus = loc["wikivoyageStatus"] as? String ?: "NOT_RUN"
        val googleMapsBaseStatus = loc["googleMapsBaseStatus"] as? String ?: "NOT_RUN"
        val fallbackUsed = loc["fallbackUsed"] as? Boolean ?: false
        val generatedContextSections = loc["generatedContextSections"] as? List<*> ?: emptyList<Any?>()
        val noContextFound = loc["noContextFound"] as? Boolean ?: false

        return mapOf(
            "original_url" to originalUrl,
            "normalized_url" to normalizedUrl,
            "incoming_place_name" to incomingPlaceName,
            "resolved_place_name" to resolvedPlaceName,
            "parser_status" to parserStatus,
            "wikipedia_status" to wikipediaStatus,
            "wikivoyage_status" to wikivoyageStatus,
            "google_maps_base_status" to googleMapsBaseStatus,
            "fallback_used" to fallbackUsed,
            "generated_context_sections" to generatedContextSections,
            "no_context_found" to noContextFound
        )
    }
}
