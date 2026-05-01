package com.airgo.airgoApp.features.activity

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airgo.airgoApp.features.map.osmView
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun activityScreen(navController: NavController) {
    val trackingViewModel: TrackingViewModel = viewModel()
    osmView(
        onNavigateToFavorites = { navController.navigate("favorites") },
        onNavigateToTrackingSummary = { navController.navigate("tracking_summary") },
        trackingViewModel = trackingViewModel,
    )
}
