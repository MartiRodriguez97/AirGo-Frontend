package com.airgo.airgoApp.features.settings.account.titols.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH

data class ChangeTitolRequest(
    val titol: String,
)

interface TitolApi {
    @PATCH
    suspend fun changeTitol(
        @retrofit2.http.Url url: String,
        @Body request: ChangeTitolRequest,
    ): Response<Void>
}
