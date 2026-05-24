package com.airgo.airgoApp.features.map.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airgo.airgoApp.features.map.domain.MapSelectionMode
import com.airgo.airgoApp.features.map.domain.MapState

private const val MAX_WAYPOINTS = 4
private const val POINT_BUTTON_SIZE = 36

/**
 * Panel compacte de selecció de ruta.
 */
@Composable
internal fun routeSelectionPanel(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    var showSelectionDialog by remember { mutableStateOf<SelectionTarget?>(null) }

    showSelectionDialog?.let { target ->
        pointSelectionDialog(
            target = target,
            currentAddress = when (target) {
                SelectionTarget.START -> state.startAddressQuery.ifBlank { null }
                SelectionTarget.END -> state.endAddressQuery.ifBlank { null }
                is SelectionTarget.Waypoint -> state.waypointAddresses.getOrNull(target.index)
            },
            onSelectOnMap = {
                when (target) {
                    SelectionTarget.START -> state.onSetSelectionMode(MapSelectionMode.START)
                    SelectionTarget.END -> state.onSetSelectionMode(MapSelectionMode.END)
                    is SelectionTarget.Waypoint -> state.onSetWaypointEditMode(target.index)
                }
                showSelectionDialog = null
            },
            onSearchAddress = {
                state.onNavigateToAddressSearch(
                    when (target) {
                        SelectionTarget.START -> "start"
                        SelectionTarget.END -> "end"
                        is SelectionTarget.Waypoint -> "waypoint-${target.index}"
                    }
                )
                showSelectionDialog = null
            },
            onDismiss = { showSelectionDialog = null },
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Punt inici (A)
                pointButton(
                    label = "A",
                    isActive = state.startPoint != null,
                    isSelected = state.selectionMode == MapSelectionMode.START,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { showSelectionDialog = SelectionTarget.START },
                )

                // Punts intermitjos
                state.waypoints.forEachIndexed { index, _ ->
                    pointButton(
                        label = "${index + 2}",
                        isActive = true,
                        isSelected = state.selectionMode == MapSelectionMode.WAYPOINT && state.editingWaypointIndex == index,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { showSelectionDialog = SelectionTarget.Waypoint(index) },
                    )
                }

                // Botó afegir waypoint
                if (state.waypoints.size < MAX_WAYPOINTS) {
                    addWaypointButton(
                        isSelected = state.selectionMode == MapSelectionMode.WAYPOINT && state.editingWaypointIndex < 0,
                        onClick = { showSelectionDialog = SelectionTarget.Waypoint(state.waypoints.size) },
                    )
                }

                // Punt destí (B)
                pointButton(
                    label = "B",
                    isActive = state.endPoint != null,
                    isSelected = state.selectionMode == MapSelectionMode.END,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { showSelectionDialog = SelectionTarget.END },
                )

                Spacer(modifier = Modifier.weight(1f))

                // Botó calcular
                actionButton(
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Calcular", modifier = Modifier.size(18.dp)) },
                    color = MaterialTheme.colorScheme.primary,
                    enabled = state.startPoint != null && state.endPoint != null,
                    onClick = state.onCalculateRoute,
                )

                // Botó netejar
                actionButton(
                    icon = { Icon(Icons.Default.Close, contentDescription = "Netejar", modifier = Modifier.size(18.dp)) },
                    color = MaterialTheme.colorScheme.error,
                    enabled = state.startPoint != null || state.endPoint != null || state.waypoints.isNotEmpty(),
                    onClick = state.onClearRoute,
                )
            }

            // Resum ruta si existeix
            state.currentRoute?.summary?.let { summary ->
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                routeSummaryCompact(state = state, summary = summary)
            }
        }
    }
}

@Composable
private fun pointButton(
    label: String,
    isActive: Boolean,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(POINT_BUTTON_SIZE.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> color
                    isActive -> color.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (isSelected) 0.dp else 1.5.dp,
                color = if (isActive || isSelected) color else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isActive -> color
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun addWaypointButton(isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(POINT_BUTTON_SIZE.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Afegir punt",
            tint = if (isSelected) MaterialTheme.colorScheme.onTertiary
            else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun actionButton(
    icon: @Composable () -> Unit,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(POINT_BUTTON_SIZE.dp)
            .clip(CircleShape)
            .background(if (enabled) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) color
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        ) {
            icon()
        }
    }
}

@Composable
private fun routeSummaryCompact(
    state: MapState,
    summary: com.airgo.airgoApp.features.map.data.RouteSummary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("⏱ ${summary.durationMinutes.toInt()} min", style = MaterialTheme.typography.bodySmall)
        Text("📏 ${summary.distanceMeters.toInt()} m", style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = { state.onToggleFavorite() }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (state.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun pointSelectionDialog(
    target: SelectionTarget,
    currentAddress: String?,
    onSelectOnMap: () -> Unit,
    onSearchAddress: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (target) {
                    SelectionTarget.START -> "Punt d'inici"
                    SelectionTarget.END -> "Punt de destí"
                    is SelectionTarget.Waypoint -> "Punt intermig"
                },
            )
        },
        text = {
            Column {
                if (!currentAddress.isNullOrBlank()) {
                    Text(
                        text = currentAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text("Com vols seleccionar el punt?")
            }
        },
        confirmButton = {
            TextButton(onClick = onSelectOnMap) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Al mapa")
            }
        },
        dismissButton = {
            TextButton(onClick = onSearchAddress) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cercar adreça")
            }
        },
    )
}

private sealed class SelectionTarget {
    data object START : SelectionTarget()
    data object END : SelectionTarget()
    data class Waypoint(val index: Int) : SelectionTarget()
}