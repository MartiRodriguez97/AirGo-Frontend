package com.airgo.airgoApp.features.login.data.auth

import com.airgo.airgoApp.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private const val BACKEND_URL = BuildConfig.BACKEND_URL
    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}
