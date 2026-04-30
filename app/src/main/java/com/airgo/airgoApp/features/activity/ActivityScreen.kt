package com.airgo.airgoApp.features.activity

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airgo.airgoApp.features.map.osmView
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel

@Composable
fun activityScreen(
    navController: NavController,
    trackingViewModel: TrackingViewModel,
) {
    val trackingViewModel: TrackingViewModel = viewModel()
    osmView(
        onNavigateToFavorites = { navController.navigate("favorites") },
        onNavigateToTrackingSummary = { navController.navigate("tracking_summary") },
        trackingViewModel = trackingViewModel,
    )
}
