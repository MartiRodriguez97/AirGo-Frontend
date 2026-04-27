package com.airgo.airgoApp.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val requestPath = response.request.url.encodedPath
        if (requestPath.contains("token/refresh")) {
            return null
        }

        val refreshToken = SessionManager.refreshToken
        if (refreshToken != null) {
            val tokenRefreshed = runBlocking {
                refreshAccessToken(refreshToken)
            }

            if (tokenRefreshed) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${SessionManager.accessToken}")
                    .build()
            }
        }

        SessionManager.logout()
        return null
    }

    private suspend fun refreshAccessToken(refreshToken: String): Boolean {
        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl(com.airgo.airgoApp.BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val authApi = retrofit.create(com.airgo.airgoApp.features.login.data.auth.AuthApi::class.java)
            val response = authApi.refreshToken(com.airgo.airgoApp.features.login.data.auth.RefreshTokenRequest(refreshToken))

            if (response.isSuccessful && response.body() != null) {
                val newTokens = response.body()!!
                SessionManager.accessToken = newTokens.access ?: newTokens.accessToken
                if (newTokens.refresh != null || newTokens.refreshToken != null) {
                    SessionManager.refreshToken = newTokens.refresh ?: newTokens.refreshToken
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

