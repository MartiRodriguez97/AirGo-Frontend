package com.airgo.airgoApp.features.map.helpers

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
)