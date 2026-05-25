package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.home.data.TrainingPlanRequest
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

private const val DATA_FORMAT_LENGTH = 10

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

    val errorMessage: StateFlow<Int?> = _errorMessage.asStateFlow()

    init {
        loadPlans()
    }

    fun updateTodayExercises(allExercises: List<Exercise>) {
        val today = java.time.LocalDate.now()
        Log.d("AirGo_Debug", "Buscando ejercicios pendientes para hoy: $today")

        val todayExercises =
            allExercises.filter { ex ->
                if (ex.dataInici != null && ex.dataInici.length >= DATA_FORMAT_LENGTH) {
                    try {
                        val dateStr = ex.dataInici.substring(0, DATA_FORMAT_LENGTH)
                        val isToday = java.time.LocalDate.parse(dateStr) == today

                        // Mostrar si es hoy Y NO está completado
                        isToday && !ex.completat
                    } catch (e: java.time.format.DateTimeParseException) {
                        false
                    }
                } else {
                    false
                }
            }

        Log.d("AirGo_Debug", "Ejercicios finales para hoy: ${todayExercises.size}")

        // Actualizamos el estado del Home
        _exercises.value = todayExercises

        // OJO: No guardes todayExercises en SessionManager.exercicis si el Calendario
        // espera que ahí estén TODOS los planificados. Mejor guardarlo en otra variable
        // como SessionManager.exercicisAvui si lo necesitas, o no guardarlo.
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

    fun loadPlans(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true

            val cachedPlans = SessionManager.trainingPlans
            if (!forceRefresh && !cachedPlans.isNullOrEmpty()) {
                // Usamos los datos locales
                _plans.value = cachedPlans
                val cachedExercises = cachedPlans.flatMap { it.exercicis ?: emptyList() }
                updateTodayExercises(cachedExercises)
                return@launch // Terminamos la ejecución aquí, ¡adiós a la llamada de red extra!
            }
            _isLoading.value = true
            try {
                val fetchedPlans = repository.getTrainingPlans()
                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
                val fetchedExercises = fetchedPlans.flatMap { it.exercicis ?: emptyList() }
                updateTodayExercises(fetchedExercises)
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("HomeViewModel", "HTTP Error $code: $errorBody", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_load_plans
            } catch (e: java.lang.Exception) {
                Log.e("HomeViewModel", "Error cargando planes de entrenamiento (silenciado)", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlan(
        onSuccess: () -> Unit,
        daysDuration: Int,
        workoutsPerWeek: Int,
        sport: String?,
        level: Int?,
        daysOfWeek: List<Int>,
        endDate: String,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request =
                    TrainingPlanRequest(
                        diesDurada = daysDuration,
                        numEntrenamentsSetmanals = workoutsPerWeek,
                        esport = sport ?: "CAM",
                        nivell = level ?: 1,
                        diesSetmana = daysOfWeek,
                        dataFi = endDate,
                    )

                repository.createTrainingPlan(request)
                SessionManager.trainingPlans = null

                val fetchedPlans = repository.getTrainingPlans()
                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans

                val totsExercicis = fetchedPlans.flatMap { it.exercicis ?: emptyList() }
                updateTodayExercises(totsExercicis)

                onSuccess()
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Error de red creando el plan", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "Error HTTP creando el plan (codi: ${e.code()})", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } catch (e: JsonSyntaxException) {
                // <-- Reemplazo de Exception por la específica del parser
                Log.e("HomeViewModel", "Error de parseo JSON: ${e.message}", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.home_error_create_plan
            } catch (e: IllegalArgumentException) {
                // Opcional: si hay errores de parámetros inválidos
                Log.e("HomeViewModel", "Error en los datos pasados: ${e.message}", e)
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
                val response =
                    com.airgo.airgoApp.core.network.RetrofitProvider.trackingApi
                        .getAvailableTitles()
                _availableTitles.value = response.opcions
                showTitleSelectionDialog = true
            } catch (e: IOException) {
                Log.e("HomeViewModel", "IO Error fetching titles", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_fetching_titles
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "HTTP Error fetching titles", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_fetching_titles
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
                    com.airgo.airgoApp.features.tracking.data
                        .UnlockTitleRequest(titleId),
                )

                if (setAsMain) {
                    SessionManager.userId?.let { userId ->
                        val selectedTitle = _availableTitles.value.find { it.id == titleId }
                        selectedTitle?.let {
                            com.airgo.airgoApp.core.network.RetrofitProvider.profileApi.updateUser(
                                userId,
                                com.airgo.airgoApp.features.profile.data
                                    .UpdateUserRequest(titol = it.nom),
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
                _errorMessage.value = com.airgo.airgoApp.R.string.error_unlocking_title
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "HTTP Error unlocking title", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_unlocking_title
            }
        }
    }
}
