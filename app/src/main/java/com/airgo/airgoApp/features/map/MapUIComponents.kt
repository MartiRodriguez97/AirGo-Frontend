package com.airgo.airgoApp.features.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import org.osmdroid.util.GeoPoint

/**
 * Aquest fitxer conté tots els elements que es mostren al mapa
 */

private const val N_DECIMALS = 6
private const val GPS_CARD_WIDTH = 0.4f
private const val ROUTE_PANEL_WIDTH = 0.9f

/**
 * Panell de selecció de ruta.
 * L'usuari pot escollir el punt d'inici i destí i calcular la ruta més neta.
 */
@Composable
internal fun routeSelectionPanel(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(ROUTE_PANEL_WIDTH),
        elevation =
            androidx.compose.material3.CardDefaults
                .cardElevation(8.dp),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(12.dp)) {
            routePointField(
                label = stringResource(R.string.map_route_start_label),
                point = state.startPoint,
                isSelected = state.selectionMode == MapSelectionMode.START,
                onClick = { state.onSetSelectionMode(MapSelectionMode.START) },
            )
            androidx.compose.foundation.layout
                .Spacer(Modifier.padding(4.dp))
            routePointField(
                label = stringResource(R.string.map_route_end_label),
                point = state.endPoint,
                isSelected = state.selectionMode == MapSelectionMode.END,
                onClick = { state.onSetSelectionMode(MapSelectionMode.END) },
            )
            state.currentRoute?.summary?.let { summary ->
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                routeSummaryRow(state = state, summary = summary)
            }
            routeActionButtons(state = state)
        }
    }
}

/**
 * Fila que mostra el resum de la ruta calculada, amb durada, distància i botó per afegir a preferits.
 */
@Composable
private fun routeSummaryRow(
    state: MapState,
    summary: RouteSummary,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(
            text = "⏱ ${summary.durationMinutes.toInt()} min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "📏 ${summary.distanceMeters} m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        androidx.compose.material3.IconButton(onClick = { state.onAddToFavorites() }) {
            Icon(
                imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Afegir a preferits",
                tint = if (state.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Botons d'acció per calcular o netejar la ruta.
 */
@Composable
private fun routeActionButtons(state: MapState) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(8.dp),
    ) {
        androidx.compose.material3.Button(
            onClick = { state.onCalculateRoute() },
            enabled = state.startPoint != null && state.endPoint != null && !state.isCalculatingRoute,
            modifier = Modifier.weight(1f),
        ) {
            if (state.isCalculatingRoute) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Text(stringResource(R.string.map_route_calculating))
                }
            } else {
                Text(stringResource(R.string.map_route_calculate_button))
            }
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { state.onClearRoute() },
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.map_route_clear_button)) }
    }
}

/**
 * Camp de punt (inici i destí) al panell de selecció de ruta.
 * Mostra el punt seleccionat o un text per indicar que no hi ha cap punt seleccionat.
 */
@Composable
private fun routePointField(
    label: String,
    point: GeoPoint?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = (
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    )
    val displayText =
        if (point != null) {
            val latToPrint = point.latitude.toString().take(N_DECIMALS)
            val lonToPrint = point.longitude.toString().take(N_DECIMALS)
            "$latToPrint, $lonToPrint"
        } else {
            label
        }
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (label.contains("Inici")) Icons.Default.Cloud else Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            androidx.compose.foundation.layout
                .Spacer(Modifier.padding(start = 8.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Missatge de GPS desactivat.
 */
@Composable
internal fun gpsDisabledWarning(modifier: Modifier = Modifier) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(GPS_CARD_WIDTH),
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            ),
        elevation =
            androidx.compose.material3.CardDefaults
                .cardElevation(4.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.map_gps_disabled),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// selector mode (bici-correr)
@Composable
internal fun modeSelector(
    isBiking: Boolean,
    onModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(4.dp),
        ) {
            // Botón Modo Pie / Correr
            FilledTonalIconToggleButton(
                checked = !isBiking,
                onCheckedChange = { onModeChange(false) },
                colors =
                    IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = stringResource(R.string.map_mode_walk),
                    modifier = Modifier.size(24.dp),
                )
            }

            // Botón Modo Bici
            FilledTonalIconToggleButton(
                checked = isBiking,
                onCheckedChange = { onModeChange(true) },
                colors =
                    IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = stringResource(R.string.map_mode_bike),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
