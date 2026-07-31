package com.example.data.contextengine

data class LocationContextInput(
    val placeName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val category: String? = null,
    val rawUrl: String? = null,
    val description: String? = null
)
