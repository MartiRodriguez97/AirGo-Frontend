package com.airgo.airgoApp.features.tracking

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.tracking.components.buildShareText
import com.airgo.airgoApp.features.tracking.components.trackingSummaryBadgeHandler
import com.airgo.airgoApp.features.tracking.components.trackingSummaryBody
import com.airgo.airgoApp.features.tracking.components.trackingSummaryTopBar
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

    if (saveState is TrackingViewModel.SaveState.InvalidActivity) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tracking_elevated_speed),
                        textAlign = TextAlign.Center,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSummary()
                    onBack()
                }) {
                    Text(stringResource(R.string.accept))
                }
            },
        )
    }

    trackingSummaryBadgeHandler(
        saveState = saveState,
        activitySavedText = activitySavedText,
        viewModel = viewModel,
        onFinish = onSave,
    )

    Scaffold(
        topBar = {
            trackingSummaryTopBar(
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
        trackingSummaryBody(
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
