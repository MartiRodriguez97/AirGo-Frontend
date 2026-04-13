package com.airgo.airgoApp.features.settings.account.tebici.data
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH

data class UpdateBikeRequest(
    val teBici: Boolean,
)

interface TeBiciApi {
    // El endpoint para actualizar el estado de teBici
    @PATCH
    suspend fun updateHasBike(
        @retrofit2.http.Url url: String,
        @Body request: UpdateBikeRequest,
    ): Response<Unit>
}
