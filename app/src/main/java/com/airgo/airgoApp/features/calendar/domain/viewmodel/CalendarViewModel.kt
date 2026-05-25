package com.airgo.airgoApp.features.calendar.domain.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.calendar.data.remote.CalendarApiService
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class CalendarViewModel(
    private val apiService: CalendarApiService,
) : ViewModel() {
    private val _plaEntrenamentList = MutableStateFlow<List<TrainingPlanResponse>>(emptyList())
    val plaEntrenamentList: StateFlow<List<TrainingPlanResponse>> = _plaEntrenamentList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _exercicis = MutableStateFlow<List<Exercise>>(emptyList())
    val exercicis: StateFlow<List<Exercise>> = _exercicis
    private val _errorResId = MutableStateFlow<Int?>(null)
    val errorResId: StateFlow<Int?> = _errorResId

    fun fetchPlaEntrenament() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorResId.value = null
            try {
                val responseExercicis = apiService.getActivities()
                if (responseExercicis.isSuccessful) {
                    val totsElsExercicis = responseExercicis.body() ?: emptyList()
                    val exercicisPlanificats = totsElsExercicis.filter { it.dataInici != null }
                    _exercicis.value = exercicisPlanificats
                    SessionManager.exercicis = exercicisPlanificats
                } else {
                    _errorResId.value = R.string.calendar_error_server
                }
                if (!SessionManager.trainingPlans.isNullOrEmpty()) {
                    _plaEntrenamentList.value = SessionManager.trainingPlans!!
                } else {
                    val responsePlans = apiService.getPlaEntrenament()
                    if (responsePlans.isSuccessful) {
                        val body = responsePlans.body() ?: emptyList()
                        _plaEntrenamentList.value = body
                        SessionManager.trainingPlans = body
                    } else {
                        _errorResId.value = R.string.calendar_error_server
                    }
                }
            } catch (e: IOException) {
                _errorResId.value = R.string.calendar_error_network
                e.printStackTrace()
            } catch (e: retrofit2.HttpException) {
                _errorResId.value = R.string.calendar_error_server
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorResId.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                        return CalendarViewModel(RetrofitProvider.calendarApi) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
