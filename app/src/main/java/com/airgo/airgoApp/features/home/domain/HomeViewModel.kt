package com.airgo.airgoApp.features.home.domain

import android.net.http.HttpException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider.profileApi
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.NomPla
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.home.data.TrainingPlanRequest
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
import com.airgo.airgoApp.features.profile.data.ComprovarEdificiRequest
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

private const val SEARCH_DEBOUNCE_MS = 400L
private const val ERROR_HTTP_404 = 404
private const val SIZE_SEARCH = 3
private const val MIN_VALOR_PUNTS = 7
private const val DATA_FORMAT_LENGTH = 10

data class CreatePlanParams(
    val name: String,
    val daysDuration: Int,
    val workoutsPerWeek: Int,
    val sport: String?,
    val level: Int?,
    val daysOfWeek: List<Int>,
    val endDate: String,
)

class HomeViewModel : ViewModel() {
    private val repository = TrainingPlanRepository()

    private val exceptionHandler =
        kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
            Log.e("HomeViewModel", "Unexpected error", exception)
            _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
        }

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

    private val _buildingBadgeEarned =
        MutableStateFlow<
            com.airgo.airgoApp.features.profile.data.BadgeDefinition?,
        >(null)
    val buildingBadgeEarned: StateFlow<
        com.airgo.airgoApp.features.profile.data.BadgeDefinition?,
    > = _buildingBadgeEarned.asStateFlow()

    private val _addressResults = MutableStateFlow<List<NominatimResult>>(emptyList())
    val addressResults: StateFlow<List<NominatimResult>> = _addressResults.asStateFlow()

    private val _isSearchingAddress = MutableStateFlow(false)
    val isSearchingAddress: StateFlow<Boolean> = _isSearchingAddress.asStateFlow()

    private var searchJob: Job? = null

    var showBuildingAddressDialog by mutableStateOf(false)

    private val _needsBuildingAddress =
        MutableStateFlow(
            SessionManager.userBadges?.none { it.insignia.tipus == "EDIFICI" } ?: true,
        )
    val needsBuildingAddress: StateFlow<Boolean> = _needsBuildingAddress.asStateFlow()

    init {
        loadPlans()
        loadUserBadges()
    }

    private fun loadUserBadges() {
        if (SessionManager.userBadges != null) {
            _needsBuildingAddress.value = SessionManager.userBadges?.none { it.insignia.tipus == "EDIFICI" } ?: true
            return
        }
        viewModelScope.launch(exceptionHandler) {
            val badges =
                profileApi
                    .getUserBadges()
            SessionManager.userBadges = badges
            _needsBuildingAddress.value = badges.none { it.insignia.tipus == "EDIFICI" }
        }
    }

    fun dismissBuildingBadge() {
        _buildingBadgeEarned.value = null
    }

    fun searchAddresses(query: String) {
        searchJob?.cancel()
        if (query.length < SIZE_SEARCH) {
            _addressResults.value = emptyList()
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                _isSearchingAddress.value = true
                try {
                    _addressResults.value = NominatimService.searchAddress(query)
                } finally {
                    _isSearchingAddress.value = false
                }
            }
    }

    fun checkBuildingAddress(
        municipi: String,
        adreca: String,
        numero: String,
    ) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                val response =
                    profileApi.comprovarEdifici(
                        ComprovarEdificiRequest(municipi, adreca, numero),
                    )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        if (body.punts >= MIN_VALOR_PUNTS) {
                            // Update points
                            SessionManager.userId?.let { userId ->
                                val currentPunts = SessionManager.punts ?: 0
                                val newPunts = currentPunts + body.punts
                                profileApi.updateUser(
                                    userId,
                                    com.airgo.airgoApp.features.profile.data
                                        .UpdateUserRequest(punts = body.punts),
                                )
                                SessionManager.punts = newPunts
                            }

                            val badgeResponse =
                                profileApi.guanyarEdificiInsignia(
                                    com.airgo.airgoApp.features.profile.data.GuanyarEdificiInsigniaRequest(
                                        puntuacio = body.punts,
                                    ),
                                )
                            if (badgeResponse.isSuccessful) {
                                val badgeBody = badgeResponse.body()
                                val parsedBadgeResponse = badgeBody?.toUserBadgeResponse()
                                val actualBadge = badgeBody?.extractBadge()

                                if (parsedBadgeResponse != null && actualBadge != null) {
                                    val currentBadges = SessionManager.userBadges?.toMutableList() ?: mutableListOf()
                                    currentBadges.add(parsedBadgeResponse)
                                    SessionManager.userBadges = currentBadges
                                    _buildingBadgeEarned.value = actualBadge
                                    _needsBuildingAddress.value = false
                                }
                                showBuildingAddressDialog = false
                            } else {
                                _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
                            }
                        } else {
                            _errorMessage.value = com.airgo.airgoApp.R.string.error_building_score_low
                        }
                    }
                } else if (response.code() == ERROR_HTTP_404) {
                    _errorMessage.value = com.airgo.airgoApp.R.string.error_address_not_found
                } else {
                    _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
                }
            } catch (e: java.io.IOException) {
                Log.e("HomeViewModel", "Error checking building address", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
            } catch (e: retrofit2.HttpException) {
                Log.e("HomeViewModel", "Error checking building address", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTodayExercises(allExercises: List<Exercise>) {
        val today = java.time.LocalDate.now()
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
            if (!forceRefresh && cachedPlans != null) {
                _plans.value = cachedPlans
                val cachedExercises = cachedPlans.flatMap { it.exercicis ?: emptyList() }
                updateTodayExercises(cachedExercises)
                _isLoading.value = false
                return@launch
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
            } catch (e: java.io.IOException) {
                Log.e("HomeViewModel", "Error cargando planes de entrenamiento (silenciado)", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPlan(
        params: CreatePlanParams,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                val request =
                    TrainingPlanRequest(
                        nom = params.name,
                        diesDurada = params.daysDuration,
                        numEntrenamentsSetmanals = params.workoutsPerWeek,
                        esport = params.sport ?: "CAM",
                        nivell = params.level ?: 1,
                        diesSetmana = params.daysOfWeek,
                        dataFi = params.endDate,
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
        viewModelScope.launch(exceptionHandler) {
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

    fun updatePlanName(
        planId: Int,
        newName: String,
    ) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                repository.updateTrainingPlanName(planId, newName)

                val currentPlans = _plans.value.toMutableList()
                val index = currentPlans.indexOfFirst { it.id == planId }

                if (index != -1) {
                    val oldPlan = currentPlans[index]

                    val updatedExercises =
                        oldPlan.exercicis.map { ex ->
                            ex.copy(nomPla = NomPla(newName))
                        }

                    currentPlans[index] =
                        oldPlan.copy(
                            nom = newName,
                            exercicis = updatedExercises,
                        )

                    _plans.value = currentPlans
                    SessionManager.trainingPlans = currentPlans

                    val totsExercicis = currentPlans.flatMap { it.exercicis ?: emptyList() }
                    updateTodayExercises(totsExercicis)
                }
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Error de xarxa actualitzant el nom del pla", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_generic
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchAvailableTitles() {
        viewModelScope.launch(exceptionHandler) {
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
        viewModelScope.launch(exceptionHandler) {
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

    fun updateExerciseDate(
        exerciseId: Int,
        newDateIso: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                repository.updateExerciseDate(exerciseId, newDateIso)

                val currentPlans =
                    _plans.value.map { plan ->
                        val updatedExercises =
                            plan.exercicis.map { ex ->
                                if (ex.id == exerciseId) {
                                    ex.copy(dataInici = newDateIso)
                                } else {
                                    ex
                                }
                            }
                        plan.copy(exercicis = updatedExercises)
                    }

                _plans.value = currentPlans
                SessionManager.trainingPlans = currentPlans

                val totsExercicis = currentPlans.flatMap { it.exercicis ?: emptyList() }
                updateTodayExercises(totsExercicis)

                onSuccess()
            } catch (e: IOException) {
                Log.e("HomeViewModel", "Error de red al mover la actividad", e)
                _errorMessage.value = com.airgo.airgoApp.R.string.error_move_activity
            } finally {
                _isLoading.value = false
            }
        }
    }
}
