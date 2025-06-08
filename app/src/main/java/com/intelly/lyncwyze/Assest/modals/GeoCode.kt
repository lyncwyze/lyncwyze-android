package com.intelly.lyncwyze.Assest.modals

data class GeoCode(
    val placeId: String,
    val formattedAddress: String,
    val location: GeoCodeLocation
)

data class GeoCodeLocation(
    val x: Double,
    val y: Double,
    val coordinates: List<Double>,
    val type: String
)