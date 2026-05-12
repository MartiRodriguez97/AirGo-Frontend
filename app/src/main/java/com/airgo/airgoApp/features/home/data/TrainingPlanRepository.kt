package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.RetrofitProvider.trainingPlanApi

class TrainingPlanRepository {
    private val api = trainingPlanApi

    suspend fun getTrainingPlans(): List<TrainingPlanRawResponse> = api.getTrainingPlans()

    suspend fun getTemplate(id: Int): TrainingTemplate = api.getTemplate(id)

    suspend fun createTrainingPlan(request: TrainingPlanRequest): TrainingPlanRawResponse = api.createTrainingPlan(request)

    suspend fun initializeTrainingPlan(
        id: Int,
        request: TrainingPlanRequest,
    ): TrainingPlanRawResponse = api.initializeTrainingPlan(id, request)

    suspend fun initializePlanFollowing(
        id: Int,
        request: TrainingPlanRequest,
    ): TrainingPlanRawResponse = api.initializePlanFollowing(id, request)

    suspend fun deleteTrainingPlan(id: Int) {
        api.deleteTrainingPlan(id)
    }

    suspend fun updateExercise(
        id: Int,
        completat: Boolean,
        distanciaFeta: Double,
    ): Exercise = api.updateExercise(id, UpdateExerciseRequest(completat, distanciaFeta))
}
