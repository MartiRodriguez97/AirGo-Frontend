package com.airgo.airgoApp.features.tracking

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.features.map.helpers.configureMapView
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel.SaveState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

private const val SEC_PER_MIN = 60
private const val TRACKING_ROUTE_PANEL = 0.9f

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun trackingSummaryScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: TrackingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val data = viewModel.data
    val saveState = viewModel.saveState

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            viewModel.clearSummary()
            viewModel.clearSaveState()
            onSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resum del tracking") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSummary()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tornar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText =
                            buildShareText(
                                distanceMeters = data.distanceMeters,
                                elapsedSeconds = data.elapsedSeconds,
                                avgSpeedKmh = data.avgSpeedKmh,
                            )
                        val intent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                        context.startActivity(Intent.createChooser(intent, "Compartir activitat"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            trackingSummaryStatsCard(data = data)
            Spacer(modifier = Modifier.height(16.dp))
            trackingSummaryRouteCard(trackPoints = viewModel.trackPoints)
            Spacer(modifier = Modifier.weight(1f))
            trackingSummaryActions(
                saveState = saveState,
                onSave = { viewModel.saveActivity() },
                onDiscard = {
                    viewModel.clearSummary()
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun trackingSummaryStatsCard(data: com.airgo.airgoApp.features.tracking.data.TrackingData) {
    Card(
        modifier = Modifier.fillMaxWidth(TRACKING_ROUTE_PANEL),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Estadístiques", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            summaryStatRow(label = "Distància", value = "%.0f m".format(data.distanceMeters))
            summaryStatRow(label = "Temps", value = formatElapsedTime(data.elapsedSeconds))
            summaryStatRow(label = "Velocitat mitjana", value = "%.1f km/h".format(data.avgSpeedKmh))
        }
    }
}

private const val TRACKING_LINE_WIDTH = 8f

private const val BORDER_SIZE = 50

/**
 * Mostra un resum de la ruta amb un mapa i el nombre de punts GPS registrats.
 */
@Composable
private fun trackingSummaryRouteCard(
    trackPoints: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(TRACKING_ROUTE_PANEL),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ruta", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "${trackPoints.size} punts GPS registrats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trackPoints.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    factory = { context ->
                        MapView(context).apply {
                            configureMapView(this)
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            isClickable = false
                        }
                    },
                    update = { map ->
                        // Netejar polilínies anteriors
                        map.overlays.removeAll(
                            map.overlays.filterIsInstance<Polyline>(),
                        )
                        // Dibuixar la ruta
                        val polyline =
                            Polyline(map).apply {
                                setPoints(trackPoints)
                                outlinePaint.color = android.graphics.Color.BLUE
                                outlinePaint.strokeWidth = TRACKING_LINE_WIDTH
                            }
                        map.overlays.add(polyline)
                        // Centrar i fer zoom per mostrar tota la ruta
                        val boundingBox = BoundingBox.fromGeoPoints(trackPoints)
                        map.post {
                            map.zoomToBoundingBox(boundingBox, false, BORDER_SIZE)
                        }
                        map.invalidate()
                    },
                )
            }
        }
    }
}

@Composable
private fun summaryStatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun trackingSummaryActions(
    saveState: SaveState,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = saveState !is SaveState.Saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saveState is SaveState.Saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Guardar activitat")
            }
        }
        if (saveState is SaveState.Error) {
            Text(
                text = saveState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) {
            Text("Descartar")
        }
    }
}

private fun buildShareText(
    distanceMeters: Double,
    elapsedSeconds: Long,
    avgSpeedKmh: Double,
): String {
    val mins = elapsedSeconds / SEC_PER_MIN
    val secs = elapsedSeconds % SEC_PER_MIN
    return "He recorregut %.0f m en %02d:%02d min a %.1f km/h de mitjana amb AirGo 🚴".format(
        distanceMeters,
        mins,
        secs,
        avgSpeedKmh,
    )
}

private fun formatElapsedTime(seconds: Long): String {
    val mins = seconds / SEC_PER_MIN
    val secs = seconds % SEC_PER_MIN
    return "%02d:%02d".format(mins, secs)
}
