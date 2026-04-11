package com.airgo.airgoApp.core.network

object ApiEndpoints {
    const val GOOGLE_LOGIN = "auth/google/"
    const val DELETE_ACCOUNT = "api/usuaris/me/"

    const val CHANGE_PROFILE_PICTURE = "api/usuaris/me/profile-pic/"

    fun getChangeUsernameEndpoint(id: Int): String = "api/usuaris/$id/"

    fun getChangeTitolEndpoint(id: Int): String = "api/usuaris/$id/"

    fun getAvailableTitolsEndpoint(id: Int): String = "api/usuaris/$id/titols/"

    const val USERS_LIST = "api/usuaris/"
}
