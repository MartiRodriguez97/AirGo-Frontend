package com.airgo.airgoApp.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.activity.domain.ActivityUiState
import com.airgo.airgoApp.features.activity.domain.ActivityViewModel
import com.airgo.airgoApp.features.tracking.components.pastActivitySensationCard
import com.airgo.airgoApp.features.tracking.components.postActivitySensationDialog
import com.airgo.airgoApp.features.tracking.data.TrackingData
import com.airgo.airgoApp.features.tracking.trackingSummaryRouteCard
import com.airgo.airgoApp.features.tracking.trackingSummaryStatsCard
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun pastActivityDetailScreen(
    activityId: Int,
    activityViewModel: ActivityViewModel,
    onBack: () -> Unit,
) {
    val uiState = activityViewModel.uiState.collectAsState().value

    val activity =
        if (uiState is ActivityUiState.Success) {
            uiState.activities.find { it.id == activityId }
        } else {
            null
        }
    var showEditSensationDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (activity != null) {
            // Mapeamos los datos para poder reutilizar los componentes de TrackingSummaryScreen
            val trackingDataMapeado =
                TrackingData(
                    distanceMeters = activity.distanceMeters,
                    elapsedSeconds = activity.durationSeconds.toLong(),
                    avgSpeedKmh = activity.avgSpeedKmh,
                )

            // Convertimos las coordenadas en una lista de GeoPoints para pintar el mapa
            val geoPoints =
                activity.routePoints?.map { point ->
                    GeoPoint(point[0], point[1])
                } ?: emptyList()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (activity.sensacio != null) {
                    pastActivitySensationCard(
                        sensacioValue = activity.sensacio,
                        onClick = { showEditSensationDialog = true },
                    )
                }
                trackingSummaryStatsCard(data = trackingDataMapeado)
                trackingSummaryRouteCard(trackPoints = geoPoints)
            }
            if (showEditSensationDialog) {
                postActivitySensationDialog(
                    onDismiss = { showEditSensationDialog = false },
                    onSensationSelected = { nuevoValor ->
                        // Llamamos al viewModel para guardar el cambio en el backend
                        activityViewModel.updateActivitySensation(activityId, nuevoValor)
                        showEditSensationDialog = false
                    },
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
