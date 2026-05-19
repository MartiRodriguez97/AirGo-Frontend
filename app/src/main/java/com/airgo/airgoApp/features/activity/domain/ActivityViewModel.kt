package com.airgo.airgoApp.features.activity.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.features.activity.data.ActivityResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import com.airgo.airgoApp.features.activity.data.UpdateSensationRequest

sealed class ActivityUiState {
    object Loading : ActivityUiState()

    object Empty : ActivityUiState()

    data class Success(
        val activities: List<ActivityResponse>,
    ) : ActivityUiState()

    data class Error(
        val message: String,
    ) : ActivityUiState()
}

class ActivityViewModel : ViewModel() {
    companion object {
        private const val TAG = "ActivityViewModel"
    }

    private val _uiState = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val uiState: StateFlow<ActivityUiState> = _uiState

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = ActivityUiState.Loading
            try {
                val activities = RetrofitProvider.activityApi.getActivities()
                _uiState.value =
                    if (activities.isEmpty()) {
                        ActivityUiState.Empty
                    } else {
                        ActivityUiState.Success(activities)
                    }
            } catch (e: IOException) {
                Log.e(TAG, "Network error loading activities", e)
                _uiState.value = ActivityUiState.Error("Error de connexió")
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} loading activities", e)
                _uiState.value = ActivityUiState.Error("Error del servidor: ${e.code()}")
            }
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitProvider.activityApi.deleteActivity(id)
                if (response.isSuccessful) {
                    // Recargar la lista tras eliminar
                    loadActivities()
                } else {
                    Log.e(TAG, "Delete failed: ${response.code()}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error deleting activity", e)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} deleting activity", e)
            }
        }
    }

    fun updateActivitySensation(activityId: Int, newSensationValue: Int) {
        viewModelScope.launch {
            try {
                // 1. Preparamos el cuerpo de la petición
                val request = UpdateSensationRequest(sensacio = newSensationValue)

                // 2. Hacemos la llamada PATCH a la API
                val response = RetrofitProvider.activityApi.updateActivitySensation(activityId, request)

                if (response.isSuccessful) {
                    // 3. Si va bien, actualizamos la lista local en la UI sin hacer otra petición GET
                    val currentState = _uiState.value
                    if (currentState is ActivityUiState.Success) {
                        val updatedActivities = currentState.activities.map { activity ->
                            if (activity.id == activityId) {
                                // Reemplazamos la actividad vieja por una copia con la nueva sensación
                                activity.copy(sensacio = newSensationValue)
                            } else {
                                activity
                            }
                        }
                        // Emitimos la lista actualizada
                        _uiState.value = ActivityUiState.Success(updatedActivities)
                    }
                    Log.d(TAG, "Sensation updated successfully for activity $activityId")
                } else {
                    Log.e(TAG, "Update failed: ${response.code()}")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error updating sensation", e)
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP ${e.code()} updating sensation", e)
            }
        }
    }
}
