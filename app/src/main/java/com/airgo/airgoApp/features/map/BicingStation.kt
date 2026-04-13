package com.airgo.airgoApp.features.map

data class GeoPoint(
    val lat: Double,
    val lon: Double,
)

data class BicingStation(
    val id: String,
    val name: String,
    val geoPoint: GeoPoint,
    val mechanicalBikes: Int,
    val electricBikes: Int,
    val freeSlots: Int,
    val status: String,
)
