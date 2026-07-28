package com.example.contextpoc

data class ContextCandidate(
    val wikipediaTitle: String,
    val description: String,
    val objectType: String, // e.g., "monument", "park", "museum"
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val wikidataId: String? = null
)
