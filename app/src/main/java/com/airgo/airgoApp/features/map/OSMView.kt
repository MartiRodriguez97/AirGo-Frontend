package com.airgo.airgoApp.features.map

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.components.errorToastHandlers
import com.airgo.airgoApp.features.map.components.fetchLocationDataEffect
import com.airgo.airgoApp.features.map.components.gpsDisabledWarning
import com.airgo.airgoApp.features.map.components.layerSelector
import com.airgo.airgoApp.features.map.components.locationCenteringButton
import com.airgo.airgoApp.features.map.components.modeSelector
import com.airgo.airgoApp.features.map.components.rememberLocationPermissions
import com.airgo.airgoApp.features.map.components.routeSelectionPanel
import com.airgo.airgoApp.features.map.components.trackingFabSection
import com.airgo.airgoApp.features.map.domain.BicingViewModel
import com.airgo.airgoApp.features.map.domain.MapLayerParams
import com.airgo.airgoApp.features.map.domain.MapState
import com.airgo.airgoApp.features.map.domain.MapStateParams
import com.airgo.airgoApp.features.map.domain.MapViewModelParams
import com.airgo.airgoApp.features.map.domain.PollutionViewModel
import com.airgo.airgoApp.features.map.domain.RouteViewModel
import com.airgo.airgoApp.features.map.helpers.BicingHelper
import com.airgo.airgoApp.features.map.helpers.LocationHelper
import com.airgo.airgoApp.features.map.helpers.PollutionHelper
import com.airgo.airgoApp.features.map.helpers.checkInitialLocationPermission
import com.airgo.airgoApp.features.map.helpers.configureMapView
import com.airgo.airgoApp.features.map.helpers.createMapEventsOverlay
import com.airgo.airgoApp.features.map.helpers.rememberGpsEnabledState
import com.airgo.airgoApp.features.map.helpers.updateRouteElements
import com.airgo.airgoApp.features.map.helpers.updateTrackingPolyline
import com.airgo.airgoApp.features.tracking.components.postActivitySensationDialog
import com.airgo.airgoApp.features.tracking.components.trackingPanel
import com.airgo.airgoApp.features.tracking.domain.TrackingViewModel
import com.airgo.airgoApp.features.tracking.helpers.TrackingService
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

private const val TILE_THREADS: Short = 8
private const val CACHE_TILE_COUNT: Short = 1000
private const val MEGABYTES_TO_BYTES = 1024L * 1024L
private const val MAX_CACHE_MB = 100L
private const val MAX_CACHE_BYTES = MAX_CACHE_MB * MEGABYTES_TO_BYTES
private const val TRACKING_ZOOM_LEVEL = 18.0

