package com.airgo.airgoApp.features.map.data

data class GeoPoint(
    val lat: Double,
    val lon: Double,
) {
    val longitude: Double get() = lon
    val latitude: Double get() = lat
}

data class BicingStation(
    val id: String,
    val name: String,
    val geoPoint: GeoPoint,
    val mechanicalBikes: Int,
    val electricBikes: Int,
    val freeSlots: Int,
    val status: String,
)
