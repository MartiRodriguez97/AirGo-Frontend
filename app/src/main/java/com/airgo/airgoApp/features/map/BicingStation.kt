package com.airgo.airgoApp.features.map

data class BicingStation(
    val id: String,
    val name: String,
    val geoPoint: GeoPointResponse?,
    val mechanicalBikes: Int,
    val electricBikes: Int,
    val freeSlots: Int,
    val status: String,
)
