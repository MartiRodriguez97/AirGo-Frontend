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
}
