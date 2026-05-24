package com.airgo.airgoApp.core.network

object ApiEndpoints {
    // Endpoints login
    const val GOOGLE_LOGIN = "auth/google/"
    const val TOKEN_REFRESH = "auth/refresh/"

    // Endpoints d'usuaris
    const val DELETE_ACCOUNT = "api/usuaris/me/"
    const val CHANGE_PROFILE_PICTURE = "api/usuaris/me/profile-pic/"
    const val SEARCH_USERS = "api/usuaris/"
    const val USER_ACCOUNT_INFO = "api/usuaris/me/profile/"
    const val USER_PROFILE = "api/usuaris/{id}/"

    const val UPDATE_FCM_TOKEN = "api/usuaris/me/fcm-token/"

    fun getChangeInfoEndpoint(id: Int): String = "api/usuaris/$id/"

    fun getAvailableTitolsEndpoint(id: Int): String = "api/usuaris/$id/titols/"

    // Endpoints d'amics
    const val FRIENDS = "api/usuaris/me/friends/"
    const val FRIEND_REQUEST = "api/usuaris/me/friends/request/"
    const val FRIEND_REQUESTS = "api/usuaris/me/friends/requests/"
    const val DELETE_FRIEND = "api/usuaris/me/friends/{amic_id}/"

    fun getFriendRespondEndpoint(amistatId: Int): String = "api/usuaris/me/friends/$amistatId/respond/"

    // Endpoints de conversació
    const val CONVERSATIONS = "api/conversations/"
    const val CONVERSATION_MESSAGES = "api/conversations/{id}/messages/"
    const val CONVERSATION_READ = "api/conversations/{id}/read/"
    const val DELETE_MESSAGE = "api/conversations/messages/{missatge_id}/"

    // Endpoints de fòrums
    const val FORUMS = "api/forums/"
    const val FORUM_DETAIL = "api/forums/{id}/"
    const val FAVORIT_FORUMS = "api/usuaris/me/forums/"
    const val DELETE_FAVORIT_FORUM = "api/usuaris/me/forums/{forum_id}/"

    // Endpoints dels serveis externs
    const val AIR_QUALITY = "air-quality/"
    const val BICING = "bicing/"

    // Endpoints de rutes
    const val ROUTE_GENERATION = "route-generation/"
    const val SAVE_FAVORITE_ROUTE = "api/usuaris/me/routes/save/"
    const val FAVORITE_ROUTES = "api/usuaris/me/routes/"
    const val DELETE_FAVORITE_ROUTE = "api/usuaris/me/routes/{route_id}/"

    // Endpoints d'exercicis
    const val SAVE_ACTIVITY = "api/exercicis/"
    const val ACTIVITY = "api/exercicis/{id}/"
    const val TEMPLATE_EXERCICI = "/api/template-exercici/{id}/"

    // Endpoints de pla d'entrenament
    const val PLA_ENTRENAMENT = "/api/pla-entrenament/"
    const val PLA_ENTRENAMENT_ID = "/api/pla-entrenament/{id}/"
    const val PLA_ENTRENAMENT_INI = "/api/pla-entrenament/{id}/inicialitzar-pla-ini/"

    // Endpoints titols
    const val TITLES_AVAILABLE = "api/usuaris/me/titols-disponibles/"
    const val UNLOCK_TITLE = "api/usuaris/me/desbloquejar-titol/"

    // Endpoints insignies
    const val INSIGNIES = "api/usuaris/me/insignies/"
}
