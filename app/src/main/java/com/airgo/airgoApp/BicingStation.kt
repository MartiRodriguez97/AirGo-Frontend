package com.airgo.airgoApp

import org.osmdroid.util.GeoPoint

data class BicingStation(
    val id: String,
    val name: String,
    val geoPoint: GeoPoint,
    val mechanicalBikes: Int,
    val electricBikes: Int,
    val freeSlots: Int,
    val status: String // "IN_SERVICE" o "MAINTENANCE"
)