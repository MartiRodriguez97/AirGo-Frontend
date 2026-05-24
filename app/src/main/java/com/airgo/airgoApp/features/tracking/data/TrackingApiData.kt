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
    @SerializedName("sensacio")
    val sensacio: Int? = null,
)

data class BadgeResponse(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val icona: String?,
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
    @SerializedName("points_earned_total")
    val pointsEarned: Int,
    @SerializedName("current_streak")
    val currentStreak: Int,
    @SerializedName("titols_pendents")
    val titolsPendents: Int,
)

data class TitleOption(
    val id: Int,
    val nom: String,
    val descripcio: String,
    @SerializedName("punts_minims")
    val puntsMinims: Int,
)

data class AvailableTitlesResponse(
    @SerializedName("titols_pendents")
    val titolsPendents: Int,
    val opcions: List<TitleOption>,
)

data class UnlockTitleRequest(
    @SerializedName("titol_id")
    val titolId: Int,
)

data class UnlockTitleResponse(
    val status: String,
    @SerializedName("titol_desbloquejat")
    val titolDesbloquejat: String,
)

data class UpdateUserStatsRequest(
    @SerializedName("punts")
    val punts: Int,
    @SerializedName("ratxa")
    val ratxa: Int,
)
