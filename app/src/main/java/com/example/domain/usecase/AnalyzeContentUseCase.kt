package com.example.domain.usecase

import android.content.Context
import com.example.data.AnalysisType
import com.example.domain.engine.AnalysisRegistry
import com.example.domain.engine.EngineRunner
import com.example.domain.model.CanonicalAnalysisInput
import com.example.domain.model.DomainSummary
import com.example.domain.repository.AnalysisRepository
import com.example.domain.repository.GeminiGateway

class AnalyzeContentUseCase(
    private val repository: AnalysisRepository,
    private val gateway: GeminiGateway = com.example.data.GeminiRepository,
    private val context: Context? = com.example.data.GeminiRepository.staticContext,
    private val registry: AnalysisRegistry = context?.let {
        com.example.data.engine.AnalysisRegistryImpl(gateway, it)
    } ?: throw IllegalStateException("CRITICAL CONTEXT ERROR: ApplicationContext is not initialized."),
    private val runner: EngineRunner = com.example.data.engine.EngineRunnerImpl()
) {

    suspend fun execute(
        input: CanonicalAnalysisInput,
        useSearchGrounding: Boolean = input.useSearchGrounding,
        analysisType: AnalysisType = input.analysisType,
        freeQuery: String? = input.freeQuery
    ): DomainSummary {
        val configuredInput = input.copy(
            useSearchGrounding = useSearchGrounding,
            analysisType = analysisType,
            freeQuery = freeQuery
        )

        // 1. Resolve Engine from pure mapping registry
        val functionId = registry.getFunctionIdForType(analysisType)
        val engine = registry.getEngine(functionId)
            ?: throw IllegalArgumentException("No engine registered for functionId: $functionId")

        // 2. Execute resolved engine plugin via isolation runner
        val summary = try {
            runner.runEngine(engine, configuredInput)
        } catch (e: Exception) {
            if (useSearchGrounding) {
                android.util.Log.w("AnalyzeContentUseCase", "Search grounding failed, falling back to direct model without grounding", e)
                val fallbackInput = configuredInput.copy(
                    useSearchGrounding = false
                )
                runner.runEngine(engine, fallbackInput)
            } else {
                throw e
            }
        }

        // 3. Save & Return results
        repository.saveAnalysis(summary)
        return summary
    }

    suspend fun executeFromText(
        text: String,
        fileName: String,
        uri: String? = null,
        analysisId: String
    ): DomainSummary {
        val input = CanonicalAnalysisInput(
            sourceType = com.example.domain.model.SourceType.DOCUMENT,
            rawText = text,
            enrichedText = text,
            metadata = mapOf(
                "fileName" to fileName,
                "uri" to (uri ?: "file://$fileName")
            ),
            analysisId = analysisId,
            analysisType = AnalysisType.DOKUMENTE
        )
        return execute(input)
    }

    suspend fun executeFromFile(
        bytes: ByteArray,
        mimeType: String,
        fileName: String,
        uri: String? = null,
        analysisId: String,
        analysisType: AnalysisType = AnalysisType.DOKUMENTE
    ): DomainSummary {
        val input = CanonicalAnalysisInput(
            sourceType = com.example.domain.model.SourceType.DOCUMENT,
            rawText = "",
            enrichedText = "",
            metadata = mapOf(
                "fileName" to fileName,
                "uri" to (uri ?: "file://$fileName")
            ),
            rawBytes = bytes,
            mimeType = mimeType,
            analysisId = analysisId,
            analysisType = analysisType
        )
        return execute(input)
    }

    companion object {
        var USE_DIRECT_PDF_PROCESSING: Boolean
            get() = com.example.data.engine.document.DocumentAnalysisEngine.USE_DIRECT_PDF_PROCESSING
            set(value) {
                com.example.data.engine.document.DocumentAnalysisEngine.USE_DIRECT_PDF_PROCESSING = value
            }
    }
}
