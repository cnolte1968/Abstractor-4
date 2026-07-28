package com.example.contextpoc
import org.junit.Test
import org.junit.Assert.*

class SimpleTest {
    @Test
    fun testMock() {
        val client = WikipediaApiClient(WikipediaMockFixtures.mockNetworkFetcher)
        val text = client.getExtract("Eiffelturm", "de")
        println("Result text: $text")
        
        val search = client.searchTitle("Eiffelturm", "de")
        println("Result search: $search")
        
        val geo = client.geoSearch(48.8584, 2.2945, "de")
        println("Result geo: $geo")
    }
}
