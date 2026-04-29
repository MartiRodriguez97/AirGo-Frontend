package com.airgo.airgoApp.features.home.data

import androidx.compose.runtime.mutableStateListOf

data class TrainingPlanResponse(
    val id: Int,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val templates: List<TrainingTemplate>
)

data class TrainingTemplate(
    val nom: String,
    val descripcio: String,
    val tipusExercici: String,
    val exercicis: List<Exercise>
)

data class Exercise(
    val dataInici: String?,
    val dataFi: String?,
    val completat: Boolean,
    val tipusExercici: String,
    val detalls_especifics: Map<String, Any>? = null
)

data class TrainingPlan(
    val sport: String,
    val level: String,
    val days: List<String>,
    val endDate: String,
)

object TrainingPlanStore {
    val plans = mutableStateListOf<TrainingPlan>()
}
