package com.airgo.airgoApp.core.network

import android.util.Log
import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.core.network.ApiEndpoints.TOKEN_REFRESH
import com.airgo.airgoApp.features.login.data.auth.AuthApi
import com.airgo.airgoApp.features.login.data.auth.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class TokenAuthenticator : Authenticator {
    private val refreshAuthApi by lazy {
        val client =
            OkHttpClient
                .Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        var prior = response?.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val isRetryLimitExceeded = responseCount(response) >= MAX_RETRIES
        val isRefreshUrl =
            response.request.url.encodedPath
                .contains(TOKEN_REFRESH)

        if (isRetryLimitExceeded || isRefreshUrl) {
            return null
        }

        return synchronized(this) {
            val previousToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (previousToken != null && previousToken != SessionManager.accessToken) {
                // El token ya fue refrescado por otro hilo
                response.request
                    .newBuilder()
                    .header("Authorization", "Bearer ${SessionManager.accessToken}")
                    .build()
            } else {
                val refreshToken = SessionManager.refreshToken
                val tokenRefreshed =
                    refreshToken?.let { token ->
                        runBlocking { refreshAccessToken(token) }
                    } ?: false

                if (tokenRefreshed) {
                    response.request
                        .newBuilder()
                        .header("Authorization", "Bearer ${SessionManager.accessToken}")
                        .build()
                } else {
                    SessionManager.logout()
                    null
                }
            }
        }
    }

    private suspend fun refreshAccessToken(refreshToken: String): Boolean =
        try {
            val request = RefreshTokenRequest(refreshToken)
            val authResponse = refreshAuthApi.refreshToken(request)
            val body = authResponse.body()

            if (authResponse.isSuccessful && body != null && body.access != null) {
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

    companion object {
        private const val TIMEOUT_SECONDS = 30L
        private const val MAX_RETRIES = 3
    }
}
