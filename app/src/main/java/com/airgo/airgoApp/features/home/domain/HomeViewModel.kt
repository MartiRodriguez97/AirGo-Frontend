package com.airgo.airgoApp.features.home.domain

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airgo.airgoApp.core.network.RetrofitProvider.profileApi
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanRepository
import com.airgo.airgoApp.features.home.data.TrainingPlanRequest
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.map.helpers.NominatimResult
import com.airgo.airgoApp.features.map.helpers.NominatimService
import com.airgo.airgoApp.features.profile.data.ComprovarEdificiRequest
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

    val needsBuildingAddress: Boolean
        get() = SessionManager.userBadges?.none { it.insignia.tipus == "EDIFICI" } ?: true

    init {
        loadPlans()
        loadUserBadges()
    }

    private fun loadUserBadges() {
        if (SessionManager.userBadges != null) return
        viewModelScope.launch(exceptionHandler) {
            val badges =
                com.airgo.airgoApp.core.network.RetrofitProvider.profileApi
                    .getUserBadges()
            SessionManager.userBadges = badges
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

                        if (body.punts > 7) {
                            val badgeResponse = profileApi.guanyarEdificiInsignia()
                            if (badgeResponse.isSuccessful) {
                                val badgeBody = badgeResponse.body()
                                if (badgeBody != null) {
                                    val currentBadges = SessionManager.userBadges?.toMutableList() ?: mutableListOf()
                                    currentBadges.add(badgeBody)
                                    SessionManager.userBadges = currentBadges
                                    _buildingBadgeEarned.value = badgeBody.insignia
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

        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                /*
                 * val rawPlans = repository.getTrainingPlans()
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
                 * */
                val fetchedPlans = repository.getTrainingPlans()

                SessionManager.trainingPlans = fetchedPlans
                _plans.value = fetchedPlans
                updateTodayExercises()
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
        onSuccess: () -> Unit,
        daysDuration: Int,
        workoutsPerWeek: Int,
        sport: String?,
        level: Int?,
        daysOfWeek: List<Int>,
        endDate: String,
    ) {
        viewModelScope.launch(exceptionHandler) {
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
}
