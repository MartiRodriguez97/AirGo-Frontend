package com.airgo.airgoApp.features.activity.data

import com.google.gson.annotations.SerializedName

data class ActivityResponse(
    val id: Int,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("avg_speed_kmh") val avgSpeedKmh: Double,
    @SerializedName("route_points") val routePoints: List<List<Double>>? = null,
    @SerializedName("sensacio") val sensacio: Int? = null,
    @SerializedName("comentari") val comentari: String? = null,
    @SerializedName("dataInici") val dataInici: String? = null,
)

data class UpdateSensationRequest(
    @SerializedName("sensacio") val sensacio: Int,
)

data class UpdateCommentRequest(
    @SerializedName("comentari") val comentari: String,
)
