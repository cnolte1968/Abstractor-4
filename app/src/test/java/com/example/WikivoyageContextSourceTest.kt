package com.example

import com.example.data.contextengine.ContextEngine
import com.example.data.contextengine.ContextSourceType
import com.example.data.contextengine.LocationContextInput
import com.example.data.contextengine.WikipediaContextSource
import com.example.data.contextengine.WikivoyageContextSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WikivoyageContextSourceTest {

    @Test
    fun testGeoSearchSuccessAndExtract() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=geosearch") -> """
                    {
                      "query": {
                        "geosearch": [
                          { "pageid": 54321, "title": "Berlin/Mitte", "lat": 52.5162, "lon": 13.3777, "dist": 20.0 }
                        ]
                      }
                    }
                """.trimIndent()
                url.contains("prop=extracts") -> """
                    {
                      "query": {
                        "pages": {
                          "54321": {
                            "pageid": 54321,
                            "title": "Berlin/Mitte",
                            "extract": "Berlin/Mitte ist der zentrale Ortsteil von Berlin mit vielen Sehenswürdigkeiten und Restaurants."
                          }
                        }
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikivoyageContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(
            placeName = "Berlin Mitte",
            latitude = 52.5162,
            longitude = 13.3777
        )

        val result = source.fetchContext(input)

        assertTrue(result.isSuccessful)
        assertEquals("WIKIVOYAGE", result.sourceName)
        assertEquals(ContextSourceType.TRAVEL_GUIDE, result.sourceType)
        assertEquals("Berlin/Mitte ist der zentrale Ortsteil von Berlin mit vielen Sehenswürdigkeiten und Restaurants.", result.snippet)
        assertEquals("MATCHED", result.metadata["status"])
        assertEquals("Berlin/Mitte", result.metadata["wikivoyageTitle"])
    }

    @Test
    fun testTitleSearchFallbackWhenNoCoordinates() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=search") -> """
                    {
                      "query": {
                        "search": [
                          { "title": "Köln", "snippet": "Reiseführer für Köln mit Tipps zu Sehenswürdigkeiten..." }
                        ]
                      }
                    }
                """.trimIndent()
                url.contains("prop=extracts") -> """
                    {
                      "query": {
                        "pages": {
                          "98765": {
                            "pageid": 98765,
                            "title": "Köln",
                            "extract": "Köln ist die viertgrößte Stadt Deutschlands und bekannt für den Kölner Dom."
                          }
                        }
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikivoyageContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "Köln")

        val result = source.fetchContext(input)

        assertTrue(result.isSuccessful)
        assertEquals(ContextSourceType.TRAVEL_GUIDE, result.sourceType)
        assertEquals("Köln ist die viertgrößte Stadt Deutschlands und bekannt für den Kölner Dom.", result.snippet)
        assertEquals("Köln", result.metadata["wikivoyageTitle"])
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

        val source = WikivoyageContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "UnknownPlace999")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }

    @Test
    fun testNetworkExceptionHandling() = runTest {
        val mockFetcher: suspend (String) -> String = { _ ->
            throw RuntimeException("Connection refused")
        }

        val source = WikivoyageContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "Hamburg")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }

    @Test
    fun testContextEngineIntegrationWithMultipleSources() = runTest {
        val wikiFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "München" }],
                    "pages": {
                      "101": { "title": "München", "extract": "München ist die Landeshauptstadt des Freistaates Bayern." }
                    }
                  }
                }
            """.trimIndent()
        }

        val voyageFetcher: suspend (String) -> String = { _ ->
            """
                {
                  "query": {
                    "search": [{ "title": "München" }],
                    "pages": {
                      "202": { "title": "München", "extract": "Tipps für München: Marienplatz, Englischer Garten und Brauhäuser." }
                    }
                  }
                }
            """.trimIndent()
        }

        val wikiSource = WikipediaContextSource(networkFetcher = wikiFetcher)
        val voyageSource = WikivoyageContextSource(networkFetcher = voyageFetcher)

        val engine = ContextEngine(sources = listOf(wikiSource, voyageSource))
        val input = LocationContextInput(placeName = "München")

        val results = engine.resolveContext(input)

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccessful)
        assertEquals("WIKIPEDIA", results[0].sourceName)
        assertTrue(results[1].isSuccessful)
        assertEquals("WIKIVOYAGE", results[1].sourceName)

        val formatted = engine.formatForGemini(results)
        assertTrue(formatted.contains("=== FAKTEN ==="))
        assertTrue(formatted.contains("München ist die Landeshauptstadt des Freistaates Bayern."))
        assertTrue(formatted.contains("=== REISEKONTEXT ==="))
        assertTrue(formatted.contains("Tipps für München: Marienplatz, Englischer Garten und Brauhäuser."))
    }

    @Test
    fun testFalseNameMatchIsRejected() = runTest {
        val mockFetcher: suspend (String) -> String = { url ->
            when {
                url.contains("list=search") -> """
                    {
                      "query": {
                        "search": [
                          { "title": "Rockledge", "snippet": "Rockledge is a city in Brevard County, Florida..." }
                        ]
                      }
                    }
                """.trimIndent()
                else -> "{}"
            }
        }

        val source = WikivoyageContextSource(networkFetcher = mockFetcher)
        val input = LocationContextInput(placeName = "Greenhouse Community Space")

        val result = source.fetchContext(input)

        assertFalse(result.isSuccessful)
        assertNull(result.snippet)
        assertEquals("NO_CONTEXT_FOUND", result.metadata["status"])
    }
}
