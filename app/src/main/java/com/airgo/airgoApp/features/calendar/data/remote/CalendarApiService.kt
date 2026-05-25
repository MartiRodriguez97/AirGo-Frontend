package com.airgo.airgoApp.features.calendar.data.remote

import com.airgo.airgoApp.core.network.ApiEndpoints.MEUS_PLANS_ENTRENAMENT
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import retrofit2.Response
import retrofit2.http.GET

interface CalendarApiService {
    @GET(MEUS_PLANS_ENTRENAMENT)
    suspend fun getPlaEntrenament(): Response<List<TrainingPlanResponse>>
}
