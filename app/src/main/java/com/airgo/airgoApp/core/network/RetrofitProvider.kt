package com.airgo.airgoApp.core.network

import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.features.login.data.auth.AuthApi
import com.airgo.airgoApp.features.map.AirQualityApiService
import com.airgo.airgoApp.features.profile.data.ProfileApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private const val BACKEND_URL = BuildConfig.BACKEND_URL
    private val authInterceptor =
        Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            SessionManager.accessToken?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

    private val httpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .build()
    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BACKEND_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val profileApi: ProfileApi = retrofit.create(ProfileApi::class.java)
    val airQualityApi: AirQualityApiService = retrofit.create(AirQualityApiService::class.java)
}
