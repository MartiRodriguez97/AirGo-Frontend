package com.airgo.airgoApp.features.settings.account.username.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH

data class ChangeUsernameRequest(
    val username: String,
)

// El endpoint para cambiar el nombre de usuario se pasará dinámicamente desde el ViewModel
interface UsernameApi {
    @PATCH
    suspend fun changeUsername(
        @retrofit2.http.Url url: String,
        @Body request: ChangeUsernameRequest,
    ): Response<Void>
}
