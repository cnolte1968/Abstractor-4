package com.example

import com.example.domain.engine.*
import com.example.domain.engine.validators.A2ContractValidator
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class A2ContractValidatorTest {

    private fun createValidSummary(
        takeaways: List<TakeawayItem> = listOf(
            TakeawayItem("Erster Punkt", "Das ist eine valide Erklärung für den ersten Punkt."),
            TakeawayItem("Zweiter Punkt", "Das ist eine valide Erklärung für den zweiten Punkt."),
            TakeawayItem("Dritter Punkt", "Das ist eine valide Erklärung für den dritten Punkt.")
        )
    ): DomainSummary {
        return DomainSummary(
            id = "id-123",
            title = "Valider Titel",
            originalUrl = "https://example.com",
            shortDescription = "Eine valide Kurzbeschreibung.",
            keyTakeaways = takeaways,
            owner = "Test Owner",
            analysisId = "test-id"
        )
    }

    @Test
    fun A2ContractValidator_validExactly3Takeaways_passes() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val validSummary = createValidSummary()
        try {
            validator.validate(validSummary)
        } catch (e: Exception) {
            fail("Expected valid output to pass validation, but failed: ${e.message}")
        }
    }

    @Test
    fun A2ContractValidator_twoTakeaways_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with 2 takeaways")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("key_takeaways count must be exactly 3"))
        }
    }

    @Test
    fun A2ContractValidator_fourTakeaways_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei."),
                TakeawayItem("Vier", "Satz vier.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with 4 takeaways")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("key_takeaways count must be exactly 3"))
        }
    }

    @Test
    fun A2ContractValidator_emptyTitle_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with empty takeaway title")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("takeaway title must not be empty or blank"))
        }
    }

    @Test
    fun A2ContractValidator_boilerplate_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Das ist Cookie-Müll hier."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with boilerplate content")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("contains forbidden boilerplate term 'Cookie'"))
        }
    }

    @Test
    fun EngineContract_delegatesToA2Validator() {
        var delegationCalled = false
        val mockValidator = object : ContractValidator {
            override fun validate(output: DomainSummary) {
                delegationCalled = true
            }
        }

        val contract = EngineContract(
            functionId = "A.2",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput",
            outputSchema = "DomainSummary",
            capabilities = EngineCapabilities("Mock A.2", false, false),
            promptPath = "prompts/F_TOP_3_KERNAUSSAGEN.md",
            customValidator = mockValidator
        )

        val summary = createValidSummary()
        contract.validateOutput(summary)
        assertTrue("Expected delegate to be called", delegationCalled)
    }

    @Test
    fun A2ContractValidator_titleExceedsEightWords_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Ein viel zu langer Titel mit mehr als acht Worten darin", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected title with > 8 words to fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("takeaway title exceeds 8 words"))
        }
    }

    @Test
    fun A2ContractValidator_detailsExceedsFourSentences_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Satz eins. Satz zwei. Satz drei. Satz vier. Satz fünf."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected details with > 4 sentences to fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("takeaway details exceeds 4 sentences"))
        }
    }

    @Test
    fun A2ContractValidator_jsonRemnants_fails() {
        val validator = A2ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Das hier ist ein JSON Fragment: {\"key_takeaways\": []}."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected JSON/Markdown codeblock remnants to fail")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("found raw JSON/Markdown codeblock remnants"))
        }
    }
}
