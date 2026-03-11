package com.airgo.airgoApp.ui.login.data.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthResponse(
    val email: String?,
    val displayName: String?,
    val accessToken: String?,
)

interface AuthApi {
    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body body: String,
    ): Response<AuthResponse>
}
