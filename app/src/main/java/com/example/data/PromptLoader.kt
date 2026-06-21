package com.example.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

object PromptLoader {
    private const val TAG = "PromptLoader"
    private const val PROMPTS_DIR = "prompts"
    private const val MANIFEST_FILE = "prompts/prompt_manifest.json"

    private val cache = ConcurrentHashMap<AnalysisType, String>()
    private val manifestMapping = ConcurrentHashMap<String, String>()
    private var isManifestLoaded = false
    private var appCtx: Context? = null
    @Volatile
    private var cachedGlobalQualityRules: String? = null

    private val FALLBACK_MAPPING = mapOf(
        AnalysisType.STANDARD_WEBSEITE to "F_STANDARD_WEBSEITE.md",
        AnalysisType.MULTIMEDIA to "F_MULTIMEDIA.md",
        AnalysisType.DOKUMENTE to "F_DOKUMENTE.md",
        AnalysisType.TOP_3_KERNAUSSAGEN to "F_TOP_3_KERNAUSSAGEN.md",
        AnalysisType.AKTUALITAETS_CHECK to "F_AKTUALITAETS_CHECK.md",
        AnalysisType.FEHLINFORMATIONS_RADAR to "F_FEHLINFORMATIONS_RADAR.md",
        AnalysisType.RISIKO_ANALYSE to "F_RISIKO_ANALYSE.md",
        AnalysisType.BUSINESS_INKUBATOR to "F_BUSINESS_INKUBATOR.md",
        AnalysisType.FACTS_VS_OPINIONS_ANALYZER to "F_FACTS_VS_OPINIONS_ANALYZER.md",
        AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS to "F_PERSPECTIVES_AND_COUNTERPOSITIONS.md"
    )

    fun init(context: Context) {
        appCtx = context.applicationContext
    }

    @Synchronized
    private fun loadManifestIfNeeded(context: Context) {
        if (isManifestLoaded) return
        try {
            val assetManager = context.assets
            val stream = assetManager.open(MANIFEST_FILE)
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val jsonText = reader.use { it.readText() }
            
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val analysisTypeStr = obj.optString("analysis_type")
                val filename = obj.optString("file")
                if (analysisTypeStr.isNotEmpty() && filename.isNotEmpty()) {
                    manifestMapping[analysisTypeStr] = filename
                }
            }
            isManifestLoaded = true
            Log.d(TAG, "Successfully loaded $MANIFEST_FILE from assets with ${manifestMapping.size} entries")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load/parse $MANIFEST_FILE; falling back to hardcoded mapping. Error: ${e.message}")
            isManifestLoaded = true // avoid repeating failed reads
        }
    }

    fun loadPromptForAnalysisType(context: Context?, analysisType: AnalysisType): String? {
        val targetContext = context ?: appCtx
        if (targetContext == null) {
            Log.w(TAG, "PromptLoader: Asset prompt unavailable for $analysisType (no context); using hardcoded fallback")
            return null
        }

        val cached = cache[analysisType]
        if (cached != null) {
            return cached
        }

        loadManifestIfNeeded(targetContext)

        val filenameInManifest = manifestMapping[analysisType.name]
        val filename = if (filenameInManifest != null) {
            filenameInManifest
        } else {
            Log.d(TAG, "PromptLoader: Mapping for $analysisType not found in manifest; using internal hardcoded mapping fallback: ${FALLBACK_MAPPING[analysisType]}")
            FALLBACK_MAPPING[analysisType]
        }

        if (filename == null) {
            Log.w(TAG, "PromptLoader: No asset filename mapped (even in internal fallback) for $analysisType; using hardcoded fallback")
            return null
        }

        return try {
            val assetManager = targetContext.assets
            val stream = assetManager.open("$PROMPTS_DIR/$filename")
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val promptContent = reader.use { it.readText() }
            
            if (promptContent.isNotBlank()) {
                cache[analysisType] = promptContent
                Log.d(TAG, "PromptLoader: Loaded asset prompt for $analysisType from $filename")
                promptContent
            } else {
                Log.w(TAG, "PromptLoader: Asset prompt file '$filename' for $analysisType is blank/empty; using hardcoded fallback")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "PromptLoader: Failed to load asset file '$filename' for $analysisType; using hardcoded fallback. Error: ${e.message}")
            null
        }
    }

    fun loadGlobalQualityRules(context: Context?): String? {
        val targetContext = context ?: appCtx
        if (targetContext == null) {
            Log.w(TAG, "PromptLoader: Global quality rules unavailable (no context)")
            return null
        }

        val cached = cachedGlobalQualityRules
        if (cached != null) {
            return cached
        }

        val filename = "_global_quality_rules.md"
        return try {
            val assetManager = targetContext.assets
            val stream = assetManager.open("$PROMPTS_DIR/$filename")
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val rulesContent = reader.use { it.readText() }

            if (rulesContent.isNotBlank()) {
                cachedGlobalQualityRules = rulesContent
                Log.d(TAG, "PromptLoader: Loaded global quality rules from $filename")
                rulesContent
            } else {
                Log.w(TAG, "PromptLoader: Global quality rules file '$filename' is blank/empty")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "PromptLoader: Failed to load global quality rules file '$filename'. Error: ${e.message}")
            null
        }
    }
}
