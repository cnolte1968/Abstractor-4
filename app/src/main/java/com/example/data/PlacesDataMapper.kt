package com.example.data

object PlacesDataMapper {

    fun mapToGeminiInput(result: GooglePlacesPoCResult): String {
        val sb = StringBuilder()
        sb.append("=== LOCATION_ANALYSIS_DATA ===\n\n")

        val name = result.displayName?.text ?: result.urlDerivedName ?: "Unbekannter Ort"
        sb.append("NAME: $name\n")

        val categories = result.types?.joinToString(", ") ?: "Keine Kategorien verfügbar"
        sb.append("KATEGORIEN: $categories\n")

        val address = result.formattedAddress ?: "Keine Adresse verfügbar"
        sb.append("ADRESSE: $address\n")

        val description = result.editorialSummary?.text ?: "Keine offizielle Beschreibung verfügbar."
        sb.append("BESCHREIBUNG: $description\n")

        val ratingVal = result.rating ?: 0.0
        val countVal = result.userRatingCount ?: 0
        sb.append("BEWERTUNG: $ratingVal Sterne ($countVal Bewertungen)\n")

        val priceLevelText = when (result.priceLevel) {
            "PRICE_LEVEL_FREE" -> "Kostenlos"
            "PRICE_LEVEL_INEXPENSIVE" -> "Günstig"
            "PRICE_LEVEL_MODERATE" -> "Moderat (normales Preisniveau)"
            "PRICE_LEVEL_EXPENSIVE" -> "Gehoben / Teuer"
            "PRICE_LEVEL_VERY_EXPENSIVE" -> "Sehr teuer"
            null -> "Keine Information zum Preisniveau verfügbar."
            else -> result.priceLevel
        }
        sb.append("PREISNIVEAU: $priceLevelText\n")

        val website = result.websiteUri ?: "Keine Website vorhanden"
        sb.append("WEBSITE: $website\n")

        sb.append("\n=== ÖFFNUNGSZEITEN ===\n")
        val openingHours = result.regularOpeningHours?.weekdayDescriptions
        if (!openingHours.isNullOrEmpty()) {
            for (day in openingHours) {
                sb.append("- $day\n")
            }
        } else {
            sb.append("Keine Informationen zu den Öffnungszeiten vorhanden.\n")
        }

        sb.append("\n=== REVIEWS / BEZUGNAHMEN ===\n")
        val reviewsList = result.reviews
        if (!reviewsList.isNullOrEmpty()) {
            for ((index, review) in reviewsList.withIndex()) {
                val author = review.authorAttribution?.displayName ?: "Anonymer Besucher"
                val reviewRating = review.rating ?: 0.0
                val textContent = review.text?.text ?: review.originalText?.text ?: "(Kein Bewertungstext)"
                val timeDesc = review.relativePublishTimeDescription ?: ""
                sb.append("--- Bewertung #${index + 1} ---\n")
                sb.append("Autor: $author ($timeDesc)\n")
                sb.append("Sterne: $reviewRating von 5\n")
                sb.append("Inhalt: $textContent\n\n")
            }
        } else {
            sb.append("Keine Bewertungen oder Erfahrungsberichte vorhanden.\n")
        }

        return sb.toString()
    }
}
