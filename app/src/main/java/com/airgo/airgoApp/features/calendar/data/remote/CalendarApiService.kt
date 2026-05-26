package com.airgo.airgoApp.features.calendar.data.remote

import com.airgo.airgoApp.core.network.ApiEndpoints.ACTIVITY
import com.airgo.airgoApp.core.network.ApiEndpoints.MEUS_PLANS_ENTRENAMENT
import com.airgo.airgoApp.features.home.data.Exercise
import com.airgo.airgoApp.features.home.data.TrainingPlanResponse
import com.airgo.airgoApp.features.home.data.UpdateExerciseDateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface CalendarApiService {
    @GET(MEUS_PLANS_ENTRENAMENT)
    suspend fun getPlaEntrenament(): Response<List<TrainingPlanResponse>>

    @PATCH(ACTIVITY)
    suspend fun updateExerciseDate(
        @Path("id") id: Int,
        @Body request: UpdateExerciseDateRequest,
    ): Exercise
}
