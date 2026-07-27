package com.example

import com.example.data.AnalysisType
import com.example.data.engine.AnalysisRegistryImpl
import com.example.ui.metadata.FeatureCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BulkMigrationVerificationTest {

    private val migrationMatrix = mapOf(
        AnalysisType.STANDARD_WEBSEITE to AnalysisType.WEB_SUMMARY,
        AnalysisType.TOP_3_KERNAUSSAGEN to AnalysisType.KEY_TAKEAWAYS,
        AnalysisType.WEITERE_RELEVANTE_ASPEKTE to AnalysisType.RELEVANT_ASPECTS,
        AnalysisType.DOKUMENTE to AnalysisType.DOCUMENT_SUMMARY,
        AnalysisType.FREIE_QUELLENANFRAGE to AnalysisType.FREE_SOURCE_QUERY,
        AnalysisType.MULTIMEDIA to AnalysisType.MULTIMEDIA_ANALYSIS,
        AnalysisType.AKTUALITAETS_CHECK to AnalysisType.FRESHNESS_CHECK,
        AnalysisType.FEHLINFORMATIONS_RADAR to AnalysisType.MISINFORMATION_RADAR,
        AnalysisType.FACTS_VS_OPINIONS_ANALYZER to AnalysisType.FACTS_VS_OPINIONS,
        AnalysisType.RISIKO_ANALYSE to AnalysisType.RISK_ANALYSIS,
        AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS to AnalysisType.PERSPECTIVES_COUNTERPOSITIONS
    )

    @Test
    fun testCanonicalMappings() {
        for ((legacy, canonical) in migrationMatrix) {
            assertEquals(
                "Legacy type ${legacy.name} must map to canonical type ${canonical.name}",
                canonical,
                legacy.canonical()
            )
            assertEquals(
                "Canonical type ${canonical.name} must map to itself",
                canonical,
                canonical.canonical()
            )
        }
    }

    @Test
    fun testRegistryFunctionIdIdentity() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val gateway = com.example.data.GeminiRepository
        val registry = AnalysisRegistryImpl(gateway, context)

        for ((legacy, canonical) in migrationMatrix) {
            val legacyFuncId = registry.getFunctionIdForType(legacy)
            val canonicalFuncId = registry.getFunctionIdForType(canonical)
            assertEquals(
                "Registry function ID must be identical for legacy ${legacy.name} and canonical ${canonical.name}",
                legacyFuncId,
                canonicalFuncId
            )
        }
    }

    @Test
    fun testFeatureCatalogUsesCanonicalEnums() {
        val features = FeatureCatalog.features
        assertTrue("Feature catalog must not be empty", features.isNotEmpty())

        for (feature in features) {
            val type = feature.analysisType ?: continue
            // If the type is one of our legacy types, it shouldn't be in the active Catalog!
            val isLegacy = type in migrationMatrix.keys
            assertTrue(
                "Active feature catalog metadata should use canonical types, but found legacy type: ${type.name}",
                !isLegacy
            )
        }
    }
}
