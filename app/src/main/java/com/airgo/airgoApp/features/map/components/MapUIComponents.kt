package com.airgo.airgoApp.features.map.components

import android.annotation.SuppressLint
import android.graphics.drawable.LayerDrawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.DrawableCompat
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.data.TransportProfile
import com.airgo.airgoApp.features.map.domain.MapState
import com.airgo.airgoApp.features.tracking.components.TrackingFabState
import com.airgo.airgoApp.features.tracking.components.trackingFab

/**
 * Aquest fitxer conté tots els elements que es mostren al mapa
 */

private const val SELECTION_PANEL_WIDTH = 0.9f
private const val COLOR_BICING_BLUE = 0xFF0081C9
private const val COLOR_BICING_RED = 0xFFFF0000
private const val COLOR_ROUTE_START = 0xFF00C853
private const val COLOR_ROUTE_END = 0xFFFF1744
private const val COLOR_WAYPOINT = 0xFFFF8C00
private const val LEGEND_HALF_WIDTH = 0.5f

/**
 * Missatge de GPS desactivat.
 */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
internal fun gpsDisabledWarning(
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    FloatingActionButton(
        onClick = {
            val message =
                if (!hasLocationPermission) {
                    context.getString(R.string.location_permission_required)
                } else {
                    context.getString(R.string.map_gps_disabled)
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.error,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = stringResource(R.string.map_gps_disabled),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
internal fun locationCenteringButton(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = {
            state.locationOverlay.myLocation?.let { loc ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    state.mapView.controller.animateTo(loc)
                }
            }
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = stringResource(R.string.center_location),
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

/**
 * Botó de tracking, que canvia d'estat segons si s'està fent tracking, si es tenen permisos i si el GPS està activat.
 * Mostra un missatge d'error si es vol iniciar el tracking sense permisos o amb el GPS desactivat.
 */
@Composable
internal fun BoxScope.trackingFabSection(
    state: MapState,
    isGpsEnabled: Boolean,
    context: android.content.Context,
) {
    val permissionError = stringResource(R.string.location_permission_required)
    val gpsError = stringResource(R.string.map_gps_disabled)
    trackingFab(
        state =
            TrackingFabState(
                isTracking = state.isTracking,
                hasLocationPermission = state.hasLocationPermission,
                isGpsOn = isGpsEnabled,
                onStartTracking = state.onStartTracking,
                onStopTracking = state.onStopTracking,
                onShowPermissionError = {
                    Toast.makeText(context, permissionError, Toast.LENGTH_SHORT).show()
                },
                onShowGpsError = {
                    Toast.makeText(context, gpsError, Toast.LENGTH_SHORT).show()
                },
            ),
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 100.dp),
    )
}

@Composable
internal fun mapLegend(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(LEGEND_HALF_WIDTH),
        horizontalAlignment = Alignment.Start,
    ) {
        Surface(
            onClick = { expanded = !expanded },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(R.string.legend_title),
                    style = MaterialTheme.typography.labelSmall,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier.padding(top = 4.dp),
                elevation = cardElevation(4.dp),
                colors =
                    cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    legendItem(Color(COLOR_BICING_BLUE), stringResource(R.string.legend_bicing_available), true)
                    legendItem(Color(COLOR_BICING_RED), stringResource(R.string.legend_bicing_empty), true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    legendItem(Color(COLOR_ROUTE_START), stringResource(R.string.legend_route_start), false)
                    legendItem(Color(COLOR_ROUTE_END), stringResource(R.string.legend_route_end), false)
                    legendItem(Color(COLOR_WAYPOINT), stringResource(R.string.legend_waypoint), false)
                }
            }
        }
    }
}

@Composable
private fun legendItem(
    color: Color,
    label: String,
    useFlag: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (useFlag) {
            legendBicingMarker(color = color)
        } else {
            legendRouteMarker(color = color)
        }
        Spacer(modifier = Modifier.size(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun legendRouteMarker(color: Color) {
    val colorInt = color.toArgb()
    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                val drawable = ctx.getDrawable(R.drawable.ic_route_marker)?.mutate()
                (drawable as? LayerDrawable)?.let { layer ->
                    val flag = layer.findDrawableByLayerId(R.id.flag_layer)
                    flag?.let {
                        DrawableCompat.setTint(
                            DrawableCompat.wrap(it).mutate(),
                            colorInt,
                        )
                    }
                }
                setImageDrawable(drawable)
            }
        },
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun legendBicingMarker(color: Color) {
    val colorInt = color.toArgb()
    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                val drawable = ctx.getDrawable(R.drawable.ic_bicing_marker)?.mutate()
                (drawable as? LayerDrawable)?.let { layer ->
                    val pin = layer.findDrawableByLayerId(R.id.pin_layer)
                    val bike = layer.findDrawableByLayerId(R.id.bike_layer)
                    pin?.let { DrawableCompat.setTint(DrawableCompat.wrap(it).mutate(), colorInt) }
                    bike?.let { DrawableCompat.setTint(DrawableCompat.wrap(it).mutate(), colorInt) }
                }
                setImageDrawable(drawable)
            }
        },
        modifier = Modifier.size(16.dp),
    )
}

/**
 * Funció per definir l'estructura del panell d'informació de les estacions Bicing
 */
@Composable
internal fun bicingStatItem(
    emoji: String,
    value: String,
    label: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
