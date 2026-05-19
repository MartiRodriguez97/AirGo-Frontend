package com.airgo.airgoApp.features.tracking.data

import com.google.gson.annotations.SerializedName

data class SaveActivityRequest(
    @SerializedName("distance_meters")
    val distanceMeters: Double,
    @SerializedName("duration_seconds")
    val durationSeconds: Long,
    @SerializedName("avg_speed_kmh")
    val avgSpeedKmh: Double,
    @SerializedName("route_points")
    val routePoints: List<List<Double>>,
    @SerializedName("sensacio")
    val sensacio: Int? = null,
)

data class SaveActivityResponse(
    val id: Int,
    @SerializedName("distance_meters")
    val distanceMeters: Double,
    @SerializedName("duration_seconds")
    val durationSeconds: Long,
    @SerializedName("avg_speed_kmh")
    val avgSpeedKmh: Double,
    @SerializedName("route_points")
    val routePoints: List<List<Double>>,
)
