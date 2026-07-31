package com.example

import org.junit.Test
import com.example.data.GoogleMapsUrlParser

class ReviewTest {
    @Test
    fun testReviewUrl() {
        val url = "https://maps.app.goo.gl/ujohQk4gcoFGzv3L8"
        println("=== REVIEW RESULTS ===")
        val result = GoogleMapsUrlParser.parseGoogleMapsUrl("Shared text", url, "https://www.google.com/maps/place/SomePlace/data=!4m2!3m1!1s0x0:0x0", "SUCCESS")
        println("placeName: ${result.placeName}")
        println("======================")
    }
}
