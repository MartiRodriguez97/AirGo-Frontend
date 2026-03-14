package com.airgo.airgoApp.ui.login.data.auth

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthResponse(
    val user: String?,
    val picture: String?,
    val access: String?,
    val refresh: String?,
)

data class GoogleTokenRequest(
    val token: String,
)

interface AuthApi {
    @POST(ApiEndpoints.GOOGLE_LOGIN)
    suspend fun loginWithGoogle(
        @Body body: GoogleTokenRequest,
    ): Response<AuthResponse>
}
