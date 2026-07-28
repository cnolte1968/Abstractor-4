package com.example.contextpoc

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader

class WikipediaApiClient(
    private val networkFetcher: (String) -> String = Companion::defaultNetworkFetcher
) {
    fun searchTitle(query: String, language: String): List<String> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://$language.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&format=json"
        
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            if (json.has("error")) {
                println("API Error: ${json.getJSONObject("error")}")
            }
            val searchResults = json.getJSONObject("query").getJSONArray("search")
            val titles = mutableListOf<String>()
            for (i in 0 until searchResults.length()) {
                titles.add(searchResults.getJSONObject(i).getString("title"))
            }
            titles
        } catch (e: Exception) {
            println("Exception in searchTitle: ${e.message}")
            emptyList()
        }
    }

    fun geoSearch(lat: Double, lon: Double, language: String, radius: Int = 10000): List<String> {
        val url = "https://$language.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord=$lat|$lon&gsradius=$radius&gslimit=10&format=json"
        
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            val searchResults = json.getJSONObject("query").getJSONArray("geosearch")
            val titles = mutableListOf<String>()
            for (i in 0 until searchResults.length()) {
                titles.add(searchResults.getJSONObject(i).getString("title"))
            }
            titles
        } catch (e: Exception) {
            println("Exception in geoSearch: ${e.message}")
            emptyList()
        }
    }

    fun getExtract(title: String, language: String): String? {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val url = "https://$language.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=false&explaintext=true&titles=$encodedTitle&format=json"
        
        return try {
            val response = networkFetcher(url)
            val json = JSONObject(response)
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val pageId = pages.keys().next()
            if (pageId == "-1") return null
            pages.getJSONObject(pageId).getString("extract")
        } catch (e: Exception) {
            println("Exception in getExtract: ${e.message}")
            null
        }
    }

    companion object {
        private const val userAgent = "RelevantorApp/1.0 (Contact: poctesting@example.com)"

        fun defaultNetworkFetcher(urlString: String): String {
            val url = URL(urlString)
            println("Making real network request to: $urlString")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode != 200) {
                val errorStream = connection.errorStream
                val errorBody = errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("HTTP error code: ${connection.responseCode}, body: $errorBody")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            return response.toString()
        }
    }
}
