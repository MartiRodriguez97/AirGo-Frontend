package com.airgo.airgoApp.features.tracking.domain

import android.location.Location
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.tracking.data.TrackingData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

private const val TIMER_INTERVAL_MS = 1000L
private const val MS_TO_KMH = 3.6

class TrackingViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

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
        saveActivity()
    }

    private fun saveActivity() {
        val activeExercise = SessionManager.exerciciActiu ?: return
        val distanceDoneKm = data.distanceMeters / 1000.0

        viewModelScope.launch {
            try {
                // Sumamos la distancia recorrida a la que ya tenía
                val totalDistance = activeExercise.distanciaFeta + distanceDoneKm
                val isCompleted = totalDistance >= activeExercise.distanciaObjectiu

                Log.d("TrackingViewModel", "Updating exercise ${activeExercise.id}: distance=$totalDistance, completed=$isCompleted")

                repository.updateExercise(
                    id = activeExercise.id,
                    completat = isCompleted,
                    distanciaFeta = totalDistance
                )

                // Limpiar ejercicio activo tras completar/guardar
                SessionManager.exerciciActiu = null
                SessionManager.nomTemplateActiu = null
                
                // Actualizar la lista de ejercicios de hoy en el SessionManager
                // Esto forzará que desaparezca de la lista de pendientes en la Home si se completó
                SessionManager.exercicis = SessionManager.exercicis?.filter { it.id != activeExercise.id }
            } catch (e: Exception) {
                Log.e("TrackingViewModel", "Error saving exercise progress", e)
            }
        }
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
