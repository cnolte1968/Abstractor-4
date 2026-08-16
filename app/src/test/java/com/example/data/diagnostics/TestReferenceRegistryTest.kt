package com.example.data.diagnostics

import com.example.data.AnalysisType
import com.example.ui.metadata.FeatureCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestReferenceRegistryTest {

    @Test
    fun `registry contains active entries matching FeatureCatalog active features`() {
        val activeFeatures = FeatureCatalog.features.filter { it.enabled && !it.isPlaceholder }
        val activeEntries = TestReferenceRegistry.getActiveEntries()

        assertEquals("Anzahl aktiver Quality-Gate-Funktionen muss exakt mit aktiven Features übereinstimmen", activeFeatures.size, activeEntries.size)

        for (feature in activeFeatures) {
            val entry = TestReferenceRegistry.getByFunctionId(feature.functionId)
            assertNotNull("Jede aktive Funktion muss in der Registry hinterlegt sein: ${feature.functionId}", entry)
            assertTrue("Aktive Funktion muss im Quality Gate relevant sein: ${feature.functionId}", entry!!.isQualityGateRelevant)
        }
    }

    @Test
    fun `placeholder functions are marked OUT_OF_SCOPE and excluded from quality gate`() {
        val placeholderFeatures = FeatureCatalog.features.filter { it.isPlaceholder }
        assertEquals("Erwarte genau 7 Platzhalter-Funktionen", 7, placeholderFeatures.size)

        for (placeholder in placeholderFeatures) {
            val entry = TestReferenceRegistry.getByFunctionId(placeholder.functionId)
            assertNotNull("Platzhalter-Funktion sollte in Registry dokumentiert sein: ${placeholder.functionId}", entry)
            assertEquals("Platzhalter muss OUT_OF_SCOPE sein", TestReferenceRegistry.VerificationStatus.OUT_OF_SCOPE, entry!!.verificationStatus)
            assertEquals("Platzhalter darf nicht im Quality Gate sein", false, entry.isQualityGateRelevant)
        }
    }

    @Test
    fun `verified live entries have non-empty url and expected values`() {
        val mapsEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.GOOGLE_MAPS_ANALYZER)
        assertNotNull(mapsEntry)
        assertEquals(TestReferenceRegistry.VerificationStatus.VERIFIED, mapsEntry!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, mapsEntry.easy!!.testMode)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, mapsEntry.difficult!!.testMode)
        assertTrue(mapsEntry.difficult!!.reference.contains("WgXTvya1yCDJjameA"))

        val ytEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.MULTIMEDIA_ANALYSIS)
        assertNotNull(ytEntry)
        assertEquals(TestReferenceRegistry.VerificationStatus.VERIFIED, ytEntry!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, ytEntry.easy!!.testMode)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, ytEntry.difficult!!.testMode)
        assertTrue(ytEntry.easy!!.reference.contains("hJP5GqnTrNo"))
        assertTrue(ytEntry.difficult!!.reference.contains("5qap5aO4i9A"))

        val webEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.WEB_SUMMARY)
        assertNotNull(webEntry)
        assertEquals(TestReferenceRegistry.VerificationStatus.VERIFIED, webEntry!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, webEntry.difficult!!.testMode)
        assertTrue(webEntry.difficult!!.reference.contains("share.google"))

        val debugRefs = TestReferenceRegistry.getLiveDebugReferences()
        assertTrue(debugRefs.any { it.first == "News-Focus" && it.second.contains("share.google") })
    }

    @Test
    fun `new live references are present and correctly classified`() {
        // FREE_SOURCE_QUERY
        val freeQuery = TestReferenceRegistry.getByAnalysisType(AnalysisType.FREE_SOURCE_QUERY)
        assertNotNull(freeQuery)
        assertEquals(TestReferenceRegistry.VerificationStatus.VERIFIED, freeQuery!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, freeQuery.difficult!!.testMode)
        assertEquals(
            "Was genau ist laut Artikel bei Google passiert und wie wurde der Fehler erklärt?",
            freeQuery.difficult!!.testQuery
        )
        assertNotNull(freeQuery.difficult!!.resolvedReference)

        // RISK_ANALYSIS
        val riskEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.RISK_ANALYSIS)
        assertNotNull(riskEntry)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, riskEntry!!.easy!!.testMode)
        assertTrue(riskEntry.easy!!.reference.contains("who.int"))
        assertEquals(TestReferenceRegistry.TestMode.SYNTHETIC, riskEntry.difficult!!.testMode)

        // PERSPECTIVES_COUNTERPOSITIONS
        val perspectivesEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.PERSPECTIVES_COUNTERPOSITIONS)
        assertNotNull(perspectivesEntry)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, perspectivesEntry!!.easy!!.testMode)
        assertTrue(perspectivesEntry.easy!!.reference.contains("pewresearch.org"))

        // RELEVANT_ASPECTS
        val aspectsEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.RELEVANT_ASPECTS)
        assertNotNull(aspectsEntry)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, aspectsEntry!!.easy!!.testMode)
        assertTrue(aspectsEntry.easy!!.reference.contains("gov.uk"))

        // DOCUMENT_SUMMARY
        val docEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.DOCUMENT_SUMMARY)
        assertNotNull(docEntry)
        assertEquals(TestReferenceRegistry.VerificationStatus.UNSURE, docEntry!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, docEntry.easy!!.testMode)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, docEntry.difficult!!.testMode)
        assertTrue(docEntry.difficult!!.reference.endsWith(".pdf"))

        // PHOTO_SCREENSHOT_ANALYSIS
        val photoEntry = TestReferenceRegistry.getByAnalysisType(AnalysisType.PHOTO_SCREENSHOT_ANALYSIS)
        assertNotNull(photoEntry)
        assertEquals(TestReferenceRegistry.VerificationStatus.UNSURE, photoEntry!!.verificationStatus)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, photoEntry.easy!!.testMode)
        assertEquals(TestReferenceRegistry.TestMode.LIVE, photoEntry.difficult!!.testMode)
        assertTrue(photoEntry.easy!!.reference.contains("nasa.gov"))
    }

    @Test
    fun `no synthetic golden domains are marked as LIVE`() {
        val syntheticDomains = listOf(
            "freie-anfrage.de",
            "techportal.de",
            "klimabericht.de",
            "finanz-blog.de",
            "debattenportal.de",
            "recht-portal.de",
            "studienportal.de",
            "immo-news.de"
        )

        for (entry in TestReferenceRegistry.ENTRIES) {
            entry.easy?.let { ref ->
                if (syntheticDomains.any { ref.reference.contains(it) }) {
                    assertEquals(
                        "Synthetische Domain ${ref.reference} darf nicht als LIVE markiert sein",
                        TestReferenceRegistry.TestMode.SYNTHETIC,
                        ref.testMode
                    )
                }
            }
            entry.difficult?.let { ref ->
                if (syntheticDomains.any { ref.reference.contains(it) }) {
                    assertEquals(
                        "Synthetische Domain ${ref.reference} darf nicht als LIVE markiert sein",
                        TestReferenceRegistry.TestMode.SYNTHETIC,
                        ref.testMode
                    )
                }
            }
        }
    }
}
