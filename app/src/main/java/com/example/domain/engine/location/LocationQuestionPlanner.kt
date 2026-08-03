package com.example.domain.engine.location

enum class QuestionCategory {
    STOSSZEITEN,
    ZUGANG_MOBILITAET,
    BARRIEREFREIHEIT,
    PARKEN,
    ATMOSPHAERE_AUSSTATTUNG,
    FAMILIEN_KINDER,
    HISTORIE_KULTUR,
    PREISE_OEFFNUNGSZEITEN,
    SAISON_EVENTS,
    SONSTIGE
}

enum class DataSourceType {
    PLACES,
    REVIEWS,
    LOCATION_CONTEXT,
    WIKIPEDIA,
    WIKIVOYAGE,
    SEARCH_GROUNDING
}

data class ExecutionPlan(
    val primaryCategory: QuestionCategory,
    val requiredSources: Set<DataSourceType>,
    val optionalSources: Set<DataSourceType>,
    val requiresGrounding: Boolean,
    val priorityOrder: List<DataSourceType>
)

object LocationQuestionPlanner {
    fun planExecution(freeQuery: String, locationName: String? = null): ExecutionPlan? {
        if (freeQuery.isBlank()) return null
        
        val lowercaseQuery = freeQuery.lowercase()
        
        // Reiner Smalltalk
        val smalltalk = listOf("hallo", "wie geht es", "guten tag", "hi", "hey", "tschüss")
        if (smalltalk.contains(lowercaseQuery.trim())) {
            return null
        }
        
        // Offensichtlich ortsfremde Fragen
        val offTopicKeywords = listOf(
            "hauptstadt von", "wer ist", "wann wurde angela", "wie backt man", "rezept für",
            "wie groß ist der mond", "bundeskanzler"
        )
        if (offTopicKeywords.any { lowercaseQuery.contains(it) }) {
            return null
        }

        val category = determineCategory(lowercaseQuery)
        val requiresGrounding = determineGrounding(lowercaseQuery, category)
        
        val requiredSources = mutableSetOf(DataSourceType.PLACES)
        val optionalSources = mutableSetOf(DataSourceType.REVIEWS)
        
        when (category) {
            QuestionCategory.STOSSZEITEN -> {
                requiredSources.add(DataSourceType.LOCATION_CONTEXT)
            }
            QuestionCategory.BARRIEREFREIHEIT, QuestionCategory.PARKEN -> {
                requiredSources.add(DataSourceType.LOCATION_CONTEXT)
            }
            QuestionCategory.ZUGANG_MOBILITAET -> {
                requiredSources.add(DataSourceType.LOCATION_CONTEXT)
                optionalSources.add(DataSourceType.WIKIVOYAGE)
            }
            QuestionCategory.HISTORIE_KULTUR -> {
                requiredSources.add(DataSourceType.WIKIPEDIA)
                optionalSources.add(DataSourceType.WIKIVOYAGE)
            }
            else -> {}
        }
        
        if (requiresGrounding) {
            requiredSources.add(DataSourceType.SEARCH_GROUNDING)
        }

        val priorityOrder = requiredSources.toList() + optionalSources.toList()

        return ExecutionPlan(
            primaryCategory = category,
            requiredSources = requiredSources,
            optionalSources = optionalSources,
            requiresGrounding = requiresGrounding,
            priorityOrder = priorityOrder
        )
    }

    private fun determineCategory(query: String): QuestionCategory {
        return when {
            query.contains("stoßzeit") || query.contains("voll") || query.contains("viel los") || query.contains("meisten los") || query.contains("wartezeit") -> QuestionCategory.STOSSZEITEN
            query.contains("barrierefrei") || query.contains("rollstuhl") || query.contains("aufzug") -> QuestionCategory.BARRIEREFREIHEIT
            query.contains("parken") || query.contains("parkplatz") || query.contains("parkplätz") || query.contains("parkhaus") -> QuestionCategory.PARKEN
            query.contains("atmosphäre") || query.contains("ausstattung") || query.contains("regen") || query.contains("fotografieren") || query.contains("klima") || query.contains("toilette") -> QuestionCategory.ATMOSPHAERE_AUSSTATTUNG
            query.contains("kinder") || query.contains("kinderwagen") || query.contains("familie") -> QuestionCategory.FAMILIEN_KINDER
            query.contains("historie") || query.contains("geschichte") || query.contains("gebaut") || query.contains("architektur") -> QuestionCategory.HISTORIE_KULTUR
            query.contains("preis") || query.contains("kosten") || query.contains("eintritt") || query.contains("öffnungszeit") || query.contains("geöffnet") || query.contains("ticket") -> QuestionCategory.PREISE_OEFFNUNGSZEITEN
            query.contains("saison") || query.contains("event") || query.contains("sperrung") || query.contains("ausstellung") -> QuestionCategory.SAISON_EVENTS
            query.contains("zugang") || query.contains("hinkommen") || query.contains("berg") || query.contains("weg") || query.contains("ältere menschen") -> QuestionCategory.ZUGANG_MOBILITAET
            else -> QuestionCategory.SONSTIGE
        }
    }

    private fun determineGrounding(query: String, category: QuestionCategory): Boolean {
        if (category == QuestionCategory.PREISE_OEFFNUNGSZEITEN || category == QuestionCategory.SAISON_EVENTS) {
            return true
        }

        val timeKeywords = listOf(
            "heute", "aktuell", "jetzt", "feiertag", "derzeit", "momentan", "aktuelle ausstellung"
        )
        val containsYear = Regex("\\b\\d{4}\\b").containsMatchIn(query)
        val containsTimeKeyword = timeKeywords.any { query.contains(it) } || containsYear

        if (containsTimeKeyword) {
            return true
        }
        
        return false
    }
}
