package com.airgo.airgoApp.features.home.data
import com.google.gson.annotations.SerializedName

data class TrainingPlanResponse(
    val id: Int,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val templates: List<TrainingTemplate> = emptyList(),
)

data class CreateTrainingPlanResponse(
    val status: String,
    val plan: BackendPlanData,
    val exercicis: List<Exercise> = emptyList(),
    val numExercicisCreats: Int,
)

data class BackendPlanData(
    val id: Int,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val esport: String,
    val nivell: Int,
    val diesSetmana: List<Int>,
    val dataFi: String,
    val templates: List<TrainingTemplate> = emptyList(),
)

data class TrainingPlanRawResponse(
    val id: Int,
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val templates: List<Int> = emptyList(),
)

data class TrainingTemplate(
    val id: Int,
    val nom: String,
    val descripcio: String,
    val tipusExercici: String,
    @SerializedName("exercicis")
    private val exercicisResposta: List<Exercise>? = emptyList(),
) {
    val exercicis: List<Exercise>
        get() = exercicisResposta ?: emptyList()
}

data class Exercise(
    val id: Int,
    val dataInici: String?,
    val template: TrainingTemplate?,
    val completat: Boolean,
    val distanciaObjectiu: Double = 0.0,
    val distanciaFeta: Double = 0.0,
)
