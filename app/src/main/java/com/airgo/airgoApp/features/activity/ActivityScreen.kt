package com.airgo.airgoApp.features.activity

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airgo.airgoApp.core.utils.navigateOnce
import com.airgo.airgoApp.features.map.OsmViewNavigationCallbacks
import com.airgo.airgoApp.features.map.osmView
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun activityScreen(navController: NavController) {
    val trackingViewModel: TrackingViewModel = viewModel()
    osmView(
        navigation =
            OsmViewNavigationCallbacks(
                onNavigateToFavorites = { navController.navigateOnce("favorites") },
                onNavigateToTrackingSummary = { navController.navigateOnce("tracking_summary") },
                onNavigateToAddressSearch = { mode -> navController.navigateOnce("address_search/$mode") },
            ),
        trackingViewModel = trackingViewModel,
    )
}
