package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TrainingPlanApi {
    @GET(PLA_ENTRENAMENT)
    suspend fun getTrainingPlans(): List<TrainingPlanResponse>

    @POST(PLA_ENTRENAMENT)
    suspend fun createTrainingPlan(@Body request: TrainingPlanRequest): TrainingPlanResponse

    @DELETE("$PLA_ENTRENAMENT/{id}")
    suspend fun deleteTrainingPlan(@Path("id") id: Int)
}

data class TrainingPlanRequest(
    val sport: String,
    val level: String,
    val days: List<String>,
    val endDate: String
)

