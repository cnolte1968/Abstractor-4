package com.example.data

object ResponseNormalizer {
    fun normalize(rawJson: String): String {
        var json = rawJson.trim()
        
        // Extract json object if it is wrapped in markdown code blocks
        val firstBrace = json.indexOf('{')
        val lastBrace = json.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            json = json.substring(firstBrace, lastBrace + 1)
        }

        // Clean trailing commas (very common from LLMs)
        json = json.replace(Regex(",\\s*\\}"), "}")
                   .replace(Regex(",\\s*\\]"), "]")

        // Map url variations
        val urlKeys = listOf("originalUrl", "url", "source_url", "sourceUrl", "link")
        for (key in urlKeys) {
            if (!json.contains("\"original_url\"") && json.contains("\"$key\"")) {
                json = json.replace("\"$key\"", "\"original_url\"")
            }
        }

        // Map description variations
        val descKeys = listOf("shortDescription", "summary", "description", "abstract", "kurz_beschreibung", "beschreibung")
        for (key in descKeys) {
            if (!json.contains("\"short_description\"") && json.contains("\"$key\"")) {
                json = json.replace("\"$key\"", "\"short_description\"")
            }
        }

        // Map key_takeaways variations
        val takeawayKeys = listOf(
            "keyTakeaways", "takeaways", "key_points", "keyPoints", "points", 
            "variations", "results", "facts", "opinions", "theses", 
            "kernaussagen", "thesen", "fakten", "meinungen"
        )
        for (key in takeawayKeys) {
            if (!json.contains("\"key_takeaways\"") && json.contains("\"$key\"")) {
                json = json.replace("\"$key\"", "\"key_takeaways\"")
            }
        }

        return json
    }
}
