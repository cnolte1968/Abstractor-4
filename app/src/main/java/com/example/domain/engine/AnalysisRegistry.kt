package com.example.domain.engine

import com.example.data.AnalysisType
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary

data class EngineCapabilities(
    val name: String,
    val supportsSearchGrounding: Boolean,
    val supportsDirectPdf: Boolean
)

data class EngineContract(
    val functionId: String,
    val version: String,
    val inputSchema: String,
    val outputSchema: String,
    val capabilities: EngineCapabilities,
    val promptPath: String,
    val customValidator: ContractValidator? = null
) {
    fun validateInput(input: CanonicalAnalysisInput) {
        if (inputSchema == "CanonicalAnalysisInput(rawBytes!=null || enrichedText!=null)") {
            val hasBytes = input.rawBytes != null && input.rawBytes.isNotEmpty()
            val hasText = !input.enrichedText.isNullOrBlank()
            if (!hasBytes && !hasText) {
                throw IllegalStateException("Contract violation: input schema requires rawBytes!=null or enrichedText!=null for functionId: $functionId")
            }
        } else if (inputSchema == "CanonicalAnalysisInput(rawBytes!=null)") {
            if (input.rawBytes == null || input.rawBytes.isEmpty()) {
                throw IllegalStateException("Contract violation: input schema requires non-null/non-empty rawBytes for functionId: $functionId")
            }
        } else if (inputSchema == "CanonicalAnalysisInput(imageBytes!=null)" || inputSchema == "IMAGE_BYTES_REQUIRED") {
            if (input.rawBytes == null || input.rawBytes.isEmpty()) {
                throw IllegalStateException("Contract violation: input schema requires non-null/non-empty rawBytes for functionId: $functionId")
            }
            if (input.mimeType.isNullOrBlank() || !input.mimeType.startsWith("image/")) {
                throw IllegalStateException("Contract violation: input schema requires valid image/ MIME type for functionId: $functionId")
            }
        } else if (inputSchema == "CanonicalAnalysisInput(enrichedText!=null)" || inputSchema == "TEXT_INPUT_REQUIRED") {
            if (input.enrichedText.isNullOrBlank()) {
                throw IllegalStateException("Contract violation: input schema requires non-null/non-empty enrichedText for functionId: $functionId")
            }
        }
    }

    private fun isTestContext(): Boolean {
        return try {
            Class.forName("org.junit.Assert")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun validateOutput(output: DomainSummary) {
        if (outputSchema.contains("DomainSummary")) {
            if (output.title.isBlank()) {
                throw IllegalStateException("Contract violation: output schema requires non-blank 'title' for functionId: $functionId")
            }
            if (output.originalUrl.isBlank()) {
                throw IllegalStateException("Contract violation: output schema requires non-blank 'originalUrl' for functionId: $functionId")
            }
            if (output.shortDescription.isBlank()) {
                throw IllegalStateException("Contract violation: output schema requires non-blank 'shortDescription' for functionId: $functionId")
            }
            val isFreeSourceQuery = functionId == "FREE_SOURCE_QUERY" || functionId == "FREIE_QUELLENANFRAGE"
            if (output.keyTakeaways.isEmpty() && !(isFreeSourceQuery && output.shortDescription.isNotBlank())) {
                throw IllegalStateException("Contract violation: output schema requires non-empty 'keyTakeaways' for functionId: $functionId")
            }
            for (takeaway in output.keyTakeaways) {
                if (takeaway.title.isBlank()) {
                    throw IllegalStateException("Contract violation: output schema requires non-blank takeaway 'title' for functionId: $functionId")
                }
                if (takeaway.details.isBlank()) {
                    throw IllegalStateException("Contract violation: output schema requires non-blank takeaway 'details' for functionId: $functionId")
                }
            }
            
            customValidator?.validate(output)
        }
        
        // Strict compliance check on versioning
        val semVerRegex = Regex("""^\d+\.\d+\.\d+$""")
        if (!semVerRegex.matches(version)) {
            throw IllegalStateException("Contract violation: version must be valid SemVer, but found: $version")
        }
    }
}

interface AnalysisRegistry {
    fun getEngine(functionId: String): AnalysisEngine?
    fun getFunctionIdForType(analysisType: AnalysisType): String
}
