package com.airgo.airgoApp

import org.osmdroid.util.GeoPoint

data class PollutionPoint(
    val zone: String,
    val geoPoint: GeoPoint,
    val aqi: Int
)
