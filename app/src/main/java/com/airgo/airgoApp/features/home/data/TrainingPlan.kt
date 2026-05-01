package com.airgo.airgoApp.features.home.data

import androidx.compose.runtime.mutableStateListOf
import com.google.gson.annotations.SerializedName

data class TrainingPlanResponse(
    val id: Int,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val templates: List<TrainingTemplate>,
)

data class TrainingTemplate(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val tipusExercici: String,
    val exercicis: List<Exercise>,
)

data class Exercise(
    val id: Int,
    val dataInici: String?,
    val completat: Boolean,
    val distanciaObjectiu: Double,
    val distanciaFeta: Double,
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
