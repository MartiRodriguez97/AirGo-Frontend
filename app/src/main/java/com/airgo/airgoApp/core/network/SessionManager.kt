package com.airgo.airgoApp.core.network

object SessionManager {
    // temporal
    var accessToken: String? = null
    var profilePictureUrl: String? = null
    var displayName: String? = null
    var userId: Int? = null
    var email: String? = null
    var titol: String? = null
    var availableTitols: List<com.airgo.airgoApp.features.settings.account.titols.data.UserTitol>? = null

    fun clear() {
        accessToken = null
        profilePictureUrl = null
        displayName = null
        userId = null
        email = null
        titol = null
        availableTitols = null
    }
}
