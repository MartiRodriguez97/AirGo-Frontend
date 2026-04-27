package com.airgo.airgoApp.core.network

import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.features.login.data.auth.AuthApi
import com.airgo.airgoApp.features.map.AirQualityApiService
import com.airgo.airgoApp.features.map.BicingApiService
import com.airgo.airgoApp.features.map.RouteGenerationApiService
import com.airgo.airgoApp.features.profile.data.ProfileApi
import com.airgo.airgoApp.features.settings.account.tebici.data.TeBiciApi
import com.airgo.airgoApp.features.settings.account.titols.data.TitolApi
import com.airgo.airgoApp.features.settings.account.username.data.UsernameApi
import com.airgo.airgoApp.features.social.SocialApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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

    const val MIN_TIME_WAITING_FOR_RESPONSE = 30L // segundos
    private val httpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .authenticator(TokenAuthenticator())
            .connectTimeout(MIN_TIME_WAITING_FOR_RESPONSE, TimeUnit.SECONDS)
            .readTimeout(MIN_TIME_WAITING_FOR_RESPONSE, TimeUnit.SECONDS)
            .writeTimeout(MIN_TIME_WAITING_FOR_RESPONSE, TimeUnit.SECONDS)
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
    val routeGenerationApi: RouteGenerationApiService = retrofit.create(RouteGenerationApiService::class.java)
    val bicingApi: BicingApiService = retrofit.create(BicingApiService::class.java)
    val usernameApi: UsernameApi = retrofit.create(UsernameApi::class.java)
    val titolApi: TitolApi = retrofit.create(TitolApi::class.java)
    val teBiciApi: TeBiciApi = retrofit.create(TeBiciApi::class.java)
    val socialApi: SocialApi = retrofit.create(SocialApi::class.java)
}
