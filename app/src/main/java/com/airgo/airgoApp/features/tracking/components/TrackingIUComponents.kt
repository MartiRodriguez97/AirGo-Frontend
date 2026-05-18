package com.airgo.airgoApp.features.tracking.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentNeutral
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.tracking.data.ActivitySensation
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
        text = {
            Text(
                text =
                    if (state.isTracking) {
                        stringResource(
                            R.string.tracking_btn_stop,
                        )
                    } else {
                        stringResource(R.string.btn_start)
                    },
            )
        },
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
    val activeExerciseName = SessionManager.nomTemplateActiu ?: stringResource(R.string.nav_activity)
    val targetDistance = SessionManager.exerciciActiu?.distanciaObjectiu ?: 0.0

    Column(
        modifier = modifier.fillMaxWidth(TRACKING_PANEL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = activeExerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (targetDistance > 0) {
                    Text(
                        text = stringResource(R.string.tracking_label_target, targetDistance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    trackingStatItem(
                        label = stringResource(R.string.tracking_stat_distance),
                        value = stringResource(R.string.tracking_unit_meters, data.distanceMeters),
                    )
                    trackingStatItem(
                        label = stringResource(R.string.tracking_stat_time),
                        value = formatElapsedTime(data.elapsedSeconds),
                    )
                    trackingStatItem(
                        label = stringResource(R.string.tracking_stat_speed),
                        value = stringResource(R.string.tracking_unit_speed_kmh, data.speedKmh),
                    )
                }
            }
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

@Composable
fun postActivitySensationDialog(
    onDismiss: () -> Unit,
    onSensationSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.tracking_sensation_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Iteramos sobre las 5 sensaciones que definimos en el Enum
                ActivitySensation.values().forEach { sensation ->
                    val icon =
                        when (sensation) {
                            ActivitySensation.VERY_BAD -> Icons.Outlined.SentimentVeryDissatisfied
                            ActivitySensation.BAD -> Icons.Outlined.SentimentDissatisfied
                            ActivitySensation.NORMAL -> Icons.Outlined.SentimentNeutral
                            ActivitySensation.GOOD -> Icons.Outlined.SentimentSatisfied
                            ActivitySensation.VERY_GOOD -> Icons.Outlined.SentimentVerySatisfied
                        }

                    IconButton(
                        onClick = { onSensationSelected(sensation.value) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(id = sensation.labelRes),
                            tint = sensation.color,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                // Omitir
                Text(stringResource(R.string.tracking_sensation_skip))
            }
        },
    )
}
