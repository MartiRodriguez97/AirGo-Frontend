package com.airgo.airgoApp.features.tracking.domain

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
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

    var titolsPendents by mutableStateOf(0)
        internal set

    var availableTitles by mutableStateOf<List<com.airgo.airgoApp.features.tracking.data.TitleOption>>(emptyList())
        internal set

    var showTitleSelectionDialog by mutableStateOf(false)

    var selectedSensation by mutableStateOf<Int?>(null)
        private set

    private val _uiEvent = MutableSharedFlow<TrackingUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var lastLocation: Location? = null
    private var locationReceiver: android.content.BroadcastReceiver? = null
    private var receiverContext: android.content.Context? = null
    var saveState by mutableStateOf<SaveState>(SaveState.Idle)
        internal set

    sealed class SaveState {
        data object Idle : SaveState()

        data object Saving : SaveState()

        data class Success(
            val response: com.airgo.airgoApp.features.tracking.data.SaveActivityResponse,
        ) : SaveState()

        data class Error(
            val message: UiText.StringResource,
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

    fun submitSensation(sensationValue: Int? = null) {
        selectedSensation = sensationValue
        showSensationDialog = false
        hasSummary = true
    }

    internal suspend fun updateExerciseProgress(
        activeExercise: Exercise,
        distanceDoneKm: Double,
    ) {
        try {
            val totalDistance = activeExercise.distanciaFeta + distanceDoneKm
            val isCompleted = totalDistance >= activeExercise.distanciaObjectiu

            repository.updateExercise(
                id = activeExercise.id,
                completat = isCompleted,
                distanciaFeta = totalDistance,
            )

            SessionManager.exerciciActiu = null
            SessionManager.nomTemplateActiu = null

            if (isCompleted) {
                SessionManager.exercicis = SessionManager.exercicis?.filter { it.id != activeExercise.id }
            } else {
                SessionManager.exercicis =
                    SessionManager.exercicis?.map {
                        if (it.id == activeExercise.id) it.copy(distanciaFeta = totalDistance) else it
                    }
            }

            _uiEvent.emit(TrackingUiEvent.SaveSuccess(isCompleted))
        } catch (e: IOException) {
            Log.e("TrackingViewModel", "Network error saving exercise progress", e)
            saveState = SaveState.Error(UiText.StringResource(R.string.error_network_generic))
        } catch (e: HttpException) {
            Log.e("TrackingViewModel", "HTTP error saving exercise progress", e)
            saveState = SaveState.Error(UiText.StringResource(R.string.error_server_generic, e.code()))
        }
    }

    fun clearSummary() {
        hasSummary = false
        trackPoints = emptyList()
        saveState = SaveState.Idle
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
                    if (intent?.action != TrackingService.ACTION_LOCATION_UPDATE) return
                    val lat = intent.getDoubleExtra(TrackingService.EXTRA_LATITUDE, 0.0)
                    val lon = intent.getDoubleExtra(TrackingService.EXTRA_LONGITUDE, 0.0)
                    val speed = intent.getFloatExtra(TrackingService.EXTRA_SPEED, 0f)
                    val location =
                        Location("").apply {
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
