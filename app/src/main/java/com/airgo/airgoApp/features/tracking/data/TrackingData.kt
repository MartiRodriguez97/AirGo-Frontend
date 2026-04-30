package com.airgo.airgoApp.features.tracking.data

data class TrackingData(
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val speedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
)
