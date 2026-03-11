package com.airgo.airgoApp.ui.login.data.auth

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {
    private val retrofit: Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://tu-backend.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}
