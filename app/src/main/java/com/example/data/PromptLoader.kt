package com.example.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.json.JSONArray

object PromptLoader {
    @JvmField
    var isManifestLoaded = false
    
    @JvmField
    val manifestMapping = ConcurrentHashMap<String, String>()

    @JvmField
    val registryMapping = ConcurrentHashMap<String, FunctionConfig>()
    
    @JvmField
    val cache = ConcurrentHashMap<String, String>()

    fun loadPromptForAnalysisType(context: Context, type: AnalysisType): String {
        val typeStr = type.name
        if (!isManifestLoaded) {
            loadManifest(context)
        }
        val fileName = manifestMapping[typeStr] ?: "prompts/F_${typeStr}.md"
        
        val cached = cache[fileName]
        if (cached != null) return cached
        
        val content = loadFromAssets(context, fileName)
        return if (content != null) {
            cache[fileName] = content
            content
        } else {
            val expectedPath = "assets/$fileName"
            val errorMsg = "CRITICAL PROMPT MISSING: File not found at '$expectedPath' for AnalysisType: $typeStr"
            android.util.Log.e("PromptLoader", errorMsg)
            throw IllegalStateException(errorMsg)
        }
    }

    fun getFunctionConfig(context: Context, typeStr: String): FunctionConfig {
        if (!isManifestLoaded) {
            loadManifest(context)
        }
        return registryMapping[typeStr] ?: FunctionConfig(
            functionId = typeStr,
            label = typeStr,
            promptFile = manifestMapping[typeStr] ?: "prompts/F_${typeStr}.md"
        )
    }

    fun getFunctionConfig(context: Context, type: AnalysisType): FunctionConfig {
        return getFunctionConfig(context, type.name)
    }

