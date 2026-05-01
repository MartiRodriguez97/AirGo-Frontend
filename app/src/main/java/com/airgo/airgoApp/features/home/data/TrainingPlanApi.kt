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
    suspend fun createTrainingPlan(
        @Body request: TrainingPlanRequest,
    ): TrainingPlanResponse

    @DELETE("$PLA_ENTRENAMENT{id}/")
    suspend fun deleteTrainingPlan(
        @Path("id") id: Int,
    )

    @retrofit2.http.PATCH("/api/exercici/{id}/")
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
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
)
