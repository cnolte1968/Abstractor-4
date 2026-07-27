package com.example

import com.example.domain.engine.*
import com.example.domain.engine.validators.A1ContractValidator
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class A1ContractValidatorTest {

    private fun createValidSummary(
        takeaways: List<TakeawayItem> = listOf(
            TakeawayItem("Wichtig eins", "Das ist ein kurzer Satz eins. Er hat korrekte Länge."),
            TakeawayItem("Wichtig zwei", "Das ist ein kurzer Satz zwei. Er hat korrekte Länge."),
            TakeawayItem("Wichtig drei", "Das ist ein kurzer Satz drei. Er hat korrekte Länge.")
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
    fun A1ContractValidator_validOutput_passes() {
        val validator = A1ContractValidator(forceActiveInTests = true)
        val validSummary = createValidSummary()
        try {
            validator.validate(validSummary)
        } catch (e: Exception) {
            fail("Expected valid output to pass validation, but failed: ${e.message}")
        }
    }

    @Test
    fun A1ContractValidator_tooFewTakeaways_fails() {
        val validator = A1ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with too few takeaways (2)")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("key_takeaways count must be between 3 and 5"))
        }
    }

    @Test
    fun A1ContractValidator_tooManyTakeaways_fails() {
        val validator = A1ContractValidator(forceActiveInTests = true)
        val invalidSummary = createValidSummary(
            takeaways = listOf(
                TakeawayItem("Eins", "Satz eins."),
                TakeawayItem("Zwei", "Satz zwei."),
                TakeawayItem("Drei", "Satz drei."),
                TakeawayItem("Vier", "Satz vier."),
                TakeawayItem("Fünf", "Satz fünf."),
                TakeawayItem("Sechs", "Satz sechs.")
            )
        )
        try {
            validator.validate(invalidSummary)
            fail("Expected validation to fail with too many takeaways (6)")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("key_takeaways count must be between 3 and 5"))
        }
    }

    @Test
    fun EngineContract_delegatesToCustomValidator() {
        var delegationCalled = false
        val mockValidator = object : ContractValidator {
            override fun validate(output: DomainSummary) {
                delegationCalled = true
            }
        }

        val contract = EngineContract(
            functionId = "MockFid",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput",
            outputSchema = "DomainSummary",
            capabilities = EngineCapabilities("Mock", false, false),
            promptPath = "prompts/mock.md",
            customValidator = mockValidator
        )

        val summary = createValidSummary()
        contract.validateOutput(summary)
        assertTrue("Expected delegate to be called", delegationCalled)
    }

    @Test
    fun EngineContract_withoutCustomValidator_usesGenericValidationOnly() {
        val contract = EngineContract(
            functionId = "MockFid",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput",
            outputSchema = "DomainSummary",
            capabilities = EngineCapabilities("Mock", false, false),
            promptPath = "prompts/mock.md",
            customValidator = null
        )

        val invalidSummary = createValidSummary().copy(title = "")
        try {
            contract.validateOutput(invalidSummary)
            fail("Expected blank title to trigger generic validation error")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("requires non-blank 'title'"))
        }

        val validSummary = createValidSummary()
        try {
            contract.validateOutput(validSummary)
        } catch (e: Exception) {
            fail("Expected generic validation to pass for valid summary: ${e.message}")
        }
    }

    @Test
    fun testRegressionA1RemainsPass() {
        val contract = EngineContract(
            functionId = "WEB_SUMMARY",
            version = "1.0.0",
            inputSchema = "CanonicalAnalysisInput",
            outputSchema = "DomainSummary",
            capabilities = EngineCapabilities("A.1 Web", true, false),
            promptPath = "prompts/F_STANDARD_WEBSEITE.md",
            customValidator = A1ContractValidator()
        )

        val summary = createValidSummary()
        try {
            contract.validateOutput(summary)
        } catch (e: Exception) {
            fail("Expected standard validateOutput to pass in test context: ${e.message}")
        }
    }
}
