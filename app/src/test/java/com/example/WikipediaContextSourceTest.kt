package com.example

import com.example.data.contextengine.ContextEngine
import com.example.data.contextengine.LocationContextInput
import com.example.data.contextengine.WikipediaContextSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WikipediaContextSourceTest {

    @Test
    fun testGeoSearchSuccessAndExtract() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=geosearch") -> """
                    {
                      "query": {
                        "geosearch": [
                          { "pageid": 12345, "title": "Brandenburger Tor", "lat": 52.5162, "lon": 13.3777, "dist": 15.0 }
                        ]
                      }
                    }
                """.trimIndent()
                url.contains("prop=extracts") -> """
                    {
                      "query": {
                        "pages": {
                          "12345": {
                            "pageid": 12345,
                            "title": "Brandenburger Tor",
                            "extract": "Das Brandenburger Tor in Berlin ist ein frühklassizistisches Triumphtor."
                          }
                        }
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikipediaContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(
            placeName = "Brandenburger Tor",
            latitude = 52.5162,
            longitude = 13.3777
        )

        val result = source.fetchContext(input)

        assertTrue(result.isSuccessful)
        assertEquals("WIKIPEDIA", result.sourceName)
        assertEquals("Umfeld- und Ortskontext (Brandenburger Tor):\nDas Brandenburger Tor in Berlin ist ein frühklassizistisches Triumphtor.", result.snippet)
        assertEquals("PARENT_LOCATION_MATCH", result.metadata["status"])
        assertEquals("Brandenburger Tor", result.metadata["wikiTitle"])
    }

    @Test
    fun testTitleSearchFallbackWhenNoCoordinates() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=search") -> """
                    {
                      "query": {
                        "search": [
                          { "title": "Kölner Dom", "snippet": "Der Kölner Dom ist eine katholische Kirche..." }
                        ]
                      }
                    }
                """.trimIndent()
                url.contains("prop=extracts") -> """
                    {
                      "query": {
                        "pages": {
                          "67890": {
                            "pageid": 67890,
                            "title": "Kölner Dom",
                            "extract": "Der Kölner Dom ist eine der bedeutendsten Kathedralen Europas."
                          }
                        }
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikipediaContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(
            placeName = "Kölner Dom"
        )

        val result = source.fetchContext(input)

        assertTrue(result.isSuccessful)
        assertEquals("Umfeld- und Ortskontext (Kölner Dom):\nDer Kölner Dom ist eine der bedeutendsten Kathedralen Europas.", result.snippet)
        assertEquals("Kölner Dom", result.metadata["wikiTitle"])
    }

    @Test
    fun testNoContextFoundReturnsGracefully() = runTest {
        val mockFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": []
                  }
                }
            """.trimIndent()
        }

        val source = WikipediaContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "NonExistentPlaceXYZ12345")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }

    @Test
    fun testNetworkExceptionHandling() = runTest {
        val mockFetcher: suspend (String) -> String = { _ ->
            throw RuntimeException("Network timeout")
        }

        val source = WikipediaContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "Berlin Hauptbahnhof")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
        assertNull(result.errorMessage)
    }

    @Test
    fun testContextEngineIntegrationWithWikipediaSource() = runTest {
        val mockFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "Alexanderplatz" }],
                    "pages": {
                      "111": { "title": "Alexanderplatz", "extract": "Der Alexanderplatz ist ein zentraler Platz in Berlin." }
                    }
                  }
                }
            """.trimIndent()
        }

        val wikiSource = WikipediaContextSource(networkFetcher = mockFetcher)
        val engine = ContextEngine(sources = listOf(wikiSource))
        val input = LocationContextInput(placeName = "Alexanderplatz")

        val results = engine.resolveContext(input)

        assertEquals(1, results.size)
        val res = results[0]
        assertTrue(res.isSuccessful)
        assertEquals("Umfeld- und Ortskontext (Alexanderplatz):\nDer Alexanderplatz ist ein zentraler Platz in Berlin.", res.snippet)
    }

    @Test
    fun testFalseNameMatchIsRejected() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=search") -> """
                    {
                      "query": {
                        "search": [
                          { "title": "Greenhouse", "snippet": "A greenhouse is a structure with walls and roof made chiefly of transparent material..." }
                        ]
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikipediaContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "Greenhouse Community Space")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }
}
