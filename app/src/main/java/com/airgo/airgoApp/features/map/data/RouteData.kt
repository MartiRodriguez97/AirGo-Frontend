package com.airgo.airgoApp.features.map.data

import com.google.gson.annotations.SerializedName

enum class TransportProfile(val value: String) {
    BIKE("eco_bike"),
    FOOT("eco_foot"),
    RUNNING("running"),
}

data class RouteRequest(
    @SerializedName("profile") val profile: String,
    @SerializedName("points") val points: List<List<Double>>,
)

data class RouteResponse(
    val status: String,
    @SerializedName("route_id") val routeId: Int,
    val summary: RouteSummary,
    val geometry: RouteGeometry,
    @SerializedName("pollution_details") val pollutionDetails: List<List<Double>>,
    @SerializedName("stations_info") val stationsInfo: List<StationInfo>,
)

data class RouteGeometry(
    val type: String,
    val coordinates: List<List<Double>>,
)

data class RouteSummary(
    @SerializedName("duration_minutes") val durationMinutes: Double,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("aqi_stations_detected") val aqiStationsDetected: Int,
)

data class StationInfo(
    val zone: String,
    val geoPoint: GeoPointData,
    val aqi: Int,
)

data class GeoPointData(
    val lat: Double,
    val lon: Double,
)