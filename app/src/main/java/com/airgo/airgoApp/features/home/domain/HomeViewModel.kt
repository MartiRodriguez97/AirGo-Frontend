package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.home.data.TrainingPlanRequest
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class HomeViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

    private val _plans = MutableStateFlow<List<TrainingPlanResponse>>(emptyList())
    val plans: StateFlow<List<TrainingPlanResponse>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<Int?>(null)
    val errorMessage: StateFlow<Int?> = _errorMessage.asStateFlow()

    init {
        loadPlans()
    }

    fun updateTodayExercises() {
        val today = java.time.LocalDate.now().toString() // "yyyy-MM-dd"
        Log.d("HomeViewModel", "Updating today's exercises for date: $today")

        val todayExercises = _plans.value.flatMap { plan ->
            plan.templates.flatMap { template ->
                template.exercicis.filter { exercise ->
                    exercise.dataInici?.startsWith(today) == true && !exercise.completat
                }
            }
        }
        Log.d("HomeViewModel", "Found ${todayExercises.size} pending exercises for today")
        SessionManager.exercicis = todayExercises
    }

    fun startExercise(exercise: Exercise) {
        val template = _plans.value.flatMap { it.templates }
            .find { it.exercicis.any { ex -> ex.id == exercise.id } }

        SessionManager.exerciciActiu = exercise
        SessionManager.nomTemplateActiu = template?.nom ?: "Activitat"
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun loadPlans() {
        if (!SessionManager.trainingPlans.isNullOrEmpty()) {
            _plans.value = SessionManager.trainingPlans!!
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val url = com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT
                Log.d("HomeViewModel", "Requesting GET training plans from URL: $url")

                val fetchedPlans = repository.getTrainingPlans().filterNotNull()
                Log.d("HomeViewModel", "Successfully fetched ${fetchedPlans.size} plans: $fetchedPlans")

                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
                updateTodayExercises()
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("HomeViewModel", "HTTP Error $code: $errorBody", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
            } catch (e: java.io.IOException) {
                Log.e("HomeViewModel", "Network error (IOException): ${e.message}", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
                // Fallback to SessionManager if available
                SessionManager.trainingPlans?.let {
                    Log.d("HomeViewModel", "Using cached plans from SessionManager as fallback")
                    _plans.value = it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlan(
        request: TrainingPlanRequest,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newPlan = repository.createTrainingPlan(request)
                Log.d("HomeViewModel", "Create plan response: $newPlan")

                val currentPlans = _plans.value.toMutableList()
                currentPlans.add(newPlan)
                SessionManager.trainingPlans = currentPlans
                _plans.value = currentPlans
                onSuccess()
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Error creating plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "Error creating plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlan(
        id: Int,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTrainingPlan(id)
                val currentPlans = _plans.value.toMutableList()
                currentPlans.removeAll { it.id == id }
                SessionManager.trainingPlans = currentPlans
                _plans.value = currentPlans
                onSuccess()
            } catch (e: java.io.IOException) {
                Log.e("HomeViewModel", "Error deleting plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_delete_plan
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "Error deleting plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_delete_plan
            } finally {
                _isLoading.value = false
            }
        }
    }
}
