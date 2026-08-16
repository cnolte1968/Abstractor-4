package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.AnalysisRegistryImpl
import com.example.domain.engine.PromptAssetLoader
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PromptAssetValidationTest {

    private val mockContext: Context by lazy { ApplicationProvider.getApplicationContext<Context>() }

    private val registry: AnalysisRegistryImpl by lazy { 
        AnalysisRegistryImpl(
            gateway = object : com.example.domain.repository.GeminiGateway { override suspend fun generateContent(model: String, request: com.example.data.GenerateContentRequest): com.example.data.GenerateContentResponse = com.example.data.GenerateContentResponse(emptyList()) }, 
            context = mockContext, 
            promptAssetLoader = object : PromptAssetLoader {
                override fun loadAsset(path: String): String = "mock"
            }
        ) 
    }

    @Test
    fun testAllRegisteredEnginesHaveValidPromptAssets() {
        val analysisTypes = AnalysisType.values()
        for (type in analysisTypes) {
            val functionId = registry.getFunctionIdForType(type)
            val engine = registry.getEngine(functionId)
            assertNotNull("AnalysisType $type maps to functionId $functionId which has no registered Engine", engine)

            val contract = engine!!.contract
            assertNotNull("Contract for functionId $functionId is null", contract)
            assertTrue("Input schema for $functionId is empty", contract.inputSchema.isNotBlank())
            assertTrue("Output schema for $functionId is empty", contract.outputSchema.isNotBlank())

            val promptPath = contract.promptPath
            assertTrue("Prompt path for $functionId is empty", promptPath.isNotBlank())

            val promptFile = File("src/main/assets", promptPath)
            assertTrue("Prompt file missing for $functionId: ${promptFile.absolutePath}", promptFile.exists())
            assertTrue("Prompt file is empty for $functionId", promptFile.readText().isNotBlank())
        }
    }

    @Test
    fun testPromptManifestHasValidFiles() {
        val manifestFile = File("src/main/assets/prompts/prompt_manifest.json")
        assertTrue("prompt_manifest.json must exist", manifestFile.exists())

        val content = manifestFile.readText()
        val json = JSONObject(content)
        val keys = json.keys()
        
        while (keys.hasNext()) {
            val key = keys.next()
            val filename = json.getString(key)
            val promptFile = File("src/main/assets/prompts", filename)
            assertTrue("Manifest entry $key points to missing file $filename", promptFile.exists())
        }
    }

    @Test
    fun testFunctionRegistryHasValidFiles() {
        val registryFile = File("src/main/assets/prompts/function_registry.json")
        assertTrue("function_registry.json must exist", registryFile.exists())

        val content = registryFile.readText()
        val json = JSONObject(content)
        val functions = json.getJSONArray("functions")
        
        for (i in 0 until functions.length()) {
            val func = functions.getJSONObject(i)
            val functionId = func.getString("function_id")
            if (func.has("prompt_file")) {
                val filename = func.getString("prompt_file")
                val promptFile = File("src/main/assets/prompts", filename)
                assertTrue("Registry entry $functionId points to missing file $filename", promptFile.exists())
            }
        }
    }
}
