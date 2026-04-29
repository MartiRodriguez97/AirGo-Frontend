package com.airgo.airgoApp.features.home

import androidx.compose.runtime.mutableStateListOf

data class TrainingPlan(
    val sport: String,
    val level: String,
    val days: List<String>,
    val endDate: String,
)

object TrainingPlanStore {
    val plans = mutableStateListOf<TrainingPlan>()
}
