package com.airgo.airgoApp.core.network

object SessionManager {
    // temporal
    var accessToken: String? = null
    var profilePictureUrl: String? = null
    var username: String? = null
    var userId: Int? = null
    var email: String? = null
    var titol: String? = null
    var availableTitols: List<com.airgo.airgoApp.features.settings.account.titols.data.UserTitol>? = null
    var trainingPlans: List<com.airgo.airgoApp.features.home.data.TrainingPlanResponse>? = null

    fun clear() {
        accessToken = null
        profilePictureUrl = null
        username = null
        userId = null
        email = null
        titol = null
        availableTitols = null
        trainingPlans = null
        punts = null
        teBici = null
        pes = null
        altura = null
        ratxa = null
        idioma = null
        limitRutes = null
        insignies = null
    }

    var punts: Int? = null
    var teBici: Boolean? = null
    var pes: Double? = null
    var altura: Double? = null
    var ratxa: Int? = null
    var idioma: String? = null
    var limitRutes: Int? = null
    var insignies: String? = null
}