/**
 * Composable principal que mostra el mapa i les capes.
 * Gestiona els permissos, la configuració del mapa i la interacció amb el ViewModel.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun osmView(
    modifier: Modifier = Modifier,
    pollutionViewModel: PollutionViewModel = viewModel(),
    routeViewModel: RouteViewModel = viewModel(),
    bicingViewModel: BicingViewModel = viewModel(),
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToTrackingSummary: () -> Unit = {},
    trackingViewModel: TrackingViewModel = viewModel(),
) {
    val context = LocalContext.current

    // Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    // Optimització: Donar permís per utilitzar més threads i més memòria
    remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            tileDownloadThreads = TILE_THREADS
            cacheMapTileCount = CACHE_TILE_COUNT
            tileFileSystemCacheMaxBytes = MAX_CACHE_BYTES
        }
    }

    // Vista del mapa
    val mapView = remember { MapView(context).apply { configureMapView(this) } }

    // Capa d'ubicació. Crida a LocationHelper que retorna la capa (overlay) amb la localització
    val locationOverlay = remember { LocationHelper.createLocationOverlay(context, mapView) }

    // Capa de Bicing
    val bicingOverlay = rememberBicingOverlay(context, mapView, bicingViewModel)

    // Capa de contaminació
    val pollutionOverlay = rememberPollutionOverlay(mapView, pollutionViewModel)

    // Gestió de permissos
    var hasLocationPermission by remember { mutableStateOf(checkInitialLocationPermission(context)) }
    val requestLocationPermission = rememberLocationPermissions { granted -> hasLocationPermission = granted }

    // Gestió d'errors
    errorToastHandlers(context, pollutionViewModel, routeViewModel, bicingViewModel)

    trackingUiEventEffect(trackingViewModel, context)

    fetchLocationDataEffect(
        hasLocationPermission = hasLocationPermission,
        requestLocationPermission = requestLocationPermission,
        pollutionViewModel = pollutionViewModel,
        bicingViewModel = bicingViewModel,
        context = context,
    )

    // Desactivar les capes quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    trackingLocationEffect(
        trackingViewModel = trackingViewModel,
        context = context,
    )

    LaunchedEffect(trackingViewModel.hasSummary) {
        if (trackingViewModel.hasSummary) {
            onNavigateToTrackingSummary()
        }
    }

    // Encapsular tot l'estat del mapa en una sola classe per passar-la a la funció de renderitzat
    val mapState =
        rememberMapState(
            MapStateParams(
                layerParams =
                    MapLayerParams(
                        mapView = mapView,
                        locationOverlay = locationOverlay,
                        bicingOverlay = bicingOverlay,
                        pollutionOverlay = pollutionOverlay,
                        hasLocationPermission = hasLocationPermission,
                    ),
                viewModelParams =
                    MapViewModelParams(
                        pollutionViewModel = pollutionViewModel,
                        routeViewModel = routeViewModel,
                        trackingViewModel = trackingViewModel,
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToTrackingSummary = onNavigateToTrackingSummary,
                    ),
            ),
        )
    // Funció que pinta el mapa i les capes

    if (trackingViewModel.showSensationDialog) {
        postActivitySensationDialog(
            onDismiss = { trackingViewModel.submitSensation(null) },
            onSensationSelected = { valor -> trackingViewModel.submitSensation(valor) },
        )
    }
    osmMapContainer(state = mapState, modifier = modifier)
}

@Composable
private fun rememberBicingOverlay(
    context: android.content.Context,
    mapView: MapView,
    bicingViewModel: BicingViewModel,
): FolderOverlay {
    val overlay = remember { FolderOverlay() }
    LaunchedEffect(bicingViewModel.stations) {
        overlay.items.clear()
        overlay.items.addAll(BicingHelper.createBicingOverlay(context, mapView, bicingViewModel.stations).items)
        mapView.invalidate()
    }
    return overlay
}

@Composable
private fun rememberPollutionOverlay(
    mapView: MapView,
    pollutionViewModel: PollutionViewModel,
): FolderOverlay {
    val overlay = remember { FolderOverlay() }
    LaunchedEffect(pollutionViewModel.stations) {
        overlay.items.clear()
        overlay.items.addAll(PollutionHelper.createPollutionOverlay(mapView, pollutionViewModel.stations).items)
        mapView.invalidate()
    }
    return overlay
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun trackingLocationEffect(
    trackingViewModel: TrackingViewModel,
    context: android.content.Context,
) {
    DisposableEffect(trackingViewModel.isTracking) {
        if (trackingViewModel.isTracking) {
            trackingViewModel.registerLocationReceiver(context)
            context.startForegroundService(
                Intent(context, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_START
                },
            )
        }
        onDispose {
            if (!trackingViewModel.isTracking) {
                trackingViewModel.unregisterLocationReceiver()
                context.startService(
                    Intent(context, TrackingService::class.java).apply {
                        action = TrackingService.ACTION_STOP
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberMapState(params: MapStateParams): MapState {
    val l = params.layerParams
    val vm = params.viewModelParams
    return remember(
        l.hasLocationPermission,
        vm.pollutionViewModel.stations,
        vm.routeViewModel.selectionMode,
        vm.routeViewModel.startPoint,
        vm.routeViewModel.endPoint,
        vm.routeViewModel.currentRoute,
        vm.routeViewModel.isLoading,
        vm.routeViewModel.isFavorite,
        vm.routeViewModel.favoriteMessage,
        vm.trackingViewModel.isTracking,
        vm.trackingViewModel.data,
        vm.trackingViewModel.trackPoints,
        vm.routeViewModel.waypoints,
        vm.routeViewModel.transportProfile,
    ) {
        MapState(
            mapView = l.mapView,
            locationOverlay = l.locationOverlay,
            bicingOverlay = l.bicingOverlay,
            pollutionOverlay = l.pollutionOverlay,
            hasLocationPermission = l.hasLocationPermission,
            stations = vm.pollutionViewModel.stations,
            selectionMode = vm.routeViewModel.selectionMode,
            startPoint = vm.routeViewModel.startPoint,
            endPoint = vm.routeViewModel.endPoint,
            currentRoute = vm.routeViewModel.currentRoute,
            isCalculatingRoute = vm.routeViewModel.isLoading,
            onPointSelected = { vm.routeViewModel.onPointSelected(it) },
            onCalculateRoute = { vm.routeViewModel.calculateRoute() },
            onClearRoute = { vm.routeViewModel.clearRoute() },
            onSetSelectionMode = { vm.routeViewModel.selectionMode = it },
            onAddToFavorites = { vm.routeViewModel.addToFavorites() },
            isFavorite = vm.routeViewModel.isFavorite,
            favoriteMessage = vm.routeViewModel.favoriteMessage,
            onNavigateToFavorites = vm.onNavigateToFavorites,
            onNavigateToTrackingSummary = vm.onNavigateToTrackingSummary,
            isTracking = vm.trackingViewModel.isTracking,
            trackingData = vm.trackingViewModel.data,
            trackPoints = vm.trackingViewModel.trackPoints,
            onStartTracking = { vm.trackingViewModel.startTracking() },
            onStopTracking = { vm.trackingViewModel.stopTracking() },
            waypoints = vm.routeViewModel.waypoints,
            onAddWaypoint = { vm.routeViewModel.addWaypoint() },
            onRemoveWaypoint = { vm.routeViewModel.removeWaypoint(it) },
            transportProfile = vm.routeViewModel.transportProfile,
            onTransportProfileChange = { vm.routeViewModel.transportProfile = it },
        )
    }
}

/**
 * Funció per pintar el mapa
 */
