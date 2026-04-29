package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.home.data.TrainingPlanRequest
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun loadPlans() {
        if (SessionManager.trainingPlans != null && SessionManager.trainingPlans!!.isNotEmpty()) {
            _plans.value = SessionManager.trainingPlans!!
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedPlans = repository.getTrainingPlans()
                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching plans", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlan(request: TrainingPlanRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newPlan = repository.createTrainingPlan(request)
                val currentPlans = _plans.value.toMutableList()
                currentPlans.add(newPlan)
                SessionManager.trainingPlans = currentPlans
                _plans.value = currentPlans
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error creating plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlan(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTrainingPlan(id)
                val currentPlans = _plans.value.toMutableList()
                currentPlans.removeAll { it.id == id }
                SessionManager.trainingPlans = currentPlans
                _plans.value = currentPlans
                onSuccess()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_delete_plan
            } finally {
                _isLoading.value = false
            }
        }
    }
}