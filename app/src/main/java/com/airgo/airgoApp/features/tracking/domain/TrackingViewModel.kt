package com.airgo.airgoApp.features.tracking.domain

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.AppEvents
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.tracking.data.SaveActivityRequest
import com.airgo.airgoApp.features.tracking.data.TrackingData
import com.airgo.airgoApp.features.tracking.helpers.TrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.HttpException
import java.io.IOException

private const val TIMER_INTERVAL_MS = 1000L
private const val MS_TO_KMH = 3.6
private const val METERS_PER_KM = 1000.0

class TrackingViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

    var isTracking by mutableStateOf(false)
        private set

    var hasSummary by mutableStateOf(false)
        private set

    var data by mutableStateOf(TrackingData())
        private set

    var trackPoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    var showSensationDialog by mutableStateOf(false)
        private set

    var selectedSensation by mutableStateOf<Int?>(null)
        private set

    private val _uiEvent = MutableSharedFlow<TrackingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var locationReceiver: android.content.BroadcastReceiver? = null
    private var receiverContext: android.content.Context? = null
    var saveState by mutableStateOf<SaveState>(SaveState.Idle)
        private set

    sealed class SaveState {
        data object Idle : SaveState()

        data object Saving : SaveState()

        data object Success : SaveState()

        data class Error(
            val message: String,
        ) : SaveState()
    }

    fun startTracking() {
        isTracking = true
        hasSummary = false
        saveState = SaveState.Idle
        data = TrackingData()
        trackPoints = emptyList()
        lastLocation = null
        showSensationDialog = false
        selectedSensation = null

        // start timer
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (isTracking) {
                    delay(TIMER_INTERVAL_MS)
                    data = data.copy(elapsedSeconds = data.elapsedSeconds + 1)
                }
            }
    }

    fun stopTracking() {
        isTracking = false
        timerJob?.cancel()

        if (data.distanceMeters > 0 || data.elapsedSeconds > 0) {
            showSensationDialog = true
        }
    }

    fun selectSensation(valor: Int) {
        selectedSensation = valor
        showSensationDialog = false
        hasSummary = true
    }

    fun skipSensation() {
        selectedSensation = null
        showSensationDialog = false
        hasSummary = true
    }

    fun submitSensation(sensationValue: Int?) {
        selectedSensation = sensationValue
        showSensationDialog = false
        hasSummary = true // Ahora sí, navegamos al resumen
    }

    private fun saveActivityPlanning() {
        val activeExercise = SessionManager.exerciciActiu ?: return
        val distanceDoneKm = data.distanceMeters / METERS_PER_KM

        viewModelScope.launch {
            try {
                // Sumamos la distancia recorrida a la que ya tenía
                val totalDistance = activeExercise.distanciaFeta + distanceDoneKm
                val isCompleted = totalDistance >= activeExercise.distanciaObjectiu

                Log.d(
                    "TrackingViewModel",
                    "Updating exercise ${activeExercise.id}:" +
                        " distance=$totalDistance," +
                        " completed=$isCompleted",
                )

                repository.updateExercise(
                    id = activeExercise.id,
                    completat = isCompleted,
                    distanciaFeta = totalDistance,
                )

                // Limpiar ejercicio activo tras completar/guardar
                SessionManager.exerciciActiu = null
                SessionManager.nomTemplateActiu = null

                // Actualizar la lista de ejercicios de hoy en el SessionManager
                // Esto forzará que desaparezca de la lista de pendientes en la Home si se completó
                if (isCompleted) {
                    SessionManager.exercicis = SessionManager.exercicis?.filter { it.id != activeExercise.id }
                } else {
                    // Si no está completado, actualizamos la distancia en el objeto de la lista
                    SessionManager.exercicis =
                        SessionManager.exercicis?.map {
                            if (it.id == activeExercise.id) it.copy(distanciaFeta = totalDistance) else it
                        }
                }

                _uiEvent.emit(TrackingUiEvent.SaveSuccess(isCompleted))
            } catch (e: java.io.IOException) {
                Log.e("TrackingViewModel", "Network error saving exercise progress", e)
                _uiEvent.emit(TrackingUiEvent.SaveError("Error de connexió en guardar l'activitat"))
            } catch (e: retrofit2.HttpException) {
                Log.e("TrackingViewModel", "HTTP error saving exercise progress", e)
                _uiEvent.emit(TrackingUiEvent.SaveError("Error del servidor en guardar l'activitat"))
            }
        }
    }

    fun clearSummary() {
        hasSummary = false
        trackPoints = emptyList()
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

        val newDistance = data.distanceMeters + addedDistance
        val avgSpeed =
            if (data.elapsedSeconds > 0) {
                newDistance / data.elapsedSeconds * MS_TO_KMH
            } else {
                0.0
            }

        data =
            data.copy(
                distanceMeters = newDistance,
                speedKmh = location.speed * MS_TO_KMH,
                avgSpeedKmh = avgSpeed,
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerLocationReceiver(context: android.content.Context) {
        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    ctx: android.content.Context?,
                    intent: android.content.Intent?,
                ) {
                    Log.d("TrackingViewModel", "Rebut broadcast: ${intent?.action}")
                    if (intent?.action != TrackingService.ACTION_LOCATION_UPDATE) return
                    val lat = intent.getDoubleExtra(TrackingService.EXTRA_LATITUDE, 0.0)
                    val lon = intent.getDoubleExtra(TrackingService.EXTRA_LONGITUDE, 0.0)
                    val speed = intent.getFloatExtra(TrackingService.EXTRA_SPEED, 0f)
                    val location =
                        android.location.Location("").apply {
                            latitude = lat
                            longitude = lon
                            this.speed = speed
                        }
                    onNewLocation(location)
                }
            }
        val filter = android.content.IntentFilter(TrackingService.ACTION_LOCATION_UPDATE)
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        locationReceiver = receiver
        receiverContext = context
    }

    fun unregisterLocationReceiver() {
        locationReceiver?.let {
            receiverContext?.let { ctx ->
                ctx.unregisterReceiver(it)
            }
        }
        locationReceiver = null
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun saveActivity() {
        viewModelScope.launch {
            saveState = SaveState.Saving
            try {
                val request =
                    SaveActivityRequest(
                        distanceMeters = data.distanceMeters,
                        durationSeconds = data.elapsedSeconds,
                        avgSpeedKmh = data.avgSpeedKmh,
                        routePoints = trackPoints.map { listOf(it.latitude, it.longitude) },
                        sensacio = selectedSensation,
                    )
                RetrofitProvider.trackingApi.saveActivity(request)
                saveActivityPlanning()
                AppEvents.notifyActivitySaved()
                saveState = SaveState.Success
            } catch (e: IOException) {
                saveState = SaveState.Error("Error de connexió: ${e.message}")
            } catch (e: HttpException) {
                saveState = SaveState.Error("Error del servidor: ${e.code()}")
            }
        }
    }

    fun clearSaveState() {
        saveState = SaveState.Idle
    }
}

sealed class TrackingUiEvent {
    data class SaveSuccess(
        val completed: Boolean,
    ) : TrackingUiEvent()

    data class SaveError(
        val message: String,
    ) : TrackingUiEvent()
}
