package com.airgo.airgoApp.features.map

data class PollutionPoint(
    val zone: String,
    val geoPoint: GeoPointResponse,
    val aqi: Int,
)

data class GeoPointResponse(
    val lat: Double,
    val lon: Double,
)
