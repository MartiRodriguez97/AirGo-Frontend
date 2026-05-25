package com.airgo.airgoApp.features.home.data

import com.airgo.airgoApp.core.network.RetrofitProvider.trainingPlanApi

class TrainingPlanRepository {
    private val api = trainingPlanApi

    suspend fun getTrainingPlans(): List<TrainingPlanResponse> = api.getTrainingPlans()

    suspend fun getTemplate(id: Int): TrainingTemplate = api.getTemplate(id)

    @Suppress("MaxLineLength")
    suspend fun createTrainingPlan(request: TrainingPlanRequest): CreateTrainingPlanResponse = api.createTrainingPlan(request)

    suspend fun getActivities(): List<Exercise> = api.getActivities()

    suspend fun initializeTrainingPlan(
        id: Int,
        request: TrainingPlanRequest,
    ): TrainingPlanRawResponse = api.initializeTrainingPlan(id, request)

    suspend fun deleteTrainingPlan(id: Int) {
        api.deleteTrainingPlan(id)
    }

/*
    suspend fun fetchFullTrainingPlans(): List<TrainingPlanResponse> {
        val rawPlans = getTrainingPlans()
        return rawPlans.map { raw ->
            val fullTemplates =
                raw.templates.map { templateId ->
                    getTemplate(templateId)
                }
            TrainingPlanResponse(
                id = raw.id,
                diesDurada = raw.diesDurada,
                numEntrenamentsSetmanals = raw.numEntrenamentsSetmanals,
                templates = fullTemplates,
            )
        }
    }
*/
    suspend fun fetchFullTrainingPlans(): List<TrainingPlanResponse> {
        // Como getTrainingPlans() ahora ya devuelve la lista completa de TrainingPlanResponse,
        // solo tienes que devolver su resultado directamente.
        return getTrainingPlans()
    }

    suspend fun updateExercise(
        id: Int,
        completat: Boolean,
        distanciaFeta: Double,
    ): Exercise = api.updateExercise(id, UpdateExerciseRequest(completat, distanciaFeta))
}
