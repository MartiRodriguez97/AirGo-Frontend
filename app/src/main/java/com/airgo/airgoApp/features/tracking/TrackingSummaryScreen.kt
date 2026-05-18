package com.airgo.airgoApp.features.tracking

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.tracking.data.TrackingData
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel.SaveState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.airgo.airgoApp.features.tracking.data.BadgeResponse
import com.airgo.airgoApp.features.map.helpers.configureMapView

private const val SEC_PER_MIN = 60
private const val TRACKING_ROUTE_PANEL = 0.9f

@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun trackingSummaryScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: TrackingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val data = viewModel.data
    val saveState = viewModel.saveState

    val activitySavedText = stringResource(R.string.activity_saved)
    val shareTitle = stringResource(R.string.share)
    val backDescription = stringResource(R.string.back)
    val summaryTitle = stringResource(R.string.activity_summary)
    val shareText =
        buildShareText(
            distanceMeters = data.distanceMeters,
            elapsedSeconds = data.elapsedSeconds,
            avgSpeedKmh = data.avgSpeedKmh,
        )

    var showBadgeDialog by remember { mutableStateOf<com.airgo.airgoApp.features.profile.data.BadgeDefinition?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            val response = saveState.response
            if (response.newBadges != null && response.newBadges.isNotEmpty()) {
                showBadgeDialog = com.airgo.airgoApp.features.profile.data.BadgeDefinition(response.newBadges.first().id, response.newBadges.first().nom, response.newBadges.first().descripcio, response.newBadges.first().icona, "", 0)
            } else {
                Toast.makeText(context, activitySavedText, Toast.LENGTH_SHORT).show()
                viewModel.clearSummary()
                viewModel.clearSaveState()
                onSave()
            }
        }
    }

    if (showBadgeDialog != null) {
        Dialog(onDismissRequest = {
            showBadgeDialog = null
            Toast.makeText(context, activitySavedText, Toast.LENGTH_SHORT).show()
            viewModel.clearSummary()
            viewModel.clearSaveState()
            onSave()
        }) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Nova Insígnia!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (showBadgeDialog?.icona != null) {
                        coil.compose.AsyncImage(
                            model = showBadgeDialog?.icona,
                            contentDescription = "Icona Insignia",
                            modifier = Modifier.size(80.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Default Badge Icon",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = showBadgeDialog?.nom ?: "",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = showBadgeDialog?.descripcio ?: "",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        showBadgeDialog = null
                        Toast.makeText(context, activitySavedText, Toast.LENGTH_SHORT).show()
                        viewModel.clearSummary()
                        viewModel.clearSaveState()
                        onSave()
                    }) {
                        Text("Acceptar")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(summaryTitle) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSummary()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backDescription,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                        context.startActivity(Intent.createChooser(intent, shareTitle))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = shareTitle,
                        )
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
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
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
private fun trackingSummaryStatsCard(data: TrackingData) {
    Card(
        modifier = Modifier.fillMaxWidth(TRACKING_ROUTE_PANEL),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            summaryStatRow(label = stringResource(R.string.distance), value = "%.0f m".format(data.distanceMeters))
            summaryStatRow(label = stringResource(R.string.time), value = formatElapsedTime(data.elapsedSeconds))
            summaryStatRow(label = stringResource(R.string.avg_speed), value = "%.1f km/h".format(data.avgSpeedKmh))
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
            Text(stringResource(R.string.route), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.tracking_summary_gps_points, trackPoints.size),
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
                                outlinePaint.color = Color.BLUE
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
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                Text(stringResource(R.string.save_activity))
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
            Text(stringResource(R.string.discard))
        }
    }
}

@Composable
private fun buildShareText(
    distanceMeters: Double,
    elapsedSeconds: Long,
    avgSpeedKmh: Double,
): String {
    val mins = elapsedSeconds / SEC_PER_MIN
    val secs = elapsedSeconds % SEC_PER_MIN
    return stringResource(R.string.share_activity_text).format(
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
