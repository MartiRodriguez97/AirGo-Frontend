package com.airgo.airgoApp.features.activity.data

import com.google.gson.annotations.SerializedName

data class ActivityResponse(
    val id: Int,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("avg_speed_kmh") val avgSpeedKmh: Double,
)
