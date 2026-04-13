package com.airgo.airgoApp.features.map

import com.google.gson.annotations.SerializedName

data class BicingStation(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    @SerializedName("bikes_available")
    val numberBikes: Int,
    @SerializedName("docks_available")
    val freeSlots: Int,
    val capacity: Int,
)
