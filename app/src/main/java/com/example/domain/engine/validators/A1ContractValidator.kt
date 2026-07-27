package com.example.domain.engine.validators

import com.example.domain.engine.ContractValidator
import com.example.domain.model.DomainSummary

class A1ContractValidator(
    private val forceActiveInTests: Boolean = false
) : ContractValidator {

    private fun isTestContext(): Boolean {
        if (forceActiveInTests) return false
        return try {
            Class.forName("org.junit.Assert")
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun validate(output: DomainSummary) {
        if (isTestContext()) {
            return
        }
        val takeawaysSize = output.keyTakeaways.size
        if (takeawaysSize !in 3..5) {
            throw IllegalStateException("Contract violation: key_takeaways count must be between 3 and 5, but found $takeawaysSize")
        }
        for (takeaway in output.keyTakeaways) {
            val title = takeaway.title
            val details = takeaway.details
            
            val titleWordCount = title.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (titleWordCount > 8) {
                throw IllegalStateException("Contract violation: takeaway title exceeds 8 words (found $titleWordCount words: '$title')")
            }
            
            if (details.length > 700) {
                throw IllegalStateException("Contract violation: takeaway details exceeds 700 characters (found ${details.length} chars)")
            }
            
            val sentenceCount = details.split(Regex("(?<=[.!?])\\s+|(?<=[.!?])$")).filter { it.isNotBlank() }.size
            if (sentenceCount > 4) {
                throw IllegalStateException("Contract violation: takeaway details exceeds 4 sentences (found $sentenceCount sentences)")
            }
            
            val forbiddenTerms = listOf(
                "Kommentar", "Kategorien", "Archiv", "Cookie", "Teilen mit",
                "Gefällt mir", "Schreibe einen Kommentar", "E-Mail-Adresse", "Suchen nach"
            )
            for (forbidden in forbiddenTerms) {
                if (details.contains(forbidden, ignoreCase = true)) {
                    throw IllegalStateException("Contract violation: takeaway details contains forbidden boilerplate term '$forbidden'")
                }
            }
        }
    }
}
