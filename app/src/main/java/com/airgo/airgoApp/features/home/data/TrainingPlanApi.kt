package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.ApiEndpoints.ACTIVITY
import com.airgo.airgoApp.core.network.ApiEndpoints.MEUS_PLANS_ENTRENAMENT
import com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT
import com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT_ID
import com.airgo.airgoApp.core.network.ApiEndpoints.SAVE_ACTIVITY
import com.airgo.airgoApp.core.network.ApiEndpoints.TEMPLATE_EXERCICI
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TrainingPlanApi {
    @GET(MEUS_PLANS_ENTRENAMENT)
    suspend fun getTrainingPlans(): List<TrainingPlanResponse>

    @GET(TEMPLATE_EXERCICI)
    suspend fun getTemplate(
        @Path("id") id: Int,
    ): TrainingTemplate

    @POST(PLA_ENTRENAMENT)
    suspend fun createTrainingPlan(
        @Body request: TrainingPlanRequest,
    ): CreateTrainingPlanResponse

    @DELETE(PLA_ENTRENAMENT_ID)
    suspend fun deleteTrainingPlan(
        @Path("id") id: Int,
    )

    @GET(SAVE_ACTIVITY)
    suspend fun getActivities(): List<Exercise>

    @retrofit2.http.PATCH(ACTIVITY)
    suspend fun updateExercise(
        @Path("id") id: Int,
        @Body request: UpdateExerciseRequest,
    ): Exercise
}

data class UpdateExerciseRequest(
    val completat: Boolean,
    val distanciaFeta: Double,
)

data class TrainingPlanRequest(
    val nom: String,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val esport: String,
    val nivell: Int,
    val diesSetmana: List<Int>,
    val dataFi: String,
    val templates: List<Int> = emptyList(),
)
