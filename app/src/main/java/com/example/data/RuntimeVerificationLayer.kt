package com.example.data

import android.util.Log
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem

object RuntimeVerificationLayer {
    private const val TAG = "RuntimeVerification"

    data class VerificationContext(
        val functionId: String,
        val promptHash: String,
        val analysisType: AnalysisType,
        val sourceUrl: String
    )

    data class ValidationResult(
        val isValid: Boolean,
        val failureReason: String? = null
    )

    fun validate(summary: DomainSummary, context: VerificationContext): ValidationResult {
        Log.i(TAG, "Starting validation for function: ${context.functionId} (${context.analysisType})")

        // 1. Core structural check
        if (summary.title.isBlank()) {
            return ValidationResult(false, "Title is empty or blank")
        }
        if (summary.keyTakeaways.isEmpty()) {
            return ValidationResult(false, "Key takeaways list is empty")
        }

        // Check for fallback placeholders or raw JSON in takeaways to trigger engine retry if parsing failed
        for (item in summary.keyTakeaways) {
            val titleClean = item.title.trim()
            val detailsClean = item.details.trim()
            
            if (detailsClean.startsWith("{") && detailsClean.contains("\"") && (detailsClean.contains(":") || detailsClean.contains("}"))) {
                return ValidationResult(false, "Takeaway details contains raw unparsed JSON: $detailsClean")
            }
            if (titleClean == "Analyse" && detailsClean.startsWith("{")) {
                return ValidationResult(false, "Takeaway title is 'Analyse' and contains raw JSON structure")
            }
            if (titleClean == "Inhalt" && detailsClean.startsWith("{")) {
                return ValidationResult(false, "Takeaway title is 'Inhalt' and contains raw JSON structure")
            }
            if (summary.keyTakeaways.size == 1) {
                if (titleClean == "Analyse" || titleClean == "Inhalt") {
                    if (detailsClean.contains("original_url") || detailsClean.contains("key_takeaways") || detailsClean.contains("short_description") || detailsClean.contains("\"title\"")) {
                        return ValidationResult(false, "Fallback takeaway detected containing raw JSON keys")
                    }
                }
            }
        }

        // 2. Function-specific checks
        when (context.analysisType) {
            AnalysisType.TOP_3_KERNAUSSAGEN, AnalysisType.KEY_TAKEAWAYS -> {
                if (summary.keyTakeaways.size !in 1..3) {
                    return ValidationResult(
                        false,
                        "TOP_3_KERNAUSSAGEN: Expected 1 to 3 takeaways, got ${summary.keyTakeaways.size}"
                    )
                }
                for (item in summary.keyTakeaways) {
                    if (hasNumbering(item.title) || hasNumbering(item.details)) {
                        return ValidationResult(
                            false,
                            "TOP_3_KERNAUSSAGEN: Numbered entries are strictly prohibited"
                        )
                    }
                }
            }

            AnalysisType.STANDARD_WEBSEITE, AnalysisType.WEB_SUMMARY, AnalysisType.DOKUMENTE, AnalysisType.MULTIMEDIA -> {
                for (item in summary.keyTakeaways) {
                    if (hasNumbering(item.title) || hasNumbering(item.details)) {
                        return ValidationResult(
                            false,
                            "Summary function ${context.analysisType}: Numbered entries are prohibited. Only bullet points allowed."
                        )
                    }
                }
            }

            AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> {
                // The output is represented as items in keyTakeaways. Let's find tags: [F], [M], [V], [W], [S]
                val combinedText = summary.keyTakeaways.joinToString(" ") { it.title + " " + it.details }
                
                val hasFacts = combinedText.contains("[F]") || combinedText.contains("Fakt", ignoreCase = true)
                val hasOpinions = combinedText.contains("[M]") || combinedText.contains("Meinung", ignoreCase = true)
                val hasUncertainties = combinedText.contains("[V]") || combinedText.contains("[S]") || 
                                       combinedText.contains("Vermutung", ignoreCase = true) || combinedText.contains("Spekulation", ignoreCase = true)

                if (!hasFacts) {
                    return ValidationResult(false, "FACTS_VS_OPINIONS: Missing facts classification [F]")
                }
                if (!hasOpinions) {
                    return ValidationResult(false, "FACTS_VS_OPINIONS: Missing opinions classification [M]")
                }
                if (!hasUncertainties) {
                    return ValidationResult(false, "FACTS_VS_OPINIONS: Missing uncertainties/speculations classification [V]/[S]")
                }
            }

            AnalysisType.MULTIMEDIA -> {
                val isYoutube = context.sourceUrl.contains("youtube.com", ignoreCase = true) || 
                                context.sourceUrl.contains("youtu.be", ignoreCase = true)
                val isTranscript = context.sourceUrl.contains("transcript", ignoreCase = true) || 
                                   summary.title.contains("transcript", ignoreCase = true) ||
                                   summary.title.contains("video", ignoreCase = true) ||
                                   summary.title.contains("podcast", ignoreCase = true)

                if (!isYoutube && !isTranscript) {
                    // Log but don't fail, to be resilient, or enforce fallback if requested
                    Log.w(TAG, "MULTIMEDIA: Source URL does not point to Youtube or a transcript context")
                }
            }

            else -> {}
        }

        Log.i(TAG, "Validation passed for function: ${context.functionId}")
        return ValidationResult(true)
    }

    private fun hasNumbering(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.contains(Regex("^\\s*\\d+[:\\.)]"))
    }
}
