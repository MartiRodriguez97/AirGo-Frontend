package com.airgo.airgoApp.features.tracking.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.helpers.configureMapView
import com.airgo.airgoApp.features.profile.data.BadgeDefinition
import com.airgo.airgoApp.features.tracking.data.TrackingData
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel.SaveState
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

private const val TRACKING_ROUTE_PANEL = 0.9f
private const val ELEVATION_DEFAULT = 4
private const val PADDING_SMALL = 4
private const val PADDING_MEDIUM = 8
private const val PADDING_LARGE = 16
private const val PADDING_EXTRA_LARGE = 24
private const val PADDING_ACTIONS_BOTTOM = 80
private const val MAP_HEIGHT_RES = 200
private const val ICON_SIZE_BADGE = 80
private const val PROGRESS_INDICATOR_SIZE = 20
private const val PROGRESS_INDICATOR_STROKE = 2
private const val TEXT_SIZE_TITLE = 20
private const val TEXT_SIZE_SUBTITLE = 18
private const val TEXT_SIZE_BODY = 14
private const val TRACKING_LINE_WIDTH = 8f
private const val BORDER_SIZE = 50
private const val MIN_TRACK_POINTS = 2
private const val SEC_PER_MIN = 60

@Composable
internal fun trackingSummaryBadgeHandler(
    saveState: SaveState,
    activitySavedText: String,
    viewModel: TrackingViewModel,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var showBadgeDialog by remember { mutableStateOf<BadgeDefinition?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            val badges = saveState.response.newBadges
            if (!badges.isNullOrEmpty()) {
                val b = badges.first()
                showBadgeDialog = BadgeDefinition(b.id, b.nom, b.descripcio, b.icona, "", 0)
            } else {
                finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
            }
        }
    }

    showBadgeDialog?.let { badge ->
        newBadgeDialog(badge) {
            showBadgeDialog = null
            finishTrackingSummary(context, activitySavedText, viewModel, onFinish)
        }
    }
}

private fun finishTrackingSummary(
    context: Context,
    message: String,
    viewModel: TrackingViewModel,
    onFinish: () -> Unit,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    viewModel.clearSummary()
    viewModel.clearSaveState()
    onFinish()
}

@Composable
internal fun buildShareText(
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

@Composable
internal fun trackingSummaryBody(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    data: TrackingData,
    trackPoints: List<GeoPoint>,
    saveState: SaveState,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = PADDING_LARGE.dp, vertical = PADDING_LARGE.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        trackingSummaryStatsCard(data = data)
        Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
        trackingSummaryRouteCard(trackPoints = trackPoints)
        Spacer(modifier = Modifier.weight(1f))
        trackingSummaryActions(
            saveState = saveState,
            onSave = onSave,
            onDiscard = onDiscard,
        )
    }
}

@Composable
internal fun trackingSummaryStatsCard(data: TrackingData) {
    Card(
        modifier = Modifier.fillMaxWidth(TRACKING_ROUTE_PANEL),
        elevation = CardDefaults.cardElevation(ELEVATION_DEFAULT.dp),
    ) {
        Column(modifier = Modifier.padding(PADDING_LARGE.dp)) {
            Text(stringResource(R.string.statistics), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = PADDING_MEDIUM.dp))
            summaryStatRow(label = stringResource(R.string.distance), value = "%.0f m".format(data.distanceMeters))
            summaryStatRow(
                label = stringResource(R.string.time),
                value = formatElapsedTime(data.elapsedSeconds),
            )
            summaryStatRow(label = stringResource(R.string.avg_speed), value = "%.1f km/h".format(data.avgSpeedKmh))
        }
    }
}

@Composable
internal fun trackingSummaryRouteCard(
    trackPoints: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(TRACKING_ROUTE_PANEL),
        elevation = CardDefaults.cardElevation(ELEVATION_DEFAULT.dp),
    ) {
        Column(modifier = Modifier.padding(PADDING_LARGE.dp)) {
            Text(stringResource(R.string.route), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = PADDING_MEDIUM.dp))
            Text(
                text = stringResource(R.string.tracking_summary_gps_points, trackPoints.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trackPoints.size >= MIN_TRACK_POINTS) {
                Spacer(modifier = Modifier.height(PADDING_MEDIUM.dp))
                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(MAP_HEIGHT_RES.dp),
                    factory = { context ->
                        MapView(context).apply {
                            configureMapView(this)
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            isClickable = false
                        }
                    },
                    update = { map ->
                        map.overlays.removeAll(map.overlays.filterIsInstance<Polyline>())
                        val polyline =
                            Polyline(map).apply {
                                setPoints(trackPoints)
                                outlinePaint.color = Color.BLUE
                                outlinePaint.strokeWidth = TRACKING_LINE_WIDTH
                            }
                        map.overlays.add(polyline)
                        val boundingBox = BoundingBox.fromGeoPoints(trackPoints)
                        map.post { map.zoomToBoundingBox(boundingBox, false, BORDER_SIZE) }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = PADDING_SMALL.dp),
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
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = PADDING_ACTIONS_BOTTOM.dp),
        verticalArrangement = Arrangement.spacedBy(PADDING_MEDIUM.dp),
    ) {
        Button(
            onClick = onSave,
            enabled = saveState !is SaveState.Saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saveState is SaveState.Saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PROGRESS_INDICATOR_SIZE.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = PROGRESS_INDICATOR_STROKE.dp,
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
private fun newBadgeDialog(
    badge: BadgeDefinition,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(PADDING_LARGE.dp).fillMaxWidth(),
            shape = RoundedCornerShape(PADDING_LARGE.dp),
        ) {
            Column(
                modifier = Modifier.padding(PADDING_EXTRA_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Nova Insígnia!", fontWeight = FontWeight.Bold, fontSize = TEXT_SIZE_TITLE.sp)
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
                if (badge.icona != null) {
                    coil.compose.AsyncImage(
                        model = badge.icona,
                        contentDescription = "Icona Insignia",
                        modifier = Modifier.size(ICON_SIZE_BADGE.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Default Badge Icon",
                        modifier = Modifier.size(ICON_SIZE_BADGE.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(PADDING_LARGE.dp))
                Text(
                    text = badge.nom,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TEXT_SIZE_SUBTITLE.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(PADDING_MEDIUM.dp))
                Text(text = badge.descripcio, fontSize = TEXT_SIZE_BODY.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(PADDING_EXTRA_LARGE.dp))
                Button(onClick = onDismiss) { Text("Acceptar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun trackingSummaryTopBar(
    title: String,
    backDescription: String,
    shareTitle: String,
    shareText: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription)
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
                Icon(Icons.Default.Share, contentDescription = shareTitle)
            }
        },
    )
}
