package com.airgo.airgoApp.features.settings.account.username.data

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH

data class ChangeUsernameRequest(
    val username: String,
)

interface UsernameApi {
    @PATCH(ApiEndpoints.CHANGE_USERNAME)
    suspend fun changeUsername(
        @Body request: ChangeUsernameRequest,
    ): Response<Void>
}
