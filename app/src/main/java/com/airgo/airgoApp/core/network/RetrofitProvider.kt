package com.airgo.airgoApp.core.network

import com.airgo.airgoApp.BuildConfig
import com.airgo.airgoApp.features.activity.data.ActivityApi
import com.airgo.airgoApp.features.calendar.data.remote.CalendarApiService
import com.airgo.airgoApp.features.login.data.auth.AuthApi
import com.airgo.airgoApp.features.map.data.AirQualityApiService
import com.airgo.airgoApp.features.map.data.BicingApiService
import com.airgo.airgoApp.features.map.data.FavoriteRouteApiService
import com.airgo.airgoApp.features.map.data.RouteGenerationApiService
import com.airgo.airgoApp.features.onboarding.data.OnboardingApi
import com.airgo.airgoApp.features.profile.data.ProfileApi
import com.airgo.airgoApp.features.settings.account.tebici.data.TeBiciApi
import com.airgo.airgoApp.features.settings.account.titols.data.TitolApi
import com.airgo.airgoApp.features.settings.account.username.data.UsernameApi
import com.airgo.airgoApp.features.social.chat.data.ChatApi
import com.airgo.airgoApp.features.social.data.SocialApi
import com.airgo.airgoApp.features.social.forums.data.ForumApi
import com.airgo.airgoApp.features.social.forums.data.ForumChatApi
import com.airgo.airgoApp.features.tracking.data.TrackingApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

object RetrofitProvider {
    private const val BACKEND_URL = BuildConfig.BACKEND_URL
    private val authInterceptor =
        Interceptor { chain ->
            val originalRequest = chain.request()

            // Comprobamos si la petición tiene nuestro Header especial
            if (originalRequest.header("No-Authentication") == "true") {
                // Quitamos la bandera porque ya no la necesitamos, y no le ponemos el Authorization
                val modifiedRequest =
                    originalRequest
                        .newBuilder()
                        .removeHeader("No-Authentication")
                        .build()
                return@Interceptor chain.proceed(modifiedRequest)
            }

            val requestBuilder = originalRequest.newBuilder()

            SessionManager.accessToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

    private val loggingInterceptor =
        Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()

            // Solo logueamos rutas y planes para no saturar
            if (url.contains("route-generation") || url.contains("pla-entrenament")) {
                if (request.method == "POST") {
                    val requestBuffer = Buffer()
                    request.body?.writeTo(requestBuffer)
                }
            }

            val response = chain.proceed(request)
            response
        }

    const val MIN_TIME_WAITING_FOR_RESPONSE = 30L // segundos
    private val httpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
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
    val calendarApi: CalendarApiService = retrofit.create(CalendarApiService::class.java)
    val trainingPlanApi: com.airgo.airgoApp.features.home.data.TrainingPlanApi =
        retrofit.create(
            com.airgo.airgoApp.features.home.data.TrainingPlanApi::class.java,
        )
    val favoriteRouteApi: FavoriteRouteApiService = retrofit.create(FavoriteRouteApiService::class.java)
    val trackingApi: TrackingApi = retrofit.create(TrackingApi::class.java)
    val onboardingApi: OnboardingApi = retrofit.create(OnboardingApi::class.java)
    val chatApi: ChatApi = retrofit.create(ChatApi::class.java)
    val activityApi: ActivityApi by lazy { retrofit.create(ActivityApi::class.java) }
    val forumApi: ForumApi = retrofit.create(ForumApi::class.java)
    val forumChatApi: ForumChatApi = retrofit.create(ForumChatApi::class.java)
}
