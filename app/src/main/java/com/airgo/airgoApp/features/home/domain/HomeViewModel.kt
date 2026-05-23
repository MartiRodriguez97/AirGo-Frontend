package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanRawResponse
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

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<Int?>(null)

    companion object {
        private const val TEMPLATE_ID_1 = 1
        private const val TEMPLATE_ID_2 = 2
        private const val TEMPLATE_ID_3 = 3
        private const val TEMPLATE_ID_4 = 4
        private const val TEMPLATE_ID_5 = 5
        private const val TEMPLATE_ID_6 = 6
    }

    val errorMessage: StateFlow<Int?> = _errorMessage.asStateFlow()

    init {
        loadPlans()
    }

    fun updateTodayExercises() {
        val today =
            java.time.LocalDate
                .now()
                .toString() // "yyyy-MM-dd"

        val todayExercises =
            _plans.value.flatMap { plan ->
                plan.templates.flatMap { template ->
                    template.exercicis.filter { exercise ->
                        exercise.dataInici?.startsWith(today) == true && !exercise.completat
                    }
                }
            }
        SessionManager.exercicis = todayExercises
        _exercises.value = todayExercises
    }

    fun startExercise(exercise: Exercise) {
        val template =
            _plans.value
                .flatMap { it.templates }
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
            updateTodayExercises()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rawPlans = repository.getTrainingPlans()

                val fetchedPlans =
                    rawPlans.map { raw ->
                        val fullTemplates =
                            raw.templates.map { templateId ->
                                repository.getTemplate(templateId)
                            }
                        TrainingPlanResponse(
                            id = raw.id,
                            diesDurada = raw.diesDurada,
                            numEntrenamentsSetmanals = raw.numEntrenamentsSetmanals,
                            templates = fullTemplates,
                        )
                    }

                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
                updateTodayExercises()
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("HomeViewModel", "HTTP Error $code: $errorBody", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Network error (IOException): ${e.message}", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
                // Fallback to SessionManager if available
                SessionManager.trainingPlans?.let {
                    _plans.value = it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlan(
        daysDuration: Int,
        workoutsPerWeek: Int,
        level: String?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val templateIds =
                    when (level) {
                        "Bàsic", "Básico", "Basic", "Principiant", "Principiante", "Beginner" ->
                            listOf(
                                TEMPLATE_ID_1,
                                TEMPLATE_ID_2,
                                TEMPLATE_ID_3,
                                TEMPLATE_ID_4,
                            )
                        "Mitjà", "Medio", "Medium", "Intermedi", "Intermedio", "Intermediate" ->
                            listOf(
                                TEMPLATE_ID_2,
                                TEMPLATE_ID_3,
                                TEMPLATE_ID_4,
                                TEMPLATE_ID_5,
                            )
                        "Avançat", "Avanzado", "Advanced" ->
                            listOf(TEMPLATE_ID_3, TEMPLATE_ID_4, TEMPLATE_ID_5, TEMPLATE_ID_6)
                        else -> emptyList()
                    }
                val request =
                    TrainingPlanRequest(
                        diesDurada = daysDuration,
                        numEntrenamentsSetmanals = workoutsPerWeek,
                        templates = templateIds,
                    )
                val rawPlans = repository.getTrainingPlans()
                val fetchedPlans =
                    rawPlans.map { raw ->
                        val fullTemplates =
                            raw.templates.map { templateId ->
                                repository.getTemplate(templateId)
                            }
                        TrainingPlanResponse(
                            id = raw.id,
                            diesDurada = raw.diesDurada,
                            numEntrenamentsSetmanals = raw.numEntrenamentsSetmanals,
                            templates = fullTemplates,
                        )
                    }
                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
                updateTodayExercises()
                onSuccess()
            } catch (e: java.io.IOException) {
                Log.e("HomeViewModel", "Error de xarxa creant o inicialitzant el pla", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "Error HTTP creant o inicialitzant el pla (codi: ${e.code()})", e)
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
            } catch (e: IOException) {
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
