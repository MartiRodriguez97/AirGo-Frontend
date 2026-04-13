package com.airgo.airgoApp.features.settings.account.titols.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

data class UserTitol(
    val id: Int,
    val nom: String,
    val descripcio: String,
)

data class AvailableTitolResponse(
    val titol: UserTitol,
)

data class ChangeTitolRequest(
    val titol: String,
)

// El endpoint para obtener los títulos disponibles y para cambiar el título se pasará dinámicamente desde el ViewModel
interface TitolApi {
    @GET
    suspend fun getAvailableTitols(
        @retrofit2.http.Url url: String,
    ): Response<List<AvailableTitolResponse>>

    @PATCH
    suspend fun changeTitol(
        @retrofit2.http.Url url: String,
        @Body request: ChangeTitolRequest,
    ): Response<Void>
}