@Composable
fun osmMapContainer(
    state: MapState,
    modifier: Modifier = Modifier,
) {
    // Variable per saber si s'activa/desactiva la capa de Bicing
    var showBicing by remember { mutableStateOf(true) }
    // Variable per saber si s'activa/desactiva la capa de qualitat de l'aire
    var showPollution by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val isGpsEnabled by rememberGpsEnabledState(context)

    Box(modifier = modifier.fillMaxSize()) {
        mapRenderer(
            state = state,
            showBicing = showBicing,
            showPollution = showPollution,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        )

        trackingMapEffects(state)

        if (!isGpsEnabled or !state.hasLocationPermission) {
            gpsDisabledWarning(
                hasLocationPermission = state.hasLocationPermission,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 170.dp, end = 16.dp),
            )
        } else {
            locationCenteringButton(
                state,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(bottom = 170.dp, end = 16.dp),
            )
        }

        // Selector de capes (Bicing i contaminació)
        layerSelector(
            showBicing = showBicing,
            onToggleBicing = { showBicing = it },
            showPollution = showPollution,
            onTogglePollution = { showPollution = it },
        )

        // Panell de selecció de ruta i botons només si no estem en mode tracking
        if (!state.isTracking) {
            mapNormalModeUi(
                state = state,
            )
        }

        // Panell de tracking només si estem en tracking
        if (state.isTracking) {
            trackingPanel(
                data = state.trackingData,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
            )
        }

        // Botó de tracking
        trackingFabSection(
            state = state,
            isGpsEnabled = isGpsEnabled,
            context = context,
        )
    }
}

@Composable
private fun trackingMapEffects(state: MapState) {
    LaunchedEffect(state.isTracking) {
        if (state.isTracking) {
            state.locationOverlay.myLocation?.let { location ->
                state.mapView.controller.animateTo(location)
                state.mapView.controller.setZoom(TRACKING_ZOOM_LEVEL)
            }
        }
    }
    LaunchedEffect(state.trackPoints.lastOrNull()) {
        if (state.isTracking && state.trackPoints.isNotEmpty()) {
            state.mapView.controller.animateTo(state.trackPoints.last())
        }
    }
}

@Composable
private fun BoxScope.mapNormalModeUi(state: MapState) {
    // Panell de selecció de ruta
    routeSelectionPanel(
        state = state,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp),
    )

    // Botó per canviar entre mode bici i mode a peu
    modeSelector(
        transportProfile = state.transportProfile,
        onModeChange = { state.onTransportProfileChange(it) },
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 240.dp, end = 16.dp),
    )

    // Botó per navegar a rutes favorites
    FloatingActionButton(
        onClick = { state.onNavigateToFavorites() },
        modifier =
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(bottom = 100.dp, start = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Icon(imageVector = Icons.Default.Favorite, contentDescription = stringResource(R.string.view_favorites_routes))
    }
}

@Composable
private fun mapRenderer(
    state: MapState,
    showBicing: Boolean,
    showPollution: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectionModeState = rememberUpdatedState(state.selectionMode)
    val onPointSelectedState = rememberUpdatedState(state.onPointSelected)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            state.mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(INITIAL_ZOOM_LEVEL)
                controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

                overlays.add(state.pollutionOverlay)
                val eventsOverlay =
                    createMapEventsOverlay(
                        mapView = this,
                        bicingOverlay = state.bicingOverlay,
                        selectionModeProvider = { selectionModeState.value },
                        onPointSelectedProvider = { onPointSelectedState.value },
                    )
                overlays.add(eventsOverlay)
                overlays.add(state.bicingOverlay)
                overlays.add(state.locationOverlay)
            }
        },
        update = { map ->
            if (state.hasLocationPermission && !state.locationOverlay.isMyLocationEnabled) {
                state.locationOverlay.enableMyLocation()
            }
            state.bicingOverlay.isEnabled = showBicing
            state.pollutionOverlay.isEnabled = showPollution

            // Dibuixar ruta de tracking
            updateTrackingPolyline(map, state.trackPoints)

            updateRouteElements(map, state)
            map.invalidate()
        },
    )
}

@Composable
private fun trackingUiEventEffect(
    trackingViewModel: TrackingViewModel,
    context: android.content.Context,
) {
    LaunchedEffect(Unit) {
        trackingViewModel.uiEvent.collect { event ->
            when (event) {
                is com.airgo.airgoApp.features.tracking.domain.TrackingUiEvent.SaveSuccess -> {
                    val message =
                        if (event.completed) {
                            "Entrenament completat i guardat!"
                        } else {
                            "Progress guardat"
                        }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                is com.airgo.airgoApp.features.tracking.domain.TrackingUiEvent.SaveError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
