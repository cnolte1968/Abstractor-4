package com.example

import com.example.data.AuthorAttribution
import com.example.data.GooglePlacesPoCResult
import com.example.data.LocalizedText
import com.example.data.OpeningHours
import com.example.data.Review
import com.example.data.contextengine.ContextEngine
import com.example.data.contextengine.ContextResult
import com.example.data.contextengine.ContextSource
import com.example.data.contextengine.ContextSourceType
import com.example.data.contextengine.GoogleMapsLocationContextService
import com.example.data.contextengine.LocationContextInput
import com.example.domain.engine.location.DataSourceType
import com.example.domain.engine.location.LocationQuestionCoordinator
import com.example.domain.engine.location.QuestionCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationQuestionCoordinatorTest {

    private fun createDummyPlacesResult(
        reviews: List<Review>? = listOf(
            Review(
                name = "review1",
                rating = 5.0,
                text = LocalizedText(text = "Toller Ort, sehr beeindruckend!", languageCode = "de"),
                authorAttribution = AuthorAttribution(displayName = "Max Mustermann")
            )
        )
    ): GooglePlacesPoCResult {
        return GooglePlacesPoCResult(
            originalSharedUrl = "https://maps.app.goo.gl/example",
            resolvedUrl = "https://www.google.com/maps/place/K%C3%B6lner+Dom/@50.9412784,6.9582814,17z",
            placeId = "ChIJ1234567",
            placeResourceName = "places/ChIJ1234567",
            urlDerivedName = "Kölner Dom",
            urlDerivedAddress = "Domkloster 4, 50667 Köln",
            formattedAddress = "Domkloster 4, 50667 Köln",
            shortFormattedAddress = "Domkloster 4",
            addressComponents = null,
            latitude = 50.9412784,
            longitude = 6.9582814,
            types = listOf("tourist_attraction", "church"),
            viewport = null,
            plusCode = null,
            placeIdResolutionMethod = "DIRECT_PLACE_ID",
            placeMatchStatus = "EXACT",
            apiStatus = "PLACE_DETAILS_SUCCESS",
            warnings = emptyList(),
            displayName = LocalizedText("Kölner Dom", "de"),
            rating = 4.8,
            userRatingCount = 50000,
            editorialSummary = LocalizedText("Wahrzeichen von Köln und UNESCO-Weltkulturerbe.", "de"),
            priceLevel = "FREE",
            websiteUri = "https://www.koelner-dom.de",
            regularOpeningHours = OpeningHours(openNow = true, weekdayDescriptions = listOf("Montag: 06:00 - 20:00 Uhr")),
            reviews = reviews
        )
    }

    private fun createStubWikipediaSource(shouldFail: Boolean = false): ContextSource {
        return object : ContextSource {
            override val sourceName: String = "WIKIPEDIA"
            override val sourceType: ContextSourceType = ContextSourceType.ENCYCLOPEDIA

            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                if (shouldFail) {
                    throw RuntimeException("Wikipedia Simulated Error")
                }
                return ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Der Kölner Dom ist eine römisch-katholische Kirche in Köln unter dem Patrozinium des heiligen Petrus.",
                    trustScore = 0.95,
                    confidenceScore = 0.9
                )
            }
        }
    }

    private fun createStubWikivoyageSource(shouldFail: Boolean = false): ContextSource {
        return object : ContextSource {
            override val sourceName: String = "WIKIVOYAGE"
            override val sourceType: ContextSourceType = ContextSourceType.TRAVEL_GUIDE

            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                if (shouldFail) {
                    throw RuntimeException("Wikivoyage Simulated Error")
                }
                return ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = true,
                    snippet = "Köln ist bekannt für seinen Dom. Führungen werden täglich angeboten.",
                    trustScore = 0.9,
                    confidenceScore = 0.85
                )
            }
        }
    }

    @Test
    fun test1_AllSourcesAvailable_AggregatesCorrectly() = runBlocking {
        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = createStubWikipediaSource(),
            wikivoyageContextSource = createStubWikivoyageSource()
        )

        val preParsed = createDummyPlacesResult()
        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Wann wurde das Gebäude gebaut?",
            preParsedPlacesResult = preParsed
        )

        assertNotNull(result)
        assertEquals("Kölner Dom", result.locationName)
        assertEquals(QuestionCategory.HISTORIE_KULTUR, result.executionPlan?.primaryCategory)
        assertTrue(result.sourcesStatus[DataSourceType.PLACES] == true)
        assertTrue(result.sourcesStatus[DataSourceType.REVIEWS] == true)
        assertTrue(result.sourcesStatus[DataSourceType.WIKIPEDIA] == true)
        assertTrue(result.formattedCombinedContext.contains("=== NATIVE FRAGE ZUM ORT ==="))
        assertTrue(result.formattedCombinedContext.contains("=== PLACES API DETAILS ==="))
        assertTrue(result.formattedCombinedContext.contains("=== WIKIPEDIA ETRAG ==="))
    }

    @Test
    fun test2_SingleSourceMissing_HandlesGracefully() = runBlocking {
        val failingWiki = object : ContextSource {
            override val sourceName: String = "WIKIPEDIA"
            override val sourceType: ContextSourceType = ContextSourceType.ENCYCLOPEDIA

            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                return ContextResult(
                    sourceName = sourceName,
                    sourceType = sourceType,
                    isSuccessful = false,
                    metadata = mapOf("status" to "NO_CONTEXT_FOUND")
                )
            }
        }

        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = failingWiki,
            wikivoyageContextSource = createStubWikivoyageSource()
        )

        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Wann wurde das gebaut?",
            preParsedPlacesResult = createDummyPlacesResult()
        )

        assertNotNull(result)
        assertEquals(false, result.sourcesStatus[DataSourceType.WIKIPEDIA])
        assertTrue(result.sourcesStatus[DataSourceType.PLACES] == true)
    }

    @Test
    fun test3_NoReviews_HandlesGracefully() = runBlocking {
        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = createStubWikipediaSource(),
            wikivoyageContextSource = createStubWikivoyageSource()
        )

        val noReviewsPlaces = createDummyPlacesResult(reviews = emptyList())
        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Wie ist die Atmosphäre?",
            preParsedPlacesResult = noReviewsPlaces
        )

        assertNotNull(result)
        assertEquals(0, result.reviews.size)
        assertEquals(false, result.sourcesStatus[DataSourceType.REVIEWS])
        assertFalse(result.formattedCombinedContext.contains("=== NUTZER-REVIEWS"))
    }

    @Test
    fun test4_WikipediaError_DoesNotCrash() = runBlocking {
        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = createStubWikipediaSource(shouldFail = true),
            wikivoyageContextSource = createStubWikivoyageSource()
        )

        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Geschichte des Doms?",
            preParsedPlacesResult = createDummyPlacesResult()
        )

        assertNotNull(result)
        assertEquals(false, result.sourcesStatus[DataSourceType.WIKIPEDIA])
        assertTrue(result.sourcesStatus[DataSourceType.PLACES] == true)
    }

    @Test
    fun test5_WikivoyageError_DoesNotCrash() = runBlocking {
        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = createStubWikipediaSource(),
            wikivoyageContextSource = createStubWikivoyageSource(shouldFail = true)
        )

        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Wegbeschreibung und Zugang?",
            preParsedPlacesResult = createDummyPlacesResult()
        )

        assertNotNull(result)
        assertEquals(false, result.sourcesStatus[DataSourceType.WIKIVOYAGE])
        assertTrue(result.sourcesStatus[DataSourceType.PLACES] == true)
    }

    @Test
    fun test6_EmptyContextData_ReturnsBasicAggregatedResult() = runBlocking {
        val failingSource = object : ContextSource {
            override val sourceName: String = "FAILING"
            override val sourceType: ContextSourceType = ContextSourceType.UNKNOWN
            override suspend fun fetchContext(input: LocationContextInput): ContextResult {
                throw RuntimeException("Error")
            }
        }
        val emptyContextEngine = ContextEngine(sources = listOf(failingSource))
        val failingContextService = GoogleMapsLocationContextService(contextEngine = emptyContextEngine)

        val failingWiki = object : ContextSource {
            override val sourceName: String = "WIKIPEDIA"
            override val sourceType: ContextSourceType = ContextSourceType.ENCYCLOPEDIA
            override suspend fun fetchContext(input: LocationContextInput): ContextResult =
                ContextResult(sourceName, sourceType, false)
        }

        val coordinator = LocationQuestionCoordinator(
            locationContextService = failingContextService,
            wikipediaContextSource = failingWiki
        )

        val result = coordinator.coordinate(
            rawLocationInput = "Unbekannter Ort X",
            userQuestion = "Wie riecht es dort?"
        )

        assertNotNull(result)
        assertEquals("Unbekannter Ort X", result.locationName)
        assertTrue(result.formattedCombinedContext.contains("Unbekannter Ort X"))
    }

    @Test
    fun test7_CorrectAggregationFormatting() = runBlocking {
        val coordinator = LocationQuestionCoordinator(
            wikipediaContextSource = createStubWikipediaSource(),
            wikivoyageContextSource = createStubWikivoyageSource()
        )

        val result = coordinator.coordinate(
            rawLocationInput = "Kölner Dom",
            userQuestion = "Was kostet der Eintritt aktuell?",
            preParsedPlacesResult = createDummyPlacesResult()
        )

        assertNotNull(result)
        assertTrue(result.requiresGrounding)
        assertTrue(result.formattedCombinedContext.contains("=== SUCHE & GROUNDING NOTICE ==="))
    }
}