    private fun hasAssetFile(context: Context, path: String): Boolean {
        val assetPath = if (path.startsWith("prompts/")) path else "prompts/$path"
        return try {
            context.assets.open(assetPath).use { }
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    private fun loadManifest(context: Context) {
        if (isManifestLoaded) return
        
        val validAnalysisTypes = AnalysisType.values().map { it.name }.toSet()
        
        // Step 1: Initialize mappings with default/fallback locations (just in case files are missing)
        for (type in AnalysisType.values()) {
            val typeStr = type.name
            val promptFile = when (type) {
                AnalysisType.WEB_SUMMARY -> "prompts/F_STANDARD_WEBSEITE.md"
                AnalysisType.KEY_TAKEAWAYS -> "prompts/F_TOP_3_KERNAUSSAGEN.md"
                AnalysisType.RELEVANT_ASPECTS -> "prompts/F_WEITERE_RELEVANTE_ASPEKTE.md"
                AnalysisType.DOCUMENT_SUMMARY -> "prompts/F_DOKUMENTE.md"
                AnalysisType.FREE_SOURCE_QUERY -> "prompts/F_FREIE_QUELLENANFRAGE.md"
                AnalysisType.MULTIMEDIA_ANALYSIS -> "prompts/F_MULTIMEDIA.md"
                AnalysisType.FRESHNESS_CHECK -> "prompts/F_AKTUALITAETS_CHECK.md"
                AnalysisType.MISINFORMATION_RADAR -> "prompts/F_FEHLINFORMATIONS_RADAR.md"
                AnalysisType.FACTS_VS_OPINIONS -> "prompts/F_FACTS_VS_OPINIONS_ANALYZER.md"
                AnalysisType.RISK_ANALYSIS -> "prompts/F_RISIKO_ANALYSE.md"
                AnalysisType.PERSPECTIVES_COUNTERPOSITIONS -> "prompts/F_PERSPECTIVES_AND_COUNTERPOSITIONS.md"
                else -> "prompts/F_${typeStr}.md"
            }
            manifestMapping[typeStr] = promptFile
            
            registryMapping[typeStr] = FunctionConfig(
                functionId = typeStr,
                label = typeStr,
                promptFile = promptFile
            )
        }
        
        var promptManifestLoaded = false
        var functionRegistryLoaded = false
        var promptManifestFormat = "NONE"
        var functionRegistryFormat = "NONE"
        
        // Step 2: Try to load and parse prompt_manifest.json (baseline)
        var manifestJsonStr: String? = null
        try {
            manifestJsonStr = context.assets.open("prompts/prompt_manifest.json").use { input ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            android.util.Log.e("PromptLoader", "Baseline prompt_manifest.json not found or unreadable: ${e.message}")
        }
        
        if (manifestJsonStr != null) {
            try {
                val json = JSONObject(manifestJsonStr)
                val keys = json.keys()
                promptManifestFormat = "JSON_FLAT_MAP"
                while (keys.hasNext()) {
                    val key = keys.next() as String
                    val value = json.getString(key)
                    val fullPath = if (value.startsWith("prompts/")) value else "prompts/$value"
                    
                    // Validate functionId
                    if (!validAnalysisTypes.contains(key)) {
                        android.util.Log.e("PromptLoader", "ERROR: Unknown function_id '$key' in prompt_manifest.json (not matching any AnalysisType)")
                    }
                    
                    // Validate prompt file existence
                    if (!hasAssetFile(context, fullPath)) {
                        android.util.Log.e("PromptLoader", "ERROR: Prompt file '$fullPath' for function '$key' specified in prompt_manifest.json DOES NOT EXIST!")
                    }
                    
                    manifestMapping[key] = fullPath
                    registryMapping[key] = FunctionConfig(
                        functionId = key,
                        label = key,
                        promptFile = fullPath
                    )
                }
                promptManifestLoaded = true
            } catch (e: Exception) {
                // Fallback regex parsing of prompt_manifest.json
                try {
                    val regex = Regex("\"([A-Z0-9_]+)\"\\s*:\\s*\"([^\"]+)\"")
                    var count = 0
                    regex.findAll(manifestJsonStr).forEach { match ->
                        val key = match.groupValues[1]
                        val value = match.groupValues[2]
                        val fullPath = "prompts/$value"
                        
                        // Validate functionId
                        if (!validAnalysisTypes.contains(key)) {
                            android.util.Log.e("PromptLoader", "ERROR: Unknown function_id '$key' in prompt_manifest.json (not matching any AnalysisType)")
                        }
                        
                        // Validate prompt file existence
                        if (!hasAssetFile(context, fullPath)) {
                            android.util.Log.e("PromptLoader", "ERROR: Prompt file '$fullPath' for function '$key' specified in prompt_manifest.json DOES NOT EXIST!")
                        }
                        
                        manifestMapping[key] = fullPath
                        registryMapping[key] = FunctionConfig(
                            functionId = key,
                            label = key,
                            promptFile = fullPath
                        )
                        count++
                    }
                    if (count > 0) {
                        promptManifestFormat = "REGEX_FALLBACK"
                        promptManifestLoaded = true
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("PromptLoader", "Failed to parse prompt_manifest.json via fallback: ${ex.message}")
                }
            }
        }
        
        // Step 3: Try to load and parse function_registry.json (overrides/additions)
        var registryJsonStr: String? = null
        try {
            registryJsonStr = context.assets.open("prompts/function_registry.json").use { input ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            // Optional file, might not exist
        }
        
        if (registryJsonStr != null) {
            try {
                val json = JSONObject(registryJsonStr)
                if (json.has("functions")) {
                    functionRegistryFormat = "NEW_FUNCTIONS_ARRAY"
                    val functionsArray = json.getJSONArray("functions")
                    val length = functionsArray.length()
                    
                    for (i in 0 until length) {
                        val jsonObj = functionsArray.getJSONObject(i)
                        try {
                            val config = parseFunctionConfig(jsonObj)
                            val id = config.functionId
                            
                            // Validate functionId
                            if (!validAnalysisTypes.contains(id)) {
                                android.util.Log.e("PromptLoader", "ERROR: Unknown function_id '$id' in function_registry.json (not matching any AnalysisType)")
                            }
                            
                            // Validate prompt file existence
                            val pf = config.promptFile
                            val fullPath = if (pf.startsWith("prompts/")) pf else "prompts/$pf"
                            if (!hasAssetFile(context, fullPath)) {
                                android.util.Log.e("PromptLoader", "ERROR: Prompt file '$fullPath' for function '$id' specified in function_registry.json DOES NOT EXIST!")
                            }
                            
                            registryMapping[id] = config
                            manifestMapping[id] = fullPath
                        } catch (ex: Exception) {
                            android.util.Log.e("PromptLoader", "Error parsing function config at index $i in function_registry.json: ${ex.message}")
                        }
                    }
                    functionRegistryLoaded = true
                } else {
                    android.util.Log.e("PromptLoader", "ERROR: function_registry.json is missing 'functions' key")
                }
            } catch (e: Exception) {
                android.util.Log.e("PromptLoader", "Failed to parse function_registry.json: ${e.message}")
            }
        }
        
        // Also perform generic check on all final loaded prompts in manifestMapping
        for ((id, fullPath) in manifestMapping) {
            if (!hasAssetFile(context, fullPath)) {
                android.util.Log.e("PromptLoader", "CRITICAL ERROR: Loaded function '$id' has prompt_file '$fullPath' but the file is MISSING in assets!")
            }
        }

        // Print diagnostic logs
        android.util.Log.i("PromptLoader", "==================================================")
        android.util.Log.i("PromptLoader", "Relevantor Function Registry Loading Report")
        android.util.Log.i("PromptLoader", "--------------------------------------------------")
        android.util.Log.i("PromptLoader", "prompt_manifest.json loaded: $promptManifestLoaded (Format: $promptManifestFormat)")
        android.util.Log.i("PromptLoader", "function_registry.json loaded: $functionRegistryLoaded (Format: $functionRegistryFormat)")
        android.util.Log.i("PromptLoader", "Total final merged functions: ${registryMapping.size}")
        android.util.Log.i("PromptLoader", "--------------------------------------------------")
        
        for ((id, config) in registryMapping) {
            val fileExistsStr = if (hasAssetFile(context, config.promptFile)) "EXISTS" else "MISSING"
            val isFromRegistry = functionRegistryLoaded && registryJsonStr?.contains("\"$id\"") == true
            val origin = if (isFromRegistry) "function_registry.json" else if (promptManifestLoaded) "prompt_manifest.json" else "CODE_DEFAULTS"
            
            android.util.Log.i("PromptLoader", "Function ID: $id ($origin)")
            android.util.Log.i("PromptLoader", "  Label: ${config.label}")
            android.util.Log.i("PromptLoader", "  Prompt File: ${config.promptFile} ($fileExistsStr)")
            android.util.Log.i("PromptLoader", "  Rendering Profile: ${config.renderingConfig.renderingProfile}")
            android.util.Log.i("PromptLoader", "  List Style: ${config.renderingConfig.listStyle}")
            android.util.Log.i("PromptLoader", "  Cardinality: min=${config.cardinalityConfig.minItems}, target=${config.cardinalityConfig.targetItems}, max=${config.cardinalityConfig.maxItems}")
            android.util.Log.i("PromptLoader", "  Grounding Mode: ${config.groundingConfig.groundingMode}")
            android.util.Log.i("PromptLoader", "  Error Behavior: ${config.errorBehavior}")
            android.util.Log.i("PromptLoader", "  Input Type: ${config.inputConfig.inputType}")
        }
        android.util.Log.i("PromptLoader", "==================================================")
        
        isManifestLoaded = true
    }

    private fun parseFunctionConfig(jsonObj: JSONObject): FunctionConfig {
        val functionId = jsonObj.optString("function_id", "").ifEmpty {
            jsonObj.optString("functionId", "")
        }
        if (functionId.isEmpty()) {
            throw IllegalArgumentException("Missing required field: function_id")
        }
        
        val promptFile = jsonObj.optString("prompt_file", "").ifEmpty {
            jsonObj.optString("promptFile", "")
        }.ifEmpty {
            "F_${functionId}.md"
        }

        val label = jsonObj.optString("label", functionId)
        val subtitle = jsonObj.optString("subtitle", "")
        val category = jsonObj.optString("category", "GENERAL")
        val categoryLabel = jsonObj.optString("category_label", "Allgemein")
        val enabled = jsonObj.optBoolean("enabled", true)
        val favoriteDefault = jsonObj.optBoolean("favorite_default", false)

        // Input Config
        val inputType = jsonObj.optString("input_type", "URL_OR_TEXT")
        val acceptedMimeTypesList = mutableListOf<String>()
        val acceptedMimeTypesArray = jsonObj.optJSONArray("accepted_mime_types")
        if (acceptedMimeTypesArray != null) {
            for (i in 0 until acceptedMimeTypesArray.length()) {
                acceptedMimeTypesList.add(acceptedMimeTypesArray.getString(i))
            }
        }
        val preferredSourceOrderList = mutableListOf<String>()
        val preferredSourceOrderArray = jsonObj.optJSONArray("preferred_source_order")
        if (preferredSourceOrderArray != null) {
            for (i in 0 until preferredSourceOrderArray.length()) {
                preferredSourceOrderList.add(preferredSourceOrderArray.getString(i))
            }
        }
        val supportsUrl = jsonObj.optBoolean("supports_url", true)
        val supportsText = jsonObj.optBoolean("supports_text", true)
        val supportsFile = jsonObj.optBoolean("supports_file", false)
        val supportsBinaryPayload = jsonObj.optBoolean("supports_binary_payload", false)
        val supportsTextExtraction = jsonObj.optBoolean("supports_text_extraction", true)
        val maxInputChars = jsonObj.optInt("max_input_chars", 15000)
        val maxFileSizeMb = jsonObj.optInt("max_file_size_mb", 10)

        val inputConfig = InputConfig(
            inputType = inputType,
            acceptedMimeTypes = acceptedMimeTypesList,
            preferredSourceOrder = preferredSourceOrderList,
            supportsUrl = supportsUrl,
            supportsText = supportsText,
            supportsFile = supportsFile,
            supportsBinaryPayload = supportsBinaryPayload,
            supportsTextExtraction = supportsTextExtraction,
            maxInputChars = maxInputChars,
            maxFileSizeMb = maxFileSizeMb
        )

        // Rendering Config
        val renderingProfile = jsonObj.optString("rendering_profile", "summary_bullets")
        val listStyle = jsonObj.optString("list_style", "BULLET")
        val renderingConfig = RenderingConfig(
            renderingProfile = renderingProfile,
            listStyle = listStyle
        )

        // Cardinality Config
        val minItems = jsonObj.optInt("min_items", 1)
        val targetItems = jsonObj.optInt("target_items", 3)
        val maxItems = jsonObj.optInt("max_items", 10)
        val allowFewerItems = jsonObj.optBoolean("allow_fewer_items", true)
        val forbidPadding = jsonObj.optBoolean("forbid_padding", true)
        val cardinalityConfig = CardinalityConfig(
            minItems = minItems,
            targetItems = targetItems,
            maxItems = maxItems,
            allowFewerItems = allowFewerItems,
            forbidPadding = forbidPadding
        )

        // Grounding Config
        val groundingMode = jsonObj.optString("grounding_mode", "OPTIONAL")
        val groundingConfig = GroundingConfig(
            groundingMode = groundingMode
        )

        val errorBehavior = jsonObj.optString("error_behavior", "ANALYSIS_ERROR")
        val fallbackBehavior = jsonObj.optString("fallback_behavior", "NONE")
        val ownerRequired = jsonObj.optBoolean("owner_required", false)
        val originalUrlRequired = jsonObj.optBoolean("original_url_required", false)
        val showOwner = jsonObj.optBoolean("show_owner", true)
        val showSource = jsonObj.optBoolean("show_source", true)
        val showTimestamp = jsonObj.optBoolean("show_timestamp", true)
        val pdfExportEnabled = jsonObj.optBoolean("pdf_export_enabled", true)

        return FunctionConfig(
            functionId = functionId,
            label = label,
            subtitle = subtitle,
            category = category,
            categoryLabel = categoryLabel,
            promptFile = promptFile,
            enabled = enabled,
            favoriteDefault = favoriteDefault,
            inputConfig = inputConfig,
            outputSchema = jsonObj.optString("output_schema", "relevantorSummarySchema"),
            renderingConfig = renderingConfig,
            cardinalityConfig = cardinalityConfig,
            groundingConfig = groundingConfig,
            errorBehavior = errorBehavior,
            fallbackBehavior = fallbackBehavior,
            ownerRequired = ownerRequired,
            originalUrlRequired = originalUrlRequired,
            showOwner = showOwner,
            showSource = showSource,
            showTimestamp = showTimestamp,
            pdfExportEnabled = pdfExportEnabled
        )
    }

    private fun loadFromAssets(context: Context, path: String): String? {
        return try {
            val assetPath = if (path.startsWith("prompts/")) path else "prompts/$path"
            context.assets.open(assetPath).use { input ->
                input.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
