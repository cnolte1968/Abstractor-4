package com.example.data

object GoogleMapsDisambiguator {

    data class UrlInfo(
        val placeName: String?,
        val address: String?,
        val lat: Double?,
        val lng: Double?,
        val placeId: String? = null
    )

    data class Candidate(
        val id: String,
        val name: String,
        val address: String? = null,
        val lat: Double? = null,
        val lng: Double? = null
    )

    fun disambiguate(urlInfo: UrlInfo, candidates: List<Candidate>): Candidate? {
        if (candidates.isEmpty()) return null

        // 1. Place-ID Exact Match
        if (urlInfo.placeId != null) {
            val exactMatch = candidates.find { it.id == urlInfo.placeId }
            if (exactMatch != null) return exactMatch
        }

        // 2. Exact Name Match Priority
        if (!urlInfo.placeName.isNullOrBlank()) {
            val q = normalize(urlInfo.placeName)
            val qNoSpaces = q.replace(" ", "")
            
            val exactNameMatches = candidates.filter {
                val n = normalize(it.name)
                n == q || n.replace(" ", "") == qNoSpaces
            }
            
            if (exactNameMatches.size == 1) {
                return exactNameMatches[0]
            }
        }

        if (candidates.size == 1) {
            val score = calculateScore(urlInfo, candidates[0])
            if (score >= 30.0) return candidates[0]
            return null
        }

        val scoredCandidates = candidates.map { it to calculateScore(urlInfo, it) }
            .sortedByDescending { it.second }

        val best = scoredCandidates[0]
        val second = scoredCandidates[1]

        // Mindestkriterium: mind. 30 Punkte
        if (best.second < 30.0) return null

        // Klare Bewertungsdifferenz (Margin of Victory): mind. 15 Punkte Abstand zum Zweitbesten
        if (best.second - second.second < 15.0) return null

        return best.first
    }

    fun calculateScore(urlInfo: UrlInfo, candidate: Candidate): Double {
        var score = 0.0
        
        // 1. Namensähnlichkeit (max 50)
        score += calculateNameScore(urlInfo.placeName, urlInfo.address, candidate.name, candidate.address)
        
        // 2. Koordinatennähe (max 50)
        score += calculateDistanceScore(urlInfo.lat, urlInfo.lng, candidate.lat, candidate.lng)
        
        return score
    }

    private fun calculateNameScore(placeName: String?, address: String?, candidateName: String, candidateAddress: String?): Double {
        if (placeName.isNullOrBlank()) return 0.0
        
        val q = normalize(placeName)
        val n = normalize(candidateName)
        
        if (q == n) return 50.0
        if (n.replace(" ", "") == q.replace(" ", "")) return 50.0
        
        val qTokens = tokenize(q)
        val nTokens = tokenize(n)
        val aTokens = if (!candidateAddress.isNullOrBlank()) tokenize(normalize(candidateAddress)) else emptySet()
        
        val qCombined = listOfNotNull(placeName, address).joinToString(" ")
        val qCombinedTokens = tokenize(normalize(qCombined))
        
        if (qCombinedTokens.isEmpty()) return 0.0
        
        val combinedCandidateTokens = nTokens + aTokens
        val intersection = qCombinedTokens.intersect(combinedCandidateTokens).size
        
        if (intersection == 0) {
            // No textual overlap at all! Penalize heavily so it doesn't win purely on distance.
            return -20.0
        }
        
        val coverage = intersection.toDouble() / qCombinedTokens.size
        
        // Base token score is up to 35.0 to ensure exact matches (50.0) win decisively
        var score = coverage * 35.0
        
        // Penalize extra tokens in the candidate name to prioritize cleaner matches
        val nameIntersection = qCombinedTokens.intersect(nTokens).size
        val extraNameTokens = nTokens.size - nameIntersection
        if (extraNameTokens > 0) {
            score -= (extraNameTokens * 2.0)
        }
        
        return Math.max(-20.0, score) // allow negative score for terrible matches
    }

    private fun calculateDistanceScore(lat1: Double?, lng1: Double?, lat2: Double?, lng2: Double?): Double {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0.0
        
        val dist = calculateDistanceMeters(lat1, lng1, lat2, lng2)
        // 0m = 50, 500m = 0
        return Math.max(0.0, 50.0 - (dist * 0.1))
    }

    private fun normalize(text: String): String {
        return text.lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9äöüß ]"), "")
            .trim()
    }

    private fun tokenize(text: String): Set<String> {
        return text.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
