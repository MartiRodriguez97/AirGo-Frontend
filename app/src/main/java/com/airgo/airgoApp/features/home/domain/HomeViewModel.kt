package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    val titolsPendents: StateFlow<Int> = SessionManager.titolsPendentsFlow

    private val _availableTitles =
        MutableStateFlow<List<com.airgo.airgoApp.features.tracking.data.TitleOption>>(emptyList())
    val availableTitles: StateFlow<List<com.airgo.airgoApp.features.tracking.data.TitleOption>> =
        _availableTitles.asStateFlow()

    var showTitleSelectionDialog by mutableStateOf(false)

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
        Log.d("HomeViewModel", "Updating today's exercises for date: $today")

        val todayExercises =
            _plans.value.flatMap { plan ->
                plan.templates.flatMap { template ->
                    template.exercicis.filter { exercise ->
                        exercise.dataInici?.startsWith(today) == true && !exercise.completat
                    }
                }
            }
        Log.d("HomeViewModel", "Found ${todayExercises.size} pending exercises for today")
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
                val url = com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT
                Log.d("HomeViewModel", "Requesting GET training plans from URL: $url")

                val rawPlans = repository.getTrainingPlans()
                Log.d("HomeViewModel", "Successfully fetched ${rawPlans.size} raw plans")

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
                    Log.d("HomeViewModel", "Using cached plans from SessionManager as fallback")
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
                // 1. Determine template IDs based on level
                val templateIds =
                    when (level) {
                        "Bàsic", "Básico", "Basic", "Principiant", "Principiante", "Beginner" ->
                            listOf(TEMPLATE_ID_1, TEMPLATE_ID_2, TEMPLATE_ID_3, TEMPLATE_ID_4)
                        "Mitjà", "Medio", "Medium", "Intermedi", "Intermedio", "Intermediate" ->
                            listOf(TEMPLATE_ID_2, TEMPLATE_ID_3, TEMPLATE_ID_4, TEMPLATE_ID_5)
                        "Avançat", "Avanzado", "Advanced" ->
                            listOf(TEMPLATE_ID_3, TEMPLATE_ID_4, TEMPLATE_ID_5, TEMPLATE_ID_6)
                        else -> emptyList()
                    }
                Log.d("HomeViewModel", "Selected level: $level, identified template IDs: $templateIds")

                val request =
                    TrainingPlanRequest(
                        diesDurada = daysDuration,
                        numEntrenamentsSetmanals = workoutsPerWeek,
                        templates = templateIds,
                    )
                Log.d("HomeViewModel", "Final TrainingPlanRequest: $request")

                // 3. Create plan (Server returns TrainingPlanRawResponse with IDs)
                val planRaw: TrainingPlanRawResponse = repository.createTrainingPlan(request)
                Log.d("HomeViewModel", "Create plan response: $planRaw")

                // 4. Initialize plan (Server also returns TrainingPlanRawResponse)
                val initializedRaw: TrainingPlanRawResponse = repository.initializeTrainingPlan(planRaw.id, request)
                Log.d("HomeViewModel", "Initialized plan response: $initializedRaw")

                // 5. Fetch all plans from server and reconstruct full objects
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

    fun fetchAvailableTitles() {
        viewModelScope.launch {
            try {
                val response = com.airgo.airgoApp.core.network.RetrofitProvider.trackingApi.getAvailableTitles()
                _availableTitles.value = response.opcions
                showTitleSelectionDialog = true
            } catch (e: IOException) {
                Log.e("HomeViewModel", "IO Error fetching titles", e)
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "HTTP Error fetching titles", e)
            }
        }
    }

    fun unlockTitle(
        titleId: Int,
        setAsMain: Boolean,
    ) {
        viewModelScope.launch {
            try {
                com.airgo.airgoApp.core.network.RetrofitProvider.trackingApi.unlockTitle(
                    com.airgo.airgoApp.features.tracking.data.UnlockTitleRequest(titleId),
                )

                if (setAsMain) {
                    SessionManager.userId?.let { userId ->
                        val selectedTitle = _availableTitles.value.find { it.id == titleId }
                        selectedTitle?.let {
                            com.airgo.airgoApp.core.network.RetrofitProvider.profileApi.updateUser(
                                userId,
                                com.airgo.airgoApp.features.profile.data.UpdateUserRequest(titol = it.nom),
                            )
                            SessionManager.titol = it.nom
                        }
                    }
                }

                // Actualizar la lista de títulos disponibles en el SessionManager
                _availableTitles.value.find { it.id == titleId }?.let { title ->
                    val currentList = SessionManager.availableTitols?.toMutableList() ?: mutableListOf()
                    if (currentList.none { it.id == title.id }) {
                        currentList.add(
                            com.airgo.airgoApp.features.settings.account.titols.data.UserTitol(
                                id = title.id,
                                nom = title.nom,
                                descripcio = title.descripcio,
                            ),
                        )
                        SessionManager.availableTitols = currentList
                    }
                }

                showTitleSelectionDialog = false
                val newValue = (SessionManager.titolsPendents - 1).coerceAtLeast(0)
                SessionManager.titolsPendents = newValue
            } catch (e: IOException) {
                Log.e("HomeViewModel", "IO Error unlocking title", e)
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "HTTP Error unlocking title", e)
            }
        }
    }

    fun refreshTitolsPendents() {
        // No-op as we use the flow from SessionManager
    }
}
