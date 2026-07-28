package com.example.contextpoc

import org.junit.Test
import org.junit.Assert.*

class ContextMatchingTest {

    @Test
    fun `test Unique Landmark Match`() {
        val input = ContextPlaceInput("Eiffel Tower", "Champ de Mars", "Paris", "France", listOf("tourist_attraction"), 48.8584, 2.2945)
        val candidates = listOf(ContextCandidate("Eiffel Tower", "Iconic iron lattice tower", "monument", "Paris", "France", 48.8584, 2.2945))
        
        val result = ContextCandidateMatcher.match(input, candidates)
        assertEquals(MatchStatus.PASS, result.status)
        assertEquals("Eiffel Tower", result.bestCandidate?.wikipediaTitle)
    }

    @Test
    fun `test Ordinary Restaurant Excluded`() {
        val input = ContextPlaceInput("My Favorite Burger", "Main St 1", "Berlin", "Germany", listOf("restaurant"), 52.5200, 13.4050)
        val candidates = listOf(ContextCandidate("My Favorite Burger", "Burger place", "restaurant", "Berlin", "Germany", 52.5200, 13.4050))
        
        val result = ContextCandidateMatcher.match(input, candidates)
        assertEquals(MatchStatus.NO_CONTEXT_FOUND, result.status)
    }

    @Test
    fun `test Ambiguous Match`() {
        val input = ContextPlaceInput("St. Mary Church", "Main St", "City", "Country", listOf("church"), 0.0, 0.0)
        val candidates = listOf(
            ContextCandidate("St. Mary Church A", "Church A", "church", "City", "Country", 0.0, 0.0),
            ContextCandidate("St. Mary Church B", "Church B", "church", "City", "Country", 0.0, 0.0)
        )
        
        val result = ContextCandidateMatcher.match(input, candidates)
        assertEquals(MatchStatus.AMBIGUOUS_MATCH, result.status)
    }
}
