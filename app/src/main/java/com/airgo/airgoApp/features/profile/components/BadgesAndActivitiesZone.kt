package com.airgo.airgoApp.features.profile.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.AppEvents
import com.airgo.airgoApp.features.activity.data.ActivityResponse
import com.airgo.airgoApp.features.activity.domain.ActivityUiState
import com.airgo.airgoApp.features.activity.domain.ActivityViewModel
import com.airgo.airgoApp.features.profile.data.UserBadgeResponse

private const val SECONDS_PER_MINUTE = 60
private const val METERS_PER_KM = 1000

@Composable
fun badgesZone(badges: List<UserBadgeResponse>) {
    var selectedBadge by remember { mutableStateOf<UserBadgeResponse?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = "Les meves insígnies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (badges.isEmpty()) {
            Text(
                text = "Encara no tens cap insígnia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            badgesList(badges) { selectedBadge = it }
        }
    }

    selectedBadge?.let { badge ->
        badgeDetailDialog(badge) { selectedBadge = null }
    }
}

@Composable
private fun badgesList(
    badges: List<UserBadgeResponse>,
    onBadgeClick: (UserBadgeResponse) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(badges) { userBadge ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onBadgeClick(userBadge) },
            ) {
                if (userBadge.insignia.icona != null) {
                    AsyncImage(
                        model = userBadge.insignia.icona,
                        contentDescription = userBadge.insignia.nom,
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = userBadge.insignia.nom,
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private const val BADGE_DIALOG_ICON_SIZE = 80
private const val DATE_LENGTH = 10

@Composable
private fun badgeDetailDialog(
    badge: UserBadgeResponse,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = badge.insignia.nom,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                badgeIcon(badge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = badge.insignia.descripcio,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aconseguida: ${badge.dataGuanyada.take(DATE_LENGTH)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text("Tancar")
                }
            }
        }
    }
}

@Composable
private fun badgeIcon(badge: UserBadgeResponse) {
    if (badge.insignia.icona != null) {
        AsyncImage(
            model = badge.insignia.icona,
            contentDescription = badge.insignia.nom,
            modifier = Modifier.size(BADGE_DIALOG_ICON_SIZE.dp),
        )
    } else {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = badge.insignia.nom,
            modifier = Modifier.size(BADGE_DIALOG_ICON_SIZE.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun badgesAndActivitiesZone(
    viewModel: ActivityViewModel = viewModel(),
    onActivityClick: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadActivities()
    }
    LaunchedEffect(Unit) {
        AppEvents.activitySaved.collect {
            viewModel.loadActivities()
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Activitats recents",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        when (uiState) {
            is ActivityUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ActivityUiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.profile_no_activities),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is ActivityUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (uiState as ActivityUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is ActivityUiState.Success -> {
                val activities = (uiState as ActivityUiState.Success).activities
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = activities,
                        key = { it.id },
                    ) { activity ->
                        activityItem(
                            activity = activity,
                            onDelete = { viewModel.deleteActivity(activity.id) },
                            onClick = { onActivityClick(activity.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun activityItem(
    activity: ActivityResponse,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${(activity.distanceMeters / METERS_PER_KM).let { "%.2f".format(it) }} km",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text =
                        formatDuration(activity.durationSeconds) +
                            " · ${"%.1f".format(activity.avgSpeedKmh)} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar activitat",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / SECONDS_PER_MINUTE
    val secs = seconds % SECONDS_PER_MINUTE
    return if (minutes > 0) "${minutes}min ${secs}s" else "${secs}s"
}
