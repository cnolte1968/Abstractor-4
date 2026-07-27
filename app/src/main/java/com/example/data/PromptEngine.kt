package com.example.data

import android.content.Context

object PromptEngine {
    fun getSystemInstruction(context: Context, type: AnalysisType): String {
        val basePrompt = PromptLoader.loadPromptForAnalysisType(context, type)
        val globalRules = loadGlobalQualityRules(context)
        return if (globalRules.isNotBlank()) {
            "$basePrompt\n\n=== GLOBAL QUALITY RULES ===\n$globalRules"
        } else {
            basePrompt
        }
    }

    private fun loadGlobalQualityRules(context: Context): String {
        return try {
            context.assets.open("prompts/_global_quality_rules.md").use { input ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            ""
        }
    }
}
