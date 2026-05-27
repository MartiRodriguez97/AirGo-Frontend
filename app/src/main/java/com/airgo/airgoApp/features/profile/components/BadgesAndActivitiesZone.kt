package com.airgo.airgoApp.features.profile.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.AppEvents
import com.airgo.airgoApp.features.activity.data.ActivityResponse
import com.airgo.airgoApp.features.activity.domain.ActivityUiState
import com.airgo.airgoApp.features.activity.domain.ActivityViewModel

private const val SECONDS_PER_MINUTE = 60
private const val METERS_PER_KM = 1000

@Composable
fun badgesAndActivitiesZone(
    viewModel: ActivityViewModel = viewModel(),
    onActivityClick: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadActivities(forceRefresh = false)
    }
    LaunchedEffect(Unit) {
        AppEvents.activitySaved.collect {
            viewModel.loadActivities(forceRefresh = true)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_activities_recent_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        when (val state = uiState) {
            is ActivityUiState.Loading -> activityLoadingBox()
            is ActivityUiState.Empty -> activityEmptyBox()
            is ActivityUiState.Error -> activityErrorBox(state)
            is ActivityUiState.Success ->
                successActivitiesSection(
                    state = state,
                    viewModel = viewModel,
                    onActivityClick = onActivityClick,
                )
        }
    }
}

@Composable
private fun activityLoadingBox() {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun activityEmptyBox() {
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

@Composable
private fun activityErrorBox(state: ActivityUiState.Error) {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(state.message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun successActivitiesSection(
    state: ActivityUiState.Success,
    viewModel: ActivityViewModel,
    onActivityClick: (Int) -> Unit,
) {
    val trackingOnlyActivities = state.activities.filter { it.dataInici == null }

    if (trackingOnlyActivities.isEmpty()) {
        activityEmptyBox()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = trackingOnlyActivities,
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

private const val MAX_DATE_LENGTH = 10

@Composable
private fun activityItem(
    activity: ActivityResponse,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
                val formattedDate =
                    remember(activity.createdAt) {
                        activity.createdAt?.let { dateStr ->
                            try {
                                val parsedDate =
                                    java.time.OffsetDateTime
                                        .parse(dateStr)
                                        .toLocalDate()
                                parsedDate.format(
                                    java.time.format.DateTimeFormatter
                                        .ofPattern("dd/MM"),
                                )
                            } catch (e: java.time.format.DateTimeParseException) {
                                dateStr.take(MAX_DATE_LENGTH)
                            }
                        } ?: "--/--"
                    }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
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
                    contentDescription = stringResource(R.string.profile_activities_delete_desc),
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
