package com.example

import com.example.data.GoogleMapsUrlParser
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class LiveTest {
    @Test
    fun analyzeUrl() = runBlocking {
        val originalSharedUrl = "https://maps.app.goo.gl/ujohQk4gcoFGzv3L8"
        
        val conn = URL(originalSharedUrl).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = false
        val location = conn.getHeaderField("Location")
        println("REDIRECT_URL: $location")

        val mapsResult = GoogleMapsUrlParser.parseGoogleMapsUrl(originalSharedUrl, originalSharedUrl, location ?: "", "SUCCESS")
        
        println("PARSED_PLACE_ID: ${mapsResult.placeId}")
        println("PARSED_CID: ${mapsResult.cid}")
        println("PARSED_NAME: ${mapsResult.placeName}")
        println("PARSED_QUERY: ${mapsResult.searchQuery}")
        println("PARSED_LAT: ${mapsResult.latitude}")
        println("PARSED_LNG: ${mapsResult.longitude}")
        println("PARSED_WARNINGS: ${mapsResult.warnings}")
    }
}
