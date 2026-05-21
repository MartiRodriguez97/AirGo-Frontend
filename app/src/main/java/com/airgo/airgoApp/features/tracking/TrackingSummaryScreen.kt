package com.airgo.airgoApp.features.tracking

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.tracking.components.TrackingSummaryBadgeHandler
import com.airgo.airgoApp.features.tracking.components.TrackingSummaryBody
import com.airgo.airgoApp.features.tracking.components.TrackingSummaryTopBar
import com.airgo.airgoApp.features.tracking.components.buildShareText
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.domain.saveActivity

/**
 * Pantalla de resum del seguiment de l'activitat.
 * Permet guardar o descartar l'activitat i visualitzar les estadístiques i la ruta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun trackingSummaryScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: TrackingViewModel = viewModel(),
) {
    val data = viewModel.data
    val saveState = viewModel.saveState

    val activitySavedText = stringResource(R.string.activity_saved)
    val shareTitle = stringResource(R.string.share)
    val backDescription = stringResource(R.string.back)
    val summaryTitle = stringResource(R.string.activity_summary)
    val shareText = buildShareText(data.distanceMeters, data.elapsedSeconds, data.avgSpeedKmh)

    TrackingSummaryBadgeHandler(
        saveState = saveState,
        activitySavedText = activitySavedText,
        viewModel = viewModel,
        onFinish = onSave,
    )

    Scaffold(
        topBar = {
            TrackingSummaryTopBar(
                title = summaryTitle,
                backDescription = backDescription,
                shareTitle = shareTitle,
                shareText = shareText,
                onBack = {
                    viewModel.clearSummary()
                    onBack()
                },
            )
        },
    ) { innerPadding ->
        TrackingSummaryBody(
            innerPadding = innerPadding,
            data = data,
            trackPoints = viewModel.trackPoints,
            saveState = saveState,
            onSave = { viewModel.saveActivity() },
            onDiscard = {
                viewModel.clearSummary()
                onBack()
            },
        )
    }
}
