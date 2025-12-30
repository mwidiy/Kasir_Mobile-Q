package com.example.kasir.data.model

data class Location(
    val id: Int,
    val name: String
)

data class LocationResponse(
    val success: Boolean,
    val message: String,
    val data: List<Location>
)

data class SingleLocationResponse(
    val success: Boolean,
    val message: String,
    val data: Location?
)
