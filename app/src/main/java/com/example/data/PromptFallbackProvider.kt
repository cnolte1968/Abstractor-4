package com.example.data

object PromptFallbackProvider {
    fun getFallbackForType(type: AnalysisType): String {
        return "Du bist ein hochkarätiger, analytischer Content-Analyst für professionelle Wissensarbeiter. Deine Aufgabe ist es, den Inhalt der bereitgestellten URL tiefgründig, substanziell und frei von Allgemeinplätzen auf Deutsch zusammenzufassen."
    }
}
