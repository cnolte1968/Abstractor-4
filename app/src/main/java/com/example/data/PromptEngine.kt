package com.example.data

import android.content.Context
import android.util.Log

object PromptEngine {
    private const val TAG = "PromptEngine"

    fun getSystemInstruction(
        context: Context?,
        analysisType: AnalysisType
    ): String {
        val assetPrompt = PromptLoader.loadPromptForAnalysisType(context, analysisType)
        val isAssetUsed = assetPrompt != null
        val functionPrompt = if (assetPrompt != null) {
            Log.d(TAG, "PromptEngine: Loaded asset prompt for $analysisType")
            assetPrompt
        } else {
            Log.d(TAG, "PromptEngine: Using hardcoded fallback prompt for $analysisType")
            PromptFallbackProvider.getFallbackSystemInstruction(analysisType)
        }

        val globalRules = PromptLoader.loadGlobalQualityRules(context)

        return if (globalRules != null) {
            Log.d(TAG, "PromptEngine: Successfully loaded global quality rules (length: ${globalRules.length} chars). Combining with function prompt.")
            val separator = "\n\n---\n\n# FUNCTION SPECIFIC PROMPT\n\n---\n\n"
            val combined = globalRules + separator + functionPrompt
            Log.d(TAG, "PromptEngine: Orchestration success for $analysisType. Combined prompt length: ${combined.length} chars (assetUsed=$isAssetUsed)")
            combined
        } else {
            Log.w(TAG, "PromptEngine: Global quality rules could not be loaded; using raw function prompt for $analysisType (assetUsed=$isAssetUsed)")
            functionPrompt
        }
    }
}
