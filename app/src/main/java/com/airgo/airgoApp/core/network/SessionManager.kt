package com.airgo.airgoApp.core.network

import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.profile.data.UserBadgeResponse
import com.airgo.airgoApp.features.settings.account.titols.data.UserTitol
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    // temporal
    var accessToken: String? = null
    var refreshToken: String? = null
    var profilePictureUrl: String? = null
    var username: String? = null
    var userId: Int? = null
    var email: String? = null
    var titol: String? = null
    var availableTitols: List<UserTitol>? = null
    var trainingPlans: List<TrainingPlanResponse>? = null
    var exercicis: List<Exercise>? = null
    var exerciciActiu: Exercise? = null
    var nomTemplateActiu: String? = null
    var userBadges: List<UserBadgeResponse>? = null

    // ID del chat actualmente abierto para evitar duplicar notificaciones
    var currentChatId: String? = null

    private val _titolsPendents = MutableStateFlow(0)
    val titolsPendentsFlow = _titolsPendents.asStateFlow()
    var titolsPendents: Int
        get() = _titolsPendents.value
        set(value) {
            _titolsPendents.value = value
        }

    fun clear() {
        accessToken = null
        refreshToken = null
        profilePictureUrl = null
        username = null
        userId = null
        email = null
        titol = null
        availableTitols = null
        trainingPlans = null
        exercicis = null
        exerciciActiu = null
        nomTemplateActiu = null
        punts = null
        teBici = null
        pes = null
        altura = null
        ratxa = null
        idioma = null
        limitRutes = null
        insignies = null
        userBadges = null
        titolsPendents = 0
        trainingPlans = null
        exercicis = null
        exerciciActiu = null
        nomTemplateActiu = null
    }

    var punts: Int? = null
    var teBici: Boolean? = null
    var pes: Double? = null
    var altura: Double? = null
    var ratxa: Int? = null
    var idioma: String? = null
    var limitRutes: Int? = null
    var insignies: String? = null

    fun logout() {
        clear()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    fun toUserPreferences(): UserPreferences =
        UserPreferences(
            token = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            profilePictureUrl = profilePictureUrl,
        )

    fun isProfileIncomplete(): Boolean =
        pes == null ||
            pes!! <= 0.0 ||
            altura == null ||
            altura!! <= 0.0 ||
            username.isNullOrBlank()
}
