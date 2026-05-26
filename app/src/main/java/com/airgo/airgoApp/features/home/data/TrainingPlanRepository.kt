package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.RetrofitProvider.trainingPlanApi

class TrainingPlanRepository {
    private val api = trainingPlanApi

    suspend fun getTrainingPlans(): List<TrainingPlanResponse> = api.getTrainingPlans()

    @Suppress("MaxLineLength")
    suspend fun createTrainingPlan(request: TrainingPlanRequest): CreateTrainingPlanResponse = api.createTrainingPlan(request)

    suspend fun deleteTrainingPlan(id: Int) {
        api.deleteTrainingPlan(id)
    }

    suspend fun fetchFullTrainingPlans(): List<TrainingPlanResponse> = getTrainingPlans()

    suspend fun updateTrainingPlanName(
        id: Int,
        newName: String,
    ) {
        api.updateTrainingPlanName(id, UpdatePlanNameRequest(newName))
    }

    suspend fun updateExercise(
        id: Int,
        completat: Boolean,
        distanciaFeta: Double,
    ): Exercise = api.updateExercise(id, UpdateExerciseRequest(completat, distanciaFeta))

    suspend fun updateExerciseDate(
        id: Int,
        dataInici: String,
    ): Exercise = api.updateExerciseDate(id, UpdateExerciseDateRequest(dataInici))
}
