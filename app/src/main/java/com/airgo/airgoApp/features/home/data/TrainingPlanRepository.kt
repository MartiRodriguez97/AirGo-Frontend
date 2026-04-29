package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.RetrofitProvider.trainingPlanApi

class TrainingPlanRepository {
    private val api = trainingPlanApi

    suspend fun getTrainingPlans(): List<TrainingPlanResponse> {
        return api.getTrainingPlans()
    }

    suspend fun createTrainingPlan(request: TrainingPlanRequest): TrainingPlanResponse {
        return api.createTrainingPlan(request)
    }

    suspend fun deleteTrainingPlan(id: Int) {
        api.deleteTrainingPlan(id)
    }
}
