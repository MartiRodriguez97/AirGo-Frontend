package com.airgo.airgoApp.core.network

import android.util.Log
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.features.login.data.auth.AuthApi
import com.airgo.airgoApp.features.login.data.auth.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class TokenAuthenticator : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.request.url.encodedPath
                .contains("token/refresh")
        ) {
            return null
        }

        val refreshToken = SessionManager.refreshToken
        val tokenRefreshed =
            refreshToken?.let { token ->
                runBlocking { refreshAccessToken(token) }
            } ?: false

        return if (tokenRefreshed) {
            response.request
                .newBuilder()
                .header("Authorization", "Bearer ${SessionManager.accessToken}")
                .build()
        } else {
            SessionManager.logout()
            null
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): Boolean =
        try {
            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(BuildConfig.BACKEND_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

            val authApi = retrofit.create(AuthApi::class.java)
            val request = RefreshTokenRequest(refreshToken)
            val authResponse = authApi.refreshToken(request)
            val body = authResponse.body()

            if (authResponse.isSuccessful && body != null) {
                SessionManager.accessToken = body.access
                if (body.refresh != null) {
                    SessionManager.refreshToken = body.refresh
                }
                true
            } else {
                false
            }
        } catch (e: IOException) {
            Log.e("TokenAuthenticator", "Network error refreshing token", e)
            false
        } catch (e: HttpException) {
            Log.e("TokenAuthenticator", "HTTP error refreshing token", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e("TokenAuthenticator", "Illegal argument error refreshing token", e)
            false
        }
}
