package com.airgo.airgoApp.features.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.data.RouteSummary
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.map.domain.MapSelectionMode
import com.airgo.airgoApp.features.map.domain.MapState
import org.osmdroid.util.GeoPoint

/**
 * Aquest fitxer conté tots els elements que es mostren al mapa
 */

private const val N_DECIMALS = 6
private const val SELECTION_PANEL_WIDTH = 0.9f

private const val ROUTE_PANEL_WIDTH = SELECTION_PANEL_WIDTH

/**
 * Panell de selecció de ruta.
 * L'usuari pot escollir el punt d'inici i destí i calcular la ruta més neta.
 */
@Composable
internal fun routeSelectionPanel(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(ROUTE_PANEL_WIDTH),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            routePointField(
                label = stringResource(R.string.map_route_start_label),
                point = state.startPoint,
                isSelected = state.selectionMode == MapSelectionMode.START,
                onClick = { state.onSetSelectionMode(MapSelectionMode.START) },
            )
            Spacer(Modifier.padding(4.dp))

            // Waypoints intermedios
            state.waypoints.forEachIndexed { index, waypoint ->
                Spacer(Modifier.padding(2.dp))
                routeWaypointField(
                    index = index,
                    point = waypoint,
                    onRemove = { state.onRemoveWaypoint(index) },
                )
                Spacer(Modifier.padding(2.dp))
            }

            // Botón añadir parada intermedia
            OutlinedButton(
                onClick = { state.onAddWaypoint() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selectionMode == MapSelectionMode.NONE,
            ) {
                Text(
                    text =
                        if (state.selectionMode == MapSelectionMode.WAYPOINT) {
                            stringResource(R.string.map_route_selecting_waypoint)
                        } else {
                            stringResource(R.string.map_route_add_waypoint)
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.padding(4.dp))

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

@Composable
private fun routeWaypointField(
    index: Int,
    point: GeoPoint,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.padding(start = 8.dp))
            Text(
                text = "${point.latitude.toString().take(N_DECIMALS)}, ${point.longitude.toString().take(N_DECIMALS)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar parada",
                    modifier = Modifier.size(16.dp),
                )
            }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
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
        IconButton(onClick = { state.onAddToFavorites() }) {
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement =
            Arrangement
                .spacedBy(8.dp),
    ) {
        Button(
            onClick = { state.onCalculateRoute() },
            enabled = state.startPoint != null && state.endPoint != null && !state.isCalculatingRoute,
            modifier = Modifier.weight(1f),
        ) {
            if (state.isCalculatingRoute) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
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
        OutlinedButton(
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
    Surface(
        onClick = onClick,
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (label.contains("Inici")) Icons.Default.Cloud else Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.padding(start = 8.dp))
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
    Card(
        modifier = modifier,
        shape = CircleShape,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = SELECTION_PANEL_WIDTH),
            ),
        elevation =
            CardDefaults
                .cardElevation(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(55.dp),
        )
    }
}

// selector mode (bici-correr)
@Composable
internal fun modeSelector(
    transportProfile: TransportProfile,
    onModeChange: (TransportProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = SELECTION_PANEL_WIDTH),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Bici
            FilledTonalIconToggleButton(
                checked = transportProfile == TransportProfile.BIKE,
                onCheckedChange = { onModeChange(TransportProfile.BIKE) },
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
            // A peu
            FilledTonalIconToggleButton(
                checked = transportProfile == TransportProfile.FOOT,
                onCheckedChange = { onModeChange(TransportProfile.FOOT) },
                colors =
                    IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = stringResource(R.string.map_mode_walk),
                    modifier = Modifier.size(24.dp),
                )
            }
            // Running
            FilledTonalIconToggleButton(
                checked = transportProfile == TransportProfile.RUNNING,
                onCheckedChange = { onModeChange(TransportProfile.RUNNING) },
                colors =
                    IconButtonDefaults.filledTonalIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = stringResource(R.string.map_mode_run),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Composable per al selector de capes, on l'usuari pot activar o desactivar les capes de Bicing i qualitat de l'aire.
 * Activa i desactiva un menú desplegable amb les opcions de capes.
 */
@Composable
internal fun BoxScope.layerSelector(
    showBicing: Boolean,
    onToggleBicing: (Boolean) -> Unit,
    showPollution: Boolean,
    onTogglePollution: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Colors per indicar si la capa està activa o no
    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val inactiveBg = Color.Transparent

    Box(
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 100.dp, end = 16.dp),
    ) {
        FloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Layers, contentDescription = stringResource(R.string.map_layers_description))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Capa Bicing
            DropdownMenuItem(
                modifier = Modifier.background(if (showBicing) activeBg else inactiveBg),
                text = {
                    Text(
                        text = stringResource(R.string.map_layer_bicing),
                        color = if (showBicing) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = if (showBicing) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                },
                onClick = { onToggleBicing(!showBicing) },
            )

            HorizontalDivider()

            // Capa qualitat de l'aire
            DropdownMenuItem(
                modifier = Modifier.background(if (showPollution) activeBg else inactiveBg),
                text = {
                    Text(
                        text = stringResource(R.string.map_layer_pollution),
                        color = if (showPollution) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (showPollution) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                },
                onClick = { onTogglePollution(!showPollution) },
            )
        }
    }
}
