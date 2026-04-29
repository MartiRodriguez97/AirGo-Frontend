package com.airgo.airgoApp.features.tracking.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.features.tracking.data.TrackingData

private const val TRACKING_PANEL_WIDTH = 0.9f
private const val SEC_PER_MIN = 60

data class TrackingFabState(
    val isTracking: Boolean,
    val hasLocationPermission: Boolean,
    val isGpsOn: Boolean,
    val onStartTracking: () -> Unit,
    val onStopTracking: () -> Unit,
    val onShowPermissionError: () -> Unit,
    val onShowGpsError: () -> Unit,
)

/**
 * Botó per iniciar o aturar el tracking.
 * Mostra un error si no hi ha permisos o GPS activat.
 */
@Composable
internal fun trackingFab(
    state: TrackingFabState,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = {
            when {
                !state.hasLocationPermission -> state.onShowPermissionError()
                !state.isGpsOn -> state.onShowGpsError()
                state.isTracking -> state.onStopTracking()
                else -> state.onStartTracking()
            }
        },
        modifier = modifier,
        containerColor =
            if (state.isTracking) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = {
            Icon(
                imageVector = if (state.isTracking) Icons.Default.Stop else Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
            )
        },
        text = { Text(text = if (state.isTracking) "Stop" else "Start") },
    )
}

/**
 * Panell de dades en temps real del tracking.
 */
@Composable
internal fun trackingPanel(
    data: TrackingData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(TRACKING_PANEL_WIDTH),
        elevation = CardDefaults.cardElevation(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trackingStatItem(label = "Distància", value = "%.0f m".format(data.distanceMeters))
            trackingStatItem(label = "Temps", value = formatElapsedTime(data.elapsedSeconds))
            trackingStatItem(label = "Velocitat", value = "%.1f km/h".format(data.speedKmh))
        }
    }
}

/**
 * Item individual del panell de tracking, mostra una dada amb la seva etiqueta.
 */
@Composable
private fun trackingStatItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

/**
 * Formateja el temps transcorregut en format mm:ss.
 */
private fun formatElapsedTime(seconds: Long): String {
    val mins = seconds / SEC_PER_MIN
    val secs = seconds % SEC_PER_MIN
    return "%02d:%02d".format(mins, secs)
}
