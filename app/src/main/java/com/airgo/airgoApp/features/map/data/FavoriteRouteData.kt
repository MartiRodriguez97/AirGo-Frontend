package com.airgo.airgoApp.features.map.data

import com.google.gson.annotations.SerializedName

data class SaveFavoriteRouteRequest(
    @SerializedName("route_id")
    val routeId: Int,
)

data class FavoriteRouteItem(
    val id: Int,
    val route: FavoriteRouteDetail,
    @SerializedName("saved_at")
    val savedAt: String,
)

data class FavoriteRouteDetail(
    val id: Int,
    val name: String,
    @SerializedName("start_location")
    val startLocation: String?,
    @SerializedName("start_address")
    val startAddress: String?,
    @SerializedName("end_location")
    val endLocation: String?,
    @SerializedName("end_address")
    val endAddress: String?,
    val distance: Double,
    @SerializedName("air_quality")
    val airQuality: Double,
    @SerializedName("is_safe")
    val isSafe: Boolean,
    @SerializedName("route_points")
    val routePoints: List<List<Double>> = emptyList(),
)
