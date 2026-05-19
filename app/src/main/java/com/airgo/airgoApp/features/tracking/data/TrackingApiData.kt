package com.airgo.airgoApp.features.tracking.data

import com.google.gson.annotations.SerializedName

data class SaveActivityRequest(
    @SerializedName("distance_meters")
    val distanceMeters: Double,
    @SerializedName("duration_seconds")
    val durationSeconds: Long,
    @SerializedName("completat")
    val completat: Boolean,
    @SerializedName("avg_speed_kmh")
    val avgSpeedKmh: Double,
    @SerializedName("route_points")
    val routePoints: List<List<Double>>,
)

data class BadgeResponse(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val icona: String?
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
    @SerializedName("new_badges")
    val newBadges: List<BadgeResponse>,
    @SerializedName("points_earned")
    val pointsEarned: Int,
    @SerializedName("current_streak")
    val currentStreak: Int
)

data class UpdateUserStatsRequest(
    val punts: Int,
    val ratxa: Int
)
