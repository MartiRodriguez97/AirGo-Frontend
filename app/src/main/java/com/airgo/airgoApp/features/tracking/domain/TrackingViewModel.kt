package com.airgo.airgoApp.features.tracking.domain

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.features.tracking.data.TrackingData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

private const val TIMER_INTERVAL_MS = 1000L
private const val MS_TO_KMH = 3.6

class TrackingViewModel : ViewModel() {
    var isTracking by mutableStateOf(false)
        private set

    var data by mutableStateOf(TrackingData())
        private set

    var trackPoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    private var timerJob: Job? = null
    private var lastLocation: Location? = null

    fun startTracking() {
        isTracking = true
        data = TrackingData()
        trackPoints = emptyList()
        lastLocation = null
        startTimer()
    }

    fun stopTracking() {
        isTracking = false
        timerJob?.cancel()
    }

    /**
     * Crida cada vegada que arriba una nova posició GPS.
     */
    fun onNewLocation(location: Location) {
        if (!isTracking) return

        val newPoint = GeoPoint(location.latitude, location.longitude)
        trackPoints = trackPoints + newPoint

        val prev = lastLocation
        val addedDistance = if (prev != null) prev.distanceTo(location).toDouble() else 0.0
        lastLocation = location

        data =
            data.copy(
                distanceMeters = data.distanceMeters + addedDistance,
                speedKmh = location.speed * MS_TO_KMH,
            )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (isTracking) {
                    delay(TIMER_INTERVAL_MS)
                    data = data.copy(elapsedSeconds = data.elapsedSeconds + 1)
                }
            }
    }
}
