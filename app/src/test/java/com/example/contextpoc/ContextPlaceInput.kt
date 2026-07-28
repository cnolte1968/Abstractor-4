package com.example.contextpoc

data class ContextPlaceInput(
    val name: String,
    val address: String,
    val city: String,
    val country: String,
    val placeTypes: List<String>,
    val latitude: Double,
    val longitude: Double
)
