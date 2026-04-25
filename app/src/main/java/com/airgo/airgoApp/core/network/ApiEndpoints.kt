package com.airgo.airgoApp.core.network

object ApiEndpoints {
    const val GOOGLE_LOGIN = "auth/google/"
    const val DELETE_ACCOUNT = "api/usuaris/me/"
    const val CHANGE_PROFILE_PICTURE = "api/usuaris/me/profile-pic/"
    const val AIR_QUALITY = "air-quality/"

    const val ROUTE_GENERATION = "route-generation/"
    const val BICING = "bicing/"

    const val SEARCH_USERS = "api/usuaris/"

    fun getChangeInfoEndpoint(id: Int): String = "api/usuaris/$id/"

    fun getAvailableTitolsEndpoint(id: Int): String = "api/usuaris/$id/titols/"

    const val USER_ACCOUNT_INFO = "api/usuaris/me/profile/"

    const val PLA_ENTRENAMENT = "/api/pla-entrenament/"
}
