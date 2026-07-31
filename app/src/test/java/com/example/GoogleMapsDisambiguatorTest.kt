package com.example

import com.example.data.GoogleMapsDisambiguator
import com.example.data.GoogleMapsDisambiguator.Candidate
import com.example.data.GoogleMapsDisambiguator.UrlInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleMapsDisambiguatorTest {

    @Test
    fun `eindeutiger Treffer ueber Name und Koordinaten`() {
        val urlInfo = UrlInfo(placeName = "Restaurant Delphi", address = null, 52.5200, 13.4050)
        val candidates = listOf(
            Candidate("1", "Restaurant Delphi", "Berlin", 52.5201, 13.4051), // Sehr nah
            Candidate("2", "Pizza Haus", "Berlin", 52.5200, 13.4050) // Selbe Koordinaten, falscher Name
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }

    @Test
    fun `leicht abweichender Name wird erkannt`() {
        val urlInfo = UrlInfo(placeName = "Delphi Berlin", address = null, 52.5200, 13.4050)
        val candidates = listOf(
            Candidate("1", "Restaurant Delphi", "Alexanderplatz, Berlin", 52.5201, 13.4051),
            Candidate("2", "Pizza Haus", "Berlin", 52.5200, 13.4050)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }

    @Test
    fun `mehrere nahe gleichnamige Treffer sind ambiguous`() {
        val urlInfo = UrlInfo(placeName = "Starbucks", address = null, 52.5200, 13.4050)
        val candidates = listOf(
            Candidate("1", "Starbucks", "Berlin", 52.5201, 13.4051), // Nah
            Candidate("2", "Starbucks", "Berlin", 52.5199, 13.4049)  // Auch nah (weniger als 15 Punkte Score Differenz)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNull("Erwartet null wegen Ambiguitaet", result)
    }

    @Test
    fun `Kartenkoordinaten liegen nicht exakt auf dem POI aber nah genug`() {
        val urlInfo = UrlInfo(placeName = "Delphi", address = null, 52.5200, 13.4050)
        val candidates = listOf(
            Candidate("1", "Restaurant Delphi", "Berlin", 52.5220, 13.4050), // ca. 200m entfernt (Punktabzug, aber Name passt)
            Candidate("2", "Pizza Haus", "Berlin", 52.5200, 13.4050) // 0m entfernt, falscher Name
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }

    @Test
    fun `fehlende Koordinaten klappen auch wenn Name sehr gut matcht`() {
        val urlInfo = UrlInfo(placeName = "Delphi Berlin", address = null, null, null)
        val candidates = listOf(
            Candidate("1", "Restaurant Delphi", "Berlin", 52.5200, 13.4050),
            Candidate("2", "Pizza Haus", "Berlin", 52.5200, 13.4050)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }

    @Test
    fun `echter mehrdeutiger Fall ohne ausreichende Abhebung`() {
        val urlInfo = UrlInfo(placeName = "Delphi", address = null, null, null)
        val candidates = listOf(
            Candidate("1", "Restaurant Delphi", "Hamburg", 53.5500, 9.9900),
            Candidate("2", "Restaurant Delphi", "Muenchen", 48.1300, 11.5800)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNull("Muss ambiguous sein, weil beide genau gleich gut passen (nur Name)", result)
    }

    @Test
    fun `identische Place-ID priorisiert sofort als BEST_MATCH`() {
        val urlInfo = UrlInfo(placeName = "Completely Wrong Name", address = null, lat = 0.0, lng = 0.0, placeId = "123")
        val candidates = listOf(
            Candidate("456", "Perfect Name", "Berlin", 52.5200, 13.4050),
            Candidate("123", "Weird Name", "Hamburg", 53.5500, 9.9900) // matches Place-ID
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("123", result?.id)
    }

    @Test
    fun `schlechter Kandidat (Name und Koordinaten passen nicht) fuehrt zu NO_MATCH`() {
        val urlInfo = UrlInfo(placeName = "Berlin Hauptbahnhof", address = null, 52.5251, 13.3694)
        val candidates = listOf(
            Candidate("1", "Muenchen Flughafen", "Muenchen", 48.3537, 11.7368)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNull("Muss null sein, weil der einzige Kandidat das 30 Punkte Minimum nicht erreicht", result)
    }

    @Test
    fun `nur Koordinaten vorhanden`() {
        val urlInfo = UrlInfo(placeName = null, address = null, lat = 52.5200, lng = 13.4050)
        val candidates = listOf(
            Candidate("1", "Restaurant", "Berlin", 52.5200, 13.4050),
            Candidate("2", "Bar", "Berlin", 52.5250, 13.4100)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }

    @Test
    fun `exakter Name schlaegt Kandidaten mit Namenszusaetzen deutlich`() {
        // "Porjai Massage" vs "Porjai Thai Massage"
        // Since they have the same location or no location, the exact name should win definitively.
        val urlInfo = UrlInfo(placeName = "Porjai Massage", address = null, 52.5200, 13.4050)
        val candidates = listOf(
            Candidate("1", "Porjai Massage", "Berlin", 52.5200, 13.4050),
            Candidate("2", "Porjai Thai Massage", "Berlin", 52.5200, 13.4050),
            Candidate("3", "Porjai Massage & Spa", "Berlin", 52.5200, 13.4050)
        )

        val result = GoogleMapsDisambiguator.disambiguate(urlInfo, candidates)
        assertNotNull(result)
        assertEquals("1", result?.id)
    }
}
