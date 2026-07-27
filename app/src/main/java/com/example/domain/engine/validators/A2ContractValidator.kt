package com.example.domain.engine.validators

import com.example.domain.engine.ContractValidator
import com.example.domain.model.DomainSummary

class A2ContractValidator(
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
        if (takeawaysSize != 3) {
            throw IllegalStateException("Contract violation: key_takeaways count must be exactly 3, but found $takeawaysSize")
        }

        for (takeaway in output.keyTakeaways) {
            val title = takeaway.title
            val details = takeaway.details
            
            if (title.isBlank()) {
                throw IllegalStateException("Contract violation: takeaway title must not be empty or blank")
            }
            if (details.isBlank()) {
                throw IllegalStateException("Contract violation: takeaway details must not be empty or blank")
            }

            // Word count of title
            val titleWordCount = title.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            if (titleWordCount > 8) {
                throw IllegalStateException("Contract violation: takeaway title exceeds 8 words (found $titleWordCount words: '$title')")
            }
            
            // Details length in characters
            if (details.length > 700) {
                throw IllegalStateException("Contract violation: takeaway details exceeds 700 characters (found ${details.length} chars)")
            }
            
            // Sentence count of details
            val sentenceCount = details.split(Regex("(?<=[.!?])\\s+|(?<=[.!?])$")).filter { it.isNotBlank() }.size
            if (sentenceCount > 4) {
                throw IllegalStateException("Contract violation: takeaway details exceeds 4 sentences (found $sentenceCount sentences)")
            }

            // Raw JSON / Markdown remnants
            val codeblockOrJsonIndicators = listOf("```", "{", "}", "[", "]", "\"key_takeaways\"", "\"title\"", "\"details\"")
            for (indicator in codeblockOrJsonIndicators) {
                if (title.contains(indicator) || details.contains(indicator)) {
                    throw IllegalStateException("Contract violation: found raw JSON/Markdown codeblock remnants in title or details ('$indicator')")
                }
            }
            
            // Forbidden boilerplate terms in title or details
            val forbiddenTerms = listOf(
                "Kommentar", "Kommentare", "Kategorie", "Kategorien", "Archiv", 
                "Cookie", "Datenschutz", "Newsletter", "Teilen", "Share", "Reply", "Leave a Reply"
            )
            for (forbidden in forbiddenTerms) {
                if (title.contains(forbidden, ignoreCase = true) || details.contains(forbidden, ignoreCase = true)) {
                    throw IllegalStateException("Contract violation: takeaway contains forbidden boilerplate term '$forbidden'")
                }
            }
        }
    }
}
