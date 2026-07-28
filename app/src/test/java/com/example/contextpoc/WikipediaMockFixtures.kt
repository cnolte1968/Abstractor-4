package com.example.contextpoc

import java.net.URLDecoder

object WikipediaMockFixtures {

    val mockNetworkFetcher: (String) -> String = { urlString ->
        println("Mock fetcher called with URL: $urlString")
        when {
            // EIFFELTURM (DE)
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=Eiffelturm") -> {
                """{"query":{"search":[{"title":"Eiffelturm"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("48.8584|2.2945") -> {
                """{"query":{"geosearch":[{"title":"Eiffelturm"}, {"title":"Champ de Mars"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=Eiffelturm") -> {
                val longExtract = "Der Eiffelturm ist ein 330 Meter hoher Eisenfachwerkturm in Paris... ".repeat(20) // > 500 chars
                """{"query":{"pages":{"123":{"extract":"$longExtract"}}}}"""
            }
            // EIFFELTURM (EN fallback not needed if DE passes)

            // WAT PHO (DE)
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=Wat+Pho") -> {
                """{"query":{"search":[{"title":"Wat Pho"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("13.7465|100.4933") -> {
                """{"query":{"geosearch":[{"title":"Wat Pho"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=Wat+Pho") -> {
                val longExtract = "Der Wat Pho ist ein buddhistischer Tempel... ".repeat(20)
                """{"query":{"pages":{"456":{"extract":"$longExtract"}}}}"""
            }

            // KREUZBERG (DE)
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=Kreuzberg") -> {
                """{"query":{"search":[{"title":"Berlin-Kreuzberg"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("52.4988|13.3918") -> {
                """{"query":{"geosearch":[{"title":"Berlin-Kreuzberg"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=Berlin-Kreuzberg") -> {
                val longExtract = "Berlin-Kreuzberg ist ein Ortsteil im Bezirk Friedrichshain-Kreuzberg... ".repeat(20)
                """{"query":{"pages":{"789":{"extract":"$longExtract"}}}}"""
            }
            
            // CENTRAL PARK (DE)
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=Central+Park") -> {
                """{"query":{"search":[{"title":"Central Park"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("40.7812|-73.9665") -> {
                """{"query":{"geosearch":[{"title":"Central Park"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=Central+Park") -> {
                val longExtract = "Der Central Park ist ein Stadtpark im Zentrum Manhattans... ".repeat(20)
                """{"query":{"pages":{"101":{"extract":"$longExtract"}}}}"""
            }

            // ST. MARY'S CHURCH (DE) -> NO RESULTS
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=St.+Mary%27s+Church") -> {
                """{"query":{"search":[]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("51.5074|-0.1278") -> {
                """{"query":{"geosearch":[]}}"""
            }
            
            // ST. MARY'S CHURCH (EN fallback)
            urlString.contains("en.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=St.+Mary%27s+Church") -> {
                // Return 6 results to trigger AMBIGUOUS
                """{"query":{"search":[{"title":"St Mary's Church, London"}, {"title":"St Mary's Church, Nottingham"}, {"title":"St Mary's Church, Warwick"}, {"title":"St Mary's Church, Beverley"}, {"title":"St Mary's Church, Chesham"}, {"title":"St Mary's Church, Ealing"}]}}"""
            }
            urlString.contains("en.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("51.5074|-0.1278") -> {
                // Return an empty list, so it doesn't match a specific location
                """{"query":{"geosearch":[]}}"""
            }

            urlString.contains("en.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=St+Mary%27s+Church%2C+London") -> {
                """{"query":{"pages":{"102":{"extract":"A church in London."}}}}"""
            }
            
            // PARTIAL MATCH TEST (DE)
            urlString.contains("de.wikipedia") && urlString.contains("list=search") && urlString.contains("srsearch=Partial+Place") -> {
                """{"query":{"search":[{"title":"Partial Place"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("list=geosearch") && urlString.contains("50.0|10.0") -> {
                """{"query":{"geosearch":[{"title":"Partial Place"}]}}"""
            }
            urlString.contains("de.wikipedia") && urlString.contains("prop=extracts") && urlString.contains("titles=Partial+Place") -> {
                val shortExtract = "Short text." // < 500 chars
                """{"query":{"pages":{"103":{"extract":"$shortExtract"}}}}"""
            }

            // SIMULATE NETWORK ERROR
            urlString.contains("error_test") -> {
                throw Exception("Mocked Network Error")
            }

            else -> {
                // Default empty response
                if (urlString.contains("list=search")) """{"query":{"search":[]}}"""
                else if (urlString.contains("list=geosearch")) """{"query":{"geosearch":[]}}"""
                else """{"query":{"pages":{"-1":{}}}}"""
            }
        }
    }
}
