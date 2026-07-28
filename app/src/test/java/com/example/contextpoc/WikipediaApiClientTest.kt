package com.example.contextpoc

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WikipediaApiClientTest {
    private val client = WikipediaApiClient(WikipediaMockFixtures.mockNetworkFetcher)

    @Test
    fun testTitleSearch() {
        val titles = client.searchTitle("Eiffelturm", "de")
        println("Found titles: $titles")
        assertTrue("Sollte Titel finden", titles.isNotEmpty())
        assertTrue("Erster Treffer sollte Eiffelturm sein", titles[0].contains("Eiffelturm", ignoreCase = true))
    }

    @Test
    fun testGeoSearch() {
        val titles = client.geoSearch(48.8584, 2.2945, "de")
        println("Found geo titles: $titles")
        assertTrue("Sollte Orte in Paris finden", titles.isNotEmpty())
        assertTrue("Eiffelturm sollte in den Resultaten sein", titles.any { it.contains("Eiffelturm", ignoreCase = true) })
    }

    @Test
    fun testExtract() {
        val text = client.getExtract("Eiffelturm", "de")
        println("Found text: ${text?.take(50)}")
        assertNotNull("Text sollte nicht null sein", text)
        assertTrue("Text sollte lang genug sein", (text?.length ?: 0) > 500)
    }

    @Test
    fun testNetworkErrorHandling() {
        val errorClient = WikipediaApiClient { throw Exception("Simulated network error") }
        val titles = errorClient.searchTitle("Test", "de")
        assertTrue("Sollte leere Liste bei Fehler zurückgeben", titles.isEmpty())
    }
}
