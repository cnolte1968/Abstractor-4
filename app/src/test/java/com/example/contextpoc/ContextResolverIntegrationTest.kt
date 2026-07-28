package com.example.contextpoc

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContextResolverIntegrationTest {
    // Inject the mock client
    private val mockClient = WikipediaApiClient(WikipediaMockFixtures.mockNetworkFetcher)
    private val resolver = ContextResolver(mockClient)

    private fun printResult(caseName: String, result: ContextResolverResult) {
        println("--- TEST CASE: $caseName ---")
        println("Status: ${result.status}")
        println("Match Confidence: ${result.matchConfidence}")
        val source = result.sources.firstOrNull()
        if (source != null) {
            println("Source URL: ${source.url}")
            println("Source Title: ${source.title}")
            println("Language: ${if (source.url.contains("de.wikipedia")) "DE" else "EN"}")
        }
        val textLength = result.contextText?.length ?: 0
        println("Text Length: $textLength characters")
        if (textLength > 0) {
            println("Snippet: ${result.contextText?.substring(0, minOf(textLength, 150))}...")
        }
        println("--------------------------\n")
    }

    @Test
    fun test1_EindeutigeSehenswuerdigkeit() {
        val input = ContextPlaceInput(
            name = "Eiffelturm",
            address = "Champ de Mars, 5 Av. Anatole France, 75007 Paris, Frankreich",
            city = "Paris",
            country = "Frankreich",
            placeTypes = listOf("tourist_attraction", "point_of_interest"),
            latitude = 48.8584,
            longitude = 2.2945
        )
        val result = resolver.resolve(input)
        printResult("Eiffelturm, Paris", result)
        assertEquals(ContextResolutionStatus.PASS, result.status)
        assertEquals("Eiffelturm", result.sources.first().title)
    }

    @Test
    fun test2_TempelMitAbweichendemNamen() {
        val input = ContextPlaceInput(
            name = "Wat Pho",
            address = "2 Sanam Chai Rd, Phra Borom Maha Ratchawang, Phra Nakhon, Bangkok 10200, Thailand",
            city = "Bangkok",
            country = "Thailand",
            placeTypes = listOf("tourist_attraction", "place_of_worship"),
            latitude = 13.7465,
            longitude = 100.4933
        )
        val result = resolver.resolve(input)
        printResult("Wat Pho, Bangkok", result)
        assertEquals(ContextResolutionStatus.PASS, result.status)
    }

    @Test
    fun test3_Stadtteil() {
        val input = ContextPlaceInput(
            name = "Kreuzberg",
            address = "Kreuzberg, Berlin, Deutschland",
            city = "Berlin",
            country = "Deutschland",
            placeTypes = listOf("neighborhood", "political"),
            latitude = 52.4988,
            longitude = 13.3918
        )
        val result = resolver.resolve(input)
        printResult("Kreuzberg, Berlin", result)
        assertEquals(ContextResolutionStatus.PASS, result.status)
        assertEquals("Berlin-Kreuzberg", result.sources.first().title)
    }

    @Test
    fun test4_GrosserPark() {
        val input = ContextPlaceInput(
            name = "Central Park",
            address = "New York, NY, USA",
            city = "New York",
            country = "USA",
            placeTypes = listOf("park", "tourist_attraction"),
            latitude = 40.7812,
            longitude = -73.9665
        )
        val result = resolver.resolve(input)
        printResult("Central Park, New York", result)
        assertEquals(ContextResolutionStatus.PASS, result.status)
    }

    @Test
    fun test5_GewoehnlichesRestaurant() {
        val input = ContextPlaceInput(
            name = "Joe's Pizza",
            address = "7 Carmine St, New York, NY 10014, USA",
            city = "New York",
            country = "USA",
            placeTypes = listOf("restaurant", "food"),
            latitude = 40.7306,
            longitude = -74.0021
        )
        val result = resolver.resolve(input)
        printResult("Joe's Pizza (Restaurant)", result)
        assertEquals(ContextResolutionStatus.NO_CONTEXT_FOUND, result.status)
    }

    @Test
    fun test6_MehrdeutigeKirche() {
        val input = ContextPlaceInput(
            name = "St. Mary's Church",
            address = "London, UK",
            city = "London",
            country = "UK",
            placeTypes = listOf("place_of_worship", "church"),
            latitude = 51.5074,
            longitude = -0.1278
        )
        val result = resolver.resolve(input)
        printResult("St. Mary's Church, London (Mehrdeutig)", result)
        assertEquals(ContextResolutionStatus.AMBIGUOUS_MATCH, result.status)
    }

    @Test
    fun test7_PartialMatch() {
        val input = ContextPlaceInput(
            name = "Partial Place",
            address = "Test, DE",
            city = "Test",
            country = "DE",
            placeTypes = listOf("tourist_attraction"),
            latitude = 50.0,
            longitude = 10.0
        )
        val result = resolver.resolve(input)
        printResult("Partial Match", result)
        assertEquals(ContextResolutionStatus.PARTIAL, result.status)
    }
}
