package com.airgo.airgoApp.features.calendar.domain.model

data class PlaEntrenament(
    val diesDurada: Int,
    val numEntrenamentsSetmanals: Int,
    val templates: List<Template>,
)

data class Template(
    val nom: String,
    val descripcio: String,
    val tipusExercici: String,
    val exercicis: List<Exercici>,
)

data class Exercici(
    val dataInici: String,
    val dataFi: String?,
    val completat: Boolean,
    val tipusExercici: String,
    val detallsEspecifics: DetallsEspecifics?,
)

data class DetallsEspecifics(
    val distObjectiuKm: Double? = null,
    val distFetaKm: Double? = null,
    val calories: Double? = null,
)
